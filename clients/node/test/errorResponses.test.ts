import type { SanitisedError } from '@ministryofjustice/hmpps-rest-client'

import { ErrorResponse, isErrorResponse } from '../src'

describe('Error responses', () => {
  it('should be recognises', async () => {
    const error = new Error('Conflict') as SanitisedError<ErrorResponse>
    Object.assign(error, {
      responseStatus: 409,
      text: 'Conflict',
      message: 'Conflict',
      headers: {},
      data: {
        status: 409,
        errorCode: 101,
        userMessage:
          'Non-association already exists for these prisoners that is open: ' +
          'Prisoners [A1234A, B1234B] already have open non-associations',
        developerMessage: 'Prisoners [A1234A, B1234B] already have open non-associations',
      },
    })

    const unknownResponse = error.data as unknown
    expect(isErrorResponse(unknownResponse)).toBe(true)
    if (isErrorResponse(unknownResponse)) {
      // `isErrorResponse` allows for type narrowing, this would not type-check without it:
      expect(unknownResponse.errorCode).toEqual(101)
    }
  })
})
