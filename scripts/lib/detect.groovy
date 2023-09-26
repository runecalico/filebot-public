//file:noinspection RegExpRedundantEscape
//file:noinspection unused
//file:noinspection GrMethodMayBeStatic
//file:noinspection GroovyAssignabilityCheck
package lib

import groovy.transform.Field
import net.filebot.Logging
import java.util.regex.Matcher
import java.util.regex.Pattern

//--- VERSION 1.5.3

// What we are trying to match:
// Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations
// Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
// Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations)
// Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations)
// with 1 or 2 digit seasons and between 1-4 digit episodes
// However it will not match if it's encased in () [] or ..
// VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
// VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
// VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
// VOID - return filename =~ /(?i)\b(?<!\[)((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
// VOID - return filename =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
// VOID - return filename =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
@Field String airDateOrderMatcherRegex = /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
@Field String airDateOrderMatcherRegexStrict = /(?i)\b((S\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b/
@Field String stripLeadingZerosRegex = /^0+(?!$)/
@Field String stripEpisodeVersionInfoRegex = /(v[\d]{1,2}\b[^-]*$)/
// AniAdd Episode Format #1
// Group 1 - Series
// Group 2 - Episode # (or Special)
// Group 3 - Episode name (might be partial)
// Group 4 - Release Group including []
// Group 5 - Release Group
// Group 6 - Source including []
// Group 7 - Source
// Group 8 - Resolution including []
// Group 9 - Resolution
// Group 10 - Codec including []
// Group 11 - Codec
// Group 12 - CRC including []
// Group 13 - CRC
@Field String matchAniAddEpisodeFormatOne = /^(.+)\s-\s(\d{1,3}|S\d{1,3})(?>v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
// Group 1 - Series
// Group 2 - Episode # (or Special)
// Group 3 - Episode Version
@Field String matchAniAddEpisodeFormatTwo = /^(.+)\s-\s(\d{1,3}|S\d{1,3})(v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
// AniAdd Movie Format #1
// Group 1 - Movie Name
// Group 2 - Release Group including []
// Group 3 - Release Group
// Group 4 - Source including []
// Group 5 - Source
// Group 6 - Resolution including []
// Group 7 - Resolution
// Group 8 - Codec including []
// Group 9 - Codec
// Group 10 - CRC including []
// Group 11 - CRC
@Field String matchAniAddMovieFormatOne = /^(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
@Field String stripTrailingSpacesDashRegex = /([\s-])*$/
@Field String stripYearDateRegex = /(?i)[\.\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
@Field String removeFileExtensionRegexForEpisodeDetection= /((?>\.\d)?\.\w{3}$|\.\w{3}$)/


/**
 *  Detect if a string is Chinese, Korean, or Japanese
 *
 *  @param string
 *  @return boolean
 */
boolean detectCJKCharacters(String str) {
  Pattern pattern = Pattern.compile("(\\p{InHiragana}|\\p{InKatakana}|\\p{IsHan}|\\p{IsHangul})", Pattern.UNICODE_CASE);
  Matcher matcher = pattern.matcher(str);
  return matcher.find();
}

/**
 * Determine if a file is a Movie.
 *  If it is longer then 60 minutes or has Movie/Gekijouban in the filename we basically consider it a movie
 *
 * @param f The file we want to check
 * @return  True/false
 */
Boolean detectAnimeMovie(File f) {
  if ( f.name =~ /(?i:Movie|Gekijouban)/ ) {
    return true as Boolean
  }
  if ( f.isVideo() && getMediaInfo(f, '{minutes}').toInteger() > 60 ) {
    if ( f.isEpisode() ) {
      return false as Boolean
    }
    return true as Boolean
  }
  return false as Boolean
  // return f.name =~ /(?i:Movie|Gekijouban)/ || ( f.isVideo() ? getMediaInfo(f, '{minutes}').toInteger() > 60 : false )
}

/**
 * Determine if a file uses Airdate Ordering
 *
 * @param filename The file we want to check
 * @return  True/false
 */
Boolean detectAirdateOrder(String filename) {
  return filename =~ /${airDateOrderMatcherRegex}/
}

// Some Forum posts about group matching .. Maybe sometime I'll look at them
// {fn.replaceAll(/$n/,n.replaceAll(/[-]/,'.')).match(/(?:(?<=[-])(?!(cd|CD)[0-9]+)\w+$)|(?:^(?!(cd|CD)[0-9]+)[A-Za-z0-9]+(?=[-]))|(?:(?<=^[\[])[^\]]+(?=[\]]))|(?:(?<=[\[])[^\]]+(?=[\]]$))/)}

/**
 * Determine the Release Group for any given file
 *
 * @param file The file we want to check
 * @return  The detected Release Group (without [], () etc)
 */
@SuppressWarnings('unused')
String detectAnimeReleaseGroupFromFile(File file) {
  def myDetectedGroup = null

  String groupsThatJustUseSpaces = /-NanDesuKa/
  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /(?i)\s(/ + groupsThatJustUseSpaces + /)\s/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1].replaceAll(/-/, '')
    }
  }

  def myRegexMatcher = file.name =~ /^[\[(]([^)\]]*)[\])].*$/
  // For any file starting with [SomeReleaseGroupHere] or (SomeReleaseGroupHere)
  if ( myRegexMatcher ) {
    myDetectedGroup = myRegexMatcher[0][1]
  }

  // Manual override for Specific File Format
  myRegexMatcher = file.name =~ /(?i)^(\[TDG Season [\w]*\])(\[[^.]*\])(\[[^.]*\])/
  // for filenames like:
  // [TDG Season 4][121][1080P]EN Kurina Official.mp4
  if (myRegexMatcher) {
    myDetectedGroup = null
  }

  String groupsWithVariableFilenamePositionsUsingBrackets = /ColdYawn|Rady|NHK|Central Anime|Netflix-Subs|peachflavored|Persona99.GSG|Uppi|tv.dtv.mere|ManifestX|h-b|sxales|AnimDL.ir|glm8892|tempsubs|tempsub|SevensRoadFansubs|DarkDream/
  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /(?i)[\[(](/ + groupsWithVariableFilenamePositionsUsingBrackets + /)[\])].*$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  String groupsThatDoNotFollowEasyRules = /MNHD-FRDS|YGYMTRAN/
  // - MNHD-FRDS aka Howl_s.Moving.Castle.2004.BluRay.1080p.x265.10bit.4Audio.MNHD-FRDS.mkv
  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /(?i)\.(/ + groupsThatDoNotFollowEasyRules + /)(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  String groupsThatEndFileNamesWithDash = /slax|YURASUKA|RONiN|FGT|Rapta|EMBER|Li0N|SvM|WiCkEd/
  if ( myDetectedGroup == null ) {
//    myRegexMatcher = file.name =~ /(?i)-(slax)(\.\d)?\.[^.]+$/
    myRegexMatcher = file.name =~ /(?i)-(/ + groupsThatEndFileNamesWithDash + /)(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /${matchAniAddEpisodeFormatOne}/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][5]
    } else {
      myRegexMatcher = file.name =~ /${matchAniAddMovieFormatOne}/
      if ( myRegexMatcher ) {
        myDetectedGroup = myRegexMatcher[0][3]
      }
    }
  }
  if ( myDetectedGroup == null ) {
    myDetectedGroup = getMediaInfo(file, '{group}')
    if ( myDetectedGroup != null ) {
      Logging.log.fine "detectAnimeReleaseGroupFromFile() - Using Filebot Metadata:[${myDetectedGroup}]"
    }
  }
  return myDetectedGroup
}

/**
 * Determine the "Episode" # of a file
 *
 * @param file The file we want to check
 * @param preferFilebotMetadata Boolean: Should we prefer Filebot Metadata instead of detection? defaults to false
 * @param episodeProcessing Boolean: Are we processing what we think is an episode? defaults to true
 * @param returnSeasonToo Boolean: Are we going to return the Season (only works for Airdate)? defaults to false
 * @param returnSpecialsType Boolean: Are we going to return the SpecialType along with the #(aka OVA1 vs 1)? defaults to true
 * @return The detected episode #
 */
def detectEpisodeNumberFromFile(File file, Boolean preferFilebotMetadata = false, Boolean episodeProcessing = true, Boolean returnSeasonToo = false, Boolean returnSpecialsType = true, Boolean returnOnlyIntegerEpisode = false) {
  if ( returnOnlyIntegerEpisode ) {
    returnSpecialsType = false
  }
  Boolean fileBotMetadataUsed = false
  Boolean isSpecialType = false
  String specialType
  Integer myDetectedIntegerEpisodeNumber
  if (preferFilebotMetadata && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
      Logging.log.fine "detectEpisodeNumberFromFile() - Prefer Filebot Metadata"
      Logging.log.fine "\t Metadata Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  //  String myFileNameForParsing = regexStep2(file.name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')).replaceAll(/${stripTrailingSpacesDashRegex}/, '').replaceAll(/(\s|\.)*$/, '')
  // Strip *most* periods except for periods between things (basically)
  // .replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' '))
  String myFileNameForParsing = regexStep2(file.name.replaceAll(/${removeFileExtensionRegexForEpisodeDetection}/, '').replaceAll(/[_\{\}]/, ' '))
      .replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')
      .replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      .replaceAll(/(\s|\.)*$/, '')
  Logging.log.fine "myFileNameForParsing:[${myFileNameForParsing}]"
  String myDetectedEpisodeNumber = null
  String myDetectedSeasonNumber = null

  // Likely prone to false positives
  def myRegexMatcher = myFileNameForParsing =~ /(?i)([\s]*)([\d]{1,4}\.\d)\s{0,1}(?>v\d)?/
  // for text like:
  //  Girls und Panzer - Recap 10.5
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Likely prone to false positives
  myRegexMatcher = file.name =~ /(?i)(?<=-)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?=\s)/
  // for filenames like: (Which end up with myFileNameForParsing that don't really work
  // [UQW] Aria the Natural - Ep09 [BDRip 720P AVC-YUV420P10 FLAC]rev.mkv
  // [UQW] Aria the Natural - Episode 09 [BDRip 720P AVC-YUV420P10 FLAC]rev.mkv
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #13 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Likely prone to false positives, aka WILL match within a CRC32 check easily.
  // So strip out a CRC from the file name aka [918154E1] or (918154E1)
  myRegexMatcher = file.name.replaceAll(/([\[(]([0-f]{8})[\])])/, "") =~ /(?i)(?<=.)(E)(\d{1,4})(?=.)/
  // for filenames like: (Which end up with myFileNameForParsing that don't really work
  // Black.Clover.E075..MULTISuBS.1080p.WEBRip.x265-SceneGuardians.mkv
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #15 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})\s[^-]*$/
  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(\s|\s-\s)[^-]*$/
  //  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})($|(\s|\s-\s)[^-]*$)/
  //  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,4})(?>v\d)?($|\s-\s|\s)/
  myRegexMatcher = file.name =~ /(?i)(^\d{1,4})(?<!(19\d\d|20\d\d))(?>v\d)?($|\s-\s|\s)/
  // For text like:
  // 01 The Lesson for Squirrel
  // 055 - The Stone Flower And Shippo s First Love
  // 04 - The Self-Styled Young Man And The Giant Wolf
  // 01
  // Yes this will falsely match titles that START with numbers, aka 11 Eyes. So I rely on later regex to match the episode correctly
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\s([\d]{1,3}\s?$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,3}$|[\d]{1,3})$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,4})$/
  // VOID - (?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,4})(?<!(19\d\d|20\d\d))$
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)(\sE?P?|\s#)([\d]{1,4})(?<!(19\d\d|20\d\d))$/
  // for text like:
  // Assault Lily - Bouquet 01
  // Wolfs Rain E01
  // Aria the Natural - Ep26
  // Keishichou Zero-gakari Season 4 EP03
  // Eromanga sensei #12
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(\d{1,4})(\sto\s)(\d{1,4})/
  // NOTE: 1.2 (above) will "match" the ending episode (21 from below)
  // for text like:
  //  My Senior Brother Has a Pit in His Brain Ep 17 to 21
  //  My Senior Brother Has a Pit in His Brain Ep 22 to 26 FINAL
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '') + '.' + myRegexMatcher[0][3].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Manual override for Specific File Format
  myRegexMatcher = file.name =~ /(?i)^(\[TDG Season [\w]*\])(\[[^.]*\])(\[[^.]*\])/
  // for filenames like:
  // [TDG Season 4][121][1080P]EN Kurina Official.mp4
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceAll(/[\[\]]/, '')
    Logging.log.fine "\t #1.4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\d[a-z][a-z]\sSeason)\s([\d]{1,4})$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\d[a-z][a-z]\sSeason)\s([\d]{1,4})(?<!(19\d\d|20\d\d))$/
  // for text like:
  // ReZero kara Hajimeru Break Time 2nd Season 01 and variations of ordinal season syntax 1st, 2nd, 3rd, 4th, 5th, 6th, 7th, 8th, 9th
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t Ordinal Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])-[\s]*([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})[_\s]?(?>v\d)?[^-]*$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})(?<!(19\d\d|20\d\d))[_\s]?(?>v\d)?[^-]*$/
  // for text like:
  // Grisaia no Kajitsu - SP1
  // Grisaia no Kajitsu - 1
  // Grisaia no Kajitsu - #1
  // Grisaia no Kajitsu - Episode 13
  // Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5
  // Will also match some edge cases of TVDB detected Episodes
  // aka Re Zero kara Hajimeru Isekai Seikatsu - 2x08
  if (myRegexMatcher) {
    //  myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/(?i)(?![a-z0-9])([^-]*$)/, '').replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/(?i)(?![a-z0-9])([^-.]*$)/, '').replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}|[\d]{1,3})(\s-\s)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-)/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})(?<!(19\d\d|20\d\d))\s{0,1}(?>v\d)?(\s?-)/
  // For text like:
  // Cap Kakumei Bottleman - 07 - A Battle! Clash between Soft and Hard!
  // Zoids Wild Zero - 01- Birth! Beast Liger
  // ZENONZARDTHE ANIMATION Episode - 04 -Ash Undercover
  // Dragon`s Dogma - 03v2 - ONA
  if ( myRegexMatcher ) {
    //      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4}-[\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
  // for text like:
  // Taiko no Tatsujin - 01-07 - The Birth of Don and Katsu!!
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    if ( returnOnlyIntegerEpisode ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceAll(/(-[0-9]*)/, '')
      Logging.log.fine "\t #4 returnOnlyIntegerEpisode - Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    } else {
      Logging.log.fine "\t #4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,4})(?>v\d)?$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,4})(?<!(19\d\d|20\d\d))(?>v\d)?$/
  // For text like:
  // To LOVE-Ru Darkness Season S2 14
  // To LOVE-Ru Darkness Season S2 14v2
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #5 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)Special\s(\d{1,3})(?>v\d)?$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,4}|SP\s?-?\s?\d{1,4}|OVA\s?-?\s?\d{1,4}|OAD\s?-?\s?\d{1,4})(?>v\d)?$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,4}|SP\s?-?\s?\d{1,4}|OVA\s?-?\s?\d{1,4}|OAD\s?-?\s?\d{1,4})(?<!(19\d\d|20\d\d))(?>v\d)?$/
  // for text like:
  // Hyakka Ryouran Samurai Bride Special 1
  // Hyakka Ryouran Samurai Bride Special - 1
  // Hyakka Ryouran Samurai Bride Sp 1
  // Hyakka Ryouran Samurai Bride Sp - 1
  // OVA - 1
  // OVA 1
  // But NOT text like
  // Special 2018
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceAll(/[a-zA-Z-]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #6.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    isSpecialType = true
    specialType = "special"
    Logging.log.finest "\t #6.1 Detected isSpecialType[${isSpecialType}]:(specialType?${specialType}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    if ( returnSpecialsType ) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
//      Logging.log.fine "\t #6.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    } else {
//      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceAll(/[a-zA-Z-]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
//      Logging.log.fine "\t #6.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    }
  }

  // myRegexMatcher = file.name =~ /(?i)(\s-[\s]*)([\d]{1,3}.\d)\s{0,1}(?>v\d)?(\s?-\s)/
  myRegexMatcher = file.name =~ /(?i)(\s-[\s]*)([\d]{1,4}\.\d)\s{0,1}(?>v\d)?(\s?-\s)/
  // For text like:
  // [DeadFish] Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5 - Special [720p][AAC].mp4
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #7 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)\s(\d{1,4})\s*-\s[^-0-9]*$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)\s(\d{1,4})(?<!(19\d\d|20\d\d))\s*-\s[^-0-9]*$/
  // For text like:
  // Code Geass - Boukoku no Akito 1 - Yokuryuu wa Maiorita
  // the Garden of sinners 08 - Epilogue
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  //  myRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\.\[(])((S\d{1,3}|\d{1,3})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)/
//  myRegexMatcher = myFileNameForParsing =~ /${constants.airDateOrderMatcherRegex}/
  myRegexMatcher = myFileNameForParsing =~ /${airDateOrderMatcherRegex}/
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations) - This is highly inaccurate ..
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t TVDB Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    tvdbSeasonalityDetected = true
    if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
      Logging.log.fine "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      // Not sure why this is here ..
//      if ( file.metadata ) {
//        myDetectedEpisodeNumber = any { file.metadata.episode } {null}
//        fileBotMetadataUsed = true
//      }
    }
  }

  myRegexMatcher = file.name =~ /${airDateOrderMatcherRegexStrict}/
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations) - This is highly inaccurate ..
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t TVDB #2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    tvdbSeasonalityDetected = true
    if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
      Logging.log.fine "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      // Not sure why this is here ..
