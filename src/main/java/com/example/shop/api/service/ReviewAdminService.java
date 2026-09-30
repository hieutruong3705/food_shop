package com.example.shop.api.service;
import com.example.shop.api.dto.admin.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.dto.catalog.VariantResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.example.shop.api.service.AdminSupport.*;

@Service
@Transactional
public class ReviewAdminService {
    private final ReviewRepository reviews;
    public ReviewAdminService(ReviewRepository reviews) {
        this.reviews = reviews;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="reviews";
return reviews.findAll(matching(search,type,filters,"content","user.fullName","user.email","product.name"),paging).map(CommerceResponse::review);
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="reviews";
return CommerceResponse.review(reviews.findById(id).orElseThrow(() -> missing(type,id)));
    }
    public void delete(long id,String currentEmail) {
        String type="reviews";
reviews.deleteById(id);
    }

}
