import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'audio/ambient_audio.dart';
import 'backup/backup_snapshot.dart';
import 'backup/drive_backup_service.dart';
import 'data/habit.dart';
import 'logic/growth.dart';
import 'logic/garden_motion.dart';
import 'logic/garden_scene_geometry.dart';
import 'logic/water_progress.dart';
import 'platform/device_features.dart';
import 'widgets/garden_life_painter.dart';
import 'widgets/adaptive_garden_background.dart';
import 'widgets/moonlight_ambience_painter.dart';
import 'widgets/shop_page.dart';

const _deepNavy = Color(0xFF071B22);
const _mint = Color(0xFF9EF7BD);
const _gold = Color(0xFFFFD989);
const _blue = Color(0xFF80DFFF);
const _glass = Color(0xDF102C31);
const _stageNames = <String>[
  'A New Seed', 'First Sprout', 'Growing Sapling',
  'Young Tree', 'Thriving Tree', 'Legendary Tree',
];
const _defaultTreeImages = <String>[
  'assets/images/trees/stage_0_seed.webp',
  'assets/images/trees/stage_1_sprout.webp',
  'assets/images/trees/stage_2_sapling.webp',
  'assets/images/trees/stage_3_young.webp',
  'assets/images/trees/stage_4_mature.webp',
  'assets/images/trees/stage_5_legendary.webp',
];

const _moonlightTreeImages = <String>[
  'assets/images/trees/skins/moonlight/stage_0_seed.png',
  'assets/images/trees/skins/moonlight/stage_1_sprout.png',
  'assets/images/trees/skins/moonlight/stage_2_sapling.png',
  'assets/images/trees/skins/moonlight/stage_3_young.png',
  'assets/images/trees/skins/moonlight/stage_4_mature.png',
  'assets/images/trees/skins/moonlight/stage_5_legendary.png',
];

const _defaultBackgroundAsset = 'assets/images/backgrounds/enchanted_valley.webp';
const _twilightBackgroundAsset = 'assets/images/backgrounds/moonlight_premium.webp';
const _defaultButterflyAsset = 'assets/images/effects/butterfly_realistic.png';
const _cosmicButterflyAsset = 'assets/images/effects/butterfly_cosmic.png';
const _defaultWateringCanAsset = 'assets/images/effects/watering_can_realistic.png';
const _celestialWateringCanAsset = 'assets/images/effects/watering_can_celestial.png';

const _habitIconChoices = <MapEntry<String, IconData>>[
  MapEntry('leaf', Icons.spa_outlined),
  MapEntry('water', Icons.local_drink_outlined),
  MapEntry('book', Icons.menu_book_outlined),
  MapEntry('money', Icons.savings_outlined),
  MapEntry('move', Icons.directions_run_outlined),
  MapEntry('heart', Icons.favorite_outline),
  MapEntry('moon', Icons.nightlight_round),
  MapEntry('meditate', Icons.self_improvement_outlined),
];

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: _deepNavy,
    systemNavigationBarIconBrightness: Brightness.light,
  ));
  runApp(const BetterSeedApp());
}

class BetterSeedApp extends StatelessWidget {
  const BetterSeedApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'BetterSeed',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          brightness: Brightness.dark,
          useMaterial3: true,
          scaffoldBackgroundColor: _deepNavy,
          colorScheme: ColorScheme.fromSeed(
            seedColor: _mint,
            brightness: Brightness.dark,
          ),
        ),
        home: const GrowthHome(),
      );
}

class GrowthHome extends StatefulWidget {
  const GrowthHome({super.key});

  @override
  State<GrowthHome> createState() => _GrowthHomeState();
}

