package com.example.shop.config;
import com.example.shop.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public AccountStatusFilter(UserRepository users){this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())){
            var u=users.findByEmail(auth.getName());
            if(u==null || u.getIsLocked() || !u.isEnabled()){
                SecurityContextHolder.clearContext();var session=request.getSession(false);if(session!=null)session.invalidate();response.setStatus(401);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"Tài khoản không còn hoạt động\"}");return;
            }
            var updated=UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(),auth.getCredentials(),List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().getName())));
            SecurityContextHolder.getContext().setAuthentication(updated);
        }
        chain.doFilter(request,response);
    }
}
