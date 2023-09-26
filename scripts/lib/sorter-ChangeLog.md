# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.10.0] - 2023-02-03
### Changed/Added
- seriesBasenameGenerator() - Renamed from basenameGenerator
- Added movieBasenameGenerator()
- Rework Movie Keyword Regex for #1, #2 and #4

## [1.9.5] - 2023-01-30
### Added
- searchForMoviesJWD() - Add Series Syntax Movie Variation
- renameOptionsForEpisodesUsingAnimeLists() - First 3rd RenamePass addition
- renameOptionForAniDBAbsoluteEpisodes() - First 3rd RenamePass addition

## [1.9.4] - 2023-01-20
### Added
- groupOverrides() added fine logging and changed the match from .equals() to a .each loop with logging (fine)

## [1.9.3] - 2023-01-16
### Added
- basenameGenerator() - If there is an AltTitle iterate over both alttitle and basename (group.anime) to compile list of baseAnimeNames

## [1.9.2] - 2023-01-15
### Added
- seriesNameGenerator() - Fixed bug when anime is using romain ordinal series format by referencing non-existant group variable.

## [1.9.1] - 2022-04-09
### Added
- When episode # is null, also log filename at log.info level

## [1.9.0] - 2022-02-27
### Added
- groupOverrides() - Added so we can externalize some of the complex match/set overrides in Series basename generation to an override json file

## [1.8.0] - 2022-02-23
### Changed
- searchForMoviesJWD() - Moved Alt Title checks so they add additional baseGeneratedAnimeNames   
- searchForMoviesJWD() - Added group.anime name back into tempBaseGeneratedAnimeNames

## [1.7.3] - 2022-01-14
### Changed
- baseNameGenerator() - Changed series numerial syntax in switch to [0-9]+ to match integer series, before defaulting to romanordinal

## [1.7.2] - 2021-12-27
### Changed
- searchForMoviesJWD() - Added switch for changing tempAnimeName based on group.animename
- searchForMoviesJWD() - Added Alt Movie #4 - Adding 'the' before 'movie', when it only has 'movie'

## [1.7.1] - 2021-12-26
### Changed
- episodeRenameOptionsPassOne() - Line 3991 - Switch `if ( animeFoundInAniDB && firstTVDBDWTMatchNumber < 0.9800000000000000000 )` to `if ( animeFoundInAniDB && firstTVDBDWTMatchNumber <= 0.9800000000000000000 )`
- basenameGenerator() & searchForMoviesJWD() - Check for v{Digit} at end of anime name and add additioinal basename without it. aka  when anime series v2 is detected, add additional basename of anime series
- ovaOnaOadSpecialBonusSyntaxMatcher - Modify Regex

## [1.7.0] - 2021-12-24
### Changed
- basenameGenerator() - Redid the Roman Ordinal's to generate alternatives for Roman Ordinals (2nd, II, 2) just like with interger seasons
- basenameGenerator() - Added getNumberFromRomanOrdinal() to get integer equiv of Roman Ordinal
- basenameGenerator() - Apply seriesNameGenerator() to both basename and group.altTitle when group.hasSeriesSyntax is true
- basenameGenerator() - Add basename without 'the' if animename starts with it.
### Added
- seriesNameGenerator() - Generate HashSet of "Series" name variations. 

## [1.6.1] - 2021-11-18
### Changed
- Update detectEpisodeNumberFromFile invocations with new param (returnOnlyIntegerEpisode)

## [1.6.0] - 2021-11-15
### Changed
- Expanded the try/catch to echo out the error, also worked to limit the methods in the try to a single one (so we know the error is only from that method)
- renameOptionForTVDBAbsoluteEpisodes()- Fixed - line 2467 If statement used mySeasonalityNumber for compare, but was using myanimeListGetTVDBSeason for the value

## [1.5.3] - 2021-11-13
### Added
- Changed all println to Logging.log.info/fine/finer/finest, Apply some Intellij suggested Fixes here and there

## [1.5.2] - 2021-11-xx
### Added
- I forgot to update for this point release.

