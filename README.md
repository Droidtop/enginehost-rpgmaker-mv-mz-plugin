# Enginehost RPG Maker MV/MZ plugin

Runs RPG Maker MV and MZ games from their own folder: a Windows deploy (the
`www` folder is all the runtime needs) or a web deploy.

The game is served to Android's WebView over a private `https` origin, which
is what its scripts expect of a web deploy, while staying confined to the game
folder. Network access is off unless the game's config turns it on.

## Saves live on disk

A WebView keeps `localStorage` in the app's private data and shares one store
between every page it opens, so two games would overwrite each other's saves.
This plugin replaces the page's `localStorage` before the game's first script
runs with a store backed by `localStorage.json` in the save folder Enginehost
chose for the game (its folder name, written into `enginehost.json` as
`saveFolder`). MV saves into it directly; MZ's localforage is pointed at it.

## Audio

RPG Maker MV asks a mobile browser for `.m4a` audio and a Windows deploy
ships only `.ogg`. When a requested audio file does not exist and its sibling
in the other format does, the sibling is served.

## Options

`allowNetwork`, `entryPoint`, `mediaPlaybackRequiresGesture`, `userAgent`,
`webContentsDebugging`; see `enginehost/bundle-metadata.json`.

## Branches

This plugin has no upstream engine: the RPG Maker runtime is the game's own
JavaScript, so everything here is the wrapper. `plugin-core` is the wrapper and
this repository's main line (owner, 2026-10-14: its main branch is its
plugin-core; `main` stays at its last commit as history). The line
`plugin/stable` is generated, never edited: it is the empty tree plus
plugin-core, pinned in its `enginehost/line.json` and checked on every push by
`.github/workflows/plugin-line.yml` (Enginehost docs/plugin-lines.md). A change
lands on plugin-core and the line is recomposed with droidtop-platforms
`tools/plugin_line.py compose stable --branch plugin/stable`.

## Releases

GitHub Releases form the catalog Enginehost reads. Every build is signed;
`enginehost-public-key.json` is the repository key Enginehost pins before
accepting a bundle. Builds of `plugin/stable` publish on the unstable channel on every push, and
are promoted to testing and stable by hand once they have run real games.

GPL-3.0 licensed. The RPG Maker runtime and the game's assets are the game's own
and are not redistributed.

## Licence

GPL-3.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). All code here is ours; the third-party attributions in NOTICE and THIRD_PARTY.md keep their own notices.
