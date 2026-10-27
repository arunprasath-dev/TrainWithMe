# Room schema exports

`room.schemaLocation` is set to this directory in `core/data/build.gradle.kts`, so KSP writes
`com.trainwithme.core.data.local.db.TrainWithMeDatabase/1.json` here on the first build.

Why it is committed:

- every schema change shows up as a reviewable diff;
- `MigrationTestHelper` (instrumented tests) needs the JSON of the *old* version to test a migration;
- a reviewer can see whether a version bump came with a migration instead of a destructive fallback.

Rules:

1. Never edit an exported schema by hand — let KSP regenerate it.
2. Bumping `@Database(version = …)` requires a `Migration` plus the new JSON in the same commit.
3. Destructive fallback stays off in release builds.
