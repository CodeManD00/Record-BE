import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  ScrollView,
  Animated,
  Dimensions,
} from 'react-native';
import { Ticket } from '../../../types/ticket';
import { ticketService } from '../../../services/api/ticketService';
import { userProfileAtom } from '../../../atoms/userAtomsApi';
import { useAtom } from 'jotai';
import { Colors, Typography, Spacing, BorderRadius } from '../../../styles/designSystem';

interface SearchTabProps {
  tickets: Ticket[];
  navigation: any;
}

const { height: SCREEN_HEIGHT } = Dimensions.get('window');

const SearchTab: React.FC<SearchTabProps> = ({ tickets, navigation }) => {
  const [userProfile] = useAtom(userProfileAtom);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Ticket[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  
  // 필터 상태
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [selectedGenre, setSelectedGenre] = useState<string>('');
  const [venueQuery, setVenueQuery] = useState('');
  const [artistQuery, setArtistQuery] = useState('');
  
  // 스크롤 애니메이션
  const scrollY = useRef(new Animated.Value(0)).current;
  const filterSectionHeight = 400; // 필터 섹션 예상 높이

  const genres = ['BAND', 'MUSICAL', 'PLAY'];

  const handleSearch = async () => {
    if (!userProfile?.id && !userProfile?.user_id) {
      return;
    }

    setIsSearching(true);
    setHasSearched(true);
    const userId = userProfile?.id || userProfile?.user_id;

    try {
      const result = await ticketService.searchTickets(userId, {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        genre: selectedGenre || undefined,
        venue: venueQuery || undefined,
        artist: artistQuery || undefined,
        performanceTitle: searchQuery || undefined,
        sortBy: 'viewDate',
        sortDirection: 'DESC',
      });

      if (result.success && result.data) {
        setSearchResults(result.data);
      } else {
        setSearchResults([]);
      }
    } catch (error) {
      console.error('검색 오류:', error);
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const renderTicketItem = ({ item }: { item: Ticket }) => (
    <TouchableOpacity style={styles.ticketItem}>
      <View style={styles.ticketHeader}>
        <Text style={styles.ticketTitle} numberOfLines={1}>
          {item.title || '제목 없음'}
        </Text>
        <Text style={styles.ticketDate}>
          {item.performedAt ? new Date(item.performedAt).toLocaleDateString('ko-KR') : '날짜 없음'}
        </Text>
      </View>
      <Text style={styles.ticketLocation} numberOfLines={1}>
        📍 {item.venue || '장소 없음'}
      </Text>
      {item.artist && (
        <Text style={styles.ticketArtist} numberOfLines={1}>
          🎭 {item.artist}
        </Text>
      )}
      {item.genre && (
        <Text style={styles.ticketGenre}>
          🎪 {item.genre}
        </Text>
      )}
    </TouchableOpacity>
  );

  // 필터 섹션 애니메이션 스타일
  const filterTranslateY = scrollY.interpolate({
    inputRange: [0, filterSectionHeight],
    outputRange: [0, -filterSectionHeight],
    extrapolate: 'clamp',
  });

  const filterOpacity = scrollY.interpolate({
    inputRange: [0, filterSectionHeight * 0.5],
    outputRange: [1, 0],
    extrapolate: 'clamp',
  });

  return (
    <View style={styles.container}>
      <Animated.ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        onScroll={Animated.event(
          [{ nativeEvent: { contentOffset: { y: scrollY } } }],
          { useNativeDriver: true }
        )}
        scrollEventThrottle={16}
      >
        {/* 검색 입력 및 필터 섹션 */}
        <Animated.View
          style={[
            styles.filterContainer,
            {
              transform: [{ translateY: filterTranslateY }],
              opacity: filterOpacity,
            },
          ]}
        >
          {/* 검색 입력 */}
          <View style={styles.searchSection}>
            <TextInput
              style={styles.searchInput}
              placeholder="작품명으로 검색..."
              value={searchQuery}
              onChangeText={setSearchQuery}
              placeholderTextColor={Colors.secondaryLabel}
            />
          </View>

          {/* 필터 섹션 */}
          <View style={styles.filterSection}>
            <Text style={styles.filterTitle}>날짜 범위</Text>
            <View style={styles.dateRow}>
              <TextInput
                style={styles.dateInput}
                placeholder="시작일 (YYYY-MM-DD)"
                value={startDate}
                onChangeText={setStartDate}
                placeholderTextColor={Colors.secondaryLabel}
              />
              <Text style={styles.dateSeparator}>~</Text>
              <TextInput
                style={styles.dateInput}
                placeholder="종료일 (YYYY-MM-DD)"
                value={endDate}
                onChangeText={setEndDate}
                placeholderTextColor={Colors.secondaryLabel}
              />
            </View>

            <Text style={styles.filterTitle}>장르</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.genreContainer}>
              <TouchableOpacity
                style={[styles.genreChip, !selectedGenre && styles.genreChipActive]}
                onPress={() => setSelectedGenre('')}
              >
                <Text style={[styles.genreChipText, !selectedGenre && styles.genreChipTextActive]}>
                  전체
                </Text>
              </TouchableOpacity>
              {genres.map((genre) => (
                <TouchableOpacity
                  key={genre}
                  style={[styles.genreChip, selectedGenre === genre && styles.genreChipActive]}
                  onPress={() => setSelectedGenre(genre)}
                >
                  <Text style={[styles.genreChipText, selectedGenre === genre && styles.genreChipTextActive]}>
                    {genre === 'BAND' ? '밴드' : genre === 'MUSICAL' ? '뮤지컬' : '연극'}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            <Text style={styles.filterTitle}>공연장</Text>
            <TextInput
              style={styles.filterInput}
              placeholder="공연장명으로 검색..."
              value={venueQuery}
              onChangeText={setVenueQuery}
              placeholderTextColor={Colors.secondaryLabel}
            />

            <Text style={styles.filterTitle}>출연진</Text>
            <TextInput
              style={styles.filterInput}
              placeholder="출연진명으로 검색..."
              value={artistQuery}
              onChangeText={setArtistQuery}
              placeholderTextColor={Colors.secondaryLabel}
            />
          </View>

          {/* 검색 버튼 */}
          <TouchableOpacity
            style={styles.searchButton}
            onPress={handleSearch}
            disabled={isSearching}
          >
            <Text style={styles.searchButtonText}>
              {isSearching ? '검색 중...' : '검색'}
            </Text>
          </TouchableOpacity>
        </Animated.View>

        {/* 검색 결과 */}
        {hasSearched && (
          <View style={styles.resultsContainer}>
            {searchResults.length > 0 ? (
              <>
                <Text style={styles.resultsTitle}>
                  검색 결과 ({searchResults.length}개)
                </Text>
                <FlatList
                  data={searchResults}
                  renderItem={renderTicketItem}
                  keyExtractor={(item) => item.id}
                  showsVerticalScrollIndicator={false}
                  contentContainerStyle={styles.resultsList}
                  scrollEnabled={false}
                />
              </>
            ) : (
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyText}>검색 결과가 없습니다</Text>
                <Text style={styles.emptySubtext}>
                  다른 조건으로 검색해보세요
                </Text>
              </View>
            )}
          </View>
        )}
      </Animated.ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  filterContainer: {
    padding: Spacing.md,
    minHeight: 400,
  },
  searchSection: {
    marginBottom: Spacing.md,
  },
  searchInput: {
    ...Typography.body,
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.lg,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.separator,
  },
  filterSection: {
    marginBottom: Spacing.md,
  },
  filterTitle: {
    ...Typography.subheadline,
    fontWeight: '600',
    marginTop: Spacing.md,
    marginBottom: Spacing.xs,
  },
  dateRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  dateInput: {
    flex: 1,
    ...Typography.body,
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.md,
    padding: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.separator,
  },
  dateSeparator: {
    ...Typography.body,
    color: Colors.secondaryLabel,
  },
  genreContainer: {
    marginVertical: Spacing.xs,
  },
  genreChip: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.lg,
    backgroundColor: Colors.tertiarySystemBackground,
    marginRight: Spacing.xs,
  },
  genreChipActive: {
    backgroundColor: Colors.primary,
  },
  genreChipText: {
    ...Typography.subheadline,
    color: Colors.secondaryLabel,
  },
  genreChipTextActive: {
    color: Colors.systemBackground,
  },
  filterInput: {
    ...Typography.body,
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.md,
    padding: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.separator,
  },
  searchButton: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.lg,
    padding: Spacing.md,
    alignItems: 'center',
    marginTop: Spacing.md,
  },
  searchButtonText: {
    ...Typography.headline,
    color: Colors.systemBackground,
    fontWeight: '600',
  },
  resultsContainer: {
    minHeight: SCREEN_HEIGHT * 0.6,
    borderTopWidth: 1,
    borderTopColor: Colors.separator,
    padding: Spacing.md,
    paddingTop: Spacing.lg,
  },
  resultsTitle: {
    ...Typography.headline,
    marginBottom: Spacing.sm,
  },
  resultsList: {
    gap: Spacing.sm,
  },
  ticketItem: {
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.lg,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
  },
  ticketHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: Spacing.xs,
  },
  ticketTitle: {
    ...Typography.headline,
    flex: 1,
    marginRight: Spacing.sm,
  },
  ticketDate: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
  ticketLocation: {
    ...Typography.body,
    color: Colors.label,
    marginBottom: Spacing.xs / 2,
  },
  ticketArtist: {
    ...Typography.body,
    color: Colors.label,
    marginBottom: Spacing.xs / 2,
  },
  ticketGenre: {
    ...Typography.caption1,
    color: Colors.secondaryLabel,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    ...Typography.headline,
    marginBottom: Spacing.xs,
  },
  emptySubtext: {
    ...Typography.body,
    color: Colors.secondaryLabel,
  },
});

export default SearchTab;

