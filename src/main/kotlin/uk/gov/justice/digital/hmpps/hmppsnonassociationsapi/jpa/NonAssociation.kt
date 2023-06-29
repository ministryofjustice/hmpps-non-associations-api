package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
class NonAssociation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val firstPrisonerNumber: String,
  var firstPrisonerReasonCode: String,

  val secondPrisonerNumber: String,
  var secondPrisonerReasonCode: String,

  // Details and restrictions
  var restrictionType: String,
  var comment: String = "",
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
