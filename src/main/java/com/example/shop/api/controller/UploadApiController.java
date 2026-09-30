package com.example.shop.api.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/admin/uploads")
public class UploadApiController {
    private final Path directory;
    public UploadApiController(@Value("${shop.upload-dir:./data/uploads}") String directory){this.directory=Path.of(directory).toAbsolutePath().normalize();}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Map<String,String> upload(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        if(file.isEmpty() || file.getSize()>5*1024*1024)throw new IllegalArgumentException("Ảnh phải nhỏ hơn 5 MB");
        byte[] bytes=file.getBytes();String ext;
        if(bytes.length>8 && bytes[0]==(byte)0x89 && bytes[1]==0x50 && bytes[2]==0x4e && bytes[3]==0x47)ext="png";
        else if(bytes.length>3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff)ext="jpg";
        else if(bytes.length>12 && new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))ext="webp";
        else throw new IllegalArgumentException("Chỉ hỗ trợ ảnh PNG, JPEG hoặc WebP");
        String original=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
        String mime=Objects.toString(file.getContentType(),"").toLowerCase(Locale.ROOT);
        boolean extension=ext.equals("jpg")?(original.endsWith(".jpg") || original.endsWith(".jpeg")):original.endsWith("."+ext);
        if(!extension || !mime.equals(ext.equals("jpg")?"image/jpeg":"image/"+ext))throw new IllegalArgumentException("Đuôi tệp và MIME phải khớp định dạng ảnh");
        if(!ext.equals("webp"))try(var input=new java.io.ByteArrayInputStream(bytes)){var image=javax.imageio.ImageIO.read(input);if(image==null || (long)image.getWidth()*image.getHeight()>40000000)throw new IllegalArgumentException("Ảnh không hợp lệ hoặc quá lớn");}
        Files.createDirectories(directory);String name=UUID.randomUUID()+"."+ext;Path target=directory.resolve(name).normalize();if(!target.startsWith(directory))throw new IllegalArgumentException("Đường dẫn không hợp lệ");Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        return Map.of("url","/uploads/"+name);
    }
}
