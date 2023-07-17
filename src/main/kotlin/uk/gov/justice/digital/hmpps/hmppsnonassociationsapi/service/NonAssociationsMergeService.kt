package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@Service
@Transactional
class NonAssociationsMergeService(
  private val nonAssociationsRepository: NonAssociationsRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun mergePrisonerNumbers(oldPrisonerNumber: String, newPrisonerNumber: String): List<NonAssociationJPA> {
    log.info("Replacing prisoner number $oldPrisonerNumber to $newPrisonerNumber")

    val updatedRecords = mutableListOf<NonAssociation>()

    nonAssociationsRepository.findAllByFirstPrisonerNumber(oldPrisonerNumber)
      .plus(nonAssociationsRepository.findAllBySecondPrisonerNumber(oldPrisonerNumber)).forEach { nonAssociation ->

        log.debug("Looking at record ${nonAssociation.id}")
        if (nonAssociation.firstPrisonerNumber == oldPrisonerNumber) {
          nonAssociationsRepository.findByFirstPrisonerNumberAndSecondPrisonerNumber(
            firstPrisonerNumber = newPrisonerNumber,
            secondPrisonerNumber = nonAssociation.secondPrisonerNumber,
          ).let {
            if (it != null) {
              nonAssociationsRepository.delete(nonAssociation)
              log.info("Deleted Non association record ${nonAssociation.id}")
              it.comment = nonAssociation.comment // replace comment??
              updatedRecords.add(it)
            } else {
              if (newPrisonerNumber == nonAssociation.secondPrisonerNumber) {
                nonAssociationsRepository.delete(nonAssociation)
                log.info("Deleted Non association record ${nonAssociation.id} as same prisoner both ways")
              } else {
                log.info("Updated Non association record ${nonAssociation.id} from ${nonAssociation.firstPrisonerNumber} to $newPrisonerNumber")
                nonAssociation.firstPrisonerNumber = newPrisonerNumber
                updatedRecords.add(nonAssociation)
              }
            }
          }
        }

        if (nonAssociation.secondPrisonerNumber == oldPrisonerNumber) {
          nonAssociationsRepository.findByFirstPrisonerNumberAndSecondPrisonerNumber(
            firstPrisonerNumber = nonAssociation.firstPrisonerNumber,
            secondPrisonerNumber = newPrisonerNumber,
          ).let {
            if (it != null) {
              nonAssociationsRepository.delete(nonAssociation)
              log.info("Deleted Non association record ${nonAssociation.id}")
              it.comment = nonAssociation.comment // replace comment??
              updatedRecords.add(it)
            } else {
              if (newPrisonerNumber == nonAssociation.firstPrisonerNumber) {
                nonAssociationsRepository.delete(nonAssociation)
                log.info("Deleted Non association record ${nonAssociation.id} as same prisoner both ways")
              } else {
                log.info("Updated Non association record ${nonAssociation.id} from ${nonAssociation.secondPrisonerNumber} to $newPrisonerNumber")
                nonAssociation.secondPrisonerNumber = newPrisonerNumber
                updatedRecords.add(nonAssociation)
              }
            }
          }
        }
      }

    return updatedRecords.toList()
  }
}
