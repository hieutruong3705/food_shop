package com.example.shop.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiChatServiceTests {
    @Test void sendsKeyOnlyInHeaderAndMapsHistoryAfterTheSystemPrompt(){
        var builder=RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        var server=MockRestServiceServer.bindTo(builder).build();
        var service=new AiChatService(builder.build(),"secret-test-key","vendor/model-test",true);
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer secret-test-key"))
            .andExpect(jsonPath("$.model").value("vendor/model-test"))
            .andExpect(jsonPath("$.messages[0].role").value("system"))
            .andExpect(jsonPath("$.messages[1].role").value("assistant"))
            .andExpect(jsonPath("$.messages[2].content").value("Laptop"))
            .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"Bạn cần ngân sách bao nhiêu?\"}}]}",MediaType.APPLICATION_JSON));
        assertThat(service.reply("Laptop",List.of(Map.of("role","assistant","text","Xin chào")))).isEqualTo("Bạn cần ngân sách bao nhiêu?");server.verify();
    }
    @Test void quotaErrorIsSanitizedAndDoesNotReturnProviderBody(){
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new AiChatService(builder.build(),"secret-test-key","vendor/model-test",true);
        server.expect(requestTo("/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret-test-key private provider detail"));
        assertThatThrownBy(()->service.reply("hi",List.of())).isInstanceOfSatisfying(ResponseStatusException.class,e->{
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);assertThat(e.getReason()).doesNotContain("secret-test-key","private provider");
        });server.verify();
    }
    @Test void emptyOrBlockedReplyIsNotReportedAsSuccessful(){
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new AiChatService(builder.build(),"secret-test-key","vendor/model-test",true);
        server.expect(requestTo("/chat/completions")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->service.reply("hi",List.of())).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));server.verify();
    }
    @Test void mockIsSelectedWhenDisabledOrKeyMissing(){
        assertThat(new AiChatService(RestClient.create(),"","vendor/model-test",true).configured()).isFalse();
        assertThat(new AiChatService(RestClient.create(),"secret-test-key","vendor/model-test",false).configured()).isFalse();
    }
}
