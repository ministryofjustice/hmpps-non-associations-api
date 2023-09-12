package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa

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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
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
  var firstPrisonerRole: Role,

  var secondPrisonerNumber: String,
  @Enumerated(value = EnumType.STRING)
  var secondPrisonerRole: Role,

  // Details and restrictions
  @Enumerated(value = EnumType.STRING)
  var reason: Reason,
  @Enumerated(value = EnumType.STRING)
  var restrictionType: RestrictionType,
  var comment: String,

  var authorisedBy: String? = null, // This is stored for legacy reasons - the data is not returned in the API except on the legacy endpoints

  // Non-associations can be closed (with details of who/why/when)
  var isClosed: Boolean = false,
  var closedBy: String? = null,
  var closedReason: String? = null,
  var closedAt: LocalDateTime? = null,

  @CreatedDate
  var whenCreated: LocalDateTime = LocalDateTime.now(),
  @LastModifiedDate
  var whenUpdated: LocalDateTime = LocalDateTime.now(),

  var updatedBy: String,
) {
  fun close(closedBy: String, closedReason: String, closedAt: LocalDateTime) {
    this.isClosed = true
    this.closedBy = closedBy
    this.closedReason = closedReason
    this.closedAt = closedAt
    this.updatedBy = closedBy
  }

  val isOpen: Boolean
    get() = !isClosed

  fun toDto(): NonAssociationDTO {
    return NonAssociationDTO(
      id = id!!,
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      firstPrisonerRoleDescription = firstPrisonerRole.description,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      secondPrisonerRoleDescription = secondPrisonerRole.description,
      reason = reason,
      reasonDescription = reason.description,
      restrictionType = restrictionType,
      restrictionTypeDescription = restrictionType.description,
      comment = comment,
      whenCreated = whenCreated,
      whenUpdated = whenUpdated,
      updatedBy = updatedBy,
      isClosed = isClosed,
      closedReason = closedReason,
      closedBy = closedBy,
      closedAt = closedAt,
    )
  }

  fun updatePrisonerNumber(prisonerNumber: String, primary: Boolean): NonAssociation {
    if (primary) {
      firstPrisonerNumber = prisonerNumber
    } else {
      secondPrisonerNumber = prisonerNumber
    }
    return this
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

  override fun toString(): String {
    return "NonAssociation(id=$id, firstPrisonerNumber='$firstPrisonerNumber', firstPrisonerRole=$firstPrisonerRole, secondPrisonerNumber='$secondPrisonerNumber', secondPrisonerRole=$secondPrisonerRole, reason=$reason, restrictionType=$restrictionType, comment='$comment', authorisedBy=$authorisedBy, isClosed=$isClosed, closedBy=$closedBy, closedReason=$closedReason, closedAt=$closedAt, whenCreated=$whenCreated, whenUpdated=$whenUpdated, updatedBy='$updatedBy')"
  }
}
