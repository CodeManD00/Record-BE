package com.example.record.STT;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SttService {

    @Value("${stt.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    /** 업로드 파일을 LINEAR16 16kHz mono WAV로 변환 */
    private Path convertToWav(Path inputFile) throws Exception {
        Path outputFile = Files.createTempFile("stt_converted_", ".wav");

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", inputFile.toString(),
                "-ar", "16000",
                "-ac", "1",
                "-c:a", "pcm_s16le",
                outputFile.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder log = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) log.append(line).append('\n');
        }

        int exit = p.waitFor();
        if (exit != 0) {
            try { Files.deleteIfExists(outputFile); } catch (Exception ignore) {}
            throw new RuntimeException("ffmpeg 변환 실패(exit=" + exit + ")\n" + log);
        }
        return outputFile;
    }

    /** 변환된 WAV를 Google STT에 전달하여 텍스트로 변환 */
    public String transcribeLocalFile(String filePath) throws Exception {
        Path input = Path.of(filePath);
        Path wav = convertToWav(input);

        try (SpeechClient speechClient = SpeechClient.create()) {
            byte[] data = Files.readAllBytes(wav);
            ByteString audioBytes = ByteString.copyFrom(data);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
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
        } finally {
            try { Files.deleteIfExists(wav); } catch (Exception ignore) {}
        }
    }
}
