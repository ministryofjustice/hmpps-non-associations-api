package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation

@Repository
interface NonAssociationsRepository : JpaRepository<NonAssociation, Long> {
  fun findAllByIsClosed(isClosed: Boolean, pageable: Pageable): Page<NonAssociation>

  /** Use findAllByPrisonerNumber convenience extension function instead */
  fun findAllByFirstPrisonerNumberOrSecondPrisonerNumber(
    firstPrisonerNumber: String,
    secondPrisonerNumber: String,
  ): List<NonAssociation>

  /** Use findAnyInvolvingPrisonerNumbers convenience extension function instead */
  fun findAllByFirstPrisonerNumberInOrSecondPrisonerNumberIn(
    p1: Collection<String>,
    p2: Collection<String>,
  ): List<NonAssociation>

  /** Use findAnyBetweenPrisonerNumbers convenience extension function instead */
  fun findAllByFirstPrisonerNumberInAndSecondPrisonerNumberIn(
    p1: Collection<String>,
    p2: Collection<String>,
  ): List<NonAssociation>

  /** Use findAnyBetweenPrisonerNumbers convenience extension function instead */
  fun findAllByFirstPrisonerNumberInAndSecondPrisonerNumberInAndIsClosed(
    p1: Collection<String>,
    p2: Collection<String>,
    isClosed: Boolean,
  ): List<NonAssociation>

  /** Use findAnyInvolvingPrisonerNumbers convenience extension function instead */
  @Query(
    value = """
      SELECT n FROM NonAssociation n
      WHERE n.isClosed = :isClosed AND (
        n.firstPrisonerNumber IN (:p1) OR n.secondPrisonerNumber IN (:p2)
      )""",
  )
  fun findAllByFirstPrisonerNumberInOrSecondPrisonerNumberInAndIsClosed(
    @Param("p1") p1: Collection<String>,
    @Param("p2") p2: Collection<String>,
    @Param("isClosed") isClosed: Boolean,
  ): List<NonAssociation>

  // TODO: Assumes that there can only be 1 non-association given a pair of prisoner numbers.
  //       In future, this will only be true for open non-associations.
  fun findByFirstPrisonerNumberAndSecondPrisonerNumber(
    firstPrisonerNumber: String,
    secondPrisonerNumber: String,
  ): NonAssociation?
}

/**
 * Returns all non-associations where the given prisoner number is either first OR second in the non-association
 */
fun NonAssociationsRepository.findAllByPrisonerNumber(prisonerNumber: String): List<NonAssociation> =
  findAllByFirstPrisonerNumberOrSecondPrisonerNumber(prisonerNumber, prisonerNumber)

/**
 * Returns non-associations between any prisoners in given prisoner numbers
 */
fun NonAssociationsRepository.findAnyBetweenPrisonerNumbers(
  prisonerNumbers: Collection<String>,
  inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
): List<NonAssociation> = when (inclusion) {
  NonAssociationListInclusion.OPEN_ONLY -> findAllByFirstPrisonerNumberInAndSecondPrisonerNumberInAndIsClosed(
    prisonerNumbers,
    prisonerNumbers,
    false,
  )
  NonAssociationListInclusion.CLOSED_ONLY -> findAllByFirstPrisonerNumberInAndSecondPrisonerNumberInAndIsClosed(
    prisonerNumbers,
    prisonerNumbers,
    true,
  )
  NonAssociationListInclusion.ALL -> findAllByFirstPrisonerNumberInAndSecondPrisonerNumberIn(
    prisonerNumbers,
    prisonerNumbers,
  )
}

/**
 * Returns non-associations involving any prisoners in given prisoner numbers
 */
fun NonAssociationsRepository.findAnyInvolvingPrisonerNumbers(
  prisonerNumbers: Collection<String>,
  inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
): List<NonAssociation> = when (inclusion) {
  NonAssociationListInclusion.OPEN_ONLY -> findAllByFirstPrisonerNumberInOrSecondPrisonerNumberInAndIsClosed(
    prisonerNumbers,
    prisonerNumbers,
    false,
  )
  NonAssociationListInclusion.CLOSED_ONLY -> findAllByFirstPrisonerNumberInOrSecondPrisonerNumberInAndIsClosed(
    prisonerNumbers,
    prisonerNumbers,
    true,
  )
  NonAssociationListInclusion.ALL -> findAllByFirstPrisonerNumberInOrSecondPrisonerNumberIn(
    prisonerNumbers,
    prisonerNumbers,
  )
}
