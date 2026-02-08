import type { RateLimitConfig } from "@/types/index.js";

interface TokenBucketState {
  readonly tokens: number;
  readonly lastRefill: number;
}

function computeRefill(
  state: TokenBucketState,
  config: RateLimitConfig
): TokenBucketState {
  const now = Date.now();
  const elapsed = now - state.lastRefill;
  const tokensToAdd = Math.floor((elapsed / config.intervalMs) * config.refillRate);

  if (tokensToAdd <= 0) {
    return state;
  }

  return {
    tokens: Math.min(config.tokens, state.tokens + tokensToAdd),
    lastRefill: now,
  };
}

export interface RateLimiter {
  readonly acquire: () => Promise<void>;
  readonly getAvailableTokens: () => number;
}

export function createRateLimiter(config: RateLimitConfig): RateLimiter {
  let state: TokenBucketState = {
    tokens: config.tokens,
    lastRefill: Date.now(),
  };

  const acquire = async (): Promise<void> => {
    state = computeRefill(state, config);

    if (state.tokens >= 1) {
      state = { ...state, tokens: state.tokens - 1 };
      return;
    }

    const waitTime = Math.ceil(
      ((1 - state.tokens) / config.refillRate) * config.intervalMs
    );

    await new Promise<void>((resolve) => {
      setTimeout(() => {
        state = computeRefill(state, config);
        state = { ...state, tokens: state.tokens - 1 };
        resolve();
      }, waitTime);
    });
  };

  const getAvailableTokens = (): number => {
    state = computeRefill(state, config);
    return state.tokens;
  };

  return { acquire, getAvailableTokens };
}

export function createRateLimiterRegistry(
  configs: Record<string, RateLimitConfig>
): (endpoint: string) => RateLimiter {
  const limiters = new Map<string, RateLimiter>();

  return (endpoint: string): RateLimiter => {
    const existing = limiters.get(endpoint);
    if (existing) {
      return existing;
    }

    const config = configs[endpoint];
    if (!config) {
      throw new Error(`Unknown rate limit endpoint: ${endpoint}`);
    }

    const limiter = createRateLimiter(config);
    limiters.set(endpoint, limiter);
    return limiter;
  };
}

export async function withRateLimit<T>(
  limiter: RateLimiter,
  operation: () => Promise<T>
): Promise<T> {
  await limiter.acquire();
  return operation();
}
