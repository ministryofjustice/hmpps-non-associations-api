export const roleOptions = {
  VICTIM: 'Victim',
  PERPETRATOR: 'Perpetrator',
  NOT_RELEVANT: 'Not relevant',
  UNKNOWN: 'Unknown',
} as const
export type Role = typeof roleOptions

export const reasonOptions = {
  BULLYING: 'Bullying',
  GANG_RELATED: 'Gang related',
  ORGANISED_CRIME: 'Organised crime',
  LEGAL_REQUEST: 'Police or legal request',
  THREAT: 'Threat',
  VIOLENCE: 'Violence',
  OTHER: 'Other',
} as const
export type Reason = typeof reasonOptions

export const restrictionTypeOptions = {
  CELL: 'Cell only',
  LANDING: 'Cell and landing',
  WING: 'Cell, landing and wing',
} as const
export type RestrictionType = typeof restrictionTypeOptions

export const sortByOptions = [
  'WHEN_CREATED',
  'WHEN_UPDATED',
  'WHEN_CLOSED',
  'LAST_NAME',
  'FIRST_NAME',
  'PRISONER_NUMBER',
  'PRISON_ID',
  'PRISON_NAME',
  'CELL_LOCATION',
] as const
export type SortBy = (typeof sortByOptions)[number]

export const sortDirectionOptions = ['ASC', 'DESC'] as const
export type SortDirection = (typeof sortDirectionOptions)[number]

/**
 * Known system usernames that might appear in authorisedBy, updatedBy and closedBy fields
 */
export const systemUsers: readonly string[] = [
  // https://github.com/ministryofjustice/hmpps-non-associations-api/blob/04bf15fd1a7d659abe785749fbedda9f13627fba/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/HmppsNonAssociationsApi.kt#L9
  'NON_ASSOCIATIONS_API',
  'PRISONER_MANAGER_API',
  'hmpps-prisoner-from-nomis-migration-non-associations',
  'hmpps-prisoner-from-nomis-migration-non-associations-1',
  'hmpps-prisoner-to-nomis-update-non-associations',
  'hmpps-prisoner-to-nomis-update-non-associations-1',
]
