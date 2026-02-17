import grpc from "@grpc/grpc-js";
import { simulateMatching } from "../services/matching.service";

export const matchingHandlers = {
  Health: (
    _call: grpc.ServerUnaryCall<any, any>,
    callback: grpc.sendUnaryData<any>
  ) => {
    callback(null, { status: "ok" });
  },

  Match: async (
    call: grpc.ServerUnaryCall<any, any>,
    callback: grpc.sendUnaryData<any>
  ) => {
    const requester = call.request?.requester || "unknown";
    console.log(`[MatchingService] Match request received from: ${requester}`);
    console.log(`[MatchingService] Request details:`, JSON.stringify(call.request, null, 2));
    
    try {
      console.log(`[MatchingService] Processing matching for: ${requester}`);
      const result = await simulateMatching();
      console.log(`[MatchingService] Matching completed - RequestId: ${result.requestId}, Matches: ${result.matches?.length || 0}`);
      callback(null, result);
    } catch (err: any) {
      console.error(`[MatchingService] Error processing match request:`, err);
      callback(
        {
          code: grpc.status.INTERNAL,
          message: err?.message ?? "Internal error",
        },
        null
      );
    }
  },
};
