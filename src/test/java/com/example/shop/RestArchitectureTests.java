package com.example.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.security.test.context.support.WithMockUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc
class RestArchitectureTests {
    @Autowired RequestMappingHandlerMapping mappings;
    @Autowired MockMvc mvc;
    @Autowired com.example.shop.repository.UserRepository users;
    @Autowired com.example.shop.repository.RoleRepository roles;
    @org.junit.jupiter.api.BeforeEach void account() {
        if (!users.existsByEmail("rest-test@example.test")) {
            var role=roles.findByName("ADMIN");
            if(role==null){role=new com.example.shop.domain.Role();role.setName("ADMIN");role=roles.save(role);}
            var user=new com.example.shop.domain.User();user.setEmail("rest-test@example.test");user.setEnabled(true);user.setRole(role);users.save(user);
        }
    }

    @Test void businessHandlersAreRestOnly() {
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            Class<?> type = handler.getBeanType();
            if (!type.getName().startsWith("com.example.shop.") || type.getSimpleName().equals("SpaController")) return;
            assertThat(type.isAnnotationPresent(RestController.class)).as(type.getName()).isTrue();
            assertThat(mapping.getPatternValues()).allMatch(path -> path.startsWith("/api/v1/") || (type.getSimpleName().equals("ChatApiController") && (path.equals("/api/chat")||path.equals("/api/chat/status"))));
        });
    }

    @Test @WithMockUser(username="rest-test@example.test",roles="ADMIN")
    void retiredMvcRoutesCannotMutateAndApiErrorsAreJson() throws Exception {
        mvc.perform(get("/admin/user/delete/1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/v1/no-such-resource")).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/json"));
        mvc.perform(post("/api/v1/products").with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/v1/auth/verify").param("code","unused")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("text/plain").content("invalid"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }
}
