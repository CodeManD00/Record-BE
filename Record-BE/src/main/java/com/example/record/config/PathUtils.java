package com.example.record.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Record-BE 폴더 기준 경로를 찾는 유틸리티 클래스
 */
public class PathUtils {
    
    /**
     * Record-BE 폴더의 절대 경로를 반환합니다.
     * 현재 작업 디렉토리에서 Record-BE 폴더를 찾습니다.
     * 
     * @return Record-BE 폴더의 Path
     */
    public static Path getRecordBEDir() {
        Path currentDir = Paths.get("").toAbsolutePath().normalize();
        
        // 현재 디렉토리가 Record-BE인 경우
        if (currentDir.getFileName().toString().equals("Record-BE")) {
            return currentDir;
        }
        
        // 상위 디렉토리에서 Record-BE 찾기
        Path parentDir = currentDir.getParent();
        if (parentDir != null) {
            Path recordBEDir = parentDir.resolve("Record-BE");
            if (Files.exists(recordBEDir)) {
                return recordBEDir;
            }
        }
        
        // 현재 디렉토리 하위에 Record-BE가 있는지 확인
        Path subRecordBEDir = currentDir.resolve("Record-BE");
        if (Files.exists(subRecordBEDir)) {
            return subRecordBEDir;
        }
        
        // 찾지 못한 경우 현재 디렉토리 반환
        return currentDir;
    }
}

