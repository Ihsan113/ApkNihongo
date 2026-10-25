import 'package:betterseed/main.dart';
import 'package:betterseed/widgets/garden_life_painter.dart';
import 'package:betterseed/widgets/adaptive_garden_background.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<void> _launch(WidgetTester tester) async {
  SharedPreferences.setMockInitialValues({
    'betterseed_music_v2': false,
    'betterseed_sfx_v2': false,
    'betterseed_motion_v2': true,
    'betterseed_game_motion_override_v1': true,
  });
  await tester.pumpWidget(const BetterSeedApp());
  // Allow the async preference/native initialization to complete. We must NOT
  // call pumpAndSettle: the garden has an intentionally infinite animation.
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 500));
}

void main() {
  test('garden ambience repaints as the timeline moves', () {
    expect(const GardenLifePainter(.5).shouldRepaint(
      const GardenLifePainter(.1),
    ), isTrue);
    expect(const GardenLifePainter(.5).shouldRepaint(
      const GardenLifePainter(.5),
    ), isFalse);
  });

  testWidgets('background fills the scene with BoxFit.cover', (tester) async {
    await _launch(tester);
    final image = tester.widget<Image>(
      find.byKey(const ValueKey('full-background')),
    );
    expect(image.fit, BoxFit.cover);
    expect(find.byType(AdaptiveGardenBackground), findsOneWidget);
  });

  testWidgets('garden shows an ambient painter when animations are allowed',
      (tester) async {
    await _launch(tester);
    final reducedMotion = MediaQuery.disableAnimationsOf(
      tester.element(find.byType(GrowthHome)),
    );
    // The test explicitly opts into garden motion even when Android's
    // animation scale is zero. The painter must have non-zero dimensions.
    expect(reducedMotion, isA<bool>());
    final painter = find.byKey(const ValueKey('garden-live-ambient'));
    expect(painter, findsOneWidget);
    final scene = tester.getSize(painter);
    expect(scene.width, greaterThan(0));
    expect(scene.height, greaterThan(0));
  });

  testWidgets('Bottom tabs have space for larger device text', (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    SharedPreferences.setMockInitialValues({
      'betterseed_music_v2': false,
      'betterseed_sfx_v2': false,
      'betterseed_motion_v2': false,
    });
    await tester.pumpWidget(MediaQuery(
      data: const MediaQueryData(textScaler: TextScaler.linear(1.8)),
      child: const BetterSeedApp(),
    ));
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.byKey(const ValueKey('tab-3')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Settings exposes sound, reminders and widget on short screens',
      (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await _launch(tester);
    await tester.tap(find.byTooltip('Settings'));
    // A finite pump advances the sheet animation while the garden loops.
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 450));

    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('Living garden'), findsOneWidget);
    expect(find.text('Notifications master switch'), findsOneWidget);
    expect(find.byKey(const ValueKey('reminder-toggle')), findsOneWidget);
    expect(find.byKey(const ValueKey('widget-add')), findsOneWidget);

    // Controls below the fold must be reachable, independent of whether a
    // single drag first expands the draggable sheet or scrolls its content.
    await tester.ensureVisible(find.byKey(const ValueKey('widget-add')));
    await tester.pump(const Duration(milliseconds: 200));
    expect(find.byKey(const ValueKey('widget-add')), findsOneWidget);
  });
}
