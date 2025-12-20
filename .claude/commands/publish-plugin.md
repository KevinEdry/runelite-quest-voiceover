# Publish Plugin

Publish a new version of the Quest Voiceover plugin to the RuneLite Plugin Hub.

**Input:** $ARGUMENTS (optional: version number override, e.g., "1.2.0")

## Prerequisites

- The plugin-hub fork must be cloned at `../plugin-hub`
- GitHub CLI (`gh`) must be authenticated
- All plugin changes must be committed and pushed to main

## Important Rules

- **NEVER use AI attestation** - no "Generated with Claude Code" footers or Co-Authored-By lines in any commits or PRs

## Instructions

1. **Get the currently published commit:**
   - Fetch `https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/quest-voiceover`
   - Extract the `commit=` value

2. **Determine the new version:**
   - If version provided in arguments, use that
   - Otherwise, analyze commits between published commit and HEAD:
     - Get current version from `build.gradle`
     - If any commit starts with `feat:` or adds new functionality → bump minor version
     - If any commit starts with `fix:` or `chore:` or `docs:` → bump patch version
     - If any commit contains `BREAKING` or starts with `feat!:` → bump major version

3. **Update version in build.gradle:**
   - Find the line `version = 'X.X.X'`
   - Update it to the new version number

4. **Commit and push the version bump:**
   - Stage build.gradle
   - Commit with message: `chore: bump version to {version}`
   - Push to origin main

5. **Get the new commit hash:**
   - Run `git rev-parse HEAD` to get the full 40-character hash

6. **Sync plugin-hub fork with upstream:**
   - Navigate to `../plugin-hub`
   - Add upstream remote if not exists: `git remote add upstream https://github.com/runelite/plugin-hub.git`
   - Fetch upstream: `git fetch upstream`
   - Checkout master: `git checkout master`
   - Reset to upstream: `git reset --hard upstream/master`

7. **Update the plugin file:**
   - Edit `plugins/quest-voiceover`
   - Update the `commit=` line with the new commit hash

8. **Create branch and commit:**
   - Create branch: `git checkout -b update-quest-voiceover`
   - Stage the plugin file
   - Commit with message: `quest-voiceover {version}`

9. **Push and create PR:**
   - Push branch: `git push -u origin update-quest-voiceover --force`
   - Create PR to upstream with:
     - Title: `Update quest-voiceover plugin to v{version}`
     - Body: Use template below

10. **PR body template:**
```
Update quest-voiceover plugin to version {version}.

Changes:
- [List relevant changes from git log between published commit and new commit]
```

11. **Output the PR URL** when complete

## Additional Notes

- If a PR already exists for the branch, the force-push will update it automatically
- The plugin-hub fork should be at `../plugin-hub` relative to this repo
- After creating the PR, return to the original repo directory
