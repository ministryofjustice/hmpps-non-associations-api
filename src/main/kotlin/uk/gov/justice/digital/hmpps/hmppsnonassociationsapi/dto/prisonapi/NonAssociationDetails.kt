package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi

import java.time.LocalDateTime

data class NonAssociationDetails(
  val offenderNo: String,
  val firstName: String,
  val lastName: String,
  val agencyDescription: String,
  val assignedLivingUnitDescription: String,
  val nonAssociations: List<NonAssociation>,
  val assignedLivingUnitId: Long,
)

data class NonAssociation(
  val reasonCode: String,
  val reasonDescription: String,
  val typeCode: String,
  val typeDescription: String,
  val effectiveDate: LocalDateTime,
  val expiryDate: LocalDateTime?,
  val authorisedBy: String?,
  val comments: String?,
  val offenderNonAssociation: OffenderNonAssociation,
)

data class OffenderNonAssociation(
  val offenderNo: String,
  val firstName: String,
  val lastName: String,
  val reasonCode: String,
  val reasonDescription: String,
  val agencyDescription: String,
  val assignedLivingUnitDescription: String,
  val assignedLivingUnitId: Long,
)
