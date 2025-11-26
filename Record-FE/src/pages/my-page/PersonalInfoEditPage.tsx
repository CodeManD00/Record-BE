//check
import { resolveImageUrl } from '../../utils/resolveImageUrl';

import { userService } from '../../services/api/userService';


import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  TextInput,
  Alert,
  Image,
  Switch,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { launchImageLibrary, ImagePickerResponse } from 'react-native-image-picker';
import { useAtom } from 'jotai';
import { userProfileAtom, updateUserProfileAtom } from '../../atoms/userAtoms';
import { changePasswordAtom } from '../../atoms/userAtomsApi';
import {
  Colors,
  Typography,
  Spacing,
  BorderRadius,
  Shadows,
} from '../../styles/designSystem';
import ModalHeader from '../../components/ModalHeader';
import { useUserProfileData } from '../../hooks/useApiData';
import apiClient from '../../services/api/client';
import { UserProfile } from '../../types/user';
import { fetchMyProfileAtom } from '../../atoms/userAtomsApi';

interface PersonalInfoEditPageProps {
  navigation: any;
}

const PersonalInfoEditPage: React.FC<PersonalInfoEditPageProps> = ({ navigation }) => {
  // 프로필 로드
  const { data: profile } = useUserProfileData({ fetchOnMount: true });
  const [localProfile] = useAtom(userProfileAtom);
  const [, updateUserProfile] = useAtom(updateUserProfileAtom);
  const [, fetchMyProfile] = useAtom(fetchMyProfileAtom);
  const [, changePassword] = useAtom(changePasswordAtom);

  const actualProfile = (profile || localProfile || {}) as UserProfile;

  // 상태
  const [profileImage, setProfileImage] = useState<string | null>(actualProfile.profileImage || null);
  const [profileImageFile, setProfileImageFile] = useState<any | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const [nickname, setNickname] = useState(actualProfile.nickname || '');
  const [userId, setUserId] = useState(actualProfile.id || '');
  const [email, setEmail] = useState(actualProfile.email || '');
  const [isAccountPrivate, setIsAccountPrivate] = useState(actualProfile.isAccountPrivate || false);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const resolvedImageUrl = resolveImageUrl(profileImage);

  // 프로필 변경 시 자동 반영
  useEffect(() => {
    if (!actualProfile) return;
    setProfileImage(actualProfile.profileImage || null);
    setNickname(actualProfile.nickname || '');
    setUserId(actualProfile.id || '');
    setEmail(actualProfile.email || '');
    setIsAccountPrivate(actualProfile.isAccountPrivate ?? false);
  }, [actualProfile]);


  // 이미지 선택
  // 이미지 선택 (압축 X, 원본 바로 사용)
  const handleProfileImagePick = () => {
    const options = {
      mediaType: 'photo' as const,
      includeBase64: false,
      quality: 0.8 as const,
      includeExtra: true,
      maxWidth: 1200,
      maxHeight: 1200,
      selectionLimit: 1,
    };

    launchImageLibrary(options, (response: ImagePickerResponse) => {
      if (response.didCancel || response.errorMessage) return;

      const asset = response.assets?.[0];
      if (!asset?.uri) return;

      if (asset.fileSize && asset.fileSize > 5 * 1024 * 1024) {
        Alert.alert('안내', '5MB 이하의 이미지만 업로드할 수 있습니다. 다른 이미지를 선택해주세요.');
        return;
      }

      // 미리보기 이미지 변경
      setProfileImage(asset.uri);

      // 업로드용 파일 설정
      setProfileImageFile({
        uri: asset.uri,
        type: asset.type || 'image/jpeg',
        name: asset.fileName || 'profile.jpg',
      });
    });
  };




  // 저장 처리
  const handleSave = async () => {
    if (isSaving) return;

    if (!nickname.trim()) {
      Alert.alert('오류', '닉네임을 입력해주세요.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      Alert.alert('오류', '올바른 이메일 형식을 입력하세요.');
      return;
    }

    setIsSaving(true);

    try {
      // 프로필 이미지 업로드 (있는 경우)
      if (profileImageFile) {
        if (!userId) {
          Alert.alert('오류', '사용자 아이디가 필요합니다.');
          setIsSaving(false);
          return;
        }
        const uploadResult = await userService.updateProfileImage(
          {
            uri: profileImageFile.uri,
            type: profileImageFile.type,
            name: profileImageFile.name,
          },
          userId
        );

        if (!uploadResult.success) {
          Alert.alert('오류', uploadResult.error?.message || '이미지 업로드 실패');
          setIsSaving(false);
          return;
        }

        // 이미지 업로드 성공 시 프로필이 자동으로 업데이트됨 (userService.updateProfileImage 내부에서 fetchMyProfile 호출)
        if (uploadResult.data) {
          await updateUserProfile(uploadResult.data);
        }
      }

      // 닉네임 변경 (PATCH /users/nickname 사용, JWT 토큰 불필요)
      if (nickname !== actualProfile.nickname) {
        if (!userId) {
          Alert.alert('오류', '사용자 아이디가 필요합니다.');
          setIsSaving(false);
          return;
        }
        const nicknameResult = await userService.updateNickname(userId, nickname);
        if (!nicknameResult.success) {
          Alert.alert('오류', nicknameResult.error?.message || '닉네임 변경에 실패했습니다.');
          setIsSaving(false);
          return;
        }
        if (nicknameResult.data) {
          await updateUserProfile(nicknameResult.data);
        }
      }

      // 이메일 또는 공개 설정 변경 (PUT /users/me 사용)
      if (
        email !== actualProfile.email ||
        isAccountPrivate !== actualProfile.isAccountPrivate
      ) {
        const payload: any = {
          email,
          isAccountPrivate,
        };

        const updateResult = await apiClient.put<UserProfile>('/users/me', payload);
        if (!updateResult.success) {
          Alert.alert('오류', updateResult.error?.message || '프로필 정보를 갱신할 수 없습니다.');
          setIsSaving(false);
          return;
        }

        if (updateResult.data) {
          await updateUserProfile(updateResult.data);
        }
      }

      // 비밀번호 변경 (입력된 경우에만)
      if (currentPassword && newPassword && confirmPassword) {
        if (newPassword.length < 8) {
          Alert.alert('오류', '새 비밀번호는 8자 이상이어야 합니다.');
          setIsSaving(false);
          return;
        }

        if (newPassword !== confirmPassword) {
          Alert.alert('오류', '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
          setIsSaving(false);
          return;
        }

        if (!userId) {
          Alert.alert('오류', '사용자 ID가 필요합니다.');
          setIsSaving(false);
          return;
        }

        const passwordResult = await changePassword({
          userId: userId,
          oldPassword: currentPassword,
          newPassword: newPassword,
        });

        if (!passwordResult.success) {
          Alert.alert('오류', passwordResult.error?.message || '비밀번호 변경에 실패했습니다.');
          setIsSaving(false);
          return;
        }

        // 비밀번호 변경 성공 시 필드 초기화
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
      }

      // 최종 프로필 정보 동기화
      await userService.fetchMyProfile();
      
      // useUserProfileData가 사용하는 atom도 강제로 새로고침
      await fetchMyProfile(true);

    Alert.alert('완료', '개인정보가 수정되었습니다.', [
      { text: '확인', onPress: () => navigation.goBack() },
    ]);
    } catch (error) {
      console.error('프로필 저장 오류:', error);
      Alert.alert('오류', '저장 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsSaving(false);
    }
  };

  // 필드
  const editFields = [
    { id: 1, label: '닉네임', value: nickname, setter: setNickname, placeholder: '닉네임을 입력하세요' },
    { id: 2, label: '아이디', value: userId, setter: setUserId, placeholder: '아이디', editable: false },
    { id: 3, label: '이메일', value: email, setter: setEmail, placeholder: '이메일을 입력하세요' },
  ];

  const passwordFields = [
    { id: 1, label: '현재 비밀번호', value: currentPassword, setter: setCurrentPassword, secure: true },
    { id: 2, label: '새 비밀번호', value: newPassword, setter: setNewPassword, secure: true },
    { id: 3, label: '비밀번호 확인', value: confirmPassword, setter: setConfirmPassword, secure: true },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <ModalHeader
        title="개인정보 수정"
        onBack={() => navigation.goBack()}
        rightAction={{
          text: isSaving ? '저장 중...' : '저장',
          onPress: handleSave,
          disabled: isSaving,
        }}
      />

      <ScrollView style={styles.content}>
        <View style={styles.formContainer}>

          {/* 프로필 사진 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>프로필 사진</Text>

            <TouchableOpacity style={styles.profileImageWrapper} onPress={handleProfileImagePick}>
              {resolvedImageUrl ? (
                <Image source={{ uri: resolvedImageUrl }} style={styles.profileImage} />
              ) : (
                <View style={styles.defaultProfileImage}>
                  <Text style={styles.defaultProfileImageText}>👤</Text>
                </View>
              )}

              <View style={styles.editImageOverlay}>
                <Text style={styles.editImageText}>✏️</Text>
              </View>
            </TouchableOpacity>
          </View>

          {/* 기본 정보 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>기본 정보</Text>

            {editFields.map((f) => (
              <View key={f.id} style={styles.fieldItem}>
                <Text style={styles.fieldLabel}>{f.label}</Text>
                <TextInput
                  style={styles.textInput}
                  value={f.value}
                  editable={f.editable !== false}
                  onChangeText={f.setter}
                  placeholder={f.placeholder}
                  placeholderTextColor={Colors.tertiaryLabel}
                />
              </View>
            ))}
          </View>

          {/* 계정 공개 설정 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>계정 공개 설정</Text>

            <View style={styles.privacyItem}>
              <View style={styles.privacyTextBox}>
                <Text style={styles.privacyTitle}>
                  {isAccountPrivate ? '비공개 계정' : '공개 계정'}
                </Text>
                <Text style={styles.privacyDescription}>
                  {isAccountPrivate
                    ? '승인된 사용자만 프로필을 볼 수 있습니다.'
                    : '모든 사용자가 프로필을 볼 수 있습니다.'}
                </Text>
              </View>

              <Switch
                value={isAccountPrivate}
                onValueChange={setIsAccountPrivate}
                trackColor={{ false: Colors.systemGray4, true: Colors.primary }}
                thumbColor={Colors.white}
              />
            </View>
          </View>

          {/* 비밀번호 변경 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>비밀번호 변경</Text>

            {passwordFields.map((f) => (
              <View key={f.id} style={styles.fieldItem}>
                <Text style={styles.fieldLabel}>{f.label}</Text>
                <TextInput
                  style={styles.textInput}
                  secureTextEntry={f.secure}
                  value={f.value}
                  onChangeText={f.setter}
                  placeholder={f.label}
                  placeholderTextColor={Colors.tertiaryLabel}
                />
              </View>
            ))}
          </View>

        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

// 🔥🔥🔥 스타일이 없어서 오류가 난 것이므로 반드시 포함해야 한다!!
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.systemBackground },
  content: { flex: 1 },

  formContainer: { padding: Spacing.xl, gap: Spacing.xl },

  section: { marginBottom: Spacing.xl },

  sectionTitle: {
    ...Typography.title3,
    fontWeight: '600',
    marginBottom: Spacing.md,
    color: Colors.label,
  },

  profileImageWrapper: {
    alignSelf: 'center',
    width: 140,
    height: 140,
    borderRadius: 70,
    overflow: 'hidden',
    backgroundColor: Colors.systemGray5,
  },
  profileImage: { width: '100%', height: '100%' },

  defaultProfileImage: {
    width: '100%',
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.systemGray5,
  },
  defaultProfileImageText: { fontSize: 48 },

  editImageOverlay: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    padding: 6,
    borderRadius: 20,
    backgroundColor: Colors.systemBackground,
    ...Shadows.small,
  },
  editImageText: { fontSize: 16 },

  fieldItem: { marginBottom: Spacing.lg },
  fieldLabel: {
    ...Typography.subheadline,
    marginBottom: Spacing.xs,
    color: Colors.secondaryLabel,
  },
  textInput: {
    ...Typography.body,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.lg,
    backgroundColor: Colors.secondarySystemBackground,
    borderRadius: BorderRadius.md,
    color: Colors.label,
  },

  privacyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },

  privacyTextBox: { flex: 1, paddingRight: Spacing.lg },
  privacyTitle: {
    ...Typography.headline,
    color: Colors.label,
  },
  privacyDescription: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
});

export default PersonalInfoEditPage;