//      if ( file.metadata ) {
//        myDetectedEpisodeNumber = any { file.metadata.episode } {null}
//        fileBotMetadataUsed = true
//      }
    }
  }

  // VOID - (?i)([\s]*Episode[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})[_\s]?(?=[a-zA-Z])/
  // VOID - (?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})[\s](?=[a-zA-Z])(?!to)
  // VOID - (?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?!$)[\s](?=[a-zA-Z])(?!to)
  myRegexMatcher = myFileNameForParsing =~ /(?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?!$)[\s](?=[a-zA-Z-])(?!to)/
  // For text like:
  // Qin's Moon S6 Episode 7 English
  // LCA - Spirit Realm Season 6 Ep 08 English
  // Legends Of Dawn-The Sacred Stone EP05 HD1080P X265. Enghish
  // Don Dracula - Episode 5 - Complete Success! Operation - Cheating Cribs
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing == /(?i)^(E)(\d{1,4})$/
  // For text like:
  // E1
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\s)(#(\d{1,4}))(?=\s-)/
  // For Text Like:
  // Dannaken #13 - Me, Her, And Another
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #16 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // AniAdd Format #1 - Series
  myRegexMatcher = file.name =~ /${matchAniAddEpisodeFormatOne}/
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2]
//    if ( myRegexMatcher[0][2].toString().startsWith(/[a-zA-Z]/) ) {
    if (myRegexMatcher[0][2] =~ /^[a-zA-Z]/ ) {
      isSpecialType = true
      specialType = "special"
      myDetectedEpisodeNumber = "s" + myRegexMatcher[0][2].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove Alpha then leading Zero's
    } else {
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    }
    Logging.log.fine "\t #8.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    Logging.log.finest "\t #8.1 Detected isSpecialType[${isSpecialType}]:specialType[${specialType}] for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    if ( returnSpecialsType ) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
