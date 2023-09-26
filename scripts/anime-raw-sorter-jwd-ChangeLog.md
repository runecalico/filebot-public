# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.11.0] - 2023-02-09
- Removed Directory base Video extras detection
- Added looser video extras detection for short duration videos

## [3.10.0] - 2023-02-05
- Added support for detecting and renaming Matroska (mks) files with a duration mismatch, for example reported duration of 1h10m, but actually only 23 minutes

## [3.9.0] - 2023-02-02
- Added moviesBasenameGeneratorOverrideJsonFile - Movies basename override file
- renamed basenameGenerator to seriesBasenameGenenerator()
- Update ordinalPartialSeasonalityMatcher Regex

## [3.8.0] - 2023-01-17
- groupGenerationNew() - use StringUtils.stripaccents from f.name to remove "accent" characters that screw up compares later on like in sorter().groupOverrides()

## [3.7.0] - 2023-01-16
- groupGenerationNew() - run animeNameGroupGenerator twice in attempt to detect multiple/layered naming schemes used in same file, or where the filename doesn't work correctly with single pass
  - example `[DKB] Boku no Hero Academia VI - S06E01 [1080p][HEVC x265 10bit][Multi-Subs].mkv` which has both Roman Ordinal (VI) AND Airdate/Seasonality instead of just one
  - example `Pocket Monsters Advanced Generation 121 - Begin! Grand Festival I.ass` as the first pass returns `pocket monsters advanced generation 121` while the 2nd pass returns `pocket monsters advanced generation` (which is what we want)

## [3.6.0] - 2022-02-27
- Added seriesBasenameGeneratorOverrideJsonFile to use with baseNameGenerator() to externalize Series basename overrides

## [3.5.6] - 2022-02-27
### Changed
- animeNameGroupGenerator() - add releaseGroup to returned object

## [3.5.5] - 2022-02-23
### Changed
- Add recognision of .mks as a valid file (it's not recognized by filebot as subtitle)

## [3.5.4] - 2022-02-22
### Changed
- groupGenerationNew() - GroupBy() - Added () and [] to the "underscores are better then spaces" group detection

## [3.5.3] - 2021-12-26
### Changed
- animeNameGroupGenerator() - Added Movie only regex removal of matchNumberDashtoEndofLine
- groupGenerationNew() - Add filename check for [ddd] text, for filenames that actually start with the episode in brackets (ugh), example: `[26] {480} Waste Not, Want Not.mp4`. Set useDetectAnimeName to `true`  
- groupGenerationNew() - Fix invalid path condition with myInputFolder when there is only 1 file being processed (Which also fixed an error with animeBaseFolder() when all files are in the same directory)

## [3.5.2] - 2021-12-26
### Changed
- ordinalSeriesMatcher - Modified Regex (lookbehind for 'the')
- wordSeasonalityMatcher - Modified Regex (lookbehind for 'the')
- groupGenerationNew() - groupBy() - update all instances of when removing stripYearDateRegex, replace with space then follow up with stripMultipleAdjacentSpaces 
`anime.replaceAll(/${stripYearDateRegex}/, ' ').replaceAll(/${stripMultipleAdjacentSpaces}/, '')`

## [3.5.1] - 2021-12-24
### Changed
- animeNameGroupGenerator() - Added insanity check for () Alt title for groups that enclose the season in them, aka (Season 2)
- Added Filebot xattr["originalfilename"] with original file name (prior to rename)

## [3.5.0] - 2021-12-23
### Changed
- groupGenerationNew() - groupBy() - Added new xattr keyword - xattr["originalfolder"] ==> Store the relative folder path to the 
original file PRIOR to rename/movement by filebot. This is very useful when the file has been misnamed, or placed in the
wrong series AND the filename is something like "episode 1.mkv", which is not helpful trying to determine what it is :)
- groupGenerationNew() - If animeParentFolder is Season xx, then use the parent directory of that directory as "animeParentFolder"


## [3.4.1] - 2021-11-25
### Changed
- renameWrapper() - Add additional logging on individual vs group renaming

