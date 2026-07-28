// Windmill runs each loop iteration as a separate job, so API rate limits can't be
// coordinated in-process — this absorbs what flow concurrency alone doesn't prevent.
// 409 is retryable because parallel commits race a branch ref forward (each job writes a
// distinct file). GitHub signals its secondary limits with a Retry-After / X-RateLimit-
// Reset, which is honored instead of blind backoff.
const RETRYABLE_STATUS = new Set([409, 429, 500, 502, 503, 504]);
const MAX_ATTEMPTS = 6;
const MAX_WAIT_MS = 120_000;

export function extractStatus(error: unknown): number | undefined {
  if (error && typeof error === "object") {
    if ("status" in error && typeof error.status === "number") return error.status;
    if ("statusCode" in error && typeof error.statusCode === "number") return error.statusCode;
  }
  return undefined;
}

function extractHeaders(error: unknown): Record<string, string> | undefined {
  if (
    error &&
    typeof error === "object" &&
    "response" in error &&
    error.response &&
    typeof error.response === "object" &&
    "headers" in error.response &&
    error.response.headers &&
    typeof error.response.headers === "object"
  ) {
    return error.response.headers as Record<string, string>;
  }
  return undefined;
}

function extractMessage(error: unknown): string {
  if (error && typeof error === "object" && "message" in error && typeof error.message === "string") {
    return error.message;
  }
  return String(error);
}

// GitHub returns 403 for both secondary rate limits and permission errors; only the
// former should be retried.
function isRateLimit(error: unknown): boolean {
  const status = extractStatus(error);
  if (status === 429) return true;
  if (status !== 403) return false;
  const headers = extractHeaders(error);
  if (headers && (headers["retry-after"] || headers["x-ratelimit-remaining"] === "0")) return true;
  const message = extractMessage(error).toLowerCase();
  return message.includes("secondary rate limit") || message.includes("abuse") || message.includes("rate limit");
}

function retryAfterMs(error: unknown): number | undefined {
  const headers = extractHeaders(error);
  if (!headers) return undefined;

  const retryAfter = headers["retry-after"];
  if (retryAfter) {
    const seconds = Number(retryAfter);
    if (!Number.isNaN(seconds)) return seconds * 1000;
  }

  if (headers["x-ratelimit-remaining"] === "0" && headers["x-ratelimit-reset"]) {
    const resetMs = Number(headers["x-ratelimit-reset"]) * 1000;
    if (!Number.isNaN(resetMs)) {
      const delta = resetMs - Date.now();
      if (delta > 0) return delta;
    }
  }
  return undefined;
}

export async function withRetry<T>(operation: () => Promise<T>, attempts = MAX_ATTEMPTS): Promise<T> {
  let lastError: unknown;
  for (let attempt = 0; attempt < attempts; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;
      const status = extractStatus(error);
      const rateLimited = isRateLimit(error);
      // An undefined status is a network error, which is worth retrying.
      const retryable = rateLimited || status === undefined || RETRYABLE_STATUS.has(status);
      if (!retryable) throw error;

      const explicitWait = retryAfterMs(error);
      const backoffMs =
        explicitWait !== undefined
          ? Math.min(MAX_WAIT_MS, explicitWait + 1000) // clear the reset window
          : Math.min(60_000, 2 ** attempt * 1000);

      console.log(
        `Retrying after ${backoffMs}ms (attempt ${attempt + 1}/${attempts}, status ${status ?? "unknown"}` +
          `${rateLimited ? ", rate-limited" : ""}${explicitWait !== undefined ? ", server-directed wait" : ""})`
      );
      await new Promise((resolve) => setTimeout(resolve, backoffMs));
    }
  }
  throw lastError;
}
