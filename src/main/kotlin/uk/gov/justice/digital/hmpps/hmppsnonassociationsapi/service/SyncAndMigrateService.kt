package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.SyncRequest
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

  fun sync(syncRequest: SyncRequest): NonAssociation {
    return nonAssociationsRepository.save(syncRequest.toNewEntity()).toDto().also {
      log.info("Sync'd' Non-association [$syncRequest]")
      telemetryClient.trackEvent(
        "Sync",
        mapOf("firstPrisonerNumber" to it.firstPrisonerNumber, "secondPrisonerNumber" to it.secondPrisonerNumber),
        null,
      )
    }
  }

  fun migrate(migrateRequest: MigrateRequest): NonAssociation {
    return nonAssociationsRepository.save(migrateRequest.toNewEntity()).toDto().also {
      log.info("Migrated Non-association [$migrateRequest]")
      telemetryClient.trackEvent(
        "Migrate",
        mapOf("firstPrisonerNumber" to it.firstPrisonerNumber, "secondPrisonerNumber" to it.secondPrisonerNumber),
        null,
      )
    }
  }
}
