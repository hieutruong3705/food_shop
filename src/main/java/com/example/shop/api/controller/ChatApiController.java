package com.example.shop.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.example.shop.api.service.GeminiChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Gemini is called server-side; the existing mock remains available when no key is configured. */
@RestController
public class ChatApiController {
    private final GeminiChatService gemini;
    public ChatApiController(GeminiChatService gemini){this.gemini=gemini;}
    public record Turn(@NotBlank @Pattern(regexp="user|assistant") String role,@NotBlank @Size(max=6000) String text){}
    public record Message(@NotBlank @Size(max=1000) String message,@Size(max=8) List<@NotNull @Valid Turn> history){}
    @GetMapping({"/api/chat/status","/api/v1/chat/status"}) public Map<String,String> status(){return Map.of("provider",gemini.configured()?"gemini":"mock");}
    @PostMapping({"/api/chat","/api/v1/chat"}) public Map<String,String> chat(@Valid @RequestBody Message input,HttpSession session){
        if(gemini.configured()){
            synchronized(session){
                long now=System.currentTimeMillis();
                var start=(Long)session.getAttribute("chat.window");
                int count=Optional.ofNullable((Integer)session.getAttribute("chat.count")).orElse(0);
                if(start==null||now-start>=60000){session.setAttribute("chat.window",now);count=0;}
                if(count>=12)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn gửi quá nhanh. Vui lòng chờ một phút rồi thử lại.");
                session.setAttribute("chat.count",count+1);
            }
            var history=input.history()==null?List.<Map<String,String>>of():input.history().stream().map(t->Map.of("role",t.role(),"text",t.text())).toList();
            return Map.of("reply",gemini.reply(input.message(),history),"provider","gemini");
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