## [1.5.1] - 2021-07-03
### Added
- Switched regex to use global variables, introduced the following:
    - stripYearDateRegex
    - ovaOnaOadSpecialBonusSyntaxMatcher

## [1.5.0] - 2021-06-06
- searchForMoviesJWD() - Switch baseAnimeName to jwdStringBlender(group.anime) from just group.anime 
- filebotMovieFallBack() - Added for logic around Filebot Movie Fall Back lookup of AniDB ID using IMDB/TMDB

## [1.4.3] - 2021-04-07
### Changed
- renameOptionForTVDBAbsoluteEpisodes() - Add renameStrict = true at 1574 for edge cases where it wouldn't be set (at all), causing script abend

## [1.4.2] - 2021-04-06
### Changed
- renameOptionForTVDBAbsoluteEpisodes() - !renameOptionsSet && myEpisodeSeason == myanimeListGetTVDBSeason.toInteger() - Add animelist offset check to send to renameOptionForAniDBAbsoluteEpisodes
 
## [1.4.1] - 2021-03-31
### Changed
- renameOptionsForEpisodesUsingAnimeLists() - Line 1389 - Set renamePass to 1 when sending to renameOptionForAniDBAbsoluteEpisodes() on 2nd pass 
- renameMapperGenerator() - Line 390 - Don't use episode.derive(e) with tvdb if it has AnimeList Entry

## [1.4.0] - 2021-03-29
### Added
- renameMapperGenerator() - Added
### Changed 
- renameOptionForTVDBAbsoluteEpisodes() - Switched existing instances of renameMapper usage to use renameMapperGenerator(), except for TVDB Absolute Ordered  decision tree 

## [1.3.4] - 2021-03-29
### Changed
- renameOptionForTVDBAbsoluteEpisodes() - Fixed a few references to mySeasonalityNumber that should have been myanimeListGetTVDBSeason

## [1.3.3] - 2021-03-29
### Changed
- renameOptionForTVDBAirdateEpisodes() - Added Check for invalid season (Do not treat as special)
- groupGenerationByTVDB() - fix case for Absolute or absolute

## [1.3.2] - 2021-03-29
### Changed
- renameOptionForAniDBAbsoluteEpisodes() - Disable episode.special filter when useNonStrictOnAniDBSpecials is true 

## [1.3.1] - 2021-03-28
- episodeRenameOptionPassOne() - Switched preferred DB to AniDB with Airdate when calling groupGenerationByAnimeLists()
- renameOptionForTVDBAbsoluteEpisodes() - Do not pass TVDBID to renameOptionForAniDBAbsoluteEpisodes when it is a possible special
- renameOptionForAniDBAbsoluteEpisodes() - move pass 1 tvdb checks to pass 2 for Episode Numbers GREATER then AniDB Episode Count WITH Specials range 

## [1.3.0] - 2021-03-07
### Added
- renameOptionsForEpisodesUsingAnimeLists() - Added useNonStrictOnTVDBSpecials
- renameOptionForTVDBAbsoluteEpisodes() - Added useNonStrictOnTVDBSpecials
- renameOptionForTVDBAirdateEpisodes() - Added useNonStrictOnTVDBSpecials
- renameOptionForAniDBAbsoluteEpisodes() - Added useNonStrictOnTVDBSpecials
- episodeRenameOptionPassOne() - Added useNonStrictOnTVDBSpecials
- renameOptionForTVDBAirdateEpisodes() - Added useNonStrictOnTVDBSpecials
- renameOptionForTVDBAbsoluteEpisodes() - Added useNonStrictOnTVDBSpecials
- renameOptionForTVDBAbsoluteEpisodes() - Fix missing renameFilter when myanimeListGetTVDBSeason == 'n'