## [3.4.0] - 2021-11-21
### Changed
- Added support for renaming "invalid" (bad) files, controlled by renameInvalid, invalidOutputFolder and checkCRC options
- Initial Invalid detection is files that fail CRC (if they have it in file name) and Files with "null" media length
- changed videoExtraDirectoryMatcher to  `/(?i)(^teaser[s]?|^extra[s]?|bonus|preview[s]?|menu[s]?|TV Ad[s]?)$/`

## [3.3.2] - 2021-11-20
### Changed
- Do not process Episode Groups that are empty/null (this happens when both Filebot & This script can't generate a name)
- videoExtraFilesMatcher - Add `^(?:ed|op)[0-9](?<!\s\d{1,4})(?>\.\d)?\.\w{3}$` to match edN.xxx or opN.xxx

## [3.3.1] - 2021-11-13
### Changed
Changed all println to Logging.log.info/fine/finer/finest, Apply some Intellij suggested Fixes here and there

## [3.3.0] - 2021-10-24
- animeNameGroupGenerator() - Updated checks to use @Field variables for most Regex Matches 
- groupGenerationNew() - Updated checks to use @Field variables for most Regex Matches
- groupGenerationNew() - Added [TDG Season x] Override for anime name
- acceptFile() - Updated Extra checks to use def variables ignoreVideoExtraFoldersRegex & ignoreVideoExtraFilesRegex for "Extra" detection

## [3.2.1] - 2021-10-22
- animeNameGroupGenerator() - Updated () Check for cases where () is actually *part* of the title, aka Nekomonogatari (Kuro)
- groupGenerationNew() - Added animeRegexBlenderName == ' ' check alongside existing null/'' check

## [3.2.0] - 2021-10-21
- animeNameGroupGenerator() - Added check for possible Alt title when numerical series syntax is detected, sometimes the alt title *is* using numerical series syntax

## [3.1.2] - 2021-10-16
- animeNameGroupGenerator() - Added Episode # detection, Added check for episode # to Seasonality (Sx) Check. If no episode the treat as special

## [3.1.1] - 2021-10-09
- Added animeDetectedName to groupGenerationNew() & animeNameGroupGenerator() to track how often Filebot detects a different anime name during Series Rename Generation

## [3.1.0] - 2021-10-03
- Added option to rename Extras using extraFormat

## [3.0.0] - 2021-08-06
- Major revamp of groupGeneration
  - Split out logic around generating the group name (which forms the basis of the Anime name) into animeNameGroupGenerator()
  - In all cases where it is not actually necessary to use the File f object, switched to using String variable
  - animeNameGroupGenerator() is called repeatedly until the anime name can not be processed anymore
  - Added Initial logic on using a parent (immediate or farther up) as the Anime name in cases where regexBlender is known to fail
  - Switched the default useAutoDetectName from using Filebot's detected Anime Name as anime, to using it for AltTitle, while
    - using the Folder (Not necessarily the parent) as anime/myFileNameForMatching
- animeNameGroupGenerator() - New method that returns the Anime Name Group and Group Options

## [2.3.0] - 2021-07-03
- Switched regex to use global variables, introduced the following:
  - stripYearDateRegex
  - stripTrailingSpacesDashRegex
  - stripMultipleAdjacentSpaces
  - stripTVDBSeasonalityAndEverythingAfter
  - stripLeadingSpacesDashRegex
  - airDateOrderMatcherRegex
  - tvdbSeasonalityMatcher
  - ovaOnaOadSpecialBonusSyntaxMatcher
- groupGeneration() - Added edge case detection for AnimeRegexBlender when it returns just episode, to use useDetectAnimeName

## [2.2.0] - 2021-06-06
- Movie Renaming - Added option enableFileBotMovieLookups to use the IMDB/TMDB ID from FileBot's detectmovie & Anime-Lists XML to map to an AniDBID when normal methods fail AND useNonStrictOnAniDBMovies is enabled.

## [2.1.3] - 2021-06-05
- groupGeneration() - Added workaround for LNSubs/LightNovel release group hyphen based naming scheme (WTH?)

## [2.1.2] - 2021-04-24
- groupGeneration() - Added \bspe\d{1,2} to regex matcher for "OVA" Type
- groupGeneration() - Added workaround removal of feat in Files from [Anime-Release]

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