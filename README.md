# Romeo Plugins

## Current status

Verified build status: successful.

Command run:

```bash
./gradlew build --console=plain
```

Result:

- Gradle: 8.10.2
- Build result: BUILD SUCCESSFUL
- Duration: ~22s
- Modules built: common, paper, fabric

## Ready-to-use commands

From the project root:

```bash
# Full project build
./gradlew build

# Clean rebuild
./gradlew clean build

# Build only the shared library
./gradlew :common:build

# Build the Paper plugin jar
./gradlew :paper:shadowJar

# Build the Fabric mod jar
./gradlew :fabric:build
```

## Notes

- The root project includes the common, paper, and fabric modules.
- The Paper module uses shadowJar for the distributable plugin artifact.
- The Fabric module uses Loom and produces a remapped mod jar.
