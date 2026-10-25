import 'dart:convert';

import 'package:betterseed/main.dart';
import 'package:betterseed/data/habit.dart';
import 'package:betterseed/logic/growth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('Checklist explains paused reminders when the master is off', (tester) async {
    SharedPreferences.setMockInitialValues({
      'betterseed_music_v2': false,
      'betterseed_sfx_v2': false,
      'betterseed_day_v1': todayLocalKey(),
      'betterseed_habits_v1': jsonEncode([
        (const Habit(id: 'read', title: 'Read', reminderEnabled: true)).toJson(),
      ]),
    });
    await tester.pumpWidget(const BetterSeedApp());
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    await tester.tap(find.byKey(const ValueKey('tab-1')));
    await tester.pump();
    expect(find.byKey(const ValueKey('reminder-master-warning')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Settings offers private Drive backup controls', (tester) async {
    SharedPreferences.setMockInitialValues({
      'betterseed_music_v2': false, 'betterseed_sfx_v2': false,
    });
    await tester.pumpWidget(const BetterSeedApp());
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    await tester.tap(find.byTooltip('Settings'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 450));
    expect(find.byKey(const ValueKey('reminder-toggle')), findsOneWidget);
    expect(find.byKey(const ValueKey('drive-connect')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
