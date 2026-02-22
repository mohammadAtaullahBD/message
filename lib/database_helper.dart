import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('messaging_app.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future _createDB(Database db, int version) async {
    const idType = 'INTEGER PRIMARY KEY AUTOINCREMENT';
    const textType = 'TEXT NOT NULL';
    const textNullableType = 'TEXT';
    const boolType = 'BOOLEAN NOT NULL';

    // Tags Table
    await db.execute('''
CREATE TABLE tags (
  id $idType,
  name $textType,
  color $textNullableType
)
''');

    // SenderTags Table (Mapping senders to tags)
    await db.execute('''
CREATE TABLE sender_tags (
  id $idType,
  sender_address $textType,
  tag_id INTEGER NOT NULL,
  FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
)
''');

    // Telegram Bots Table
    await db.execute('''
CREATE TABLE telegram_bots (
  id $idType,
  bot_token $textType,
  chat_id $textType,
  name $textType,
  active $boolType
)
''');

    // Forwarding Rules Table
    await db.execute('''
CREATE TABLE forwarding_rules (
  id $idType,
  bot_id INTEGER NOT NULL,
  tag_id INTEGER,
  sender_address $textNullableType,
  FOREIGN KEY (bot_id) REFERENCES telegram_bots (id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
)
''');

    // Offline Forwarding Queue Table
    await db.execute('''
CREATE TABLE offline_queue (
  id $idType,
  bot_token $textType,
  chat_id $textType,
  message $textType,
  created_at $textType
)
''');

    // Spam / Blocked list
    await db.execute('''
CREATE TABLE blocked_senders (
  id $idType,
  sender_address $textType
)
''');
  }

  Future close() async {
    final db = await instance.database;
    db.close();
  }
}
