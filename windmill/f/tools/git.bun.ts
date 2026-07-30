import { Octokit } from "@octokit/rest";
import { extractStatus, withRetry } from "./retry";

export interface GitHubClientConfig {
  readonly token: string;
  readonly owner: string;
  readonly repo: string;
}

export interface UploadAudioInput {
  readonly audioData: Buffer;
  readonly hash: string;
  readonly questName: string;
  readonly character: string;
  readonly soundsBranch: string;
}

export interface GitHubClient {
  readonly getFile: (path: string, branch: string) => Promise<{ content: Buffer; sha: string } | null>;
  readonly fileExists: (path: string, branch: string) => Promise<boolean>;
  readonly createOrUpdateFile: (path: string, content: Buffer, branch: string, message: string) => Promise<void>;
  readonly uploadAudioFile: (input: UploadAudioInput) => Promise<string>;
  readonly checkAudioFileExists: (hash: string, soundsBranch: string) => Promise<boolean>;
  readonly getFileSha: (path: string, branch: string) => Promise<string | null>;
  readonly listDirectory: (path: string, branch: string) => Promise<string[]>;
  readonly branchExists: (branchName: string) => Promise<boolean>;
  readonly createBranch: (branchName: string, sourceBranch: string) => Promise<void>;
  readonly createPullRequest: (input: PullRequestInput) => Promise<{ url: string; number: number } | null>;
}

export interface PullRequestInput {
  readonly head: string;
  readonly base: string;
  readonly title: string;
  readonly body: string;
}

export function createGitHubClient(config: GitHubClientConfig): GitHubClient {
  const octokit = new Octokit({ auth: config.token });

  const fileExists = async (path: string, branch: string): Promise<boolean> => {
    try {
      await octokit.repos.getContent({ owner: config.owner, repo: config.repo, path, ref: branch });
      return true;
    } catch (error: unknown) {
      if (extractStatus(error) === 404) return false;
      throw error;
    }
  };

  const getFile = async (
    path: string,
    branch: string
  ): Promise<{ content: Buffer; sha: string } | null> => {
    try {
      const response = await octokit.repos.getContent({ owner: config.owner, repo: config.repo, path, ref: branch });
      if (Array.isArray(response.data) || response.data.type !== "file") return null;

      const sha = response.data.sha;
      if (response.data.content && response.data.content.length > 0) {
        return { content: Buffer.from(response.data.content, "base64"), sha };
      }

      // Large files (>1MB) come back without inline content; fetch the raw blob.
      const rawUrl = `https://raw.githubusercontent.com/${config.owner}/${config.repo}/${branch}/${path}`;
      const rawResponse = await fetch(rawUrl);
      if (!rawResponse.ok) throw new Error(`Failed to fetch raw file: ${rawResponse.status}`);
      return { content: Buffer.from(await rawResponse.arrayBuffer()), sha };
    } catch (error: unknown) {
      if (extractStatus(error) === 404) return null;
      throw error;
    }
  };

  const createOrUpdateFile = async (
    path: string,
    content: Buffer,
    branch: string,
    message: string
  ): Promise<void> => {
    const existing = await getFile(path, branch);
    await withRetry(() =>
      octokit.repos.createOrUpdateFileContents({
        owner: config.owner,
        repo: config.repo,
        path,
        message,
        content: content.toString("base64"),
        branch,
        sha: existing?.sha,
      })
    );
  };

  const uploadAudioFile = async (input: UploadAudioInput): Promise<string> => {
    const filename = `${input.hash}.mp3`;
    const message = `feat: Add sound for quest ${input.questName} character: ${input.character}`;
    await createOrUpdateFile(filename, input.audioData, input.soundsBranch, message);
    return filename;
  };

  const checkAudioFileExists = async (hash: string, soundsBranch: string): Promise<boolean> =>
    fileExists(`${hash}.mp3`, soundsBranch);

  // Identical content yields the same blob sha across branches, so callers compare shas
  // to detect whether a file differs from a baseline without downloading it.
  const getFileSha = async (path: string, branch: string): Promise<string | null> => {
    try {
      const response = await octokit.repos.getContent({ owner: config.owner, repo: config.repo, path, ref: branch });
      if (Array.isArray(response.data) || response.data.type !== "file") return null;
      return response.data.sha;
    } catch (error: unknown) {
      if (extractStatus(error) === 404) return null;
      throw error;
    }
  };

  const listDirectory = async (path: string, branch: string): Promise<string[]> => {
    const response = await octokit.repos.getContent({ owner: config.owner, repo: config.repo, path, ref: branch });
    if (!Array.isArray(response.data)) return [];
    return response.data.filter((entry) => entry.type === "file").map((entry) => entry.path);
  };

  const branchExists = async (branchName: string): Promise<boolean> => {
    try {
      await octokit.git.getRef({ owner: config.owner, repo: config.repo, ref: `heads/${branchName}` });
      return true;
    } catch (error: unknown) {
      if (extractStatus(error) === 404) return false;
      throw error;
    }
  };

  const createBranch = async (branchName: string, sourceBranch: string): Promise<void> => {
    const source = await octokit.git.getRef({
      owner: config.owner,
      repo: config.repo,
      ref: `heads/${sourceBranch}`,
    });
    await octokit.git.createRef({
      owner: config.owner,
      repo: config.repo,
      ref: `refs/heads/${branchName}`,
      sha: source.data.object.sha,
    });
    console.log(`Created branch ${branchName} from ${sourceBranch}`);
  };

  // Idempotent: a resume run re-opening the same head/base returns the already-open PR
  // instead of failing, and a head with no new commits yields null rather than throwing.
  const createPullRequest = async (
    input: PullRequestInput
  ): Promise<{ url: string; number: number } | null> => {
    try {
      const created = await withRetry(() =>
        octokit.pulls.create({
          owner: config.owner,
          repo: config.repo,
          head: input.head,
          base: input.base,
          title: input.title,
          body: input.body,
        })
      );
      return { url: created.data.html_url, number: created.data.number };
    } catch (error: unknown) {
      if (extractStatus(error) !== 422) throw error;
      const existing = await octokit.pulls.list({
        owner: config.owner,
        repo: config.repo,
        head: `${config.owner}:${input.head}`,
        base: input.base,
        state: "open",
      });
      const open = existing.data[0];
      return open ? { url: open.html_url, number: open.number } : null;
    }
  };

  return {
    getFile,
    fileExists,
    createOrUpdateFile,
    uploadAudioFile,
    checkAudioFileExists,
    getFileSha,
    listDirectory,
    branchExists,
    createBranch,
    createPullRequest,
  };
}
