package com.example.shop.api.service;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.time.Duration;
import java.util.Map;
/** Transport for the VNPay querydr call, kept apart so reconciliation is testable without the gateway. */
@Component
public class VnpayQueryClient {
    private final RestClient client;private final String url;
    @org.springframework.beans.factory.annotation.Autowired
    public VnpayQueryClient(RestClient.Builder builder,@Value("${shop.vnpay.query-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}") String url){
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));factory.setReadTimeout(Duration.ofSeconds(20));
        this.client=builder.requestFactory(factory).build();this.url=url;
    }
    VnpayQueryClient(RestClient client,String url){this.client=client;this.url=url;}
    /** Returns the gateway response, or null when it cannot be reached. Bodies are never logged: they carry merchant data. */
    public JsonNode post(Map<String,String> body){
        try{return client.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);}
        catch(RestClientException exception){return null;}
    }
}
