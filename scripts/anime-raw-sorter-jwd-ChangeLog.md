# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.1] - 2021-03-29
- Reset mySeasonalityNumber to zero when processing each episode group (The value would otherwise be whatever the last group was)

## [2.1.0] - 2021-03-06
- groupGeneration() - switched airdate regex from f.name to myFileNameForParsing to align with detectAirdateOrder() usage
- groupGeneration() - added missing \b to special work on OVA regex
- renamewrapper() - Switched to LinkedHashMap for all rename Options, part of update to support isSpecialEpisode decision from GroupByRenameOptions decision tree's (non-persistant between passes)
- renamewrapper() - Defaulted to animeformat, only isSpecialEpisode now uses specialFormat
- renameMovieWrapper() - Added so movie renaming can keep old renameWrapper syntax
- Episode Renaming - Instead of co-opting group values for isMovieType, isEpisodeType, isSpecialEpisode I am using groupByRenameOptions 
LinkedHashMap with those values in it, which are used by renameWrapper(). Those values are set by the various groupByRename 
methods in their respective decision tree's. Changes to groupByRenameOptions do not persist between rename passes, OR when processing passes between AniDB/TVDB within a decision tree. 
- Script no longer die's if no files where matched as that would exit non-zero (error codition) to the shell.
- Script "reporting" output changed to be easier to import into a spreadsheet.

## [2.0.0] - 2021-02-28
- Major rewrite of the way episode renaming is done and rename option decision trees.
- Initial switch from using XML searches (AniDB Title/Synonym) to LinkedHashMap Searches
- Initial support for treating Episodes that are likely Specials vs Anime Series that are Special (OVA/ONA) using isMovieType, isSpecialType, isSpecialEpisode and specialType
- Moved most of the code involving Episode decision tree work to methods in lib\sorter.groovy
- Changed Episode renaming to rename by group, with the grouping be by rename options to pass to renameWrapper
- Rename options are decided using Episode # of the file, File Ordering (Absolute/Airdate) and AnimeLists
- Episodes over 99 almost always use episode filtering to that specific episode to deal with sporatic episode mismatches for triple digit episodes
- Changed Episode renaming to rename until "done", so leftOver files get processed in subsequent passes
- Removed 3rd Pass completely (The Edge cases it was used for no longer are valid)
### Added
- groupGeneration() - Removed group.releaseGroup (It caused way too many groups to form, which also greatly slowed down processing)
- groupGeneration() - Added isMovieType, isSpecialType, isSpecialEpisode and specialType placeholders to group generation
- groupGeneration() - Updated "Special" detection with rudimentary isSpecialType, isSpecialEpisode and specialType usage
- groupGeneration() - Switched to use anidbHashTitleSearch (instead of anidbXMLTitleSearch)
- groupGeneration() - Added Release Group Checks (ASW|DKB) for fixing improper file naming for dragon quest dai no daibouken 2020
- renameWrapper() - Updated to use different formats based on isMovieType, isSpecialType, isSpecialEpisode (or default) 
- seriesnameGenerator() - Added support for Partial Syntax WITH Seasonality/Airdate Syntax (Thanks to Re:Zero Season 2 Part 2)
- seriesnameGenerator() - Updated for group.isSpecialType and group.isSpecialEpisode

### Changed
- Added metadata to Processed file output at end of script (it's cached even if I don't write the metadata to the file)

## [1.2.7] - 2021-02-14
### Added
- groupGeneration() - Added Release Group (group.releaseGroup) detection/tracking using shared.detectAnimeReleaseGroupFromFile()
- groupGeneration() - Added detection of word seasonality aka anime series second season, first season, third season etc. using ordinalSeasonality variables 

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