//      Logging.log.fine "\t #8.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    } else {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove Alpha then leading Zero's
//     Logging.log.fine "\t #8.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    }
  }

  if (!episodeProcessing) {
    // AniAdd Format #2 - Movies
    myRegexMatcher = file.name =~ /${matchAniAddMovieFormatOne}/
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1]
      Logging.log.fine "\t #9 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
      Logging.log.fine "\t Metadata: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    // VOID - (\d{1,4}(?<!(19\d\d|20\d\d))$)
    // VOID - (?<! s)(\d{1,4}(?<!(19\d\d|20\d\d))$)
    // VOID - ^\w*\d{1,3}$
    myRegexMatcher = myFileNameForParsing =~ /^[a-zA-Z\.]*(\d{1,3}?)$/
    // Matches up to 3 digits at the end of the line as long as there are only alpha and/or periods
    // TomicaKizunaGattaiEarthGranner45
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    myRegexMatcher = myFileNameForParsing =~ /(?i)(?i)(?<=[a-z0-9])(\s\d{1,2}\s)(?=[a-z0-9])/
    // Matches first two digits surrounded by spaces
    // Trigun 06 Lost July
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    // VOID - /(?i)(\d{1,2})\s*?(?<!Season\s|Part\s|[a-z0-9])(v[\d]{1,2}\b.*$)/
    myRegexMatcher = myFileNameForParsing =~ /(?i)(\d{1,2})\s*?(?<!Season\s|Part\s)(v[\d]{1,2}\b.*$)/
    // Matches up to 4 digits at the end of the line when followed by <space> vXX
    // Dual! Parallel Runrun Monogatari 14 v2
    // Wild Arms 06v2
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\s)(s([\d]{1,2}))(?=$)/
    // Matches up to 2 digits at the end of the line when followed by S and would be considered a Special
    // Ani Tore! EX S01
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      isSpecialType = true
      specialType = "special"
      Logging.log.finest "\t #10.4 Detected isSpecialType[${isSpecialType}]:specialType[${specialType}] for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    // This is here because it's likely to produce lots of false matches
    myRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
    // for text like:
    // Legend of the Galactic Heroes Gaiden.2x19
    if (myRegexMatcher) {
      myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")
      myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")
      Logging.log.fine "\t #10.5 - TVDB Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      tvdbSeasonalityDetected = true
      if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
        Logging.log.fine "\t #10.5 Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      }
    }
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(^#(\d{1,4})$)/
  // Matches up to 4 digits when it's just #\d\d\d\d
  // #01
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    Logging.log.fine "\t #14 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=-\s)(E(\d{1,2}))(?=\s-)/
  // Matches Edd surrounded by a space & Single Dash
  // A Day Before Us ZERO - E18 - Our Eighteen
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    Logging.log.fine "\t #12 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  if (myDetectedEpisodeNumber == null && !episodeProcessing) {
    // For Movies, myFileNameForParsing is generally not that far off..
    myDetectedEpisodeNumber = regexRemoveKeywords(myFileNameForParsing).replaceAll(/${stripYearDateRegex}/, '')
    Logging.log.fine "\t #11 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  if (myDetectedEpisodeNumber != null) {
    myDetectedEpisodeNumber = regexStep4(myDetectedEpisodeNumber as String)
  }

  if ( returnOnlyIntegerEpisode && myDetectedEpisodeNumber != null) {
    try {
      myDetectedIntegerEpisodeNumber = myDetectedEpisodeNumber.toInteger()
    } catch (e) {
      Logging.log.fine "\t returnOnlyIntegerEpisode is true, but Detected episode[${myDetectedEpisodeNumber}] is not an Integer."
      myDetectedIntegerEpisodeNumber = null
    }
  }
  Logging.log.fine "//--- Final: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected isSpecialType[${isSpecialType}]:(SpecialType?${specialType}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected myDetectedSeasonNumber[${myDetectedSeasonNumber}]:(myDetectedSeasonNumber?${myDetectedSeasonNumber}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"

  // Should probably just return a map, with the episode number, season, special type etc.
