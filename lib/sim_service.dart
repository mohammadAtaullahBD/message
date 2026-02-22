import 'package:flutter/services.dart';

class SimService {
  static const MethodChannel _channel = MethodChannel('dev.ataullah.messaging_app/role');

  static Future<List<Map<String, dynamic>>> getSimCards() async {
    try {
      final List<dynamic> result = await _channel.invokeMethod('getSimCards');
      return result.map((e) => Map<String, dynamic>.from(e)).toList();
    } catch (e) {
      return [];
    }
  }

  static Future<bool> sendSms(String address, String message, {int? simId}) async {
    try {
      final bool result = await _channel.invokeMethod('sendSmsViaSim', {
        'address': address,
        'message': message,
        'simId': simId,
      });
      return result;
    } catch (e) {
      return false;
    }
  }
}
