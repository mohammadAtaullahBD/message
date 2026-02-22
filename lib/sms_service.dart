import 'package:telephony/telephony.dart';
import 'database_helper.dart';
import 'telegram_service.dart';

Future<void> onBackgroundMessage(SmsMessage message) async {
  // This headless callback runs when the app is in background.
  final senderInfo = message.address ?? "Unknown";
  final body = message.body ?? "";
  
  // Open DB and check tags/forwarding rules natively
  final db = await DatabaseHelper.instance.database;

  // 1. Check if the sender is blocked
  final blocked = await db.query(
    'blocked_senders',
    where: 'sender_address = ?',
    whereArgs: [senderInfo],
  );
  if (blocked.isNotEmpty) return; // Do not process blocked sender
  
  // 2. See if the sender matches any specific bot forwarding rules (by sender directly)
  final directRules = await db.rawQuery('''
    SELECT b.bot_token, b.chat_id 
    FROM forwarding_rules r 
    JOIN telegram_bots b ON r.bot_id = b.id 
    WHERE r.sender_address = ? AND b.active = 1
  ''', [senderInfo]);

  for (var rule in directRules) {
    await TelegramService.instance.sendMessage(
      rule['bot_token'] as String,
      rule['chat_id'] as String,
      "SMS from $senderInfo: $body"
    );
  }

  // 3. Check for tag-based forwarding rules
  final tagRules = await db.rawQuery('''
    SELECT b.bot_token, b.chat_id 
    FROM forwarding_rules r 
    JOIN sender_tags st ON r.tag_id = st.tag_id
    JOIN telegram_bots b ON r.bot_id = b.id 
    WHERE st.sender_address = ? AND b.active = 1
  ''', [senderInfo]);

  for (var rule in tagRules) {
    await TelegramService.instance.sendMessage(
      rule['bot_token'] as String,
      rule['chat_id'] as String,
      "SMS from $senderInfo (Tagged): $body"
    );
  }
}

class SmsService {
  final Telephony telephony = Telephony.instance;

  void initialize() {
    telephony.listenIncomingSms(
      onNewMessage: (SmsMessage message) {
        // Handle foreground message (update UI or show notification)
        _processForegroundMessage(message);
      },
      onBackgroundMessage: onBackgroundMessage,
    );
  }

  void _processForegroundMessage(SmsMessage message) {
    // We can use Riverpod to dispatch an event here.
    // Also we should execute the same Telegram logic if the app is open.
    onBackgroundMessage(message);
  }
}
