import 'dart:convert';

import 'package:betterseed/backup/backup_snapshot.dart';
import 'package:betterseed/data/habit.dart';
import 'package:betterseed/logic/water_progress.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('backup round trips habits, reminders, credits, and theme', () {
    final progress = WaterProgress();
    expect(progress.earnForHabit('2026-10-24', 'h1'), isTrue);
    expect(progress.waterTree(), isTrue);
    final original = BackupSnapshot(
      habits: const [Habit(id: 'h1', title: 'Read', iconKey: 'book',
          completed: true, reminderEnabled: true, reminderHour: 7, reminderMinute: 30)],
      progress: progress,
      day: '2026-10-24',
      packId: 'moonlight',
      ownedMoonlight: true,
      savedAtUtc: DateTime.utc(2026, 10, 25),
    );
    final restored = BackupSnapshot.decode(original.encode());
    expect(restored.habits.single.reminderHour, 7);
    expect(restored.habits.single.reminderEnabled, true);
    expect(restored.progress.xp, 10);
    expect(restored.progress.totalSpent, 1);
    expect(restored.progress.wasCredited('2026-10-24', 'h1'), true);
    expect(restored.packId, 'moonlight');
    expect(restored.ownedMoonlight, true);
  });

  test('rejects invalid, unsupported, and duplicate backups', () {
    expect(() => BackupSnapshot.decode('{}'), throwsFormatException);
    final payload = <String, dynamic>{
      'format': 'betterseed.backup', 'version': 3,
      'savedAtUtc': '2026-10-25T00:00:00Z',
      'data': <String, dynamic>{'habits': <dynamic>[], 'progress': <String, dynamic>{}, 'day': '2026-10-25'},
    };
    expect(() => BackupSnapshot.decode(jsonEncode(payload)), throwsFormatException);
    payload['version'] = 1;
    (payload['data'] as Map<String, dynamic>)['habits'] = <dynamic>[
      <String, dynamic>{'id': 'same', 'title': 'A'},
      <String, dynamic>{'id': 'same', 'title': 'B'},
    ];
    expect(() => BackupSnapshot.decode(jsonEncode(payload)), throwsFormatException);
  });
}
