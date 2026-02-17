import { SAMPLE_MATCHES } from "../data/sample.matches";
import { randomInt, sleep } from "../utils/sleep";
import { requestId } from "../utils/requestId";

export async function simulateMatching() {
  const latencyMs = randomInt(100, 200);
  console.log(`[MatchingService] Simulating matching with latency: ${latencyMs}ms`);
  await sleep(latencyMs);

  const result = {
    requestId: requestId(),
    simulatedLatencyMs: latencyMs,
    serverTimeEpochMs: Date.now(),
    matches: SAMPLE_MATCHES,
  };
  
  console.log(`[MatchingService] Generated ${result.matches.length} matches`);
  return result;
}
