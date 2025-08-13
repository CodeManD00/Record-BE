package com.example.record.STT;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SttService {

    // 로컬 음성 파일을 읽어 Google STT API로 텍스트 변환
    public String transcribeLocalFile(String filePath) throws Exception {
        try (SpeechClient speechClient = SpeechClient.create()) {
            Path path = Path.of(filePath);
            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            // 파일 확장자로 간단 분기 (정석은 ffmpeg로 LINEAR16 통일)
            String lower = filePath.toLowerCase();
            RecognitionConfig.AudioEncoding enc = RecognitionConfig.AudioEncoding.LINEAR16;
            if (lower.endsWith(".mp3")) enc = RecognitionConfig.AudioEncoding.MP3;
            else if (lower.endsWith(".flac")) enc = RecognitionConfig.AudioEncoding.FLAC;
            else if (lower.endsWith(".ogg")) enc = RecognitionConfig.AudioEncoding.OGG_OPUS;
            // 그 외는 기본 LINEAR16

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(enc)
                    .setLanguageCode("ko-KR")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);

            StringBuilder result = new StringBuilder();
            for (SpeechRecognitionResult res : response.getResultsList()) {
                if (res.getAlternativesCount() > 0) {
                    result.append(res.getAlternatives(0).getTranscript()).append(" ");
                }
            }
            return result.toString().trim();
        }
    }
}
