package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO

@Entity
@EntityListeners(AuditingEntityListener::class)
class NonAssociation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  var firstPrisonerNumber: String,
  @Enumerated(value = EnumType.STRING)
  @Column(name = "first_prisoner_reason_code")
  var firstPrisonerReason: NonAssociationReason,

  var secondPrisonerNumber: String,
  @Enumerated(value = EnumType.STRING)
  @Column(name = "second_prisoner_reason_code")
  var secondPrisonerReason: NonAssociationReason,

  // Details and restrictions
  @Enumerated(value = EnumType.STRING)
  @Column(name = "restriction_type_code")
  var restrictionType: NonAssociationRestrictionType,
  var comment: String,
  var authorisedBy: String? = null,
  var incidentReportNumber: String? = null,

  // Non-associations can be closed (with details of who/why/when)
  var isClosed: Boolean = false,
  var closedBy: String? = null,
  var closedReason: String? = null,
  var closedAt: LocalDateTime? = null,

  @CreatedDate
  var whenCreated: LocalDateTime = LocalDateTime.now(),
  @LastModifiedDate
  var whenUpdated: LocalDateTime = LocalDateTime.now(),
) {
  fun close(closedBy: String, closedReason: String, closedAt: LocalDateTime) {
    this.isClosed = true
    this.closedBy = closedBy
    this.closedReason = closedReason
    this.closedAt = closedAt
  }

  fun isOpen() = !isClosed

  fun toDto(): NonAssociationDTO {
    return NonAssociationDTO(
      id = id!!,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReason = firstPrisonerReason,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReason = secondPrisonerReason,
      restrictionType = restrictionType,
      comment = comment,
      // TODO: Do we need to do anything special with this?
      //       This field being optional in NOMIS/Prison API
      //       It may be one of the things we make mandatory after migration?
      authorisedBy = authorisedBy ?: "",
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as NonAssociation

    if (firstPrisonerNumber != other.firstPrisonerNumber) return false
    return secondPrisonerNumber == other.secondPrisonerNumber
  }

  override fun hashCode(): Int {
    var result = firstPrisonerNumber.hashCode()
    result = 31 * result + secondPrisonerNumber.hashCode()
    return result
  }
}
