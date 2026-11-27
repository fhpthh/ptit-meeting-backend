package org.ptit.meeting.modules.notification.service;

/**
 * Processes automatic reminders for upcoming online timetable sessions.
 */
public interface OnlineClassReminderService {

  /**
   * Scans upcoming online sessions within the configured reminder window and publishes
   * reminders for eligible lecturers and students.
   *
   * @return total number of newly notified recipients across all processed sessions
   */
  int processUpcomingOnlineSessionReminders();
}
