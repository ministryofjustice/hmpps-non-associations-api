package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationNotFoundException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.OpenNonAssociationAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpsertSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateToRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import java.time.Clock
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

const val NO_CLOSURE_REASON_PROVIDED = "No closure reason provided"
const val NO_COMMENT_PROVIDED = "No comment provided"

@Service
@Transactional
class SyncAndMigrateService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(syncRequest: UpsertSyncRequest): NonAssociation {
    val recordToUpdate = if (syncRequest.id != null) {
      val existing = nonAssociationsRepository.findById(syncRequest.id).getOrNull() ?: throw NonAssociationNotFoundException(syncRequest.id)
      val prisonersToKeepApart = listOf(
        existing.firstPrisonerNumber,
        existing.secondPrisonerNumber,
      )
      val existingOpenRecords = nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart)
      if (existingOpenRecords.isNotEmpty() && existingOpenRecords[0].id != existing.id) {
        throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
      }
      existing
    } else {
      val existingRecords =
        nonAssociationsRepository.findAnyBetweenPrisonerNumbers(
          listOf(
            syncRequest.firstPrisonerNumber,
            syncRequest.secondPrisonerNumber,
          ),
          NonAssociationListInclusion.ALL,
        )
      val latestClosedRecord = existingRecords.filter { na -> na.isClosed }.maxByOrNull { na -> na.whenUpdated }
      existingRecords.firstOrNull { na -> na.isOpen } ?: latestClosedRecord
    }
    return if (recordToUpdate != null) {
      updateRecord(syncRequest, recordToUpdate)
    } else {
      nonAssociationsRepository.save(syncRequest.toNewEntity(clock)).toDto().also {
        log.info("Created Non-association [${it.id}] between ${it.firstPrisonerNumber} and ${it.secondPrisonerNumber}")
        telemetryClient.trackEvent(
          "Sync (Create)",
          mapOf(
            "id" to it.id.toString(),
            "firstPrisonerNumber" to syncRequest.firstPrisonerNumber,
            "secondPrisonerNumber" to syncRequest.secondPrisonerNumber,
          ),
          null,
        )
      }
    }
  }

  private fun updateRecord(
    syncRequest: UpsertSyncRequest,
    recordToUpdate: uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation,
  ): NonAssociation {
    val (firstPrisonerRoleUpdate, secondPrisonerRoleUpdate, reasonUpdate) = translateToRolesAndReason(
      syncRequest.firstPrisonerReason,
      syncRequest.secondPrisonerReason,
    )

    return with(recordToUpdate) {
      restrictionType = syncRequest.restrictionType.toRestrictionType()
      firstPrisonerRole = firstPrisonerRoleUpdate
      secondPrisonerRole = secondPrisonerRoleUpdate
      reason = reasonUpdate
      comment = syncRequest.comment ?: NO_COMMENT_PROVIDED
      authorisedBy = syncRequest.authorisedBy
      isClosed = syncRequest.isClosed(clock)

      if (syncRequest.isOpen(clock)) {
        closedReason = null
        closedBy = null
        closedAt = null
      } else {
        closedReason = NO_CLOSURE_REASON_PROVIDED
        closedBy = syncRequest.authorisedBy ?: SYSTEM_USERNAME
        closedAt = syncRequest.expiryDate?.atStartOfDay() ?: LocalDateTime.now(clock)
      }
      toDto().also {
        log.info("Updated Non-association [${it.id}] between ${it.firstPrisonerNumber} and ${it.secondPrisonerNumber}")
        telemetryClient.trackEvent(
          "Sync (Update)",
          mapOf(
            "id" to it.id.toString(),
            "firstPrisonerNumber" to recordToUpdate.firstPrisonerNumber,
            "secondPrisonerNumber" to recordToUpdate.secondPrisonerNumber,
          ),
          null,
        )
      }
    }
  }

  fun sync(deleteSyncRequest: DeleteSyncRequest) {
    val prisonersToKeepApart = listOf(
      deleteSyncRequest.firstPrisonerNumber,
      deleteSyncRequest.secondPrisonerNumber,
    )
    nonAssociationsRepository.deleteAll(
      nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart, NonAssociationListInclusion.ALL)
        .also {
          log.info("Deleted ${it.size} non-associations between ${deleteSyncRequest.firstPrisonerNumber} and ${deleteSyncRequest.secondPrisonerNumber}")
          telemetryClient.trackEvent(
            "Delete Sync",
            mapOf(
              "firstPrisonerNumber" to deleteSyncRequest.firstPrisonerNumber,
              "secondPrisonerNumber" to deleteSyncRequest.secondPrisonerNumber,
            ),
            null,
          )
        },
    )
  }

  fun migrate(migrateRequest: UpsertSyncRequest): NonAssociation {
    if (migrateRequest.isOpen(clock)) {
      val prisonersToKeepApart = listOf(
        migrateRequest.firstPrisonerNumber,
        migrateRequest.secondPrisonerNumber,
      )
      if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart).isNotEmpty()) {
        throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
      }
    }

    return nonAssociationsRepository.save(migrateRequest.toNewEntity(clock)).toDto().also {
      log.info("Migrated Non-association [$migrateRequest]")
      telemetryClient.trackEvent(
        "Migrate",
        mapOf("id" to it.id.toString()),
        null,
      )
    }
  }
}
