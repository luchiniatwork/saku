import * as saku from "saku-policy-store-client";

const client = saku.client({url: "http://localhost:8080", debug: true})

const identityPolicy1 = await saku.upsertIdentityPolicies(client, {
    policies: [{
        drn: "drn:identity:example:1",
        statements: [{
            effect: "ALLOW",
            actionIds: ["read"],
            resources: ["drn:resource:example:1"]
        }]
    }]
})
console.log("Identity policy", identityPolicy1)

const resourcePolicy1 = await saku.upsertResourcePolicies(client, {
    policies: [{
        drn: "drn:resource:example:1",
        statements: [{
            effect: "ALLOW",
            actionIds: ["read"],
            identities: ["drn:identity:example:1"]
        }]
    }]
})
console.log("Resource policy", resourcePolicy1)

const addIdentityStatement = await saku.addIdentityStatements(client, {
    policy: {
        drn: "drn:identity:example:1",
        statements: [{
            sid: "i1",
            effect: "ALLOW",
            actionIds: ["read"],
            resources: ["drn:resource:example:2"]
        }]
    }
})
console.log("addIdentityStatement", addIdentityStatement)

const addResourceStatement = await saku.addResourceStatements(client, {
    policy: {
        drn: "drn:resource:example:2",
        statements: [{
            sid: "r1",
            effect: "ALLOW",
            actionIds: ["read"],
            identities: ["drn:identity:example:1"]
        }]
    }
})
console.log("addResourceStatement", addResourceStatement)

const retractIdentityStatement = await saku.retractStatements(client, {
    drn: "drn:identity:example:1",
    sids: ["i1"]
})
console.log("retractIdentityStatement", retractIdentityStatement)

const retractResourceStatement = await saku.retractStatements(client, {
    drn: "drn:resource:example:2",
    sids: ["r1"]
})
console.log("retractResourceStatement", retractResourceStatement)

const evaluateOneResourceAllow = await saku.evaluateOne(client, {
    drn: "drn:resource:example:1",
    actionId: "read",
    identities: ["drn:identity:example:1"]
})
console.log("evaluateOneResourceAllow", evaluateOneResourceAllow)

const evaluateOneResourceDeny = await saku.evaluateOne(client, {
    drn: "drn:resource:example:1",
    actionId: "read2",
    identities: ["drn:identity:example:1"]
})
console.log("evaluateOneResourceDeny", evaluateOneResourceDeny)

const evaluateManyResources = await saku.evaluateMany(client, {
    drns: ["drn:resource:example:1", "drn:resource:example:2"],
    actionId: "read2",
    identities: ["drn:identity:example:1"]
})
console.log("evaluateManyResources", evaluateManyResources)


const policies = await saku.describePolicies(client, {
    drns: ["drn:resource:example:1", "drn:identity:example:1"]
})
console.log("policies", policies)
