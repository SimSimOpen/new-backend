# SimSim backend

Spring Boot 4.1 / Java 21 modular monolith for the SimSim marketplace (api.simsim.fit).
Frontends live in separate repos: `sim-sim-client` (public site) and `sim-sim-realtor` (agent/admin panel).

## Workflow — read first
- Work on a personal branch (e.g. `batyr/dev`) and open a PR into `main`.
- CI on PR/`main` runs `./gradlew clean build`; it must pass before merging.
- **Deploy happens only on a `v*` tag** (`.github/workflows/main.yml`): the tag builds the image,
  pushes it to GHCR (`latest`, the tag name, `sha-…`) and redeploys the VPS. Pushes to `main` only
  build.
- Schema changes go through Hibernate `ddl-auto=update`: new tables/columns appear on boot, but
  nothing is ever dropped or renamed, and seeders never rewrite existing rows. Note any manual SQL a
  deploy needs in the PR description.

## Build & run
```bash
./gradlew compileJava          # quick check
./gradlew clean build          # what CI runs
./gradlew bootJar -x test      # build/libs/simsim-0.0.1-SNAPSHOT.jar
```
Local config: copy `src/main/resources/application-local.properties.example` to
`application-local.properties` (gitignored). Startup needs Postgres (`sim-sim` DB), Redis and an
S3/MinIO endpoint; without MinIO keys the app fails on the `minioClient` bean, and an empty
`jwt.secret` silently breaks auth (every token → 403).

## Modules
| Module | What |
|---|---|
| `src` (root) | app shell: `SecurityConfig` (public routes), `JwtFilter`, CORS, cross-module API impls |
| `common` | shared DTOs, enums, `*Api` interfaces between modules, `UserContext` |
| `auth`, `profile` | users, JWT, registration |
| `product` | listings: real estate (`property`), cars (`car`), categories, locations |
| `media` | photo upload pipeline → webp + watermark → MinIO (async, results reported via SSE) |
| `notification`, `notify` | SMS/Telegram OTP, operator feed |

Modules talk only through interfaces in `common/.../api` (e.g. `ProductApi`), never by importing
each other's internals.

## Conventions
- Controllers under `/api/<module>/v1/...`. A new public (anonymous) endpoint must be added to
  `SecurityConfig`; everything else requires a JWT.
- Errors: throw `UserException` (rendered as 400).
- Mapping: MapStruct mappers in `product/.../mapper`; partial updates use
  `NullValuePropertyMappingStrategy.IGNORE` (see `CarMapper`).
- Responses expose location as `location: [region, district, place, address]` in the request
  language (`LocationNames.currentLang()`).
- Comments explain *why*, not *what*; match the density of the surrounding code.

## Security rules for listings
Follow the cars implementation (`CarServiceImpl`):
- public `filter` returns only `ACTIVE` listings, whatever `listingStatus` is requested;
- agents see only their own listings, admins see all;
- only the owning agent or an admin may update, delete or add/remove photos;
- ownership for uploads is checked on the request thread, before the async upload is queued
  (`UserContext` is empty on async threads).
Real estate (`PropertyServiceImpl`, `PropertyFilterServiceImpl`) follows the same rules.

## Docs
- `docs/cars-backend-report.md` — cars category: API, security fixes, open issues in real estate.
