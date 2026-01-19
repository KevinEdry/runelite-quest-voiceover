import { Octokit } from "@octokit/rest";
import { createRateLimiterRegistry, withRateLimit, type RateLimiter } from "./base.client.js";
import type { RateLimitConfig } from "../types/index.js";

const RATE_LIMIT_CONFIGS: Record<string, RateLimitConfig> = {
  contentCreate: { tokens: 30, refillRate: 30, intervalMs: 60000 },
  contentEdit: { tokens: 30, refillRate: 30, intervalMs: 60000 },
};

export interface GitHubClientConfig {
  readonly token: string;
  readonly owner: string;
  readonly repo: string;
}

export interface GitHubClient {
  readonly fileExists: (path: string, branch: string) => Promise<boolean>;
  readonly getFile: (path: string, branch: string) => Promise<{ content: Buffer; sha: string } | null>;
  readonly createFile: (path: string, content: Buffer, branch: string, message: string) => Promise<void>;
  readonly updateFile: (path: string, content: Buffer, branch: string, message: string, sha: string) => Promise<void>;
  readonly createOrUpdateFile: (path: string, content: Buffer, branch: string, message: string) => Promise<void>;
  readonly uploadAudioFile: (input: UploadAudioInput) => Promise<string>;
  readonly checkAudioFileExists: (hash: string, soundsBranch: string) => Promise<boolean>;
  readonly createBranch: (branchName: string, sourceBranch: string) => Promise<void>;
  readonly branchExists: (branchName: string) => Promise<boolean>;
}

export interface UploadAudioInput {
  readonly audioData: Buffer;
  readonly hash: string;
  readonly questName: string;
  readonly character: string;
  readonly soundsBranch: string;
}

export function createGitHubClient(config: GitHubClientConfig): GitHubClient {
  const octokit = new Octokit({ auth: config.token });
  const getRateLimiter = createRateLimiterRegistry(RATE_LIMIT_CONFIGS);

  const fileExists = async (path: string, branch: string): Promise<boolean> => {
    try {
      await octokit.repos.getContent({
        owner: config.owner,
        repo: config.repo,
        path,
        ref: branch,
      });
      return true;
    } catch (error: unknown) {
      if (error && typeof error === "object" && "status" in error && error.status === 404) {
        return false;
      }
      throw error;
    }
  };

  const getFile = async (
    path: string,
    branch: string
  ): Promise<{ content: Buffer; sha: string } | null> => {
    try {
      const response = await octokit.repos.getContent({
        owner: config.owner,
        repo: config.repo,
        path,
        ref: branch,
      });

      if (Array.isArray(response.data) || response.data.type !== "file") {
        return null;
      }

      const sha = response.data.sha;

      if (response.data.content && response.data.content.length > 0) {
        const content = Buffer.from(response.data.content, "base64");
        return { content, sha };
      }

      const rawUrl = `https://raw.githubusercontent.com/${config.owner}/${config.repo}/${branch}/${path}`;
      const rawResponse = await fetch(rawUrl);
      if (!rawResponse.ok) {
        throw new Error(`Failed to fetch raw file: ${rawResponse.status}`);
      }
      const content = Buffer.from(await rawResponse.arrayBuffer());
      return { content, sha };
    } catch (error: unknown) {
      if (error && typeof error === "object" && "status" in error && error.status === 404) {
        return null;
      }
      throw error;
    }
  };

  const createFile = async (
    path: string,
    content: Buffer,
    branch: string,
    message: string
  ): Promise<void> => {
    const buffer = Buffer.isBuffer(content) ? content : Buffer.from(content);
    await withRateLimit(getRateLimiter("contentCreate"), async () => {
      await octokit.repos.createOrUpdateFileContents({
        owner: config.owner,
        repo: config.repo,
        path,
        message,
        content: buffer.toString("base64"),
        branch,
      });
    });
  };

  const updateFile = async (
    path: string,
    content: Buffer,
    branch: string,
    message: string,
    sha: string
  ): Promise<void> => {
    const buffer = Buffer.isBuffer(content) ? content : Buffer.from(content);
    await withRateLimit(getRateLimiter("contentEdit"), async () => {
      await octokit.repos.createOrUpdateFileContents({
        owner: config.owner,
        repo: config.repo,
        path,
        message,
        content: buffer.toString("base64"),
        branch,
        sha,
      });
    });
  };

  const createOrUpdateFile = async (
    path: string,
    content: Buffer,
    branch: string,
    message: string
  ): Promise<void> => {
    const existing = await getFile(path, branch);

    if (existing) {
      await updateFile(path, content, branch, message, existing.sha);
    } else {
      await createFile(path, content, branch, message);
    }
  };

  const uploadAudioFile = async (input: UploadAudioInput): Promise<string> => {
    const isDryRun = process.env.DRY_RUN === "true";
    const filename = `${input.hash}.mp3`;

    if (isDryRun) {
      console.log(`[DRY RUN] Would upload audio file: ${filename} (${input.questName} - ${input.character})`);
      return filename;
    }

    const message = `feat: Add sound for quest ${input.questName} character: ${input.character}`;
    await createOrUpdateFile(filename, input.audioData, input.soundsBranch, message);

    return filename;
  };

  const checkAudioFileExists = async (hash: string, soundsBranch: string): Promise<boolean> => {
    const path = `${hash}.mp3`;
    return fileExists(path, soundsBranch);
  };

  const branchExists = async (branchName: string): Promise<boolean> => {
    try {
      await octokit.git.getRef({
        owner: config.owner,
        repo: config.repo,
        ref: `heads/${branchName}`,
      });
      return true;
    } catch (error: unknown) {
      if (error && typeof error === "object" && "status" in error && error.status === 404) {
        return false;
      }
      throw error;
    }
  };

  const createBranch = async (branchName: string, sourceBranch: string): Promise<void> => {
    const sourceRef = await octokit.git.getRef({
      owner: config.owner,
      repo: config.repo,
      ref: `heads/${sourceBranch}`,
    });

    const sourceSha = sourceRef.data.object.sha;

    await octokit.git.createRef({
      owner: config.owner,
      repo: config.repo,
      ref: `refs/heads/${branchName}`,
      sha: sourceSha,
    });

    console.log(`Created branch ${branchName} from ${sourceBranch}`);
  };

  return {
    fileExists,
    getFile,
    createFile,
    updateFile,
    createOrUpdateFile,
    uploadAudioFile,
    checkAudioFileExists,
    createBranch,
    branchExists,
  };
}

let githubClientInstance: GitHubClient | null = null;

export function getGitHubClient(): GitHubClient {
  if (githubClientInstance) {
    return githubClientInstance;
  }

  const token = process.env.GITHUB_TOKEN;
  const owner = process.env.GITHUB_OWNER;
  const repo = process.env.GITHUB_REPO;

  if (!token) {
    throw new Error("GITHUB_TOKEN environment variable is required");
  }
  if (!owner) {
    throw new Error("GITHUB_OWNER environment variable is required");
  }
  if (!repo) {
    throw new Error("GITHUB_REPO environment variable is required");
  }

  githubClientInstance = createGitHubClient({ token, owner, repo });
  return githubClientInstance;
}
