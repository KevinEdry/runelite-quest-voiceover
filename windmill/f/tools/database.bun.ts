import { Database } from "bun:sqlite";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import type { GitHubClient } from "./git";

// Must match the file the plugin downloads (DatabaseVersionManager.DATABASE_FILENAME on
// the main branch). The old quest_voiceover.db is the pre-split legacy DB the plugin no
// longer reads.
export const DATABASE_FILE = "quest_voiceover_v2.db";

export interface DialogRecord {
  quest: string;
  character: string;
  text: string;
  uri: string;
}

export interface DialogsDatabase {
  readonly database: Database;
  readonly dbPath: string;
  readonly getByCharacter: (character: string) => DialogRecord[];
  readonly exists: (character: string, text: string) => boolean;
  readonly insert: (record: DialogRecord) => boolean;
  readonly save: (branch: string, message: string) => Promise<number>;
  readonly cleanup: () => void;
}

// Windmill runs every job stateless, so the plugin database is round-tripped through
// GitHub: download the file from `branch` into a temp dir, mutate it locally, upload
// it back. Pass readonly to skip schema creation when only querying. Call cleanup()
// (or save(), which cleans up after uploading) to remove the temp dir.
export async function openDialogsDatabase(
  github: GitHubClient,
  branch: string,
  options: { readonly?: boolean } = {}
): Promise<DialogsDatabase> {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "quest-voiceover-"));
  const dbPath = path.join(tempDir, DATABASE_FILE);

  const existing = await github.getFile(DATABASE_FILE, branch);
  if (existing) {
    fs.writeFileSync(dbPath, existing.content);
    console.log(`Downloaded database: ${existing.content.length} bytes`);
  } else if (options.readonly) {
    throw new Error(`No database found on the ${branch} branch`);
  } else {
    console.log("No database found on GitHub, creating a new one");
  }

  const database = new Database(dbPath, { create: !options.readonly, readonly: options.readonly });

  if (!options.readonly) {
    database.run(
      `CREATE TABLE IF NOT EXISTS dialogs (quest TEXT NOT NULL, character TEXT NOT NULL, text TEXT NOT NULL, uri TEXT NOT NULL)`
    );
    database.run(`CREATE INDEX IF NOT EXISTS idx_dialogs_character ON dialogs(character)`);
    database.run(`CREATE INDEX IF NOT EXISTS idx_dialogs_character_text ON dialogs(character, text)`);
  }

  const existsStmt = database.query("SELECT 1 FROM dialogs WHERE character = ? AND text = ? LIMIT 1");
  const insertStmt = database.query("INSERT INTO dialogs (quest, character, text, uri) VALUES (?, ?, ?, ?)");

  const exists = (character: string, text: string): boolean => existsStmt.get(character, text) !== null;

  const getByCharacter = (character: string): DialogRecord[] =>
    database.query("SELECT quest, character, text, uri FROM dialogs WHERE character = ?").all(character) as DialogRecord[];

  // Inserts unless an identical (character, text) row already exists. Returns whether a row was added.
  const insert = (record: DialogRecord): boolean => {
    if (exists(record.character, record.text)) return false;
    insertStmt.run(record.quest, record.character, record.text, record.uri);
    return true;
  };

  const cleanup = (): void => {
    fs.rmSync(tempDir, { recursive: true, force: true });
  };

  const save = async (targetBranch: string, message: string): Promise<number> => {
    database.close();
    const content = fs.readFileSync(dbPath);
    await github.createOrUpdateFile(DATABASE_FILE, content, targetBranch, message);
    console.log(`Database uploaded to ${targetBranch}`);
    cleanup();
    return content.length;
  };

  return { database, dbPath, getByCharacter, exists, insert, save, cleanup };
}
