# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [1.0.6] - 2021-02-14
### Added
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