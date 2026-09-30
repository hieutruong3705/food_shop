package com.example.shop.api.service;
import com.example.shop.api.service.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.domain.OrderStatus;
import com.example.shop.domain.PaymentStatus;
import com.example.shop.api.dto.CommerceRequests.Status;
import com.example.shop.repository.*;
import com.example.shop.service.InvoiceService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.*;

@org.springframework.stereotype.Service  
@Transactional
public class AdminOrderService {
    private final CommerceService commerce;private final OrderRepository orders;private final ProductRepository products;private final UserRepository users;private final InvoiceService invoices;private final java.time.Clock clock;
    public AdminOrderService(CommerceService co,OrderRepository o,ProductRepository p,UserRepository u,InvoiceService i,java.time.Clock clock){commerce=co;orders=o;products=p;users=u;invoices=i;this.clock=clock;}
     public Map<String,Object> dashboard(java.time.LocalDate startDate,java.time.LocalDate endDate){
        var end=endDate==null?java.time.LocalDate.now(clock):endDate;var start=startDate==null?end.minusDays(29):startDate;if(start.isAfter(end)||java.time.temporal.ChronoUnit.DAYS.between(start,end)>366)throw new IllegalArgumentException("Chọn khoảng ngày tối đa 367 ngày");
        var zone=java.time.ZoneId.of("Asia/Ho_Chi_Minh");var rows=orders.findByCreatedAtBetween(Date.from(start.atStartOfDay(zone).toInstant()),Date.from(end.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)));
        var chart=new TreeMap<String,Double>();for(var day=start;!day.isAfter(end);day=day.plusDays(1))chart.put(day.toString(),0d);
        var completed=orders.findCompletedBetween(Date.from(start.atStartOfDay(zone).toInstant()),Date.from(end.plusDays(1).atStartOfDay(zone).toInstant()));
        for(var order:completed)chart.merge((order.getCompletedAt()==null?order.getCreatedAt():order.getCompletedAt()).toInstant().atZone(zone).toLocalDate().toString(),order.getTotalPrice(),Double::sum);
        return AdminSupport.map("orders",rows.size(),"newOrders",rows.stream().filter(o->o.getStatus()!=OrderStatus.CANCELLED).count(),"cancelledOrders",rows.stream().filter(o->o.getStatus()==OrderStatus.CANCELLED).count(),"completedOrders",completed.size(),"products",products.count(),"users",users.count(),"revenue",chart.values().stream().mapToDouble(Double::doubleValue).sum(),"pending",rows.stream().filter(o->o.getStatus()==OrderStatus.PENDING).count(),"lowStock",products.countByQuantityLessThanEqual(5),"refundRequired",orders.countByPaymentStatus(PaymentStatus.REFUND_REQUIRED),"completed",completed.size(),"shipping",rows.stream().filter(o->o.getStatus()==OrderStatus.SHIPPING).count(),"cancelled",rows.stream().filter(o->o.getStatus()==OrderStatus.CANCELLED).count(),"chart",chart,"bestSelling",products.findBestSellingProducts(PageRequest.of(0,5)));
    }
     public Page<CommerceResponse.OrderView> orders(int page,int size,String search,String status,boolean archived,boolean refundRequired){if(page<0 || size<1 || size>100 || search.length()>100)throw new IllegalArgumentException("Phân trang không hợp lệ");return orders.findAll((root,q,cb)->{var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();filters.add(cb.equal(root.get("adminArchived"),archived));if(!search.isBlank())filters.add(cb.or(cb.like(cb.lower(root.get("receiverName")),"%"+search.toLowerCase(Locale.ROOT)+"%"),cb.like(root.get("receiverPhone"),"%"+search+"%")));if(!status.isBlank())filters.add(cb.equal(root.get("status"),OrderStatus.parse(status)));if(refundRequired)filters.add(cb.equal(root.get("paymentStatus"),PaymentStatus.REFUND_REQUIRED));return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));},PageRequest.of(page,size,Sort.by("id").descending())).map(CommerceResponse::order);}
     public CommerceResponse.OrderView order(Authentication a,long id){return commerce.detail(a.getName(),id,true);}
      public void archive(Authentication a,long id){commerce.archive(a.getName(),id,true);}
     public CommerceResponse.OrderView restore(Authentication a,long id){return commerce.archive(a.getName(),id,false);}
     public CommerceResponse.OrderView status(Authentication a,long id,Status s){return commerce.status(a.getName(),id,s.status(),true);}
     public void invoice(Authentication a,long id,jakarta.servlet.http.HttpServletResponse response){invoices.exportInvoice(response,commerce.requireOrder(a.getName(),id,true));}
     public org.springframework.http.ResponseEntity<String> report(){var csv=new StringBuilder("id,createdAt,status,paymentMethod,totalPrice\n");orders.findAll(Sort.by("id")).forEach(o -> csv.append(o.getId()).append(',').append(o.getCreatedAt()).append(',').append(o.getStatus()).append(',').append(o.getPaymentMethod()).append(',').append(o.getTotalPrice()).append('\n'));return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=orders.csv").body(csv.toString());}
}