class _GrowthHomeState extends State<GrowthHome>
    with TickerProviderStateMixin, WidgetsBindingObserver {
  static const _storageHabits = 'betterseed_habits_v1';
  static const _storageXp = 'betterseed_xp_v1';
  static const _storageDate = 'betterseed_day_v1';
  static const _storageProgress = 'betterseed_water_progress_v3';
  static const _storageMusic = 'betterseed_music_v2';
  static const _storageSfx = 'betterseed_sfx_v2';
  static const _storageMotion = 'betterseed_motion_v2';
  static const _storageMotionOverride = 'betterseed_game_motion_override_v1';
  static const _storageTreeSkin = 'betterseed_tree_skin_v1';
  static const _storageBackground = 'betterseed_background_skin_v1';
  static const _storageButterfly = 'betterseed_butterfly_skin_v1';
  static const _storageWateringCan = 'betterseed_watering_can_skin_v1';
  static const _storageFirefly = 'betterseed_firefly_skin_v1';
  static const _storageBundle = 'betterseed_active_bundle_v1';
  static const _storageOwnedMoonlight = 'betterseed_owned_moonlight_v1';
  static const _starterHabits = <Habit>[
    Habit(id: 'drink', title: 'Drink water', iconKey: 'water'),
    Habit(id: 'read', title: 'Read 10 pages', iconKey: 'book'),
    Habit(id: 'move', title: 'Move for 15 minutes', iconKey: 'move'),
  ];

  final AmbientAudio _audio = AmbientAudio();
  final DriveBackupService _driveBackup = DriveBackupService();
  bool _driveBusy = false;
  List<Habit> _habits = List<Habit>.from(_starterHabits);
  WaterProgress _progress = WaterProgress();
  String _today = todayLocalKey();
  Future<void> _saveQueue = Future<void>.value();
  Timer? _dayTimer;
  int _selectedTab = 0;
  bool _loaded = false;
  bool _musicEnabled = true;
  bool _effectsEnabled = true;
  bool _motionEnabled = true;
  bool _motionOverride = false;
  bool _systemReduceMotion = false;
  bool _reminderEnabled = false;
  bool _reminderBusy = false;
  ShopLoadout _shopLoadout = ShopLoadout.defaultPack;
  bool _moonlightOwned = false;
  bool _focusMode = false;

  List<String> get _activeTreeImages =>
      _shopLoadout.treeSkinId == 'moonlight_tree_skin' ? _moonlightTreeImages : _defaultTreeImages;

  String get _activeBackgroundAsset =>
      _shopLoadout.backgroundId == 'twilight_overlook' ? _twilightBackgroundAsset : _defaultBackgroundAsset;

  String get _activeButterflyAsset =>
      _shopLoadout.butterflyId == 'cosmic_butterfly' ? _cosmicButterflyAsset : _defaultButterflyAsset;

  String get _activeWateringCanAsset =>
      _shopLoadout.wateringCanId == 'celestial_watering_can' ? _celestialWateringCanAsset : _defaultWateringCanAsset;

  PlantingGround get _activePlantingGround => _shopLoadout.packId == 'moonlight'
      ? PlantingGround.moonlightGarden : PlantingGround.defaultGarden;

  FireflyPalette get _activeFireflyPalette =>
      _shopLoadout.fireflyId == 'prism_fireflies' ? FireflyPalette.prism : FireflyPalette.warm;

  late final AnimationController _windController;
  late final AnimationController _waterController;

  bool get _showAnimation => shouldAnimateGarden(
        livingGardenEnabled: _motionEnabled,
        systemReducedMotion: _systemReduceMotion,
        allowWhenSystemReduced: _motionOverride,
      );
  int get _completed => _habits.where((h) => h.completed).length;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _windController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 14),
      animationBehavior: AnimationBehavior.preserve,
    )..repeat();
    _waterController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1700),
      animationBehavior: AnimationBehavior.preserve,
    );
    _dayTimer = Timer.periodic(const Duration(minutes: 1), (_) => _ensureToday());
    _load();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _systemReduceMotion = MediaQuery.disableAnimationsOf(context);
    _applyAnimationPreference();
  }

  void _applyAnimationPreference() {
    if (_showAnimation && _selectedTab == 0) {
      if (!_windController.isAnimating) _windController.repeat();
    } else {
      _windController.stop();
    }
    if (!_showAnimation) _waterController.stop();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _audio.setForeground(true);
      _ensureToday();
      _refreshReminderPermission();
    } else if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden ||
        state == AppLifecycleState.detached) {
      // `inactive` can be a transient dialog/system-overlay event; avoid
      // interrupting music every time a sheet/dialog is opened.
      _audio.setForeground(false);
    }
  }

  @override
  void dispose() {
    if (_focusMode) {
      // Restore Android system bars if the page is closed while immersed.
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
    _dayTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    _windController.dispose();
    _waterController.dispose();
    _audio.dispose();
    _driveBackup.dispose();
    super.dispose();
  }

  /// Immersive Garden viewing. No progress or audio settings are changed.
  Future<void> _setFocusMode(bool enabled) async {
    if (_focusMode == enabled || !mounted) return;
    setState(() => _focusMode = enabled);
    if (enabled) {
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    } else {
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
      SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: _deepNavy,
        statusBarIconBrightness: Brightness.light,
        systemNavigationBarIconBrightness: Brightness.light,
      ));
    }
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    var loaded = List<Habit>.from(_starterHabits);
    final rawHabits = prefs.getString(_storageHabits);
    if (rawHabits != null) {
      try {
        loaded = (jsonDecode(rawHabits) as List)
            .map((value) => Habit.fromJson(Map<String, dynamic>.from(value as Map)))
            .toList();
      } catch (_) {
        // Preserve app usability on corrupted legacy data.
      }
    }
    final freshDay = todayLocalKey();
    if (prefs.getString(_storageDate) != freshDay) {
      loaded = loaded.map((habit) => habit.copyWith(completed: false)).toList();
    }
    var progress = WaterProgress(xp: math.max(0, prefs.getInt(_storageXp) ?? 0));
    final rawProgress = prefs.getString(_storageProgress);
    if (rawProgress != null) {
      try {
        progress = WaterProgress.fromJson(
          Map<String, dynamic>.from(jsonDecode(rawProgress) as Map),
        );
      } catch (_) {
        // Legacy XP is retained when a v3 preference is unreadable.
      }
    }
    if (!mounted) return;
    setState(() {
      _habits = loaded;
      _progress = progress;
      _today = freshDay;
      _musicEnabled = prefs.getBool(_storageMusic) ?? true;
      _effectsEnabled = prefs.getBool(_storageSfx) ?? true;
      _motionEnabled = prefs.getBool(_storageMotion) ?? true;
      _motionOverride = prefs.getBool(_storageMotionOverride) ?? false;
      // Migrate previous per-cosmetic preferences into a single full pack.
      // Existing users keep their Moonlight entitlement rather than losing it.
      final legacyMoonlight = <String?>[
        prefs.getString(_storageTreeSkin), prefs.getString(_storageBackground),
        prefs.getString(_storageButterfly), prefs.getString(_storageWateringCan),
        prefs.getString(_storageFirefly),
      ].any((value) => value != null && value != 'default');
      final savedBundle = prefs.getString(_storageBundle);
      _moonlightOwned = (prefs.getBool(_storageOwnedMoonlight) ?? false) || legacyMoonlight;
      _shopLoadout = (savedBundle == 'moonlight' ||
              (savedBundle == null && legacyMoonlight))
          ? ShopLoadout.moonlightPack : ShopLoadout.defaultPack;
      _loaded = true;
    });
    _applyAnimationPreference();
    final reminder = await DeviceFeatures.getReminder();
    if (!mounted) return;
    setState(() {
      _reminderEnabled = reminder['enabled'] == true && reminder['permissionGranted'] == true;
    });
    await _audio.initialize(music: _musicEnabled, effects: _effectsEnabled);
    if (!mounted) return;
    // Ensure the migrated data has an initialized v3 record and today key.
    _save();
  }

  Future<void> _save() {
    final habitSnapshot = _habits.map((habit) => habit.toJson()).toList();
    final habits = jsonEncode(habitSnapshot);
    final progress = jsonEncode(_progress.toJson());
    final day = _today;
    final water = _progress.waterBalance;
    final stage = _progress.treeStage + 1;
    final done = _completed;
    final total = _habits.length;
    final packId = _shopLoadout.packId;
    final ownedMoonlight = _moonlightOwned;
    final activeLoadout = _shopLoadout;
    // Serialized writes avoid an earlier slow SharedPreferences write
    // overwriting newer completion/water data.
    _saveQueue = _saveQueue.catchError((Object _) {}).then((_) async {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_storageHabits, habits);
      await prefs.setString(_storageProgress, progress);
      await prefs.setString(_storageDate, day);
      await prefs.setString(_storageBundle, packId);
      await prefs.setBool(_storageOwnedMoonlight, ownedMoonlight);
      await prefs.setString(_storageTreeSkin, activeLoadout.treeSkinId);
      await prefs.setString(_storageBackground, activeLoadout.backgroundId);
      await prefs.setString(_storageButterfly, activeLoadout.butterflyId);
      await prefs.setString(_storageWateringCan, activeLoadout.wateringCanId);
      await prefs.setString(_storageFirefly, activeLoadout.fireflyId);
      await DeviceFeatures.syncHabitReminders(habits: habitSnapshot, day: day);
      await DeviceFeatures.updateWidget(water: water, stage: stage, done: done, total: total, day: day, theme: packId);
    });
    return _saveQueue;
  }

  void _ensureToday() {
    if (!_loaded) return;
    final now = todayLocalKey();
    if (_today == now) return;
    setState(() {
      _today = now;
      _habits = _habits.map((habit) => habit.copyWith(completed: false)).toList();
    });
    _save();
  }

  void _changeTab(int index) {
    if (index == _selectedTab) return;
    _ensureToday();
    setState(() => _selectedTab = index);
    _applyAnimationPreference();
    // Navigation is silent so it cannot interrupt the ambient soundscape.
  }

  void _toggleHabit(Habit habit) {
    if (!_loaded) return;
    _ensureToday();
    final index = _habits.indexWhere((item) => item.id == habit.id);
    if (index < 0) return;
    final done = !_habits[index].completed;
    bool awarded = false;
    setState(() {
      _habits[index] = _habits[index].copyWith(completed: done);
      if (done) awarded = _progress.earnForHabit(_today, habit.id);
    });
    _audio.effect(done ? 'complete' : 'tap');
    if (awarded && mounted) {
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('+1 water earned! Visit Garden to water your tree. 💧'),
        duration: Duration(milliseconds: 1500),
        behavior: SnackBarBehavior.floating,
      ));
    }
    _save();
  }

  Future<void> _waterTree() async {
    if (!_loaded) return;
    _ensureToday();
    if (_progress.waterBalance == 0) {
      _changeTab(1);
      return;
    }
    final oldStage = _progress.treeStage;
    setState(() => _progress.waterTree());
    if (_showAnimation) _waterController.forward(from: 0);
    final newStage = _progress.treeStage;
    _audio.effect(newStage > oldStage ? 'growth' : 'water');
    _save();
    if (newStage > oldStage) {
      if (_showAnimation) {
        await Future<void>.delayed(const Duration(milliseconds: 850));
      }
      if (mounted) await _showStageCelebration(newStage);
    }
  }

  Future<void> _showStageCelebration(int stage) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.symmetric(horizontal: 22),
        child: SingleChildScrollView(
          child: Container(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(25),
            gradient: const LinearGradient(begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF24464E), Color(0xFF10252D), Color(0xFF302949)]),
            border: Border.all(color: _gold.withValues(alpha: .75)),
            boxShadow: const [BoxShadow(color: Color(0x88000000), blurRadius: 30)],
          ),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.auto_awesome_rounded, color: _gold, size: 29),
            const SizedBox(height: 10),
            const Text('Congratulations!',
              textAlign: TextAlign.center,
              style: TextStyle(color: _gold, fontSize: 23, fontWeight: FontWeight.w900)),
            const SizedBox(height: 7),
            Text('Your tree reached Stage ${stage + 1}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
            SizedBox(height: 175, child: TweenAnimationBuilder<double>(
              tween: Tween(begin: .72, end: 1),
              duration: _showAnimation ? const Duration(milliseconds: 550) : Duration.zero,
              curve: Curves.easeOutBack,
              builder: (_, value, child) => Transform.scale(
                scale: value, alignment: Alignment.bottomCenter, child: child),
              child: Image.asset(_activeTreeImages[stage], fit: BoxFit.contain),
            )),
            const Text('Every small step helps your garden grow ✨',
              textAlign: TextAlign.center,
              style: TextStyle(color: Color(0xFFE2EEDC), fontSize: 12)),
            const SizedBox(height: 15),
            SizedBox(width: double.infinity,
              child: FilledButton(
                key: const ValueKey('stage-congratulations-close'),
                style: FilledButton.styleFrom(backgroundColor: _mint,
                  foregroundColor: _deepNavy),
                onPressed: () => Navigator.pop(dialogContext),
                child: const Text('Continue Growing'),
              ),
            ),
          ]),
        ),
        ),
      ),
    );
  }

  Future<_HabitDraft?> _composeHabit({Habit? initial}) =>
      showModalBottomSheet<_HabitDraft>(
        context: context,
        isScrollControlled: true,
        showDragHandle: true,
        backgroundColor: const Color(0xFF102D31),
        builder: (_) => _HabitComposer(
          initial: initial, masterEnabled: _reminderEnabled,
          onEnableMaster: () => _configureReminder(true),
        ),
      );

  Future<void> _addHabit() async {
    final draft = await _composeHabit();
    if (!mounted || draft == null || draft.title.isEmpty) return;
    setState(() => _habits.add(Habit(
          id: DateTime.now().microsecondsSinceEpoch.toString(),
          title: draft.title,
          iconKey: draft.iconKey,
          reminderEnabled: draft.reminderEnabled,
          reminderHour: draft.hour,
          reminderMinute: draft.minute,
        )));
    _audio.effect('plant');
    _save();
  }

  Future<void> _editHabit(Habit habit) async {
    final draft = await _composeHabit(initial: habit);
    if (!mounted || draft == null || draft.title.isEmpty) return;
    final index = _habits.indexWhere((item) => item.id == habit.id);
    if (index < 0) return;
    setState(() => _habits[index] = _habits[index].copyWith(
      title: draft.title,
      iconKey: draft.iconKey,
      reminderEnabled: draft.reminderEnabled,
      reminderHour: draft.hour,
      reminderMinute: draft.minute,
    ));
    _save();
  }

  Future<void> _removeHabit(Habit habit) async {
    final remove = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Remove habit?'),
        content: Text('Remove “${habit.title}” from your checklist?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );
    if (remove != true || !mounted) return;
    setState(() => _habits.removeWhere((item) => item.id == habit.id));
    // Previous water awards/XP are deliberately not deleted or duplicated.
    _save();
  }

  void _applyShopLoadout(ShopLoadout loadout) {
    if (loadout.packId == 'moonlight' && !_moonlightOwned) return;
    setState(() => _shopLoadout = loadout);
    _save();
  }

  void _claimMoonlight() {
    if (_moonlightOwned) return;
    setState(() => _moonlightOwned = true);
    _save();
  }

  void _setMusic(bool value) {
    setState(() => _musicEnabled = value);
    SharedPreferences.getInstance().then((prefs) => prefs.setBool(_storageMusic, value));
    _audio.setMusic(value);
  }

  void _setEffects(bool value) {
    setState(() => _effectsEnabled = value);
    SharedPreferences.getInstance().then((prefs) => prefs.setBool(_storageSfx, value));
    _audio.setEffects(value);
  }

  void _setMotion(bool value) {
    setState(() => _motionEnabled = value);
    _applyAnimationPreference();
    SharedPreferences.getInstance().then((prefs) => prefs.setBool(_storageMotion, value));
  }

  void _setMotionOverride(bool value) {
    setState(() => _motionOverride = value);
    _applyAnimationPreference();
    SharedPreferences.getInstance().then(
      (prefs) => prefs.setBool(_storageMotionOverride, value),
    );
  }

  Future<void> _refreshReminderPermission() async {
    final status = await DeviceFeatures.getReminder();
    if (!mounted) return;
    final enabled = status['enabled'] == true && status['permissionGranted'] == true;
    if (enabled != _reminderEnabled) setState(() => _reminderEnabled = enabled);
  }

  Future<bool> _configureReminder(bool enabled) async {
    if (_reminderBusy) return _reminderEnabled;
    setState(() => _reminderBusy = true);
    final result = await DeviceFeatures.setReminder(enabled: enabled);
    if (!mounted) return result;
    setState(() {
      _reminderBusy = false;
      _reminderEnabled = result;
    });
    if (enabled && !result) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Allow notifications in Android Settings to enable reminders.'),
      ));
    }
    // Refresh the native schedule for each opt-in habit on master changes.
    await _save();
    return result;
  }

  Future<void> _installHomeWidget() async {
    final installed = await DeviceFeatures.widgetInstalled();
    if (!mounted) return;
    if (installed) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('BetterSeed widget is already on your home screen.'),
      ));
      return;
    }
    final accepted = await DeviceFeatures.requestPinWidget();
    if (!mounted) return;
    if (accepted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Confirm the widget placement in your Android launcher.'),
      ));
    } else {
      await showDialog<void>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Add BetterSeed widget'),
          content: const Text('On your Android home screen, long-press an empty area, choose Widgets, find BetterSeed, then drag the widget onto your screen. Widget availability depends on your launcher.'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Got it')),
          ],
        ),
      );
    }
  }

  BackupSnapshot _snapshotForDrive() => BackupSnapshot(
        habits: List<Habit>.from(_habits),
        progress: _progress,
        day: _today,
        packId: _shopLoadout.packId,
        ownedMoonlight: _moonlightOwned,
        savedAtUtc: DateTime.now().toUtc(),
      );

  void _driveMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 5)),
    );
  }

  Future<void> _connectDrive() async {
    if (_driveBusy) return;
    setState(() => _driveBusy = true);
    try {
      await _driveBackup.connect();
      _driveMessage('Google Drive connected. Your local progress is unchanged.');
    } catch (_) {
      _driveMessage('Could not connect. Enable Drive API and configure OAuth + SHA fingerprints.');
    } finally {
      if (mounted) setState(() => _driveBusy = false);
    }
  }

  Future<void> _backupNow() async {
    if (_driveBusy || !_loaded || !_driveBackup.isConnected) return;
    setState(() => _driveBusy = true);
    try {
      await _saveQueue;
      await _driveBackup.upload(_snapshotForDrive());
      _driveMessage('Backup saved to your private Google Drive app data.');
    } catch (e) {
      _driveMessage('Backup failed. Your local data is safe. $e');
    } finally {
      if (mounted) setState(() => _driveBusy = false);
    }
  }

  Future<void> _restoreFromDrive() async {
    if (_driveBusy || !_loaded || !_driveBackup.isConnected) return;
    setState(() => _driveBusy = true);
    try {
      final backup = await _driveBackup.download();
      if (!mounted) return;
      if (backup == null) {
        _driveMessage('No BetterSeed backup found in this Google account.');
        return;
      }
      final approved = await showDialog<bool>(
        context: context,
        builder: (dialog) => AlertDialog(
          title: const Text('Restore Google Drive backup?'),
          content: Text(
            'Saved: ${backup.savedAtUtc.toLocal()}\n'
            'Habits: ${backup.habits.length}\n'
            'XP: ${backup.progress.xp}\n\n'
            'This REPLACES the current habits, water, XP and selected theme. '
            'A local emergency copy will be saved first. Notification master settings remain local.',
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialog, false),
                child: const Text('Cancel')),
            FilledButton(onPressed: () => Navigator.pop(dialog, true),
                child: const Text('Restore backup')),
          ],
        ),
      );
      if (approved != true || !mounted) return;
      await _saveQueue;
      final prefs = await SharedPreferences.getInstance();
      // Local rollback checkpoint survives even if applying new data fails.
      await prefs.setString('betterseed_pre_restore_checkpoint_v1',
          _snapshotForDrive().encode());
      final today = todayLocalKey();
      setState(() {
        _habits = backup.day == today
            ? List<Habit>.from(backup.habits)
            : backup.habits.map((h) => h.copyWith(completed: false)).toList();
        _progress = backup.progress;
        _today = today;
        _moonlightOwned = backup.ownedMoonlight;
        _shopLoadout = backup.packId == 'moonlight'
            ? ShopLoadout.moonlightPack : ShopLoadout.defaultPack;
      });
      await _save();
      _driveMessage('Backup restored. Garden, water and checklist updated.');
    } catch (e) {
      _driveMessage('Restore failed; cloud backup and existing local data were not deleted. $e');
    } finally {
      if (mounted) setState(() => _driveBusy = false);
    }
  }

  Future<void> _restorePreviousLocal() async {
    if (_driveBusy || !_loaded) return;
    final prefs = await SharedPreferences.getInstance();
    final checkpoint = prefs.getString('betterseed_pre_restore_checkpoint_v1');
    if (!mounted) return;
    if (checkpoint == null) {
      _driveMessage('No previous local restore checkpoint found.');
      return;
    }
    BackupSnapshot backup;
    try {
      backup = BackupSnapshot.decode(checkpoint);
    } catch (_) {
      _driveMessage('The previous local checkpoint cannot be read.');
      return;
    }
    final ok = await showDialog<bool>(
      context: context,
      builder: (dialog) => AlertDialog(
        title: const Text('Undo last restore?'),
        content: Text('Restore ${backup.habits.length} habits and ${backup.progress.xp} XP from the local checkpoint?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialog, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialog, true), child: const Text('Undo restore')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    await _saveQueue;
    final today = todayLocalKey();
    setState(() {
      _habits = backup.day == today
          ? List<Habit>.from(backup.habits)
          : backup.habits.map((h) => h.copyWith(completed: false)).toList();
      _progress = backup.progress;
      _today = today;
      _moonlightOwned = backup.ownedMoonlight;
      _shopLoadout = backup.packId == 'moonlight'
          ? ShopLoadout.moonlightPack : ShopLoadout.defaultPack;
    });
    await _save();
    await prefs.remove('betterseed_pre_restore_checkpoint_v1');
    _driveMessage('Your previous local progress has been restored.');
  }

  Future<void> _openSettings() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: const Color(0xFF102D31),
      builder: (sheetContext) => StatefulBuilder(
        builder: (sheetContext, refresh) => DraggableScrollableSheet(
          expand: false,
          initialChildSize: .72,
          minChildSize: .46,
          maxChildSize: .92,
          builder: (sheetContext, scrollController) => SafeArea(
            top: false,
            // Eagerly mount all Settings controls, including those below
            // the fold. This allows accessibility/finder discovery regardless
            // of sheet height; the content still scrolls with the sheet.
            child: SingleChildScrollView(
              key: const ValueKey('settings-scroll'),
              controller: scrollController,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 3, 20, 26),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.settings_rounded, color: _mint, size: 26),
                        SizedBox(width: 10),
                        Text('Settings',
                            style: TextStyle(fontSize: 23, fontWeight: FontWeight.w800)),
                      ],
                    ),
                    const SizedBox(height: 6),
                    const Text('Make your garden feel just right.',
                        style: TextStyle(color: Color(0xFFCDDED5), fontSize: 12)),
                    const SizedBox(height: 22),
                    const Text('SOUND & GARDEN',
                        style: TextStyle(color: _mint, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 8),
                    SwitchListTile.adaptive(
                      title: const Text('Forest piano & nature'),
                      subtitle: const Text('Ambient music while the app is open'),
                      secondary: const Icon(Icons.music_note_rounded, color: _gold),
                      value: _musicEnabled,
                      onChanged: (v) { _setMusic(v); refresh(() {}); },
                    ),
                    SwitchListTile.adaptive(
                      title: const Text('Gentle sound effects'),
                      subtitle: const Text('Checklist and watering sounds'),
                      secondary: const Icon(Icons.graphic_eq_rounded, color: _gold),
                      value: _effectsEnabled,
                      onChanged: (v) { _setEffects(v); refresh(() {}); },
                    ),
                    SwitchListTile.adaptive(
                      title: const Text('Living garden'),
                      subtitle: const Text('Continuous butterflies and fireflies'),
                      secondary: const Icon(Icons.auto_awesome, color: _mint),
                      value: _motionEnabled,
                      onChanged: (v) { _setMotion(v); refresh(() {}); },
                    ),
                    SwitchListTile.adaptive(
                      key: const ValueKey('garden-motion-override'),
                      title: const Text('Keep Garden effects animated'),
                      subtitle: const Text(
                        'Explicitly play garden effects even when device animations are disabled',
                      ),
                      secondary: const Icon(Icons.auto_awesome_outlined, color: _mint),
                      value: _motionOverride,
                      onChanged: _motionEnabled ? (v) {
                        _setMotionOverride(v);
                        refresh(() {});
                      } : null,
                    ),
                    if (_systemReduceMotion)
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
                        child: Text(
                          _motionOverride
                              ? 'Garden effects are enabled by your choice. You can switch them off anytime.'
                              : 'Android currently requests reduced motion. Enable the option above if you want Garden effects anyway.',
                          style: const TextStyle(fontSize: 12, color: Color(0xFFCEDFD7)),
                        ),
                      ),
                    const SizedBox(height: 24),
                    const Text('DAILY ROUTINE',
                        style: TextStyle(color: _mint, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 8),
                    SwitchListTile.adaptive(
                      key: const ValueKey('reminder-toggle'),
                      secondary: const Icon(Icons.notifications_active_outlined, color: _mint),
                      title: const Text('Notifications master switch'),
                      subtitle: const Text('Enable or pause reminders for all habits'),
                      value: _reminderEnabled,
                      onChanged: _reminderBusy ? null : (value) async {
                        await _configureReminder(value);
                        if (sheetContext.mounted) refresh(() {});
                      },
                    ),
                    const Padding(
                      padding: EdgeInsets.fromLTRB(16, 2, 12, 14),
                      child: Text('Choose a reminder time separately when you add or edit each habit. Only habits with reminders enabled will notify you.',
                          style: TextStyle(color: Color(0xFFBCD1C8), fontSize: 12)),
                    ),
                    const Divider(),
                    ListTile(
                      key: const ValueKey('widget-add'),
                      leading: const Icon(Icons.widgets_outlined, color: _mint),
                      title: const Text('Home screen widget'),
                      subtitle: const Text('See tree stage, water and checklist at a glance'),
                      trailing: const Icon(Icons.add_circle_outline_rounded, color: _mint),
                      onTap: _installHomeWidget,
                    ),
                    const Divider(),
                    const SizedBox(height: 12),
                    const Text('GOOGLE DRIVE BACKUP',
                        style: TextStyle(color: _mint, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 8),
                    const Text(
                      'Your habits and garden stay on this phone. Connect Google Drive for a manual backup or restore when online. No automatic overwrite.',
                      style: TextStyle(color: Color(0xFFBCD1C8), fontSize: 12),
                    ),
                    const SizedBox(height: 8),
                    AnimatedBuilder(
                      animation: _driveBackup,
                      builder: (context, _) => Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          ListTile(
                            key: const ValueKey('drive-connect'),
                            leading: const Icon(Icons.cloud_outlined, color: _mint),
                            title: Text(_driveBackup.isConnected
                                ? 'Connected to Google Drive' : 'Connect Google Drive'),
                            subtitle: Text(_driveBackup.email ?? 'Private app backup · Google account required'),
                            onTap: (_driveBusy || _driveBackup.isConnected)
                                ? null : () async {
                                    await _connectDrive();
                                    if (sheetContext.mounted) refresh(() {});
                                  },
                          ),
                          if (_driveBackup.isConnected) ...[
                            Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                              child: Wrap(spacing: 9, runSpacing: 9, children: [
                                FilledButton.icon(
                                  key: const ValueKey('drive-backup-now'),
                                  onPressed: _driveBusy ? null : () async {
                                    await _backupNow();
                                    if (sheetContext.mounted) refresh(() {});
                                  },
                                  icon: const Icon(Icons.cloud_upload_outlined),
                                  label: const Text('Backup now'),
                                ),
                                OutlinedButton.icon(
                                  key: const ValueKey('drive-restore'),
                                  onPressed: _driveBusy ? null : () async {
                                    await _restoreFromDrive();
                                    if (sheetContext.mounted) refresh(() {});
                                  },
                                  icon: const Icon(Icons.restore_rounded),
                                  label: const Text('Restore'),
                                ),
                              ]),
                            ),
                            TextButton.icon(
                              key: const ValueKey('drive-disconnect'),
                              onPressed: _driveBusy ? null : () async {
                                await _driveBackup.disconnect();
                                if (sheetContext.mounted) refresh(() {});
                                _driveMessage('Google account disconnected. Local data is safe.');
                              },
                              icon: const Icon(Icons.logout, size: 17),
                              label: const Text('Disconnect Google account'),
                            ),
                          ],
                          if (_driveBusy) const LinearProgressIndicator(),
                        ],
                      ),
                    ),
                    TextButton.icon(
                      key: const ValueKey('drive-undo-local'),
                      onPressed: _driveBusy ? null : () async {
                        await _restorePreviousLocal();
                        if (sheetContext.mounted) refresh(() {});
                      },
                      icon: const Icon(Icons.undo_rounded, color: _gold),
                      label: const Text('Undo last cloud restore (if available)'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final stage = _progress.treeStage;
    return PopScope(
      canPop: !_focusMode,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && _focusMode) _setFocusMode(false);
      },
      child: Scaffold(
        body: Stack(
        fit: StackFit.expand,
        children: [
          AdaptiveGardenBackground(asset: _activeBackgroundAsset),
          const DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                stops: [0, .20, .75, 1],
                colors: [
                  Color(0x85071820), Color(0x26071921),
                  Color(0x00071921), Color(0x7003151A),
                ],
              ),
            ),
          ),
          if (_loaded && _showAnimation && _shopLoadout.packId == 'moonlight')
            Positioned.fill(
              child: IgnorePointer(
                child: AnimatedBuilder(
                  animation: _windController,
                  builder: (_, __) => RepaintBoundary(
                    child: CustomPaint(
                      key: const ValueKey('moonlight-dynamic-background'),
                      painter: MoonlightAmbiencePainter(_windController.value),
                    ),
                  ),
                ),
              ),
            ),
          if (_focusMode)
            Positioned.fill(
              child: LayoutBuilder(
                builder: (context, constraints) => Stack(
                  children: [
                    Positioned.fill(
                      top: constraints.maxHeight * .16,
                      bottom: constraints.maxHeight * .18,
                      child: _buildTreeScene(stage),
                    ),
                    SafeArea(
                      child: Align(
                        alignment: Alignment.topRight,
                        child: Padding(
                          padding: const EdgeInsets.all(14),
                          child: Material(
                            color: const Color(0xDC09272C),
                            borderRadius: BorderRadius.circular(18),
                            child: IconButton(
                              key: const ValueKey('exit-focus-mode'),
                              tooltip: 'Exit focus mode',
                              onPressed: () => _setFocusMode(false),
                              icon: const Icon(Icons.fullscreen_exit_rounded,
                                  color: _mint, size: 28),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            )
          else SafeArea(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 600),
                child: Column(
                  children: [
                    _buildHeader(),
                    Expanded(
                      child: _loaded
                          ? IndexedStack(
                              index: _selectedTab,
                              sizing: StackFit.expand,
                              children: [
                                _buildGarden(stage),
                                _buildChecklist(),
                                _buildStats(),
                                BetterSeedShopPage(
                                  loadout: _shopLoadout,
                                  onApplyLoadout: _applyShopLoadout,
                                  moonlightOwned: _moonlightOwned,
                                  onClaimMoonlight: _claimMoonlight,
                                ),
                              ],
                            )
                          : const Center(child: CircularProgressIndicator(color: _mint)),
                    ),
                    _buildNavigation(),
                  ],
                ),
              ),
            ),
          ),
        ],
        ),
      ),
    );
  }

  Widget _glassPanel({required Widget child, EdgeInsetsGeometry? padding}) => Container(
        padding: padding ?? const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _glass,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0x777AC5A4)),
          boxShadow: const [
            BoxShadow(color: Color(0x44000000), blurRadius: 18, offset: Offset(0, 5)),
          ],
        ),
        child: child,
      );

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 6, 16, 8),
      child: Row(
        children: [
          Image.asset('assets/icon_512.png', height: 36, width: 36),
          const SizedBox(width: 8),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('BetterSeed',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
                Text('Grow one step at a time',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 10, color: Color(0xFFCFDED6))),
              ],
            ),
          ),
          Semantics(
            label: '${_progress.waterBalance} water drops available',
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xD10B3033),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: const Color(0x8876CEF8)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.water_drop_rounded, size: 18, color: _blue),
                  const SizedBox(width: 3),
                  Text('${_progress.waterBalance}',
                      style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                ],
              ),
            ),
          ),
          const SizedBox(width: 5),
          IconButton(
            tooltip: 'Settings',
            onPressed: _openSettings,
            icon: const Icon(Icons.settings_rounded, color: _mint, size: 23),
          ),
        ],
      ),
    );
  }

  // The nav bar has four tabs. Its height must account for the current text
  // scaling factor; a fixed 64px bar could overflow by a few pixels on short
  // screens (and more when Android accessibility text is enlarged).
  Widget _buildNavigation() {
    final scaledLabelHeight = MediaQuery.textScalerOf(context).scale(11) * 1.45;
    final contentHeight = 22.0 + 3.0 + scaledLabelHeight + 3.0 + 2.0;
    final navHeight = math.max(72.0, contentHeight + 16.0);
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 3, 12, 8),
      child: Container(
        height: navHeight,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          color: const Color(0xEE092529),
          border: Border.all(color: const Color(0x9874AD96)),
        ),
        child: Row(
          children: [
            _navItem(0, Icons.spa_rounded, 'Garden'),
            _navItem(1, Icons.fact_check_outlined, 'Checklist'),
            _navItem(2, Icons.bar_chart_rounded, 'Stats'),
            _navItem(3, Icons.storefront_outlined, 'Shop'),
          ],
        ),
      ),
    );
  }

  Widget _navItem(int index, IconData icon, String label) {
    final active = index == _selectedTab;
    return Expanded(
      child: Semantics(
        selected: active,
        button: true,
        label: '$label tab',
        child: InkWell(
          borderRadius: BorderRadius.circular(18),
          key: ValueKey('tab-$index'),
          onTap: () => _changeTab(index),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: active ? _mint : const Color(0xFFBDCCC8), size: 22),
              const SizedBox(height: 3),
              Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: active ? FontWeight.w800 : FontWeight.w500,
                  color: active ? _mint : const Color(0xFFCFDEDA),
                ),
              ),
              const SizedBox(height: 3),
              Container(
                width: 28, height: 2,
                decoration: BoxDecoration(
                  color: active ? _mint : Colors.transparent,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGarden(int stage) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 3),
          child: Column(
            children: [
              Row(
                children: [
                  const Icon(Icons.park_outlined, size: 18, color: _mint),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(_stageNames[stage],
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800)),
                  ),
                  Text('Stage ${stage + 1} / 6',
                      style: const TextStyle(fontSize: 12, color: Color(0xFFE4F5E9))),
                ],
              ),
              const SizedBox(height: 7),
              ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: LinearProgressIndicator(
                  value: progressToNextStage(_progress.xp),
                  minHeight: 5, color: _mint,
                  backgroundColor: const Color(0x80617E73),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: Stack(
            children: [
              Positioned.fill(child: _buildTreeScene(stage)),
              Positioned(
                right: 12,
                top: 8,
                child: Material(
                  color: const Color(0xBC0A2B30),
                  borderRadius: BorderRadius.circular(15),
                  child: IconButton(
                    key: const ValueKey('enter-focus-mode'),
                    tooltip: 'Focus mode',
                    onPressed: () => _setFocusMode(true),
                    icon: const Icon(Icons.fullscreen_rounded, color: _mint, size: 25),
                  ),
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 1, 12, 8),
          child: _glassPanel(
            padding: const EdgeInsets.fromLTRB(12, 8, 8, 8),
            child: Row(
              children: [
                const Icon(Icons.water_drop_rounded, color: _blue, size: 27),
                const SizedBox(width: 7),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${_progress.waterBalance} water available',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13)),
                      Text(
                        _progress.waterBalance == 0
                            ? 'Complete a habit to earn water'
                            : 'Give your tree a drink to help it grow',
                        maxLines: 2,
                        style: const TextStyle(fontSize: 10, color: Color(0xFFD1E1DA)),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 6),
                FilledButton.icon(
                  key: const ValueKey('water-tree'),
                  onPressed: _waterTree,
                  icon: Icon(_progress.waterBalance > 0
                      ? Icons.water_drop_outlined : Icons.fact_check_outlined,
                      size: 18),
                  label: Text(_progress.waterBalance > 0 ? 'Water' : 'Earn'),
                  style: FilledButton.styleFrom(
                    backgroundColor: _mint,
                    foregroundColor: _deepNavy,
                    padding: const EdgeInsets.symmetric(horizontal: 13),
                    minimumSize: const Size(84, 44),
                    textStyle: const TextStyle(fontWeight: FontWeight.w800, fontSize: 12),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTreeScene(int stage) => LayoutBuilder(
        builder: (context, constraints) {
          final w = constraints.maxWidth;
          final h = constraints.maxHeight;
          final baseSize = GardenSceneGeometry.treeSize(Size(w, h));
          // Sprites are normalized per stage; use one size for both packs.
          final size = baseSize;
          final groundWidth = math.min(w * .70, baseSize * .82);
          final scene = Size(w, h);
          final groundBottom = GardenSceneGeometry.groundBottom(
            scene, ground: _activePlantingGround);
          return Stack(
            clipBehavior: Clip.hardEdge,
            children: [
              Positioned(
                bottom: GardenSceneGeometry.plantingPlaneBottom(
                  scene, ground: _activePlantingGround),
                left: (w - groundWidth) / 2,
                width: groundWidth,
                height: GardenSceneGeometry.plantingPlaneHeight(scene),
                child: RepaintBoundary(
                  child: CustomPaint(painter: _GroundContactPainter(
                    // Theme palettes change without altering the root anchor.
                    moonlight: _shopLoadout.packId == 'moonlight',
                  )),
                ),
              ),
              Positioned(
                left: (w - size) / 2,
                bottom: groundBottom - size * .04,
                width: size,
                height: size,
                child: RepaintBoundary(
                  child: AnimatedBuilder(
                    animation: _windController,
                    builder: (context, child) {
                      if (!_showAnimation) return child!;
                      final sway = math.sin(_windController.value * math.pi * 2);
                      return Transform.rotate(
                        angle: sway * .005,
                        alignment: Alignment.bottomCenter,
                        child: child,
                      );
                    },
                    child: AnimatedSwitcher(
                      duration: _showAnimation
                          ? const Duration(milliseconds: 700) : Duration.zero,
                      layoutBuilder: (current, previous) => Stack(
                        alignment: Alignment.bottomCenter,
                        children: [...previous, if (current != null) current],
                      ),
                      transitionBuilder: (child, animation) => FadeTransition(
                        opacity: animation,
                        child: ScaleTransition(
                          alignment: Alignment.bottomCenter,
                          scale: Tween<double>(begin: .83, end: 1).animate(
                            CurvedAnimation(parent: animation, curve: Curves.easeOutCubic),
                          ),
                          child: child,
                        ),
                      ),
                      child: Image.asset(
                        _activeTreeImages[stage],
                        key: ValueKey(stage),
                        width: size,
                        height: size,
                        fit: BoxFit.contain,
                        filterQuality: FilterQuality.medium,
                        gaplessPlayback: true,
                      ),
                    ),
                  ),
                ),
              ),
              if (_showAnimation)
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedBuilder(
                      animation: _windController,
                      builder: (_, __) => RepaintBoundary(
                        child: Stack(
                          fit: StackFit.expand,
                          children: [
                            CustomPaint(
                              key: const ValueKey('garden-live-ambient'),
                              painter: GardenLifePainter(_windController.value, palette: _activeFireflyPalette),
                            ),
                            ..._buildButterflySprites(w, h, _windController.value),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              if (_showAnimation)
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedBuilder(
                      animation: _waterController,
                      builder: (_, __) => Stack(
                        fit: StackFit.expand,
                        children: [
                          CustomPaint(
                            key: const ValueKey('garden-watering-stream'),
                            painter: _WateringPainter(_waterController.value, _activePlantingGround),
                          ),
                          ..._buildWateringCanOverlay(w, h, _waterController.value),
                        ],
                      ),
                    ),
                  ),
                ),
              if (h > 260)
                const Positioned(
                  top: 8, left: 12, right: 12,
                  child: Text('Small steps. Brighter tomorrows. ✨',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Color(0xFFF0FBEA), fontSize: 11,
                        shadows: [Shadow(blurRadius: 10, color: Colors.black)],
                      )),
                ),
            ],
          );
        },
      );


  List<Widget> _buildButterflySprites(double width, double height, double t) {
    return [
      _butterflySprite(
        width: width,
        height: height,
        progress: t,
        anchorX: .22,
        anchorY: .42,
        size: 68,
        orbitX: 22,
        orbitY: 16,
        phase: 0,
        flip: false,
      ),
      _butterflySprite(
        width: width,
        height: height,
        progress: t,
        anchorX: .76,
        anchorY: .37,
        size: 60,
        orbitX: 20,
        orbitY: 14,
        phase: 2.2,
        flip: true,
      ),
      _butterflySprite(
        width: width,
        height: height,
        progress: t,
        anchorX: .63,
        anchorY: .62,
        size: 54,
        orbitX: 16,
        orbitY: 11,
        phase: 4.1,
        flip: false,
      ),
    ];
  }

  Widget _butterflySprite({
    required double width,
    required double height,
    required double progress,
    required double anchorX,
    required double anchorY,
    required double size,
    required double orbitX,
    required double orbitY,
    required double phase,
    required bool flip,
  }) {
    const tau = math.pi * 2;
    final x = width * anchorX + math.sin(tau * progress + phase) * orbitX;
    final y = height * anchorY + math.sin(tau * progress * 2 + phase) * orbitY;
    // 28 complete flap cycles per ambient loop, with no phase jump.
    final flap = .62 + .38 * math.sin(tau * 28 * progress + phase).abs();
    final fade = .88 + .12 * math.sin(tau * 4 * progress + phase).abs();
    final angle = .13 * math.sin(tau * progress + phase);
    return Positioned(
      left: x - size / 2,
      top: y - size / 2,
      width: size,
      height: size,
      child: Opacity(
        opacity: fade,
        child: Transform.rotate(
          angle: angle,
          child: Transform(
            alignment: Alignment.center,
            transform: Matrix4.diagonal3Values(
              (flip ? -1.0 : 1.0) * flap,
              .94 + .08 * math.sin(tau * 6 * progress + phase).abs(),
              1,
            ),
            child: Image.asset(_activeButterflyAsset, fit: BoxFit.contain),
          ),
        ),
      ),
    );
  }

  List<Widget> _buildWateringCanOverlay(double width, double height, double progress) {
    if (progress <= 0 || progress >= 1) return const <Widget>[];
    final fade = math.min(1.0, (1 - progress) * 3).toDouble();
    final scene = Size(width, height);
    final size = GardenSceneGeometry.wateringCanSize(scene);
    final center = GardenSceneGeometry.wateringCanCenter(scene, progress);
    final angle = GardenSceneGeometry.wateringCanAngle(progress);
    return [
      Positioned(
        left: center.dx - size / 2,
        top: center.dy - size / 2,
        width: size,
        height: size,
        child: Opacity(
          opacity: fade,
          child: Transform.rotate(
            angle: angle,
            alignment: Alignment.center,
            child: Image.asset(_activeWateringCanAsset, fit: BoxFit.contain),
          ),
        ),
      ),
    ];
  }

  Widget _buildChecklist() {
    final completed = _completed;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
          child: _glassPanel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Today’s Checklist',
                    style: TextStyle(fontSize: 23, fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                const Text('Complete habits to earn water for your tree 💧',
                    style: TextStyle(color: Color(0xFFD1DFD9), fontSize: 12)),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: LinearProgressIndicator(
                        value: _habits.isEmpty ? 0 : completed / _habits.length,
                        minHeight: 7,
                        borderRadius: BorderRadius.circular(10),
                        color: _mint,
                        backgroundColor: const Color(0xAA527368),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text('$completed/${_habits.length}',
                        style: const TextStyle(color: _mint, fontWeight: FontWeight.bold)),
                  ],
                ),
              ],
            ),
          ),
        ),
        if (!_reminderEnabled && _habits.any((h) => h.reminderEnabled))
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Material(
              key: const ValueKey('reminder-master-warning'),
              color: const Color(0xED4A3828),
              borderRadius: BorderRadius.circular(14),
              child: Padding(
                padding: const EdgeInsets.all(11),
                child: Row(children: [
                  const Icon(Icons.notifications_off_outlined, color: _gold),
                  const SizedBox(width: 8),
                  const Expanded(child: Text(
                    'Habit reminders are paused because master notifications are off.',
                    style: TextStyle(fontSize: 12),
                  )),
                  TextButton(onPressed: () => _configureReminder(true),
                    child: const Text('Enable')),
                ]),
              ),
            ),
          ),
        Expanded(
          child: _habits.isEmpty
              ? const Center(child: Text('Plant your first daily habit 🌱'))
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 3, 16, 8),
                  itemCount: _habits.length,
                  itemBuilder: (context, index) => Padding(
                    padding: const EdgeInsets.only(bottom: 9),
                    child: _habitTile(_habits[index]),
                  ),
                ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 3, 16, 12),
          child: SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _addHabit,
              icon: const Icon(Icons.add_circle_outline),
              label: const Text('Add a daily habit'),
              style: FilledButton.styleFrom(
                foregroundColor: _deepNavy,
                backgroundColor: _mint,
                minimumSize: const Size(0, 52),
                textStyle: const TextStyle(fontWeight: FontWeight.w800),
              ),
            ),
          ),
        ),
      ],
    );
  }

  IconData _iconDataForKey(String key) {
    for (final choice in _habitIconChoices) {
      if (choice.key == key) return choice.value;
    }
    return Icons.spa_outlined;
  }

  IconData _iconFor(Habit habit) {
    if (habit.iconKey.isNotEmpty) return _iconDataForKey(habit.iconKey);
    final title = habit.title.toLowerCase();
    if (title.contains('water') || title.contains('drink')) return Icons.local_drink_outlined;
    if (title.contains('read') || title.contains('book') || title.contains('study')) {
      return Icons.menu_book_outlined;
    }
    if (title.contains('save') || title.contains('money')) return Icons.savings_outlined;
    if (title.contains('walk') || title.contains('move') || title.contains('run')) {
      return Icons.directions_run_outlined;
    }
    return Icons.spa_outlined;
  }

  Widget _habitTile(Habit habit) {
    final credited = _progress.wasCredited(_today, habit.id);
    return Material(
      color: _glass,
      borderRadius: BorderRadius.circular(17),
      child: InkWell(
        borderRadius: BorderRadius.circular(17),
        onTap: () => _toggleHabit(habit),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 11),
          child: Row(
            children: [
              SizedBox(
                height: 44, width: 44,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: const Color(0xAA1E4947),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(_iconFor(habit), color: _gold, size: 24),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(habit.title,
                        maxLines: 2, overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 15, fontWeight: FontWeight.w700,
                          decoration: habit.completed ? TextDecoration.lineThrough : null,
                        )),
                    Text(credited ? 'Water earned today' : 'Complete to earn +1 water',
                        style: const TextStyle(fontSize: 11, color: Color(0xFFBBD1CA))),
                    if (habit.reminderEnabled)
                      Text('Reminder ${TimeOfDay(hour: habit.reminderHour, minute: habit.reminderMinute).format(context)}${_reminderEnabled ? '' : ' · paused'}',
                        style: const TextStyle(fontSize: 10, color: _gold)),
                  ],
                ),
              ),
              const Icon(Icons.water_drop_rounded, size: 16, color: _blue),
              const SizedBox(width: 2),
              Text(credited ? '✓' : '+1', style: const TextStyle(color: _blue, fontSize: 12)),
              Checkbox(
                key: ValueKey('habit-${habit.id}'),
                value: habit.completed,
                onChanged: (_) => _toggleHabit(habit),
                activeColor: _mint,
                checkColor: _deepNavy,
              ),
              PopupMenuButton<String>(
                tooltip: 'Habit options',
                icon: const Icon(Icons.more_vert, size: 20),
                onSelected: (value) {
                  if (value == 'remove') _removeHabit(habit);
                  if (value == 'edit') _editHabit(habit);
                },
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'edit', child: Text('Edit habit / reminder')),
                  PopupMenuItem(value: 'remove', child: Text('Remove habit')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _statCard(IconData icon, String value, String title, Color accent) =>
      _glassPanel(
        padding: const EdgeInsets.all(13),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: accent, size: 25),
            const SizedBox(height: 8),
            Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
            Text(title, style: const TextStyle(fontSize: 11, color: Color(0xFFCFDFD8))),
          ],
        ),
      );

  Widget _buildStats() {
    final now = DateTime.now();
    final days = List<DateTime>.generate(7, (i) =>
        DateTime(now.year, now.month, now.day - (6 - i)));
    final values = days.map((day) => _progress.earnedOn(todayLocalKey(day))).toList();
    final maxValue = math.max(1, values.fold<int>(0, (a, b) => math.max(a, b)));
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 18),
      children: [
        const Text('Your Progress', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800)),
        const Text('Every small win adds up.',
            style: TextStyle(color: Color(0xFFD7E9DD), fontSize: 12)),
        const SizedBox(height: 14),
        Row(children: [
          Expanded(child: _statCard(Icons.local_fire_department_rounded,
              '${_progress.dayStreak(now)}', 'Day streak', _gold)),
          const SizedBox(width: 10),
          Expanded(child: _statCard(Icons.water_drop_rounded,
              '${_progress.totalEarned}', 'Water earned', _blue)),
        ]),
        const SizedBox(height: 10),
        Row(children: [
          Expanded(child: _statCard(Icons.task_alt_rounded,
              '${_progress.totalEarned}', 'Habits credited', _mint)),
          const SizedBox(width: 10),
          Expanded(child: _statCard(Icons.spa_rounded,
              '${_progress.treeStage + 1} / 6', 'Tree stage', _mint)),
        ]),
        const SizedBox(height: 16),
        _glassPanel(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('This week', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              const Text('Water earned from daily checklists',
                  style: TextStyle(fontSize: 11, color: Color(0xFFD1DFD9))),
              const SizedBox(height: 17),
              SizedBox(
                height: 132,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    for (var i = 0; i < days.length; i++)
                      Expanded(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            Text('${values[i]}',
                                style: const TextStyle(color: _blue, fontSize: 10)),
                            const SizedBox(height: 5),
                            Container(
                              width: 22,
                              height: 8 + 75 * values[i] / maxValue,
                              decoration: BoxDecoration(
                                color: i == 6 ? _mint : const Color(0xFF5DC8A3),
                                borderRadius: BorderRadius.circular(6),
                              ),
                            ),
                            const SizedBox(height: 7),
                            Text(const ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
                                [days[i].weekday - 1],
                                style: const TextStyle(fontSize: 9, color: Color(0xFFDBE7E3))),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _glassPanel(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Tree growth • ${_progress.xp} XP',
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              const SizedBox(height: 12),
              SizedBox(
                height: 90,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: _activeTreeImages.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 6),
                  itemBuilder: (_, i) => Column(
                    children: [
                      SizedBox(
                        width: 62, height: 62,
                        child: Opacity(
                          opacity: i <= _progress.treeStage ? 1 : .40,
                          child: Image.asset(_activeTreeImages[i], fit: BoxFit.contain),
                        ),
                      ),
                      Text('${i + 1}', style: TextStyle(
                        fontSize: 11,
                        color: i <= _progress.treeStage ? _mint : Colors.white60,
                      )),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 9),
              LinearProgressIndicator(
                value: progressToNextStage(_progress.xp),
                backgroundColor: const Color(0xFF527368),
                color: _mint,
                minHeight: 6,
                borderRadius: BorderRadius.circular(5),
              ),
              const SizedBox(height: 8),
              Text('${_progress.totalSpent} water drops used to grow your tree',
                  style: const TextStyle(fontSize: 11, color: Color(0xFFD1DFD9))),
            ],
          ),
        ),
      ],
    );
  }

}

