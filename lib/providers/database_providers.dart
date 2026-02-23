import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../database_helper.dart';

final tagsProvider = FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final db = await DatabaseHelper.instance.database;
  return await db.query('tags');
});

final senderTagsProvider = FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final db = await DatabaseHelper.instance.database;
  return await db.query('sender_tags');
});

final telegramBotsProvider = FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final db = await DatabaseHelper.instance.database;
  return await db.query('telegram_bots');
});
