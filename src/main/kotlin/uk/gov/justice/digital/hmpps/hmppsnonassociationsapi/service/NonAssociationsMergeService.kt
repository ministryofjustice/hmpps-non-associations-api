package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber

@Service
@Transactional
class NonAssociationsMergeService(
  private val nonAssociationsRepository: NonAssociationsRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun mergePrisonerNumbers(oldPrisonerNumber: String, newPrisonerNumber: String): List<NonAssociation> {
    log.info("Replacing prisoner number $oldPrisonerNumber to $newPrisonerNumber")

    val updatedRecords = mutableListOf<NonAssociation>()

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
            )?.let { updatedRecord ->
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
            )?.let { updatedRecord ->
              updatedRecords.add(updatedRecord)
            }
          }
        }
      }

    return updatedRecords.toList()
  }

  private fun merge(
    nonAssociation: NonAssociation,
    duplicateRecord: NonAssociation?,
    newPrisonerNumber: String,
    otherPrisonerNumber: String,
    primary: Boolean,
  ) =
    if (duplicateRecord != null) {
      log.info("Deleting non-association record $nonAssociation - Duplicate")
      nonAssociationsRepository.delete(nonAssociation)
      duplicateRecord
    } else {
      if (newPrisonerNumber == otherPrisonerNumber) {
        log.info("Deleting non-association record $nonAssociation - same prisoner number")
        nonAssociationsRepository.delete(nonAssociation)
        null
      } else {
        log.info("Merge non-association record $nonAssociation")
        nonAssociation.updatePrisonerNumber(newPrisonerNumber, primary)
      }
    }
}
