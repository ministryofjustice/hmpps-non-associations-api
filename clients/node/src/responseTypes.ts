import type { Reason, RestrictionType, Role } from './constants'
import type { ObjectWithDates } from './parseDates'

interface EnumerationItem {
  code: string
  description: string
}
export interface Constants extends Record<'roles' | 'reasons' | 'restrictionTypes', EnumerationItem[]> {}

interface BaseNonAssociationsListItem extends ObjectWithDates {
  id: number
  role: keyof Role
  roleDescription: Role[keyof Role]
  reason: keyof Reason
  reasonDescription: Reason[keyof Reason]
  restrictionType: keyof RestrictionType
  restrictionTypeDescription: RestrictionType[keyof RestrictionType]
  comment: string
  authorisedBy: string
  updatedBy: string
  whenCreated: Date
  whenUpdated: Date
  otherPrisonerDetails: {
    prisonerNumber: string
    role: keyof Role
    roleDescription: Role[keyof Role]
    firstName: string
    lastName: string
    prisonId: string
    prisonName: string
    cellLocation?: string
  }
}

export interface OpenNonAssociationsListItem extends BaseNonAssociationsListItem {
  isClosed: false
  closedBy: null
  closedReason: null
  closedAt: null
}

export interface ClosedNonAssociationsListItem extends BaseNonAssociationsListItem {
  isClosed: true
  closedBy: string
  closedReason: string
  closedAt: Date
}

/**
 * List of non-associations for a particular prisoner
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/PrisonerNonAssociations.kt#L8-L31
 */
export interface NonAssociationsList<
  Item extends BaseNonAssociationsListItem = OpenNonAssociationsListItem | ClosedNonAssociationsListItem,
> {
  prisonerNumber: string
  firstName: string
  lastName: string
  prisonId: string
  prisonName: string
  cellLocation?: string
  openCount: number
  closedCount: number
  nonAssociations: Item[]
}

interface BaseNonAssociation extends ObjectWithDates {
  id: number
  firstPrisonerNumber: string
  firstPrisonerRole: keyof Role
  firstPrisonerRoleDescription: Role[keyof Role]
  secondPrisonerNumber: string
  secondPrisonerRole: keyof Role
  secondPrisonerRoleDescription: Role[keyof Role]
  reason: keyof Reason
  reasonDescription: Reason[keyof Reason]
  restrictionType: keyof RestrictionType
  restrictionTypeDescription: RestrictionType[keyof RestrictionType]
  comment: string
  authorisedBy: string
  updatedBy: string
  whenCreated: Date
  whenUpdated: Date
}

export interface OpenNonAssociation extends BaseNonAssociation {
  isClosed: false
  closedBy: null
  closedReason: null
  closedAt: null
}

export interface ClosedNonAssociation extends BaseNonAssociation {
  isClosed: true
  closedBy: string
  closedReason: string
  closedAt: Date
}

/**
 * Non-association between two prisoners
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/NonAssociation.kt#L10-L61
 */
export type NonAssociation = OpenNonAssociation | ClosedNonAssociation

/**
 * Request payload for creating a new non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/NonAssociation.kt#L63-L87
 */
export interface CreateNonAssociationRequest {
  firstPrisonerNumber: string
  firstPrisonerRole: keyof Role
  secondPrisonerNumber: string
  secondPrisonerRole: keyof Role
  reason: keyof Reason
  restrictionType: keyof RestrictionType
  comment: string
}

/**
 * Request payload for updating an existing non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/NonAssociation.kt#L105-L124
 */
export interface UpdateNonAssociationRequest {
  firstPrisonerRole?: keyof Role
  secondPrisonerRole?: keyof Role
  reason?: keyof Reason
  restrictionType?: keyof RestrictionType
  comment?: string
}

/**
 * Request payload for closing an existing open non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/NonAssociation.kt#L125-L138
 */
export interface CloseNonAssociationRequest {
  closedReason: string
  closedAt?: Date
  closedBy?: string
}

/**
 * Request payload for deleting an existing non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest class
 * https://github.com/ministryofjustice/hmpps-non-associations-api/blob/f6002aa1da50b8c4ccd3613e970327d5c67c44ae/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsnonassociationsapi/dto/NonAssociation.kt#L140-L151
 */
export interface DeleteNonAssociationRequest {
  deletionReason: string
  staffUserNameRequestingDeletion: string
}
