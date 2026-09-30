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

class GeminiChatServiceTests {
    @Test void sendsKeyOnlyInHeaderAndMapsHistoryWithoutThoughts(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        var server=MockRestServiceServer.bindTo(builder).build();
        var service=new GeminiChatService(builder.build(),"secret-test-key","gemini-test",true);
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key","secret-test-key"))
            .andExpect(jsonPath("$.contents[0].role").value("model"))
            .andExpect(jsonPath("$.contents[1].parts[0].text").value("Laptop"))
            .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"hidden\"},{\"text\":\"Bạn cần ngân sách bao nhiêu?\"}]}}]}",MediaType.APPLICATION_JSON));
        assertThat(service.reply("Laptop",List.of(Map.of("role","assistant","text","Xin chào")))).isEqualTo("Bạn cần ngân sách bao nhiêu?");server.verify();
    }
    @Test void quotaErrorIsSanitizedAndDoesNotReturnProviderBody(){
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new GeminiChatService(builder.build(),"secret-test-key","gemini-test",true);
        server.expect(requestTo("/models/gemini-test:generateContent")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret-test-key private provider detail"));
        assertThatThrownBy(()->service.reply("hi",List.of())).isInstanceOfSatisfying(ResponseStatusException.class,e->{
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);assertThat(e.getReason()).doesNotContain("secret-test-key","private provider");
        });server.verify();
    }
    @Test void emptyOrBlockedReplyIsNotReportedAsSuccessful(){
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new GeminiChatService(builder.build(),"secret-test-key","gemini-test",true);
        server.expect(requestTo("/models/gemini-test:generateContent")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->service.reply("hi",List.of())).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));server.verify();
    }
    @Test void mockIsSelectedWhenDisabledOrKeyMissing(){
        assertThat(new GeminiChatService(RestClient.create(),"","gemini-test",true).configured()).isFalse();
        assertThat(new GeminiChatService(RestClient.create(),"secret-test-key","gemini-test",false).configured()).isFalse();
    }
}
