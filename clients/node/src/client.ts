import Agent, { HttpsAgent, type HttpsOptions } from 'agentkeepalive'
import superagent, { type Plugin, type SuperAgentRequest } from 'superagent'

import type { SortBy, SortDirection } from './constants'
import type { ErrorResponse } from './errorTypes'
import type { Page, PageRequest } from './paginationTypes'
import type {
  CreateNonAssociationRequest,
  UpdateNonAssociationRequest,
  CloseNonAssociationRequest,
  DeleteNonAssociationRequest,
} from './requestTypes'
import type {
  Constants,
  NonAssociationsList,
  OpenNonAssociationsListItem,
  ClosedNonAssociationsListItem,
  NonAssociation,
  OpenNonAssociation,
  ClosedNonAssociation,
} from './responseTypes'
import { parseDates } from './parseDates'
import { sanitiseError } from './sanitiseError'

/**
 * API configuration standard in DPS typescript express apps
 */
export interface ApiConfig {
  url: string
  timeout: {
    response: number
    deadline: number
  }
  agent: HttpsOptions
}

/**
 * Logger interface compatible with standard library `console` and with `bunyan`
 */
export interface Logger {
  info(msg: string): void
  warn(msg: string): void
  error(msg: string): void
}

type SortedPageRequest = PageRequest<
  'id' | 'firstPrisonerNumber' | 'secondPrisonerNumber' | 'whenCreated' | 'whenUpdated'
>

/**
 * REST client to access HMPPS Non-associations API
 */
export class NonAssociationsApi {
  readonly name = 'HMPPS Non-associations API'

  readonly agent: Agent

  constructor(
    /**
     * Provide a system token with necessary roles, not a user token
     * READ_NON_ASSOCIATIONS and optionally WRITE_NON_ASSOCIATIONS or DELETE_NON_ASSOCIATIONS
     */
    readonly systemToken: string,

    /**
     * API configuration standard in DPS front-end apps
     */
    readonly config: ApiConfig,

    /**
     * Logger such as standard library’s `console` or `bunyan` instance
     */
    readonly logger: Logger,

    /**
     * Plugins for superagent requests, e.g. restClientMetricsMiddleware
     */
    readonly plugins: Plugin[] = [],
  ) {
    this.agent = config.url.startsWith('https') ? new HttpsAgent(config.agent) : new Agent(config.agent)
  }

  /**
   * Builds requests
   * - uses bearer token authentication
   * - uses keep-alive agent
   * - installs plugins
   * - adds optional retry handler
   * - times-out if necessary
   * - logs messages
   */
  protected sendRequest<Request extends SuperAgentRequest>(
    request: Request,
    retry = false,
    retryAttempts = 2,
  ): Promise<superagent.Response> {
    const req = request
      .agent(this.agent)
      .auth(this.systemToken, { type: 'bearer' })
      .retry(retryAttempts, (err, res): boolean | undefined => {
        if (!retry) {
          return false
        }
        if (err) {
          this.logger.info(
            `${this.name} retrying ${request.method.toUpperCase()} ${request.url} due to error with ${err.code} ${
              err.message
            }`,
          )
        }
        // retry handler only for logging retries, not to influence retry logic
        return undefined
      })
      .timeout(this.config.timeout)
    for (const plugin of this.plugins) {
      req.use(plugin)
    }
    this.logger.info(`${this.name} ${request.method.toUpperCase()}: ${request.url}`)
    return req.then(undefined, error => {
      const sanitisedError = sanitiseError<ErrorResponse>(error)
      this.logger.error(
        `${this.name} error ${request.method.toUpperCase()} ${request.url}: ${JSON.stringify(sanitisedError)}`,
      )
      throw sanitisedError
    })
  }

  protected buildUrl(path: string): string {
    return `${this.config.url}${path}`
  }

  /**
   * Returns enumeration constants and their human-readable descriptions
   *
   * @throws SanitisedError<ErrorResponse>
   */
  constants(): Promise<Constants> {
    const request = superagent.get(this.buildUrl('/constants'))

    return this.sendRequest(request, true).then(response => response.body)
  }

