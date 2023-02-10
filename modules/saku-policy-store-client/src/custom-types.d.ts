declare module 'saku-policy-store-client' {

    export type Drn = string;

    export type Effect = 'DENY' | 'ALLOW';

    export type Nature = 'IMPLICIT' | 'EXPLICIT';

    export type ActionId = string;

    export type ResourceStatement = {
        actionIds: ActionId[],
        identities: Drn[],
        effect: Effect,
    }

    export type ResourcePolicy = {
        drn: Drn;
        statements: ResourceStatement[]
    }

    export type IdentityStatement = {
        actionIds: ActionId[],
        resources: Drn[],
        effect: Effect,
    }

    export type IdentityPolicy = {
        drn: Drn;
        statements: IdentityStatement[]
    }

    export type PolicyDocument = { drn: Drn } & (IdentityPolicy | ResourceStatement);

    export function isIdentityPolicy(type: unknown): type is IdentityPolicy;

    export function isResourcePolicy(type: unknown): type is ResourcePolicy;

    export function isPolicyDocument(type: unknown): type is PolicyDocument;


    export type EvaluateOneParams = {
        drn: Drn,
        actionId: ActionId,
        identities: Drn[]
    };

    export type EvaluateManyParams = {
        drns: Drn[],
        actionId: ActionId,
        identities: Drn[]
    };

    export type EvaluationResult = {
        effect: 'DENY' | 'ALLOW';
        nature: 'IMPLICIT' | 'EXPLICIT';
    };

    export type EvaluationResultSet = {
        drn: Drn,
        result: EvaluationResult
    };

    export type ServerMeta = {
        version: string,
        environmentId: string
    };

    export function connect(url: string): void;

    export function disconnect(): void;

    export function serverMeta(): Promise<ServerMeta>

    export function policies(drns: Drn[]): Promise<PolicyDocument[]>

    export function resourcePolicies(drns: Drn[]): Promise<ResourcePolicy[]>

    export function identityPolicies(drns: Drn[]): Promise<IdentityPolicy[]>

    export function upsertResourcePolicies(policies: ResourcePolicy[]): Promise<ResourcePolicy[]>

    export function upsertIdentityPolicies(policies: IdentityPolicy[]): Promise<IdentityPolicy[]>

    export function retractPolicies(drns: Drn[]): Promise<Drn[]>

    export function evaluateOne(params?: EvaluateOneParams): Promise<EvaluationResult>;

    export function evaluateMany(params?: EvaluateManyParam): Promise<EvaluationResultSet>;

}