//  return [ myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSeasonNumber: myDetectedSeasonNumber, isSpecialType: isSpecialType, specialType: specialType, myDetectedIntegerEpisodeNumber: myDetectedIntegerEpisodeNumber]

  if ( returnSeasonToo ) {
    return [myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSeasonNumber: myDetectedSeasonNumber]
  } else {
    return myDetectedEpisodeNumber as String
//    if ( returnSpecialsType && isSpecialType ) {
//      return [myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSpecialType: specialType]
//    } else {
//      return myDetectedEpisodeNumber as String
//    }
  }
}

def detectEpisodeNumberFromFile2(File file, Boolean preferFilebotMetadata = false, Boolean episodeProcessing = true) {
  Boolean fileBotMetadataUsed = false
  Boolean isSpecialType = false
  String specialType
  String myDetectedEpisodeNumber = null
  String myDetectedSeasonNumber = null
  Integer myDetectedIntegerEpisodeNumber = null
  String myDetectedEpisodeVersion = null

  if (preferFilebotMetadata && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
      Logging.log.fine "detectEpisodeNumberFromFile() - Prefer Filebot Metadata"
      Logging.log.fine "\t Metadata Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }
  //  String myFileNameForParsing = regexStep2(file.name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')).replaceAll(/${stripTrailingSpacesDashRegex}/, '').replaceAll(/(\s|\.)*$/, '')
  // Strip *most* periods except for periods between things (basically)
  // .replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' '))
  String myFileNameForParsing = regexStep2(file.name.replaceAll(/${removeFileExtensionRegexForEpisodeDetection}/, '').replaceAll(/[_\{\}]/, ' '))
      .replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')
      .replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      .replaceAll(/(\s|\.)*$/, '')
  Logging.log.fine "myFileNameForParsing:[${myFileNameForParsing}]"
  // Likely prone to false positives
  def myRegexMatcher = myFileNameForParsing =~ /(?i)([\s]*)([\d]{1,4}\.\d)\s{0,1}(?>v\d)?/
  // for text like:
  //  Girls und Panzer - Recap 10.5
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Likely prone to false positives
  myRegexMatcher = file.name =~ /(?i)(?<=-)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?=\s)/
  // for filenames like: (Which end up with myFileNameForParsing that don't really work
  // [UQW] Aria the Natural - Ep09 [BDRip 720P AVC-YUV420P10 FLAC]rev.mkv
  // [UQW] Aria the Natural - Episode 09 [BDRip 720P AVC-YUV420P10 FLAC]rev.mkv
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #13 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Likely prone to false positives, aka WILL match within a CRC32 check easily.
  // So strip out a CRC from the file name aka [918154E1] or (918154E1)
  myRegexMatcher = file.name.replaceAll(/([\[(]([0-f]{8})[\])])/, "") =~ /(?i)(?<=.)(E)(\d{1,4})(?=.)/
  // for filenames like: (Which end up with myFileNameForParsing that don't really work
  // Black.Clover.E075..MULTISuBS.1080p.WEBRip.x265-SceneGuardians.mkv
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #15 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})\s[^-]*$/
  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(\s|\s-\s)[^-]*$/
  //  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})($|(\s|\s-\s)[^-]*$)/
  //  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,4})(?>v\d)?($|\s-\s|\s)/
  myRegexMatcher = file.name =~ /(?i)(^\d{1,4})(?<!(19\d\d|20\d\d))(?>v\d)?($|\s-\s|\s)/
  // For text like:
  // 01 The Lesson for Squirrel
  // 055 - The Stone Flower And Shippo s First Love
  // 04 - The Self-Styled Young Man And The Giant Wolf
  // 01
  // Yes this will falsely match titles that START with numbers, aka 11 Eyes. So I rely on later regex to match the episode correctly
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\s([\d]{1,3}\s?$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,3}$|[\d]{1,3})$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,4})$/
  // VOID - (?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,4})(?<!(19\d\d|20\d\d))$
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)(\sE?P?|\s#)([\d]{1,4})(?<!(19\d\d|20\d\d))$/
  // for text like:
  // Assault Lily - Bouquet 01
  // Wolfs Rain E01
  // Aria the Natural - Ep26
  // Keishichou Zero-gakari Season 4 EP03
  // Eromanga sensei #12
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(\d{1,4})(\sto\s)(\d{1,4})/
  // NOTE: 1.2 (above) will "match" the ending episode (21 from below)
  // for text like:
  //  My Senior Brother Has a Pit in His Brain Ep 17 to 21
  //  My Senior Brother Has a Pit in His Brain Ep 22 to 26 FINAL
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '') + '.' + myRegexMatcher[0][3].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #1.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // Manual override for Specific File Format
  myRegexMatcher = file.name =~ /(?i)^(\[TDG Season [\w]*\])(\[[^.]*\])(\[[^.]*\])/
  // for filenames like:
  // [TDG Season 4][121][1080P]EN Kurina Official.mp4
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceAll(/[\[\]]/, '')
    Logging.log.fine "\t #1.4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\d[a-z][a-z]\sSeason)\s([\d]{1,4})$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\d[a-z][a-z]\sSeason)\s([\d]{1,4})(?<!(19\d\d|20\d\d))$/
  // for text like:
  // ReZero kara Hajimeru Break Time 2nd Season 01 and variations of ordinal season syntax 1st, 2nd, 3rd, 4th, 5th, 6th, 7th, 8th, 9th
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t Ordinal Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])-[\s]*([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})[_\s]?(?>v\d)?[^-]*$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})(?<!(19\d\d|20\d\d))[_\s]?(?>v\d)?[^-]*$/
  // for text like:
  // Grisaia no Kajitsu - SP1
  // Grisaia no Kajitsu - 1
  // Grisaia no Kajitsu - #1
  // Grisaia no Kajitsu - Episode 13
  // Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5
  // Will also match some edge cases of TVDB detected Episodes
  // aka Re Zero kara Hajimeru Isekai Seikatsu - 2x08
  if (myRegexMatcher) {
    //  myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/(?i)(?![a-z0-9])([^-]*$)/, '').replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/(?i)(?![a-z0-9])([^-.]*$)/, '').replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t #2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}|[\d]{1,3})(\s-\s)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-)/
  // /(?i)(\s-[\s]*)([\d]{1,4})(?<!(19\d\d|20\d\d))\s{0,1}(?>v\d)?(\s?-)/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})(?<!(19\d\d|20\d\d))\s{0,1}(v\d)?(\s?-)/
  // For text like:
  // Cap Kakumei Bottleman - 07 - A Battle! Clash between Soft and Hard!
  // Zoids Wild Zero - 01- Birth! Beast Liger
  // ZENONZARDTHE ANIMATION Episode - 04 -Ash Undercover
  // Dragon`s Dogma - 03v2 - ONA
  // Soukyuu no Fafner Dead Aggressor - The Beyond - 04v2 - Powerless Person
  if ( myRegexMatcher ) {
    //      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    try {
      myDetectedEpisodeVersion = myRegexMatcher[0][4].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
    } catch (e) {
      Logging.log.finest "\t #3 No Episode Number Version detected"
    }
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4}-[\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
  // for text like:
  // Taiko no Tatsujin - 01-07 - The Birth of Don and Katsu!!
  if ( myRegexMatcher ) {
    Logging.log.finest "\t #3 Start"
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    myDetectedIntegerEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceAll(/(-[0-9]*)/, '') as Integer
    Logging.log.fine "\t #4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    Logging.log.fine "\t #4 myDetectedIntegerEpisodeNumber - Detected episode[${myDetectedIntegerEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    if ( returnOnlyIntegerEpisode ) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "").replaceAll(/(-[0-9]*)/, '')
//      Logging.log.fine "\t #4 returnOnlyIntegerEpisode - Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    } else {
//      Logging.log.fine "\t #4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    }
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,4})(?>v\d)?$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,4})(?<!(19\d\d|20\d\d))(?>v\d)?$/
  // For text like:
  // To LOVE-Ru Darkness Season S2 14
  // To LOVE-Ru Darkness Season S2 14v2
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #5 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)Special\s(\d{1,3})(?>v\d)?$/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,4}|SP\s?-?\s?\d{1,4}|OVA\s?-?\s?\d{1,4}|OAD\s?-?\s?\d{1,4})(?>v\d)?$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,4}|SP\s?-?\s?\d{1,4}|OVA\s?-?\s?\d{1,4}|OAD\s?-?\s?\d{1,4})(?<!(19\d\d|20\d\d))(?>v\d)?$/
  // for text like:
  // Hyakka Ryouran Samurai Bride Special 1
  // Hyakka Ryouran Samurai Bride Special - 1
  // Hyakka Ryouran Samurai Bride Sp 1
  // Hyakka Ryouran Samurai Bride Sp - 1
  // OVA - 1
  // OVA 1
  // But NOT text like
  // Special 2018
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceAll(/[a-zA-Z-]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #6.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    isSpecialType = true
    specialType = "special"
    Logging.log.finest "\t #6.1 Detected isSpecialType[${isSpecialType}]:(specialType?${specialType}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    if ( returnSpecialsType ) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
//      Logging.log.fine "\t #6.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    } else {
//      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceAll(/[a-zA-Z-]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
//      Logging.log.fine "\t #6.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    }
  }

  // myRegexMatcher = file.name =~ /(?i)(\s-[\s]*)([\d]{1,3}.\d)\s{0,1}(?>v\d)?(\s?-\s)/
  myRegexMatcher = file.name =~ /(?i)(\s-[\s]*)([\d]{1,4}\.\d)\s{0,1}(?>v\d)?(\s?-\s)/
  // For text like:
  // [DeadFish] Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5 - Special [720p][AAC].mp4
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #7 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)\s(\d{1,4})\s*-\s[^-0-9]*$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)\s(\d{1,4})(?<!(19\d\d|20\d\d))\s*-\s[^-0-9]*$/
  // For text like:
  // Code Geass - Boukoku no Akito 1 - Yokuryuu wa Maiorita
  // the Garden of sinners 08 - Epilogue
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  //  myRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\.\[(])((S\d{1,3}|\d{1,3})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)/
//  myRegexMatcher = myFileNameForParsing =~ /${constants.airDateOrderMatcherRegex}/
  myRegexMatcher = myFileNameForParsing =~ /${airDateOrderMatcherRegex}/
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations) - This is highly inaccurate ..
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t TVDB Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    tvdbSeasonalityDetected = true
    if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
      Logging.log.fine "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      // Not sure why this is here ..
