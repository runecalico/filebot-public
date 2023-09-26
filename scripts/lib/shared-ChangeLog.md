# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0] - 2023-02-05
### Changed
- Added convertSecondsToHMS()

## [1.5.4] - 2023-02-02
### Changed
- getWordNumber() added singular word (one, two, three etc)

## [1.5.3] - 2022-04-18
### Added
- stripMetadataKeywordsWordComplex() - Add BD Regex when followed by specific resolutions

## [1.5.2] - 2022-02-20
### Added
- Added removeVersionAtEndOfLine Regext to RegexBlender Step 3 
- removeAbsoluteEpisodeInfo() - Add number as alternative for episode

## [1.5.1 - 2021-12-26
### Changed
- stripMetadataKeywordsWordBoundary - Modified Regex

## [1.5.0 - 2021-12-24
### Added
- getNumberFromRomanOrdinal() - Return the # equiv to a roman ordinal up to xx (20)

## [1.4.3 - 2021-11-18
### Changed
- removeAbsoluteEpisodeInfo - Update for 4 digit Absolute numbering (Case Closed)

## [1.4.2] - 2021-11-13
### Changed
- Changed all println to Logging.log.info/fine/finer/finest, Apply some Intellij suggested Fixes here and there

## [1.4.1] - 2021-10-19
### Changed
- regexStep2() - Fine tuning of Regex to remove file extention (.ext or .1.ext)


## [1.4.0] - 2021-10-06
### Changed
- stripMetadataKeywords() - Split the Regex into Static variables for easier time applying in "order"
- regexStep2() - uses stripMetadataKeywords() now instead of repeating the regex code there

## [1.3.0] - 2021-08-06
### Added
- getShortestSharedPath() - Method to return the shared path for files that all share a common path
### Changed
- stripMetadataKeywords() - Added additional keywords
- regexStep2() - Changed extension removal regex

## [1.2.0] - 2021-07-03
### Changed
- Changed several Regex to reference variables, introducing the following regex vars 
  - jwdStripSpecialCharaters
  - stripMetadataKeywords
  - stripMultipleAdjacentSpaces 
  - stripTrailingSpacesDashRegex
  - stripLeadingSpacesDashRegex
    
## [1.1.0] - 2021-02-28
### Added
- javadoc for methods

### Changed
- altjwdStringBlender() - Updated Regex to not remove any ending . in the string

## [1.0.5] - 2021-02-14
### Added
- altjwdStringBlender()/jwdStringBlender() - Added ; to Characters removed
- regexRemoveKeywords()/regexStep2() - Added additional keywords to remove
- regexBlender() - Added String type to variables
- getWordNumber() - Added overloaded method (returns Integer vs String) to get the # that a word represents aka return 1 for first

### Changed
- getWordNumber() - fixed typo in eighth
- - regexBlender() - Switched step2 to use regexStep2 function
  