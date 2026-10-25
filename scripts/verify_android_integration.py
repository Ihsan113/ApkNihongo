"""Check packaged native sources and their integration into generated Android files.

Run ``--sources`` before ``flutter create`` in CI to detect missing Git-tracked
native files early (including files accidentally hidden by .gitignore).
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
app = root / 'android/app/src/main'
native = root / 'native/android'

KOTLIN = (
    'MainActivity', 'ReminderScheduler', 'ReminderReceiver',
    'ReminderBootReceiver', 'BetterSeedWidget',
)
RESOURCES = (
    'layout/betterseed_widget.xml',
    'xml/betterseed_widget_info.xml',
    'drawable/widget_background.xml',
    'drawable/ic_notification.xml',
    *(f'drawable-nodpi/widget_tree_{i}.webp' for i in range(6)),
    'drawable-nodpi/widget_preview.webp',
    *(f'drawable-nodpi/widget_moon_tree_{i}.webp' for i in range(6)),
)


def require(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(
            f'MISSING {label}: {path}\n'
            'Native Android source files must be committed to the repository.\n'
            'Check that .gitignore uses "/android/" (not "android/") and\n'
            'commit the native/android/ folder from the new ZIP.'
        )


for name in KOTLIN:
    require(native / 'kotlin' / f'{name}.kt', 'native Kotlin source')
for name in RESOURCES:
    require(native / 'res' / name, 'native Android resource')
print('Native Android sources and widget resources are present.')

if '--sources' in sys.argv:
    raise SystemExit(0)

require(app / 'AndroidManifest.xml', 'generated Android manifest')
ns_name = '{http://schemas.android.com/apk/res/android}name'
manifest = ET.parse(app / 'AndroidManifest.xml').getroot()
permissions = {element.get(ns_name) for element in manifest.findall('uses-permission')}
missing_permissions = {'android.permission.POST_NOTIFICATIONS', 'android.permission.RECEIVE_BOOT_COMPLETED', 'android.permission.INTERNET'} - permissions
if missing_permissions:
    raise SystemExit(f'Missing Android permissions: {sorted(missing_permissions)}')
receivers = {r.get(ns_name) for r in manifest.findall('application/receiver')}
missing_receivers = {'.ReminderReceiver', '.ReminderBootReceiver', '.BetterSeedWidget'} - receivers
if missing_receivers:
    raise SystemExit(f'Missing Android receivers: {sorted(missing_receivers)}')
for name in KOTLIN:
    require(app / f'kotlin/com/example/betterseed/{name}.kt', 'generated Kotlin source')
for name in RESOURCES:
    file = app / 'res' / name
    require(file, 'generated Android resource')
    if file.suffix == '.xml':
        ET.parse(file)
print('Verified Android permissions, broadcast receivers, Kotlin classes, widget XML')
