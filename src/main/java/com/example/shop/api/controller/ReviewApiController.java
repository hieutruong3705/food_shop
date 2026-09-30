package com.example.shop.api.controller;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.Review;
import com.example.shop.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/products/{id}/reviews") @Transactional
public class ReviewApiController {
    private final ReviewRepository reviews;private final ProductRepository products;private final UserRepository users;private final OrderDetailRepository details;
    public ReviewApiController(ReviewRepository r,ProductRepository p,UserRepository u,OrderDetailRepository d){reviews=r;products=p;users=u;details=d;}
    @GetMapping public java.util.List<CommerceResponse.ReviewView> list(@PathVariable long id,Authentication a){return reviews.findByProduct(products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm",id))).stream().map(r->CommerceResponse.review(r,a==null?null:a.getName())).toList();}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public CommerceResponse.ReviewView create(Authentication a,@PathVariable long id,@Valid @RequestBody ReviewInput input){
        var u=users.lockByEmail(a.getName());var p=products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm",id));
        if(!details.existsByOrderUserEmailAndProductIdAndOrderStatus(a.getName(),id,"COMPLETED")) throw new IllegalArgumentException("Bạn chỉ được đánh giá sản phẩm đã mua và nhận hàng");
        if(reviews.existsByUserAndProduct(u,p))throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này");
        var r=new Review();r.setUser(u);r.setProduct(p);r.setRating(input.rating());r.setContent(input.content());return CommerceResponse.review(reviews.save(r));
    }
    @GetMapping("/summary") @Transactional(readOnly=true)
    public java.util.Map<String,Object> summary(@PathVariable long id,Authentication a){
        if(!products.existsById(id))throw new ResourceNotFoundException("Sản phẩm",id);
        var summary=reviews.summarize(id);
        boolean purchased=a!=null&&details.existsByOrderUserEmailAndProductIdAndOrderStatus(a.getName(),id,"COMPLETED");
        boolean reviewed=a!=null&&reviews.existsByUserEmailAndProductId(a.getName(),id);
        return java.util.Map.of("average",summary.getAverage(),"count",summary.getCount(),"canReview",purchased&&!reviewed,"purchased",purchased);
    }
    @PutMapping("/{reviewId}")
    public CommerceResponse.ReviewView update(Authentication a,@PathVariable long id,@PathVariable long reviewId,@Valid @RequestBody ReviewInput input){
        var r=owned(a,id,reviewId);r.setRating(input.rating());r.setContent(input.content());return CommerceResponse.review(r,a.getName());
    }
    @DeleteMapping("/{reviewId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(Authentication a,@PathVariable long id,@PathVariable long reviewId){reviews.delete(owned(a,id,reviewId));}
    private Review owned(Authentication a,long productId,long reviewId){
        users.lockByEmail(a.getName());
        var r=reviews.findById(reviewId).orElseThrow(()->new ResourceNotFoundException("Đánh giá",reviewId));
        if(r.getProduct().getId()!=productId || !r.getUser().getEmail().equals(a.getName()))throw new ResourceNotFoundException("Đánh giá",reviewId);
        return r;
    }
}
