# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [1.2.7] - 2021-02-14
### Added
- groupGeneration() - Added Release Group (group.releaseGroup) detection/tracking using shared.detectAnimeReleaseGroupFromFile()
- groupGeneration() - Added detection of word seasonality aka anime series second season, first season, third season etc. using ordinalSeasonality variables 
- 
### Changed
- groupGeneration() - airdate detection - updated regex for myTVDBSeasonalityRegexMatcher to improve detection of non-standard TVDB Syntax
- groupGeneration() - AltText using - : Adjusted Regex to catch the "last" - and all text to the end of the line (vs the first - and all text till end of line)
- groupGeneration() - altText using () : Added Check for ASW release of Dragon Quest: The Adventure of Dai
- groupGeneration() - forced lowercase for anime name in return as Filebot Generated Anime Names where frequently Camel Case (Matches for animename now don't need to care about case)
- Series Rename - 1st Pass - Skip TVDB for Absolute ordering if Series has Partial AND Ordinal Seasonality (right now this usually results in incorrect matching)
  - ```
    myanimeListXMLGetTVDBID = (hasPartialSeasonality && hasOrdinalSeasonality) == true ? null : filebotAnimeListReturnFromAID(anidbFirstMatchDetails.dbid, true)
    ```
- Series Rename - 3rd Pass - Changed params to Absolute order, no Filter, No Mapper with TVDB Lookups 
  
## [1.2.6]
### Changed
- Fix error in - alt title check (groupGeneration)

## [1.2.5]
### Removed
- Removal of most commented out code (except println/log)

## [1.2.4]
### Changed
- Switch to using filebotAnimeListReturnFromAID to query filebot's version of AnimeLists to see if TVDB can be used and still get AniDB metadata

## [1.2.3]
### Changed
- Switch AniDB Romanisation case statements to use returnAniDBRomanization method instead

### Added
- Adjust groupGeneration so it will work with series that have BOTH partial and Ordinal Seasonality Syntax (like re zero kara hajimeru isekai seikatsu 2nd season part 2)

## [1.2.2]
### Changed
- Adjust 3rd rename attempt for Absolute files using TVDB (Too many incorrect matches)
  
## [1.2.1]
### Changed
- GroupGeneration: Files that start with # to use FileBot Detected Anime Name by updating regex to /^([0-9]|#[0-9])/ (from: /^[0-9]/)

## [1.2.0]
### Changed
- Fixed issue where 3rd pass would always run instead of the few cases when it *should* run

### Added
- Added Individual File Renaming to 2nd Pass RFSPartial Rename, except when useNonStrictPartialRenames is true
- Since Individual File Renaming works VERY well, I defaulted the 1st pass to ALSo use Individual File Renaming 
  - Added useIndividualFileRenaming option (Default true) which can turn that off if desired
  - Using IFR dramatically improves matching when strict=true, as well as it seems to also reduce false matches in *some* instances
- Added skipMovies option (default false) to allow skipping movie renaming.

## [1.1.0]
- First Release