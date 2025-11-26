import React, { useState, useEffect, useCallback } from 'react';
import { useFocusEffect } from '@react-navigation/native';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Image,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAtom } from 'jotai';
import { 
  friendsAtom, 
  removeFriendAtom, 
  receivedFriendRequestsAtom, 
  respondToFriendRequestAtom,
  fetchReceivedRequestsAtom,
  fetchFriendsAtom,
} from '../../atoms';
import { Friend, FriendRequest } from '../../types/friend';
import { Colors, Typography, Spacing, BorderRadius, Shadows, ComponentStyles, Layout } from '../../styles/designSystem';
import ModalHeader from '../../components/ModalHeader';
import { resolveImageUrl } from '../../utils/resolveImageUrl';

interface FriendsListPageProps {
  navigation: any;
}

const FriendsListPage: React.FC<FriendsListPageProps> = ({ navigation }) => {
  const [friends] = useAtom(friendsAtom);
  const [, removeFriend] = useAtom(removeFriendAtom);
  const [friendRequests] = useAtom(receivedFriendRequestsAtom);
  const [, respondToRequest] = useAtom(respondToFriendRequestAtom);
  const [, fetchReceivedRequests] = useAtom(fetchReceivedRequestsAtom);
  const [, fetchFriends] = useAtom(fetchFriendsAtom);

  // 페이지 로드 시 데이터 가져오기
  useEffect(() => {
    fetchReceivedRequests(true);
    fetchFriends(true);
  }, []);

  // 화면 포커스 시 데이터 새로고침
  useFocusEffect(
    useCallback(() => {
      fetchReceivedRequests(true);
      fetchFriends(true);
    }, [fetchReceivedRequests, fetchFriends])
  );

  const friendRequestsCount = friendRequests.length;
  const friendsCount = friends.length;

  // 친구 삭제
  const handleDeleteFriend = async (friend: Friend) => {
    Alert.alert('친구 삭제', `${friend.nickname}님을 친구 목록에서 삭제하시겠어요?`, [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: async () => {
          try {
            const result = await removeFriend(friend);
            if (result.success) {
              Alert.alert('완료', '친구를 삭제했습니다.');
              await fetchFriends(true);
            } else {
              Alert.alert('오류', result.error?.message || '친구 삭제 중 오류가 발생했습니다.');
            }
          } catch (error) {
            Alert.alert('오류', '친구 삭제 중 오류가 발생했습니다.');
          }
        },
      },
    ]);
  };

  // 친구 요청 거절
  const handleRejectRequest = async (request: FriendRequest) => {
    Alert.alert(
      '친구 요청 거절',
      `${request.nickname}님의 친구 요청을 거절하시겠어요?`,
      [
        { text: '취소', style: 'cancel' },
        {
          text: '거절',
          style: 'destructive',
          onPress: async () => {
            try {
              const result = await respondToRequest({ requestId: request.id, accept: false });
              if (result.success) {
                Alert.alert('완료', '친구 요청을 거절했습니다.');
                fetchReceivedRequests(true);
              } else {
                Alert.alert('오류', result.error?.message || '친구 요청 거절 중 오류가 발생했습니다.');
              }
            } catch (error) {
              Alert.alert('오류', '친구 요청 거절 중 오류가 발생했습니다.');
            }
          },
        },
      ]
    );
  };

  // 친구 프로필로 이동 (모달 닫기 → 풀스크린 열기)
  const handleNavigateToFriendProfile = (friendOrRequest: Friend | FriendRequest) => {
    // FriendRequest를 Friend로 변환
    const friend: Friend = {
      id: friendOrRequest.fromUserId || friendOrRequest.id,
      user_id: friendOrRequest.user_id,
      nickname: friendOrRequest.nickname,
      profileImage: friendOrRequest.profileImage,
      createdAt: friendOrRequest.createdAt,
      updatedAt: friendOrRequest.updatedAt,
    };
    
    // 먼저 현재 모달을 닫기
    navigation.goBack();
    
    // 모달 닫기 애니메이션이 완료된 후 풀스크린 열기
    setTimeout(() => {
      navigation.navigate('FriendProfile', { friend });
    }, 300); // 모달 닫기 애니메이션 시간 고려
  };

  // 친구 요청 수락
  const handleAcceptRequest = async (request: FriendRequest) => {
    Alert.alert(
      '친구 요청 수락',
      `${request.nickname}님의 친구 요청을 수락하시겠어요?`,
      [
        { text: '취소', style: 'cancel' },
        {
          text: '수락',
          onPress: async () => {
            try {
              const result = await respondToRequest({ requestId: request.id, accept: true });
              if (result.success) {
                // 친구 목록과 요청 목록 강제 새로고침
                await fetchReceivedRequests(true);
                await fetchFriends(true);
                Alert.alert('성공', `${request.nickname}님과 친구가 되었습니다! 🎉`);
              } else {
                Alert.alert('오류', result.error?.message || '친구 요청 수락 중 오류가 발생했습니다.');
              }
            } catch (error) {
              Alert.alert('오류', '친구 요청 수락 중 오류가 발생했습니다.');
            }
          },
        },
      ]
    );
  };


  return (
    <SafeAreaView style={styles.container} edges={['left', 'right']}>
      <ModalHeader
        title="친구"
        onBack={() => navigation.goBack()}
        rightContent={
            <TouchableOpacity
              style={styles.iconButton}
              onPress={() => navigation.navigate('AddFriend')}
            >
              <Image
                source={require('../../assets/person_add.png')}
                style={styles.iconImage}
              />
            </TouchableOpacity>
        }
      />

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {/* 친구 요청 섹션 */}
        <View style={styles.friendsSection}>
          <View style={styles.friendsSectionHeader}>
            <Text style={styles.friendsSectionTitle}>
              친구 요청 ({friendRequestsCount})
            </Text>

            <TouchableOpacity
              style={styles.sentFriendButton}
              onPress={() => navigation.navigate('SentRequests')}
            >
              <Text style={styles.sentFriendText}>보낸 요청</Text>
            </TouchableOpacity>
          </View>

          {friendRequests.map(request => {
            const requestProfileImageUrl = resolveImageUrl(request.profileImage);
            return (
              <View key={request.id} style={styles.friendItem}>
                {/* 프로필 클릭 가능 */}
                <TouchableOpacity
                  style={styles.friendInfo}
                  onPress={() => handleNavigateToFriendProfile(request)}
                >
                  {requestProfileImageUrl ? (
                    <Image
                      source={{ uri: requestProfileImageUrl }}
                      style={styles.friendAvatar}
                    />
                  ) : (
                    <View style={[styles.friendAvatar, styles.defaultAvatar]}>
                      <Text style={styles.defaultAvatarText}>👤</Text>
                    </View>
                  )}
                  <View style={styles.friendDetails}>
                    <Text style={styles.friendName}>{request.nickname}</Text>
                    <Text style={styles.friendUsername}>{request.user_id}</Text>
                  </View>
                </TouchableOpacity>

                {/* 수락 / 거절 버튼 */}
                <View style={styles.requestButtons}>
                  <TouchableOpacity
                    style={[styles.requestButton, styles.acceptButton]}
                    onPress={() => handleAcceptRequest(request)}
                  >
                    <Text style={styles.requestButtonText}>수락</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.requestButton, styles.rejectButton]}
                    onPress={() => handleRejectRequest(request)}
                  >
                    <Text style={styles.requestButtonText}>거절</Text>
                  </TouchableOpacity>
                </View>
              </View>
            );
          })}
        </View>

        {/* 친구 목록 섹션 */}
        <View style={styles.friendsSection}>
          <View style={styles.friendsSectionHeader}>
            <Text style={styles.friendsSectionTitle}>
              내 친구들 ({friendsCount})
            </Text>
            <View style={styles.placeholder} />
          </View>
          {friends.map(friend => {
            const profileImageUrl = resolveImageUrl(friend.profileImage);
            return (
              <View key={friend.id} style={styles.friendItem}>
                <TouchableOpacity
                  style={styles.friendInfo}
                  onPress={() => handleNavigateToFriendProfile(friend)}
                >
                  {profileImageUrl ? (
                    <Image
                      source={{ uri: profileImageUrl }}
                      style={styles.friendAvatar}
                    />
                  ) : (
                    <View style={[styles.friendAvatar, styles.defaultAvatar]}>
                      <Text style={styles.defaultAvatarText}>👤</Text>
                    </View>
                  )}
                  <View style={styles.friendDetails}>
                    <Text style={styles.friendName}>{friend.nickname}</Text>
                    <Text style={styles.friendUsername}>{friend.user_id}</Text>
                  </View>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.menuButtonLeft}
                  onPress={() => handleDeleteFriend(friend)}
                >
                  <Text style={styles.menuIcon}>⋯</Text>
                </TouchableOpacity>
              </View>
            );
          })}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.secondarySystemBackground },
  iconButton: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.round,
    backgroundColor: `${Colors.secondarySystemBackground}CC`,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconImage: {
    width: 24,
    height: 24,
    resizeMode: 'contain',
  },
  content: { flex: 1, backgroundColor: Colors.secondarySystemBackground },
  placeholder: {
    width: 44,
    height: 44,
  },

  friendsSection: {
    ...ComponentStyles.card,
    marginHorizontal: Spacing.lg,
    marginTop: Spacing.lg,
    overflow: 'hidden',
    padding: 0,
  },
  friendsSectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F3F4',
  },
  friendsSectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2C3E50',
  },

  sentFriendButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
  },
  sentFriendText: {
    color: '#0b0b0bff',
    fontSize: 14,
    fontWeight: '600',
  },

  friendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F3F4',
  },
  friendInfo: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  friendAvatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    marginRight: 15,
    backgroundColor: '#E9ECEF',
  },
  defaultAvatar: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#E9ECEF',
  },
  defaultAvatarText: {
    fontSize: 24,
  },
  friendDetails: { flex: 1 },
  friendName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#2C3E50',
    marginBottom: 2,
  },
  friendUsername: { fontSize: 14, color: '#6C757D' },

  menuButtonLeft: { marginRight: 10 },
  menuIcon: { fontSize: 20, color: '#ADB5BD', fontWeight: 'bold' },

  requestButtons: { flexDirection: 'row' },
  requestButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
    marginLeft: 8,
  },
  acceptButton: { backgroundColor: '#28A745' },
  rejectButton: { backgroundColor: '#DC3545' },
  requestButtonText: { color: '#FFFFFF', fontWeight: '600' },
});

export default FriendsListPage;