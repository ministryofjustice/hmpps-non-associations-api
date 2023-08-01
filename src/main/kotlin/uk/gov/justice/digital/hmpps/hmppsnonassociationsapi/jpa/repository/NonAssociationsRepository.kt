package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

@Repository
interface NonAssociationsRepository : JpaRepository<NonAssociation, Long> {
  fun findAllByFirstPrisonerNumberOrSecondPrisonerNumber(firstPrisonerNumber: String, secondPrisonerNumber: String): List<NonAssociation>
  fun findAllByFirstPrisonerNumberAndSecondPrisonerNumberOrFirstPrisonerNumberAndSecondPrisonerNumber(pn1: String, pn2: String, pn3: String, pn4: String): List<NonAssociation>

  // TODO: Assumes that there can only be 1 non-association given a pair of prisoner numbers.
  //       In future, this will only be true for open non-associations.
  fun findByFirstPrisonerNumberAndSecondPrisonerNumber(firstPrisonerNumber: String, secondPrisonerNumber: String): NonAssociation?
}

/**
 * Returns all non-associations where the given prisoner number is either first OR second in the non-association
 */
fun NonAssociationsRepository.findAllByPrisonerNumber(prisonerNumber: String): List<NonAssociation> =
  findAllByFirstPrisonerNumberOrSecondPrisonerNumber(prisonerNumber, prisonerNumber)

/**
 * Returns all non-associations given two prisoner numbers
 */
fun NonAssociationsRepository.findAllByPairOfPrisonerNumber(prisonerNumbers: Pair<String, String>): List<NonAssociation> =
  findAllByFirstPrisonerNumberAndSecondPrisonerNumberOrFirstPrisonerNumberAndSecondPrisonerNumber(
    prisonerNumbers.first,
    prisonerNumbers.second,
    prisonerNumbers.second,
    prisonerNumbers.first,
  )
