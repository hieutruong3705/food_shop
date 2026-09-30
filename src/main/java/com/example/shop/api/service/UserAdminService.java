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
public class UserAdminService {
    private final UserRepository users;
    private final RoleRepository roles;
    public UserAdminService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }
    @Transactional(readOnly = true)
    public Page<?> list(int page,int size,String search,Map<String,String> filters) {
        var paging=page(page,size,search);
        String type="users";
return users.findAll(matching(search,type,filters,"fullName","email"),paging).map(u -> map("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole().getName(),"enabled",u.isEnabled(),"locked",u.getIsLocked()));
    }
    @Transactional(readOnly = true)
    public Object detail(long id) {
        String type="users";
var u=users.findById(id).orElseThrow(() -> missing(type,id));return map("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole().getName(),"enabled",u.isEnabled(),"locked",u.getIsLocked());
    }
    public Map<String,Object> save(Long id,UserInput n,String currentEmail) {
        String type="users";

                if(id==null)throw new IllegalArgumentException("Người dùng tạo tài khoản qua đăng ký");
                var u=users.findById(id).orElseThrow(() -> missing(type,id));if(u.getEmail().equals(currentEmail))throw new IllegalArgumentException("Không đổi quyền hoặc khóa chính tài khoản đang dùng");
                String role=n.role().trim();if(!Set.of("USER","ADMIN").contains(role))throw new IllegalArgumentException("Quyền không hợp lệ");
                u.setRole(roles.findByName(role));u.setIsLocked(Boolean.TRUE.equals(n.locked()));u.setEnabled(Boolean.TRUE.equals(n.enabled()));return map("id",u.getId());
    }
    public void delete(long id,String currentEmail) {
        String type="users";
var u=users.findById(id).orElseThrow(() -> missing(type,id));if(u.getEmail().equals(currentEmail))throw new IllegalArgumentException("Không thể khóa chính mình");if(u.getOrders()!=null && !u.getOrders().isEmpty())throw new IllegalArgumentException("Không xóa tài khoản đã có đơn hàng; hãy dùng chức năng khóa");users.delete(u);
    }

}
