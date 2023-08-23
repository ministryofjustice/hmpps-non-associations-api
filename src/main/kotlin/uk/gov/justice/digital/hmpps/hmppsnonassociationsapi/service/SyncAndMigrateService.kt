package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.OpenNonAssociationAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpsertSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateToRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers

const val NO_CLOSURE_REASON_PROVIDED = "No closure reason provided"
const val NO_COMMENT_PROVIDED = "No comment provided"

@Service
@Transactional
class SyncAndMigrateService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(syncRequest: UpsertSyncRequest): NonAssociation {
    val prisonersToKeepApart = listOf(
      syncRequest.firstPrisonerNumber,
      syncRequest.secondPrisonerNumber,
    )
    val existingRecords =
      nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart, NonAssociationListInclusion.ALL)
    val latestClosedRecord = existingRecords.filter { na -> na.isClosed }.maxByOrNull { na -> na.whenUpdated }
    val recordToUpdate = existingRecords.firstOrNull { na -> na.isOpen } ?: latestClosedRecord

    return if (recordToUpdate != null) {
      val (firstPrisonerRoleUpdate, secondPrisonerRoleUpdate, reasonUpdate) = translateToRolesAndReason(syncRequest.firstPrisonerReason, syncRequest.secondPrisonerReason)

      with(recordToUpdate) {
        restrictionType = syncRequest.restrictionType.toRestrictionType()
        firstPrisonerRole = firstPrisonerRoleUpdate
        secondPrisonerRole = secondPrisonerRoleUpdate
        reason = reasonUpdate
        // TODO: can we have a better fall back message?
        comment = syncRequest.comment ?: NO_COMMENT_PROVIDED
        authorisedBy = syncRequest.authorisedBy
        isClosed = !syncRequest.active
        closedAt = if (!syncRequest.active) {
          syncRequest.expiryDate?.atStartOfDay()
        } else {
          null
        }
        if (syncRequest.active) {
          closedReason = null
          closedBy = null
        } else {
          if (closedReason == null) {
            // TODO: can we have a better message?
            closedReason = NO_CLOSURE_REASON_PROVIDED
          }
          if (closedBy == null) {
            // TODO: perhaps system user would be more appropriate here
            closedBy = syncRequest.authorisedBy
          }
        }
        toDto().also {
          log.info("Updated Non-association [${it.id}] between ${it.firstPrisonerNumber} and ${it.secondPrisonerNumber}")
          telemetryClient.trackEvent(
            "Sync (Update)",
            mapOf(
              "id" to it.id.toString(),
              "firstPrisonerNumber" to syncRequest.firstPrisonerNumber,
              "secondPrisonerNumber" to syncRequest.secondPrisonerNumber,
            ),
            null,
          )
        }
      }
    } else {
      nonAssociationsRepository.save(syncRequest.toNewEntity()).toDto().also {
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

  fun sync(deleteSyncRequest: DeleteSyncRequest) {
    val prisonersToKeepApart = listOf(
      deleteSyncRequest.firstPrisonerNumber,
      deleteSyncRequest.secondPrisonerNumber,
    )
    nonAssociationsRepository.deleteAll(
      nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart, NonAssociationListInclusion.ALL).also {
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

  fun migrate(migrateRequest: MigrateRequest): NonAssociation {
    if (migrateRequest.active) {
      val prisonersToKeepApart = listOf(
        migrateRequest.firstPrisonerNumber,
        migrateRequest.secondPrisonerNumber,
      )
      if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart).isNotEmpty()) {
        throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
      }
    }

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
