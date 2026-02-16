import { SAMPLE_MATCHES } from "../data/sample.matches";
import { randomInt, sleep } from "../utils/sleep";
import { requestId } from "../utils/requestId";

export async function simulateMatching() {
  const latencyMs = randomInt(100, 200);
  await sleep(latencyMs);

  return {
    requestId: requestId(),
    simulatedLatencyMs: latencyMs,
    serverTimeEpochMs: Date.now(),
    matches: SAMPLE_MATCHES,
  };
}