class _HabitDraft {
  const _HabitDraft({required this.title, required this.iconKey,
      required this.reminderEnabled, required this.hour, required this.minute});
  final String title;
  final String iconKey;
  final bool reminderEnabled;
  final int hour;
  final int minute;
}

class _HabitComposer extends StatefulWidget {
  const _HabitComposer({this.initial, required this.masterEnabled,
      required this.onEnableMaster});
  final Habit? initial;
  final bool masterEnabled;
  final Future<bool> Function() onEnableMaster;

  @override
  State<_HabitComposer> createState() => _HabitComposerState();
}

class _HabitComposerState extends State<_HabitComposer> {
  late final TextEditingController _controller;
  late String _iconKey;
  late bool _hasReminder;
  late bool _masterActive;
  bool _enablingMaster = false;
  late TimeOfDay _reminderTime;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initial?.title ?? '');
    _iconKey = widget.initial?.iconKey ?? 'leaf';
    _hasReminder = widget.initial?.reminderEnabled ?? false;
    _masterActive = widget.masterEnabled;
    _reminderTime = TimeOfDay(hour: widget.initial?.reminderHour ?? 20,
        minute: widget.initial?.reminderMinute ?? 0);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final title = _controller.text.trim();
    if (title.isEmpty) return;
    Navigator.pop(context, _HabitDraft(title: title, iconKey: _iconKey,
      reminderEnabled: _hasReminder,
      hour: _reminderTime.hour, minute: _reminderTime.minute));
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context, initialTime: _reminderTime,
      helpText: 'Remind me about this habit',
    );
    if (picked != null && mounted) setState(() => _reminderTime = picked);
  }

  @override
  Widget build(BuildContext context) {
    final keyboard = MediaQuery.viewInsetsOf(context).bottom;
    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.only(bottom: keyboard),
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: math.max(190.0, MediaQuery.sizeOf(context).height * .86 - keyboard)),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 5, 20, 22),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(widget.initial == null ? 'Plant a new habit' : 'Edit habit',
                    style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                const SizedBox(height: 5),
                const Text('Small actions grow into something beautiful.'),
                const SizedBox(height: 15),
                TextField(
                  controller: _controller,
                  autofocus: widget.initial == null,
                  maxLength: 48,
                  textCapitalization: TextCapitalization.sentences,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _submit(),
                  decoration: InputDecoration(
                    hintText: 'e.g. Read 10 pages',
                    filled: true,
                    fillColor: const Color(0xFF234640),
                    border: OutlineInputBorder(
                      borderSide: BorderSide.none,
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                ),
                const Text('Pick an icon',
                    style: TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10, runSpacing: 10,
                  children: [
                    for (final choice in _habitIconChoices)
                      Semantics(
                        button: true, selected: _iconKey == choice.key,
                        label: '${choice.key} habit icon',
                        child: InkWell(
                          onTap: () => setState(() => _iconKey = choice.key),
                          borderRadius: BorderRadius.circular(14),
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 180),
                            width: 54, height: 54,
                            decoration: BoxDecoration(
                              color: _iconKey == choice.key
                                  ? const Color(0xFF2A5F56) : const Color(0xFF234640),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(
                                color: _iconKey == choice.key ? _mint : const Color(0x556FC09D),
                                width: _iconKey == choice.key ? 1.6 : 1,
                              ),
                            ),
                            child: Icon(choice.value,
                                color: _iconKey == choice.key ? _mint : _gold, size: 26),
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                SwitchListTile.adaptive(
                  key: const ValueKey('habit-reminder-toggle'),
                  contentPadding: EdgeInsets.zero,
                  secondary: const Icon(Icons.notifications_active_outlined, color: _mint),
                  title: const Text('Remind me daily'),
                  subtitle: Text(_masterActive
                      ? 'Set a time for this habit' : 'Master notifications are OFF — reminders will be paused'),
                  value: _hasReminder,
                  onChanged: (value) => setState(() => _hasReminder = value),
                ),
                if (_hasReminder && !_masterActive)
                  Container(
                    key: const ValueKey('composer-master-off-warning'),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFF4C3B26),
                      borderRadius: BorderRadius.circular(13),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Master notifications are disabled. This habit will NOT notify you until enabled.',
                          style: TextStyle(color: Color(0xFFFFE5AC), fontSize: 12)),
                        const SizedBox(height: 6),
                        TextButton.icon(
                          key: const ValueKey('composer-enable-master'),
                          onPressed: _enablingMaster ? null : () async {
                            setState(() => _enablingMaster = true);
                            final success = await widget.onEnableMaster();
                            if (!mounted) return;
                            setState(() { _enablingMaster = false; _masterActive = success; });
                          },
                          icon: const Icon(Icons.notifications_active_outlined),
                          label: Text(_enablingMaster ? 'Enabling...' : 'Enable master notifications'),
                        ),
                      ],
                    ),
                  ),
                if (_hasReminder)
                  ListTile(
                    key: const ValueKey('habit-reminder-time'),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                    leading: const Icon(Icons.schedule_rounded, color: _gold),
                    title: const Text('Reminder time'),
                    subtitle: Text(_reminderTime.format(context)),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: _pickTime,
                  ),
                const SizedBox(height: 14),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: _submit,
                    style: FilledButton.styleFrom(backgroundColor: _mint,
                      foregroundColor: _deepNavy,
                      padding: const EdgeInsets.symmetric(vertical: 15)),
                    child: Text(widget.initial == null ? 'Create habit' : 'Save changes'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Ground plane: a contact shadow and a sunlit patch under the tree roots.
class _GroundContactPainter extends CustomPainter {
  const _GroundContactPainter({required this.moonlight});
  final bool moonlight;

  @override
  void paint(Canvas canvas, Size size) {
    final r = Rect.fromLTWH(0, 0, size.width, size.height);
    canvas.drawOval(
      r.deflate(2),
      Paint()
        ..color = (moonlight ? const Color(0xC50B1832) : const Color(0xCC11231A))
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 14),
    );
    canvas.drawOval(
      Rect.fromLTWH(size.width * .06, size.height * .20, size.width * .88, size.height * .54),
      Paint()
        ..shader = RadialGradient(
          colors: moonlight
              ? const [Color(0xC6715B62), Color(0xAD354353), Color(0x004A5478)]
              : const [Color(0xE27F713D), Color(0xC83B4825), Color(0x004A5428)],
          stops: const [0, .68, 1],
        ).createShader(r),
    );
  }

  @override
  bool shouldRepaint(covariant _GroundContactPainter oldDelegate) =>
      oldDelegate.moonlight != moonlight;
}


/// Screen-space drops pour into the existing soil contact point. Entirely
/// painted above the tree; never affects its constraints or root baseline.
class _WateringPainter extends CustomPainter {
  const _WateringPainter(this.progress, this.ground);
  final double progress;
  final PlantingGround ground;

  @override
  void paint(Canvas canvas, Size size) {
    if (progress == 0 || progress >= 1) return;
    final fade = math.min(1.0, (1 - progress) * 3);
    final origin = GardenSceneGeometry.wateringSpout(size, progress);
    final target = GardenSceneGeometry.rootTarget(size, ground: ground);
    final p = Paint()..color = _blue.withValues(alpha: fade);

    // Repeated bright droplets travel a curved stream to the fixed tree base.
    for (var i = 0; i < 26; i++) {
      final t = (progress * 1.55 + i / 26) % 1.0;
      final x = origin.dx + (target.dx - origin.dx) * t -
          25 * math.sin(t * math.pi) + math.sin(i * 2.8) * 4;
      final y = origin.dy + (target.dy - origin.dy) * t * t;
      final r = 2.3 + (i % 4) * .45;
      canvas.drawCircle(Offset(x, y), r * 2.4,
          Paint()..color = _blue.withValues(alpha: fade * .13));
      canvas.drawCircle(Offset(x, y), r, p);
    }
    if (progress > .33) {
      final burst = (1 - progress).clamp(0.0, 1.0);
      for (var i = 0; i < 12; i++) {
        final angle = (math.pi * 2 * i) / 12;
        final dist = 8 + progress * 29;
        final x = target.dx + math.cos(angle) * dist;
        final y = target.dy + math.sin(angle) * dist * .45;
        canvas.drawCircle(Offset(x, y), 1.4 + (i % 3),
            Paint()..color = _mint.withValues(alpha: fade * burst));
      }
    }
  }

  @override
  bool shouldRepaint(covariant _WateringPainter oldDelegate) =>
      oldDelegate.progress != progress || oldDelegate.ground != ground;
}
