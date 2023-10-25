HMPPS Non-associations API NodeJS REST Client
=============================================

This library is designed to be used by DPS/HMPPS front-end applications that are based on
[hmpps-typescript-template](https://github.com/ministryofjustice/hmpps-template-typescript)
and need to access the non-associations api.

Using the library
-----------------

Typescript applications can install the library in several ways:

### Install from GitHub Releases

This is the recommended method currently.

Find the [latest release version](https://github.com/ministryofjustice/hmpps-non-associations-api/releases)
and copy the link to the `node-client.tgz` asset.

```shell
npm install --save [link to asset]
```

Pros:
- easy to install and requires no authentication

Cons:
- dependency upgrade tools are unlikely to notice new releases

### Install from GitHub Packages – npm registry

Unfortunately, GitHub Packages requires authentication to pull from public npm registries,
so the setup here is more complex.

In the application repository, create `.npmrc` with:

```text
registry=https://registry.npmjs.org/
@ministryofjustice:registry=https://npm.pkg.github.com
```

Create a classic GitHub personal access token with at least `read:packages` scope, adding it to `~/.npmrc`:

```text
//npm.pkg.github.com/:_authToken=[token]
```

Install library by adding the
[latest release version](https://github.com/ministryofjustice/hmpps-non-associations-api/releases)
to `package.json`:

```text
"@ministryofjustice/hmpps-non-associations-api": "[latest version number]"
```

Cons:
- requires authentication when using locally and in CI other than GitHub Actions
- it’s unclear whether dependency upgrade tools can process new releases

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

General notes regarding permissions and roles:

- All prison users, i.e. those with the `PRISON` role, can _view_ all non-associations
- Users with the `NON_ASSOCIATIONS` role can _add_, _update_ and _close_ non-associations for prisoners both in a prison in any of their caseloads
- Users also having the `GLOBAL_SEARCH` role can also _add_, _update_ and _close_ non-associations for prisoners in transfer and where one prisoner is not in a prison that’s not in their caseloads
- Users also having the `INACTIVE_BOOKINGS` role can also _add_, _update_ and _close_ non-associations for prisoners outside any establishment / released
- Users must _close_ rather than _delete_ non-associations
- No users should be able to _add_, _update_ or _close_ non-associations for prisoners without a booking / with a null location

Release a new version
---------------------

Do not change the version set in package.json, it should remain "0.0.0".

- Check the [latest release version](https://github.com/ministryofjustice/hmpps-non-associations-api/releases)
  and choose the next semantic versioning numbers to use
- Tag the commit (on the main branch) to release
  with `node-client-[version]` replacing `[version]` with the next version,
  e.g. "node-client-0.1.7"
- Create a release from the tag on GitHub
