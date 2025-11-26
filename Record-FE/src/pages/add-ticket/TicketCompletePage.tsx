import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
  StatusBar,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAtom } from 'jotai';
import { addTicketAtom, TicketStatus, basePromptAtom } from '../../atoms';
import { ticketService } from '../../services/api/index';
import { userProfileAtom } from '../../atoms/userAtomsApi';
import { Alert } from 'react-native';
import {
  Colors,
  Typography,
  Spacing,
  BorderRadius,
  Shadows,
} from '../../styles/designSystem';

interface TicketCompletePageProps {
  navigation: any;
  route?: {
    params?: {
      ticketData?: any;
      reviewData?: {
        reviewText?: string;
        text?: string;
        isPublic?: boolean;
      };
      images?: string[];
    };
  };
}

const { width, height } = Dimensions.get('window');

const TicketCompletePage: React.FC<TicketCompletePageProps> = ({ navigation, route }) => {
  const ticketData = route?.params?.ticketData;
  const reviewData = route?.params?.reviewData;
  const images = route?.params?.images ?? [];
  const [, addTicket] = useAtom(addTicketAtom);
  const [, setBasePrompt] = useAtom(basePromptAtom);
  const [userProfile] = useAtom(userProfileAtom);
  const [basePrompt] = useAtom(basePromptAtom);

  /** 표시될 이미지 선택 */
  const ticketImage =
    images.length > 0
      ? images[0]
      : ticketData?.images?.length > 0
      ? ticketData.images[0]
      : null;

  console.log('=== TicketCompletePage 이미지 디버깅 ===');
  console.log('전달받은 images:', images);
  console.log('ticketData.images:', ticketData?.images);
  console.log('최종 표시할 이미지:', ticketImage);

  useEffect(() => {
    if (!ticketData) {
      console.warn('ticketData가 없습니다!');
      return;
    }

    const saveTicketToBackend = async () => {
      console.log('=== 티켓 저장 시작 (백엔드 API) ===');
      console.log('ticketData:', ticketData);
      console.log('reviewData:', reviewData);
      console.log('images:', images);

      // 사용자 ID 확인
      const userId = userProfile?.id;
      if (!userId) {
        Alert.alert('오류', '사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.');
        return;
      }

      // performedAt을 Date 객체로 변환
      const parsePerformedAt = (value: any): Date => {
        if (!value) return new Date();
        if (value instanceof Date) return value;
        if (typeof value === 'string') {
          const parsed = new Date(value);
          return isNaN(parsed.getTime()) ? new Date() : parsed;
        }
        return new Date();
      };

      const performedAt = parsePerformedAt(ticketData?.performedAt);
      
      // viewDate 형식으로 변환 (YYYY-MM-DD)
      const viewDate = performedAt.toISOString().split('T')[0];

      // genre를 백엔드 형식으로 변환
      const mapGenreToBackend = (genre: string | null | undefined): string => {
        if (!genre) return 'MUSICAL'; // 기본값
        const genreMap: Record<string, string> = {
          '밴드': 'BAND',
          '연극/뮤지컬': 'MUSICAL',
          '뮤지컬': 'MUSICAL',
          '연극': 'PLAY',
        };
        return genreMap[genre] || 'MUSICAL';
      };

      // 이미지 URL 처리 (상대 경로인 경우 첫 번째 이미지만 사용)
      let imageUrl: string | null = null;
      if (images && images.length > 0) {
        let firstImage = images[0];
        
        // 쿼리 파라미터 제거 (DB에 저장할 때는 순수 경로만 저장)
        if (firstImage.includes('?')) {
          firstImage = firstImage.split('?')[0];
        }
        
        // 절대 URL인 경우 상대 경로로 변환 필요할 수 있음
        // 백엔드에 저장된 경로인 경우 그대로 사용
        if (firstImage.startsWith('http://localhost:8080')) {
          imageUrl = firstImage.replace('http://localhost:8080', '');
        } else if (firstImage.startsWith('/uploads/')) {
          imageUrl = firstImage;
        } else {
          // 이미 절대 경로인 경우 그대로 사용
          imageUrl = firstImage;
        }
      }

      // 백엔드 요청 데이터 생성
      const requestData = {
        userId: userId,
        performanceTitle: ticketData?.title || '',
        venue: ticketData?.venue || '',
        seat: ticketData?.seat || '',
        artist: ticketData?.artist || '',
        posterUrl: ticketData?.posterUrl || null,
        genre: mapGenreToBackend(ticketData?.genre),
        viewDate: viewDate,
        imageUrl: imageUrl,
        imagePrompt: basePrompt || null,
        reviewText: reviewData?.reviewText || reviewData?.text || null,
        isPublic: reviewData?.isPublic !== false, // 기본값은 true
      };

      console.log('📤 백엔드 API 요청 데이터:', JSON.stringify(requestData, null, 2));

      try {
        // 백엔드 API 호출
        const result = await ticketService.createTicket(requestData);

        if (!result.success) {
          console.error('❌ 티켓 저장 실패:', result.error);
          Alert.alert('저장 실패', result.error?.message || '티켓 저장에 실패했습니다.');
          return;
        }

        console.log('✅ 티켓 저장 성공:', result.data);
        
        // 로컬 atom에도 저장 (UI 업데이트용)
        const ticketReview =
          reviewData?.reviewText || reviewData?.text
            ? {
                reviewText: reviewData.reviewText || reviewData.text || '',
                createdAt: new Date(),
              }
            : undefined;

        const status =
          reviewData?.isPublic === false ? TicketStatus.PRIVATE : TicketStatus.PUBLIC;

        const ticketToAdd = {
          ...ticketData,
          performedAt: performedAt,
          review: ticketReview,
          images: images ?? [],
          status,
        };

        const localResult = addTicket(ticketToAdd);
        if (localResult.success) {
          console.log('✅ 로컬 티켓 저장 성공');
        }

        // 티켓 저장 완료 시 basePrompt 초기화
        setBasePrompt(null);
        console.log('🗑️ basePrompt 초기화 완료');
      } catch (error) {
        console.error('❌ 티켓 저장 중 오류:', error);
        Alert.alert('저장 실패', '티켓 저장 중 오류가 발생했습니다.');
      }
    };

    saveTicketToBackend();

    /** 3초 후 홈으로 이동 */
    const timer = setTimeout(() => {
      navigation.reset({
        index: 0,
        routes: [{ name: 'MainTabs' }],
      });
    }, 3000);

    return () => clearTimeout(timer);
  }, [navigation, ticketData, reviewData, images, userProfile, basePrompt, addTicket, setBasePrompt]);

  const handleBackPress = () => {
    navigation.reset({
      index: 0,
      routes: [{ name: 'MainTabs' }],
    });
  };

  return (
    <SafeAreaView style={styles.container} edges={['left', 'right']}>
      <StatusBar barStyle="dark-content" backgroundColor="#F8F9FA" />

      <View style={styles.content}>
        {/* Title */}
        <Text style={styles.title}>새로운 티켓 생성 완료</Text>
        <Text style={styles.subtitle}>하나의 추억을 저장했어요</Text>

        {/* Ticket Card */}
        <View style={styles.ticketCard}>
          <View style={styles.ticketHeader}>
            <Text style={styles.ticketHeaderText}>{ticketData?.title}</Text>
          </View>

          <View style={styles.ticketMain}>
            {ticketImage ? (
              <Image
                source={{ uri: ticketImage }}
                style={styles.ticketImage}
                resizeMode="cover"
                onError={e => {
                  console.error('이미지 로드 실패:', e.nativeEvent.error);
                }}
              />
            ) : (
              <View style={styles.ticketPlaceholder}>
                <Text style={styles.noImageText}>이미지 없음</Text>
              </View>
            )}
          </View>

          <View style={styles.ticketFooter}>
            <Text style={styles.footerSubtext}>
              {ticketData?.venue || ''} •{' '}
              {ticketData?.performedAt
                ? new Date(ticketData.performedAt).toLocaleDateString('ko-KR', {
                    month: 'long',
                    day: 'numeric',
                  })
                : ''}
              {' '}• 8PM
            </Text>
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8F9FA' },
  content: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 20 },
  title: { fontSize: 24, fontWeight: 'bold', color: '#2C3E50', textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 16, color: '#7F8C8D', textAlign: 'center', marginBottom: 40 },

  ticketCard: {
    width: width - 60,
    height: height * 0.6,
    backgroundColor: '#ffffff',
    borderRadius: 20,
    position: 'relative',
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
  },

  ticketHeader: { padding: 20 },
  ticketHeaderText: { fontSize: 18, fontWeight: 'bold', color: '#2C3E50', letterSpacing: 2 },

  ticketMain: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 20 },
  ticketImage: { width: '100%', height: '100%', borderRadius: 12 },

  ticketPlaceholder: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 12,
  },
  noImageText: { fontSize: 14, color: '#7F8C8D' },

  ticketFooter: { padding: 20, alignItems: 'flex-end' },
  footerSubtext: { fontSize: 12, color: '#2C3E50' },
});

export default TicketCompletePage;
