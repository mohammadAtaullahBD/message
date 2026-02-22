import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/database_providers.dart';
import '../../database_helper.dart';

Future<void> showTagDialog(BuildContext context, WidgetRef ref, Set<String> selectedSenders) async {
  final tagsProviderData = ref.read(tagsProvider.future);
  
  await showDialog(
    context: context,
    builder: (ctx) {
      return FutureBuilder<List<Map<String, dynamic>>>(
        future: tagsProviderData,
        builder: (context, snapshot) {
          if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
          
          final tags = snapshot.data!;
          return AlertDialog(
            title: const Text('Assign Tag'),
            content: SizedBox(
              width: double.maxFinite,
              child: ListView(
                shrinkWrap: true,
                children: [
                  ...tags.map((tag) => ListTile(
                    title: Text(tag['name']),
                    onTap: () async {
                      await _assignTags(tag['id'], selectedSenders);
                      ref.invalidate(tagsProvider);
                      if (ctx.mounted) Navigator.pop(ctx);
                    },
                  )),
                  ListTile(
                    leading: const Icon(Icons.add),
                    title: const Text('Create New Tag'),
                    onTap: () async {
                      Navigator.pop(ctx);
                      await _showCreateTagDialog(context, ref, selectedSenders);
                    },
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Cancel'),
              ),
            ],
          );
        },
      );
    }
  );
}

Future<void> _showCreateTagDialog(BuildContext context, WidgetRef ref, Set<String> selectedSenders) async {
  final controller = TextEditingController();
  await showDialog(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('New Tag'),
      content: TextField(
        controller: controller,
        decoration: const InputDecoration(hintText: 'Tag Name'),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx),
          child: const Text('Cancel'),
        ),
        ElevatedButton(
          onPressed: () async {
            if (controller.text.isNotEmpty) {
              final db = await DatabaseHelper.instance.database;
              final tagId = await db.insert('tags', {'name': controller.text, 'color': '#000000'});
              await _assignTags(tagId, selectedSenders);
              ref.invalidate(tagsProvider);
              if (ctx.mounted) Navigator.pop(ctx);
            }
          },
          child: const Text('Create & Assign'),
        ),
      ],
    ),
  );
}

Future<void> _assignTags(int tagId, Set<String> senders) async {
  final db = await DatabaseHelper.instance.database;
  for (var sender in senders) {
    // Avoid duplicates
    await db.delete('sender_tags', where: 'sender_address = ? AND tag_id = ?', whereArgs: [sender, tagId]);
    await db.insert('sender_tags', {
      'sender_address': sender,
      'tag_id': tagId,
    });
  }
}
