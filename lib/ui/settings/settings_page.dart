import 'package:flutter/material.dart';
import '../../database_helper.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        children: [
          ListTile(
            leading: const Icon(Icons.telegram),
            title: const Text('Telegram Integration'),
            subtitle: const Text('Manage bots and forwarding rules'),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const TelegramIntegrationPage()));
            },
          ),
          ListTile(
            leading: const Icon(Icons.block),
            title: const Text('Blocked/Spam'),
            subtitle: const Text('Manage blocked senders'),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const BlockedSendersPage()));
            },
          ),
          SwitchListTile(
            secondary: const Icon(Icons.dark_mode),
            title: const Text('Dark Mode'),
            value: Theme.of(context).brightness == Brightness.dark,
            onChanged: (val) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Theme follows system settings natively. Override not yet implemented.')),
              );
            },
          )
        ],
      ),
    );
  }
}

class TelegramIntegrationPage extends StatefulWidget {
  const TelegramIntegrationPage({super.key});

  @override
  State<TelegramIntegrationPage> createState() => _TelegramIntegrationPageState();
}

class _TelegramIntegrationPageState extends State<TelegramIntegrationPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<Map<String, dynamic>> _bots = [];
  List<Map<String, dynamic>> _rules = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    final db = await DatabaseHelper.instance.database;
    final bots = await db.query('telegram_bots');
    final rules = await db.rawQuery('''
      SELECT r.id, b.name as bot_name, t.name as tag_name, r.sender_address 
      FROM forwarding_rules r
      LEFT JOIN telegram_bots b ON r.bot_id = b.id
      LEFT JOIN tags t ON r.tag_id = t.id
    ''');
    setState(() {
      _bots = bots;
      _rules = rules;
    });
  }

  void _showAddEditBotDialog({Map<String, dynamic>? existingBot}) {
    final nameCtrl = TextEditingController(text: existingBot?['name']?.toString() ?? '');
    final tokenCtrl = TextEditingController(text: existingBot?['bot_token']?.toString() ?? '');
    final chatCtrl = TextEditingController(text: existingBot?['chat_id']?.toString() ?? '');
    final isEditing = existingBot != null;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(isEditing ? 'Edit Telegram Bot' : 'Add Telegram Bot'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: nameCtrl, decoration: const InputDecoration(labelText: 'Bot Name')),
            TextField(controller: tokenCtrl, decoration: const InputDecoration(labelText: 'Bot Token')),
            TextField(controller: chatCtrl, decoration: const InputDecoration(labelText: 'Chat ID')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              if (tokenCtrl.text.isNotEmpty && chatCtrl.text.isNotEmpty) {
                final db = await DatabaseHelper.instance.database;
                final values = {
                  'name': nameCtrl.text.isEmpty ? 'My Bot' : nameCtrl.text,
                  'bot_token': tokenCtrl.text,
                  'chat_id': chatCtrl.text,
                  'active': isEditing ? existingBot['active'] : 1, // Preserve active state if editing, else default to 1
                };
                if (isEditing) {
                  await db.update('telegram_bots', values, where: 'id = ?', whereArgs: [existingBot['id']]);
                } else {
                  await db.insert('telegram_bots', values);
                }
                if (ctx.mounted) Navigator.pop(ctx);
                _loadData();
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _showAddRuleDialog() async {
    final db = await DatabaseHelper.instance.database;
    final bots = await db.query('telegram_bots');
    final tags = await db.query('tags');

    if (bots.isEmpty) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please add a bot first')));
      return;
    }

    int selectedBotId = bots.first['id'] as int;
    int? selectedTagId;
    final senderCtrl = TextEditingController();

    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setStateSB) {
            return AlertDialog(
              title: const Text('Add Rule'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<int>(
                      initialValue: selectedBotId,
                      decoration: const InputDecoration(labelText: 'Select Bot'),
                      items: bots.map((b) => DropdownMenuItem<int>(
                        value: b['id'] as int, child: Text(b['name'] as String)
                      )).toList(),
                      onChanged: (v) => setStateSB(() => selectedBotId = v!),
                    ),
                    const SizedBox(height: 16),
                    TextField(controller: senderCtrl, decoration: const InputDecoration(labelText: 'Specific Sender Number (Optional)')),
                    const SizedBox(height: 16),
                    const Text('OR Target Tag (Optional)'),
                    DropdownButtonFormField<int?>(
                      initialValue: selectedTagId,
                      decoration: const InputDecoration(labelText: 'Select Tag'),
                      items: [
                         const DropdownMenuItem<int?>(value: null, child: Text('None')),
                        ...tags.map((t) => DropdownMenuItem<int?>(
                          value: t['id'] as int, child: Text(t['name'] as String)
                        ))
                      ],
                      onChanged: (v) => setStateSB(() => selectedTagId = v),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
                ElevatedButton(
                  onPressed: () async {
                    if (senderCtrl.text.isNotEmpty || selectedTagId != null) {
                      await db.insert('forwarding_rules', {
                        'bot_id': selectedBotId,
                        'tag_id': selectedTagId,
                        'sender_address': senderCtrl.text.isEmpty ? null : senderCtrl.text,
                      });
                      if (ctx.mounted) Navigator.pop(ctx);
                      _loadData();
                    }
                  },
                  child: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Telegram Integration'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Bots'),
            Tab(text: 'Rules'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // BOTS TAB
          ListView.builder(
            itemCount: _bots.length,
            itemBuilder: (context, index) {
              final bot = _bots[index];
              return ListTile(
                title: Text(bot['name']),
                subtitle: Text('Chat: ${bot['chat_id']}'),
                onTap: () => _showAddEditBotDialog(existingBot: bot),
                onLongPress: () {
                  showDialog(
                    context: context,
                    builder: (ctx) => AlertDialog(
                      title: const Text('Delete Bot?'),
                      content: Text('Are you sure you want to delete ${bot['name']}?'),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
                        TextButton(
                          onPressed: () async {
                            final db = await DatabaseHelper.instance.database;
                            await db.delete('telegram_bots', where: 'id = ?', whereArgs: [bot['id']]);
                            if (ctx.mounted) Navigator.pop(ctx);
                            _loadData();
                          },
                          child: const Text('Delete', style: TextStyle(color: Colors.red)),
                        ),
                      ],
                    ),
                  );
                },
                trailing: Switch(
                  value: bot['active'] == 1,
                  onChanged: (val) async {
                    final db = await DatabaseHelper.instance.database;
                    await db.update('telegram_bots', {'active': val ? 1 : 0}, where: 'id = ?', whereArgs: [bot['id']]);
                    _loadData();
                  },
                ),
              );
            },
          ),
          
          // RULES TAB
          ListView.builder(
            itemCount: _rules.length,
            itemBuilder: (context, index) {
              final r = _rules[index];
              final target = r['tag_name'] != null ? 'Tag: ${r['tag_name']}' : 'Sender: ${r['sender_address']}';
              return ListTile(
                title: Text('${r['bot_name']}'),
                subtitle: Text('Forwards $target'),
                trailing: IconButton(
                  icon: const Icon(Icons.delete),
                  onPressed: () async {
                    final db = await DatabaseHelper.instance.database;
                    await db.delete('forwarding_rules', where: 'id = ?', whereArgs: [r['id']]);
                    _loadData();
                  },
                ),
              );
            },
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          if (_tabController.index == 0) {
            _showAddEditBotDialog();
          } else {
            _showAddRuleDialog();
          }
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}

class BlockedSendersPage extends StatefulWidget {
  const BlockedSendersPage({super.key});
  @override
  State<BlockedSendersPage> createState() => _BlockedSendersPageState();
}

class _BlockedSendersPageState extends State<BlockedSendersPage> {
  List<Map<String, dynamic>> _blocked = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final db = await DatabaseHelper.instance.database;
    final res = await db.query('blocked_senders');
    setState(() {
      _blocked = res;
    });
  }

  void _showAddDialog() {
    final numCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Block Sender'),
        content: TextField(
          controller: numCtrl,
          decoration: const InputDecoration(labelText: 'Phone Number / Sender ID'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              if (numCtrl.text.isNotEmpty) {
                final db = await DatabaseHelper.instance.database;
                await db.insert('blocked_senders', {'sender_address': numCtrl.text});
                if (ctx.mounted) Navigator.pop(ctx);
                _load();
              }
            },
            child: const Text('Block'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Blocked Senders')),
      body: ListView.builder(
        itemCount: _blocked.length,
        itemBuilder: (context, index) {
          final b = _blocked[index];
          return ListTile(
             title: Text(b['sender_address']),
            trailing: IconButton(
              icon: const Icon(Icons.unarchive),
              onPressed: () async {
                final db = await DatabaseHelper.instance.database;
                await db.delete('blocked_senders', where: 'id = ?', whereArgs: [b['id']]);
                _load();
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddDialog,
        child: const Icon(Icons.add),
      ),
    );
  }
}
