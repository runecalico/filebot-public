# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2012-02-28
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
  