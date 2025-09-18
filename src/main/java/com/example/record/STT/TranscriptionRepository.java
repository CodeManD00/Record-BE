// TranscriptionRepository: Transcription 엔티티에 대한 CRUD 및 사용자별 조회 기능을 제공하는 JPA 리포지토리입니다.
// 예: /stt/list 에서 로그인한 사용자의 STT 기록 목록 조회에 사용됩니다.

package com.example.record.STT;

import com.example.record.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    // 특정 사용자(User)의 STT 기록 전체 조회
    List<Transcription> findByUser(User user);
}
