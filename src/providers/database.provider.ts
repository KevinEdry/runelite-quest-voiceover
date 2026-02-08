import { Database } from "bun:sqlite";
import * as fs from "fs";
import * as path from "path";
import * as os from "os";
import { fileURLToPath } from "url";
import { getGitHubClient } from "../clients/github.client.js";

const DATABASE_FILE = "quest_voiceover.db";
const DATABASE_BRANCH = "database";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const LOCAL_DB_DIR = path.resolve(__dirname, "../../output_db");
const LOCAL_DB_PATH = path.join(LOCAL_DB_DIR, DATABASE_FILE);

export interface DialogRecord {
  readonly quest: string;
  readonly character: string;
  readonly text: string;
  readonly uri: string;
}

export interface DatabaseProvider {
  readonly checkDialogExists: (character: string, text: string) => Promise<boolean>;
  readonly getCompletedQuests: () => Promise<readonly string[]>;
  readonly getDialogsByCharacter: (character: string) => Promise<readonly DialogRecord[]>;
  readonly insertDialog: (record: DialogRecord) => Promise<void>;
  readonly uploadDatabase: () => Promise<void>;
  readonly closeDatabase: () => Promise<void>;
  readonly resetConnection: () => void;
}

interface DatabaseState {
  database: Database | null;
  localDbPath: string | null;
}

export function createDatabaseProvider(): DatabaseProvider {
  const state: DatabaseState = {
    database: null,
    localDbPath: null,
  };

  const ensureDatabase = async (): Promise<Database> => {
    if (state.database) {
      return state.database;
    }

    const isDryRun = process.env.DRY_RUN === "true";

    if (isDryRun) {
      if (!fs.existsSync(LOCAL_DB_DIR)) {
        fs.mkdirSync(LOCAL_DB_DIR, { recursive: true });
      }
      state.localDbPath = LOCAL_DB_PATH;

      if (fs.existsSync(LOCAL_DB_PATH)) {
        console.log(`Using local database at ${LOCAL_DB_PATH}`);
      } else {
        console.log(`No local database found, fetching from GitHub...`);
        const github = getGitHubClient();
        const existingDb = await github.getFile(DATABASE_FILE, DATABASE_BRANCH);
        if (existingDb) {
          console.log(`Downloaded database: ${existingDb.content.length} bytes`);
          fs.writeFileSync(state.localDbPath, existingDb.content);
        }
      }
    } else if (fs.existsSync(LOCAL_DB_PATH)) {
      console.log(`Using local database at ${LOCAL_DB_PATH}`);
      state.localDbPath = LOCAL_DB_PATH;
    } else {
      const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "quest-voiceover-"));
      state.localDbPath = path.join(tempDir, DATABASE_FILE);

      console.log(`Fetching database from GitHub (${DATABASE_BRANCH} branch)...`);
      const github = getGitHubClient();
      const existingDb = await github.getFile(DATABASE_FILE, DATABASE_BRANCH);

      if (existingDb) {
        console.log(`Downloaded database: ${existingDb.content.length} bytes`);
        fs.writeFileSync(state.localDbPath, existingDb.content);
      } else {
        console.log(`No database found on GitHub, creating empty database`);
      }
    }

    state.database = new Database(state.localDbPath, { create: true });

    state.database.run(`
      CREATE TABLE IF NOT EXISTS dialogs (
        quest TEXT NOT NULL,
        character TEXT NOT NULL,
        text TEXT NOT NULL,
        uri TEXT NOT NULL
      )
    `);
    state.database.run(`CREATE INDEX IF NOT EXISTS idx_dialogs_character ON dialogs(character)`);
    state.database.run(`CREATE INDEX IF NOT EXISTS idx_dialogs_character_text ON dialogs(character, text)`);

    return state.database;
  };

  const checkDialogExists = async (character: string, text: string): Promise<boolean> => {
    const database = await ensureDatabase();

    const row = database
      .query("SELECT 1 FROM dialogs WHERE character = ? AND text = ? LIMIT 1")
      .get(character, text);

    return row !== null;
  };

  const getCompletedQuests = async (): Promise<readonly string[]> => {
    const database = await ensureDatabase();

    const rows = database
      .query("SELECT DISTINCT quest FROM dialogs")
      .all() as { quest: string }[];

    return rows.map((row) => row.quest);
  };

  const getDialogsByCharacter = async (character: string): Promise<readonly DialogRecord[]> => {
    const database = await ensureDatabase();

    const rows = database
      .query("SELECT quest, character, text, uri FROM dialogs WHERE character = ?")
      .all(character) as DialogRecord[];

    return rows;
  };

  const insertDialog = async (record: DialogRecord): Promise<void> => {
    const database = await ensureDatabase();

    const exists = await checkDialogExists(record.character, record.text);
    if (exists) {
      console.log(`Dialog already exists for ${record.character}: "${record.text.substring(0, 50)}..."`);
      return;
    }

    database
      .query("INSERT INTO dialogs (quest, character, text, uri) VALUES (?, ?, ?, ?)")
      .run(record.quest, record.character, record.text, record.uri);

    console.log(`Inserted dialog for ${record.character}: "${record.text.substring(0, 50)}..."`);
  };

  const uploadDatabase = async (): Promise<void> => {
    if (!state.localDbPath || !state.database) {
      console.warn("No database changes to upload");
      return;
    }

    const isDryRun = process.env.DRY_RUN === "true";

    state.database.close();

    if (isDryRun) {
      console.log(`[DRY RUN] Database saved locally at: ${state.localDbPath}`);
      console.log(`[DRY RUN] Would upload database to GitHub (${DATABASE_BRANCH} branch)`);
      state.database = null;
      return;
    }

    const github = getGitHubClient();
    const content = fs.readFileSync(state.localDbPath);

    await github.createOrUpdateFile(
      DATABASE_FILE,
      content,
      DATABASE_BRANCH,
      "feat: Update quest voiceover database"
    );

    console.log("Database uploaded to GitHub");

    fs.rmSync(path.dirname(state.localDbPath), { recursive: true });
    state.localDbPath = null;
    state.database = null;
  };

  const closeDatabase = async (): Promise<void> => {
    if (state.database) {
      state.database.close();
      state.database = null;
    }

    if (state.localDbPath) {
      try {
        fs.rmSync(path.dirname(state.localDbPath), { recursive: true });
      } catch {
        // ignore cleanup errors
      }
      state.localDbPath = null;
    }
  };

  const resetConnection = (): void => {
    if (state.database) {
      state.database.close();
      state.database = null;
    }
    state.localDbPath = null;
  };

  return {
    checkDialogExists,
    getCompletedQuests,
    getDialogsByCharacter,
    insertDialog,
    uploadDatabase,
    closeDatabase,
    resetConnection,
  };
}

let databaseProviderInstance: DatabaseProvider | null = null;

export function getDatabaseProvider(): DatabaseProvider {
  if (databaseProviderInstance) {
    return databaseProviderInstance;
  }

  databaseProviderInstance = createDatabaseProvider();
  return databaseProviderInstance;
}

export function resetDatabaseProvider(): void {
  if (databaseProviderInstance) {
    databaseProviderInstance.resetConnection();
  }
  databaseProviderInstance = null;
}
