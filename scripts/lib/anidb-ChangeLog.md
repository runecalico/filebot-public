# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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