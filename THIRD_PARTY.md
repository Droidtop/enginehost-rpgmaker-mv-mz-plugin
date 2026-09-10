# Third-party components

This repository is a thin Android WebView wrapper (a `localStorage` save
bridge, audio-format fallback, and a private `https` origin) around RPG Maker
MV/MZ web deploys. It does not vendor the RPG Maker MV/MZ runtime or any
engine source: a game's own `www` folder (which contains RPG Maker's runtime
JS, e.g. `rpg_core.js`/`rmmz_core.js` and its own bundled libraries such as
pixi.js and localforage) is supplied by the game at launch, never redistributed
by this repository.

| Component | Version / commit | Licence | Source | Where in tree |
|---|---|---|---|---|
| Enginehost's own wrapper (WebView bridge, save shim, audio fallback) | this repository | MIT | https://github.com/Droidtop/enginehost-rpgmaker-mv-mz-plugin | entire tree |
| RPG Maker MV/MZ runtime, pixi.js, localforage | supplied per-game at launch | not applicable (not vendored) | shipped inside each game's own `www` folder | never present in this repository |

## Obligations

None beyond MIT's notice-preservation requirement for this repository's own
code; no third-party runtime or engine source is bundled or redistributed.
