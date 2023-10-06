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
    // eslint-disable-next-line no-param-reassign
    data.whenCreated = new Date(data.whenCreated)
  }
  if (typeof data.whenUpdated === 'string') {
    // eslint-disable-next-line no-param-reassign
    data.whenUpdated = new Date(data.whenUpdated)
  }
  if (typeof data.closedAt === 'string') {
    // eslint-disable-next-line no-param-reassign
    data.closedAt = new Date(data.closedAt as string)
  }
  return data
}
