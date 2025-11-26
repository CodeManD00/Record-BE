// =========================================
// AddReviewPage.tsx — 최종 완성본
// =========================================

import React, { useEffect, useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Switch,
  Platform,
  Alert,
  Animated,
  Dimensions,
  PanResponder,
  Image,
  KeyboardAvoidingView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Colors, Typography, Spacing, Shadows, BorderRadius } from '../../styles/designSystem';
import DocumentPicker from 'react-native-document-picker';
import { sttService } from '../../services/api/sttService';
import { apiClient } from '../../services/api/client';

import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/reviewTypes';
import ReviewSummaryModal from '../../components/ReviewSummaryModal';
import ModalHeader from '../../components/ModalHeader';
import { useAtom } from 'jotai';
import { userProfileAtom } from '../../atoms/userAtoms';
import { useUserProfileData } from '../../hooks/useApiData';

type AddReviewPageProps = NativeStackScreenProps<RootStackParamList, 'AddReview'>;

const { width } = Dimensions.get('window');

const AddReviewPage = ({ navigation, route }: AddReviewPageProps) => {
  /** ===============================
   *              상태값
   *  =============================== */
  const { ticketData } = route.params;

  // 사용자 프로필 가져오기
  const { data: userProfile } = useUserProfileData({ fetchOnMount: true });
  const [localProfile] = useAtom(userProfileAtom);
  const currentUser = userProfile || localProfile;

  const [reviewText, setReviewText] = useState('');
  const [isPublic, setIsPublic] = useState(true);
  const [showSummaryModal, setShowSummaryModal] = useState(false);
  const [summaryText, setSummaryText] = useState('');
  const [modalTitle, setModalTitle] = useState('요약완료!'); // 모달 제목 상태 추가
  const [isProcessingSTT, setIsProcessingSTT] = useState(false);
  const [transcriptionId, setTranscriptionId] = useState<number | undefined>(undefined);
  const [questions, setQuestions] = useState<string[]>([]);
  const [isLoadingQuestions, setIsLoadingQuestions] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isCardVisible, setIsCardVisible] = useState(true);
  const [isOrganizing, setIsOrganizing] = useState(false);
  const [selectedAudioUri, setSelectedAudioUri] = useState<string | null>(null);
  const [selectedAudioFileName, setSelectedAudioFileName] = useState<string | null>(null);
  const [selectedAudioFileType, setSelectedAudioFileType] = useState<string | null>(null);

  // reviewText 변경 추적 (디버깅)
  useEffect(() => {
    console.log('📝 reviewText 상태 변경됨:', {
      reviewText,
      length: reviewText?.length || 0,
      type: typeof reviewText,
      isEmpty: !reviewText || reviewText.trim().length === 0,
    });
  }, [reviewText]);

  /** ===============================
   *       Navigation Warning Fix
   *  =============================== */
  useEffect(() => {
    if (ticketData.performedAt instanceof Date) {
      (ticketData as any).performedAt = ticketData.performedAt.toISOString();
    }
  }, [ticketData]);


  /** ===============================
   *           질문 가져오기
   *  =============================== */
  useEffect(() => {
    const fetchQuestions = async () => {
      try {
        setIsLoadingQuestions(true);

        // 인증 토큰을 먼저 확실히 로드
        await apiClient.ensureAuthToken();
        
        // 인증 토큰 확인
        const token = await apiClient.getStoredToken();
        console.log('인증 토큰 존재 여부:', token ? '있음' : '없음');
        
        if (!token) {
          console.warn('⚠️ 인증 토큰이 없습니다. 기본 질문을 사용합니다.');
          setQuestions([
            '이 공연을 보게 된 계기는?',
            '가장 인상 깊었던 순간은?',
            '다시 본다면 어떤 점이 기대되나요?',
          ]);
          return;
        }

        /**
         * AddTicketPage에서 선택한 장르를 백엔드 API 형식으로 매핑
         * AddTicketPage 장르 옵션: '밴드', '연극/뮤지컬'
         * 백엔드 API 기대 값: '밴드', '연극/뮤지컬', 'COMMON'
         */
        const mapGenre = (g: string | undefined): string => {
          if (!g) return 'COMMON';
          
          // 정확한 매칭 우선
          const normalizedGenre = g.trim();
          
          if (normalizedGenre === '밴드') return '밴드';
          if (normalizedGenre === '연극/뮤지컬') return '연극/뮤지컬';
          
          // 부분 매칭 (호환성)
          if (normalizedGenre.includes('밴드')) return '밴드';
          if (normalizedGenre.includes('뮤지컬') || normalizedGenre.includes('연극')) {
            return '연극/뮤지컬';
          }
          
          // 기본값
          return 'COMMON';
        };

        const genre = mapGenre(ticketData.genre);
        const apiUrl = `/review-questions?genre=${encodeURIComponent(genre)}`;
        
        console.log('=== 질문 가져오기 시작 ===');
        console.log('AddTicketPage에서 받은 장르:', ticketData.genre);
        console.log('백엔드로 전달할 장르:', genre);
        console.log('API 요청 URL:', apiUrl);
        console.log('API Base URL:', __DEV__ ? 'http://localhost:8080' : 'https://api.ticketbook.app');
        console.log('전체 URL:', `${__DEV__ ? 'http://localhost:8080' : 'https://api.ticketbook.app'}${apiUrl}`);
        
        const result = await apiClient.get<any>(apiUrl);
        
        console.log('=== /review-questions API 응답 ===');
        console.log('응답 success:', result.success);
        console.log('응답 data:', result.data);
        console.log('응답 data 타입:', typeof result.data);
        console.log('응답 data가 배열인가?', Array.isArray(result.data));
        
        if (result.success && result.data) {
          // 백엔드 응답 구조: ApiResponseObject { success: boolean, data: {...}, message: string }
          // apiClient.get은 data.data를 반환하므로 result.data는 실제 질문 데이터
          let questionsArray: string[] = [];
          
          // 경우 1: result.data가 직접 배열인 경우
          if (Array.isArray(result.data)) {
            questionsArray = result.data;
          }
          // 경우 2: result.data가 객체이고 내부에 data 필드가 있는 경우
          else if (result.data && typeof result.data === 'object' && Array.isArray(result.data.data)) {
            questionsArray = result.data.data;
          }
          // 경우 3: result.data가 객체이고 내부에 다른 필드명으로 배열이 있는 경우
          else if (result.data && typeof result.data === 'object') {
            // 가능한 필드명들을 확인
            const possibleFields = ['questions', 'items', 'content', 'list'];
            for (const field of possibleFields) {
              if (Array.isArray(result.data[field])) {
                questionsArray = result.data[field];
                break;
              }
            }
          }
          
          // 질문 배열이 있으면 사용
          if (questionsArray.length > 0) {
            console.log('✅ 질문 가져오기 성공! 가져온 질문 개수:', questionsArray.length);
            console.log('질문 내용:', questionsArray);
            setQuestions(questionsArray);
          } else {
            console.warn('⚠️ 질문 배열이 비어있거나 파싱 실패');
            console.warn('원본 응답 data:', result.data);
            // 기본 질문 사용
            setQuestions([
              '이 공연을 보게 된 계기는?',
              '가장 인상 깊었던 순간은?',
              '다시 본다면 어떤 점이 기대되나요?',
            ]);
          }
        } else {
          // API 호출 실패 시 기본 질문 사용
          console.warn('⚠️ 질문 가져오기 실패');
          console.warn('응답 상세:', {
            success: result.success,
            data: result.data,
            error: result.error,
          });
          
          // 에러 상세 정보 로깅
          if (!result.success && result.error) {
            console.error('❌ 에러 코드:', result.error.code);
            console.error('❌ 에러 메시지:', result.error.message);
            console.error('❌ 에러 상세:', result.error.details);
            console.error('❌ 전체 에러 객체:', JSON.stringify(result.error, null, 2));
            
            // 인증 에러인 경우 추가 로깅
            if (result.error.code === 'VALIDATION_ERROR' && result.error.message.includes('인증')) {
              console.error('🔒 인증 문제 감지 - 토큰을 다시 확인합니다.');
              const currentToken = await apiClient.getStoredToken();
              console.error('현재 토큰:', currentToken ? '존재함' : '없음');
            }
          }
          
          // 기본 질문 사용 (인증 실패 시에도 계속 진행 가능하도록)
          setQuestions([
            '이 공연을 보게 된 계기는?',
            '가장 인상 깊었던 순간은?',
            '다시 본다면 어떤 점이 기대되나요?',
          ]);
        }
      } finally {
        setIsLoadingQuestions(false);
      }
    };

    fetchQuestions();
  }, [ticketData.genre]);

  /** ===============================
   *           스와이프 카드
   *  =============================== */
  const pan = useRef(new Animated.ValueXY()).current;
  const opacity = useRef(new Animated.Value(1)).current;
  const cardScale = useRef(new Animated.Value(1)).current;
  const scrollX = useRef(new Animated.Value(0)).current;
  const cardHeight = useRef(new Animated.Value(1)).current;
  const reviewTranslateY = useRef(new Animated.Value(0)).current;
  const currentIndexRef = useRef(currentIndex);

  useEffect(() => {
    currentIndexRef.current = currentIndex;
  }, [currentIndex]);

  const resetCard = () => {
    Animated.parallel([
      Animated.spring(pan, { toValue: { x: 0, y: 0 }, useNativeDriver: false }),
      Animated.spring(opacity, { toValue: 1, useNativeDriver: false }),
    ]).start();
  };

  const bounce = (dir: 'left' | 'right') => {
    Animated.sequence([
      Animated.timing(pan, {
        toValue: { x: dir === 'left' ? -30 : 30, y: 0 },
        duration: 150,
        useNativeDriver: false,
      }),
      Animated.spring(pan, { toValue: { x: 0, y: 0 }, useNativeDriver: false }),
    ]).start();
  };

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: (_, gs) => Math.abs(gs.dx) > 10,
      onPanResponderMove: (_, gs) => pan.setValue({ x: gs.dx, y: 0 }),
      onPanResponderRelease: (_, gs) => {
        const idx = currentIndexRef.current;
        const total = questions.length;

        const left = gs.dx < -80 || gs.vx < -0.3;
        const right = gs.dx > 80 || gs.vx > 0.3;

        if (right) {
          if (idx === 0) bounce('left');
          else {
            const next = idx - 1;
            setCurrentIndex(next);
            Animated.timing(scrollX, {
              toValue: next * width,
              duration: 200,
              useNativeDriver: false,
            }).start();
            resetCard();
          }
        } else if (left) {
          if (idx === total - 1) bounce('right');
          else {
            const next = idx + 1;
            setCurrentIndex(next);
            Animated.timing(scrollX, {
              toValue: next * width,
              duration: 200,
              useNativeDriver: false,
            }).start();
            resetCard();
          }
        } else {
          resetCard();
        }
      },
    })
  ).current;

  /** ===============================
   *          오디오 파일 선택
   *  =============================== */
  const handleAudioFilePick = async () => {
    try {
      const result = await DocumentPicker.pick({
        type: [DocumentPicker.types.audio],
        copyTo: 'cachesDirectory',
      });

      if (result && result.length > 0) {
        const file = result[0];

        // 파일 정보 저장
        const fileName = file.name || file.uri.split('/').pop() || 'audio.m4a';
        const fileType = file.type || 'audio/m4a';
        const fileUri = file.uri;

        setSelectedAudioUri(fileUri);
        setSelectedAudioFileName(fileName);
        setSelectedAudioFileType(fileType);

        Alert.alert('완료', '오디오 파일이 선택되었습니다.\n"STT 변환 실행" 버튼을 눌러 변환하세요.');
      }
    } catch (err) {
      if (DocumentPicker.isCancel(err)) {
        // 사용자가 취소함
        console.log('파일 선택 취소');
      } else {
        console.error('파일 선택 오류:', err);
        Alert.alert('오류', '파일 선택 중 문제가 발생했습니다.');
      }
    }
  };

  /** ===============================
   *        STT 변환 실행
   *  =============================== */
  const handleSTTConversion = async () => {
    if (!selectedAudioUri || !selectedAudioFileName || !selectedAudioFileType) {
      Alert.alert('알림', '먼저 오디오 파일을 선택해주세요.');
      return;
    }

    if (!currentUser?.id) {
      Alert.alert('오류', '사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.');
      return;
    }

    try {
      setIsProcessingSTT(true);

      console.log('🎵 STT 변환 시작:', {
        fileName: selectedAudioFileName,
        fileType: selectedAudioFileType,
        uri: selectedAudioUri,
        userId: currentUser.id,
      });

      const sttResult = await sttService.transcribeAndSave(
        selectedAudioUri,
        selectedAudioFileName,
        selectedAudioFileType,
        currentUser.id
      );

      if (sttResult.success && sttResult.data) {
        console.log('🎤 STT 변환 성공 - 전체 응답:', JSON.stringify(sttResult.data, null, 2));
        console.log('🎤 sttResult.data 타입:', typeof sttResult.data);
        console.log('🎤 sttResult.data 키들:', Object.keys(sttResult.data || {}));
        
        // resultText가 있으면 우선 사용, 없으면 transcript 사용
        // 응답 구조가 다를 수 있으므로 여러 방법으로 시도
        const resultText = 
          sttResult.data.resultText || 
          sttResult.data.transcript || 
          (sttResult.data as any)?.resultText ||
          (sttResult.data as any)?.transcript ||
          '';
        
        console.log('🎤 추출된 resultText:', resultText);
        console.log('🎤 resultText 타입:', typeof resultText);
        console.log('🎤 resultText 길이:', resultText?.length);
        console.log('🎤 현재 reviewText:', reviewText);
        console.log('🎤 현재 reviewText 타입:', typeof reviewText);
        
        if (!resultText || (typeof resultText === 'string' && resultText.trim().length === 0)) {
          console.error('❌ resultText가 비어있습니다!');
          console.error('❌ sttResult.data:', sttResult.data);
          Alert.alert('오류', '변환된 텍스트를 가져올 수 없습니다.');
          return;
        }
        
        // 문자열로 변환 (혹시 모를 타입 문제 방지)
        const resultTextString = String(resultText);
        
        // 기존 텍스트가 있으면 새 줄로 추가, 없으면 그대로 설정
        const updatedText = reviewText?.trim() 
          ? `${reviewText}\n${resultTextString}` 
          : resultTextString;
        
        console.log('🎤 업데이트될 텍스트:', updatedText);
        console.log('🎤 업데이트될 텍스트 길이:', updatedText?.length);
        console.log('🎤 업데이트될 텍스트 타입:', typeof updatedText);

        // 상태 업데이트 - 강제로 즉시 반영
        // React의 상태 업데이트를 확실히 하기 위해 함수형 업데이트 사용
        setReviewText(prev => {
          console.log('🎤 setReviewText 함수형 업데이트 호출');
          console.log('🎤 이전 reviewText:', prev);
          console.log('🎤 새로운 reviewText:', updatedText);
          return updatedText;
        });
        
        console.log('🎤 setReviewText 호출 완료. updatedText:', updatedText);

        const newTranscriptionId = sttResult.data.id ?? transcriptionId;
        if (newTranscriptionId) setTranscriptionId(newTranscriptionId);

        // 변환 완료 후 선택된 파일 정보 초기화
        setSelectedAudioUri(null);
        setSelectedAudioFileName(null);
        setSelectedAudioFileType(null);

        // 상태 업데이트가 완료된 후 Alert 표시
        // requestAnimationFrame을 사용하여 다음 렌더링 사이클에 실행
        requestAnimationFrame(() => {
          setTimeout(() => {
            Alert.alert('완료', '오디오 파일을 텍스트로 변환했어요.');
            console.log('🎤 Alert 표시 완료');
          }, 100);
        });
      } else {
        Alert.alert('오류', sttResult.error?.message || 'STT 변환 실패');
      }
    } catch (error) {
      console.error('STT 변환 오류:', error);
      Alert.alert('오류', 'STT 변환 중 문제가 발생했습니다.');
    } finally {
      setIsProcessingSTT(false);
    }
  };

  /** ===============================
   *        리뷰 정리 (Organize)
   *  =============================== */
  const handleOrganizeReview = async (
    textOverride?: string,
    transcriptionIdOverride?: number,
    options?: { showAlert?: boolean }
  ) => {
    const textToUse = ((textOverride ?? reviewText) || '').trim();

    if (!textToUse) {
      Alert.alert('알림', '정리할 텍스트가 없습니다.');
      return;
    }

    try {
      setIsOrganizing(true);
      const organizeResult = await sttService.organizeReview(
        textToUse,
        transcriptionIdOverride ?? transcriptionId
      );

      if (organizeResult.success && organizeResult.data) {
        const organizedText =
          organizeResult.data.finalReview ??
          organizeResult.data.summary ??
          organizeResult.data.transcript ??
          textToUse;

        // 기존 reviewText는 유지하고, 정리된 텍스트는 모달에 표시
        // 이렇게 하면 사용자가 원본 텍스트를 보존하면서 정리된 버전을 확인할 수 있음
        setSummaryText(organizedText);
        setModalTitle('정리완료!'); // 모달 제목을 "정리완료!"로 설정
        setShowSummaryModal(true); // 모달 표시

        if (organizeResult.data.id) {
          setTranscriptionId(organizeResult.data.id);
        }

        // Alert는 제거 (모달로 대체)
        // 사용자가 모달에서 결과를 확인하고 복사할 수 있으므로 별도 알림 불필요

        return organizedText;
      } else {
        // 에러 메시지 분석
        const errorMessage = organizeResult.error?.message || '정리에 실패했습니다.';
        const errorCode = organizeResult.error?.code || '';
        
        // 타임아웃 오류
        const isTimeout = errorCode === 'TIMEOUT_ERROR' || errorMessage.includes('timeout') || errorMessage.includes('Aborted');
        // OpenAI API 오류
        const isOpenAIError = errorMessage.includes('OpenAI') || errorMessage.includes('Retries exhausted');
        
        let alertMessage = errorMessage;
        if (isTimeout) {
          alertMessage = '요청 시간이 초과되었습니다.\n\nAI 처리가 오래 걸리고 있습니다. 잠시 후 다시 시도해주세요.';
        } else if (isOpenAIError) {
          alertMessage = 'AI 서비스에 일시적인 문제가 발생했습니다.\n\n잠시 후 다시 시도해주세요.';
        }
        
        Alert.alert(
          '정리 실패',
          alertMessage,
          [
            { text: '취소', style: 'cancel' },
            {
              text: '다시 시도',
              onPress: () => handleOrganizeReview(textOverride, transcriptionIdOverride, options),
            },
          ]
        );
      }
    } catch (error) {
      console.error('정리 요청 오류:', error);
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';
      const isTimeout = errorMessage.includes('timeout') || errorMessage.includes('Aborted');
      
      Alert.alert(
        '오류',
        isTimeout
          ? '요청 시간이 초과되었습니다.\n\n네트워크 연결을 확인하거나 잠시 후 다시 시도해주세요.'
          : '정리 요청 중 문제가 발생했습니다.\n\n네트워크 연결을 확인하거나 잠시 후 다시 시도해주세요.',
        [
          { text: '취소', style: 'cancel' },
          {
            text: '다시 시도',
            onPress: () => handleOrganizeReview(textOverride, transcriptionIdOverride, options),
          },
        ]
      );
      return undefined;
    } finally {
      setIsOrganizing(false);
    }
  };

  /** ===============================
   *         이미지 생성 페이지 이동
   *  =============================== */
  const handleSubmit = () => {
    navigation.navigate('ImageOptions', {
      ticketData,
      reviewData: { reviewText },
    });
  };


  const handleCloseCard = () => {
    Animated.parallel([
      Animated.timing(cardScale, {
        toValue: 0,
        duration: 300,
        useNativeDriver: false,
      }),
      Animated.timing(opacity, {
        toValue: 0,
        duration: 300,
        useNativeDriver: false,
      }),
      Animated.timing(cardHeight, {
        toValue: 0,
        duration: 300,
        useNativeDriver: false,
      }),
      Animated.timing(reviewTranslateY, {
        toValue: 44,
        duration: 300,
        useNativeDriver: false,
      }),
    ]).start(() => {
      setIsCardVisible(false);
    });
  };

  /** ===============================
   *                 UI
   *  =============================== */
  return (
    <SafeAreaView style={styles.container} edges={['left', 'right']}>
      {/* HEADER */}
      <ModalHeader
        title="후기 작성하기"
        onBack={() => navigation.goBack()}
        rightAction={{ text: '다음', onPress: handleSubmit }}
      />

      {/* 진행 표시기 */}
      {isCardVisible && !isLoadingQuestions && questions.length > 0 && (
        <View style={styles.progressDots}>
          {questions.map((_, i) => (
            <View
              key={i}
              style={[
                styles.progressDot,
                i === currentIndex && styles.progressDotActive,
              ]}
            />
          ))}
        </View>
      )}

      {/* 질문 카드 */}
      {isCardVisible && !isLoadingQuestions && questions.length > 0 && (
        <Animated.View
          style={[
            styles.questionSection,
            {
              height: cardHeight.interpolate({
                inputRange: [0, 1],
                outputRange: [0, 150],
              }),
              opacity,
            },
          ]}
        >
          <Animated.View
            style={[
              styles.animatedCard,
              { transform: [...pan.getTranslateTransform(), { scale: cardScale }] },
            ]}
            {...panResponder.panHandlers}
          >
            <View style={styles.questionCard}>
              <TouchableOpacity
                style={styles.closeButton}
                onPress={handleCloseCard}
              >
                <Text style={styles.closeButtonText}>✕</Text>
              </TouchableOpacity>
              <View style={styles.questionHeaderRow}>
                <View style={styles.questionIconContainer}>
                  <Text style={styles.questionIcon}>✈️</Text>
                </View>
                <View style={styles.textContainer}>
                  <Text style={styles.questionLabel}>질문 {currentIndex + 1}</Text>
                  <Text style={styles.questionText}>{questions[currentIndex]}</Text>
                </View>
              </View>
            </View>
          </Animated.View>
        </Animated.View>
      )}

      {/* Text Input */}
      <KeyboardAvoidingView
        style={styles.keyboardView}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.reviewContainer}>
          <TextInput
            style={styles.reviewInput}
            placeholder="후기를 입력하세요..."
            placeholderTextColor={Colors.tertiaryLabel}
            multiline
            value={reviewText || ''}
            onChangeText={(text) => {
              console.log('📝 TextInput onChangeText 호출:', text?.substring(0, 50));
              setReviewText(text);
            }}
            textAlignVertical="top"
            key={reviewText ? 'has-text' : 'empty'} // 강제 리렌더링을 위한 key
          />
        </View>
      </KeyboardAvoidingView>

      {/* 하단 버튼들 */}
      <View style={styles.bottomButtons}>
        <TouchableOpacity
          style={[styles.bottomButton, styles.bottomButtonWhite]}
          onPress={() => handleOrganizeReview()}
          disabled={!reviewText?.trim() || isOrganizing}
        >
          <Text style={styles.bottomButtonIcon}>🎫</Text>
          <Text style={[styles.bottomButtonText, styles.bottomButtonTextDark]}>
            후기 정리하기
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.bottomButton, styles.bottomButtonWhite]}
          onPress={handleAudioFilePick}
          disabled={isProcessingSTT}
        >
          <Text style={styles.bottomButtonIcon}>🎵</Text>
          <Text style={[styles.bottomButtonText, styles.bottomButtonTextDark]}>
            오디오 파일 선택
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.bottomButton,
            styles.bottomButtonPrimary,
            (isProcessingSTT || !selectedAudioUri) && styles.bottomButtonDisabled,
          ]}
          onPress={handleSTTConversion}
          disabled={isProcessingSTT || !selectedAudioUri}
        >
          <Text style={[styles.bottomButtonText, styles.bottomButtonTextWhite]}>
            STT 변환 실행
          </Text>
        </TouchableOpacity>
      </View>
        
      {/* 후기 요약/정리 모달 */}
      <ReviewSummaryModal
        visible={showSummaryModal}
        onClose={() => setShowSummaryModal(false)}
        summaryText={summaryText || "이곳에 요약/정리된 결과가 나옵니다."}
        title={modalTitle}
      />

    </SafeAreaView>
  );
};

