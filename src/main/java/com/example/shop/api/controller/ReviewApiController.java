package com.example.shop.api.controller;
import com.example.shop.api.service.ReviewService;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.Review;
import com.example.shop.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/products/{id}/reviews") 
public class ReviewApiController {
    private final ReviewService service;
    public ReviewApiController(ReviewService service) {
        this.service = service;
    }

    @GetMapping public java.util.List<CommerceResponse.ReviewView> list(@PathVariable long id,Authentication a){
        return service.list(id, a);
    }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public CommerceResponse.ReviewView create(Authentication a,@PathVariable long id,@Valid @RequestBody ReviewInput input){
        return service.create(a, id, input);
    }
    @GetMapping("/summary") 
    public java.util.Map<String,Object> summary(@PathVariable long id,Authentication a){
        return service.summary(id, a);
    }
    @PutMapping("/{reviewId}")
    public CommerceResponse.ReviewView update(Authentication a,@PathVariable long id,@PathVariable long reviewId,@Valid @RequestBody ReviewInput input){
        return service.update(a, id, reviewId, input);
    }
    @DeleteMapping("/{reviewId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(Authentication a,@PathVariable long id,@PathVariable long reviewId){
        service.delete(a, id, reviewId);
    }

}
