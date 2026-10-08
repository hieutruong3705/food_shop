package com.example.shop.api.service;

import com.example.shop.api.dto.PaymentResponse;
import com.example.shop.domain.Order;
import com.example.shop.domain.OrderStatus;
import com.example.shop.domain.PaymentStatus;
import com.example.shop.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** VNPay sandbox only. Payment status changes exclusively through a verified IPN or a verified querydr reconciliation. */
@Service
public class SandboxPaymentService {
    @org.springframework.beans.factory.annotation.Autowired private VariantService variants;
    @org.springframework.beans.factory.annotation.Autowired private VoucherUsage voucherUsage;
    @org.springframework.beans.factory.annotation.Autowired private VnpayQueryClient queries;
    private final OrderRepository orders; private final ProductRepository products;
    private final boolean enabled; private final String merchant,secret,clientUrl,payUrl,returnUrl; private final Clock clock;
    /** Checkout window offered to the payer; the sweep job allows a longer grace period before it releases stock. */
    private static final long WINDOW_SECONDS=900;
    private static final DateTimeFormatter STAMP=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Map<String,String> RESPONSES=Map.ofEntries(Map.entry("00","Giao dịch thành công"),
        Map.entry("07","Giao dịch bị nghi ngờ gian lận"),Map.entry("09","Thẻ chưa đăng ký dịch vụ Internet Banking"),
        Map.entry("10","Xác thực thông tin thẻ không đúng quá 3 lần"),Map.entry("11","Hết hạn chờ thanh toán"),
        Map.entry("12","Thẻ hoặc tài khoản bị khóa"),Map.entry("13","Nhập sai mật khẩu OTP"),
        Map.entry("24","Khách hàng đã hủy giao dịch"),Map.entry("51","Tài khoản không đủ số dư"),
        Map.entry("65","Tài khoản đã vượt hạn mức giao dịch trong ngày"),Map.entry("75","Ngân hàng đang bảo trì"),
        Map.entry("79","Nhập sai mật khẩu thanh toán quá số lần quy định"),Map.entry("91","Không tìm thấy giao dịch"),
        Map.entry("99","Lỗi không xác định"));
    /** Documented querydr response signing order; missing optional fields sign as empty strings. */
    private static final List<String> QUERY_RESPONSE_FIELDS=List.of("vnp_ResponseId","vnp_Command","vnp_ResponseCode","vnp_Message",
        "vnp_TmnCode","vnp_TxnRef","vnp_Amount","vnp_BankCode","vnp_PayDate","vnp_TransactionNo","vnp_TransactionType",
        "vnp_TransactionStatus","vnp_OrderInfo","vnp_PromotionCode","vnp_PromotionAmount");
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entities;
    public SandboxPaymentService(OrderRepository orders,ProductRepository products,
        @Value("${shop.vnpay.enabled:false}") boolean enabled,@Value("${shop.vnpay.tmn-code:}") String merchant,
        @Value("${shop.vnpay.secret:}") String secret,@Value("${shop.client-url:http://localhost:5173}") String clientUrl,
        @Value("${shop.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
        @Value("${shop.vnpay.return-url:}") String returnUrl,Clock clock) {
        this.orders=orders;this.products=products;this.enabled=enabled;this.merchant=merchant;this.secret=secret;this.clientUrl=clientUrl;
        this.payUrl=payUrl;this.returnUrl=returnUrl.isBlank()?clientUrl+"/payment-return":returnUrl;this.clock=clock;
    }
    public boolean available(){return enabled && !merchant.isBlank() && !secret.isBlank();}
    /** Last moment the payer may complete the order, or null when the order is not waiting for an online payment. */
    public Instant expiry(Order order){
        return "VNPAY".equals(order.getPaymentMethod()) && order.getStatus()==OrderStatus.PENDING && order.getPaymentStatus()==PaymentStatus.UNPAID
            ? order.getCreatedAt().toInstant().plusSeconds(WINDOW_SECONDS) : null;
    }
    public PaymentResponse.StatusView status(Order order){
        Instant expires=expiry(order);
        return new PaymentResponse.StatusView(order.getStatus().name(),order.getPaymentStatus().name(),order.getPaymentReference(),
            expires==null?null:expires.toString(),order.isRefundRequired());
    }
    public String paymentUrl(Order order,String ip){
        if(!available())throw new IllegalArgumentException("VNPay sandbox chưa được cấu hình");
        if(!"VNPAY".equals(order.getPaymentMethod()) || order.getPaymentStatus()!=PaymentStatus.UNPAID || order.getStatus()!=OrderStatus.PENDING)throw new IllegalArgumentException("Đơn hàng không thể thanh toán trực tuyến");
        Instant expires=order.getCreatedAt().toInstant().plusSeconds(WINDOW_SECONDS);
        if(expires.isBefore(clock.instant()))throw new IllegalArgumentException("Phiên thanh toán đã hết hạn");
        var params=new TreeMap<String,String>();
        params.put("vnp_Version","2.1.0");params.put("vnp_Command","pay");params.put("vnp_TmnCode",merchant);
        params.put("vnp_Amount",Long.toString(Math.round(order.getTotalPrice()*100)));params.put("vnp_CurrCode","VND");
        params.put("vnp_TxnRef",Long.toString(order.getId()));params.put("vnp_OrderInfo",orderInfo(order.getId()));params.put("vnp_OrderType","other");
        params.put("vnp_Locale","vn");params.put("vnp_ReturnUrl",returnUrl);params.put("vnp_IpAddr",ip);
        params.put("vnp_CreateDate",STAMP.format(order.getCreatedAt().toInstant()));params.put("vnp_ExpireDate",STAMP.format(expires));
        String query=canonical(params);return payUrl+"?"+query+"&vnp_SecureHash="+sign(query);
    }
    /**
     * Reads a return-URL callback. The payer's browser is not a trusted channel, so this never writes:
     * the order only moves on a verified IPN or a verified querydr reconciliation.
     */
    @Transactional(readOnly=true) public PaymentResponse.ReturnView verifyReturn(Map<String,String> incoming){
        if(!available())return invalid("VNPay sandbox chưa được cấu hình");
        var params=signedFields(incoming);
        if(!verified(incoming,params) || !merchant.equals(params.get("vnp_TmnCode")))return invalid("Không xác thực được kết quả thanh toán");
        Long id=null;try{id=Long.parseLong(params.get("vnp_TxnRef"));}catch(Exception exception){/* reported as a missing order below */}
        Long amount=null;try{amount=Long.parseLong(params.get("vnp_Amount"))/100;}catch(Exception exception){/* absent on some failures */}
        String code=params.get("vnp_ResponseCode");
        var order=id==null?Optional.<Order>empty():orders.findById(id);
        return new PaymentResponse.ReturnView(true,id,code,params.get("vnp_TransactionStatus"),amount,params.get("vnp_BankCode"),
            params.get("vnp_TransactionNo"),order.map(o->o.getStatus().name()).orElse(null),order.map(o->o.getPaymentStatus().name()).orElse(null),
            order.map(Order::getPaymentReference).orElse(null),order.map(Order::isRefundRequired).orElse(false),
            order.isEmpty()?"Không tìm thấy đơn hàng":describe(code));
    }
    @Transactional public Map<String,String> ipn(Map<String,String> incoming){
        if(!available())return reply("99","Sandbox unavailable");
        var params=signedFields(incoming);
        if(!verified(incoming,params))return reply("97","Invalid signature");
        if(!merchant.equals(params.get("vnp_TmnCode")))return reply("97","Invalid merchant");
        long id,amount;try{id=Long.parseLong(params.get("vnp_TxnRef"));amount=Long.parseLong(params.get("vnp_Amount"));}catch(Exception e){return reply("01","Order not found");}
        var optional=orders.lockById(id);if(optional.isEmpty())return reply("01","Order not found");Order o=optional.get();
        boolean paid="00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        return settleOrder(o,amount,paid,reference(params.get("vnp_TransactionNo")));
    }
    /** The one place an order's payment outcome is written. Callers must hold the order's row lock. */
    private Map<String,String> settleOrder(Order o,long amount,boolean paid,String transactionNo){
        if(!"VNPAY".equals(o.getPaymentMethod()) || amount!=Math.round(o.getTotalPrice()*100))return reply("04","Invalid amount");
        if(o.getPaymentStatus()!=PaymentStatus.UNPAID || o.getStatus()!=OrderStatus.PENDING){
            // Money was taken after the order was cancelled or expired. Stock may already belong to
            // someone else, so the order stays closed and the payment is flagged for a manual refund.
            if(paid && isClosedWithoutPayment(o)){o.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);o.setPaymentReference(transactionNo);return reply("00","Confirmed");}
            return reply("02","Order already confirmed");
        }
        if(paid){o.setPaymentStatus(PaymentStatus.PAID);o.setPaymentReference(transactionNo);}
        else {o.setPaymentStatus(PaymentStatus.FAILED);release(o);}
        return reply("00","Confirmed");
    }
    /** Outcome of a querydr call. `verified` means the gateway answered with a signature this merchant can check. */
    public record QueryResult(boolean verified,String responseCode,String transactionStatus,long amount,String transactionNo,String message) {}
    private static QueryResult unverified(String message){return new QueryResult(false,null,null,0,null,message);}
    /**
     * Asks VNPay what happened to an order, for the case where no IPN ever arrived. Pure HTTP: holds no
     * transaction and changes nothing, so the caller passes the result to {@link #settle(long,QueryResult)}.
     */
    public QueryResult query(long id){
        if(!available())return unverified("VNPay sandbox chưa được cấu hình");
        var optional=orders.findById(id);
        if(optional.isEmpty())return unverified("Không tìm thấy đơn hàng");
        Order order=optional.get();
        if(!"VNPAY".equals(order.getPaymentMethod()))return unverified("Đơn hàng không thanh toán qua VNPay");
        var body=new LinkedHashMap<String,String>();
        body.put("vnp_RequestId",UUID.randomUUID().toString().replace("-",""));body.put("vnp_Version","2.1.0");
        body.put("vnp_Command","querydr");body.put("vnp_TmnCode",merchant);body.put("vnp_TxnRef",Long.toString(id));
        body.put("vnp_OrderInfo",orderInfo(id));body.put("vnp_TransactionDate",STAMP.format(order.getCreatedAt().toInstant()));
        body.put("vnp_CreateDate",STAMP.format(clock.instant()));body.put("vnp_IpAddr","127.0.0.1");
        // querydr signs a fixed field order joined with "|", not the alphabetical query string the pay/IPN flow uses.
        body.put("vnp_SecureHash",sign(String.join("|",body.get("vnp_RequestId"),body.get("vnp_Version"),body.get("vnp_Command"),
            merchant,body.get("vnp_TxnRef"),body.get("vnp_TransactionDate"),body.get("vnp_CreateDate"),body.get("vnp_IpAddr"),body.get("vnp_OrderInfo"))));
        var response=queries.post(body);
        if(response==null)return unverified("Không kết nối được VNPay để đối soát");
        // A refusal carries no signature at all, for instance 94 when the same order is asked about twice within a few minutes.
        if(text(response,"vnp_SecureHash")==null && text(response,"vnp_ResponseCode")!=null)return unverified("VNPay chưa trả kết quả đối soát (mã "+text(response,"vnp_ResponseCode")+"). Vui lòng thử lại sau ít phút.");
        if(!responseVerified(response))return unverified("Chữ ký phản hồi đối soát không hợp lệ");
        if(!merchant.equals(text(response,"vnp_TmnCode")) || !Long.toString(id).equals(text(response,"vnp_TxnRef")))return unverified("Phản hồi đối soát không khớp đơn hàng");
        long amount=0;try{amount=Long.parseLong(text(response,"vnp_Amount"));}catch(Exception exception){/* absent when nothing was charged */}
        String code=text(response,"vnp_ResponseCode");
        return new QueryResult(true,code,text(response,"vnp_TransactionStatus"),amount,text(response,"vnp_TransactionNo"),describe(code));
    }
    /**
     * Applies a verified querydr result. Transaction status 00 settles as paid and 02 as failed; anything else
     * (01 pending, 91 unknown) is reported and left alone so the sweep job can retry or expire the order.
     */
    @Transactional public Map<String,Object> settle(long id,QueryResult result){
        var out=new LinkedHashMap<String,Object>();
        out.put("verified",result.verified());out.put("responseCode",result.responseCode());
        out.put("transactionStatus",result.transactionStatus());out.put("message",result.message());
        var optional=orders.lockById(id);
        if(optional.isEmpty()){out.put("applied",null);out.put("message","Không tìm thấy đơn hàng");return out;}
        Order order=optional.get();
        boolean settles=result.verified() && "00".equals(result.responseCode())
            && ("00".equals(result.transactionStatus()) || "02".equals(result.transactionStatus()));
        out.put("applied",settles?settleOrder(order,result.amount(),"00".equals(result.transactionStatus()),reference(result.transactionNo())).get("RspCode"):null);
        out.put("orderStatus",order.getStatus().name());out.put("paymentStatus",order.getPaymentStatus().name());
        out.put("paymentReference",order.getPaymentReference());out.put("refundRequired",order.isRefundRequired());
        return out;
    }
    private static boolean isClosedWithoutPayment(Order o){return o.getStatus()==OrderStatus.CANCELLED && Set.of(PaymentStatus.UNPAID,PaymentStatus.EXPIRED,PaymentStatus.FAILED).contains(o.getPaymentStatus());}
    private static String reference(String value){return value==null || value.isBlank()?null:value.substring(0,Math.min(value.length(),64));}
    private static String orderInfo(long id){return "Thanh toan don hang "+id;}
    private static String describe(String code){return code==null?"Không có mã phản hồi":RESPONSES.getOrDefault(code,"Mã phản hồi "+code);}
    private static PaymentResponse.ReturnView invalid(String message){return new PaymentResponse.ReturnView(false,null,null,null,null,null,null,null,null,null,false,message);}
    @Transactional public void expire(long id){var optional=orders.lockById(id);if(optional.isEmpty())return;var o=optional.get();if("VNPAY".equals(o.getPaymentMethod()) && o.getStatus()==OrderStatus.PENDING && o.getPaymentStatus()==PaymentStatus.UNPAID && o.getCreatedAt().toInstant().plusSeconds(1200).isBefore(clock.instant())){o.setPaymentStatus(PaymentStatus.EXPIRED);release(o);}}
    private void release(Order order){
        // Voucher first, then products: checkout locks them in the same order.
        voucherUsage.refund(order);
        for(var d:order.getOrderDetails().stream().sorted(Comparator.comparingLong(i->i.getProduct().getId())).toList()){
            variants.restore(d);
        }
        order.setStatus(OrderStatus.CANCELLED);
    }
    /** Signed fields of a callback: every non-empty vnp_ parameter except the hash itself. */
    private static TreeMap<String,String> signedFields(Map<String,String> incoming){
        var params=new TreeMap<String,String>();
        incoming.forEach((k,v)->{if(k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType") && v!=null && !v.isEmpty())params.put(k,v);});
        return params;
    }
    private boolean verified(Map<String,String> incoming,Map<String,String> params){
        String received=incoming.getOrDefault("vnp_SecureHash","");
        return received.matches("(?i)[0-9a-f]{128}") && MessageDigest.isEqual(sign(canonical(params)).getBytes(StandardCharsets.US_ASCII),
            received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    private boolean responseVerified(com.fasterxml.jackson.databind.JsonNode response){
        String received=text(response,"vnp_SecureHash");
        if(received==null || !received.matches("(?i)[0-9a-f]{128}"))return false;
        String data=String.join("|",QUERY_RESPONSE_FIELDS.stream().map(field->{var value=text(response,field);return value==null?"":value;}).toList());
        return MessageDigest.isEqual(sign(data).getBytes(StandardCharsets.US_ASCII),received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    private static String text(com.fasterxml.jackson.databind.JsonNode node,String field){var value=node.get(field);return value==null || value.isNull()?null:value.asText();}
    public static String canonical(Map<String,String> params){return new TreeMap<>(params).entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.US_ASCII)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));}
    private String sign(String data){try{var mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Cannot sign payment",e);}}
    private Map<String,String> reply(String code,String message){return Map.of("RspCode",code,"Message",message);}
}
