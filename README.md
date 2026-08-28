# enginehost RPG Maker MV/MZ plugin

Programmatically hosts deployed RPG Maker MV and MZ games in place through an
Android WebView. This is one independently installable runtime in enginehost's
shared `rpgmaker` plugin family; XP/VX/VX Ace and 2000/2003 live in their own
engine forks.

The game supplies its released RPG Maker JavaScript runtime and assets. The
plugin supplies the Android web host, validates the requested context and entry
point, confines local navigation to the game directory, and defaults network
access off. No game files are copied or modified.

Supported options are `entryPoint`, `allowNetwork`, `domStorage`, `database`,
`mediaPlaybackRequiresUserGesture`, `userAgent`, and
`webContentsDebugging`. A top-level `execFile` takes precedence over the option
entry point after enginehost has completed its authoritative folder-first
configuration merge.

The first build declares only MV 1.6.2 and MZ 1.9.0. Wider spans will be added
only after game-level verification. Android builds run in GitHub Actions only.