## [1.2.0] - 2021-03-07
### Changed
- renameOptionsForEpisodesUsingAnimeLists() - Removed updating group.isSpecialEpisode, isSpecialEpisode value is used for local decision tree/return only.
- renameOptionForTVDBAbsoluteEpisodes() - Updated to use local isSpecialEpisode, isMovieType, isSpecialType variables (no mix of local or group)
- renameOptionForTVDBAirdateEpisodes() - Updated to use local isSpecialEpisode, isMovieType, isSpecialType variables (no mix of local or group)
- renameOptionForAniDBAbsoluteEpisodes() - Removed updating group.isSpecialEpisode, isSpecialEpisode value is used for local decision tree/return only.
- renameOptionForAniDBAbsoluteEpisodes() - Add if ( !renameOptionsSet && group.isSpecialEpisode ) Decision Tree
- renameOptionForAniDBAbsoluteEpisodes() - Add isSpecialType options to when myEpisodeNumber == null
- episodeRenameOptionPassOne() - Removed setAnimeTypeFromAID() usage (it's now included in AniDBMatchDetails)
- episodeRenameOptionPassOne() - Switched preferred DB to anidb for airdate when AniDB Matches 0.98+
- renameOptionForTVDBAirdateEpisodes() - Added processAsSpecial Boolean (default value is false)
- renameOptionForTVDBAbsoluteEpisodes() - Added processAsSpecial Boolean (default value is false)

## [1.1.2] - 2021-03-03
### Changed
- renameOptionForTVDBAirdateEpisodes() - Updated renameMapper to use different mappers if Absolute vs Airdate Ordering.
- renameOptionForAniDBAbsoluteEpisodes() - Added renameStrict = false option support when  No AnimeList Entry. AniDB Fallback will be used

## [1.1.1] - 2021-03-01
### Changed
- renameOptionForAniDBAbsoluteEpisodes() - Updated decision tree when ep # greater then # of eps to use AnimeList map if no TVDBID supplied and it has one

## [1.1.0] - 2021-02-28
### Added
- filebotAnidbJWDSearch() - Added support for isMovieType, isSpecialType, isSpecialEpisode and specialType detection using setAnimeTypeFromAID()
- filebotAnidbJWDSearch() - Added support for hasAnimeListEntry using filebotAnimeListReturnFromAID()
- filebotTVDBJWDSearch() - Added support for hasAnimeListEntry using filebotAnimeListReturnFromTVDBID()
- groupInfoGenerator() - Added support for isMovieType, isSpecialType, isSpecialEpisode and specialType
- groupGenerationByAnimeLists() - Rename Option Group Generation using AnimeList Decision Tree
- groupGenerationByTVDB() - Rename Option Group Generation using TheTVDB Decision Tree
- groupGenerationByAniDB() - Rename Option Group Generation using AniDB Decision Tree
- renameOptionsForEpisodesUsingAnimeLists() - Rename Option picker using AnimeList Decision Tree
- renameOptionForTVDBAbsoluteEpisodes() - Rename Option picker using TVDB & Absolute Ordering Decision Tree
- renameOptionForTVDBAirdateEpisodes() -  Rename Option picker using TVDB & Airdate Ordering Decision Tree
- renameOptionForAniDBAbsoluteEpisodes() - Rename Option picker using AniDB & Absolute Ordering Decision Tree
- episodeRenameOptionPassOne() - Episode Renaming 1st Pass JWD Decision Tree
- javadoc for methods

### Changed
- searchForMoviesJWD() - updated usage of filebotAnidbJWDSearch for anidbHashTitleSearch()
- filebotAnidbJWDSearch() - Switched to use anidbHashTitleSearch (instead of anidbXMLTitleSearch)
- basenameGenerator() - Added additional custom checks for series


## [1.0.6] - 2021-02-14
### Changed
- basenameGenerator - Added additional custom checks for series
```
  ~/girl gaku/
  ~/sk/
  ~/rezero kara hajimeru break time/
  ~/mushoku tensei/
  baseAnimeName == 'shokugeki no souma' && group.seasonNumber == 4
  baseAnimeName == 'dragon quest dai no daibouken' && group.yearDateInName == "2021"
```
- groupInfoGenerator() - Added group.releaseGroup support