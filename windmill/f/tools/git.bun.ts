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
  readonly branchExists: (branchName: string) => Promise<boolean>;
  readonly createBranch: (branchName: string, sourceBranch: string) => Promise<void>;
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

  // The content-addressed git blob sha of a file on a branch (null if absent). Identical
  // content yields the same sha across branches, so comparing shas tells whether a file
  // differs from a baseline without downloading it.
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

  return {
    getFile,
    fileExists,
    createOrUpdateFile,
    uploadAudioFile,
    checkAudioFileExists,
    getFileSha,
    branchExists,
    createBranch,
  };
}
