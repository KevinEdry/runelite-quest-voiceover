// Retry helper shared by the API-client toolsets. Windmill runs each loop iteration
// as a separate job, so rate limiting can't be coordinated in-process; this absorbs
// the throttling / transient errors that flow concurrency alone doesn't prevent:
//   - 429 (rate limited) and 5xx (transient server errors)
//   - 409 — parallel commits racing a branch ref forward; each job writes a distinct
//     file, so retrying resolves it
//   - 403 *only when it's a GitHub secondary/abuse rate limit* (never a genuine
//     permission 403), detected via headers / message
// When the response carries a Retry-After or X-RateLimit-Reset, that wait is honored
// instead of blind exponential backoff — GitHub returns 403/429 with these on its
// secondary limits and expects callers to wait exactly that long.
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

// A 403 that is really a rate/abuse limit (not a permission error), or any 429.
function isRateLimit(error: unknown): boolean {
  const status = extractStatus(error);
  if (status === 429) return true;
  if (status !== 403) return false;
  const headers = extractHeaders(error);
  if (headers && (headers["retry-after"] || headers["x-ratelimit-remaining"] === "0")) return true;
  const message = extractMessage(error).toLowerCase();
  return message.includes("secondary rate limit") || message.includes("abuse") || message.includes("rate limit");
}

// Explicit wait requested by the server, in ms, if any.
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
      // Retry transient/rate-limit statuses and network errors (no status); throw
      // anything else (e.g. a genuine 403/404/422) immediately.
      const retryable = rateLimited || status === undefined || RETRYABLE_STATUS.has(status);
      if (!retryable) throw error;

      const explicitWait = retryAfterMs(error);
      const backoffMs =
        explicitWait !== undefined
          ? Math.min(MAX_WAIT_MS, explicitWait + 1000) // small buffer past the reset
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
