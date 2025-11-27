import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
} from 'react-native';
import { useAtom } from 'jotai';
import { userProfileAtom } from '../../../atoms/userAtomsApi';
import { ticketService } from '../../../services/api/ticketService';
import { Colors, Typography, Spacing } from '../../../styles/designSystem';

interface YearInReviewTabProps {
  navigation: any;
}

const YearInReviewTab: React.FC<YearInReviewTabProps> = ({ navigation }) => {
  const [userProfile] = useAtom(userProfileAtom);
  const [review, setReview] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  useEffect(() => {
    loadYearInReview();
  }, [selectedYear]);

  const loadYearInReview = async () => {
    if (!userProfile?.id && !userProfile?.user_id) return;
    
    setLoading(true);
    const userId = userProfile?.id || userProfile?.user_id;
    const result = await ticketService.getYearInReview(userId, selectedYear);
    
    if (result.success && result.data) {
      setReview(result.data);
    }
    setLoading(false);
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  if (!review) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>데이터가 없습니다</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
      {/* 연도 선택 */}
      <View style={styles.yearSelector}>
        <TouchableOpacity
          onPress={() => setSelectedYear(selectedYear - 1)}
          style={styles.yearButton}
        >
          <Text style={styles.yearButtonText}>◀</Text>
        </TouchableOpacity>
        <Text style={styles.yearText}>{selectedYear}년 결산</Text>
        <TouchableOpacity
          onPress={() => setSelectedYear(selectedYear + 1)}
          style={styles.yearButton}
        >
          <Text style={styles.yearButtonText}>▶</Text>
        </TouchableOpacity>
      </View>

      {/* 헤로 섹션 */}
      <View style={styles.heroSection}>
        <Text style={styles.heroTitle}>
          {selectedYear}년,{'\n'}
          당신의 공연 라이프
        </Text>
        <Text style={styles.heroSubtitle}>
          총 {review.totalCount || 0}회의 관람 기록
        </Text>
        {review.yearComparison && (
          <View style={styles.comparisonBadge}>
            <Text style={styles.comparisonText}>
              작년 대비 {review.yearComparison.change >= 0 ? '+' : ''}
              {review.yearComparison.change}회 ({review.yearComparison.trend})
            </Text>
          </View>
        )}
      </View>

      {/* 장르 TOP 3 */}
      {review.topGenres && review.topGenres.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>올해 내가 가장 자주 본 장르 TOP 3</Text>
          {review.topGenres.map((genre: any, index: number) => (
            <View key={index} style={styles.genreCard}>
              <View style={styles.genreRank}>
                <Text style={styles.rankNumber}>{index + 1}</Text>
              </View>
              <View style={styles.genreInfo}>
                <Text style={styles.genreName}>{genre.genre}</Text>
                <Text style={styles.genreCount}>
                  {genre.count}회 ({genre.percentage?.toFixed(1) || 0}%)
                </Text>
              </View>
            </View>
          ))}
        </View>
      )}

      {/* 주요 통계 */}
      <View style={styles.statsGrid}>
        {review.mostVisitedVenue && (
          <View style={styles.statBox}>
            <Text style={styles.statBoxLabel}>가장 많이 간 공연장</Text>
            <Text style={styles.statBoxValue} numberOfLines={2}>
              {review.mostVisitedVenue}
            </Text>
          </View>
        )}
        {review.mostWatchedPerformance && (
          <View style={styles.statBox}>
            <Text style={styles.statBoxLabel}>가장 많이 본 작품</Text>
            <Text style={styles.statBoxValue} numberOfLines={2}>
              {review.mostWatchedPerformance}
            </Text>
          </View>
        )}
        {review.mostWatchedArtist && (
          <View style={styles.statBox}>
            <Text style={styles.statBoxLabel}>가장 많이 본 아티스트</Text>
            <Text style={styles.statBoxValue} numberOfLines={2}>
              {review.mostWatchedArtist}
            </Text>
          </View>
        )}
      </View>

      {/* 관람 패턴 */}
      {review.dayOfWeekStatistics && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>관람 패턴</Text>
          <View style={styles.patternCard}>
            <Text style={styles.patternText}>
              {review.dayOfWeekStatistics.preferredDayType}을 선호하는 관람자
            </Text>
            <View style={styles.patternDetails}>
              <Text style={styles.patternDetail}>
                평일: {review.dayOfWeekStatistics.weekdayCount || 0}회
              </Text>
              <Text style={styles.patternDetail}>
                주말: {review.dayOfWeekStatistics.weekendCount || 0}회
              </Text>
            </View>
          </View>
        </View>
      )}

      {/* 상반기 vs 하반기 */}
      {review.halfYearPattern && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>관람 패턴 변화</Text>
          <View style={styles.patternCard}>
            <Text style={styles.patternText}>{review.halfYearPattern.pattern}</Text>
            <View style={styles.patternDetails}>
              <Text style={styles.patternDetail}>
                상반기: {review.halfYearPattern.firstHalfCount || 0}회
              </Text>
              <Text style={styles.patternDetail}>
                하반기: {review.halfYearPattern.secondHalfCount || 0}회
              </Text>
            </View>
          </View>
        </View>
      )}

      {/* 스페셜 포인트 */}
      {review.specialPoints && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>스페셜 포인트</Text>
          {review.specialPoints.firstTicket && (
            <View style={styles.specialCard}>
              <Text style={styles.specialLabel}>올해의 첫 공연</Text>
              <Text style={styles.specialTitle}>
                {review.specialPoints.firstTicket.performanceTitle}
              </Text>
              <Text style={styles.specialDate}>
                {review.specialPoints.firstTicket.viewDate}
              </Text>
            </View>
          )}
          {review.specialPoints.lastTicket && (
            <View style={styles.specialCard}>
              <Text style={styles.specialLabel}>올해의 마지막 공연</Text>
              <Text style={styles.specialTitle}>
                {review.specialPoints.lastTicket.performanceTitle}
              </Text>
              <Text style={styles.specialDate}>
                {review.specialPoints.lastTicket.viewDate}
              </Text>
            </View>
          )}
          {review.specialPoints.mostMemorableTicket && (
            <View style={styles.specialCard}>
              <Text style={styles.specialLabel}>가장 기억에 남는 후기</Text>
              <Text style={styles.specialTitle}>
                {review.specialPoints.mostMemorableTicket.performanceTitle}
              </Text>
              {review.specialPoints.mostMemorableTicket.reviewPreview && (
                <Text style={styles.specialReview}>
                  "{review.specialPoints.mostMemorableTicket.reviewPreview}"
                </Text>
              )}
            </View>
          )}
        </View>
      )}

      {/* 공연 소비 성향 유형 */}
      {review.consumptionType && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>나만의 공연 소비 성향</Text>
          <View style={styles.typeCard}>
            <Text style={styles.typeName}>{review.consumptionType.type}</Text>
            <Text style={styles.typeDescription}>
              {review.consumptionType.description}
            </Text>
          </View>
        </View>
      )}

      {/* 가장 사랑한 아티스트 */}
      {review.favoriteArtists && review.favoriteArtists.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>올해 내가 가장 사랑한 아티스트 TOP 5</Text>
          {review.favoriteArtists.map((artist: any, index: number) => (
            <View key={index} style={styles.artistRow}>
              <Text style={styles.artistRank}>{index + 1}</Text>
              <View style={styles.artistInfo}>
                <Text style={styles.artistName}>{artist.artist}</Text>
                <Text style={styles.artistCount}>
                  {artist.count}회 ({artist.percentage?.toFixed(1) || 0}%)
                </Text>
              </View>
            </View>
          ))}
        </View>
      )}

      {/* 공연 수집 성향 */}
      {review.collectionMetrics && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>공연 수집 성향</Text>
          <View style={styles.collectionCard}>
            <Text style={styles.collectionText}>
              이미지 생성: {review.collectionMetrics.totalImageGenerations || 0}회
            </Text>
            <Text style={styles.collectionText}>
              이미지 생성률: {review.collectionMetrics.imageGenerationRate?.toFixed(1) || 0}%
            </Text>
          </View>
        </View>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  content: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    ...Typography.body,
    color: Colors.secondaryLabel,
  },
  yearSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.md,
    backgroundColor: Colors.systemBackground,
    borderBottomWidth: 0.5,
    borderBottomColor: Colors.separator,
  },
  yearButton: {
    padding: Spacing.sm,
  },
  yearButtonText: {
    fontSize: 18,
    color: Colors.primary,
  },
  yearText: {
    ...Typography.headline,
    marginHorizontal: Spacing.lg,
  },
  heroSection: {
    backgroundColor: Colors.systemBackground,
    padding: Spacing.xl,
    alignItems: 'center',
    borderBottomWidth: 0.5,
    borderBottomColor: Colors.separator,
  },
  heroTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: Colors.label,
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  heroSubtitle: {
    ...Typography.subheadline,
    color: Colors.secondaryLabel,
    marginBottom: Spacing.md,
  },
  comparisonBadge: {
    backgroundColor: Colors.tertiarySystemBackground,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderRadius: 16,
  },
  comparisonText: {
    ...Typography.caption1,
    color: Colors.label,
  },
  section: {
    backgroundColor: Colors.systemBackground,
    padding: Spacing.lg,
    marginTop: Spacing.md,
    borderTopWidth: 0.5,
    borderBottomWidth: 0.5,
    borderColor: Colors.separator,
  },
  sectionTitle: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.md,
  },
  genreCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.tertiarySystemBackground,
    padding: Spacing.md,
    borderRadius: 12,
    marginBottom: Spacing.sm,
  },
  genreRank: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: Spacing.md,
  },
  rankNumber: {
    ...Typography.headline,
    color: Colors.systemBackground,
  },
  genreInfo: {
    flex: 1,
  },
  genreName: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  genreCount: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: Spacing.md,
    backgroundColor: Colors.systemBackground,
  },
  statBox: {
    width: '33.33%',
    padding: Spacing.md,
    alignItems: 'center',
  },
  statBoxLabel: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
    marginBottom: Spacing.xs,
    textAlign: 'center',
  },
  statBoxValue: {
    ...Typography.subheadline,
    color: Colors.label,
    textAlign: 'center',
  },
  patternCard: {
    backgroundColor: Colors.tertiarySystemBackground,
    padding: Spacing.md,
    borderRadius: 12,
  },
  patternText: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.sm,
  },
  patternDetails: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  patternDetail: {
    ...Typography.body,
    color: Colors.secondaryLabel,
  },
  specialCard: {
    backgroundColor: Colors.tertiarySystemBackground,
    padding: Spacing.md,
    borderRadius: 12,
    marginBottom: Spacing.sm,
  },
  specialLabel: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
    marginBottom: Spacing.xs,
  },
  specialTitle: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  specialDate: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
  specialReview: {
    ...Typography.body,
    color: Colors.label,
    fontStyle: 'italic',
    marginTop: Spacing.xs,
  },
  typeCard: {
    backgroundColor: Colors.tertiarySystemBackground,
    padding: Spacing.lg,
    borderRadius: 12,
    alignItems: 'center',
  },
  typeName: {
    fontSize: 24,
    fontWeight: 'bold',
    color: Colors.primary,
    marginBottom: Spacing.sm,
  },
  typeDescription: {
    ...Typography.body,
    color: Colors.label,
    textAlign: 'center',
    lineHeight: 24,
  },
  artistRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 0.5,
    borderBottomColor: Colors.separator,
  },
  artistRank: {
    ...Typography.headline,
    width: 30,
    color: Colors.primary,
  },
  artistInfo: {
    flex: 1,
  },
  artistName: {
    ...Typography.body,
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  artistCount: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
  collectionCard: {
    backgroundColor: Colors.tertiarySystemBackground,
    padding: Spacing.md,
    borderRadius: 12,
  },
  collectionText: {
    ...Typography.body,
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
});

export default YearInReviewTab;


