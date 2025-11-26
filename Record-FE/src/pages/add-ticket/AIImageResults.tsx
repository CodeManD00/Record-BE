// === AIImageResults.tsx (UI 미변경, API 로직만 완전 수정) ===

import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  Image,
  ActivityIndicator,
  Dimensions,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  imageGenerationService,
  ImageGenerationRequest,
} from '../../services/api';
import { apiClient } from '../../services/api/client';
import { useAtom } from 'jotai';
import { basePromptAtom } from '../../atoms';
import {
  Colors,
  Typography,
  Spacing,
  BorderRadius,
  Shadows,
} from '../../styles/designSystem';
import { resolveImageUrl } from '../../utils/resolveImageUrl';
import { sanitizePrompt } from '../../utils/sanitizePrompt';

interface AIImageResultsProps {
  navigation: any;
  route?: {
    params?: {
      ticketData?: any;
      reviewData?: {
        reviewText: string;
      };
      images?: string[];
      settings?: {
        backgroundColor: string;
        includeText: boolean;
        imageStyle: string;
        aspectRatio: string;
      };
    };
  };
}

const { width } = Dimensions.get('window');
const cardWidth = width - 48;
const cardHeight = (cardWidth * 5) / 4;

const AIImageResults: React.FC<AIImageResultsProps> = ({ navigation, route }) => {
  const [isGenerating, setIsGenerating] = useState(true);
  const [generatedImage, setGeneratedImage] = useState<string | null>(null);
  const [generationHistory, setGenerationHistory] = useState<string[]>([]);
  const [regenerationRequest, setRegenerationRequest] = useState<string>('');
  const [currentPrompt, setCurrentPrompt] = useState<string | null>(null);
  const [basePrompt] = useAtom(basePromptAtom);

  const ticketData = route?.params?.ticketData;
  const reviewData = route?.params?.reviewData;
  const settings = route?.params?.settings;

  useEffect(() => {
    handleGenerateAIImage();
  }, []);

  /** 🎨 장르 매핑 */
  const mapGenreForBackend = (frontendGenre: string): string => {
    if (frontendGenre?.includes('뮤지컬') || frontendGenre?.includes('연극'))
      return '뮤지컬';
    if (frontendGenre?.includes('밴드')) return '밴드';
    return '뮤지컬';
  };

  /** 🖼 이미지 최초 생성 */
  const handleGenerateAIImage = async () => {
    setIsGenerating(true);

    try {
      if (!ticketData?.title || !reviewData?.reviewText) {
        Alert.alert('오류', '티켓 정보 또는 후기 정보가 없습니다.');
        setIsGenerating(false);
        return;
      }

      // 인증 토큰 확인 및 로드
      await apiClient.ensureAuthToken();
      const token = await apiClient.getStoredToken();
      
      if (!token) {
        console.error('❌ 인증 토큰이 없습니다.');
        Alert.alert('인증 오류', '로그인이 필요합니다. 다시 로그인해주세요.');
        setIsGenerating(false);
        return;
      }

      console.log('🔑 인증 토큰 확인 완료');

      // performedAt이 Date라면 문자열로 변환
      const dateValue =
        ticketData?.performedAt instanceof Date
          ? ticketData.performedAt.toISOString()
          : ticketData?.performedAt ?? '';

      // basePrompt 정제 (OpenAI 안전 정책 준수)
      const sanitizedBasePrompt = basePrompt ? sanitizePrompt(basePrompt) : null;

      // 요청 데이터 정리 (빈 값 제거)
      const requestData: ImageGenerationRequest = {
        title: ticketData.title,
        review: reviewData?.reviewText || '',
        ...(mapGenreForBackend(ticketData.genre || '') && {
          genre: mapGenreForBackend(ticketData.genre || ''),
        }),
        ...(ticketData.venue && ticketData.venue.trim() && {
          location: ticketData.venue.trim(),
        }),
        ...(dateValue && { date: dateValue }),
        ...(sanitizedBasePrompt && sanitizedBasePrompt.trim() && {
          basePrompt: sanitizedBasePrompt.trim(),
        }),
      };

      console.log('🔍 이미지 생성 요청 데이터:', JSON.stringify(requestData, null, 2));
      console.log('📋 원본 basePrompt:', basePrompt);
      console.log('📋 정제된 basePrompt:', sanitizedBasePrompt);
      console.log('📋 basePrompt 길이:', sanitizedBasePrompt?.length || 0);

      const result = await imageGenerationService.generateImage(requestData);

      if (result.success && result.data) {
        const imageData = result.data;

        // 상대 경로를 전체 URL로 변환
        const resolvedImageUrl = resolveImageUrl(imageData.imageUrl);
        console.log('🖼 원본 imageUrl:', imageData.imageUrl);
        console.log('🖼 변환된 imageUrl:', resolvedImageUrl);

        if (resolvedImageUrl) {
          setGeneratedImage(resolvedImageUrl);
          setGenerationHistory(prev => [resolvedImageUrl, ...prev]);
        } else {
          console.error('❌ 이미지 URL 변환 실패');
          Alert.alert('오류', '이미지 URL을 가져올 수 없습니다.');
          setIsGenerating(false);
          return;
        }

        if (imageData.prompt) setCurrentPrompt(imageData.prompt);
      } else {
        console.error('❌ 이미지 생성 실패');
        console.error('응답 success:', result.success);
        console.error('응답 data:', result.data);
        console.error('에러 코드:', result.error?.code);
        console.error('에러 메시지:', result.error?.message);
        console.error('에러 상세:', result.error?.details);
        
        const errorMessage = result.error?.message || 'AI 이미지 생성에 실패했습니다.';
        Alert.alert('오류', errorMessage);
      }
    } catch (error) {
      console.error('❌ 이미지 생성 중 오류:', error);
      Alert.alert('오류', 'AI 이미지 생성 중 문제가 발생했습니다.');
    } finally {
      setIsGenerating(false);
    }
  };

  /** 🖌 이미지 재생성 */
  const handleRegenerateImage = async () => {
    if (!generatedImage) {
      Alert.alert('오류', '생성된 이미지가 없습니다.');
      return;
    }

    setIsGenerating(true);
    setGeneratedImage(null);

    try {
      // 인증 토큰 확인 및 로드
      await apiClient.ensureAuthToken();
      const token = await apiClient.getStoredToken();
      
      if (!token) {
        console.error('❌ 인증 토큰이 없습니다.');
        Alert.alert('인증 오류', '로그인이 필요합니다. 다시 로그인해주세요.');
        setIsGenerating(false);
        return;
      }

      console.log('🔑 인증 토큰 확인 완료');

      const dateValue =
        ticketData?.performedAt instanceof Date
          ? ticketData.performedAt.toISOString()
          : ticketData?.performedAt ?? '';

      // basePrompt 정제 (OpenAI 안전 정책 준수)
      const sanitizedBasePrompt = basePrompt ? sanitizePrompt(basePrompt) : null;

      // 요청 데이터 정리 (빈 값 제거)
      const requestData: ImageGenerationRequest = {
        title: ticketData.title,
        review: reviewData?.reviewText || '',
        ...(mapGenreForBackend(ticketData.genre || '') && {
          genre: mapGenreForBackend(ticketData.genre || ''),
        }),
        ...(ticketData.venue && ticketData.venue.trim() && {
          location: ticketData.venue.trim(),
        }),
        ...(dateValue && { date: dateValue }),
        ...(sanitizedBasePrompt && sanitizedBasePrompt.trim() && {
          basePrompt: sanitizedBasePrompt.trim(),
        }),
        ...(regenerationRequest.trim() && {
          imageRequest: sanitizePrompt(regenerationRequest.trim()),
        }),
      };

      console.log('🔄 재생성 요청:', JSON.stringify(requestData, null, 2));
      console.log('📝 사용자 요구사항:', regenerationRequest);
      console.log('📋 원본 basePrompt:', basePrompt);
      console.log('📋 정제된 basePrompt:', sanitizedBasePrompt);

      const result = await imageGenerationService.generateImage(requestData);

      if (result.success && result.data) {
        const imageData = result.data;

        // 상대 경로를 전체 URL로 변환
        const resolvedImageUrl = resolveImageUrl(imageData.imageUrl);
        console.log('🔄 재생성 - 원본 imageUrl:', imageData.imageUrl);
        console.log('🔄 재생성 - 변환된 imageUrl:', resolvedImageUrl);

        if (resolvedImageUrl) {
          setGeneratedImage(resolvedImageUrl);
          setGenerationHistory(prev => [resolvedImageUrl, ...prev]);
        } else {
          console.error('❌ 이미지 URL 변환 실패');
          Alert.alert('오류', '이미지 URL을 가져올 수 없습니다.');
          return;
        }

        if (imageData.prompt) setCurrentPrompt(imageData.prompt);

        setRegenerationRequest('');
      } else {
        Alert.alert('오류', result.error?.message || '이미지 재생성에 실패했습니다.');
      }
    } catch (error) {
      console.error('❌ 재생성 오류:', error);
      Alert.alert('오류', '이미지 재생성 중 문제가 발생했습니다.');
    } finally {
      setIsGenerating(false);
    }
  };

  /** 선택 버튼 */
  const handleSelectImage = () => {
    if (generatedImage) {
      navigation.navigate('TicketComplete', {
        ticketData,
        reviewData,
        images: [generatedImage],
      });
    }
  };

  /** 히스토리 이미지 선택 */
  const handleSelectFromHistory = (imageUrl: string) => {
    setGeneratedImage(imageUrl);
  };

  return (
    <SafeAreaView style={styles.container} edges={['left', 'right']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
          <Text style={styles.backButtonText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>티켓 이미지 생성</Text>

        {generatedImage && (
          <TouchableOpacity style={styles.nextButton} onPress={handleSelectImage}>
            <Text style={styles.nextButtonText}>다음</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* 로딩 화면 */}
      {isGenerating ? (
        <View style={styles.loadingFullScreen}>
          <ActivityIndicator size="large" color="#b11515" />
          <Text style={styles.generatingTitle}>AI 이미지 생성 중...</Text>
        </View>
      ) : (
        <ScrollView
          style={styles.content}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={true}
          keyboardShouldPersistTaps="handled"
          bounces={true}
          scrollEnabled={true}
          nestedScrollEnabled={true}
        >
          {generatedImage && (
            <>
              {/* 메시지 */}
              <View style={styles.successMessageContainer}>
                <Text style={styles.successMessage}>이미지가 생성되었어요!</Text>
              </View>

              {/* 생성 이미지 */}
              <View style={styles.generatedImageContainer}>
                <Image
                  source={{ uri: generatedImage }}
                  style={styles.generatedImage}
                  resizeMode="cover"
                />
              </View>

              {/* 재생성 UI */}
              <View style={styles.regenerationSection}>
                <Text style={styles.regenerationTitle}>이렇게 바꿔주세요</Text>

                <View style={styles.hintBubble}>
                  <Text style={styles.hintText}>
                    생성된 티켓이 마음에 들지 않나요?{'\n'}
                    원하는 스타일을 알려주세요!
                  </Text>
                </View>

                <TextInput
                  style={styles.regenerationInput}
                  placeholder="요구사항을 입력하세요..."
                  placeholderTextColor={Colors.tertiaryLabel}
                  value={regenerationRequest}
                  onChangeText={setRegenerationRequest}
                  multiline
                  textAlignVertical="top"
                />

                <TouchableOpacity
                  style={[styles.regenerateButton, isGenerating && styles.regenerateButtonDisabled]}
                  disabled={isGenerating}
                  onPress={handleRegenerateImage}
                >
                  <Text style={styles.regenerateButtonText}>다시 생성하기</Text>
                </TouchableOpacity>
              </View>
            </>
          )}

          {/* 히스토리 */}
          {generationHistory.length > 1 && (
            <View style={styles.sectionContainer}>
              <Text style={styles.sectionTitle}>생성 히스토리</Text>

              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.historyContainer}>
                {generationHistory.slice(1).map((imageUrl, index) => (
                  <TouchableOpacity
                    key={index}
                    style={styles.historyImageWrapper}
                    onPress={() => handleSelectFromHistory(imageUrl)}
                  >
                    <Image
                      source={{ uri: imageUrl }}
                      style={[
                        styles.historyImage,
                        generatedImage === imageUrl && styles.selectedHistoryImage,
                      ]}
                    />
                    {generatedImage === imageUrl && (
                      <View style={styles.selectedOverlay}>
                        <Text style={styles.selectedText}>✓</Text>
                      </View>
                    )}
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
};

// === 아래는 UI 스타일 — 절대 수정 없음 ===
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8F9FA' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.lg,
    backgroundColor: Colors.systemBackground,
    ...Shadows.small,
    zIndex: 1,
  },
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
  nextButton: { paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm },
  nextButtonText: { ...Typography.callout, color: '#b11515', fontWeight: '600' },

  content: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingBottom: Spacing.xxxl * 2,
  },

  loadingFullScreen: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F8F9FA',
  },
  generatingTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2C3E50',
    marginTop: 8,
  },

  successMessageContainer: {
    marginHorizontal: Spacing.xl,
    marginTop: Spacing.xl,
    marginBottom: Spacing.md,
    alignItems: 'center',
  },
  successMessage: { ...Typography.title2, fontWeight: '600', color: Colors.label },

  generatedImageContainer: {
    marginHorizontal: Spacing.xl,
    marginTop: Spacing.lg,
    alignItems: 'center',
  },

  generatedImage: {
    width: cardWidth,
    height: cardHeight,
    borderRadius: BorderRadius.lg,
    backgroundColor: Colors.systemGray5,
  },

  regenerationSection: {
    marginHorizontal: Spacing.xl,
    marginTop: Spacing.xxxl,
    marginBottom: Spacing.xxxl,
  },
  regenerationTitle: {
    ...Typography.title3,
    fontWeight: '600',
    color: Colors.label,
    marginBottom: Spacing.md,
  },

  hintBubble: {
    backgroundColor: '#FFF5F5',
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: '#FFE5E5',
  },
  hintText: { ...Typography.caption1, color: '#8B4513', lineHeight: 18 },

  regenerationInput: {
    backgroundColor: Colors.systemBackground,
    borderWidth: 1,
    borderColor: Colors.systemGray5,
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    minHeight: 100,
    ...Typography.body,
    color: Colors.label,
    marginBottom: Spacing.lg,
    ...Shadows.small,
  },

  regenerateButton: {
    backgroundColor: '#FF6B6B',
    borderRadius: BorderRadius.lg,
    paddingVertical: Spacing.lg,
    paddingHorizontal: Spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadows.button,
  },
  regenerateButtonDisabled: { opacity: 0.6 },

  regenerateButtonText: {
    ...Typography.headline,
    color: Colors.white,
    fontWeight: '600',
  },

  sectionContainer: {
    backgroundColor: '#FFFFFF',
    marginHorizontal: 20,
    marginTop: 20,
    marginBottom: 20,
    padding: 20,
    borderRadius: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2C3E50',
    marginBottom: 12,
  },
  historyContainer: { marginTop: 12 },
  historyImageWrapper: { position: 'relative', marginRight: 12 },
  historyImage: {
    width: 80,
    height: 100,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  selectedHistoryImage: { borderColor: '#b11515' },
  selectedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  selectedText: { fontSize: 24, color: '#b11515', fontWeight: 'bold' },
});

export default AIImageResults;
