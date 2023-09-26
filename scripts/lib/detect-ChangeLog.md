# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [1.5.4] - 2023-09-26
### Added
detectCJKCharacters() - Detect CJK characters in a String (filename)
detectEpisodeNumberFromFile() - Refinement(s) on additional edge cases
detectEpisodeNumberFromFile2() - Testing version of detectEpisodeNumberFromFile() with workingSort script

## [1.5.3] - 2022-04-18
### Added
detectEpisodeNumberFromFile() - Added 10.2, 10.3, 10.4, 10.5, 12, 13, 14, 15, 16  Detection

## [1.5.2] - 2022-02-20
### Added
detectEpisodeNumberFromFile() - Added 10.2 Edge Case Detection

## [1.5.1] - 2021-11-20
### Changed
detectEpisodeNumberFromFile() - Switch to Logging.fine for output
detectEpisodeNumberFromFile() - Add removeFileExtensionRegexForEpisodeDetection 

## [1.5.0] - 2021-11-13
### Changed
detectEpisodeNumberFromFile() - Added returnOnlyIntegerEpisode param for situations where a non-integer would cause issues (like 212-213) 

## [1.4.3] - 2021-11-13
### Changed
Changed all println to Logging.log.info/fine/finer/finest, Apply some Intellij suggested Fixes here and there

## [1.4.2] - 2021-10-24
### Changed
detectAnimeReleaseGroupFromFile() - Added manual override regex for [TDG Season x][###][XXX] File format


## [1.4.1] - 2021-10-18
### Changed
detectEpisodeNumberFromFile() - Added a additional episode regex for [TDG Season x][###][XXX] Text File format

## [1.4.0] - 2021-10-18
### Changed
detectEpisodeNumberFromFile() - Added a few additional episode regex

## [1.3.1] - 2021-10-15
### Changed
- detectEpisodeNumberFromFile() - Moved AirDate order detection to later in method 

## [1.3.0] - 2012-07-03
### Changed
- Switched regex to use global variables, introduced the following:
    - airDateOrderMatcherRegex
    - stripLeadingZerosRegex
    - stripEpisodeVersionInfoRegex
    - matchAniAddEpisodeFormatOne
    - matchAniAddMovieFormatOne
    - stripTrailingSpacesDashRegex
    - stripYearDateRegex

## [1.2.4] - 2012-03-28
### Changed
- detectEpisodeNumberFromFile() - Reordered regex matching putting some inaccurate one's first

## [1.2.3] - 2012-03-06
### Changed
- detectAirdateOrder() - Added support for 4 digit episodes (Case Closed is now 999+)

## [1.2.2] - 2012-02-28
### Changed
- detectEpisodeNumberFromFile() - fix missing \ from regex to match episodes with periods (aka 5.5)
- detectEpisodeNumberFromFile() - Initial Update to support 4 digit episode # (and try to exclude 19/20 year dates) and 3 digit Season (airdate only)
- detectEpisodeNumberFromFile() - Update aniadd to support returnSpecialsType Boolean

## [1.2.0] - 2012-02-28
### Added
- detectEpisodeNumberFromFile() - Detect the Episode # from a file
- javadoc for methods

### Changed
- detectAirdateOrder - Refine regex for a few corner cases

## [1.1.2] - 2012-02-14
### Changed
- detectAirdateOrder - Updated regex to catch wider range of TVDB syntax schemes
  
## [1.1.1]
### Added
- Before I started keeping a Changelog :)