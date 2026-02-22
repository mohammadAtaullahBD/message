import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'database_helper.dart';

class TelegramService {
  static final TelegramService instance = TelegramService._init();
  TelegramService._init();

  Future<void> logOfflineQueue(String token, String chatId, String message) async {
    final db = await DatabaseHelper.instance.database;
    await db.insert('offline_queue', {
      'bot_token': token,
      'chat_id': chatId,
      'message': message,
      'created_at': DateTime.now().toIso8601String()
    });
  }

  Future<bool> sendMessage(String token, String chatId, String text) async {
    final url = Uri.parse('https://api.telegram.org/bot$token/sendMessage');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'chat_id': chatId,
          'text': text,
        }),
      );
      if (response.statusCode == 200) {
        return true;
      } else {
        await logOfflineQueue(token, chatId, text);
        return false;
      }
    } on SocketException {
      await logOfflineQueue(token, chatId, text);
      return false;
    } catch (e) {
      await logOfflineQueue(token, chatId, text);
      return false;
    }
  }

  Future<void> processOfflineQueue() async {
    final db = await DatabaseHelper.instance.database;
    final queue = await db.query('offline_queue');
    if (queue.isEmpty) return;

    for (var item in queue) {
      final id = item['id'] as int;
      final token = item['bot_token'] as String;
      final chatId = item['chat_id'] as String;
      final message = item['message'] as String;

      final url = Uri.parse('https://api.telegram.org/bot$token/sendMessage');
      try {
        final response = await http.post(
          url,
          headers: {'Content-Type': 'application/json'},
          body: json.encode({
            'chat_id': chatId,
            'text': message,
          }),
        );
        if (response.statusCode == 200) {
          await db.delete('offline_queue', where: 'id = ?', whereArgs: [id]);
        }
      } catch (e) {
        // Still offline or error, keep it in the queue for next retry
        return;
      }
    }
  }
}
