import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'sms_service.dart';
import 'ui/inbox/inbox_page.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize the native SMS headless callback
  SmsService().initialize();
  
  runApp(
    const ProviderScope(
      child: MessagingApp(),
    ),
  );
}

class MessagingApp extends ConsumerWidget {
  const MessagingApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // We will read a theme setting provider later, defaulting to system for now
    return MaterialApp(
      title: 'Default SMS App',
      theme: ThemeData.light(useMaterial3: true),
      darkTheme: ThemeData.dark(useMaterial3: true),
      themeMode: ThemeMode.system,
      home: const InboxPage(),
    );
  }
}