//      if ( file.metadata ) {
//        myDetectedEpisodeNumber = any { file.metadata.episode } {null}
//        fileBotMetadataUsed = true
//      }
    }
  }

  myRegexMatcher = file.name =~ /${airDateOrderMatcherRegexStrict}/
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations) - This is highly inaccurate ..
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")  //.replaceFirst(/${stripEpisodeVersionInfoRegex}/, '')
    Logging.log.fine "\t TVDB #2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    tvdbSeasonalityDetected = true
    if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
      Logging.log.fine "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      // Not sure why this is here ..
//      if ( file.metadata ) {
//        myDetectedEpisodeNumber = any { file.metadata.episode } {null}
//        fileBotMetadataUsed = true
//      }
    }
  }

  // VOID - (?i)([\s]*Episode[\s]*)(SP\d{1,4}|\d{1,4}\.\d|\d{1,4})[_\s]?(?=[a-zA-Z])/
  // VOID - (?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})[\s](?=[a-zA-Z])(?!to)
  // VOID - (?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?!$)[\s](?=[a-zA-Z])(?!to)
  myRegexMatcher = myFileNameForParsing =~ /(?i)([\s]Episode[\s]|[\s]Ep[\s]?)(\d{1,4}\.\d|\d{1,4})(?!$)[\s](?=[a-zA-Z-])(?!to)/
  // For text like:
  // Qin's Moon S6 Episode 7 English
  // LCA - Spirit Realm Season 6 Ep 08 English
  // Legends Of Dawn-The Sacred Stone EP05 HD1080P X265. Enghish
  // Don Dracula - Episode 5 - Complete Success! Operation - Cheating Cribs
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing == /(?i)^(E)(\d{1,4})$/
  // For text like:
  // E1
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #8.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\s)(#(\d{1,4}))(?=\s-)/
  // For Text Like:
  // Dannaken #13 - Me, Her, And Another
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "")
    Logging.log.fine "\t #16 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // AniAdd Format #1 - Series
  myRegexMatcher = file.name =~ /${matchAniAddEpisodeFormatTwo}/
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2]
//    if ( myRegexMatcher[0][2].toString().startsWith(/[a-zA-Z]/) ) {
    if (myRegexMatcher[0][2] =~ /^[a-zA-Z]/ ) {
      isSpecialType = true
      specialType = "special"
//      myDetectedEpisodeNumber = "s" + myRegexMatcher[0][2].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")      // Remove Alpha then leading Zero's
    }
