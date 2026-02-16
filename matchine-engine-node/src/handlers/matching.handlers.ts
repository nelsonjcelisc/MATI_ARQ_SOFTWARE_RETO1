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
    _call: grpc.ServerUnaryCall<any, any>,
    callback: grpc.sendUnaryData<any>
  ) => {
    try {
      const result = await simulateMatching();
      callback(null, result);
    } catch (err: any) {
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