  /**
   * Retrieves a list of non-associations for given prisoner number.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  listNonAssociations(
    prisonerNumber: string,
    options?: {
      includeOpen?: true
      includeClosed?: false
      includeOtherPrisons?: boolean
      sortBy?: SortBy
      sortDirection?: SortDirection
    },
  ): Promise<NonAssociationsList<OpenNonAssociationsListItem>>

  listNonAssociations(
    prisonerNumber: string,
    options: {
      includeOpen: false
      includeClosed: true
      includeOtherPrisons?: boolean
      sortBy?: SortBy
      sortDirection?: SortDirection
    },
  ): Promise<NonAssociationsList<ClosedNonAssociationsListItem>>

  listNonAssociations(
    prisonerNumber: string,
    options: {
      includeOpen: false
      includeClosed?: false
      includeOtherPrisons?: boolean
      sortBy?: SortBy
      sortDirection?: SortDirection
    },
  ): Promise<never>

  listNonAssociations(
    prisonerNumber: string,
    options: {
      includeOpen?: true | boolean
      includeClosed: true | boolean
      includeOtherPrisons?: boolean
      sortBy?: SortBy
      sortDirection?: SortDirection
    },
  ): Promise<NonAssociationsList>

  listNonAssociations(
    prisonerNumber: string,
    {
      includeOpen = true,
      includeClosed = false,
      includeOtherPrisons = false,
      sortBy = 'WHEN_CREATED',
      sortDirection = 'DESC',
    }: {
      includeOpen?: boolean
      includeClosed?: boolean
      includeOtherPrisons?: boolean
      sortBy?: SortBy
      sortDirection?: SortDirection
    } = {},
  ): Promise<NonAssociationsList> {
    const request = superagent
      .get(this.buildUrl(`/prisoner/${encodeURIComponent(prisonerNumber)}/non-associations`))
      .query({
        includeOpen: includeOpen.toString(),
        includeClosed: includeClosed.toString(),
        includeOtherPrisons: includeOtherPrisons.toString(),
        sortBy,
        sortDirection,
      })

    return this.sendRequest(request, true).then(response => {
      const nonAssociationList: NonAssociationsList = response.body
      nonAssociationList.nonAssociations.forEach(nonAssociation => parseDates(nonAssociation))
      return nonAssociationList
    })
  }

  /**
   * Get non-associations between two or more prisoners by prisoner number.
   * Both people in the non-associations must be in the provided list.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  listNonAssociationsBetween(
    prisonerNumbers: string[],
    options?: {
      includeOpen?: true
      includeClosed?: false
    },
  ): Promise<OpenNonAssociation[]>

  listNonAssociationsBetween(
    prisonerNumbers: string[],
    options: {
      includeOpen: false
      includeClosed: true
    },
  ): Promise<ClosedNonAssociation[]>

  listNonAssociationsBetween(
    prisonerNumbers: string[],
    options: {
      includeOpen: false
      includeClosed: false
    },
  ): Promise<never[]>

  listNonAssociationsBetween(
    prisonerNumbers: string[],
    options: {
      includeOpen?: boolean
      includeClosed?: boolean
    },
  ): Promise<NonAssociation[]>

  listNonAssociationsBetween(
    prisonerNumbers: string[],
    {
      includeOpen = true,
      includeClosed = false,
    }: {
      includeOpen?: boolean
      includeClosed?: boolean
    } = {},
  ): Promise<NonAssociation[]> {
    const request = superagent
      .post(this.buildUrl('/non-associations/between'))
      .query({
        includeOpen: includeOpen.toString(),
        includeClosed: includeClosed.toString(),
      })
      .send(prisonerNumbers)

    return this.sendRequest(request).then(response => {
      const nonAssociations: NonAssociation[] = response.body
      return nonAssociations.map(nonAssociation => parseDates(nonAssociation))
    })
  }

  /**
   * Get non-associations involving any of the given prisoners.
   * Either person in the non-association must be in the provided list.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  listNonAssociationsInvolving(
    prisonerNumbers: string[],
    options?: {
      includeOpen?: true
      includeClosed?: false
    },
  ): Promise<OpenNonAssociation[]>

  listNonAssociationsInvolving(
    prisonerNumbers: string[],
    options: {
      includeOpen: false
      includeClosed: true
    },
  ): Promise<ClosedNonAssociation[]>

  listNonAssociationsInvolving(
    prisonerNumbers: string[],
    options: {
      includeOpen: false
      includeClosed: false
    },
  ): Promise<never[]>

  listNonAssociationsInvolving(
    prisonerNumbers: string[],
    options: {
      includeOpen?: boolean
      includeClosed?: boolean
    },
  ): Promise<NonAssociation[]>

  listNonAssociationsInvolving(
    prisonerNumbers: string[],
    {
      includeOpen = true,
      includeClosed = false,
    }: {
      includeOpen?: boolean
      includeClosed?: boolean
    } = {},
  ): Promise<NonAssociation[]> {
    const request = superagent
      .post(this.buildUrl('/non-associations/involving'))
      .query({
        includeOpen: includeOpen.toString(),
        includeClosed: includeClosed.toString(),
      })
      .send(prisonerNumbers)

    return this.sendRequest(request).then(response => {
      const nonAssociations: NonAssociation[] = response.body
      return nonAssociations.map(nonAssociation => parseDates(nonAssociation))
    })
  }

  /**
   * Retrieves ALL non-associations in pages.
   * This is rarely a useful endpoint due to the filters being very limited.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  pagedNonAssociations(
    options?: {
      includeOpen?: true
      includeClosed?: false
    } & SortedPageRequest,
  ): Promise<Page<OpenNonAssociation>>

  pagedNonAssociations(
    options: {
      includeOpen: false
      includeClosed: true
    } & SortedPageRequest,
  ): Promise<Page<ClosedNonAssociation>>

  pagedNonAssociations(
    options: {
      includeOpen: false
      includeClosed: false
    } & SortedPageRequest,
  ): Promise<Page<never>>

  pagedNonAssociations(
    options: {
      includeOpen?: boolean
      includeClosed?: boolean
    } & SortedPageRequest,
  ): Promise<Page<NonAssociation>>

  pagedNonAssociations({
    includeOpen = true,
    includeClosed = false,
    page,
    size,
    sort,
  }: {
    includeOpen?: boolean
    includeClosed?: boolean
  } & SortedPageRequest = {}): Promise<Page<NonAssociation>> {
    const query: Record<string, number | string | string[]> = {
      includeOpen: includeOpen.toString(),
      includeClosed: includeClosed.toString(),
    }
    if (typeof page !== 'undefined') {
      query.page = page
    }
    if (typeof size !== 'undefined') {
      query.size = size
    }
    if (typeof sort !== 'undefined') {
      query.sort = sort
    }
    const request = superagent.get(this.buildUrl('/non-associations')).query(query)

    return this.sendRequest(request, true).then(response => {
      const nonAssociations: Page<NonAssociation> = response.body
      nonAssociations.content.forEach(nonAssociation => parseDates(nonAssociation))
      return nonAssociations
    })
  }

  /**
   * Retrieve a non-association by ID.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  getNonAssociation(id: number): Promise<NonAssociation> {
    const request = superagent.get(this.buildUrl(`/non-associations/${encodeURIComponent(id)}`))

    return this.sendRequest(request, true).then(response => {
      const nonAssociation = response.body
      return parseDates(nonAssociation)
    })
  }

  /**
   * Create a new non-association.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  createNonAssociation(payload: CreateNonAssociationRequest): Promise<OpenNonAssociation> {
    const request = superagent.post(this.buildUrl('/non-associations')).send(payload)

    return this.sendRequest(request).then(response => {
      const nonAssociation: OpenNonAssociation = response.body
      return parseDates(nonAssociation)
    })
  }

  /**
   * Update an existing new non-association by ID.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  updateNonAssociation(id: number, payload: UpdateNonAssociationRequest): Promise<NonAssociation> {
    const request = superagent.patch(this.buildUrl(`/non-associations/${encodeURIComponent(id)}`)).send(payload)

    return this.sendRequest(request).then(response => {
      const nonAssociation: NonAssociation = response.body
      return parseDates(nonAssociation)
    })
  }

  /**
   * Close an open non-association by ID.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  closeNonAssociation(id: number, payload: CloseNonAssociationRequest): Promise<ClosedNonAssociation> {
    const request = superagent.put(this.buildUrl(`/non-associations/${encodeURIComponent(id)}/close`)).send(payload)

    return this.sendRequest(request).then(response => {
      const nonAssociation: ClosedNonAssociation = response.body
      return parseDates(nonAssociation)
    })
  }

  /**
   * Delete a non-association by ID.
   *
   * **Please note**: This is a special endpoint which should NOT be exposed to regular users,
   * they should instead close non-associations.
   *
   * Requires DELETE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  deleteNonAssociation(id: number, payload: DeleteNonAssociationRequest): Promise<null> {
    const request = superagent.post(this.buildUrl(`/non-associations/${encodeURIComponent(id)}/delete`)).send(payload)

    return this.sendRequest(request).then(() => null)
  }
}
