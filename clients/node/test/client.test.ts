// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import nock from 'nock'
import type { Plugin } from 'superagent'

import {
  NonAssociationsApi,
  type Logger,
  type NonAssociation,
  type OpenNonAssociation,
  type ClosedNonAssociation,
  type NonAssociationsList,
  type OpenNonAssociationsListItem,
  type ClosedNonAssociationsListItem,
  type Page,
} from '../src'
import { nonAssociationListOpen, openNonAssociation, closedNonAssociation } from './testData'

const baseUrl = 'http://localhost:8080'
const token = 'token-1'
const logger = {
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
} as unknown as jest.Mocked<Logger>
const plugin = jest.fn() as unknown as jest.Mocked<Plugin>
const client = new NonAssociationsApi(
  token,
  {
    url: baseUrl,
    timeout: {
      response: 1000,
      deadline: 1000,
    },
    agent: { timeout: 1000 },
  },
  logger,
  [plugin],
)

beforeEach(() => {
  jest.resetAllMocks()
  nock.cleanAll()
})

afterEach(() => {
  expect(logger.warn).not.toHaveBeenCalled()
})

function mockResponse(): nock.Scope {
  return nock(baseUrl, {
    reqheaders: {
      authorization: `Bearer ${token}`,
    },
  })
}

