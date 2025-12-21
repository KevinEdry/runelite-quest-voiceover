# Changelog

## [1.4.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.3.0...v1.4.0) (2025-12-21)


### Features

* add X Marks the Spot quest transcript ([41969a9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/41969a964b1275da9e704f45abb2726fa0130ff2))
* **config:** reorganize settings into Quest Dialog and Quest List sections ([7cf9243](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7cf92438a78aabae2db70f3d8494baa3ecc48a3c))
* **database:** add getDatabaseVersion method to DatabaseVersionManager ([955a854](https://github.com/KevinEdry/runelite-quest-voiceover/commit/955a854b14b7ad4238e7a137aa63b2859e97838d))
* **database:** add isConnected method to DatabaseManager ([6a5caa6](https://github.com/KevinEdry/runelite-quest-voiceover/commit/6a5caa6b82270560388baa06f08cfe585a98d235))
* **dialog:** respect config toggles for mute button and quest name ([1a2b8ce](https://github.com/KevinEdry/runelite-quest-voiceover/commit/1a2b8ce4c41e27238b1bae3ed02abd67d13ece70))
* **logging:** change 'no voiceover found' log from debug to info ([a8a8713](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a8a8713233963e71b33f1c2babdf1a3323fbea92))
* **logging:** include dialog text and matched text in voiceover logs ([abb729b](https://github.com/KevinEdry/runelite-quest-voiceover/commit/abb729b240ba0664704ec4efc1c18c6a6c7baf2f))
* **logging:** log Levenshtein matches below threshold with best match ([a62e920](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a62e9204ebefd45a50c9abad52ed98791fae1360))
* **ui:** add info panel with plugin stats and quick links ([ab4bda1](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ab4bda145873856e9f4990b545cdf0330b66f2d7))


### Bug Fixes

* **matching:** lower Levenshtein threshold from 85% to 70% ([6aee813](https://github.com/KevinEdry/runelite-quest-voiceover/commit/6aee8133a54d08f92e172e80ad9fa081c7ca5a26))
* **matching:** remove FTS query that caused incorrect matches ([b6274f2](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b6274f2afdc653d7a164d37f873ae78674ecf668))

## [1.3.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.2.1...v1.3.0) (2025-12-21)


### Features

* improve dialog text matching with widget text and Levenshtein fallback ([#43](https://github.com/KevinEdry/runelite-quest-voiceover/issues/43)) ([c3143e9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/c3143e9fb4dab380ce02dccd13339458c0f6decb))

## [1.2.1](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.2.0...v1.2.1) (2025-12-20)


### Bug Fixes

* add missing lwjgl-bom module checksum to verification metadata ([#40](https://github.com/KevinEdry/runelite-quest-voiceover/issues/40)) ([00ff7b7](https://github.com/KevinEdry/runelite-quest-voiceover/commit/00ff7b7e82e234068b6fa8723641c1e9622000a4))

## [1.2.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.1.1...v1.2.0) (2025-12-20)


### Features

* add publish-plugin Claude command ([a5f72c9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a5f72c9d23c619bc401b720ef18ef2a873545c89))
* add quest list voice indicators and refactor codebase ([#13](https://github.com/KevinEdry/runelite-quest-voiceover/issues/13)) ([a8a303f](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a8a303f58cbe1beb7ca29f1483e93d59b2f1c7e3))
* add quest transcript JSON files ([643bffd](https://github.com/KevinEdry/runelite-quest-voiceover/commit/643bffd29337645a9bcfc6c1dda8b626ead48e07))
* add quest transcripts and update documentation ([9feae28](https://github.com/KevinEdry/runelite-quest-voiceover/commit/9feae28a4409c066306dcfe59cde199a0d5f9bbf))
* add Tutorial Island transcript and update command docs ([3955bac](https://github.com/KevinEdry/runelite-quest-voiceover/commit/3955bacba743daca1e9b23a4c28a22a42448c8dd))


### Bug Fixes

* Clear audio playlist regardless of playback state ([#12](https://github.com/KevinEdry/runelite-quest-voiceover/issues/12)) ([22a91a4](https://github.com/KevinEdry/runelite-quest-voiceover/commit/22a91a481f6adc309757249d5b9871feed4539a0))
* enable multi-threaded SQLite access ([7f63087](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7f63087aaf4d4f19d023f678606d96ac645a06b6))
