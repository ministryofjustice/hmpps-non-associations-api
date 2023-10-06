import type { ResponseError } from 'superagent'

export interface SanitisedError<Data = unknown> extends Error {
  text?: string
  status?: number
  headers?: unknown
  data?: Data
  stack?: string
  message: string
}

/**
 * Converts a superagent.ResponseError into a simpler Error object,
 * omitting request inforation (e.g. sensitive headers)
 */
export function sanitiseError<Data = unknown>(error: ResponseError): SanitisedError<Data> {
  const e = new Error() as SanitisedError<Data>
  e.message = error.message
  e.stack = error.stack
  if (error.response) {
    e.text = error.response.text
    e.status = error.response.status
    e.headers = error.response.headers
    e.data = error.response.body
  }
  return e
}