describe('REST Client', () => {
  describe('should return response body', () => {
    it('when gettings constants', async () => {
      const constants = {
        roles: [{ code: 'VICTIM', description: 'Victim' }],
        reasons: [{ code: 'BULLYING', description: 'Bullying' }],
        restrictionTypes: [{ code: 'CELL', description: 'Cell only' }],
      }
      mockResponse().get('/constants').reply(200, constants)

      expect(await client.constants()).toStrictEqual(constants)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET' }))
    })

    it('when getting a non-association by id', async () => {
      mockResponse().get(`/non-associations/${openNonAssociation.id}`).reply(200, openNonAssociation)

      const nonAssociation = await client.getNonAssociation(openNonAssociation.id)
      expect(nonAssociation).toStrictEqual(openNonAssociation)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET' }))
    })

    it('when listing non-associations for a prisoner', async () => {
      mockResponse()
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(200, nonAssociationListOpen)

      const nonAssociations = await client.listNonAssociations(nonAssociationListOpen.prisonerNumber)
      expect(nonAssociations).toStrictEqual(nonAssociationListOpen)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET' }))
    })

    it('when listing non-associations between 2 prisoners', async () => {
      mockResponse()
        .post(
          '/non-associations/between?includeOpen=false&includeClosed=true',
          `["${closedNonAssociation.firstPrisonerNumber}","${closedNonAssociation.secondPrisonerNumber}"]`,
          {
            reqheaders: { 'content-type': 'application/json' },
          },
        )
        .reply(200, [closedNonAssociation])

      const nonAssociationsList = await client.listNonAssociationsBetween(
        [closedNonAssociation.firstPrisonerNumber, closedNonAssociation.secondPrisonerNumber],
        { includeOpen: false, includeClosed: true },
      )
      expect(nonAssociationsList).toStrictEqual([closedNonAssociation])
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST' }))
    })

    it('when listing non-associations involving 2 prisoners', async () => {
      mockResponse()
        .post(
          '/non-associations/involving?includeOpen=false&includeClosed=true',
          `["${closedNonAssociation.secondPrisonerNumber}","${closedNonAssociation.firstPrisonerNumber}"]`,
          {
            reqheaders: { 'content-type': 'application/json' },
          },
        )
        .reply(200, [closedNonAssociation])

      const nonAssociationsList = await client.listNonAssociationsInvolving(
        [closedNonAssociation.secondPrisonerNumber, closedNonAssociation.firstPrisonerNumber],
        { includeOpen: false, includeClosed: true },
      )
      expect(nonAssociationsList).toStrictEqual([closedNonAssociation])
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST' }))
    })

    it('when listing all non-associations in pages', async () => {
      mockResponse()
        .get('/non-associations?includeOpen=true&includeClosed=true')
        .reply(200, {
          content: [openNonAssociation, closedNonAssociation],
          number: 0,
          totalPages: 1,
          totalElements: 2,
        })

      const nonAssociationsList = await client.pagedNonAssociations({ includeClosed: true })
      expect(nonAssociationsList).toStrictEqual({
        content: [openNonAssociation, closedNonAssociation],
        number: 0,
        totalPages: 1,
        totalElements: 2,
      })
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET' }))
    })

    it('when creating a non-association', async () => {
      mockResponse()
        .post('/non-associations', /See IR 12133100/, {
          reqheaders: { 'content-type': 'application/json' },
        })
        .reply(201, openNonAssociation)

      const nonAssociation = await client.createNonAssociation({
        firstPrisonerNumber: 'A1234BC',
        firstPrisonerRole: 'PERPETRATOR',
        secondPrisonerNumber: 'A1235EF',
        secondPrisonerRole: 'VICTIM',
        reason: 'THREAT',
        restrictionType: 'CELL',
        comment: 'See IR 12133100',
      })
      expect(nonAssociation).toEqual(openNonAssociation)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST' }))
    })

    it('when updating a non-association', async () => {
      mockResponse()
        .patch(`/non-associations/${openNonAssociation.id}`, /LANDING/, {
          reqheaders: { 'content-type': 'application/json' },
        })
        .reply(200, {
          ...openNonAssociation,
          restrictionType: 'LANDING',
          restrictionTypeDescription: 'Cell and landing',
        })

      const nonAssociation = await client.updateNonAssociation(openNonAssociation.id, {
        restrictionType: 'LANDING',
      })
      expect(nonAssociation.id).toEqual(openNonAssociation.id)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'PATCH' }))
    })

    it('when closing a non-association', async () => {
      mockResponse()
        .put(`/non-associations/${openNonAssociation.id}/close`, /Problem solved/, {
          reqheaders: { 'content-type': 'application/json' },
        })
        .reply(200, closedNonAssociation)

      const nonAssociation = await client.closeNonAssociation(openNonAssociation.id, {
        closedReason: 'Problem solved',
      })
      expect(nonAssociation).toEqual(closedNonAssociation)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'PUT' }))
    })

    it('when deleting a non-association', async () => {
      mockResponse().post('/non-associations/1/delete').reply(204, '')

      const result: null = await client.deleteNonAssociation(1, {
        deletionReason: 'Entered in error',
        staffUserNameRequestingDeletion: 'abc123',
      })
      expect(result).toBeNull()
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
      expect(plugin).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST' }))
    })
  })

  describe('should retry on failure', () => {
    it('when gettings constants', async () => {
      const constants = {
        roles: [{ code: 'VICTIM', description: 'Victim' }],
        reasons: [{ code: 'BULLYING', description: 'Bullying' }],
        restrictionTypes: [{ code: 'CELL', description: 'Cell only' }],
      }
      mockResponse().get('/constants').reply(503).get('/constants').reply(200, constants)

      expect(await client.constants()).toStrictEqual(constants)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
    })

    it('when getting a non-association by id', async () => {
      mockResponse()
        .get(`/non-associations/${openNonAssociation.id}`)
        .reply(503)
        .get(`/non-associations/${openNonAssociation.id}`)
        .reply(200, openNonAssociation)

      const nonAssociation = await client.getNonAssociation(openNonAssociation.id)
      expect(nonAssociation).toStrictEqual(openNonAssociation)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
    })

    it('when listing non-associations for a prisoner', async () => {
      mockResponse()
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(503)
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(200, nonAssociationListOpen)

      const nonAssociations = await client.listNonAssociations(nonAssociationListOpen.prisonerNumber)
      expect(nonAssociations).toStrictEqual(nonAssociationListOpen)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
    })

    it('when listing all non-associations in pages', async () => {
      mockResponse()
        .get('/non-associations?includeOpen=true&includeClosed=false')
        .reply(503)
        .get('/non-associations?includeOpen=true&includeClosed=false')
        .reply(200, {
          content: [openNonAssociation],
          number: 0,
          totalPages: 1,
          totalElements: 1,
        })

      const nonAssociations = await client.pagedNonAssociations()
      expect(nonAssociations.content[0]).toStrictEqual(openNonAssociation)
      expect(nock.isDone()).toEqual(true)

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).not.toHaveBeenCalled()
    })
  })

  describe('should fail after 2 tries', () => {
    it('when gettings constants', async () => {
      mockResponse().get('/constants').reply(503).get('/constants').reply(503).get('/constants').reply(503)

      await expect(client.constants()).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when getting a non-association by id', async () => {
      mockResponse()
        .get(`/non-associations/${openNonAssociation.id}`)
        .reply(503)
        .get(`/non-associations/${openNonAssociation.id}`)
        .reply(503)
        .get(`/non-associations/${openNonAssociation.id}`)
        .reply(503)

      await expect(client.getNonAssociation(openNonAssociation.id)).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when listing non-associations for a prisoner', async () => {
      mockResponse()
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(503)
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(503)
        .get(
          `/prisoner/${nonAssociationListOpen.prisonerNumber}/non-associations` +
            '?includeOpen=true&includeClosed=false&includeOtherPrisons=false&sortBy=WHEN_CREATED&sortDirection=DESC',
        )
        .reply(503)

      await expect(client.listNonAssociations(nonAssociationListOpen.prisonerNumber)).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when listing all non-associations in pages', async () => {
      mockResponse()
        .get('/non-associations?includeOpen=true&includeClosed=false')
        .reply(503)
        .get('/non-associations?includeOpen=true&includeClosed=false')
        .reply(503)
        .get('/non-associations?includeOpen=true&includeClosed=false')
        .reply(503)

      await expect(client.pagedNonAssociations()).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })
  })

  describe('should not retry', () => {
    it('when listing non-associations between 2 prisoners', async () => {
      mockResponse().post('/non-associations/between?includeOpen=true&includeClosed=false').reply(503)

      await expect(
        client.listNonAssociationsBetween([
          closedNonAssociation.firstPrisonerNumber,
          closedNonAssociation.secondPrisonerNumber,
        ]),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when listing non-associations involving 2 prisoners', async () => {
      mockResponse().post('/non-associations/involving?includeOpen=true&includeClosed=false').reply(503)

      await expect(
        client.listNonAssociationsInvolving([
          closedNonAssociation.firstPrisonerNumber,
          closedNonAssociation.secondPrisonerNumber,
        ]),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when creating a non-association', async () => {
      mockResponse().post('/non-associations').reply(503)

      await expect(
        client.createNonAssociation({
          firstPrisonerNumber: 'A1234BC',
          firstPrisonerRole: 'PERPETRATOR',
          secondPrisonerNumber: 'A1235EF',
          secondPrisonerRole: 'VICTIM',
          reason: 'THREAT',
          restrictionType: 'CELL',
          comment: 'See IR 12133100',
        }),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when updating a non-association', async () => {
      mockResponse().patch('/non-associations/1').reply(503)

      await expect(
        client.updateNonAssociation(1, {
          comment: 'See IR 12133555',
        }),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when closing a non-association', async () => {
      mockResponse().put('/non-associations/1/close').reply(503)

      await expect(
        client.closeNonAssociation(1, {
          closedReason: 'Problem solved',
        }),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 503,
          message: 'Service Unavailable',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })

    it('when deleting a non-association', async () => {
      mockResponse().post('/non-associations/1/delete').reply(403)

      await expect(
        client.deleteNonAssociation(1, {
          deletionReason: 'Entered in error',
          staffUserNameRequestingDeletion: 'abc123',
        }),
      ).rejects.toEqual(
        expect.objectContaining({
          status: 403,
          message: 'Forbidden',
        }),
      )

      expect(logger.info).toHaveBeenCalledTimes(1)
      expect(logger.error).toHaveBeenCalledTimes(1)
    })
  })

  describe('should specify varying return types', () => {
    it('when listing non-associations for a prisoner', async () => {
      mockResponse()
        .persist()
        .get(/prisoner\/A1234BC\/non-associations/)
        .reply(200, nonAssociationListOpen)

      const promises: [
        NonAssociationsList<OpenNonAssociationsListItem>,
        NonAssociationsList<OpenNonAssociationsListItem>,
        NonAssociationsList<OpenNonAssociationsListItem>,
        NonAssociationsList<OpenNonAssociationsListItem>,
        NonAssociationsList<ClosedNonAssociationsListItem>,
        NonAssociationsList,
        NonAssociationsList<never>,
      ] = await Promise.all([
        client.listNonAssociations('A1234BC'),
        client.listNonAssociations('A1234BC', { includeOpen: true }),
        client.listNonAssociations('A1234BC', { includeClosed: false }),
        client.listNonAssociations('A1234BC', { includeOpen: true, includeClosed: false }),
        client.listNonAssociations('A1234BC', { includeOpen: false, includeClosed: true }),
        client.listNonAssociations('A1234BC', { includeOpen: true, includeClosed: true }),
        client.listNonAssociations('A1234BC', { includeOpen: false, includeClosed: false }),
      ])
      expect(promises).toHaveLength(7)
    })

    it('when listing non-associations between 2 prisoners', async () => {
      const emptyResponse: NonAssociation[] = []
      mockResponse()
        .persist()
        .post(/non-associations\/between/)
        .reply(200, emptyResponse)

      const promises: [
        OpenNonAssociation[],
        OpenNonAssociation[],
        OpenNonAssociation[],
        OpenNonAssociation[],
        ClosedNonAssociation[],
        NonAssociation[],
        never[],
      ] = await Promise.all([
        client.listNonAssociationsBetween([]),
        client.listNonAssociationsBetween([], { includeOpen: true }),
        client.listNonAssociationsBetween([], { includeClosed: false }),
        client.listNonAssociationsBetween([], { includeOpen: true, includeClosed: false }),
        client.listNonAssociationsBetween([], { includeOpen: false, includeClosed: true }),
        client.listNonAssociationsBetween([], { includeOpen: true, includeClosed: true }),
        client.listNonAssociationsBetween([], { includeOpen: false, includeClosed: false }),
      ])
      expect(promises).toHaveLength(7)
    })

    it('when listing non-associations involving 2 prisoners', async () => {
      const emptyResponse: NonAssociation[] = []
      mockResponse()
        .persist()
        .post(/non-associations\/involving/)
        .reply(200, emptyResponse)

      const promises: [
        OpenNonAssociation[],
        OpenNonAssociation[],
        OpenNonAssociation[],
        OpenNonAssociation[],
        ClosedNonAssociation[],
        NonAssociation[],
        never[],
      ] = await Promise.all([
        client.listNonAssociationsInvolving([]),
        client.listNonAssociationsInvolving([], { includeOpen: true }),
        client.listNonAssociationsInvolving([], { includeClosed: false }),
        client.listNonAssociationsInvolving([], { includeOpen: true, includeClosed: false }),
        client.listNonAssociationsInvolving([], { includeOpen: false, includeClosed: true }),
        client.listNonAssociationsInvolving([], { includeOpen: true, includeClosed: true }),
        client.listNonAssociationsInvolving([], { includeOpen: false, includeClosed: false }),
      ])
      expect(promises).toHaveLength(7)
    })

    it('when listing all non-associations in pages', async () => {
      const emptyResponse: Page<NonAssociation> = {
        content: [],
        number: 0,
        totalPages: 0,
        totalElements: 0,
      }
      mockResponse()
        .persist()
        .get(/non-associations/)
        .reply(200, emptyResponse)

      const promises: [
        Page<OpenNonAssociation>,
        Page<OpenNonAssociation>,
        Page<OpenNonAssociation>,
        Page<OpenNonAssociation>,
        Page<ClosedNonAssociation>,
        Page<NonAssociation>,
        Page<never>,
      ] = await Promise.all([
        client.pagedNonAssociations(),
        client.pagedNonAssociations({ includeOpen: true }),
        client.pagedNonAssociations({ includeClosed: false }),
        client.pagedNonAssociations({ includeOpen: true, includeClosed: false }),
        client.pagedNonAssociations({ includeOpen: false, includeClosed: true }),
        client.pagedNonAssociations({ includeOpen: true, includeClosed: true }),
        client.pagedNonAssociations({ includeOpen: false, includeClosed: false }),
      ])
      expect(promises).toHaveLength(7)
    })
  })

  describe('should pass query parameters to api', () => {
    it('when listing all non-associations in pages', async () => {
      const emptyResponse: Page<NonAssociation> = {
        content: [],
        number: 0,
        totalPages: 0,
        totalElements: 0,
      }
      mockResponse()
        .get('/non-associations?includeOpen=true&includeClosed=false&page=2')
        .reply(200, emptyResponse)
        .get('/non-associations?includeOpen=true&includeClosed=false&size=100')
        .reply(200, emptyResponse)
        .get('/non-associations?includeOpen=true&includeClosed=false&page=1&size=200')
        .reply(200, emptyResponse)
        .get('/non-associations?includeOpen=true&includeClosed=false&page=345&sort=id')
        .reply(200, emptyResponse)
        .get('/non-associations?includeOpen=true&includeClosed=false&size=1&sort=whenCreated,DESC')
        .reply(200, emptyResponse)

      await Promise.all([
        client.pagedNonAssociations({ page: 2 }),
        client.pagedNonAssociations({ size: 100 }),
        client.pagedNonAssociations({ page: 1, size: 200 }),
        client.pagedNonAssociations({ sort: 'id', page: 345 }),
        client.pagedNonAssociations({ sort: 'whenCreated,DESC', size: 1 }),
      ])
      expect(nock.isDone()).toEqual(true)
    })
  })
})
