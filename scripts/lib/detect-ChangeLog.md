# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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