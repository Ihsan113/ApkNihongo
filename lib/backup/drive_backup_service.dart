import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;

import 'backup_snapshot.dart';

/// Uses ONLY appDataFolder, not access to normal user Drive documents.
/// An OAuth client, SHA fingerprint and enabled Drive API are required to sign in.
class DriveBackupService extends ChangeNotifier {
  static const _scope = <String>['https://www.googleapis.com/auth/drive.appdata'];
  static const _filename = 'betterseed_backup_v1.json';
  static const _api = 'https://www.googleapis.com/drive/v3/files';
  static const _upload = 'https://www.googleapis.com/upload/drive/v3/files';

  static const String _serverClientId =
      String.fromEnvironment('BETTERSEED_GOOGLE_SERVER_CLIENT_ID');
  final GoogleSignIn _signIn = GoogleSignIn.instance;
  GoogleSignInAccount? _account;
  StreamSubscription<GoogleSignInAuthenticationEvent>? _authEvents;
  bool _initialized = false;
  bool _busy = false;
  String? _error;

  bool get isConnected => _account != null;
  bool get isBusy => _busy;
  String? get email => _account?.email;
  String? get error => _error;

  Future<void> initialize() async {
    if (_initialized) return;
    // Initialization is called only once by the owning screen.
    await _signIn.initialize(
      serverClientId: _serverClientId.isEmpty ? null : _serverClientId,
    );
    _initialized = true;
    _authEvents = _signIn.authenticationEvents.listen((event) {
      switch (event) {
        case GoogleSignInAuthenticationEventSignIn():
          _account = event.user;
          break;
        case GoogleSignInAuthenticationEventSignOut():
          _account = null;
          break;
      }
      notifyListeners();
    }, onError: (Object _) {
      _account = null;
      notifyListeners();
    });
    // Existing consent can restore the account without displaying a prompt.
    _signIn.attemptLightweightAuthentication();
  }

  Future<void> connect() async {
    await initialize();
    if (_busy) return;
    _busy = true;
    _error = null;
    notifyListeners();
    try {
      if (!_signIn.supportsAuthenticate()) {
        throw StateError('Interactive Google sign-in requires Android in this build');
      }
      final account = _account ?? await _signIn.authenticate();
      // Explicit user interaction: requesting Drive permission is permitted here.
      if (await account.authorizationClient.authorizationForScopes(_scope) == null) {
        await account.authorizationClient.authorizeScopes(_scope);
      }
      _account = account;
    } catch (e) {
      _error = 'Google Sign-In failed. Check your OAuth / SHA configuration and internet.';
      rethrow;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<void> disconnect() async {
    if (_busy) return;
    await initialize();
    await _signIn.signOut();
    _account = null;
    notifyListeners();
  }

  Future<Map<String, String>> _headers() async {
    final account = _account;
    if (account == null) throw StateError('Connect a Google account first');
    final headers = await account.authorizationClient.authorizationHeaders(_scope);
    if (headers == null) {
      throw StateError('Drive permission expired. Connect again from Settings.');
    }
    return headers;
  }

  Future<String?> _existingFileId(Map<String, String> headers) async {
    final uri = Uri.parse(_api).replace(queryParameters: <String, String>{
      'spaces': 'appDataFolder',
      'q': "name = '$_filename' and trashed = false",
      'fields': 'nextPageToken,files(id,name,modifiedTime)',
      'pageSize': '20',
      'orderBy': 'modifiedTime desc',
    });
    final reply = await http.get(uri, headers: headers).timeout(const Duration(seconds: 25));
    _check(reply, 'Find backup');
    final json = jsonDecode(reply.body) as Map<String, dynamic>;
    final files = json['files'] as List? ?? const [];
    return files.isEmpty ? null : (files.first as Map)['id'] as String?;
  }

  /// Explicit replacement of a single backup file. Never removes local data.
  Future<void> upload(BackupSnapshot snapshot) async {
    final headers = await _headers();
    final id = await _existingFileId(headers);
    final content = snapshot.encode();
    if (id != null) {
      final uri = Uri.parse('$_upload/${Uri.encodeComponent(id)}?uploadType=media');
      final reply = await http.patch(uri, headers: <String, String>{
        ...headers, 'Content-Type': 'application/json; charset=utf-8',
      }, body: content).timeout(const Duration(seconds: 40));
      _check(reply, 'Update backup');
    } else {
      // Google's multipart/related upload: metadata + JSON data in one request.
      const boundary = 'BetterSeedDriveBackupBoundary';
      final metadata = jsonEncode(<String, dynamic>{
        'name': _filename,
        'parents': <String>['appDataFolder'],
        'mimeType': 'application/json',
      });
      final body = '--$boundary\r\n'
          'Content-Type: application/json; charset=UTF-8\r\n\r\n'
          '$metadata\r\n'
          '--$boundary\r\n'
          'Content-Type: application/json; charset=UTF-8\r\n\r\n'
          '$content\r\n'
          '--$boundary--\r\n';
      final reply = await http.post(
        Uri.parse('$_upload?uploadType=multipart&fields=id'),
        headers: <String, String>{
          ...headers,
          'Content-Type': 'multipart/related; boundary=$boundary',
        },
        body: utf8.encode(body),
      ).timeout(const Duration(seconds: 40));
      _check(reply, 'Create backup');
    }
  }

  Future<BackupSnapshot?> download() async {
    final headers = await _headers();
    final id = await _existingFileId(headers);
    if (id == null) return null;
    final uri = Uri.parse('$_api/${Uri.encodeComponent(id)}?alt=media');
    final reply = await http.get(uri, headers: headers).timeout(const Duration(seconds: 35));
    _check(reply, 'Download backup');
    return BackupSnapshot.decode(utf8.decode(reply.bodyBytes));
  }

  static void _check(http.Response reply, String action) {
    if (reply.statusCode >= 200 && reply.statusCode < 300) return;
    if (reply.statusCode == 401 || reply.statusCode == 403) {
      throw StateError('$action denied. Check Google Drive access and account permissions.');
    }
    throw StateError('$action failed (HTTP ${reply.statusCode}). Check your connection.');
  }

  @override
  void dispose() {
    _authEvents?.cancel();
    super.dispose();
  }
}
