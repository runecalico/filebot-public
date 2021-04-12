# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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