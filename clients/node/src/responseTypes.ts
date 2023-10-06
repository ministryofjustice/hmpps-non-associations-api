import type { Reason, RestrictionType, Role } from './constants'
import type { ObjectWithDates } from './parseDates'

interface EnumerationItem {
  code: string
  description: string
}
export interface Constants extends Record<'roles' | 'reasons' | 'restrictionTypes', EnumerationItem[]> {}

interface BaseNonAssociationsListItem extends ObjectWithDates {
  id: number
  role: keyof Role
  roleDescription: Role[keyof Role]
  reason: keyof Reason
  reasonDescription: Reason[keyof Reason]
  restrictionType: keyof RestrictionType
  restrictionTypeDescription: RestrictionType[keyof RestrictionType]
  comment: string
  authorisedBy: string
  updatedBy: string
  whenCreated: Date
  whenUpdated: Date
  otherPrisonerDetails: {
    prisonerNumber: string
    role: keyof Role
    roleDescription: Role[keyof Role]
    firstName: string
    lastName: string
    prisonId: string
    prisonName: string
    cellLocation?: string
  }
}

export interface OpenNonAssociationsListItem extends BaseNonAssociationsListItem {
  isClosed: false
  closedBy: null
  closedReason: null
  closedAt: null
}

export interface ClosedNonAssociationsListItem extends BaseNonAssociationsListItem {
  isClosed: true
  closedBy: string
  closedReason: string
  closedAt: Date
}

export interface NonAssociationsList<
  Item extends BaseNonAssociationsListItem = OpenNonAssociationsListItem | ClosedNonAssociationsListItem,
> {
  prisonerNumber: string
  firstName: string
  lastName: string
  prisonId: string
  prisonName: string
  cellLocation?: string
  openCount: number
  closedCount: number
  nonAssociations: Item[]
}

interface BaseNonAssociation extends ObjectWithDates {
  id: number
  firstPrisonerNumber: string
  firstPrisonerRole: keyof Role
  firstPrisonerRoleDescription: Role[keyof Role]
  secondPrisonerNumber: string
  secondPrisonerRole: keyof Role
  secondPrisonerRoleDescription: Role[keyof Role]
  reason: keyof Reason
  reasonDescription: Reason[keyof Reason]
  restrictionType: keyof RestrictionType
  restrictionTypeDescription: RestrictionType[keyof RestrictionType]
  comment: string
  authorisedBy: string
  updatedBy: string
  whenCreated: Date
  whenUpdated: Date
}

export interface OpenNonAssociation extends BaseNonAssociation {
  isClosed: false
  closedBy: null
  closedReason: null
  closedAt: null
}

export interface ClosedNonAssociation extends BaseNonAssociation {
  isClosed: true
  closedBy: string
  closedReason: string
  closedAt: Date
}

export type NonAssociation = OpenNonAssociation | ClosedNonAssociation

export interface CreateNonAssociationRequest {
  firstPrisonerNumber: string
  firstPrisonerRole: keyof Role
  secondPrisonerNumber: string
  secondPrisonerRole: keyof Role
  reason: keyof Reason
  restrictionType: keyof RestrictionType
  comment: string
}

export interface UpdateNonAssociationRequest {
  firstPrisonerRole?: keyof Role
  secondPrisonerRole?: keyof Role
  reason?: keyof Reason
  restrictionType?: keyof RestrictionType
  comment?: string
}

export interface CloseNonAssociationRequest {
  closedReason: string
  closedAt?: Date
  closedBy?: string
}
