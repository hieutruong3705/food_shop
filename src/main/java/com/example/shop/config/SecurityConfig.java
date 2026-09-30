package com.example.shop.config;
import com.example.shop.service.CustomUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean public DaoAuthenticationProvider authProvider(PasswordEncoder encoder,CustomUserDetailsService users){
        var provider=new DaoAuthenticationProvider();provider.setUserDetailsService(users);provider.setPasswordEncoder(encoder);return provider;
    }
    @Bean public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider){return new ProviderManager(provider);}
    @Bean public SecurityFilterChain staticFilterChain(HttpSecurity http)throws Exception{
        http.authorizeHttpRequests(a -> a.requestMatchers(org.springframework.http.HttpMethod.GET,"/**").permitAll().requestMatchers(org.springframework.http.HttpMethod.HEAD,"/**").permitAll().anyRequest().denyAll());
        return http.build();
    }
}
