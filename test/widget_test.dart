import 'package:betterseed/logic/growth.dart';
import 'package:flutter_test/flutter_test.dart';

// Prevent flutter create from generating the default counter-app widget test.
// UI test scenarios will be added after the first APK is verified.
void main() {
  test('BetterSeed ships six growth stages', () {
    expect(treeStageThresholds.length, 6);
  });
}
