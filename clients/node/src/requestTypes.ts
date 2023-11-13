import type { Reason, RestrictionType, Role } from './constants'

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
