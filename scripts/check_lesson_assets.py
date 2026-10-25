#!/usr/bin/env python3
"""Run locally against source assets or against compiled APK.
Fails the GitHub Actions build if lesson payloads didn't make it into Android APK.
"""
import gzip
import json
import sys
import zipfile
from pathlib import Path

assert len(sys.argv) == 2, 'Usage: python3 scripts/check_lesson_assets.py app-debug.apk|app/src/main/assets'
source = Path(sys.argv[1])
if source.is_file() and zipfile.is_zipfile(source):
    with zipfile.ZipFile(source) as z:
        base = 'assets/study/'
        idx = json.loads(z.read(base + 'index.json'))
        all_files = set(idx.values())
        not_packed = [f for f in all_files if base + f not in z.namelist()]
        if not_packed:
            sys.exit('FAIL: Missing packaged lesson assets: '+', '.join(not_packed[:8]))
        for filename in all_files:
            data = json.loads(gzip.decompress(z.read(base + filename)).decode('utf-8'))
            assert isinstance(data, list), filename
            assert all(isinstance(lesson, dict) and lesson.get('id') in idx for lesson in data), filename
elif source.is_dir():
    base = source / 'study'
    idx = json.loads((base / 'index.json').read_text(encoding='utf8'))
    all_files = set(idx.values())
    for filename in all_files:
        data = json.loads(gzip.decompress((base/filename).read_bytes()).decode('utf-8'))
        assert isinstance(data, list), filename
        assert all(isinstance(lesson,dict) and lesson.get('id') in idx for lesson in data), filename
else:
    sys.exit('FAIL: invalid file/folder ' + str(source))
assert len(idx)==1296 and len(all_files)==296, (len(idx), len(all_files))
assert all(file.endswith('.dat') for file in all_files)
print(f'PASS: {len(idx)} lesson IDs and {len(all_files)} compressed chapters validated in {source}')
