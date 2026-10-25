import 'dart:math' as math;

import 'package:flutter/widgets.dart';

/// Geometry shared by the tree, watering can, and water stream.
/// All positions are calculated in the SAME local scene coordinates.
enum PlantingGround { defaultGarden, moonlightGarden }

class GardenSceneGeometry {
  const GardenSceneGeometry._();

  static double treeSize(Size scene) =>
      math.min(scene.width * .74, scene.height * .72)
          .clamp(88.0, 430.0)
          .toDouble();

  /// Each theme has a calibrated planting marker, so no expensive/fragile
  /// image-recognition is needed on the phone. Both the tree and its effects
  /// resolve against the same local scene geometry.
  static double groundBottom(Size scene, {
    PlantingGround ground = PlantingGround.defaultGarden,
  }) {
    final fraction = ground == PlantingGround.moonlightGarden ? .082 : .035;
    return math.max(10.0, scene.height * fraction);
  }

  static double plantingPlaneHeight(Size scene) =>
      math.max(24.0, treeSize(scene) * .15);

  /// The shadow plane is centered on the root target.
  static double plantingPlaneBottom(Size scene, {
    PlantingGround ground = PlantingGround.defaultGarden,
  }) => groundBottom(scene, ground: ground) - treeSize(scene) * .045;


  /// A stable root/soil contact point that never changes during watering.
  static Offset rootTarget(Size scene, {
    PlantingGround ground = PlantingGround.defaultGarden,
  }) {
    final tree = treeSize(scene);
    return Offset(scene.width * .5,
        scene.height - groundBottom(scene, ground: ground) - tree * .03);
  }

  static double wateringCanSize(Size scene) =>
      math.min(scene.width, scene.height) * .27;

  static Offset wateringCanCenter(Size scene, double progress) => Offset(
        scene.width * .77 + math.sin(progress * math.pi) * 10,
        scene.height * .23 + math.sin(progress * math.pi) * 6,
      );

  static double wateringCanAngle(double progress) =>
      -.20 - .24 * math.sin(progress * math.pi);

  /// The nozzle is near the left-hand tip of the illustrated watering can.
  /// Apply the SAME rotation used on the can sprite.
  static Offset wateringSpout(Size scene, double progress) {
    final center = wateringCanCenter(scene, progress);
    final size = wateringCanSize(scene);
    final angle = wateringCanAngle(progress);
    final dx = -size * .44;
    final dy = -size * .03;
    return Offset(
      center.dx + dx * math.cos(angle) - dy * math.sin(angle),
      center.dy + dx * math.sin(angle) + dy * math.cos(angle),
    );
  }
}
