// SttService: 로컬 음성 파일을 Google Cloud STT(Google Speech-to-Text) API로 변환하여 텍스트를 반환하는 서비스입니다.

package com.example.record.STT;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SttService {

    // 로컬 음성 파일을 읽어 Google STT API를 통해 텍스트로 변환
    public String transcribeLocalFile(String filePath) throws Exception {
        try (SpeechClient speechClient = SpeechClient.create()) {
            // 파일 경로에서 바이트 데이터 로딩
            Path path = Path.of(filePath);
            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            // 인식 설정: LINEAR16 포맷, 한국어
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("ko-KR")
                    .build();

            // 오디오 데이터 설정
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // STT 요청 수행
            RecognizeResponse response = speechClient.recognize(config, audio);

            // 결과 텍스트 추출
            StringBuilder result = new StringBuilder();
            for (SpeechRecognitionResult res : response.getResultsList()) {
                result.append(res.getAlternatives(0).getTranscript()).append(" ");
            }

            return result.toString().trim();
        }
    }
}
