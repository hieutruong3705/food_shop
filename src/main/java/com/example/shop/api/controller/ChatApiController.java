package com.example.shop.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.example.shop.api.service.AiChatService;

/** The AI provider is called server-side; the existing mock remains available when no key is configured. */
@RestController
public class ChatApiController {
    private final AiChatService ai;
    public ChatApiController(AiChatService ai){this.ai=ai;}
    /** Longest history sent to the provider in total; the client drops the oldest turns to stay under it. */
    static final int MAX_HISTORY_CHARS=4000;
    public record Turn(@NotBlank @Pattern(regexp="user|assistant") String role,@NotBlank @Size(max=1000,message="Mỗi lượt hội thoại tối đa 1.000 ký tự") String text){}
    public record Message(@NotBlank @Size(max=1000) String message,@Size(max=8) List<@NotNull @Valid Turn> history){}
    @GetMapping({"/api/chat/status","/api/v1/chat/status"}) public Map<String,String> status(){return Map.of("provider",ai.configured()?"ai":"mock");}
    /** The 12 requests a minute per client address are enforced by RateLimitInterceptor. */
    @PostMapping({"/api/chat","/api/v1/chat"}) public Map<String,String> chat(@Valid @RequestBody Message input){
        int historyChars=input.history()==null?0:input.history().stream().mapToInt(t->t.text().length()).sum();
        if(historyChars>MAX_HISTORY_CHARS)throw new IllegalArgumentException("Lịch sử hội thoại quá dài (tối đa 4.000 ký tự). Hãy bắt đầu cuộc trò chuyện mới.");
        if(ai.configured()){
            var history=input.history()==null?List.<Map<String,String>>of():input.history().stream().map(t->Map.of("role",t.role(),"text",t.text())).toList();
            return Map.of("reply",ai.reply(input.message(),history),"provider","ai");
        }
        String value=java.text.Normalizer.normalize(input.message(),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        String reply;
        if(value.contains("so sanh"))reply="Bạn gửi tên hai sản phẩm cần so sánh nhé. Tôi sẽ gợi ý các tiêu chí: hiệu năng, màn hình, pin và bảo hành. Bạn có thể xem thông số chính xác ở trang chi tiết từng sản phẩm.";
        else if(value.contains("20 trieu"))reply="Bạn có thể vào danh mục sản phẩm, đặt Giá đến là 20.000.000 đồng và bật Sẵn hàng. Bạn muốn tìm laptop hay điện thoại trong mức giá này?";
        else if(value.contains("laptop"))reply="Bạn dùng laptop để học tập, làm văn phòng, thiết kế hay chơi game? Hãy cho tôi biết ngân sách và nhu cầu mang máy di chuyển để chọn tiêu chí phù hợp.";
        else if(value.contains("dien thoai"))reply="Bạn ưu tiên camera, pin, chơi game hay thiết kế? Cho tôi biết ngân sách; bạn có thể lọc hãng và thông số trong danh mục Điện thoại.";
        else reply="Bạn muốn tìm sản phẩm, so sánh thiết bị hay tư vấn theo nhu cầu? Hãy cho tôi biết loại thiết bị và ngân sách. Đây là trợ lý phản hồi mẫu; giá và tồn kho chính xác được hiển thị trên trang sản phẩm.";
        return Map.of("reply",reply,"provider","mock");
    }
}
