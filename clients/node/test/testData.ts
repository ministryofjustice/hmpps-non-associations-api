import type {
  OpenNonAssociation,
  ClosedNonAssociation,
  NonAssociationsList,
  OpenNonAssociationsListItem,
  ClosedNonAssociationsListItem,
} from '../src'

export const openNonAssociation: OpenNonAssociation = {
  id: 101,
  firstPrisonerNumber: 'A1234BC',
  firstPrisonerRole: 'PERPETRATOR',
  firstPrisonerRoleDescription: 'Perpetrator',
  secondPrisonerNumber: 'A1235EF',
  secondPrisonerRole: 'VICTIM',
  secondPrisonerRoleDescription: 'Victim',
  reason: 'THREAT',
  reasonDescription: 'Threat',
  restrictionType: 'CELL',
  restrictionTypeDescription: 'Cell only',
  comment: 'See IR 12133100',
  authorisedBy: 'cde87s',
  updatedBy: 'cde87s',
  whenCreated: new Date('2023-07-21T08:14:21'),
  whenUpdated: new Date('2023-07-21T08:14:21'),
  isClosed: false,
  closedBy: null,
  closedReason: null,
  closedAt: null,
}

export const closedNonAssociation: ClosedNonAssociation = {
  ...openNonAssociation,
  isClosed: true,
  closedBy: 'abc12a',
  closedReason: 'Problem solved',
  closedAt: new Date('2023-07-26T12:34:56'),
}

export const nonAssociationListOpen: NonAssociationsList<OpenNonAssociationsListItem> = {
  prisonId: 'MDI',
  prisonName: 'Moorland (HMP & YOI)',
  prisonerNumber: 'A1234BC',
  firstName: 'DAVID',
  lastName: 'JONES',
  cellLocation: '1-1-001',
  openCount: 1,
  closedCount: 0,
  nonAssociations: [
    {
      id: 101,
      role: 'PERPETRATOR',
      roleDescription: 'Perpetrator',
      reason: 'THREAT',
      reasonDescription: 'Threat',
      restrictionType: 'CELL',
      restrictionTypeDescription: 'Cell only',
      comment: 'See IR 12133100',
      otherPrisonerDetails: {
        prisonId: 'MDI',
        prisonName: 'Moorland (HMP & YOI)',
        firstName: 'DAVID',
        lastName: 'JONES',
        prisonerNumber: 'A1235EF',
        role: 'VICTIM',
        roleDescription: 'Victim',
      },
      authorisedBy: 'cde87s',
      updatedBy: 'cde87s',
      whenCreated: new Date('2023-07-26T12:34:56'),
      whenUpdated: new Date('2023-07-26T12:34:56'),
      isClosed: false,
      closedBy: null,
      closedReason: null,
      closedAt: null,
    },
  ],
}

export const nonAssociationListClosed: NonAssociationsList<ClosedNonAssociationsListItem> = {
  ...nonAssociationListOpen,
  openCount: 0,
  closedCount: 1,
  nonAssociations: [
    {
      ...nonAssociationListOpen.nonAssociations[0],
      isClosed: true,
      closedBy: 'lev79n',
      closedReason: 'Problem solved',
      closedAt: new Date('2023-07-27T12:34:56'),
      whenUpdated: new Date('2023-07-27T12:34:56'),
    },
  ],
}