//    } else {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
//    }
//    myDetectedIntegerEpisodeNumber = myRegexMatcher[0][2].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove Alpha then leading Zero's
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    Logging.log.fine "\t #8.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    Logging.log.finest "\t #8.1 Detected isSpecialType[${isSpecialType}]:specialType[${specialType}] for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    try {
      myDetectedEpisodeVersion = myRegexMatcher[0][3].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "")
      Logging.log.finest "\t #8.1 Detected myDetectedEpisodeVersion[${myDetectedEpisodeVersion}] for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    } catch (e) {
      Logging.log.finest "\t #8.1 No Episode Number Version detected"
    }
//    if ( returnSpecialsType ) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
//      Logging.log.fine "\t #8.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    } else {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceAll(/[a-zA-Z]/, '').replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove Alpha then leading Zero's
//     Logging.log.fine "\t #8.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
//    }
  }

  if (!episodeProcessing) {
    // AniAdd Format #2 - Movies
    myRegexMatcher = file.name =~ /${matchAniAddMovieFormatOne}/
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1]
      Logging.log.fine "\t #9 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
      Logging.log.fine "\t Metadata: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }

  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    // VOID - (\d{1,4}(?<!(19\d\d|20\d\d))$)
    // VOID - (?<! s)(\d{1,4}(?<!(19\d\d|20\d\d))$)
    // VOID - ^\w*\d{1,3}$
    myRegexMatcher = myFileNameForParsing =~ /^[a-zA-Z\.]*(\d{1,3}?)$/
    // Matches up to 3 digits at the end of the line as long as there are only alpha and/or periods
    // TomicaKizunaGattaiEarthGranner45
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.1 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    myRegexMatcher = myFileNameForParsing =~ /(?i)(?i)(?<=[a-z0-9])(\s\d{1,2}\s)(?=[a-z0-9])/
    // Matches first two digits surrounded by spaces
    // Trigun 06 Lost July
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    // VOID - /(?i)(\d{1,2})\s*?(?<!Season\s|Part\s|[a-z0-9])(v[\d]{1,2}\b.*$)/
    myRegexMatcher = myFileNameForParsing =~ /(?i)(\d{1,2})\s*?(?<!Season\s|Part\s)(v[\d]{1,2}\b.*$)/
    // Matches up to 4 digits at the end of the line when followed by <space> vXX
    // Dual! Parallel Runrun Monogatari 14 v2
    // Wild Arms 06v2
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.3 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\s)(s([\d]{1,2}))(?=$)/
    // Matches up to 2 digits at the end of the line when followed by S and would be considered a Special
    // Ani Tore! EX S01
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
      Logging.log.fine "\t #10.4 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      isSpecialType = true
      specialType = "special"
      Logging.log.finest "\t #10.4 Detected isSpecialType[${isSpecialType}]:specialType[${specialType}] for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
    // This is here because it's likely to produce lots of false matches
    myRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
    // for text like:
    // Legend of the Galactic Heroes Gaiden.2x19
    if (myRegexMatcher) {
      myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")
      myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/${stripLeadingZerosRegex}/, "")
      Logging.log.fine "\t #10.5 - TVDB Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      tvdbSeasonalityDetected = true
      if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
        Logging.log.fine "\t #10.5 Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      }
    }
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(^#(\d{1,4})$)/
  // Matches up to 4 digits when it's just #\d\d\d\d
  // #01
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    Logging.log.fine "\t #14 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=-\s)(E(\d{1,2}))(?=\s-)/
  // Matches Edd surrounded by a space & Single Dash
  // A Day Before Us ZERO - E18 - Our Eighteen
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/${stripLeadingZerosRegex}/, "") // Remove leading Zero's
    Logging.log.fine "\t #12 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  if (myDetectedEpisodeNumber == null && !episodeProcessing) {
    // For Movies, myFileNameForParsing is generally not that far off..
    myDetectedEpisodeNumber = regexRemoveKeywords(myFileNameForParsing).replaceAll(/${stripYearDateRegex}/, '')
    Logging.log.fine "\t #11 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  if (myDetectedEpisodeNumber != null) {
    myDetectedEpisodeNumber = regexStep4(myDetectedEpisodeNumber as String)
  }

