// OcrService: Google Cloud Vision API를 사용하여 이미지에서 텍스트를 추출하는 OCR 서비스 클래스입니다.

package com.example.record.OCR;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class OcrService {

    // 이미지 파일을 입력받아 텍스트를 추출하여 반환
    public String extractTextFromImage(File imageFile) throws IOException {
        // Google Vision API 클라이언트 생성 (try-with-resources로 자동 종료)
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // 이미지 파일을 바이트 배열로 읽고 Vision API용 Image 객체 생성
            ByteString imgBytes = ByteString.readFrom(new FileInputStream(imageFile));
            Image image = Image.newBuilder().setContent(imgBytes).build();

            // 텍스트 감지를 위한 Feature 설정
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            // 이미지와 기능 정보를 담은 요청 생성
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            // Vision API 요청 실행
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));

            // 결과에서 전체 추출 텍스트 가져오기
            String extracted = response.getResponses(0).getFullTextAnnotation().getText();

            return extracted;
        }
    }
}
