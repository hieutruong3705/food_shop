package com.example.shop.api.controller;
import com.example.shop.api.service.*;
import com.example.shop.api.dto.CommerceResponse;
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

@RestController @RequestMapping("/api/v1/admin") @Transactional
public class AdminApiController {
    private final AdminCatalogService catalog;private final CommerceService commerce;private final OrderRepository orders;private final ProductRepository products;private final UserRepository users;private final InvoiceService invoices;
    public AdminApiController(AdminCatalogService c,CommerceService co,OrderRepository o,ProductRepository p,UserRepository u,InvoiceService i){catalog=c;commerce=co;orders=o;products=p;users=u;invoices=i;}
    @GetMapping("/dashboard") public Map<String,Object> dashboard(@RequestParam(required=false) java.time.LocalDate startDate,@RequestParam(required=false) java.time.LocalDate endDate){
        var end=endDate==null?java.time.LocalDate.now():endDate;var start=startDate==null?end.minusDays(29):startDate;if(start.isAfter(end)||java.time.temporal.ChronoUnit.DAYS.between(start,end)>366)throw new IllegalArgumentException("Chọn khoảng ngày tối đa 367 ngày");
        var zone=java.time.ZoneId.of("Asia/Ho_Chi_Minh");var rows=orders.findByCreatedAtBetween(Date.from(start.atStartOfDay(zone).toInstant()),Date.from(end.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)));
        var chart=new TreeMap<String,Double>();for(var day=start;!day.isAfter(end);day=day.plusDays(1))chart.put(day.toString(),0d);
        for(var order:rows)if("COMPLETED".equals(order.getStatus()))chart.merge(order.getCreatedAt().toInstant().atZone(zone).toLocalDate().toString(),order.getTotalPrice(),Double::sum);
        return AdminCatalogService.map("orders",rows.size(),"products",products.count(),"users",users.count(),"revenue",chart.values().stream().mapToDouble(Double::doubleValue).sum(),"pending",rows.stream().filter(o->"PENDING".equals(o.getStatus())).count(),"lowStock",products.countByQuantityLessThanEqual(5),"refundRequired",orders.countByPaymentStatus("REFUND_REQUIRED"),"completed",rows.stream().filter(o->"COMPLETED".equals(o.getStatus())).count(),"shipping",rows.stream().filter(o->"SHIPPING".equals(o.getStatus())).count(),"cancelled",rows.stream().filter(o->"CANCELLED".equals(o.getStatus())).count(),"chart",chart,"bestSelling",products.findBestSellingProducts(PageRequest.of(0,5)));
    }
    @GetMapping("/orders") public Page<CommerceResponse.OrderView> orders(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="false") boolean archived,@RequestParam(defaultValue="false") boolean refundRequired){if(page<0 || size<1 || size>100 || search.length()>100)throw new IllegalArgumentException("Phân trang không hợp lệ");return orders.findAll((root,q,cb)->{var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();filters.add(cb.equal(root.get("adminArchived"),archived));if(!search.isBlank())filters.add(cb.or(cb.like(cb.lower(root.get("receiverName")),"%"+search.toLowerCase(Locale.ROOT)+"%"),cb.like(root.get("receiverPhone"),"%"+search+"%")));if(!status.isBlank())filters.add(cb.equal(root.get("status"),status));if(refundRequired)filters.add(cb.equal(root.get("paymentStatus"),"REFUND_REQUIRED"));return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));},PageRequest.of(page,size,Sort.by("id").descending())).map(CommerceResponse::order);}
    @GetMapping("/orders/{id}") public CommerceResponse.OrderView order(Authentication a,@PathVariable long id){return commerce.detail(a.getName(),id,true);}
    @DeleteMapping("/orders/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void archive(Authentication a,@PathVariable long id){commerce.archive(a.getName(),id,true);}
    @PostMapping("/orders/{id}/restore") public CommerceResponse.OrderView restore(Authentication a,@PathVariable long id){return commerce.archive(a.getName(),id,false);}
    @PatchMapping("/orders/{id}/status") public CommerceResponse.OrderView status(Authentication a,@PathVariable long id,@Valid @RequestBody Status s){return commerce.status(a.getName(),id,s.status(),true);}
    @GetMapping("/orders/{id}/invoice") public void invoice(Authentication a,@PathVariable long id,jakarta.servlet.http.HttpServletResponse response){invoices.exportInvoice(response,commerce.requireOrder(a.getName(),id,true));}
    @GetMapping(value="/reports/orders",produces="text/csv;charset=UTF-8") public org.springframework.http.ResponseEntity<String> report(){var csv=new StringBuilder("id,createdAt,status,paymentMethod,totalPrice\n");orders.findAll(Sort.by("id")).forEach(o -> csv.append(o.getId()).append(',').append(o.getCreatedAt()).append(',').append(o.getStatus()).append(',').append(o.getPaymentMethod()).append(',').append(o.getTotalPrice()).append('\n'));return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=orders.csv").body(csv.toString());}
    @GetMapping("/{resource}") public Page<?> list(@PathVariable String resource,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam Map<String,String> filters){return catalog.list(resource,page,size,search,filters);}
    @GetMapping("/options/{resource}") public Page<Map<String,Object>> options(@PathVariable String resource,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String search,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="") List<Long> ids){return catalog.options(resource,page,search,categoryId,ids);}
    @PatchMapping("/products/{id}/status") public Map<String,Object> selling(@PathVariable long id,@RequestBody JsonNode body){
        if(!body.path("active").isBoolean())throw new IllegalArgumentException("Trạng thái active phải là true hoặc false");
        return catalog.selling(id,body.path("active").asBoolean());
    }
    @GetMapping("/{resource}/{id}") public Object detail(@PathVariable String resource,@PathVariable long id){return catalog.detail(resource,id);}
    @PostMapping("/{resource}") public org.springframework.http.ResponseEntity<Map<String,Object>> create(Authentication a,@PathVariable String resource,@RequestBody JsonNode body){var saved=catalog.save(resource,null,body,a.getName());return org.springframework.http.ResponseEntity.created(java.net.URI.create("/api/v1/admin/"+resource+"/"+saved.get("id"))).body(saved);}
    @PutMapping("/{resource}/{id}") public Map<String,Object> update(Authentication a,@PathVariable String resource,@PathVariable long id,@RequestBody JsonNode body){return catalog.save(resource,id,body,a.getName());}
    @DeleteMapping("/{resource}/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(Authentication a,@PathVariable String resource,@PathVariable long id){catalog.delete(resource,id,a.getName());}
}
