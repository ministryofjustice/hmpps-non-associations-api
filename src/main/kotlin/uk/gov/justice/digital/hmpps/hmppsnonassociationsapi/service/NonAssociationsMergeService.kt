package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class NonAssociationsMergeService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun mergePrisonerNumbers(
    oldPrisonerNumber: String,
    newPrisonerNumber: String,
  ): Map<MergeResult, List<NonAssociation>> {
    log.info("Replacing prisoner number $oldPrisonerNumber to $newPrisonerNumber")

    val updatedRecords = mutableListOf<Pair<MergeResult, NonAssociation>>()

    nonAssociationsRepository.findAllByPrisonerNumber(oldPrisonerNumber)
      .forEach { nonAssociation ->

        log.debug("Looking at record {}", nonAssociation)
        if (nonAssociation.firstPrisonerNumber == oldPrisonerNumber) {
          nonAssociationsRepository.findByFirstPrisonerNumberAndSecondPrisonerNumber(
            firstPrisonerNumber = newPrisonerNumber,
            secondPrisonerNumber = nonAssociation.secondPrisonerNumber,
          ).let { duplicateRecord ->
            merge(
              nonAssociation,
              duplicateRecord,
              newPrisonerNumber,
              nonAssociation.secondPrisonerNumber,
              true,
            ).let { updatedRecord ->
              updatedRecords.add(updatedRecord)
            }
          }
        }

        if (nonAssociation.secondPrisonerNumber == oldPrisonerNumber) {
          nonAssociationsRepository.findByFirstPrisonerNumberAndSecondPrisonerNumber(
            firstPrisonerNumber = nonAssociation.firstPrisonerNumber,
            secondPrisonerNumber = newPrisonerNumber,
          ).let { duplicateRecord ->
            merge(
              nonAssociation,
              duplicateRecord,
              newPrisonerNumber,
              nonAssociation.firstPrisonerNumber,
              false,
            ).let { updatedRecord ->
              updatedRecords.add(updatedRecord)
            }
          }
        }
      }

    val results = updatedRecords.groupBy({ (result, _) -> result }, { (_, nonAssociation) -> nonAssociation })

    telemetryClient.trackEvent(
      "Merge",
      mapOf(
        "NOMS-MERGE-FROM" to oldPrisonerNumber,
        "NOMS-MERGE-TO" to newPrisonerNumber,
        "MERGED-RECORDS" to results[MergeResult.MERGED]?.size.toString(),
        "CLOSED-RECORDS" to results[MergeResult.CLOSED]?.size.toString(),
        "DELETED-RECORDS" to results[MergeResult.DELETED]?.size.toString(),
      ),
      null,
    )
    log.info("Merge results: {}", results)
    return results
  }

  private fun merge(
    nonAssociation: NonAssociation,
    duplicateRecord: NonAssociation?,
    newPrisonerNumber: String,
    otherPrisonerNumber: String,
    primary: Boolean,
  ): Pair<MergeResult, NonAssociation> {
    nonAssociation.updatePrisonerNumber(newPrisonerNumber, primary)

    if (duplicateRecord != null) {
      nonAssociation.close(SYSTEM_USERNAME, "MERGE", LocalDateTime.now(clock))
      log.info("Closed non-association record $nonAssociation - Duplicate")
      return Pair(MergeResult.CLOSED, nonAssociation)
    } else if (newPrisonerNumber == otherPrisonerNumber) {
      log.info("Deleting non-association record $nonAssociation - same prisoner number")
      nonAssociationsRepository.delete(nonAssociation)
      return Pair(MergeResult.DELETED, nonAssociation)
    }

    log.info("Merged non-association record $nonAssociation")
    return Pair(MergeResult.MERGED, nonAssociation)
  }
}

enum class MergeResult {
  MERGED,
  CLOSED,
  DELETED,
}
