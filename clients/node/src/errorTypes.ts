/**
 * Structure representing an error response from the api, wrapped in SanitisedError.
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse class
 */
export interface ErrorResponse {
  status: number

  userMessage: string

  developerMessage: string

  errorCode?: ErrorCode

  moreInfo?: string
}

/**
 * Used to determine if an api error was of type `ErrorResponse` to use relevant properties
 */
export function isErrorResponse(obj: unknown): obj is ErrorResponse {
  return Boolean(
    obj &&
      typeof obj === 'object' &&
      'status' in obj &&
      typeof obj.status === 'number' &&
      'userMessage' in obj &&
      'developerMessage' in obj,
  )
}

/**
 * Unique codes to discriminate errors returned from the api.
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorCode enumeration
 */
export enum ErrorCode {
  NonAssociationAlreadyClosed = 100,
  OpenNonAssociationAlreadyExist = 101,
  ValidationFailure = 102,
  UserInContextMissing = 401,
}
