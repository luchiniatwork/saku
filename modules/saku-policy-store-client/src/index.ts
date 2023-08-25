export type Drn = string

export type ActionId = string

export type Effect = 'DENY' | 'ALLOW'

export type Nature = 'IMPLICIT' | 'EXPLICIT'

export type PolicyType = 'Identity' | 'Resource'

export type ErrorCategory =
    | 'unavailable'
    | 'interrupted'
    | 'incorrect'
    | 'forbidden'
    | 'unsupported'
    | 'notFound'
    | 'conflict'
    | 'fault'
    | 'busy'

export type Error = {
    category: ErrorCategory
    message: string
    data?: any
}

export type IdentityStatement = {
    sid?: string
    actionIds: ActionId[]
    resources: Drn[]
    effect: Effect
}

export type IdentityPolicy = {
    drn: Drn
    statements: IdentityStatement[]
}

export type ResourceStatement = {
    sid?: string
    actionIds: ActionId[]
    identities: Drn[]
    effect: Effect
}

export type ResourcePolicy = {
    drn: Drn
    statements: ResourceStatement[]
}

export type PolicyDocument = { drn: Drn } & (IdentityPolicy | ResourceStatement)

export interface DescribePoliciesInput {
    drns: Drn[]
    policyType?: PolicyType
}

export interface DescribePoliciesOutput {
    policies: PolicyDocument[]
}

export interface UpsertResourcePoliciesInput {
    policies: ResourcePolicy[]
}

export interface UpsertResourcePoliciesOutput {
    policies: ResourcePolicy[]
}

export interface UpsertIdentityPoliciesInput {
    policies: IdentityPolicy[]
}

export interface UpsertIdentityPoliciesOutput {
    policies: IdentityPolicy[]
}

export interface AddIdentityStatementsInput {
    policy: IdentityPolicy
}

export interface AddIdentityStatementsOutput {
    policies: IdentityPolicy[]
}

export interface AddResourceStatementsInput {
    policy: ResourcePolicy
}

export interface AddResourceStatementsOutput {
    policies: ResourcePolicy[]
}

export interface RetractStatementsInput {
    drn: Drn
    sids: string[]
}

export interface RetractStatementsOutput {
    policy: PolicyDocument
}

export interface RetractPoliciesInput {
    drns: Drn[]
}

export interface RetractPoliciesOutput {
    retractedDrns: Drn[]
}

export type EvaluationResult = {
    effect: Effect
    nature: Nature
}

export interface EvaluateOneInput {
    drn: Drn
    actionId: ActionId
    identities: Drn[]
}

export type EvaluateOneOutput = EvaluationResult

export type EvaluationResultSet = {
    drn: Drn
    result: EvaluationResult
}

export interface EvaluateManyInput {
    drns: Drn[]
    actionId: ActionId
    identities: Drn[]
}

export type EvaluateManyOutput = EvaluationResultSet[]

interface ClientArgm {
    url: string
}

class Client {
    public readonly url: string

    constructor(argm: ClientArgm) {
        const { url } = argm
        this.url = url
    }
}

/**
 * Returns a new `Client` object for use in API calls.
 * @param argm The client argument map.
 * @param argm.url The host url for saku-policy-store
 */
export function client(argm: ClientArgm) {
    return new Client(argm)
}

async function send<RequestData, ResponseData>(
    client: Client,
    argm: {
        op: string
        request: RequestData
    }
): Promise<ResponseData | Error> {
    const { op, request } = argm

    try {
        const response = await fetch(`${client.url}/api/${op}`, {
            method: 'POST',
            headers: {
                'content-type': 'application/json;charset=UTF-8',
            },
            body: JSON.stringify(request),
        })
        const { status } = response

        if (status === 200) {
            return (await response.json()) as ResponseData
        } else if (status === 400) {
            return {
                category: 'incorrect',
                message: `Invalid '${op}' request`,
                data: await response.json(),
            }
        } else if (status === 404) {
            return {
                category: 'notFound',
                message: `Unknown operation '${op}'`,
            }
        } else if (status === 405) {
            return {
                category: 'unsupported',
                message: `Operation '${op}' is not supported.`,
            }
        } else {
            return {
                category: 'fault',
                message: 'Unhandled error.',
                data: { text: await response.text() },
            }
        }
    } catch (error: any) {
        return {
            category: 'fault',
            message: error?.message,
            data: {
                ...(error?.cause && { cause: error.cause }),
            },
        }
    }
}

