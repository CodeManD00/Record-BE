
package com.example.record.STTorText.stt;

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

    /**
     * 모든 파일을 m4a 형식으로 변환 (Whisper API 호환성 보장)
     * 파일 형식 문제를 방지하기 위해 모든 파일을 ffmpeg로 변환
     */
    public byte[] maybeReencodeToM4a(byte[] inputBytes, String originalFilename) throws Exception {
        Path src = null, out = null;
        try {
            // 임시 입력 파일 생성
            String srcSuffix = ".tmp";
            if (originalFilename != null && originalFilename.contains(".")) {
                srcSuffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            src = Files.createTempFile("whisper_src_", srcSuffix);
            Files.write(src, inputBytes);

            // 출력 파일 (항상 .m4a)
            out = Files.createTempFile("whisper_enc_", ".m4a");
            
            System.out.println("=== ffmpeg 변환 시작 ===");
            System.out.println("입력 파일: " + src);
            System.out.println("출력 파일: " + out);
            
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-y",
                    "-i", src.toString(),
                    "-ac", "1",           // 모노
                    "-ar", "16000",       // 16kHz 샘플레이트
                    "-c:a", "aac",        // AAC 코덱
                    "-b:a", "64k",        // 64kbps 비트레이트
                    "-f", "mp4",          // mp4 컨테이너 (m4a는 mp4의 오디오 전용)
                    out.toString()
            ).redirectErrorStream(true);

            Process p = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exit = p.waitFor();
            
            if (exit != 0) {
                System.err.println("ffmpeg 출력: " + output.toString());
                throw new RuntimeException("ffmpeg 재인코딩 실패(exit=" + exit + ")");
            }

            byte[] result = Files.readAllBytes(out);
            System.out.println("변환 완료: " + result.length + " bytes");
            return result;
        } catch (Exception e) {
            System.err.println("ffmpeg 변환 오류: " + e.getMessage());
            // 변환 실패 시 원본 반환 (25MB 이하인 경우만)
            if (inputBytes.length <= 24 * 1024 * 1024) {
                System.out.println("변환 실패, 원본 파일 사용");
                return inputBytes;
            }
            throw e;
        } finally {
            try { if (src != null) Files.deleteIfExists(src); } catch (Exception ignore) {}
            try { if (out != null) Files.deleteIfExists(out); } catch (Exception ignore) {}
        }
    }
}
