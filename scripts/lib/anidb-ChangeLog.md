# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.2] - 2023-01-15
### Changed
- animeListXMLGetTVDBNAme() - Explicitly reference WebServices.TheTVDB.search

## [1.5.1] - 2021-12-25
### Changed
animeListXMLGetAniDBFromIMDBID() - Update to work with imdbid entries that contain multiple IDs
animeListXMLGetAniDBFromTMDBID() - Update to work with tmdbid entries that contain multiple IDs

## [1.5.1] - 2021-11-13
### Changed
Changed all println to Logging.log.info/fine/finer/finest, Apply some Intellij suggested Fixes here and there

## [1.5.0] - 2021-10-16
### Added
- anidbXMLEntryGetAnimeTitles() - Get all Official/Main Titles that are likely a romantic language
### Changes
- loadAniDBOfflineXML() - Switched anidbXMLEntryGetAnimeOMTitles for anidbXMLEntryGetAnimeTitles

## [1.4.0] - 2021-06-06
### Added
- animeListXMLGetAniDBFromIMDBID() - Return AniDB ID mapped to IMDB ID in Anime-Lists
- animeListXMLGetAniDBFromTMDBID() - Return AniDB ID mapped to TMDB ID in Anime-Lists

## [1.3.1] - 2021-03-02
### Changed
- anidbHashTitleSearch() - Switch altjwdcompare(searchItemString) to searchItemStringCompare variable

## [1.3.0] - 2021-02-28
### Added
- filebotAniDBgetEpisodeList() - Get the Episode List for an AID from Filebot
- filebotAniDBEpisodeCount() - Count the # of Episodes
- aniDBGetEpisodeNumberForAID() - For an AID, return the # of Episodes in that AID.
- anidbHashTitleSearch() - Functional replacement for anidbXMLTitleSearch to use with LinkedHashMap
- loadAniDBOfflineXML() - Merge AniDB Title/Synonym XML's into single LinkedHashMap
- javadoc for methods

### Changed
- anidbXMLTitleSearch() - Changed strictJWDMatch to jwdStrictness 

## [1.2.0] - 2021-02-14
### Added
- filebotAnimeListReturnFromTVDBID - Return animelist entry that matches TVDB ID
  
### Changed
- filebotAnimeListReturnFromAID - Added return 1 if defaulttvdbseason is null when tvdbSeasonOnly is true