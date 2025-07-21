//사용자별 결과 조회 API (/stt/list)

package com.example.record.STT;

import com.example.record.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
    List<Transcription> findByUser(User user);
}
