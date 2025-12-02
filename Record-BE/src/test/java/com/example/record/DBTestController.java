package com.example.record;


import com.example.record.user.User;
import com.example.record.user.Friendship;
import com.example.record.user.FriendshipService;
import com.example.record.user.dto.FriendshipResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DBTestController {

    private final FriendshipService friendshipService;

    @GetMapping("/protected")
    public String protectedEndpoint(@AuthenticationPrincipal User user) {
        return "안녕하세요, " + user.getNickname() + "님! 인증이 완료되었습니다.";
    }

    /**
     * 특정 사용자의 친구 리스트를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 친구 리스트
     */
    @GetMapping("/users/{userId}/friends")
    public List<FriendshipResponse> getUserFriends(@PathVariable("userId") String userId) {
        List<Friendship> friends = friendshipService.getFriends(userId);
        return friends.stream()
                .map(friendship -> {
                    FriendshipResponse response = FriendshipResponse.from(friendship);
                    // 현재 사용자가 아닌 친구의 정보만 포함하도록 변환
                    if (friendship.getUser().getId().equals(userId)) {
                        // 현재 사용자가 요청을 보낸 경우 -> friend가 친구
                        response.setUserId(friendship.getFriend().getId());
                        response.setUserNickname(friendship.getFriend().getNickname());
                        response.setUserProfileImage(friendship.getFriend().getProfileImage());
                        response.setFriendId(null);
                        response.setFriendNickname(null);
                        response.setFriendProfileImage(null);
                    } else {
                        // 현재 사용자가 요청을 받은 경우 -> user가 친구
                        response.setFriendId(null);
                        response.setFriendNickname(null);
                        response.setFriendProfileImage(null);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }
}
