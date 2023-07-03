package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
class NonAssociation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val firstPrisonerNumber: String,
  @Enumerated(value = EnumType.STRING)
  @Column(name = "first_prisoner_reason_code")
  var firstPrisonerReason: NonAssociationReason,

  val secondPrisonerNumber: String,
  @Enumerated(value = EnumType.STRING)
  @Column(name = "second_prisoner_reason_code")
  var secondPrisonerReason: NonAssociationReason,

  // Details and restrictions
  @Enumerated(value = EnumType.STRING)
  @Column(name = "restriction_type_code")
  var restrictionType: NonAssociationRestrictionType,
  var comment: String = "",
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
)
