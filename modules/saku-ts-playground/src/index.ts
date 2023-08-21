import {
  addIdentityStatements, addResourceStatements,
  connect,
  disconnect,
  evaluateMany,
  evaluateOne,
  isIdentityPolicy,
  isPolicyDocument,
  isResourcePolicy,
  policies,
  PolicyDocument,
  retractPolicies, retractStatements,
  serverMeta,
  upsertIdentityPolicies,
  upsertResourcePolicies
} from "saku-policy-store-client";

connect("http://localhost:8080/api");

// const meta = await serverMeta();
// console.log(await serverMeta());

// serverMeta().then((meta) => {
//   console.log(`Connected to Saku Policy Store ${meta.version} on ${meta.environmentId}`);
// });

// upsertIdentityPolicies([{
//   drn: "drn:policy:example:1",
//   statements: [{
//     effect: "ALLOW",
//     actionIds: ["read"],
//     resources: ["drn:resource:example:1"]
//   }]
// }]).then((result) => {
//   console.log(`Upserted ${result.length} policies`);
// }).catch((err) => {
//   console.error("ERRRRRR", err);
// });

//upsertResourcePolicies

Promise.all([
  upsertIdentityPolicies([{
    drn: "drn:identity:example:1",
    statements: [{
      effect: "ALLOW",
      actionIds: ["read"],
      resources: ["drn:resource:example:1"]
    }]
  }]),
  upsertResourcePolicies([{
    drn: "drn:resource:example:1",
    statements: [{
      effect: "ALLOW",
      actionIds: ["read"],
      identities: ["drn:identity:example:1"]
    }]
  }]),

  addIdentityStatements({
    drn: "drn:identity:example:1",
    statements: [{
      sid: "i1",
      effect: "ALLOW",
      actionIds: ["read"],
      resources: ["drn:resource:example:2"]
    }]
  }),
  addResourceStatements({
    drn: "drn:resource:example:2",
    statements: [{
      sid: "r1",
      effect: "ALLOW",
      actionIds: ["read"],
      identities: ["drn:identity:example:1"]
    }]
  }),
  retractStatements({
    drn: "drn:identity:example:1",
    statementIds: ["i1"]
  }),
  retractStatements({
    drn: "drn:resource:example:2",
    statementIds: ["r1"]
  }),


  evaluateOne({
    drn: "drn:resource:example:1",
    actionId: "read",
    identities: ["drn:identity:example:1"]
  }),

  evaluateOne({
    drn: "drn:resource:example:1",
    actionId: "read2",
    identities: ["drn:identity:example:1"]
  }),

  evaluateMany({
    drns: ["drn:resource:example:1", "drn:resource:example:2"],
    actionId: "read2",
    identities: ["drn:identity:example:1"]
  }),

  policies(["drn:resource:example:1", "drn:identity:example:1"]),
  // policies(["drn:policy:example:1", "drn:policy:example:2"]),
  //retractPolicies(["drn:identity:example:1", "drn:resource:example:1"])
]).then((data) => {
    console.log("done", data);
    console.log(JSON.stringify(data[data.length - 1], null, 2));

    if (data[data.length - 1]) {
      const my_policies = data[data.length - 1] as PolicyDocument[];
      console.log("ummm", my_policies[0]);
      const x = my_policies[0];
      my_policies.map((p) => {
        console.log("p", p, isPolicyDocument(p), isIdentityPolicy(p), isResourcePolicy(p));
      });
    }

  }).catch((err) => {
    console.error("ERRRRRR", err);
  });


// Promise.all([
//   upsertIdentityPolicies([{
//     drn: "drn:identity:example:1",
//     statements: [{
//       effect: "ALLOW",
//       actionIds: ["read"],
//       resources: ["drn:resource:example:1"]
//     }]
//   }]),
//   upsertResourcePolicies([{
//     drn: "drn:resource:example:1",
//     statements: [{
//       effect: "ALLOW",
//       actionIds: ["read"],
//       identities: ["drn:identity:example:1"]
//     }]
//   }])
// ]).then((data) => {
//   console.log("done", data);
// }).catch((err) => {
//   console.error("ERRRRRR", err);
// });


// (async function () {  
// })().then(() => {
// });

disconnect();
