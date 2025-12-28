# Changelog

## [1.8.1](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.8.0...v1.8.1) (2025-12-28)


### Code Refactoring

* migrate database schema from FTS4 to regular table with indexes ([2941542](https://github.com/KevinEdry/runelite-quest-voiceover/commit/2941542b503c951c0e9b67b019a4b0bcba07c441))

## [1.8.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.7.0...v1.8.0) (2025-12-28)


### Features

* add quest transcript extraction script and transcripts ([fecbf76](https://github.com/KevinEdry/runelite-quest-voiceover/commit/fecbf76732354d24f8d74cbbab027fcdb6ff4f84))

## [1.7.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.6.0...v1.7.0) (2025-12-24)


### Features

* add audio ducking config options ([f8f4405](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f8f440588146d037fb3d7080cda03e9b86f95dae))
* add AudioChannelsManager for game volume control ([d553f2d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/d553f2d24806d6983f40a34f0b0ee2374b2805bd))
* add AudioDuckingManager for game audio ducking ([0230bf0](https://github.com/KevinEdry/runelite-quest-voiceover/commit/0230bf00a0574721e7c09917d8110cd09815bc2b))
* add ColorUtility for OSRS color tag functions ([a5b268d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a5b268d826b02188b9dac9dac4cb81e65c506736))
* add Constants for shared project constants ([56e8878](https://github.com/KevinEdry/runelite-quest-voiceover/commit/56e8878d954e9830209a4dadc3191cc3df61888a))
* add DialogSpeechHighlightHandler for speech highlighting ([2b20a1d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/2b20a1df08d7cca9872bbf6b9743c4421a27ac2e))
* add publish-plugin Claude command ([a5f72c9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a5f72c9d23c619bc401b720ef18ef2a873545c89))
* add quest list voice indicators and refactor codebase ([#13](https://github.com/KevinEdry/runelite-quest-voiceover/issues/13)) ([6e9f0e3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/6e9f0e32a294f91f65071e70887adfd138294cbd))
* add quest transcript JSON files ([643bffd](https://github.com/KevinEdry/runelite-quest-voiceover/commit/643bffd29337645a9bcfc6c1dda8b626ead48e07))
* add quest transcripts and update documentation ([f26e00d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f26e00d172f70ef766500b57d6f5535a404d071c))
* add quest transcripts for 13 quests ([f0eec83](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f0eec83733d578c2d42000956c7f052886d36b68))
* add Skippy and the Mogres transcript ([bd0fbee](https://github.com/KevinEdry/runelite-quest-voiceover/commit/bd0fbee8fa67d553c03441f98c2fe4eb90521a40))
* add TextUtility for text manipulation functions ([a83d8e1](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a83d8e1e6ce53bcdef464c66ea61d486363d10b9))
* add Tutorial Island transcript and update command docs ([42a62ee](https://github.com/KevinEdry/runelite-quest-voiceover/commit/42a62ee246b3e37acbdd1bb2aa655a9620729153))
* add X Marks the Spot quest transcript ([eee1340](https://github.com/KevinEdry/runelite-quest-voiceover/commit/eee13405493aef263bd368b579c0a686d755e1d5))
* **config:** reorganize settings into Quest Dialog and Quest List sections ([02f003e](https://github.com/KevinEdry/runelite-quest-voiceover/commit/02f003eb156b1866eba4113c2aa8d74c51bc86ef))
* **database:** add getDatabaseVersion method to DatabaseVersionManager ([99fbf17](https://github.com/KevinEdry/runelite-quest-voiceover/commit/99fbf1731efc89c07ebc70ba3be54c80d0909735))
* **database:** add isConnected method to DatabaseManager ([eec834b](https://github.com/KevinEdry/runelite-quest-voiceover/commit/eec834be5519480d70b88d75b7c0081fb8f7e58c))
* **dialog:** respect config toggles for mute button and quest name ([8f419c3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/8f419c3e5e851a2155c8fdfb4d3a2b16940fe517))
* improve dialog text matching with widget text and Levenshtein fallback ([#43](https://github.com/KevinEdry/runelite-quest-voiceover/issues/43)) ([86d515d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/86d515df22c9ac23af489a383215213be6967024))
* **logging:** change 'no voiceover found' log from debug to info ([bff49a3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/bff49a33a2d460fe48e31698613ddb3a5f6d880a))
* **logging:** include dialog text and matched text in voiceover logs ([b9b65d9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b9b65d9f35ac2ff46e1ba7b3212a3567fa5d1fcb))
* **logging:** log Levenshtein matches below threshold with best match ([d23217f](https://github.com/KevinEdry/runelite-quest-voiceover/commit/d23217f39190faeb9d03c3863425251ae2b0c69e))
* **ui:** add info panel with plugin stats and quick links ([20b3080](https://github.com/KevinEdry/runelite-quest-voiceover/commit/20b3080d259fa902cf761b7f2ddb285ba08f48f4))


### Bug Fixes

* add missing lwjgl-bom module checksum to verification metadata ([#40](https://github.com/KevinEdry/runelite-quest-voiceover/issues/40)) ([ce89b59](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ce89b594df43c0ed8b721c98b83ba6597f64fcac))
* Clear audio playlist regardless of playback state ([#12](https://github.com/KevinEdry/runelite-quest-voiceover/issues/12)) ([22a91a4](https://github.com/KevinEdry/runelite-quest-voiceover/commit/22a91a481f6adc309757249d5b9871feed4539a0))
* enable multi-threaded SQLite access ([7f63087](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7f63087aaf4d4f19d023f678606d96ac645a06b6))
* **matching:** lower Levenshtein threshold from 85% to 70% ([509d9b7](https://github.com/KevinEdry/runelite-quest-voiceover/commit/509d9b75833fd42b97e672d6f80109d6fb6c26ae))
* **matching:** remove FTS query that caused incorrect matches ([9d57417](https://github.com/KevinEdry/runelite-quest-voiceover/commit/9d57417804b7c29842a4025d588f6db571cb171e))
* stop audio when no voiceover found for dialog ([e25b0a9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/e25b0a90b5b64222f98b404bfebd732d40ecbaa3))


### Code Refactoring

* apply early return pattern to reduce nesting ([9c89050](https://github.com/KevinEdry/runelite-quest-voiceover/commit/9c89050eb62bddcf42dc02b4009a9ac4327fe5e8))
* remove unused playerName param from getDialogCharacterName ([3507679](https://github.com/KevinEdry/runelite-quest-voiceover/commit/35076790fe4823b315a617c44c33de91ae748f49))
* rename config options from karaoke to speech highlighting ([ac25b3a](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ac25b3a8fcb528e95095284f1c8dffe47160e3a1))
* rename HashUtil to HashUtility ([599af5d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/599af5d8a46141ab4a5fc9aef60f5df1f3856e2b))
* rename MessageParser to MessageUtility with stateless design ([541a5e2](https://github.com/KevinEdry/runelite-quest-voiceover/commit/541a5e2b33007372073be999d4c96670dc23a449))
* rename SoundEngine to AudioManager ([b0db370](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b0db370b9a9906af62e0d61f4e28e91b71534a50))
* simplify AudioManager and use Constants ([8473edb](https://github.com/KevinEdry/runelite-quest-voiceover/commit/8473edbd7f736547da64fdfc4b1a2f77c675e738))
* update DatabaseVersionManager to use Constants ([18f49c3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/18f49c39178ccf37f760cbcc875121bc106ebb0c))
* update DialogManager to use ColorUtility and Constants ([b0aaa51](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b0aaa5121b48ba989ac7967bca3ab9745e916f69))
* update MessageUtility to use TextUtility and Constants ([73f2b9a](https://github.com/KevinEdry/runelite-quest-voiceover/commit/73f2b9ada0a9bdccf2aefa3c8f7afa92a339b7f6))
* update plugin to use AudioManager and AudioDuckingManager ([201cea3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/201cea37de8447600c5feb263090e12e2c8a47c2))
* update VoiceoverHandler to use AudioManager ([de000b3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/de000b317b8bc2772316d143f1c702a8161ae184))
* update VoiceoverHandler to use DialogSpeechHighlightHandler ([7fabce9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7fabce9f0c4c3031d6b4351a6b85b7e2e13ae5cc))

## [1.6.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.5.0...v1.6.0) (2025-12-24)


### Features

* add audio ducking config options ([f8f4405](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f8f440588146d037fb3d7080cda03e9b86f95dae))
* add AudioChannelsManager for game volume control ([d553f2d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/d553f2d24806d6983f40a34f0b0ee2374b2805bd))
* add AudioDuckingManager for game audio ducking ([0230bf0](https://github.com/KevinEdry/runelite-quest-voiceover/commit/0230bf00a0574721e7c09917d8110cd09815bc2b))
* add ColorUtility for OSRS color tag functions ([a5b268d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a5b268d826b02188b9dac9dac4cb81e65c506736))
* add Constants for shared project constants ([56e8878](https://github.com/KevinEdry/runelite-quest-voiceover/commit/56e8878d954e9830209a4dadc3191cc3df61888a))
* add DialogSpeechHighlightHandler for speech highlighting ([2b20a1d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/2b20a1df08d7cca9872bbf6b9743c4421a27ac2e))
* add publish-plugin Claude command ([a5f72c9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a5f72c9d23c619bc401b720ef18ef2a873545c89))
* add quest list voice indicators and refactor codebase ([#13](https://github.com/KevinEdry/runelite-quest-voiceover/issues/13)) ([6e9f0e3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/6e9f0e32a294f91f65071e70887adfd138294cbd))
* add quest transcript JSON files ([643bffd](https://github.com/KevinEdry/runelite-quest-voiceover/commit/643bffd29337645a9bcfc6c1dda8b626ead48e07))
* add quest transcripts and update documentation ([f26e00d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f26e00d172f70ef766500b57d6f5535a404d071c))
* add quest transcripts for 13 quests ([f0eec83](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f0eec83733d578c2d42000956c7f052886d36b68))
* add Skippy and the Mogres transcript ([bd0fbee](https://github.com/KevinEdry/runelite-quest-voiceover/commit/bd0fbee8fa67d553c03441f98c2fe4eb90521a40))
* add TextUtility for text manipulation functions ([a83d8e1](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a83d8e1e6ce53bcdef464c66ea61d486363d10b9))
* add Tutorial Island transcript and update command docs ([42a62ee](https://github.com/KevinEdry/runelite-quest-voiceover/commit/42a62ee246b3e37acbdd1bb2aa655a9620729153))
* add X Marks the Spot quest transcript ([eee1340](https://github.com/KevinEdry/runelite-quest-voiceover/commit/eee13405493aef263bd368b579c0a686d755e1d5))
* **config:** reorganize settings into Quest Dialog and Quest List sections ([02f003e](https://github.com/KevinEdry/runelite-quest-voiceover/commit/02f003eb156b1866eba4113c2aa8d74c51bc86ef))
* **database:** add getDatabaseVersion method to DatabaseVersionManager ([99fbf17](https://github.com/KevinEdry/runelite-quest-voiceover/commit/99fbf1731efc89c07ebc70ba3be54c80d0909735))
* **database:** add isConnected method to DatabaseManager ([eec834b](https://github.com/KevinEdry/runelite-quest-voiceover/commit/eec834be5519480d70b88d75b7c0081fb8f7e58c))
* **dialog:** respect config toggles for mute button and quest name ([8f419c3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/8f419c3e5e851a2155c8fdfb4d3a2b16940fe517))
* improve dialog text matching with widget text and Levenshtein fallback ([#43](https://github.com/KevinEdry/runelite-quest-voiceover/issues/43)) ([86d515d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/86d515df22c9ac23af489a383215213be6967024))
* **logging:** change 'no voiceover found' log from debug to info ([bff49a3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/bff49a33a2d460fe48e31698613ddb3a5f6d880a))
* **logging:** include dialog text and matched text in voiceover logs ([b9b65d9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b9b65d9f35ac2ff46e1ba7b3212a3567fa5d1fcb))
* **logging:** log Levenshtein matches below threshold with best match ([d23217f](https://github.com/KevinEdry/runelite-quest-voiceover/commit/d23217f39190faeb9d03c3863425251ae2b0c69e))
* **ui:** add info panel with plugin stats and quick links ([20b3080](https://github.com/KevinEdry/runelite-quest-voiceover/commit/20b3080d259fa902cf761b7f2ddb285ba08f48f4))


### Bug Fixes

* add missing lwjgl-bom module checksum to verification metadata ([#40](https://github.com/KevinEdry/runelite-quest-voiceover/issues/40)) ([ce89b59](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ce89b594df43c0ed8b721c98b83ba6597f64fcac))
* Clear audio playlist regardless of playback state ([#12](https://github.com/KevinEdry/runelite-quest-voiceover/issues/12)) ([22a91a4](https://github.com/KevinEdry/runelite-quest-voiceover/commit/22a91a481f6adc309757249d5b9871feed4539a0))
* enable multi-threaded SQLite access ([7f63087](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7f63087aaf4d4f19d023f678606d96ac645a06b6))
* **matching:** lower Levenshtein threshold from 85% to 70% ([509d9b7](https://github.com/KevinEdry/runelite-quest-voiceover/commit/509d9b75833fd42b97e672d6f80109d6fb6c26ae))
* **matching:** remove FTS query that caused incorrect matches ([9d57417](https://github.com/KevinEdry/runelite-quest-voiceover/commit/9d57417804b7c29842a4025d588f6db571cb171e))
* stop audio when no voiceover found for dialog ([e25b0a9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/e25b0a90b5b64222f98b404bfebd732d40ecbaa3))


### Code Refactoring

* apply early return pattern to reduce nesting ([9c89050](https://github.com/KevinEdry/runelite-quest-voiceover/commit/9c89050eb62bddcf42dc02b4009a9ac4327fe5e8))
* remove unused playerName param from getDialogCharacterName ([3507679](https://github.com/KevinEdry/runelite-quest-voiceover/commit/35076790fe4823b315a617c44c33de91ae748f49))
* rename config options from karaoke to speech highlighting ([ac25b3a](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ac25b3a8fcb528e95095284f1c8dffe47160e3a1))
* rename HashUtil to HashUtility ([599af5d](https://github.com/KevinEdry/runelite-quest-voiceover/commit/599af5d8a46141ab4a5fc9aef60f5df1f3856e2b))
* rename MessageParser to MessageUtility with stateless design ([541a5e2](https://github.com/KevinEdry/runelite-quest-voiceover/commit/541a5e2b33007372073be999d4c96670dc23a449))
* rename SoundEngine to AudioManager ([b0db370](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b0db370b9a9906af62e0d61f4e28e91b71534a50))
* simplify AudioManager and use Constants ([8473edb](https://github.com/KevinEdry/runelite-quest-voiceover/commit/8473edbd7f736547da64fdfc4b1a2f77c675e738))
* update DatabaseVersionManager to use Constants ([18f49c3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/18f49c39178ccf37f760cbcc875121bc106ebb0c))
* update DialogManager to use ColorUtility and Constants ([b0aaa51](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b0aaa5121b48ba989ac7967bca3ab9745e916f69))
* update MessageUtility to use TextUtility and Constants ([73f2b9a](https://github.com/KevinEdry/runelite-quest-voiceover/commit/73f2b9ada0a9bdccf2aefa3c8f7afa92a339b7f6))
* update plugin to use AudioManager and AudioDuckingManager ([201cea3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/201cea37de8447600c5feb263090e12e2c8a47c2))
* update VoiceoverHandler to use AudioManager ([de000b3](https://github.com/KevinEdry/runelite-quest-voiceover/commit/de000b317b8bc2772316d143f1c702a8161ae184))
* update VoiceoverHandler to use DialogSpeechHighlightHandler ([7fabce9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/7fabce9f0c4c3031d6b4351a6b85b7e2e13ae5cc))

## [1.5.0](https://github.com/KevinEdry/runelite-quest-voiceover/compare/v1.4.0...v1.5.0) (2025-12-24)


### Features

* add audio ducking config options ([dc812d2](https://github.com/KevinEdry/runelite-quest-voiceover/commit/dc812d286922111f7e1154bff89d6ab93dca3977))
* add AudioChannelsManager for game volume control ([6ea1540](https://github.com/KevinEdry/runelite-quest-voiceover/commit/6ea1540c3041d588e41b76d8a6536a1febb1d1d4))
* add AudioDuckingManager for game audio ducking ([a4a9273](https://github.com/KevinEdry/runelite-quest-voiceover/commit/a4a927367596985b2d914c05e8f9e17b02ba98d8))
* add quest transcripts for 13 quests ([0fb3142](https://github.com/KevinEdry/runelite-quest-voiceover/commit/0fb3142c9aa2ef30dc98ffaa6895b76e64a45731))
* add Skippy and the Mogres transcript ([b105411](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b1054110d00610664e524bfc57e82ab6b55b491f))


### Bug Fixes

* stop audio when no voiceover found for dialog ([f204975](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f204975486ca4d02ae8bbae48449e70edc22fea7))


### Code Refactoring

* apply early return pattern to reduce nesting ([1ac36dc](https://github.com/KevinEdry/runelite-quest-voiceover/commit/1ac36dc5eda04084e9ce4909d63510cd7c6671dd))
* remove unused playerName param from getDialogCharacterName ([06e9b30](https://github.com/KevinEdry/runelite-quest-voiceover/commit/06e9b30852245e31ccc16cfd34cc9b325d9a21d1))
* rename HashUtil to HashUtility ([3ae1e81](https://github.com/KevinEdry/runelite-quest-voiceover/commit/3ae1e81eec50fbca47e2fa3fde166cccbc1c26b1))
* rename MessageParser to MessageUtility with stateless design ([ee9d0df](https://github.com/KevinEdry/runelite-quest-voiceover/commit/ee9d0df973ecf7c713e495cde3cd53cefd5def6c))
* rename SoundEngine to AudioManager ([68393e6](https://github.com/KevinEdry/runelite-quest-voiceover/commit/68393e604463cef3c5426a4719609a0b658b541d))
* update plugin to use AudioManager and AudioDuckingManager ([b5b655b](https://github.com/KevinEdry/runelite-quest-voiceover/commit/b5b655b89d419056f58e9bb3febacd5ece363c45))
* update VoiceoverHandler to use AudioManager ([f7714a9](https://github.com/KevinEdry/runelite-quest-voiceover/commit/f7714a9ca0cbf962ebc01eac1ea7da90d024c042))

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