//  if ( returnOnlyIntegerEpisode && myDetectedEpisodeNumber != null) {
  if ( myDetectedEpisodeNumber != null) {
    try {
      myDetectedIntegerEpisodeNumber = myDetectedEpisodeNumber.toInteger()
    } catch (e) {
      if ( myDetectedIntegerEpisodeNumber == null) {
        Logging.log.fine "\t Detected episode[${myDetectedEpisodeNumber}] is not an Integer."
        myDetectedIntegerEpisodeNumber = null
      }
    }
  }
  Logging.log.fine "//--- Final: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected isSpecialType[${isSpecialType}]:(SpecialType?${specialType}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected myDetectedSeasonNumber[${myDetectedSeasonNumber}]:(myDetectedSeasonNumber?${myDetectedSeasonNumber}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected myDetectedIntegerEpisodeNumber[${myDetectedIntegerEpisodeNumber}]:(myDetectedSeasonNumber?${myDetectedIntegerEpisodeNumber}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  Logging.log.finest "//--- Final: Detected myDetectedEpisodeVersion[${myDetectedEpisodeVersion}]:(myDetectedEpisodeNumber?${myDetectedEpisodeNumber}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"


  // Should probably just return a map, with the episode number, season, special type etc.
  return [ myDetectedEpisodeNumber: myDetectedEpisodeNumber,  myDetectedEpisodeVersion: myDetectedEpisodeVersion, myDetectedSeasonNumber: myDetectedSeasonNumber, isSpecialType: isSpecialType, specialType: specialType, myDetectedIntegerEpisodeNumber: myDetectedIntegerEpisodeNumber]

//  if ( returnSeasonToo ) {
//    return [myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSeasonNumber: myDetectedSeasonNumber]
//  } else {
//    return myDetectedEpisodeNumber as String
////    if ( returnSpecialsType && isSpecialType ) {
////      return [myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSpecialType: specialType]
////    } else {
////      return myDetectedEpisodeNumber as String
////    }
//  }
}