/**
 * Returns the set of policies for `drns`.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function describePolicies(
    client: Client,
    argm: DescribePoliciesInput
) {
    return await send<DescribePoliciesInput, DescribePoliciesOutput>(client, {
        op: 'DescribePolicies',
        request: argm,
    })
}

/**
 * Updates or inserts the `policies` specified. If a policy exists, the policy
 * data will be replaced with the data passed here. If the policy does not exist,
 * a new policy will be created.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function upsertResourcePolicies(
    client: Client,
    argm: UpsertResourcePoliciesInput
) {
    return await send<
        UpsertResourcePoliciesInput & { policyType: PolicyType },
        UpsertResourcePoliciesOutput
    >(client, {
        op: 'UpsertPolicies',
        request: {
            ...argm,
            policyType: 'Resource',
        },
    })
}

/**
 * Updates or inserts the `policies` specified. If a policy exists, the policy
 * data will be replaced with the data passed here. If the policy does not exist,
 * a new policy will be created.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function upsertIdentityPolicies(
    client: Client,
    argm: UpsertIdentityPoliciesInput
) {
    return await send<
        UpsertIdentityPoliciesInput & { policyType: PolicyType },
        UpsertIdentityPoliciesOutput
    >(client, {
        op: 'UpsertPolicies',
        request: {
            ...argm,
            policyType: 'Identity',
        },
    })
}

/**
 * Adds or updates statements for the specified `policy`. Statements without a
 * `sid` will be added to the policy. Statements with a `sid` will be added if
 * a statement with the `sid` does not already exist in the policy, otherwise
 * the existing statement will be replaced.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function addIdentityStatements(
    client: Client,
    argm: AddIdentityStatementsInput
) {
    return await send<
        AddIdentityStatementsInput & { policyType: PolicyType },
        AddIdentityStatementsOutput
    >(client, {
        op: 'AddStatements',
        request: {
            ...argm,
            policyType: 'Identity',
        },
    })
}

/**
 * Adds or updates statements for the specified `policy`. Statements without a
 * `sid` will be added to the policy. Statements with a `sid` will be added if
 * a statement with the `sid` does not already exist in the policy, otherwise
 * the existing statement will be replaced.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function addResourceStatements(
    client: Client,
    argm: AddResourceStatementsInput
) {
    return await send<
        AddResourceStatementsInput & { policyType: PolicyType },
        AddResourceStatementsOutput
    >(client, {
        op: 'AddStatements',
        request: {
            ...argm,
            policyType: 'Resource',
        },
    })
}

/**
 * Removes statement IDs `sids` from the policy identified by `drn`. If the `sid`
 * does not exist, nothing will happen. If the `sid` exists, it will be entirely
 * removed from the policy.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function retractStatements(
    client: Client,
    argm: RetractStatementsInput
) {
    return await send<RetractStatementsInput, RetractStatementsOutput>(client, {
        op: 'RetractStatements',
        request: argm,
    })
}

/**
 * Removes the policies identified by `drns`.
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function retractPolicies(
    client: Client,
    argm: RetractPoliciesInput
) {
    return await send<RetractPoliciesInput, RetractPoliciesOutput>(client, {
        op: 'RetractPolicies',
        request: argm,
    })
}

/**
 * Computes the effect for the given inputs.
 * https://github.com/luchiniatwork/saku#effects
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function evaluateOne(client: Client, argm: EvaluateOneInput) {
    return await send<EvaluateOneInput, EvaluateOneOutput>(client, {
        op: 'EvaluateOne',
        request: argm,
    })
}

/**
 * Computes the effect for the given inputs.
 * https://github.com/luchiniatwork/saku#effects
 *
 * @param client the client for this api interaction
 * @param argm
 */
export async function evaluateMany(client: Client, argm: EvaluateManyInput) {
    return await send<EvaluateManyInput, EvaluateManyOutput>(client, {
        op: 'EvaluateMany',
        request: argm,
    })
}
