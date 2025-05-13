export interface ObjectWithDates {
  whenCreated: unknown
  whenUpdated: unknown
  closedAt: unknown
}

/**
 * Dates are returned as strings in JSON responses so need conversion;
 * works on NonAssociation and items in NonAssociationsList.nonAssociations
 */
export function parseDates<O extends ObjectWithDates>(data: O): O {
  if (typeof data.whenCreated === 'string') {
    data.whenCreated = new Date(data.whenCreated)
  }
  if (typeof data.whenUpdated === 'string') {
    data.whenUpdated = new Date(data.whenUpdated)
  }
  if (typeof data.closedAt === 'string') {
    data.closedAt = new Date(data.closedAt as string)
  }
  return data
}
