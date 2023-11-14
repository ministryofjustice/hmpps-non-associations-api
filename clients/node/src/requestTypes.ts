import type { Reason, RestrictionType, Role } from './constants'

/**
 * Request payload for creating a new non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest class
 * see https://github.com/ministryofjustice/hmpps-non-associations-api
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
 * see https://github.com/ministryofjustice/hmpps-non-associations-api
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
 * see https://github.com/ministryofjustice/hmpps-non-associations-api
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
 * see https://github.com/ministryofjustice/hmpps-non-associations-api
 */
export interface DeleteNonAssociationRequest {
  deletionReason: string
  staffUserNameRequestingDeletion: string
}

/**
 * Request payload for re-opening an existing closed non-association
 *
 * Defined in uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.ReopenNonAssociationRequest class
 * see https://github.com/ministryofjustice/hmpps-non-associations-api
 */
export interface ReopenNonAssociationRequest {
  reopenReason: string
  reopenedAt?: Date
  reopenedBy?: string
}
