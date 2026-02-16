import * as grpc from "@grpc/grpc-js";
import { env } from "./app/config/env";
import { buildGrpcServer } from "./app/grpc/server.factory";

function main() {
  const server = buildGrpcServer();
  const address = `${env.HOST}:${env.PORT}`;

  server.bindAsync(address, grpc.ServerCredentials.createInsecure(), (err) => {
    if (err) {
      console.error("Failed to bind:", err);
      process.exit(1);
    }
    console.log(`gRPC server listening on ${address}`);
  });

  process.on("SIGINT", () => {
    console.log("Shutting down...");
    server.tryShutdown(() => process.exit(0));
  });
}

main();
