import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Colors, Typography, Spacing, BorderRadius } from '../styles/designSystem';
import GNB from './GNB';

interface CalendarHeaderProps {
  monthlyTicketCount: number;
}

const CalendarHeader: React.FC<CalendarHeaderProps> = ({ monthlyTicketCount }) => {
  return (
    <>
      {/* Header */}
      <GNB />

      {/* Calendar Title and Count */}
      <View style={styles.titleSection}>
        <Text style={styles.calendarTitle}>캘린더</Text>
        <View style={styles.ticketCountBadge}>
          <Text style={styles.ticketCountText}>🎟️  {monthlyTicketCount}개</Text>
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
