package com.example.record.user.dto;

import com.example.record.user.Friendship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Long id;
    private String userId;  // 요청을 보낸 사용자 ID
    private String userNickname;  // 요청을 보낸 사용자 닉네임
    private String userProfileImage;  // 요청을 보낸 사용자 프로필 이미지
    private String friendId;  // 요청을 받은 사용자 ID
    private String friendNickname;  // 요청을 받은 사용자 닉네임
    private String friendProfileImage;  // 요청을 받은 사용자 프로필 이미지
    private String status;  // PENDING, ACCEPTED, REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FriendshipResponse from(Friendship friendship) {
        return FriendshipResponse.builder()
                .id(friendship.getId())
                .userId(friendship.getUser().getId())
                .userNickname(friendship.getUser().getNickname())
                .userProfileImage(friendship.getUser().getProfileImage())
                .friendId(friendship.getFriend().getId())
                .friendNickname(friendship.getFriend().getNickname())
                .friendProfileImage(friendship.getFriend().getProfileImage())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(friendship.getUpdatedAt())
                .build();
    }
}

