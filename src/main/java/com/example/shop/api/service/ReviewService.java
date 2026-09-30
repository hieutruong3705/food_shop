package com.example.shop.api.service;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.Review;
import com.example.shop.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@org.springframework.stereotype.Service  
@Transactional
public class ReviewService {
    private final ReviewRepository reviews;private final ProductRepository products;private final UserRepository users;private final OrderDetailRepository details;
    public ReviewService(ReviewRepository r,ProductRepository p,UserRepository u,OrderDetailRepository d){reviews=r;products=p;users=u;details=d;}
     public java.util.List<CommerceResponse.ReviewView> list(long id,Authentication a){return reviews.findByProduct(products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm",id))).stream().map(r->CommerceResponse.review(r,a==null?null:a.getName())).toList();}
     
    public CommerceResponse.ReviewView create(Authentication a,long id,ReviewInput input){
        var u=users.lockByEmail(a.getName());var p=products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm",id));
        if(!details.existsByOrderUserEmailAndProductIdAndOrderStatus(a.getName(),id,com.example.shop.domain.OrderStatus.COMPLETED)) throw new IllegalArgumentException("Bạn chỉ được đánh giá sản phẩm đã mua và nhận hàng");
        if(reviews.existsByUserAndProduct(u,p))throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này");
        var r=new Review();r.setUser(u);r.setProduct(p);r.setRating(input.rating());r.setContent(input.content());return CommerceResponse.review(reviews.save(r));
    }
     
    public java.util.Map<String,Object> summary(long id,Authentication a){
        if(!products.existsById(id))throw new ResourceNotFoundException("Sản phẩm",id);
        var summary=reviews.summarize(id);
        boolean purchased=a!=null&&details.existsByOrderUserEmailAndProductIdAndOrderStatus(a.getName(),id,com.example.shop.domain.OrderStatus.COMPLETED);
        boolean reviewed=a!=null&&reviews.existsByUserEmailAndProductId(a.getName(),id);
        return java.util.Map.of("average",summary.getAverage(),"count",summary.getCount(),"canReview",purchased&&!reviewed,"purchased",purchased);
    }
    
    public CommerceResponse.ReviewView update(Authentication a,long id,long reviewId,ReviewInput input){
        var r=owned(a,id,reviewId);r.setRating(input.rating());r.setContent(input.content());return CommerceResponse.review(r,a.getName());
    }
     
    public void delete(Authentication a,long id,long reviewId){reviews.delete(owned(a,id,reviewId));}
    private Review owned(Authentication a,long productId,long reviewId){
        users.lockByEmail(a.getName());
        var r=reviews.findById(reviewId).orElseThrow(()->new ResourceNotFoundException("Đánh giá",reviewId));
        if(r.getProduct().getId()!=productId || !r.getUser().getEmail().equals(a.getName()))throw new ResourceNotFoundException("Đánh giá",reviewId);
        return r;
    }
}
