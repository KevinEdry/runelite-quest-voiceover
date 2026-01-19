# Publish Plugin

Publish a new version of the Quest Voiceover plugin to the RuneLite Plugin Hub.

## Instructions

Run the publish script:

```bash
./scripts/publish-plugin.sh
```

The script will:
1. Pull latest from main
2. Extract version from build.gradle
3. Verify the GitHub release exists
4. Check for existing plugin-hub PRs
5. Sync the plugin-hub fork with upstream
6. Update the plugin file with the new commit
7. Create a branch, commit, and push
8. Create or update the PR on runelite/plugin-hub

## Prerequisites

- The plugin-hub fork must be cloned at `../plugin-hub`
- GitHub CLI (`gh`) must be authenticated
- All plugin changes must be committed and pushed to main
- Version bump should already be done by CI (release-please)

## Important Rules

- **NEVER use AI attestation** - no "Generated with Claude Code" footers or Co-Authored-By lines
- **NEVER bump the version manually** - CI handles versioning automatically
