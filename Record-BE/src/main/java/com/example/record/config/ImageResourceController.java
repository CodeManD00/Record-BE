package com.example.record.config;

// S3로 마이그레이션 완료 - 로컬 파일 서빙 비활성화
// 모든 이미지는 S3에서 제공되므로 이 컨트롤러는 더 이상 필요 없음
// 
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.FileSystemResource;
// import org.springframework.core.io.Resource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import java.io.File;
// import java.nio.file.Path;

// S3로 마이그레이션 완료 - 로컬 파일 서빙 비활성화
// 모든 이미지는 S3에서 제공되므로 이 컨트롤러는 더 이상 필요 없음
// 
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.FileSystemResource;
// import org.springframework.core.io.Resource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import java.io.File;
// import java.nio.file.Path;
//
// @RestController
// @RequestMapping("/uploads")
public class ImageResourceController {

    // @Value("${app.upload.profile-image-dir:uploads/profile-images}")
    // private String profileImageDir;
    //
    // @Value("${app.upload.generated-image-dir:uploads/generated-images}")
    // private String generatedImageDir;
    //
    // @GetMapping("/profile-images/{filename:.+}")
    // public ResponseEntity<Resource> getProfileImage(@PathVariable("filename") String filename) {
    //     try {
    //         Path baseDir = PathUtils.getRecordBEDir();
    //         Path imagePath = baseDir.resolve(profileImageDir).resolve(filename).normalize();
    //         File file = imagePath.toFile();
    //         
    //         if (!file.exists() || !file.isFile()) {
    //             return ResponseEntity.notFound().build();
    //         }
    //         
    //         Resource resource = new FileSystemResource(file);
    //         return ResponseEntity.ok()
    //                 .contentType(MediaType.IMAGE_JPEG)
    //                 .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
    //                 .body(resource);
    //     } catch (Exception e) {
    //         return ResponseEntity.notFound().build();
    //     }
    // }
    //
    // @GetMapping("/generated-images/{filename:.+}")
    // public ResponseEntity<Resource> getGeneratedImage(@PathVariable("filename") String filename) {
    //     try {
    //         Path baseDir = PathUtils.getRecordBEDir();
    //         Path imagePath = baseDir.resolve(generatedImageDir).resolve(filename).normalize();
    //         File file = imagePath.toFile();
    //         
    //         if (!file.exists() || !file.isFile()) {
    //             return ResponseEntity.notFound().build();
    //         }
    //         
    //         Resource resource = new FileSystemResource(file);
    //         String contentType = filename.toLowerCase().endsWith(".png") 
    //             ? MediaType.IMAGE_PNG_VALUE 
    //             : MediaType.IMAGE_JPEG_VALUE;
    //         
    //         return ResponseEntity.ok()
    //                 .contentType(MediaType.parseMediaType(contentType))
    //                 .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
    //                 .body(resource);
    //     } catch (Exception e) {
    //         return ResponseEntity.notFound().build();
    //     }
    // }
}
