import type { ResponseError } from 'superagent'

import { sanitiseError, type SanitisedError, ErrorResponse, isErrorResponse } from '../src'

describe('sanitiseError', () => {
  it('should omit the request headers from the error object ', () => {
    const error = {
      name: '',
      status: 404,
      response: {
        req: {
          method: 'GET',
          url: 'https://test-api/endpoint?active=true',
          headers: {
            property: 'not for logging',
          },
        },
        headers: {
          date: 'Tue, 19 May 2020 15:16:20 GMT',
        },
        status: 404,
        statusText: 'Not found',
        text: { details: 'details' },
        body: { content: 'hello' },
      },
      message: 'Not Found',
      stack: 'stack description',
    } as unknown as ResponseError

    const e = new Error() as SanitisedError<{ content: string }>
    e.message = 'Not Found'
    e.text = 'details'
    e.status = 404
    e.headers = { date: 'Tue, 19 May 2020 15:16:20 GMT' }
    e.data = { content: 'hello' }
    e.stack = 'stack description'

    expect(sanitiseError(error)).toEqual(e)
  })

  it('should return the error message', () => {
    const error = {
      message: 'error description',
    } as unknown as ResponseError

    expect(sanitiseError(error)).toBeInstanceOf(Error)
    expect(sanitiseError(error)).toHaveProperty('message', 'error description')
  })

  it('should return an empty Error instance for an unknown error structure', () => {
    const error = {
      property: 'unknown',
    } as unknown as ResponseError

    expect(sanitiseError(error)).toBeInstanceOf(Error)
    expect(sanitiseError(error)).not.toHaveProperty('property')
  })

  it('should recognise error responses', async () => {
    const error = new Error('Conflict') as SanitisedError<ErrorResponse>
    Object.assign(error, {
      status: 409,
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
    expect(isErrorResponse(unknownResponse)).toEqual(true)
    if (isErrorResponse(unknownResponse)) {
      // `isErrorResponse` allows for type narrowing, this would not type-check without it:
      expect(unknownResponse.errorCode).toEqual(101)
    }
  })
})
