package com.example.shop.api.service;

import com.example.shop.domain.Order;
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

/** VNPay sandbox only. Payment status changes exclusively through a verified IPN. */
@Service
public class SandboxPaymentService {
    @org.springframework.beans.factory.annotation.Autowired private VariantService variants;
    private final OrderRepository orders; private final ProductRepository products;
    private final boolean enabled; private final String merchant,secret,clientUrl;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entities;
    public SandboxPaymentService(OrderRepository orders,ProductRepository products,
        @Value("${shop.vnpay.enabled:false}") boolean enabled,@Value("${shop.vnpay.tmn-code:}") String merchant,
        @Value("${shop.vnpay.secret:}") String secret,@Value("${shop.client-url:http://localhost:5173}") String clientUrl) {
        this.orders=orders;this.products=products;this.enabled=enabled;this.merchant=merchant;this.secret=secret;this.clientUrl=clientUrl;
    }
    public boolean available(){return enabled && !merchant.isBlank() && !secret.isBlank();}
    public String paymentUrl(Order order,String ip){
        if(!available())throw new IllegalArgumentException("VNPay sandbox chưa được cấu hình");
        if(!"VNPAY".equals(order.getPaymentMethod()) || !"UNPAID".equals(order.getPaymentStatus()) || !"PENDING".equals(order.getStatus()))throw new IllegalArgumentException("Đơn hàng không thể thanh toán trực tuyến");
        Instant expires=order.getCreatedAt().toInstant().plusSeconds(900);
        if(expires.isBefore(Instant.now()))throw new IllegalArgumentException("Phiên thanh toán đã hết hạn");
        var format=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        var params=new TreeMap<String,String>();
        params.put("vnp_Version","2.1.0");params.put("vnp_Command","pay");params.put("vnp_TmnCode",merchant);
        params.put("vnp_Amount",Long.toString(Math.round(order.getTotalPrice()*100)));params.put("vnp_CurrCode","VND");
        params.put("vnp_TxnRef",Long.toString(order.getId()));params.put("vnp_OrderInfo","Thanh toan don hang "+order.getId());params.put("vnp_OrderType","other");
        params.put("vnp_Locale","vn");params.put("vnp_ReturnUrl",clientUrl+"/payment-return");params.put("vnp_IpAddr",ip);
        params.put("vnp_CreateDate",format.format(order.getCreatedAt().toInstant()));params.put("vnp_ExpireDate",format.format(expires));
        String query=canonical(params);return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"+query+"&vnp_SecureHash="+sign(query);
    }
    @Transactional public Map<String,String> ipn(Map<String,String> incoming){
        if(!available())return reply("99","Sandbox unavailable");
        var params=new TreeMap<String,String>();incoming.forEach((k,v)->{if(k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType") && v!=null && !v.isEmpty())params.put(k,v);});
        String received=incoming.getOrDefault("vnp_SecureHash","");
        if(!received.matches("(?i)[0-9a-f]{128}") || !MessageDigest.isEqual(sign(canonical(params)).getBytes(StandardCharsets.US_ASCII),received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))return reply("97","Invalid signature");
        if(!merchant.equals(params.get("vnp_TmnCode")))return reply("97","Invalid merchant");
        long id,amount;try{id=Long.parseLong(params.get("vnp_TxnRef"));amount=Long.parseLong(params.get("vnp_Amount"));}catch(Exception e){return reply("01","Order not found");}
        var optional=orders.lockById(id);if(optional.isEmpty())return reply("01","Order not found");Order o=optional.get();
        if(!"VNPAY".equals(o.getPaymentMethod()) || amount!=Math.round(o.getTotalPrice()*100))return reply("04","Invalid amount");
        boolean paid="00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        if(!"UNPAID".equals(o.getPaymentStatus()) || !"PENDING".equals(o.getStatus())){
            // Money was taken after the order was cancelled or expired. Stock may already belong to
            // someone else, so the order stays closed and the payment is flagged for a manual refund.
            if(paid && isClosedWithoutPayment(o)){o.setPaymentStatus("REFUND_REQUIRED");o.setPaymentReference(reference(params));return reply("00","Confirmed");}
            return reply("02","Order already confirmed");
        }
        if(paid){o.setPaymentStatus("PAID");o.setPaymentReference(reference(params));}
        else {o.setPaymentStatus("FAILED");release(o);}
        return reply("00","Confirmed");
    }
    private static boolean isClosedWithoutPayment(Order o){return "CANCELLED".equals(o.getStatus()) && Set.of("UNPAID","EXPIRED","FAILED").contains(o.getPaymentStatus());}
    private static String reference(Map<String,String> params){String value=params.get("vnp_TransactionNo");return value==null?null:value.substring(0,Math.min(value.length(),64));}
    @Transactional public void expire(long id){var optional=orders.lockById(id);if(optional.isEmpty())return;var o=optional.get();if("VNPAY".equals(o.getPaymentMethod()) && "PENDING".equals(o.getStatus()) && "UNPAID".equals(o.getPaymentStatus()) && o.getCreatedAt().toInstant().plusSeconds(1200).isBefore(Instant.now())){o.setPaymentStatus("EXPIRED");release(o);}}
    private void release(Order order){
        for(var d:order.getOrderDetails().stream().sorted(Comparator.comparingLong(i->i.getProduct().getId())).toList()){
            variants.restore(d);
        }
        order.setStatus("CANCELLED");
    }
    public static String canonical(Map<String,String> params){return new TreeMap<>(params).entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.US_ASCII)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));}
    private String sign(String data){try{var mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Cannot sign payment",e);}}
    private Map<String,String> reply(String code,String message){return Map.of("RspCode",code,"Message",message);}
}
