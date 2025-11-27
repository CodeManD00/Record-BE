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

interface AnalyticsTabProps {
  navigation: any;
}

const AnalyticsTab: React.FC<AnalyticsTabProps> = ({ navigation }) => {
  const [userProfile] = useAtom(userProfileAtom);
  const [statistics, setStatistics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  useEffect(() => {
    loadStatistics();
  }, [selectedYear]);

  const loadStatistics = async () => {
    if (!userProfile?.id && !userProfile?.user_id) return;
    
    setLoading(true);
    const userId = userProfile?.id || userProfile?.user_id;
    const result = await ticketService.getTicketStatistics(userId, selectedYear);
    
    if (result.success && result.data) {
      setStatistics(result.data);
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

  if (!statistics) {
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
        <Text style={styles.yearText}>{selectedYear}년</Text>
        <TouchableOpacity
          onPress={() => setSelectedYear(selectedYear + 1)}
          style={styles.yearButton}
        >
          <Text style={styles.yearButtonText}>▶</Text>
        </TouchableOpacity>
      </View>

      {/* 총 관람 수 */}
      <View style={styles.statCard}>
        <Text style={styles.statLabel}>올해 총 관람 수</Text>
        <Text style={styles.statValue}>{statistics.totalCountThisYear || 0}회</Text>
        {statistics.yearOverYearChange !== undefined && (
          <Text style={[
            styles.statChange,
            statistics.yearOverYearChange >= 0 ? styles.positive : styles.negative
          ]}>
            작년 대비 {statistics.yearOverYearChange >= 0 ? '+' : ''}{statistics.yearOverYearChange}회
          </Text>
        )}
      </View>

      {/* 장르별 통계 */}
      {statistics.genreStatistics && statistics.genreStatistics.length > 0 && (
        <View style={styles.statCard}>
          <Text style={styles.cardTitle}>장르별 관람 비율</Text>
          {statistics.genreStatistics.map((genre: any, index: number) => (
            <View key={index} style={styles.genreRow}>
              <Text style={styles.genreName}>{genre.genre || '기타'}</Text>
              <View style={styles.progressBarContainer}>
                <View
                  style={[
                    styles.progressBar,
                    { width: `${genre.percentage || 0}%` }
                  ]}
                />
              </View>
              <Text style={styles.genrePercentage}>
                {genre.count}회 ({genre.percentage?.toFixed(1) || 0}%)
              </Text>
            </View>
          ))}
        </View>
      )}

      {/* TOP 공연장 */}
      {statistics.topVenues && statistics.topVenues.length > 0 && (
        <View style={styles.statCard}>
          <Text style={styles.cardTitle}>가장 많이 간 공연장 TOP 5</Text>
          {statistics.topVenues.slice(0, 5).map((venue: any, index: number) => (
            <View key={index} style={styles.rankingRow}>
              <Text style={styles.rankNumber}>{index + 1}</Text>
              <Text style={styles.rankingText}>{venue.venue}</Text>
              <Text style={styles.rankingCount}>{venue.count}회</Text>
            </View>
          ))}
        </View>
      )}

      {/* TOP 작품 */}
      {statistics.topPerformances && statistics.topPerformances.length > 0 && (
        <View style={styles.statCard}>
          <Text style={styles.cardTitle}>가장 많이 본 작품 TOP 5</Text>
          {statistics.topPerformances.slice(0, 5).map((perf: any, index: number) => (
            <View key={index} style={styles.rankingRow}>
              <Text style={styles.rankNumber}>{index + 1}</Text>
              <Text style={styles.rankingText} numberOfLines={1}>{perf.performanceTitle}</Text>
              <Text style={styles.rankingCount}>{perf.count}회</Text>
            </View>
          ))}
        </View>
      )}

      {/* 평일/주말 비율 */}
      {statistics.weekdayWeekendRatio && (
        <View style={styles.statCard}>
          <Text style={styles.cardTitle}>관람 패턴</Text>
          <View style={styles.ratioContainer}>
            <View style={styles.ratioItem}>
              <Text style={styles.ratioLabel}>평일</Text>
              <Text style={styles.ratioValue}>
                {statistics.weekdayWeekendRatio.weekdayCount || 0}회
              </Text>
              <Text style={styles.ratioPercentage}>
                ({statistics.weekdayWeekendRatio.weekdayPercentage?.toFixed(1) || 0}%)
              </Text>
            </View>
            <View style={styles.ratioItem}>
              <Text style={styles.ratioLabel}>주말</Text>
              <Text style={styles.ratioValue}>
                {statistics.weekdayWeekendRatio.weekendCount || 0}회
              </Text>
              <Text style={styles.ratioPercentage}>
                ({statistics.weekdayWeekendRatio.weekendPercentage?.toFixed(1) || 0}%)
              </Text>
            </View>
          </View>
        </View>
      )}

      {/* 상반기 vs 하반기 */}
      {statistics.halfYearComparison && (
        <View style={styles.statCard}>
          <Text style={styles.cardTitle}>상반기 vs 하반기</Text>
          <View style={styles.ratioContainer}>
            <View style={styles.ratioItem}>
              <Text style={styles.ratioLabel}>상반기</Text>
              <Text style={styles.ratioValue}>
                {statistics.halfYearComparison.firstHalfCount || 0}회
              </Text>
              <Text style={styles.ratioPercentage}>
                ({statistics.halfYearComparison.firstHalfPercentage?.toFixed(1) || 0}%)
              </Text>
            </View>
            <View style={styles.ratioItem}>
              <Text style={styles.ratioLabel}>하반기</Text>
              <Text style={styles.ratioValue}>
                {statistics.halfYearComparison.secondHalfCount || 0}회
              </Text>
              <Text style={styles.ratioPercentage}>
                ({statistics.halfYearComparison.secondHalfPercentage?.toFixed(1) || 0}%)
              </Text>
            </View>
          </View>
        </View>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  content: {
    flex: 1,
    padding: Spacing.lg,
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
    marginBottom: Spacing.lg,
    paddingVertical: Spacing.md,
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
  statCard: {
    backgroundColor: Colors.systemBackground,
    borderRadius: 12,
    padding: Spacing.lg,
    marginBottom: Spacing.md,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  statLabel: {
    ...Typography.subheadline,
    color: Colors.secondaryLabel,
    marginBottom: Spacing.xs,
  },
  statValue: {
    fontSize: 32,
    fontWeight: 'bold',
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  statChange: {
    ...Typography.caption1,
  },
  positive: {
    color: '#34C759',
  },
  negative: {
    color: '#FF3B30',
  },
  cardTitle: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.md,
  },
  genreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.sm,
  },
  genreName: {
    ...Typography.body,
    width: 80,
    color: Colors.label,
  },
  progressBarContainer: {
    flex: 1,
    height: 8,
    backgroundColor: Colors.tertiarySystemBackground,
    borderRadius: 4,
    marginHorizontal: Spacing.sm,
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: Colors.primary,
    borderRadius: 4,
  },
  genrePercentage: {
    ...Typography.caption1,
    width: 80,
    textAlign: 'right',
    color: Colors.secondaryLabel,
  },
  rankingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 0.5,
    borderBottomColor: Colors.separator,
  },
  rankNumber: {
    ...Typography.headline,
    width: 30,
    color: Colors.primary,
  },
  rankingText: {
    ...Typography.body,
    flex: 1,
    color: Colors.label,
  },
  rankingCount: {
    ...Typography.body,
    color: Colors.secondaryLabel,
  },
  ratioContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginTop: Spacing.md,
  },
  ratioItem: {
    alignItems: 'center',
  },
  ratioLabel: {
    ...Typography.subheadline,
    color: Colors.secondaryLabel,
    marginBottom: Spacing.xs,
  },
  ratioValue: {
    ...Typography.headline,
    color: Colors.label,
    marginBottom: Spacing.xs,
  },
  ratioPercentage: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
});

export default AnalyticsTab;

