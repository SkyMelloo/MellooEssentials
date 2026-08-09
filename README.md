# MellooEssentials

A [Fabric](https://fabricmc.net/) client mod for Hypixel: party and team member highlighting, plus
purely cosmetic particle effects. No account or login required. Paired with a companion page at
[sky.melloo.me/mellooessentials](https://sky.melloo.me/mellooessentials).

Also the shared core [SkyMelloo](https://github.com/SkyMelloo/SkyMelloo) builds on - anything both
mods need (cosmetics, a few shared utilities) lives here once, so SkyMelloo requires this mod
installed alongside it.

Not an official Minecraft product. Not approved by or associated with Mojang, Microsoft, or
Hypixel Inc.

## Features

- **Cosmetics** - dozens of purely cosmetic particle effects (halos, trails, auras, procedural
  shapes, a physics-simulated cape), shared with nearby players also running the mod.
- **Highlighting** - party and team member glow.
- **Party tools** - party membership tracking via Hypixel's own Mod API.

## Download

The official, signed build is only ever distributed from
**[sky.melloo.me/mellooessentials](https://sky.melloo.me/mellooessentials)**. If you got a jar from
anywhere else, it isn't an official release - see Building below to make your own from this source
instead.

## Building

Requires JDK 25.

```
./gradlew build -PtestBuild=true                # test build, no key or changelog needed
./gradlew build -PchangelogFile=path.txt   # dev build, needs the signing key + a changelog
```

The build also runs a couple of automated tasks (`reportBuild`, `uploadJar`) that talk to
sky.melloo.me for release tracking - these fail silently/non-fatally if you don't have the
maintainer's own tokens (which live outside this repo entirely and are never distributed with it).
You're free to share a test build with others too, by the way - this is AGPL-3.0, same as the rest
of the project.

## Community

Bug reports and feature requests go through the [website's Report a Bug
form](https://sky.melloo.me/report-bug) or GitHub Issues here. See
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community expectations and
[SECURITY.md](SECURITY.md) if you've found a vulnerability.

## License

[GNU Affero General Public License v3.0](LICENSE). Copyright (C) 2026 Maja Bekurdts (hexedmaya).

## Contact

[sky.melloo.me/contact](https://sky.melloo.me/contact)