/** ============================================
 *                  Styles
 *  ============================================ */
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.systemBackground },

  // 진행 표시기
  progressDots: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: Spacing.md,
    gap: Spacing.xs,
  },
  progressDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.systemGray4,
  },
  progressDotActive: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: Colors.label,
  },

  // 질문 섹션
  questionSection: {
    marginHorizontal: Spacing.xl,
    marginTop: Spacing.sm,
    marginBottom: Spacing.md,
  },
  animatedCard: {
    width: '100%',
  },
  questionCard: {
    width: '100%',
    backgroundColor: Colors.systemGray5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.lg,
    ...Shadows.small,
    position: 'relative',
  },
  closeButton: {
    position: 'absolute',
    top: Spacing.sm,
    right: Spacing.sm,
    width: 32,
    height: 32,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1,
  },
  closeButtonText: {
    fontSize: 20,
    color: Colors.secondaryLabel,
    fontWeight: '300',
  },
  questionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingRight: Spacing.xl,
  },
  questionIconContainer: {
    marginRight: Spacing.md,
    width: 32,
    height: 32,
    justifyContent: 'center',
    alignItems: 'center',
  },
  questionIcon: {
    fontSize: 20,
  },
  textContainer: {
    flex: 1,
  },
  questionLabel: {
    ...Typography.subheadline,
    fontWeight: '600',
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  questionText: {
    ...Typography.body,
    fontWeight: '500',
    color: Colors.label,
  },

  // 키보드 뷰
  keyboardView: {
    flex: 1,
  },

  // 후기 입력 영역
  reviewContainer: {
    flex: 1,
    marginHorizontal: Spacing.xl,
    marginTop: Spacing.md,
  },
  reviewInput: {
    flex: 1,
    backgroundColor: Colors.systemGray5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.lg,
    ...Typography.body,
    color: Colors.label,
    minHeight: 300,
  },

  // 하단 버튼들
  bottomButtons: {
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.lg,
    gap: Spacing.md,
    backgroundColor: Colors.systemBackground,
    borderTopWidth: 0.5,
    borderTopColor: Colors.systemGray5,
  },
  bottomButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.lg,
    paddingHorizontal: Spacing.xl,
    borderRadius: BorderRadius.lg,
    gap: Spacing.sm,
    ...Shadows.small,
  },
  bottomButtonWhite: {
    backgroundColor: Colors.white,
  },
  bottomButtonPrimary: {
    backgroundColor: Colors.primary,
  },
  bottomButtonDisabled: {
    opacity: 0.5,
  },
  bottomButtonIcon: {
    fontSize: 20,
  },
  bottomButtonText: {
    ...Typography.subheadline,
    fontWeight: '600',
  },
  bottomButtonTextDark: {
    color: Colors.label,
  },
  bottomButtonTextWhite: {
    color: Colors.white,
  },
});

export default AddReviewPage;
