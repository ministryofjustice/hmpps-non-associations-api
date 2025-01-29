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

  /**
   * Replaces an old prisoner number for a new one in all non-associations,
   * closing newly-duplicate records and deleting non-associations with the same prisoner number on both sides.
   */
  fun replacePrisonerNumber(
    oldPrisonerNumber: String,
    newPrisonerNumber: String,
  ): Map<MergeResult, List<NonAssociation>> =
    replacePrisonerNumberInDateRange(oldPrisonerNumber, newPrisonerNumber, null, null)

  /**
   * Replaces an old prisoner number for a new one in non-associations created within given date range (inclusive),
   * closing newly-duplicate records and deleting non-associations with the same prisoner number on both sides.
   */
  fun replacePrisonerNumberInDateRange(
    oldPrisonerNumber: String,
    newPrisonerNumber: String,
    since: LocalDateTime?,
    until: LocalDateTime?,
  ): Map<MergeResult, List<NonAssociation>> {
    log.info(
      "Replacing prisoner number $oldPrisonerNumber to $newPrisonerNumber " +
        "(created between ${since ?: "whenever"} and ${until ?: "now"})",
    )

    val updatedRecords = mutableListOf<Pair<MergeResult, NonAssociation>>()

    val createdAtFilter: (NonAssociation) -> Boolean = when {
      // created between `since` and `until`
      since != null && until != null -> { nonAssociation ->
        nonAssociation.whenCreated >= since &&
          nonAssociation.whenCreated <= until
      }
      // created after `since`
      since != null -> { nonAssociation -> nonAssociation.whenCreated >= since }
      // created before `until`
      until != null -> { nonAssociation -> nonAssociation.whenCreated <= until }
      // created at any time
      else -> { _ -> true }
    }

    nonAssociationsRepository.findAllByPrisonerNumber(oldPrisonerNumber)
      .filter(createdAtFilter)
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
      return MergeResult.CLOSED to nonAssociation
    } else if (newPrisonerNumber == otherPrisonerNumber) {
      log.info("Deleting non-association record $nonAssociation - same prisoner number")
      nonAssociationsRepository.delete(nonAssociation)
      return MergeResult.DELETED to nonAssociation
    }

    log.info("Merged non-association record $nonAssociation")
    return MergeResult.MERGED to nonAssociation
  }
}

enum class MergeResult {
  MERGED,
  CLOSED,
  DELETED,
}
