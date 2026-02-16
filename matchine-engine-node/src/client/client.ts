import path from "path";
import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";

const PROTO_PATH = path.join(
  __dirname,
  "..", "..",
  "proto",
  "matching.proto"
);

const packageDef = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
});

const proto = grpc.loadPackageDefinition(packageDef) as any;
const matchingPkg = proto.matching;

function main() {
  const target = process.env.TARGET || "localhost:50051";
  const client = new matchingPkg.MatchingService(
    target,
    grpc.credentials.createInsecure()
  );

  client.Health({}, (err: any, res: any) => {
    if (err) return console.error("Health error:", err);
    console.log("Health:", res);
  });

  client.Match({ requester: "test-client" }, (err: any, res: any) => {
    if (err) return console.error("Match error:", err);
    console.log("Match response:\n", JSON.stringify(res, null, 2));
  });
}

main();
