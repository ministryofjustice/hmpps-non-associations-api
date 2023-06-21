package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
data class ReasonType(
  @Id
  val code: String,
  val description: String,

  @CreatedDate
  val whenCreated: LocalDateTime = LocalDateTime.now(),
  @LastModifiedDate
  var whenUpdated: LocalDateTime = LocalDateTime.now(),
)
