import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import '../../sim_service.dart';

class ComposePage extends StatefulWidget {
  final String? initialRecipient;
  const ComposePage({super.key, this.initialRecipient});

  @override
  State<ComposePage> createState() => _ComposePageState();
}

class _ComposePageState extends State<ComposePage> {
  final _recipientController = TextEditingController();
  final _messageController = TextEditingController();
  List<Map<String, dynamic>> _simCards = [];
  List<Contact> _contacts = [];
  int? _selectedSimId;
  bool _isLoading = true;
  bool _isSending = false;

  @override
  void initState() {
    super.initState();
    if (widget.initialRecipient != null) {
      _recipientController.text = widget.initialRecipient!;
    }
    _fetchSimCards();
    _fetchContacts();
  }

  Future<void> _fetchContacts() async {
    if (await FlutterContacts.requestPermission()) {
      final contacts = await FlutterContacts.getContacts(withProperties: true);
      if (mounted) {
        setState(() {
          _contacts = contacts;
        });
      }
    }
  }

  Future<void> _fetchSimCards() async {
    final sims = await SimService.getSimCards();
    setState(() {
      _simCards = sims;
      _isLoading = false;
      if (_simCards.isNotEmpty) {
        // Find default or fallback
        final defaultSim = _simCards.firstWhere((s) => s['isDefault'] == true, orElse: () => _simCards.first);
        _selectedSimId = defaultSim['id'];
        
        // If there's a default SMS sim identified by Android, we actually don't NEED to show the option,
        // but for safety in dual-sim setups, showing the default prominently is preferred.
      }
    });
  }

  Future<void> _sendMessage() async {
    if (_recipientController.text.isEmpty || _messageController.text.isEmpty) return;
    
    setState(() {
      _isSending = true;
    });

    final success = await SimService.sendSms(
      _recipientController.text, 
      _messageController.text,
      simId: _selectedSimId,
    );

    setState(() {
      _isSending = false;
    });

    if (success && mounted) {
      Navigator.pop(context); // Go back to inbox after sending
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to send message')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('New Message'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Autocomplete<Contact>(
              displayStringForOption: (Contact option) => option.phones.isNotEmpty ? option.phones.first.number : '',
              optionsBuilder: (TextEditingValue textEditingValue) {
                if (textEditingValue.text.isEmpty) {
                  return const Iterable<Contact>.empty();
                }
                final query = textEditingValue.text.toLowerCase();
                return _contacts.where((Contact contact) {
                  final nameMatches = contact.displayName.toLowerCase().contains(query);
                  final phoneMatches = contact.phones.any((phone) => phone.number.replaceAll(RegExp(r'\D'), '').contains(query.replaceAll(RegExp(r'\D'), '')));
                  return nameMatches || phoneMatches;
                });
              },
              onSelected: (Contact selection) {
                if (selection.phones.isNotEmpty) {
                  _recipientController.text = selection.phones.first.number;
                }
              },
              fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                // Keep the _recipientController in sync if they type without selecting
                controller.addListener(() {
                  if (_recipientController.text != controller.text) {
                     _recipientController.text = controller.text;
                  }
                });
                // Initialize the controller with our initial value if passed
                if (_recipientController.text.isNotEmpty && controller.text.isEmpty) {
                  controller.text = _recipientController.text;
                }
                
                return TextField(
                  controller: controller,
                  focusNode: focusNode,
                  decoration: const InputDecoration(
                    labelText: 'To',
                    hintText: 'Phone number or name',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.person),
                  ),
                  keyboardType: TextInputType.phone,
                );
              },
              optionsViewBuilder: (context, onSelected, options) {
                return Align(
                  alignment: Alignment.topLeft,
                  child: Material(
                    elevation: 4,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxHeight: 200, maxWidth: 300),
                      child: ListView.builder(
                        padding: EdgeInsets.zero,
                        itemCount: options.length,
                        itemBuilder: (BuildContext context, int index) {
                          final Contact option = options.elementAt(index);
                          final phone = option.phones.isNotEmpty ? option.phones.first.number : 'No number';
                          return ListTile(
                            leading: const CircleAvatar(child: Icon(Icons.person)),
                            title: Text(option.displayName),
                            subtitle: Text(phone),
                            onTap: () {
                              onSelected(option);
                            },
                          );
                        },
                      ),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),
            if (!_isLoading && _simCards.length > 1)
              DropdownButtonFormField<int>(
                initialValue: _selectedSimId,
                decoration: const InputDecoration(
                  labelText: 'Send via SIM',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.sim_card),
                ),
                items: _simCards.map((sim) {
                  final isDefault = sim['isDefault'] == true ? ' (Default)' : '';
                  return DropdownMenuItem<int>(
                    value: sim['id'],
                    child: Text('${sim['name']}$isDefault'),
                  );
                }).toList(),
                onChanged: (val) {
                  setState(() {
                    _selectedSimId = val;
                  });
                },
              ),
            const Spacer(),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _messageController,
                    decoration: InputDecoration(
                      hintText: 'Text message...',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(24),
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    ),
                    maxLines: null,
                    textCapitalization: TextCapitalization.sentences,
                  ),
                ),
                const SizedBox(width: 8),
                _isSending 
                  ? const CircularProgressIndicator()
                  : IconButton.filled(
                      onPressed: _sendMessage,
                      icon: const Icon(Icons.send),
                    )
              ],
            )
          ],
        ),
      ),
    );
  }
}
