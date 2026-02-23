import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../providers/sms_provider.dart';
import '../../providers/database_providers.dart';
import 'tag_dialog.dart';
import '../compose/compose_page.dart';
import '../settings/settings_page.dart';

class InboxPage extends ConsumerStatefulWidget {
  const InboxPage({super.key});

  @override
  ConsumerState<InboxPage> createState() => _InboxPageState();
}

class _InboxPageState extends ConsumerState<InboxPage> {
  final Set<String> _selectedAddresses = {};
  bool _isSelectionMode = false;
  String _searchQuery = '';
  int? _selectedTagId;

  void _toggleSelection(String address) {
    setState(() {
      if (_selectedAddresses.contains(address)) {
        _selectedAddresses.remove(address);
        if (_selectedAddresses.isEmpty) _isSelectionMode = false;
      } else {
        _selectedAddresses.add(address);
      }
    });
  }

  void _selectAll(List<SmsConversation> convos) {
    setState(() {
      _selectedAddresses.addAll(convos.map((c) => c.address));
    });
  }

  @override
  Widget build(BuildContext context) {
    final conversationsAsync = ref.watch(conversationsProvider);

    return Scaffold(
      appBar: _isSelectionMode 
        ? AppBar(
            leading: IconButton(
              icon: const Icon(Icons.close),
              onPressed: () {
                setState(() {
                  _isSelectionMode = false;
                  _selectedAddresses.clear();
                });
              },
            ),
            title: Text('\${_selectedAddresses.length} Selected'),
            actions: [
              IconButton(
                icon: const Icon(Icons.select_all),
                tooltip: 'Select All',
                onPressed: () {
                  if (conversationsAsync.value != null) {
                    _selectAll(conversationsAsync.value!);
                  }
                },
              ),
              IconButton(
                icon: const Icon(Icons.label),
                tooltip: 'Assign Tag',
                onPressed: () async {
                  await showTagDialog(context, ref, _selectedAddresses);
                  setState(() {
                    _isSelectionMode = false;
                    _selectedAddresses.clear();
                  });
                },
              ),
              IconButton(
                icon: const Icon(Icons.delete),
                tooltip: 'Delete',
                onPressed: () {
                  // TODO: Implement Delete functionality safely
                },
              ),
            ],
          )
        : AppBar(
            title: const Text('Inbox'),
            actions: [
              IconButton(
                icon: const Icon(Icons.settings),
                onPressed: () {
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsPage()));
                },
              )
            ],
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(110),
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: TextField(
                      decoration: InputDecoration(
                        hintText: 'Search by number or content...',
                        prefixIcon: const Icon(Icons.search),
                        filled: true,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(30),
                          borderSide: BorderSide.none,
                        ),
                      ),
                      onChanged: (val) {
                        setState(() {
                          _searchQuery = val.toLowerCase();
                        });
                      },
                    ),
                  ),
                  ref.watch(tagsProvider).when(
                    data: (tags) {
                      if (tags.isEmpty) return const SizedBox.shrink();
                      return SizedBox(
                        height: 40,
                        child: ListView.builder(
                          scrollDirection: Axis.horizontal,
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          itemCount: tags.length,
                          itemBuilder: (ctx, idx) {
                            final t = tags[idx];
                            final isSelected = _selectedTagId == t['id'];
                            return Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: FilterChip(
                                label: Text(t['name']),
                                selected: isSelected,
                                onSelected: (val) {
                                  setState(() {
                                    _selectedTagId = val ? t['id'] : null;
                                  });
                                },
                              ),
                            );
                          },
                        ),
                      );
                    },
                    loading: () => const SizedBox.shrink(),
                    error: (e, st) => const SizedBox.shrink(),
                  ),
                  const SizedBox(height: 8),
                ],
              ),
            ),
          ),
      body: conversationsAsync.when(
        data: (conversations) {
          final senderTagsAsync = ref.watch(senderTagsProvider);
          final senderTags = senderTagsAsync.value ?? [];
          
          final filtered = conversations.where((c) {
            final addr = c.address.toLowerCase();
            final body = c.latestMessage.body?.toLowerCase() ?? '';
            final matchesSearch = addr.contains(_searchQuery) || body.contains(_searchQuery);
            
            bool matchesTag = true;
            if (_selectedTagId != null) {
               matchesTag = senderTags.any((st) => st['sender_address'] == c.address && st['tag_id'] == _selectedTagId);
            }
            
            return matchesSearch && matchesTag;
          }).toList();

          if (filtered.isEmpty) {
            return const Center(child: Text("No messages found."));
          }

          return ListView.builder(
            itemCount: filtered.length,
            itemBuilder: (context, index) {
              final convo = filtered[index];
              final isSelected = _selectedAddresses.contains(convo.address);
              final date = convo.latestMessage.date != null 
                  ? DateTime.fromMillisecondsSinceEpoch(convo.latestMessage.date!)
                  : DateTime.now();
              final timeStr = DateFormat('MMM d, h:mm a').format(date);

              return ListTile(
                selected: isSelected,
                selectedTileColor: Theme.of(context).colorScheme.primaryContainer,
                leading: CircleAvatar(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  child: Text(
                    convo.address.isNotEmpty ? convo.address[0].toUpperCase() : '?',
                    style: TextStyle(color: Theme.of(context).colorScheme.onPrimary),
                  ),
                ),
                title: Text(
                  convo.address,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  convo.latestMessage.body ?? '',
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: Text(
                  timeStr,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                onLongPress: () {
                  setState(() {
                    _isSelectionMode = true;
                    _toggleSelection(convo.address);
                  });
                },
                onTap: () {
                  if (_isSelectionMode) {
                    _toggleSelection(convo.address);
                  } else {
                    // TODO: Open conversation detailed view
                  }
                },
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error loading SMS: \$err')),
      ),
      floatingActionButton: _isSelectionMode ? null : FloatingActionButton(
        onPressed: () {
          Navigator.push(context, MaterialPageRoute(builder: (_) => const ComposePage()));
        },
        child: const Icon(Icons.message),
      ),
    );
  }
}
