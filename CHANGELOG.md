# Changelog

All notable changes to this plugin are documented in this file. The format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Unstable, testing and stable stay moving pointers to whichever build was
last published to each; every build they ever point at also gets a
permanent release of its own (`<bundle>-build.<run>`), which is never
overwritten.

## [Unreleased]

### Added

- A permanent, never-overwritten release for every bundle build published
  to any channel, so a version that was once installable stays that way in
  the release history even after the next push moves the channel pointers.

## [1.0.0] - 2026-09-28

### Changed

- Promoted to the stable channel at version 1.0.0. The maintainer's decision of
  2026-09-28 promotes every plugin line that has already run a real game
  from testing, without the usual promotion checks. CI stamps its run
  number as the third component of the published version (1.0.<run>), so
  every build stays newer than the one before it.

## [0.1] - 2026-09-28

This is the plugin's first version-history entry: there was no changelog
before this release, so this section summarizes what already exists.

### Added

- Runs RPG Maker MV and MZ games from a Windows or web deploy, served to
  Android's WebView over a private origin confined to the game's folder.
- Per-game save storage backed by a file in Enginehost's save folder for
  the game, so MV's direct saves and MZ's localforage both persist and
  never collide between games.
- Automatic `.m4a`/`.ogg` audio fallback for decks that only shipped one
  format.
- Signed bundle releases on unstable, testing and stable channels, verified
  file by file against this repository's pinned key at install time.
