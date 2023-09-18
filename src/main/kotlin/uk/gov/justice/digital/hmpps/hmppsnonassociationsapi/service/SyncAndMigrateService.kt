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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.preventFutureDate
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateToRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType
import java.time.Clock
import java.time.LocalDate
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

  fun sync(syncRequest: UpsertSyncRequest): Pair<NonAssociationDomainEventType, NonAssociation> {
    log.info("Syncing $syncRequest")

    val recordToUpdate = if (syncRequest.id != null) {
      val existing =
        nonAssociationsRepository.findById(syncRequest.id).getOrNull() ?: throw NonAssociationNotFoundException(
          syncRequest.id,
        )
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
      Pair(NonAssociationDomainEventType.NON_ASSOCIATION_UPSERT, updateRecord(syncRequest, recordToUpdate).also { log.info("SYNC: Updating Non Association [$it]") })
    } else {
      Pair(
        NonAssociationDomainEventType.NON_ASSOCIATION_CREATED,
        nonAssociationsRepository.save(syncRequest.toNewEntity(clock).also { log.info("SYNC: Creating Non Association [$it]") }).toDto().also {
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
        },
      )
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
      updatedBy = syncRequest.lastModifiedByUsername ?: SYSTEM_USERNAME
      whenCreated = preventFutureDate(syncRequest.effectiveFromDate, LocalDate.now(clock)).atStartOfDay()

      if (syncRequest.isOpen(clock)) {
        closedReason = null
        closedBy = null
        closedAt = null
        whenUpdated = LocalDateTime.now(clock)
      } else {
        closedReason = NO_CLOSURE_REASON_PROVIDED
        closedBy = syncRequest.lastModifiedByUsername ?: SYSTEM_USERNAME
        closedAt = preventFutureDate(syncRequest.expiryDate, LocalDate.now(clock)).atStartOfDay()
        whenUpdated = preventFutureDate(syncRequest.expiryDate, LocalDate.now(clock)).atStartOfDay()
      }
      toDto().also {
        log.info("Updated Non-association [${it.id}] between ${it.firstPrisonerNumber} and ${it.secondPrisonerNumber}")
        telemetryClient.trackEvent(
          "Sync (Update)",
          mapOf(
            "id" to it.id.toString(),
            "firstPrisonerNumber" to it.firstPrisonerNumber,
            "secondPrisonerNumber" to it.secondPrisonerNumber,
          ),
          null,
        )
      }
    }
  }

  fun delete(deleteSyncRequest: DeleteSyncRequest): List<NonAssociation> {
    log.info("Deleting $deleteSyncRequest")
    val nonAssociations = nonAssociationsRepository.findAnyBetweenPrisonerNumbers(
      listOf(
        deleteSyncRequest.firstPrisonerNumber,
        deleteSyncRequest.secondPrisonerNumber,
      ),
      NonAssociationListInclusion.ALL,
    )
    nonAssociationsRepository.deleteAll(
      nonAssociations
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
    return nonAssociations.map { it.toDto() }
  }

  fun delete(id: Long): NonAssociation {
    log.info("Deleting non-association [ID=$id]")
    val naToDelete = nonAssociationsRepository.findById(id).getOrNull() ?: throw NonAssociationNotFoundException(id)
    nonAssociationsRepository.delete(naToDelete)
    log.info("Deleted non-association [ID=$id between ${naToDelete.firstPrisonerNumber} and ${naToDelete.secondPrisonerNumber}")
    telemetryClient.trackEvent(
      "Delete Sync",
      mapOf(
        "id" to id.toString(),
        "firstPrisonerNumber" to naToDelete.firstPrisonerNumber,
        "secondPrisonerNumber" to naToDelete.secondPrisonerNumber,
      ),
      null,
    )
    return naToDelete.toDto()
  }

  fun migrate(migrateRequest: UpsertSyncRequest): NonAssociation {
    log.info("Migrating $migrateRequest")
    if (migrateRequest.isOpen(clock)) {
      val prisonersToKeepApart = listOf(
        migrateRequest.firstPrisonerNumber,
        migrateRequest.secondPrisonerNumber,
      )
      if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart).isNotEmpty()) {
        throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
      }
    }

    return nonAssociationsRepository.save(migrateRequest.toNewEntity(clock).also { log.info("MIGRATE: Creating Non Association [$it]") }).toDto().also {
      log.info("Migrated Non-association [$migrateRequest]")
      telemetryClient.trackEvent(
        "Migrate",
        mapOf("id" to it.id.toString()),
        null,
      )
    }
  }
}
