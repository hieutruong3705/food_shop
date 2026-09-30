package com.example.shop.api.service;

import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.dto.CommerceResponse.*;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @Transactional
public class CommerceService {
    @org.springframework.beans.factory.annotation.Autowired private VariantService variants;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final UserRepository users;
    private final CartRepository carts;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final VoucherRepository vouchers;
    private final SandboxPaymentService payments;
    public CommerceService(UserRepository users, CartRepository carts, ProductRepository products, OrderRepository orders, VoucherRepository vouchers, SandboxPaymentService payments) {
        this.users=users; this.carts=carts; this.products=products; this.orders=orders; this.vouchers=vouchers;
        this.payments=payments;
    }
    private User user(String email) {
        User u=users.lockByEmail(email);
        if(u==null || !u.isEnabled() || u.getIsLocked()) throw new org.springframework.security.access.AccessDeniedException("Account unavailable");
        return u;
    }
    private Cart cart(User u) {
        Cart c=carts.findByUser(u);
        if(c==null) { c=new Cart(); c.setUser(u); c=carts.save(c); }
        return c;
    }
    private CartView view(Cart c) {
        List<CartLine> lines=c.getCartItems().stream().map(i -> new CartLine(i.getId(),i.getProduct().getId(),i.getProduct().getName(),i.getVariant()!=null && i.getVariant().getImageUrl()!=null && !i.getVariant().getImageUrl().isBlank()?i.getVariant().getImageUrl():i.getProduct().getImage(),i.getQuantity(),VariantService.stock(i.getProduct(),i.getVariant()),price(i.getProduct(),i.getVariant()),i.getProduct().isActive() && i.getProduct().isTechnologyCatalog() && (i.getVariant()==null?!i.getProduct().hasVariants():i.getVariant().isActive()),i.getVariant()==null?null:i.getVariant().getId(),i.getVariant()==null?null:i.getVariant().getLabel())).toList();
        return new CartView(lines,lines.stream().mapToLong(CartLine::quantity).sum(),lines.stream().mapToDouble(i -> i.price()*i.quantity()).sum());
    }
    public CartView getCart(String email) { return view(cart(user(email))); }
    public Quote quote(String email,String code,List<Long> selectedIds) {
        Cart c=cart(user(email));if(c.getCartItems().isEmpty())throw new IllegalArgumentException("Giỏ hàng trống");
        Voucher voucher=resolveVoucher(code);BigDecimal subtotal=BigDecimal.ZERO,eligible=BigDecimal.ZERO;
        for(var item:selection(c,selectedIds)){
            var p=item.getProduct();VariantService.check(p,item.getVariant(),item.getQuantity());var amount=BigDecimal.valueOf(price(p,item.getVariant())).multiply(BigDecimal.valueOf(item.getQuantity()));subtotal=subtotal.add(amount);
            if(applies(voucher,p))eligible=eligible.add(amount);
        }
        return totals(subtotal,eligible,voucher);
    }
    private List<CartItem> selection(Cart cart,List<Long> selectedIds){
        if(selectedIds==null)return new ArrayList<>(cart.getCartItems());
        if(selectedIds.isEmpty() || selectedIds.size()>100 || new HashSet<>(selectedIds).size()!=selectedIds.size())throw new IllegalArgumentException("Chọn sản phẩm cần đặt hàng");
        var selected=cart.getCartItems().stream().filter(i->selectedIds.contains(i.getId())).toList();
        if(selected.size()!=selectedIds.size())throw new IllegalArgumentException("Sản phẩm được chọn không còn trong giỏ hàng");return selected;
    }
    private Voucher resolveVoucher(String code){
        if(code==null || code.isBlank())return null;
        if(code.length()>50)throw new IllegalArgumentException("Voucher không hợp lệ");
        Voucher v=vouchers.lockByCode(code.trim().toUpperCase(Locale.ROOT));
        if(v==null)throw new IllegalArgumentException("Voucher không hợp lệ");
        entityManager.refresh(v, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!v.isActive() || !Double.isFinite(v.getDiscount()) || v.getDiscount()<=0 || v.getDiscount()>100)throw new IllegalArgumentException("Voucher không hợp lệ hoặc đã ngừng hoạt động");
        Date now=new Date();
        if(v.getStartDate()!=null && now.before(v.getStartDate()))throw new IllegalArgumentException("Voucher chưa đến thời gian sử dụng");
        if(v.getEndDate()!=null && !now.before(v.getEndDate()))throw new IllegalArgumentException("Voucher đã hết hạn");
        if(v.getUsageLimit()!=null && v.getUsedCount()>=v.getUsageLimit())throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
        return v;
    }
    private boolean applies(Voucher voucher,Product p){return voucher!=null && voucher.appliesTo(p);}
    private Quote totals(BigDecimal subtotal,BigDecimal eligible,Voucher voucher){
        if(voucher!=null && eligible.signum()==0)throw new IllegalArgumentException("Voucher không áp dụng cho sản phẩm trong giỏ");
        if(voucher!=null && subtotal.compareTo(BigDecimal.valueOf(voucher.getMinimumOrder()))<0)throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của voucher");
        BigDecimal discount=voucher==null?BigDecimal.ZERO:eligible.multiply(BigDecimal.valueOf(voucher.getDiscount())).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);
        if(voucher!=null && voucher.getMaximumDiscount()!=null)discount=discount.min(BigDecimal.valueOf(voucher.getMaximumDiscount()));
        double shipping=subtotal.compareTo(BigDecimal.valueOf(1000000))>=0?0:30000;
        return new Quote(subtotal.doubleValue(),shipping,discount.doubleValue(),subtotal.subtract(discount).add(BigDecimal.valueOf(shipping)).doubleValue(),voucher==null?"":voucher.getCode());
    }
    public CartView add(String email, AddItem input) {
        Cart c=cart(user(email));
        Product p=products.lockById(input.productId()).orElseThrow(() -> missing("Sản phẩm",input.productId()));
        entityManager.refresh(p,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var variant=variants.require(p,input.variantId());
        CartItem item=c.getCartItems().stream().filter(i -> i.getProduct().getId()==p.getId() && Objects.equals(i.getVariant()==null?null:i.getVariant().getId(),input.variantId())).findFirst().orElse(null);
        long quantity=input.quantity()+(item==null?0:item.getQuantity());
        VariantService.check(p,variant,quantity);
        if(item==null) { item=new CartItem(); item.setCart(c); item.setProduct(p); item.setVariant(variant); c.getCartItems().add(item); }
        item.setQuantity(quantity); item.setPrice(price(p,variant)); c.setSum(c.getCartItems().size());
        carts.saveAndFlush(c); return view(c);
    }
    public CartView update(String email,long id,Long quantity) {
        Cart c=cart(user(email));
        CartItem item=c.getCartItems().stream().filter(i -> i.getId()==id).findFirst().orElseThrow(() -> missing("Mục giỏ hàng",id));
        if(quantity==null) c.getCartItems().remove(item);
        else { VariantService.check(item.getProduct(),item.getVariant(),quantity); item.setQuantity(quantity); }
        c.setSum(c.getCartItems().size()); carts.saveAndFlush(c); return view(c);
    }
    public OrderView checkout(String email,String key,Checkout input) {
        User u=user(email);
        if(key==null || !key.matches("[A-Za-z0-9-]{16,80}")) throw new IllegalArgumentException("Thiếu mã yêu cầu đặt hàng hợp lệ");
        String scopedKey=u.getId()+":"+key;
        String fingerprint=UUID.nameUUIDFromBytes(input.toString().getBytes(StandardCharsets.UTF_8)).toString();
        var existing=orders.findByCheckoutKey(scopedKey);
        if(existing.isPresent()) {
            if(!fingerprint.equals(existing.get().getCheckoutFingerprint())) throw new IllegalArgumentException("Mã yêu cầu đã dùng cho nội dung khác");
            return CommerceResponse.order(existing.get());
        }
        if(!"COD".equals(input.paymentMethod()) && !("VNPAY".equals(input.paymentMethod()) && payments.available())) throw new IllegalArgumentException("Thanh toán trực tuyến chưa được cấu hình. Vui lòng chọn COD.");
        Cart c=cart(u);
        if(c.getCartItems().isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống");
        List<CartItem> selected=selection(c,input.cartItemIds());
        Order o=new Order(); o.setUser(u); o.setCheckoutKey(scopedKey); o.setCheckoutFingerprint(fingerprint);
        o.setReceiverName(input.receiverName().trim()); o.setReceiverAddress(input.receiverAddress().trim()); o.setReceiverPhone(input.receiverPhone());
        o.setNote(input.note()==null?null:input.note().trim());
        o.setStatus("PENDING"); o.setPaymentMethod(input.paymentMethod()); o.setPaymentStatus("UNPAID");
        List<OrderDetail> lines=new ArrayList<>(); BigDecimal subtotal=BigDecimal.ZERO, eligible=BigDecimal.ZERO;
        Voucher voucher=resolveVoucher(input.voucherCode());
        for(CartItem item:selected.stream().sorted(Comparator.comparingLong(i -> i.getProduct().getId())).toList()) {
            entityManager.flush();
            Product p=products.lockById(item.getProduct().getId()).orElseThrow(() -> missing("Sản phẩm",item.getProduct().getId()));
            entityManager.refresh(p, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            var variant=variants.require(p,item.getVariant()==null?null:item.getVariant().getId());
            VariantService.check(p,variant,item.getQuantity());
            OrderDetail d=new OrderDetail(); d.setOrder(o); d.setProduct(p); d.setProductName(p.getName()); d.setProductSku(variant==null?p.getSku():variant.getSku()); d.setVariant(variant); d.setVariantLabel(variant==null?null:variant.getLabel()); d.setQuantity(item.getQuantity()); d.setPrice(price(p,variant)); lines.add(d);
            BigDecimal amount=BigDecimal.valueOf(d.getPrice()).multiply(BigDecimal.valueOf(d.getQuantity())); subtotal=subtotal.add(amount);
            if(applies(voucher,p)) eligible=eligible.add(amount);
            if(variant!=null)variant.setQuantity(variant.getQuantity()-item.getQuantity());
            p.setQuantity(p.getQuantity()-item.getQuantity()); p.setSold(p.getSold()+item.getQuantity());
        }
        Quote quote=totals(subtotal,eligible,voucher);
        if(voucher!=null)voucher.setUsedCount(voucher.getUsedCount()+1);
        o.setOrderDetails(lines); o.setDiscountAmount(quote.discountAmount()); o.setShippingFee(quote.shippingFee()); o.setVoucherCode(voucher==null?null:voucher.getCode());
        o.setTotalPrice(quote.totalPrice());
        orders.saveAndFlush(o); c.getCartItems().removeAll(selected); c.setSum(c.getCartItems().size()); carts.save(c);
        return CommerceResponse.order(o);
    }
    public List<OrderView> list(String email) { return orders.findByUserOrderByIdDesc(user(email)).stream().map(CommerceResponse::order).toList(); }
    public Order requireOrder(String email,long id,boolean admin) {
        User u=user(email); Order o=orders.lockById(id).orElseThrow(() -> missing("Đơn hàng",id));
        if(!admin && o.getUser().getId()!=u.getId()) throw missing("Đơn hàng",id);
        return o;
    }
    public OrderView detail(String email,long id,boolean admin) { return CommerceResponse.order(requireOrder(email,id,admin)); }
    public OrderView archive(String email,long id,boolean archived){
        var order=requireOrder(email,id,true);
        if(!Set.of("COMPLETED","CANCELLED").contains(order.getStatus()))throw new IllegalArgumentException("Chỉ lưu trữ đơn đã kết thúc");
        order.setAdminArchived(archived);return CommerceResponse.order(order);
    }
    public OrderView status(String email,long id,String next,boolean admin) {
        Order o=requireOrder(email,id,admin); String current=o.getStatus();
        if(current.equals(next)) return CommerceResponse.order(o);
        boolean allowed=admin ? com.example.shop.domain.OrderTransitions.allowed(o).contains(next) : "PENDING".equals(current) && "CANCELLED".equals(next);
        if(!allowed) throw new IllegalArgumentException("Không thể chuyển trạng thái đơn hàng");
        if(!"CANCELLED".equals(next) && "VNPAY".equals(o.getPaymentMethod()) && !"PAID".equals(o.getPaymentStatus()))throw new IllegalArgumentException("Đơn trực tuyến chưa thanh toán thành công");
        if("CANCELLED".equals(next)) {
            if("PAID".equals(o.getPaymentStatus())) throw new IllegalArgumentException("Đơn đã thanh toán cần xử lý hoàn tiền trước");
            for(OrderDetail d:o.getOrderDetails().stream().sorted(Comparator.comparingLong(x -> x.getProduct().getId())).toList()) {
                variants.restore(d);
            }
        }
        if("COMPLETED".equals(next)) { o.setCompletedAt(new Date()); o.setPaymentStatus("PAID"); }
        o.setStatus(next); return CommerceResponse.order(o);
    }
    private static double price(Product p,ProductVariant variant) {
        double value=VariantService.price(p,variant);
        if(!Double.isFinite(value) || value<0) throw new IllegalArgumentException("Giá sản phẩm không hợp lệ");
        return BigDecimal.valueOf(value).setScale(0,RoundingMode.HALF_UP).doubleValue();
    }
    private static ResourceNotFoundException missing(String type,long id) { return new ResourceNotFoundException(type,id); }
}
