import { useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Image,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAtom } from 'jotai';
import { Colors, Typography, Spacing, BorderRadius, Shadows, ComponentStyles, Layout } from '../../styles/designSystem';
import ModalHeader from '../../components/ModalHeader';
import { 
  sentFriendRequestsAtom,
  fetchSentRequestsAtom,
  cancelFriendRequestAtom,
} from '../../atoms';
import { FriendRequest } from '../../types/friend';

interface SentRequestsPageProps {
  navigation: any;
}

const SentRequestsPage: React.FC<SentRequestsPageProps> = ({ navigation }) => {
  const [sentRequests] = useAtom(sentFriendRequestsAtom);
  const [, fetchSentRequests] = useAtom(fetchSentRequestsAtom);
  const [, cancelFriendRequest] = useAtom(cancelFriendRequestAtom);

  // 페이지 로드 시 데이터 가져오기
  useEffect(() => {
    fetchSentRequests(true);
  }, []);

  // 친구 요청 취소
  const handleCancelRequest = async (request: FriendRequest) => {
    Alert.alert(
      '친구 요청 취소',
      `${request.nickname}님에게 보낸 친구 요청을 취소하시겠어요?`,
      [
        { text: '취소', style: 'cancel' },
        {
          text: '취소하기',
          style: 'destructive',
          onPress: async () => {
            try {
              const result = await cancelFriendRequest(request.id);
              if (result.success) {
                Alert.alert('완료', '친구 요청을 취소했습니다.');
                fetchSentRequests(true);
              } else {
                Alert.alert('오류', result.error?.message || '친구 요청 취소 중 오류가 발생했습니다.');
              }
            } catch (error) {
              Alert.alert('오류', '친구 요청 취소 중 오류가 발생했습니다.');
            }
          },
        },
      ]
    );
  };

  // 친구 프로필로 이동
  const handleNavigateToFriendProfile = (request: FriendRequest) => {
    const friend = {
      id: request.toUserId || request.id,
      user_id: request.user_id,
      nickname: request.nickname,
      profileImage: request.profileImage,
      createdAt: request.createdAt,
      updatedAt: request.updatedAt,
    };
    
    navigation.navigate('FriendProfile', { friend });
  };

  const activeRequests = sentRequests.filter(r => r.status === 'PENDING');
  

  return (
    <SafeAreaView style={styles.container}>
      <ModalHeader 
        title="보낸 요청"
        onBack={() => navigation.goBack()}
      />

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {/* 보낸 요청 섹션 */}
        <View style={styles.sectionContainer}>
          <Text style={styles.sectionTitle}>
            보낸 요청 ({activeRequests.length})
          </Text>
        </View>

        {/* 보낸 요청 목록 */}
        {sentRequests.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>보낸 친구 요청이 없습니다</Text>
          </View>
        ) : (
          sentRequests.map((request, index) => (
            <View 
              key={request.id} 
              style={[
                styles.requestItem,
                index === sentRequests.length - 1 && styles.lastRequestItem
              ]}
            >
              {/* 프로필 클릭 가능 */}
              <TouchableOpacity
                style={styles.requestInfo}
                onPress={() => handleNavigateToFriendProfile(request)}
              >
                {request.profileImage ? (
                  <Image
                    source={{ uri: request.profileImage }}
                    style={styles.avatar}
                  />
                ) : (
                  <View style={[styles.avatar, styles.avatarPlaceholder]}>
                    <Text style={styles.avatarText}>
                      {request.nickname?.charAt(0) || '?'}
                    </Text>
                  </View>
                )}
                <View style={styles.requestDetails}>
                  <Text style={styles.requestName}>{request.nickname}</Text>
                  <Text style={styles.requestHandle}>{request.user_id}</Text>
                </View>
              </TouchableOpacity>

              {request.status === 'PENDING' && (
                <TouchableOpacity
                  style={styles.cancelButton}
                  onPress={() => handleCancelRequest(request)}
                >
                  <Text style={styles.cancelButtonText}>취소</Text>
                </TouchableOpacity>
              )}
              
              {request.status === 'ACCEPTED' && (
                <Text style={styles.acceptedText}>수락됨</Text>
              )}
              
              {request.status === 'REJECTED' && (
                <Text style={styles.rejectedText}>거절됨</Text>
              )}
            </View>
          ))
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
  content: {
    flex: 1,
    backgroundColor: Colors.secondarySystemBackground,
  },
  sectionContainer: {
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    marginTop: 16,
    borderTopLeftRadius: 12,
    borderTopRightRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2C3E50',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  emptyContainer: {
    padding: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#6C757D',
  },
  requestItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F3F4',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  requestInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#E9ECEF',
    marginRight: 15,
  },
  avatarPlaceholder: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#E9ECEF',
  },
  avatarText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#6C757D',
  },
  requestDetails: {
    flex: 1,
  },
  requestName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#2C3E50',
    marginBottom: 2,
  },
  requestHandle: {
    fontSize: 14,
    color: '#6C757D',
  },
  cancelButton: {
    backgroundColor: '#DC3545',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  cancelButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  acceptedText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#28A745',
    paddingHorizontal: 12,
  },
  rejectedText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#6C757D',
    paddingHorizontal: 12,
  },
  lastRequestItem: {
    borderBottomLeftRadius: 12,
    borderBottomRightRadius: 12,
    borderBottomWidth: 0,
  },
});

export default SentRequestsPage;

