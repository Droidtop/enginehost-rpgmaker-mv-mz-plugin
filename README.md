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

## Releases

GitHub Releases form the catalog Enginehost reads. Every build is signed;
`enginehost-public-key.json` is the repository key Enginehost pins before
accepting a bundle. Builds publish on the unstable channel on every push, and
are promoted to testing and stable by hand once they have run real games.

MIT-licensed. The RPG Maker runtime and the game's assets are the game's own
and are not redistributed.
