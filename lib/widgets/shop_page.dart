import 'package:flutter/material.dart';

const _mint = Color(0xFF9EF7BD);
const _moon = Color(0xFFCBB4FF);
const _ink = Color(0xFF081F25);

/// Each owned pack equips every Garden cosmetic together.
class ShopLoadout {
  const ShopLoadout({
    this.treeSkinId = 'default',
    this.backgroundId = 'default',
    this.butterflyId = 'default',
    this.wateringCanId = 'default',
    this.fireflyId = 'default',
  });
  final String treeSkinId;
  final String backgroundId;
  final String butterflyId;
  final String wateringCanId;
  final String fireflyId;

  static const defaultPack = ShopLoadout();
  static const moonlightPack = ShopLoadout(
    treeSkinId: 'moonlight_tree_skin',
    backgroundId: 'twilight_overlook',
    butterflyId: 'cosmic_butterfly',
    wateringCanId: 'celestial_watering_can',
    fireflyId: 'prism_fireflies',
  );

  String get packId => treeSkinId == 'moonlight_tree_skin' ? 'moonlight' : 'default';
}

class BetterSeedShopPage extends StatefulWidget {
  const BetterSeedShopPage({
    super.key,
    this.loadout = ShopLoadout.defaultPack,
    this.moonlightOwned = false,
    this.onClaimMoonlight,
    this.onApplyLoadout,
  });
  final ShopLoadout loadout;
  final bool moonlightOwned;
  final VoidCallback? onClaimMoonlight;
  final ValueChanged<ShopLoadout>? onApplyLoadout;

  @override
  State<BetterSeedShopPage> createState() => _BetterSeedShopPageState();
}

class _BetterSeedShopPageState extends State<BetterSeedShopPage> {
  int _tab = 0;

  void _equip(String id) {
    widget.onApplyLoadout?.call(
        id == 'moonlight' ? ShopLoadout.moonlightPack : ShopLoadout.defaultPack);
  }

  void _claim() {
    widget.onClaimMoonlight?.call();
    setState(() => _tab = 1);
  }

