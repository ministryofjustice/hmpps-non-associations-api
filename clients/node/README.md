HMPPS Non-associations API NodeJS REST Client
=============================================

This library is designed to be used by DPS/HMPPS front-end applications that are based on
[hmpps-typescript-template](https://github.com/ministryofjustice/hmpps-template-typescript)
and need to access the non-associations api.

Using the library
-----------------

Typescript applications can install the library in several ways:

### GitHub Packages – npm registry

TODO

### GitHub Releases

TODO

### Usage

Applications would usually subclass the client:

```typescript
export class Client extends NonAssociationsApi {
  constructor(systemToken: string) {
    super(
      /**
       * Provide a system token with necessary roles, not a user token
       * READ_NON_ASSOCIATIONS and optionally WRITE_NON_ASSOCIATIONS
       */
      systemToken,
      
      /**
       * API configuration standard in DPS front-end apps
       */
      config.apis.hmppsNonAssociationsApi,

      /**
       * Logger such as standard library’s `console` or `bunyan` instance
       */
      logger,

      /**
       * Plugins for superagent requests, e.g. restClientMetricsMiddleware
       */
      [restClientMetricsMiddleware],
    )
  }
}
```

…and use the client in a request handler:

```typescript
async (req, res) => {
  const { user } = res.locals
  const systemToken = await hmppsAuthClient.getSystemClientToken(user.username)
  const api = new Client(systemToken)
  const nonAssociation = await api.getNonAssociation(nonAssociationId)
}
```

**NB: It is left to the application to determine which actions a user is allowed to perfom!**

General notes:

- All prison users can _view_ all non-associations
- Users with the `NON_ASSOCIATIONS` role can _add_, _update_ and _close_ non-associations for prisoners in any of their caseloads
- Users with the `GLOBAL_SEARCH` role can _add_, _update_ and _close_ non-associations for prisoners in transfer
- Users with the `INACTIVE_BOOKINGS` role can _add_, _update_ and _close_ non-associations for prisoners outside any establishment / released

Release a new version
---------------------

Do not change the version set in package.json, it should remain "0.0.0".

- Check the latest release version and choose the next semantic versioning numbers to use
- Tag the commit to release with `node-client-[version]` replacing `[version]` with the next version, e.g. "node-client-0.1.7"
- Create a release from the tag on GitHub
