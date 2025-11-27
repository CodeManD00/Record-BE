import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { Colors, Typography, Spacing, BorderRadius } from '../styles/designSystem';
import GNB from './GNB';

interface CalendarHeaderProps {
  monthlyTicketCount: number;
}

const CalendarHeader: React.FC<CalendarHeaderProps> = ({ monthlyTicketCount }) => {
  const navigation = useNavigation();

  return (
    <>
      {/* Header */}
      <GNB />

      {/* Calendar Title and Count */}
      <View style={styles.titleSection}>
        <Text style={styles.calendarTitle}>캘린더</Text>
        <View style={styles.rightSection}>
          <TouchableOpacity
            style={styles.archiveButton}
            onPress={() => navigation.navigate('PerformanceArchive' as never)}
          >
            <Text style={styles.archiveButtonText}>아카이브</Text>
          </TouchableOpacity>
          <View style={styles.ticketCountBadge}>
            <Text style={styles.ticketCountText}>🎟️  {monthlyTicketCount}개</Text>
          </View>
        </View>
      </View>
    </>
  );
};

const styles = StyleSheet.create({
  // Title Section
  titleSection: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.screenPadding,
    paddingVertical: Spacing.sectionSpacing,
    backgroundColor: Colors.systemBackground,
  },
  calendarTitle: {
    ...Typography.title1,
    fontWeight: '700',
    color: Colors.label,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  archiveButton: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    justifyContent: 'center',
    borderRadius: BorderRadius.lg,
    borderWidth: 0.5,
    borderColor: Colors.systemGray5,
  },
  archiveButtonText: {
    ...Typography.subheadline,
    color: Colors.label,
    fontWeight: '600',
  },
  // Ticket count
  ticketCountBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.systemBackground,
    borderRadius: BorderRadius.xl,
    borderWidth: 0.5,
    borderColor: Colors.systemGray5,
    height: 36,
    paddingHorizontal: Spacing.md,
  },
  ticketCountText: {
    color: Colors.primary,
    ...Typography.subheadline,
    fontWeight: 'bold',
  },
});

export default CalendarHeader;
