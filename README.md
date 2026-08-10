# MellooEssentials

A lightweight [Fabric](https://fabricmc.net/) client mod for Hypixel: party/team highlighting, a
mod-native friends system, and cloud-synced settings - no sky.melloo.me account needed. Paired
with a companion page at [sky.melloo.me/download](https://sky.melloo.me/download).

Also the shared core [SkyMelloo](https://github.com/SkyMelloo/SkyMelloo) builds on - anything both
mods need (cosmetics, a few shared utilities) lives here once, so SkyMelloo requires this mod
installed alongside it.

Not an official Minecraft product. Not approved by or associated with Mojang, Microsoft, or
Hypixel Inc.

## Features

- **Highlighting** - party and team member glow, colored on the tab list and nametags.
- **SkyMelloo Friends** - a mod-native friend list with relay chat (key G). Uses only your
  Minecraft account's own identity - no sky.melloo.me account or login needed.
- **Cloud Saves** - your settings follow you to a new device/reinstall automatically. Same as
  Friends, this only ever uses your Minecraft account's own identity.
- Also includes purely cosmetic particle effects (halos, trails, auras, a physics-simulated cape),
  shared with nearby players also running the mod.

## Download

The official, signed build is only ever distributed from
**[sky.melloo.me/download](https://sky.melloo.me/download)**. If you got a jar from anywhere else,
it isn't an official release - see Building below to make your own from this source instead.

## Building

Requires JDK 25.

```
./gradlew build -PtestBuild=true                # test build, no key or changelog needed
./gradlew build -PchangelogFile=path.txt   # dev build, needs the signing key + a changelog
```

The build also runs `reportBuild`/`uploadJar` for sky.melloo.me release tracking - these fail
silently without the maintainer's own tokens, which live outside this repo. Test builds are
shareable - this is AGPL-3.0.

## Community

Bug reports and feature requests go through the [website's Report a Bug
form](https://sky.melloo.me/report-bug) or GitHub Issues here. See
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community expectations and
[SECURITY.md](SECURITY.md) if you've found a vulnerability.

## License

[GNU Affero General Public License v3.0](LICENSE). Copyright (C) 2026 Maja Bekurdts (hexedmaya).

## Contact

[sky.melloo.me/contact](https://sky.melloo.me/contact)