  @override
  Widget build(BuildContext context) {
    final owned = widget.moonlightOwned;
    final active = widget.loadout.packId;
    // Only two complete packs exist. Build them eagerly so offscreen items
    // remain available to accessibility, tests and keyboard navigation.
    // The outer scroll view still permits short displays and enlarged text.
    return SingleChildScrollView(
      key: const ValueKey('shop-scroll'),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
        const Text('Garden Packs',
            style: TextStyle(fontSize: 25, fontWeight: FontWeight.w800)),
        const SizedBox(height: 3),
        const Text('A complete look for every step of your journey',
            style: TextStyle(fontSize: 12, color: Color(0xFFDEEAE5))),
        const SizedBox(height: 14),
        Container(
          decoration: BoxDecoration(
            color: const Color(0xDD082E31),
            border: Border.all(color: const Color(0x8067BDA4)),
            borderRadius: BorderRadius.circular(17),
          ),
          padding: const EdgeInsets.all(4),
          child: Row(children: [
            Expanded(child: _tabButton('Shop', 0, const ValueKey('shop-store-tab'))),
            Expanded(child: _tabButton('My Collection', 1,
                const ValueKey('shop-collection-tab'))),
          ]),
        ),
        const SizedBox(height: 16),
        if (_tab == 0) ...[
          _packCard(id: 'default', title: 'Original Garden',
              subtitle: 'Your familiar enchanted valley and growing tree.',
              image: 'assets/images/backgrounds/enchanted_valley.webp',
              active: active == 'default', owned: true, onTap: () => _equip('default')),
          const SizedBox(height: 14),
          _packCard(id: 'moonlight', title: 'Moonlight Night Pack',
              subtitle: 'Six tree stages, moonlit garden, butterflies, watering can and glowing fireflies.',
              image: 'assets/images/backgrounds/moonlight_premium.webp',
              active: active == 'moonlight', owned: owned,
              onTap: owned ? () => setState(() => _tab = 1) : _claim),
          const SizedBox(height: 16),
          _hint('Everything comes as one pack. Claim once, then equip it from My Collection.'),
        ] else ...[
          const Text('My Collection',
              style: TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          _packCard(id: 'owned-default', title: 'Original Garden',
              subtitle: 'Built-in theme • Always yours',
              image: 'assets/images/backgrounds/enchanted_valley.webp',
              active: active == 'default', owned: true, onTap: () => _equip('default')),
          if (owned) ...[
            const SizedBox(height: 14),
            _packCard(id: 'owned-moonlight', title: 'Moonlight Night Pack',
                subtitle: 'Your complete moonlight garden set',
                image: 'assets/images/backgrounds/moonlight_premium.webp',
                active: active == 'moonlight', owned: true,
                onTap: () => _equip('moonlight')),
          ],
          const SizedBox(height: 16),
          _hint('Tap Use on any owned pack to switch the Garden and home screen widget together.'),
        ],
        ],
      ),
    );
  }

  Widget _tabButton(String title, int id, Key key) {
    final selected = _tab == id;
    return TextButton(
      key: key,
      onPressed: () => setState(() => _tab = id),
      style: TextButton.styleFrom(
        backgroundColor: selected ? const Color(0xFF5364A0) : Colors.transparent,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 12),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      child: Text(title,
          maxLines: 2, textAlign: TextAlign.center,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)),
    );
  }

  Widget _packCard({required String id, required String title,
      required String subtitle, required String image, required bool active,
      required bool owned, required VoidCallback onTap}) {
    final moon = id.contains('moonlight');
    final highlight = moon ? _moon : _mint;
    final buttonText = active ? 'In Use' : !owned ? 'Claim Free' :
        (_tab == 0 ? 'My Collection' : 'Use');
    return Container(
      key: ValueKey('shop-pack-$id'),
      decoration: BoxDecoration(
        color: const Color(0xF20B2831),
        borderRadius: BorderRadius.circular(19),
        border: Border.all(color: active ? highlight : highlight.withValues(alpha: .45)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          ClipRRect(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
            child: Stack(children: [
              Image.asset(image, width: double.infinity, height: 150,
                  fit: BoxFit.cover, cacheWidth: 720),
              Positioned(top: 9, right: 10,
                child: DecoratedBox(
                  decoration: BoxDecoration(color: const Color(0xDD102C31),
                      borderRadius: BorderRadius.circular(15)),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    child: Text(active ? 'IN USE' : owned ? 'OWNED' : 'FREE',
                        style: TextStyle(color: highlight, fontSize: 11,
                            fontWeight: FontWeight.w900)),
                  ),
                ),
              ),
            ]),
          ),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontSize: 17,
                    fontWeight: FontWeight.w800)),
                const SizedBox(height: 5),
                Text(subtitle, style: const TextStyle(fontSize: 12,
                    color: Color(0xFFDBE5E1), height: 1.35)),
                const SizedBox(height: 12),
                SizedBox(width: double.infinity,
                  child: FilledButton.icon(
                    key: ValueKey('shop-action-$id'),
                    onPressed: active ? null : onTap,
                    icon: Icon(active ? Icons.check_circle_outline :
                        owned ? Icons.auto_awesome_outlined : Icons.card_giftcard_outlined,
                        size: 18),
                    label: Text(buttonText, maxLines: 1, overflow: TextOverflow.ellipsis),
                    style: FilledButton.styleFrom(
                      backgroundColor: highlight,
                      foregroundColor: _ink,
                      padding: const EdgeInsets.symmetric(vertical: 13),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _hint(String message) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(color: const Color(0xD70B2A31),
      borderRadius: BorderRadius.circular(16)),
    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Icon(Icons.info_outline, color: _mint, size: 19),
      const SizedBox(width: 10),
      Expanded(child: Text(message,
          style: const TextStyle(fontSize: 12, height: 1.5))),
    ]),
  );
}
