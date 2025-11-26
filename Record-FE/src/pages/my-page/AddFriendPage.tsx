import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  Colors,
  Typography,
  Spacing,
  BorderRadius,
  Shadows,
  ComponentStyles,
  Layout,
} from '../../styles/designSystem';
import { useAtom } from 'jotai';
import { 
  friendsAtom, 
  friendSearchResultsAtom, 
  searchFriendsAtom, 
  sendFriendRequestAtom,
  sentFriendRequestsAtom,
  fetchSentRequestsAtom,
} from '../../atoms';
import { Friend } from '../../types/friend';
import ModalHeader from '../../components/ModalHeader';
import { useUserProfileData } from '../../hooks/useApiData';
import { resolveImageUrl } from '../../utils/resolveImageUrl';

const AddFriendPage: React.FC<{ navigation: any }> = ({ navigation }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults] = useAtom(friendSearchResultsAtom);
  const [, searchFriends] = useAtom(searchFriendsAtom);
  const [, sendFriendRequest] = useAtom(sendFriendRequestAtom);
  const [sentRequests] = useAtom(sentFriendRequestsAtom);
  const [, fetchSentRequests] = useAtom(fetchSentRequestsAtom);
  const [friends] = useAtom(friendsAtom);
  const { data: userProfile } = useUserProfileData({ fetchOnMount: true });

  // 검색어 변경 시 API 호출
  useEffect(() => {
    if (searchQuery.trim()) {
      searchFriends(searchQuery);
    }
  }, [searchQuery]);

  // 페이지 로드 시 보낸 요청 목록 가져오기
  useEffect(() => {
    fetchSentRequests(true);
  }, []);

  const handleSendFriendRequest = async (user: Friend) => {
    if (!userProfile) {
      Alert.alert('오류', '사용자 정보를 가져올 수 없습니다.');
      return;
    }

    // 이미 요청을 보냈는지 확인
    const alreadySent = sentRequests.some(req => req.toUserId === user.id);
    if (alreadySent) {
      Alert.alert('알림', '이미 친구 요청을 보냈습니다.');
      return;
    }

    try {
      if (__DEV__) {
        console.log('📤 친구 요청 전송 준비:', {
          user,
          'user.id': user.id,
          'user.user_id': user.user_id,
          'user.nickname': user.nickname,
        });
      }

      // 백엔드 curl 테스트에서 targetId: "9rmmy" (user_id 값)로 성공
      // 백엔드는 user_id를 targetId로 받는 것으로 보임
      // user.id는 내부 ID이고, user.user_id가 실제 사용자 ID
      const targetUserId = user.user_id || user.id;

      if (__DEV__) {
        console.log('📤 친구 요청 전송 - targetUserId 결정:', {
          'user.id': user.id,
          'user.user_id': user.user_id,
          '최종 targetUserId': targetUserId,
        });
      }

      const result = await sendFriendRequest({
        toUserId: targetUserId, // user_id를 targetId로 전송
        nickname: user.nickname,
        user_id: user.user_id,
      });

      if (result.success) {
        Alert.alert('완료', '친구 요청을 보냈습니다.');
        fetchSentRequests(true);
      } else {
        Alert.alert('오류', result.error?.message || '친구 요청 전송에 실패했습니다.');
      }
    } catch (error) {
      Alert.alert('오류', '친구 요청 전송 중 문제가 발생했습니다.');
    }
  };

  // 친구 프로필로 이동하는 함수 (모달 닫기 → 풀스크린 열기)
  const navigateToFriendProfile = (friend: Friend) => {
    // 먼저 현재 모달을 닫기
    navigation.goBack();

    // 모달 닫기 애니메이션이 완료된 후 풀스크린 열기
    setTimeout(() => {
      navigation.navigate('FriendProfile', { friend });
    }, 300); // 모달 닫기 애니메이션 시간 고려
  };

  return (
    <SafeAreaView style={styles.container} edges={['left', 'right']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backButtonText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>친구 추가</Text>
        <View style={styles.placeholder} />
      </View>

      {/* 검색창 */}
      <View style={styles.searchContainer}>
        <View style={styles.searchBox}>
          <Text style={styles.searchIcon}>🔍</Text>
          <TextInput
            style={styles.searchInput}
            placeholder="사용자 검색"
            placeholderTextColor="#8E8E93"
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
        </View>
      </View>

      {/* 검색 결과 */}
      <ScrollView style={styles.content}>
        {/* 항상 표시되는 내 프로필 */}

        {/* 검색 결과 */}
        {searchResults.map(user => {
          const alreadySent = sentRequests.some(req => req.toUserId === user.id);
          const isFriend = friends.some(f => f.id === user.id);
          const userProfileImageUrl = resolveImageUrl(user.profileImage);
          
          return (
            <View key={user.id} style={styles.userItem}>
              <View style={styles.userInfo}>
                {userProfileImageUrl ? (
                  <Image
                    source={{ uri: userProfileImageUrl }}
                    style={styles.avatar}
                  />
                ) : (
                  <View style={styles.avatar}>
                    <Text style={styles.avatarText}>
                      {user.nickname.charAt(0)}
                    </Text>
                  </View>
                )}
                <View style={styles.userDetails}>
                  <Text style={styles.userName}>{user.nickname}</Text>
                  <Text style={styles.userHandle}>{user.user_id}</Text>
                </View>
              </View>

              {!isFriend && (
                <TouchableOpacity
                  style={[
                    styles.addButton,
                    alreadySent && styles.sentButton,
                  ]}
                  onPress={() => handleSendFriendRequest(user)}
                  disabled={alreadySent}
                >
                  <Text
                    style={[
                      styles.addButtonText,
                      alreadySent && styles.sentButtonText,
                    ]}
                  >
                    {alreadySent ? '보냈음' : '추가'}
                  </Text>
                </TouchableOpacity>
              )}
            </View>
          );
        })}

        {/* 기존 친구들 섹션 */}
        {!searchQuery && friends.length > 0 && (
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>
              내 친구들 ({friends.length})
            </Text>
          </View>
        )}
        {!searchQuery && friends.map(friend => {
          const friendProfileImageUrl = resolveImageUrl(friend.profileImage);
          return (
            <TouchableOpacity
              key={friend.id}
              style={styles.userItem}
              onPress={() => navigateToFriendProfile(friend)}
              activeOpacity={0.7}
            >
              <View style={styles.userInfo}>
                {friendProfileImageUrl ? (
                  <Image
                    source={{ uri: friendProfileImageUrl }}
                    style={styles.avatar}
                  />
                ) : (
                  <View style={styles.avatar}>
                    <Text style={styles.avatarText}>
                      {friend.nickname.charAt(0)}
                    </Text>
                  </View>
                )}
                <View style={styles.userDetails}>
                  <Text style={styles.userName}>{friend.nickname}</Text>
                  <Text style={styles.userHandle}>{friend.user_id}</Text>
                </View>
              </View>
            </TouchableOpacity>
          );
        })}

        {searchQuery && searchResults.length === 0 && (
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>검색 결과가 없습니다.</Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.secondarySystemBackground,
  },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.lg,
    backgroundColor: Colors.systemBackground,
    borderBottomWidth: 0.5,
    borderBottomColor: Colors.separator,
    position: 'relative',
  },

  // 뒤로가기 버튼
  backButton: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.round,
    backgroundColor: Colors.secondarySystemBackground,
    justifyContent: 'center',
    alignItems: 'center',
    ...Shadows.small,
    zIndex: 2,
  },
  backButtonText: {
    ...Typography.title3,
    color: Colors.label,
    fontWeight: 'bold',
  },

  headerTitle: {
    ...Typography.headline,
    color: Colors.label,
    position: 'absolute',
    left: 0,
    right: 0,
    textAlign: 'center',
  },

  placeholder: {
    position: 'absolute',
    right: Spacing.lg,
    width: 44,
    height: 44,
  },

  // 본문
  searchContainer: {
    width: '100%',
    alignItems: 'center',
  },
  searchBox: {
    width: '89%', // 부모 SafeAreaView 기준
    flexDirection: 'row', // 아이콘 + 입력창 가로 배치
    alignItems: 'center', // 수직 가운데 정렬
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: Spacing.cardPadding,
    borderWidth: 0.5,
    borderColor: '#DEE2E6',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    marginTop: 16,
  },
  searchIcon: {
    marginRight: 8,
    fontSize: 18,
  },
  searchInput: {
    flex: 1, // 나머지 공간 차지
    color: '#2C3E50',
    fontSize: 16,
  },

  content: {
    flex: 1,
    padding: Spacing.screenPadding,
  },

  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.cardPadding,
    backgroundColor: '#FFFFFF',
    marginBottom: 12,
    borderRadius: 12,
  },
  userInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#E9ECEF',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 15,
  },
  avatarText: { fontSize: 24 },

  userDetails: { flex: 1 },
  userName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#2C3E50',
    marginBottom: 4,
  },
  userHandle: { fontSize: 14, color: '#6C757D' },

  addButton: {
    backgroundColor: '#B11515',
    paddingHorizontal: 20,
    paddingVertical: 8,
    borderRadius: 8,
  },
  addButtonText: { fontSize: 14, fontWeight: '600', color: '#FFFFFF' },

  sentButton: { backgroundColor: '#6C757D' },
  sentButtonText: { color: '#FFFFFF' },


  shareButton: {
    backgroundColor: '#9c9c9cff',
    width: 40,
    height: 40,
    borderRadius: 8,
    opacity: 0.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  shareText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 16,
  },

  sectionHeader: {
    paddingVertical: Spacing.lg,
    backgroundColor: Colors.secondarySystemBackground,
  },
  sectionTitle: {
    ...Typography.headline,
    color: Colors.label,
    fontWeight: '600',
  },

  emptyState: {
    padding: Spacing.xl,
    alignItems: 'center',
  },
  emptyStateText: {
    ...Typography.callout,
    color: Colors.tertiaryLabel,
  },
});

export default AddFriendPage;
