import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:another_telephony/telephony.dart';

final telephonyProvider = Provider<Telephony>((ref) {
  return Telephony.instance;
});

final smsListProvider = FutureProvider<List<SmsMessage>>((ref) async {
  final telephony = ref.watch(telephonyProvider);
  bool? permissionsGranted = await telephony.requestPhoneAndSmsPermissions;
  if (permissionsGranted ?? false) {
    return await telephony.getInboxSms(
      sortOrder: [OrderBy(SmsColumn.DATE, sort: Sort.DESC)]
    );
  }
  return [];
});

final conversationsProvider = FutureProvider<List<SmsConversation>>((ref) async {
  final smsList = await ref.watch(smsListProvider.future);
  final map = <String, SmsConversation>{};

  for (var message in smsList) {
    if (message.address == null) continue;
    if (!map.containsKey(message.address!)) {
      map[message.address!] = SmsConversation(
        address: message.address!,
        latestMessage: message,
        messages: [message],
      );
    } else {
      map[message.address!]!.messages.add(message);
    }
  }

  final list = map.values.toList();
  list.sort((a, b) => (b.latestMessage.date ?? 0).compareTo(a.latestMessage.date ?? 0));
  return list;
});

class SmsConversation {
  final String address;
  final SmsMessage latestMessage;
  final List<SmsMessage> messages;

  SmsConversation({
    required this.address,
    required this.latestMessage,
    required this.messages,
  });
}
