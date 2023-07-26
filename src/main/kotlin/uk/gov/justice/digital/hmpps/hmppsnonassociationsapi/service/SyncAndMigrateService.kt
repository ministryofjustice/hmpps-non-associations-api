package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpdateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository

@Service
@Transactional
class SyncAndMigrateService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun syncCreate(createSyncRequest: CreateSyncRequest): NonAssociation {
    return nonAssociationsRepository.save(createSyncRequest.toNewEntity()).toDto().also {
      log.info("Created Non-association [$createSyncRequest]")
      telemetryClient.trackEvent(
        "Sync",
        mapOf("id" to it.id.toString()),
        null,
      )
    }
  }

  fun syncUpdate(updateSyncRequest: UpdateSyncRequest): NonAssociation {
    val nonAssociation = nonAssociationsRepository.findById(updateSyncRequest.id)
      .orElseThrow { EntityNotFoundException(updateSyncRequest.id.toString()) }

    val na = with(nonAssociation) {
      restrictionType = updateSyncRequest.restrictionType.toRestrictionType()
      firstPrisonerRole = updateSyncRequest.firstPrisonerReason.toRole()
      secondPrisonerRole = updateSyncRequest.secondPrisonerReason.toRole()
      // TODO: can we have a better fall back message?
      comment = updateSyncRequest.comment ?: "No comment provided"
      authorisedBy = updateSyncRequest.authorisedBy
      isClosed = !updateSyncRequest.active
      closedAt = if (!updateSyncRequest.active) { updateSyncRequest.expiryDate?.atStartOfDay() } else { null }
      if (updateSyncRequest.active) {
        closedReason = null
        closedBy = null
      } else {
        if (closedReason == null) {
          // TODO: can we have a better message?
          closedReason = "UNDEFINED"
        }
        if (closedBy == null) {
          // TODO: perhaps system user would be more appropriate here
          closedBy = updateSyncRequest.authorisedBy
        }
      }
      toDto()
    }

    log.info("Updated Non-association [${updateSyncRequest.id}]")
    telemetryClient.trackEvent(
      "Update Sync",
      mapOf("id" to updateSyncRequest.id.toString()),
      null,
    )

    return na
  }

  fun syncDelete(id: String) {
    nonAssociationsRepository.deleteById(id.toLong())
    log.info("Deleted Non-association [$id]")
    telemetryClient.trackEvent(
      "Delete Sync",
      mapOf("id" to id),
      null,
    )
  }

  fun migrate(migrateRequest: MigrateRequest): NonAssociation {
    return nonAssociationsRepository.save(migrateRequest.toNewEntity()).toDto().also {
      log.info("Migrated Non-association [$migrateRequest]")
      telemetryClient.trackEvent(
        "Migrate",
        mapOf("id" to it.id.toString()),
        null,
      )
    }
  }
}
