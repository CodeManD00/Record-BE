package com.example.record.OCR;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class OcrService {

    /**
     * 이미지에서 텍스트 추출
     * - DOCUMENT_TEXT_DETECTION 사용 (티켓/문서에 유리)
     * - 응답/에러 널가드
     */
    public String extractTextFromImage(File imageFile) throws IOException {
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create();
             FileInputStream fis = new FileInputStream(imageFile)) {

            ByteString imgBytes = ByteString.readFrom(fis);
            Image image = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            if (response == null || response.getResponsesCount() == 0) {
                return "";
            }

            AnnotateImageResponse r0 = response.getResponses(0);
            if (r0.hasError()) {
                throw new IOException("Vision API error: " + r0.getError().getMessage());
            }

            return r0.hasFullTextAnnotation()
                    ? r0.getFullTextAnnotation().getText()
                    : "";
        }
    }
}
