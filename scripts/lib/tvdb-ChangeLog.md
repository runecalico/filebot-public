# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.3] - 2023-01-15
### Changed
- Filebot 2.9.6 depreciated theTVDBSeriesInfo class we were using, so updated to use SeriesInfo instead
## [1.2.2] - 2021-11-25
### Changed
- filebotTVDBgetEpisodeList() - Add Try/Catch on WebServices.TheTVDB.getSeriesInfo() for when the Series ID doesn't exist on TVDB

## [1.2.1] - 2021-11-13
### Changed
- Changed println to Logging.log.warning

## [1.2.0] - 2021-03-29
### Changed
- filebotTVDBSeasonEpisodeRange() - Switch from collect to findAll (Now it will not return null "matches")

## [1.1.0] - 2021-02-28
### Added
- filebotTVDBgetEpisodeList() - get the Episode List for a TVDB Series
- filebotTVDBSeasonEpisodeRange() - Return all TVDB Episodes in a specific Season of a TVDB Series
- filebotTVDBSeasonEpisodes() - Return all TVDB Episodes in a specific Season of a TVDB Series
- filebotTVDBSeasonContainsEpisodeNumber() - Determine if an episode is contained in a collection of Episodes
- filebotAnimeListReturnFromTVDBID() - Search Filebot's cache of AnimeLists for the specific TVDB ID.
- filebotAnimeListReturnAIDEntry() -  Try to determine the AniDB AID for a specific episode using TVDB ID # and Filebot's AnimeLists cache
- javadoc for methods

## [1.0.0] - 2021-02-14
### Added
- Changelog (This file)