declare module 'saku-core' {

    export type Drn = string;

    export type Effect = 'DENY' | 'ALLOW';

    export type Nature = 'IMPLICIT' | 'EXPLICIT';

    export type ActionId = string;

    export type ResourceStatement = {
        actions: ActionId[],
        identities: Drn[],
        effect: Effect,
    }

    export type ResourcePolicy = {
        drn: Drn;
        statements: ResourceStatement[]
    }

    export type IdentityStatement = {
        actions: ActionId[],
        resources: Drn[],
        effect: Effect,
    }

    export type IdentityPolicy = {
        drn: Drn;
        statements: IdentityStatement[]
    }

    export type EvaluateParams = {
        drn: Drn,
        action: ActionId,
        identityPoliciesMap?: Record<Drn, IdentityPolicy>
    };

    export type EvaluationResult = {
        effect: 'DENY' | 'ALLOW';
        nature: 'IMPLICIT' | 'EXPLICIT';
    }

    export function evaluateOne(
        params?: EvaluateParams & { resourcePolicy?: ResourcePolicy }
    ): EvaluationResult;

    export function evaluateMany(
        params?: EvaluateParams & { resourcePolicies?: ResourcePolicy[] }
    ): EvaluationResult;

}
