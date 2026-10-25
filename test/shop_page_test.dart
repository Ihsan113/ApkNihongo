import 'dart:io';
import 'package:betterseed/widgets/shop_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Moonlight complete pack includes six stages and a new 9:16 backdrop', () {
    const stages = <String>['seed','sprout','sapling','young','mature','legendary'];
    for (var i = 0; i < stages.length; i++) {
      final path = 'assets/images/trees/skins/moonlight/stage_${i}_${stages[i]}.png';
      expect(File(path).existsSync(), isTrue, reason: path);
    }
    expect(File('assets/images/backgrounds/moonlight_premium.webp').existsSync(), isTrue);
    expect(File('assets/images/effects/butterfly_cosmic.png').existsSync(), isTrue);
    expect(File('assets/images/effects/watering_can_celestial.png').existsSync(), isTrue);
    expect(File('native/android/res/drawable/widget_background.xml').existsSync(), isTrue);
    for (var i=0;i<6;i++) {
      expect(File('native/android/res/drawable-nodpi/widget_moon_tree_$i.webp').existsSync(), isTrue);
    }
  });

  test('A bundle applies all five cosmetics together', () {
    expect(ShopLoadout.defaultPack.packId, 'default');
    expect(ShopLoadout.moonlightPack.packId, 'moonlight');
    expect(ShopLoadout.moonlightPack.treeSkinId, 'moonlight_tree_skin');
    expect(ShopLoadout.moonlightPack.backgroundId, 'twilight_overlook');
    expect(ShopLoadout.moonlightPack.butterflyId, 'cosmic_butterfly');
    expect(ShopLoadout.moonlightPack.wateringCanId, 'celestial_watering_can');
    expect(ShopLoadout.moonlightPack.fireflyId, 'prism_fireflies');
  });

  for (final scale in <double>[1, 1.6, 2]) {
    testWidgets('Shop and Collection fit a 320x640 screen at text scale $scale', (tester) async {
      tester.view.physicalSize = const Size(320,640);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(MaterialApp(
        home: MediaQuery(
          data: MediaQueryData(size: const Size(320,640),
            textScaler: TextScaler.linear(scale)),
          child: const Scaffold(body: BetterSeedShopPage(moonlightOwned: true)),
        ),
      ));
      await tester.pump();
      expect(find.byKey(const ValueKey('shop-pack-default')), findsOneWidget);
      expect(find.byKey(const ValueKey('shop-pack-moonlight')), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.tap(find.byKey(const ValueKey('shop-collection-tab')));
      await tester.pump();
      expect(find.byKey(const ValueKey('shop-pack-owned-default')), findsOneWidget);
      expect(find.byKey(const ValueKey('shop-pack-owned-moonlight')), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('Claim once, equip from collection, and return to default', (tester) async {
    var owned = false;
    var loadout = ShopLoadout.defaultPack;
    await tester.pumpWidget(MaterialApp(home: StatefulBuilder(
      builder: (ctx, setLocal) => Scaffold(
        body: BetterSeedShopPage(
          moonlightOwned: owned, loadout: loadout,
          onClaimMoonlight: () => setLocal(() => owned = true),
          onApplyLoadout: (newLoadout) => setLocal(() => loadout = newLoadout),
        ),
      ),
    )));
    final claim = find.byKey(const ValueKey('shop-action-moonlight'));
    await tester.ensureVisible(claim);
    await tester.pump();
    await tester.tap(claim);
    await tester.pump();
    expect(owned, isTrue);
    expect(loadout.packId, 'default'); // Claim never auto-equips.
    final equip = find.byKey(const ValueKey('shop-action-owned-moonlight'));
    await tester.ensureVisible(equip);
    await tester.pump();
    await tester.tap(equip);
    await tester.pump();
    expect(loadout.packId, 'moonlight');
    final original = find.byKey(const ValueKey('shop-action-owned-default'));
    await tester.ensureVisible(original);
    await tester.pump();
    await tester.tap(original);
    await tester.pump();
    expect(loadout.packId, 'default');
    expect(tester.takeException(), isNull);
  });
}
