import * as grpc from "@grpc/grpc-js";
import { loadProto } from "./loader";
import { matchingHandlers } from "../../handlers/matching.handlers";

export function buildGrpcServer() {
  const matchingPkg = loadProto();
  const server = new grpc.Server();

  server.addService(matchingPkg.MatchingService.service, matchingHandlers);

  return server;
}
