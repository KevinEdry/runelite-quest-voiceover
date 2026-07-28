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
  readonly getClipUris: (character: string, max: number) => string[];
  readonly getQuests: () => string[];
  readonly exists: (character: string, text: string) => boolean;
  readonly insert: (record: DialogRecord) => boolean;
  readonly save: (branch: string, message: string) => Promise<number>;
  readonly cleanup: () => void;
}

// Windmill jobs are stateless, so the plugin database is round-tripped through GitHub
// rather than held open across steps.
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

  const getQuests = (): string[] =>
    (database.query("SELECT DISTINCT quest FROM dialogs").all() as { quest: string }[]).map((row) => row.quest);

  // Deduped, longest-first — longer clips reach an IVC sample-duration target with fewer files.
  const getClipUris = (character: string, max: number): string[] => {
    const seen = new Set<string>();
    const uris: string[] = [];
    for (const row of getByCharacter(character).sort((a, b) => b.text.length - a.text.length)) {
      if (seen.has(row.uri)) continue;
      seen.add(row.uri);
      uris.push(row.uri);
      if (uris.length >= max) break;
    }
    return uris;
  };

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

  return { database, dbPath, getByCharacter, getClipUris, getQuests, exists, insert, save, cleanup };
}
