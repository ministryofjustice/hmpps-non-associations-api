import { ApiConfig, asSystem, RestClient } from '@ministryofjustice/hmpps-rest-client'

import type { SortBy, SortDirection } from './constants'
import type { Page, PageRequest } from './paginationTypes'
import type {
  CreateNonAssociationRequest,
  UpdateNonAssociationRequest,
  CloseNonAssociationRequest,
  DeleteNonAssociationRequest,
  ReopenNonAssociationRequest,
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
export class NonAssociationsApi extends RestClient {
  static readonly name = 'HMPPS Non-associations API'

  constructor(
    /**
     * Provide a system token with necessary roles, not a user token
     * READ_NON_ASSOCIATIONS and optionally WRITE_NON_ASSOCIATIONS or DELETE_NON_ASSOCIATIONS
     * This should be authenticated for the acting username
     */
    systemToken: string,

    /**
     * API configuration standard in DPS front-end apps
     */
    config: ApiConfig,

    /**
     * Logger such as standard library’s `console` or `bunyan` instance
     */
    logger: Logger,
  ) {
    super(NonAssociationsApi.name, config, logger, {
      async getToken(): Promise<string> {
        return systemToken
      },
    })
  }

  /**
   * Returns enumeration constants and their human-readable descriptions
   *
   * @throws SanitisedError<ErrorResponse>
   */
  constants(): Promise<Constants> {
    return this.get<Constants>(
      {
        path: '/constants',
      },
      asSystem(),
    )
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

  async listNonAssociations(
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
    const nonAssociationList = await this.get<NonAssociationsList>(
      {
        path: `/prisoner/${encodeURIComponent(prisonerNumber)}/non-associations`,
        query: {
          includeOpen: includeOpen.toString(),
          includeClosed: includeClosed.toString(),
          includeOtherPrisons: includeOtherPrisons.toString(),
          sortBy,
          sortDirection,
        },
      },
      asSystem(),
    )
    nonAssociationList.nonAssociations.forEach(nonAssociation => parseDates(nonAssociation))
    return nonAssociationList
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

  async listNonAssociationsBetween(
    prisonerNumbers: string[],
    {
      includeOpen = true,
      includeClosed = false,
    }: {
      includeOpen?: boolean
      includeClosed?: boolean
    } = {},
  ): Promise<NonAssociation[]> {
    const nonAssociations = await this.post<NonAssociation[]>(
      {
        path: '/non-associations/between',
        query: {
          includeOpen: includeOpen.toString(),
          includeClosed: includeClosed.toString(),
        },
        data: prisonerNumbers,
      },
      asSystem(),
    )
    return nonAssociations.map(nonAssociation => parseDates(nonAssociation))
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

  async listNonAssociationsInvolving(
    prisonerNumbers: string[],
    {
      includeOpen = true,
      includeClosed = false,
    }: {
      includeOpen?: boolean
      includeClosed?: boolean
    } = {},
  ): Promise<NonAssociation[]> {
    const nonAssociations = await this.post<NonAssociation[]>(
      {
        path: '/non-associations/involving',
        query: {
          includeOpen: includeOpen.toString(),
          includeClosed: includeClosed.toString(),
        },
        data: prisonerNumbers,
      },
      asSystem(),
    )
    return nonAssociations.map(nonAssociation => parseDates(nonAssociation))
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

  async pagedNonAssociations({
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
    const nonAssociations = await this.get<Page<NonAssociation>>(
      {
        path: '/non-associations',
        query,
      },
      asSystem(),
    )
    nonAssociations.content.forEach(nonAssociation => parseDates(nonAssociation))
    return nonAssociations
  }

  /**
   * Retrieve a non-association by ID.
   *
   * Requires READ_NON_ASSOCIATIONS role.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  async getNonAssociation(id: number): Promise<NonAssociation> {
    const nonAssociation = await this.get<NonAssociation>(
      {
        path: `/non-associations/${encodeURIComponent(id)}`,
      },
      asSystem(),
    )
    return parseDates(nonAssociation)
  }

  /**
   * Create a new non-association.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  async createNonAssociation(payload: CreateNonAssociationRequest): Promise<OpenNonAssociation> {
    const nonAssociation = await this.post<OpenNonAssociation>(
      {
        path: '/non-associations',
        data: payload as unknown as Record<string, undefined>,
      },
      asSystem(),
    )
    return parseDates(nonAssociation)
  }

  /**
   * Update an existing new non-association by ID.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  async updateNonAssociation(id: number, payload: UpdateNonAssociationRequest): Promise<NonAssociation> {
    const nonAssociation = await this.patch<NonAssociation>(
      {
        path: `/non-associations/${encodeURIComponent(id)}`,
        data: payload as unknown as Record<string, undefined>,
      },
      asSystem(),
    )
    return parseDates(nonAssociation)
  }

  /**
   * Close an open non-association by ID.
   *
   * Requires WRITE_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  async closeNonAssociation(id: number, payload: CloseNonAssociationRequest): Promise<ClosedNonAssociation> {
    const nonAssociation = await this.put<ClosedNonAssociation>(
      {
        path: `/non-associations/${encodeURIComponent(id)}/close`,
        data: payload as unknown as Record<string, undefined>,
      },
      asSystem(),
    )
    return parseDates(nonAssociation)
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
  async deleteNonAssociation(id: number, payload: DeleteNonAssociationRequest): Promise<null> {
    await this.post(
      {
        path: `/non-associations/${encodeURIComponent(id)}/delete`,
        data: payload as unknown as Record<string, undefined>,
      },
      asSystem(),
    )
    return null
  }

  /**
   * Reopen a closed non-association by ID.
   *
   * **Please note**: This is a special endpoint which should NOT be exposed to regular users,
   * they should instead open new non-associations.
   *
   * Requires REOPEN_NON_ASSOCIATIONS role with write scope.
   *
   * @throws SanitisedError<ErrorResponse>
   */
  async reopenNonAssociation(id: number, payload: ReopenNonAssociationRequest): Promise<OpenNonAssociation> {
    const nonAssociation = await this.put<OpenNonAssociation>(
      {
        path: `/non-associations/${encodeURIComponent(id)}/reopen`,
        data: payload as unknown as Record<string, undefined>,
      },
      asSystem(),
    )
    return parseDates(nonAssociation)
  }
}
