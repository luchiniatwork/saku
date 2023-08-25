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
    statementIds: string[]
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

export async function describePolicies(
    client: Client,
    argm: DescribePoliciesInput
) {
    return await send<DescribePoliciesInput, DescribePoliciesOutput>(client, {
        op: 'DescribePolicies',
        request: argm,
    })
}

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

export async function retractStatements(
    client: Client,
    argm: RetractStatementsInput
) {
    return await send<RetractStatementsInput, RetractStatementsOutput>(client, {
        op: 'RetractStatements',
        request: argm,
    })
}

export async function retractPolicies(
    client: Client,
    argm: RetractPoliciesInput
) {
    return await send<RetractPoliciesInput, RetractPoliciesOutput>(client, {
        op: 'RetractPolicies',
        request: argm,
    })
}

export async function evaluateOne(client: Client, argm: EvaluateOneInput) {
    return await send<EvaluateOneInput, EvaluateOneOutput>(client, {
        op: 'EvaluateOne',
        request: argm,
    })
}

export async function evaluateMany(client: Client, argm: EvaluateManyInput) {
    return await send<EvaluateManyInput, EvaluateManyOutput>(client, {
        op: 'EvaluateMany',
        request: argm,
    })
}
