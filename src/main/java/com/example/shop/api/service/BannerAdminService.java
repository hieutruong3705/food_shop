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
public class BannerAdminService {
    private final BannerRepository banners;
    public BannerAdminService(BannerRepository banners) {
        this.banners = banners;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="banners";
return banners.findAll(matching(search,type,filters,"name"),paging).map(b -> map("id",b.getId(),"name",b.getName(),"imageUrl",b.getImageUrl(),"link",b.getLink(),"active",b.isActive(),"technologyCatalog",b.isTechnologyCatalog()));
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="banners";
var b=banners.findById(id).orElseThrow(() -> missing(type,id));return map("id",b.getId(),"name",b.getName(),"imageUrl",b.getImageUrl(),"link",b.getLink(),"active",b.isActive(),"technologyCatalog",b.isTechnologyCatalog());
    }
    public Map<String,Object> save(Long id,BannerInput n,String currentEmail) {
        String type="banners";
var b=id==null?new Banner():banners.findById(id).orElseThrow(() -> missing(type,id));b.setTechnologyCatalog(enabled(n.technologyCatalog()));b.setName(n.name().trim());b.setImageUrl(url(n.imageUrl().trim()));b.setLink(url(n.link()==null?"/products":n.link()));b.setActive(enabled(n.active()));return map("id",banners.save(b).getId());
    }
    public void delete(long id,String currentEmail) {
        String type="banners";
banners.deleteById(id);
    }

}
