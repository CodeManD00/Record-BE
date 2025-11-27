import React, { useEffect, useMemo, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  Dimensions,
  ScrollView,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import PagerView, { PagerViewOnPageSelectedEvent } from 'react-native-pager-view';
import TicketDetailModal from './TicketDetailModal';
import CustomCalendar from './CustomCalendar';
import EventsList from './EventsList';
import TicketGrid from './TicketGrid';
import { Friend } from '../types/friend';
import { Ticket } from '../types/ticket';
import { FriendProfileScreenProps } from '../types/navigation';
import { useAtom } from 'jotai';
import { fetchFriendTicketsAtom, friendTicketsMapAtom } from '../atoms/ticketsAtomsApi';
import { isPlaceholderTicket } from '../utils/isPlaceholder';
import { resolveImageUrl } from '../utils/resolveImageUrl';
import { Colors, Typography, Spacing, BorderRadius, Shadows } from '../styles/designSystem';

const { width } = Dimensions.get('window');

const FriendProfilePage: React.FC<FriendProfileScreenProps> = ({ navigation, route }) => {
  const { friend } = route.params;
  const insets = useSafeAreaInsets();
  const [friendTicketsMap] = useAtom(friendTicketsMapAtom);
  const [, fetchFriendTickets] = useAtom(fetchFriendTicketsAtom);
  const [selectedDate, setSelectedDate] = React.useState(
    new Date().toISOString().split('T')[0],
  );
  const [isModalVisible, setIsModalVisible] = React.useState(false);
  const [selectedTicket, setSelectedTicket] = React.useState<Ticket | null>(
    null,
  );
  const [currentPage, setCurrentPage] = React.useState(0);

  const pagerRef = useRef<PagerView>(null);

  useEffect(() => {
    fetchFriendTickets(friend.id);
  }, [fetchFriendTickets, friend.id]);

  const friendTickets = friendTicketsMap.get(friend.id) ?? [];

  const realFriendTickets = useMemo(
    () =>
      friendTickets
        .filter(ticket => !isPlaceholderTicket(ticket))
        .sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return dateB - dateA;
        }),
    [friendTickets],
  );

  const formatDate = (date: Date | string) => {
    const d = date instanceof Date ? date : new Date(date);
    return d.toISOString().split('T')[0];
  };

  const selectedEvents = useMemo(
    () =>
      realFriendTickets.filter(
        ticket => {
          const ticketDate = formatDate(ticket.performedAt);
          return ticketDate === selectedDate;
        },
      ),
    [realFriendTickets, selectedDate],
  );
  
  // 디버그 로그
  useEffect(() => {
    console.log('🔍 FriendProfilePage - friendTickets:', friendTickets.length);
    console.log('🔍 FriendProfilePage - realFriendTickets:', realFriendTickets.length);
    console.log('🔍 FriendProfilePage - friendTickets 데이터:', friendTickets);
    console.log('🔍 FriendProfilePage - realFriendTickets 데이터:', realFriendTickets);
  }, [friendTickets, realFriendTickets]);

  const handleTicketPress = (ticket: Ticket) => {
    setSelectedTicket(ticket);
    setIsModalVisible(true);
  };

  const handleCloseModal = () => {
    setIsModalVisible(false);
    setSelectedTicket(null);
  };

  const handleDayPress = (day: { dateString: string }) => {
    setSelectedDate(day.dateString);
  };

  const handleTabPress = (pageIndex: number) => {
    setCurrentPage(pageIndex);
    pagerRef.current?.setPage(pageIndex);
  };

  const handlePageSelected = (e: any) => {
    setCurrentPage(e.nativeEvent.position);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backButtonText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{friend.nickname || friend.user_id}</Text>
        <View style={styles.placeholder} />
      </View>

      {/* 프로필 + 탭 + PagerView */}
      <View style={styles.mainContent}>
        <View style={styles.profileSection}>
          <Image 
            source={{ uri: resolveImageUrl(friend.profileImage) || undefined }} 
            style={styles.profileAvatar} 
          />
          <View style={styles.badgeWrapper}>
            <Text style={styles.badgeEmoji}>🎟️</Text>
            <Text style={styles.badgeText}>{realFriendTickets.length}</Text>
          </View>
          <Text style={styles.profileName}>{friend.nickname || '닉네임 없음'}</Text>
          <Text style={styles.profileUsername}>@{friend.user_id || friend.id}</Text>
        </View>

        {/* 탭 */}
        <View style={styles.tabContainer}>
          <TouchableOpacity
            style={[styles.tabButton, currentPage === 0 && styles.activeTab]}
            onPress={() => handleTabPress(0)}
          >
            <Text
              style={[
                styles.tabText,
                currentPage === 0 && styles.activeTabText,
              ]}
            >
              티켓
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tabButton, currentPage === 1 && styles.activeTab]}
            onPress={() => handleTabPress(1)}
          >
            <Text
              style={[
                styles.tabText,
                currentPage === 1 && styles.activeTabText,
              ]}
            >
              캘린더
            </Text>
          </TouchableOpacity>
        </View>

        <PagerView
          ref={pagerRef}
          style={styles.pager}
          initialPage={0}
          onPageSelected={handlePageSelected}
        >
          {/* 피드 탭 */}
          <View key="feed" style={styles.pageContainer}>
            <ScrollView
              style={styles.feedScrollView}
              contentContainerStyle={styles.feedContent}
              showsVerticalScrollIndicator={false}
            >
              {realFriendTickets.length > 0 ? (
                <TicketGrid
                  tickets={realFriendTickets}
                  onTicketPress={handleTicketPress}
                  containerStyle={styles.friendGridContainer}
                  cardWidth={(width - 15) / 3}
                  cardAspectRatio={1.4}
                />
              ) : (
                <View style={styles.emptyState}>
                  <Text style={styles.emptyStateTitle}>티켓 기록이 없어요</Text>
                  <Text style={styles.emptyStateSubtitle}>
                    친구가 공유한 티켓이 여기에서 보여집니다.
                  </Text>
                </View>
              )}
            </ScrollView>
          </View>

          {/* 캘린더 탭 */}
          <View key="calendar" style={styles.pageContainer}>
            <ScrollView
              style={styles.calendarScrollView}
              contentContainerStyle={styles.calendarContent}
              showsVerticalScrollIndicator={false}
            >
              {realFriendTickets.length > 0 ? (
                <>
                  <CustomCalendar
                    selectedDate={selectedDate}
                    tickets={realFriendTickets}
                    onDayPress={handleDayPress}
                  />
                  <EventsList
                    selectedEvents={selectedEvents}
                    onTicketPress={handleTicketPress}
                  />
                </>
              ) : (
                <View style={styles.emptyState}>
                  <Text style={styles.emptyStateTitle}>표시할 일정이 없어요</Text>
                  <Text style={styles.emptyStateSubtitle}>
                    공유된 티켓이 등록되면 캘린더에서 확인할 수 있어요.
                  </Text>
                </View>
              )}
            </ScrollView>
          </View>
        </PagerView>
      </View>

      {/* 티켓 모달 */}
      {selectedTicket && (
        <TicketDetailModal
          visible={isModalVisible}
          ticket={selectedTicket}
          onClose={handleCloseModal}
          isMine={false}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.systemBackground },
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
  placeholder: {
    position: 'absolute',
    right: Spacing.lg,
    width: 44,
    height: 44,
  },


  mainContent: { flex: 1, },

  profileSection: {
    alignItems: 'center',
    padding : 20,
    backgroundColor: '#FFFFFF',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
  },
  profileAvatar: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: Colors.systemGray5,
  },
  profileName: {
    ...Typography.title1,
    fontWeight: 'bold',
    color: Colors.label,
  },
  profileUsername: {
    fontSize: 16,
    color: '#6C757D',
    marginVertical: 4,
  },
  
  // 뱃지
  badgeWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 0.5,
    borderColor: Colors.systemGray5,
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.xl,
    height: 32,
    paddingHorizontal: Spacing.md,
    top: -20,
    ...Shadows.medium,
  },
  badgeEmoji: {
    ...Typography.footnote,
    marginRight: Spacing.xs,
  },
  badgeText: {
    color: Colors.primary,
    ...Typography.caption1,
    fontWeight: 'bold',
  },

  tabContainer: {
    flexDirection: 'row',
    paddingVertical: 8,
    paddingHorizontal: 20,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E9ECEF',
  },
  tabButton: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: { 
    borderBottomColor: '#B11515',
  },
  tabText: { fontSize: 16, fontWeight: '500', color: '#6C757D' },
  activeTabText: { color: '#B11515', fontWeight: '600' },
  
  pager: { 
    flex: 1,
    alignItems: 'center',
  },
  pageContainer: { 
    flex: 1, 
    backgroundColor: '#26282B', 
  },

  feedScrollView: {
    flex: 1,
  },
  feedContent: {
    flexGrow: 1,
    paddingBottom: 20,
    alignItems: 'flex-start',
  },
  emptyState: {
    paddingVertical: Spacing.xl,
    paddingHorizontal: Spacing.lg,
    alignItems: 'center',
    gap: Spacing.sm,
  },
  emptyStateTitle: {
    ...Typography.body,
    fontWeight: '600',
    color: Colors.label,
  },
  emptyStateSubtitle: {
    ...Typography.subheadline,
    color: Colors.secondaryLabel,
    textAlign: 'center',
  },

  friendGridContainer: {
    padding: 4,
  },
  calendarScrollView: {
    flex: 1,
  },
  calendarContent: {
    paddingHorizontal: 4,
    paddingVertical: 24,
  },
});

export default FriendProfilePage;
