import {
  parseDates,
  type NonAssociation,
  type OpenNonAssociationsListItem,
  type ClosedNonAssociationsListItem,
} from '../src'
import { openNonAssociation, closedNonAssociation, nonAssociationListOpen, nonAssociationListClosed } from './testData'

describe('parseDates', () => {
  describe('of non-associations', () => {
    type AppFormat = NonAssociation
    type WireFormat = Omit<AppFormat, 'whenCreated' | 'whenUpdated' | 'closedAt'> & {
      whenCreated: string
      whenUpdated: string
      closedAt: string | null
    }

    it('should work when they’re open', () => {
      const parsedNonAssociation = parseDates({
        ...openNonAssociation,
        whenCreated: '2023-07-28T18:10:51',
        whenUpdated: '2023-07-29T14:00:12',
      } satisfies WireFormat)
      expect(parsedNonAssociation.whenCreated).toEqual(new Date(2023, 6, 28, 18, 10, 51))
      expect(parsedNonAssociation.whenUpdated).toEqual(new Date(2023, 6, 29, 14, 0, 12))
      expect(parsedNonAssociation.closedAt).toBeNull()
    })

    it('should work when they’re closed', () => {
      const parsedNonAssociation = parseDates({
        ...closedNonAssociation,
        whenCreated: '2023-07-28T18:10:51',
        whenUpdated: '2023-08-30T09:30:39',
        closedAt: '2023-08-30T09:30:39',
      } satisfies WireFormat)
      expect(parsedNonAssociation.whenCreated).toEqual(new Date(2023, 6, 28, 18, 10, 51))
      expect(parsedNonAssociation.whenUpdated).toEqual(new Date(2023, 7, 30, 9, 30, 39))
      expect(parsedNonAssociation.closedAt).toEqual(new Date(2023, 7, 30, 9, 30, 39))
    })
  })

  describe('of non-association lists', () => {
    type AppFormat = OpenNonAssociationsListItem | ClosedNonAssociationsListItem
    type WireFormat = Omit<AppFormat, 'whenCreated' | 'whenUpdated' | 'closedAt'> & {
      whenCreated: string
      whenUpdated: string
      closedAt: string | null
    }

    it('should work when they’re open', () => {
      const nonAssociationsListItem: WireFormat = {
        ...nonAssociationListOpen.nonAssociations[0],
        whenCreated: '2023-07-28T18:10:51',
        whenUpdated: '2023-07-29T14:00:12',
      }
      const parsedNonAssociationsListItem = parseDates(nonAssociationsListItem)
      expect(parsedNonAssociationsListItem.whenCreated).toEqual(new Date(2023, 6, 28, 18, 10, 51))
      expect(parsedNonAssociationsListItem.whenUpdated).toEqual(new Date(2023, 6, 29, 14, 0, 12))
      expect(parsedNonAssociationsListItem.closedAt).toBeNull()
    })

    it('should work when they’re closed', () => {
      const nonAssociationsListItem: WireFormat = {
        ...nonAssociationListClosed.nonAssociations[0],
        whenCreated: '2023-07-28T18:10:51',
        whenUpdated: '2023-08-30T09:30:39',
        closedAt: '2023-08-30T09:30:39',
      }
      const parsedNonAssociationsListItem = parseDates(nonAssociationsListItem)
      expect(parsedNonAssociationsListItem.whenCreated).toEqual(new Date(2023, 6, 28, 18, 10, 51))
      expect(parsedNonAssociationsListItem.whenUpdated).toEqual(new Date(2023, 7, 30, 9, 30, 39))
      expect(parsedNonAssociationsListItem.closedAt).toEqual(new Date(2023, 7, 30, 9, 30, 39))
    })
  })
})
