# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2012-03-07
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

## [1.1.2] - 2012-03-03
### Changed
- renameOptionForTVDBAirdateEpisodes() - Updated renameMapper to use different mappers if Absolute vs Airdate Ordering.
- renameOptionForAniDBAbsoluteEpisodes() - Added renameStrict = false option support when  No AnimeList Entry. AniDB Fallback will be used

## [1.1.1] - 2012-03-01
### Changed
- renameOptionForAniDBAbsoluteEpisodes() - Updated decision tree when ep # greater then # of eps to use AnimeList map if no TVDBID supplied and it has one

## [1.1.0] - 2012-02-28
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