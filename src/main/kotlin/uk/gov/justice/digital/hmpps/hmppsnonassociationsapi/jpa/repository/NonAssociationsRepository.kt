package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

@Repository
interface NonAssociationsRepository : JpaRepository<NonAssociation, Long> {
  fun findAllByFirstPrisonerNumberOrSecondPrisonerNumber(firstPrisonerNumber: String, secondPrisonerNumber: String): List<NonAssociation>
  fun findByFirstPrisonerNumberAndSecondPrisonerNumber(firstPrisonerNumber: String, secondPrisonerNumber: String): NonAssociation?
}

/**
 * Returns all non-associations where the given prisoner number is either first OR second in the non-association
 */
fun NonAssociationsRepository.findAllByPrisonerNumber(prisonerNumber: String): List<NonAssociation> =
  findAllByFirstPrisonerNumberOrSecondPrisonerNumber(prisonerNumber, prisonerNumber)
