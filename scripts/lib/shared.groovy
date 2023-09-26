//file:noinspection GrMethodMayBeStatic
//file:noinspection unused
//file:noinspection RegExpRedundantEscape
package lib
//--- VERSION 1.6.0

import groovy.time.TimeCategory
import net.filebot.Logging

import java.nio.file.Path
import java.util.zip.GZIPInputStream
import groovy.transform.Field

// Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
// Strip out the following UNLESS they are at the end of the string ` or ! or . why? Because there are a few AniDB Series where the ending punctuation is the ONLY difference between multiple series.
@Field String jwdStripSpecialCharaters = /(@|"|,|_|\\|\/|:|;|-|\||\[|]|((?![\.]+$)([\.]))|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/
// 1 - Strip out by regex (Just the regex)
@Field String stripMetadataKeywordsWordComplex = /(?i)\b(BD)(?=\s(1024x576[p]?|1280x720[p]?|640480[p]?|768x576[p]?|1448x1080[p]?|1408x1056[p]?|1920x1080[p]?))|\b(BD[,\s]{1,2}(540|720|1080|480|800)p?)\b|\b(1024x576[p]?|1280x720[p]?|640480[p]?|768x576[p]?|1448x1080[p]?|1408x1056[p]?|1920x1080[p]?)\b|\b(dual|tri)[- .]audio\b|\bbd[- .]remux\b|\b(8|10)-bit\b|\bh\s?(264|265)\b/
// 2 -  Strip out Keywords surrounded by word boundry
@Field String stripMetadataKeywordsWordBoundary = /theatrical edition|Remaster|LCA|H\.264|flac2|EngSub|THORA|EAC3|HDR10|QAAC|480p|SDR|Blu-ray|WebRip|bJPTVTS|4k|bENGSUB|flac|ac-3|promo|cht|chs|vp9|remux|restored|korean|NVENC|uhd|truehd|ttga|dvdrip|hr-sr|rencode|r2fr|divx|vostfr|rus|jap|deadmauvlad|hd1080|nooped|Multi-Subs|MULTi|2ch|subtitles|tv|hr-rg|hd720|web-dl|web|opus|opus-m3d|DVD|AC3|AAC|dts|ita|eng|sub/
// 3- Strip out Misc Regex (Just the regex)
@Field String stripMetadataKeywordsWordMisc = /(?i)\b(-DE|RAW)$|\sbd\sbox\s|(?<=\]\s)BD(?=\s\[)/
// 4 - Strip out Metadata Keywords (Just the keyword)
@Field String stripMetadataKeywordsOnly = /HDMA-RONiN|DD5\.1|NTSC|-Krispy|-SceneGuardians|-WiCkEd|LM ANIME|-NanDesuKa|GameRip|-DucksterPS|-RONiN|-YURASUKA|-sLaX|WEB-DL|540p|800p|hd720blu|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|120fps|HEVC|HDTV|Subbed|MultiSub/
@Field String stripMultipleAdjacentSpaces = /(\s){2,20}/
@Field String stripTrailingSpacesDashRegex = /([\s-])*$/
@Field String stripLeadingSpacesDashRegex = /^([\s-])*/
// - Convert all but the last (two) period to a space
// VOID - /\.(?!(\d\.[^.]+$|[^.]+$))/
@Field String stripAllPeriodsExceptLast = /\.(?!(\d\.[^.]+$|[^.]+$|$))/
// - Remove File Extension
// VOID - /(\.\d)?\.[^.]+$/
// VOID - /(\.\d)?\.\w\w\w$/
// VOID - /(?<!\s\d{1,4})(\.\d)?\.\w{3}$/
@Field String removeFileExtensionRegex = /((?<!\s\d{1,4})(?>\.\d)?\.\w{3}$|\.\w{3}$)/
// - Remove Starting Bracket/Parenthesis info
// VOID - /^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&~.!#$%@]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@]*\)){0,10}/
// VOID - /^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&\(\)~.!#$%@\+]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@\+]*\)){0,10}/
// VOID - /(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/
@Field String removeStartingBracketParenthesis = /(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/
// - Remove all Ending Bracket/Parenthesis info unless it's likely a 4 digit date
// VOID - /(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&~.!#$%@]*\)){0,10}$/
// VOID - /((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/
@Field String removeEndingBracketParenthesis = /((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/
// - Remove all brackets/paren that are not YYYY
// VOID - /([\[\(](?!\d\d\d\d).*[\]\)])/
// VOID - /(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/
@Field String removeBracketsParenthesisOtherThenDate = /(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/
// Remove the Episode Info from Series Formatted Portion SxxExx and everything after it.
// VOID - /(?i)(?<=\d)E\d{1,2}/
// VOID - /(?i)(?<=\d)E\d{1,2}v?\d?/
// VOID - /(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)/
@Field String removeSeriesEpisodeInfo = /(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)\b.*$/
// - Remove the Absolute Episode info when there is episode/ep and the # to end of line
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep|#)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep|#|e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/
// VOID - /(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,4}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,4}\b.*$)/
@Field String removeAbsoluteEpisodeInfo = /(?i)([-\s]+(episode|number)[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,4}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,4}\b.*$)/
// - Remove the Absolute Episode info when it's a dash space(s) then digits to end of line
// VOID - /(?i)(?<!Season)(?<!Part)(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/
// VOID - /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/
@Field String removeAbsoluteEpisodeWithDashInfo = /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*[\d]{1,4}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,4}\b.*$)/
// - Remove the version/revision info when it's at the end of the line (aka something 01 v2)
@Field String removeVersionAtEndOfLine = /(?i)(?<!Season\s|Part\s|[a-z0-9])(v[\d]{1,2}\b.*$)/
// - Remove the text after a number followed by dash till end of line as long as there are NO digits!
@Field String removeEndingTextAfterNumberDash = /(?i)(?>[\d]{1,4})(\s?-[^\d]*?$)/
// - Remove - vol/sp in the name
@Field String removeVolSp = /(?i)(?<!Season)(?<!Part)(-[\s]+(vol|sp)\b.*$)/


/**
 * Return the URL from a redirect
 * https://stackoverflow.com/questions/39718059/read-from-url-in-groovy-with-redirect?rq=1
 * https://anidb.net/perl-bin/animedb.pl?show=cmt&id=90537
 *
 * @param url The url to follow
 * @return  url from redirect (if there is one)
 */
URL findRealUrl(URL url) {
    HttpURLConnection conn = url.openConnection()
    conn.followRedirects = false
    conn.requestMethod = 'GET'
    if (conn.responseCode in [301, 302]) {
        if (conn.headerFields.'Location') {
          return findRealUrl(conn.headerFields.Location.first().toURL())
        } else {
            throw new RuntimeException('Failed to follow redirect')
        }
    }
    return url
}

/**
 * Decompress a gzip file (leaving the original .gz file intact)
 * https://mkyong.com/java/how-to-decompress-file-from-gzip-file/
 *
 * @param source The Path to the Source File
 * @param target The Path to decompress the file into
 * @void
 */
void decompressGzip(Path source, Path target) throws IOException {
  GZIPInputStream gis = new GZIPInputStream( new FileInputStream(source.toFile()))
  FileOutputStream fos = new FileOutputStream(target.toFile())
  // copy GZIPInputStream to FileOutputStream
  byte[] buffer = new byte[1024]
  int len
  while ((len = gis.read(buffer)) > 0) {
    fos.write(buffer, 0, len)
  }
}

/**
 * Take an Anime Series name and regex it to hell into a form we will use when doing a JWD compare that *usually* works
 * well for that kind of thing. This is the version that strips out all the spaces from the returned string
 *
 * @param name Anime Series Name
 * @return String that has been regex blended to a form we will be using when doing an JWD compare without any spaces.
 */
String altjwdStringBlender(String name) {
  // --- What I want it to do...
  // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
  // Strip out the following UNLESS they are at the end of the string ` or ! or . why? Because there are a few AniDB Series where the ending punctuation is the ONLY difference between multiple series.
  // Replace all characters removed with a space, and then squish it down by removing all spaces :)
  // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/\s/, '')) : (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/\s/, ''))
  // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/\s/, '')
  // stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|;|-|\||\[|]|\.|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/\s/, '')
  stringMangled = name.toLowerCase().replaceAll(/${jwdStripSpecialCharaters}/, ' ').replaceAll(/\s/, '')
  return stringMangled
}


/**
 * Take an Anime Series name and regex it to hell into a form we will use when doing a JWD compare that *usually* works
 * well for that kind of thing.
 *
 * @param name Anime Series Name
 * @return String that has been regex blended to a form we will be using when doing an JWD compare, shrinking any concurrently occuring spaces to a single space.
 */
String jwdStringBlender(String name) {
  // --- What I want it to do...
  // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
  // Strip out the following UNLESS they are at the end of the string `!
  // Replace all characters removed with a space, and make sure there is a maximum of 1 space between non-spaces.
  // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/(\s){2,20}/, ' ')) : (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/(\s){2,20}/, ' '))
  // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  // stringMangled = name.toLowerCase().replaceAll(/(@|"|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|((?![`]+$)([`]))|\&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  // stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|;|-|\||\[|]|\.|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  stringMangled = name.toLowerCase().replaceAll(/${jwdStripSpecialCharaters}/, ' ').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ')
  return stringMangled
}

/**
 * Given a starting Date and Ending Date, calculate the # of day's between them.
 *
 * @param startDate
 * @param endDate
 * @return String that has been regex blended to a form we will be using when doing an JWD compare, shrinking any concurrently occuring spaces to a single space.
 */
def daysBetween(def startDate, def endDate) {
    use(TimeCategory) {
        def duration = endDate - startDate
        return duration.days
    }
}

/**
 * Log all mediainfo filebot can see for a file.
 * taken from mediainfo.groovy script: https://github.com/filebot/scripts/blob/master/mediainfo.groovy
 *
 * @param f File we want mediainfo for
 * @void
 */
void printAllMediaInfo(f) {
  Logging.log.finer "\nFile: [$f]"
  MediaInfo.snapshot(f).each{ kind, streams ->
    // find optimal padding
    def pad = streams*.keySet().flatten().collect{ it.length() }.max()

    streams.each { properties ->
      Logging.log.info "\nKind: [$kind]"
      properties.each{ k,v ->
        Logging.log.info "Property [${k.padRight(pad)}] : $v"
      }
    }
  }
}

/**
 * Given an input String, return the string with some common AniDB Romanisation applied to it.
 *  https://wiki.anidb.net/AniDB_Definition:Romanisation#Hepburn_romanisation
 *  https://wiki.anidb.net/AniDB_Definition:Romanisation_for_Chinese
 *
 * @param animeName The String we want to evaluate
 * @return The anime name with any Hepburn Romanisation applied to it (there may be no changes)
 */
String returnAniDBRomanization(String animeName) {
  if (animeName =~ /(?i)(\swo\s)/) {
    Logging.log.info '---------- Adding Hepburn Romanisation of Particle を as o (instead of wo)'
    animeName = animeName.replaceAll(/(?i)(\swo\s)/, ' o ')
  }
  if (animeName =~ /(?i)(\she\s)/) {
    Logging.log.info '---------- Adding Hepburn Romanisation of Particle へ as e (instead of he)'
    animeName = animeName.replaceAll(/(?i)(\she\s)/, ' e ')
  }
  if (animeName =~ /(?i)(\sha\s)/) {
    Logging.log.info '---------- Adding Hepburn Romanisation of Particle は as wa (instead of ha)'
    animeName = animeName.replaceAll(/(?i)(\sha\s)/, ' wa ')
  }
  if (animeName =~ /(?i)(\sand\s)/) {
    Logging.log.info '---------- Adding AniDBSyntax variation of & instead of and'
    animeName = animeName.replaceAll(/(?i)(\sand\s)/, ' & ')
  }
  return animeName
}

// Step #1 - "Normalize" spacing character
// - Covert _ to Space ==> replaceAll(/_/, ' ')
// - Convert { to [ and } to ] ==> replaceAll(/{/, '[').replaceAll(/\/, ']')
// - Convert all but the last period to a space
/**
 * Filename Mangling Regex Step #1
 * Step #1 - "Normalize" spacing character
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep1(String name) {
  return name.replaceAll(/_/, ' ')
          .replaceAll(/\{/, '[')
          .replaceAll(/\}/, ']')
          .replaceAll(/${stripAllPeriodsExceptLast}/, ' ')
}

// Step #2 - Remove data infomation
// - Remove File Extension
// - Remove Starting Bracket/Parenthesis info
// - Remove all Ending Bracket/Parenthesis info unless it's likely a 4 digit date
// - Remove any leftover metadata keywords
// - Remove all brackets/paren that are not YYYY
// - Replace brackets around [YYYY] with (YYYY) ==> replaceAll(/\[/, '(').replaceAll(/\]/, ')')
/**
 * Filename Mangling Regex Step #2
 * Step #2 - Remove data infomation
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep2(String name) {
  String temp = name.replaceAll(/${removeFileExtensionRegex}/, '')
      .replaceAll(/${removeStartingBracketParenthesis}/, '')
      .replaceAll(/${removeEndingBracketParenthesis}/, '')
  return regexRemoveKeywords(temp)
      .replaceAll(/${removeBracketsParenthesisOtherThenDate}/, '')
      .replaceAll(/\[/, '(')
      .replaceAll(/\]/, ')')
}

// Step #4 - Strip out leading/trailing spaces
// - Remove leading spaces/dash
// - Remove trailing spaces/dash
// - Shorten multiple spaces to a single space
// - Convert to all lowercase ==> toLowerCase()
/**
 * Filename Mangling Regex Step #4
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep4(String name) {
  return name.replaceAll(/${stripLeadingSpacesDashRegex}/, '')
      .replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      .replaceAll(/${stripMultipleAdjacentSpaces}/, ' ')
      .toLowerCase()
}

/**
 * Remove specific keywords from a string (Usually a filename string)
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexRemoveKeywords(String name) {
  return name.replaceAll(/${stripMetadataKeywordsWordComplex}/, ' ')
      .replaceAll(/(?i)\b(/ + stripMetadataKeywordsWordBoundary + /)\b/, ' ')
      .replaceAll(/${stripMetadataKeywordsWordMisc}/, ' ')
      .replaceAll(/${stripMetadataKeywordsOnly}/, ' ')
      .replaceAll(/${stripMultipleAdjacentSpaces}/, ' ')
}


/**
 * Regex mangle a string (aka the name of a file) in preperation for a script to parse and figure out the Anime Series name, Seasonality Syntax etc.
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexBlender(String name) {
  Logging.log.fine "regexBlender:[${name}]"
  String step1 = regexStep1(name)
  Logging.log.fine "regexBlender - Step 1:[${step1}]"
  String step2 = regexStep2(step1)
  Logging.log.fine "regexBlender - Step 2:[${step2}]"
// Step #3 - Remove Episode Information
// - Remove the Episode Info from Series Formatted Portion SxxExx and everything after it
// - Remove the Absolute Episode info when there is episode/ep and the # to end of line
// - Remove the Absolute Episode info when it's a dash space(s) then digits to end of line
// - Remove - vol/sp in the name
// - Remove vxx at the end of the line (aka the version or revision) when it's got a space in front of it.
// VOID - Remove trailing numbers from the end of the line, need to be very careful on exceptions
// VOID - replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}\s?$)/, '')
// VOID - replaceAll(/(?i)(?<!\d\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{2,3}\s?$|\s[\d]{2,3}v[\d]{1,2}\s?)/, '')
  String step3 = step2.replaceFirst(/${removeSeriesEpisodeInfo}/, '')
      .replaceAll(/${removeAbsoluteEpisodeInfo}/, '')
      .replaceAll(/${removeAbsoluteEpisodeWithDashInfo}/, '')
      .replaceAll(/${removeVolSp}/, '')
      .replaceAll(/${removeVersionAtEndOfLine}/, '')
  Logging.log.fine "regexBlender - Step 3:[${step3}]"
  String step4 = regexStep4(step3)
  Logging.log.fine "regexBlender - Step 4:[${step4}]"
  return step4
}

/**
 * Return the parent file path of a file
 *
 * @param f The file
 * @return The parent file path of the file
 */
def parentFilePath(f) {
  return f.getParentFile().getName()
}

/**
 * Return the Roman Ordinal for numbers 1-10 (aka I, II, IV etc)
 *
 * @param i A number from 1-10
 * @return The Roman Numeral
 */
String getRomanOrdinal(int i) {
  switch (i) {
    case 1:
      return 'I'
    case 2:
      return 'II'
    case 3:
      return 'III'
    case 4:
      return 'IV'
    case 5:
      return 'V'
    case 6:
      return 'VI'
    case 7:
      return 'VII'
    case 8:
      return 'VIII'
    case 9:
      return 'IX'
    case 10:
      return 'X'
    default:
      return 'I didnt want to add roman ordinal past 10, so there.'
  }
}

/**
 * Return the number from an Roman Ordinal for numbers 1-10 (aka I, II, IV etc)
 *
 * @param ordinal
 * @return The Roman Numeral
 */
Integer getNumberFromRomanOrdinal(String ordinal) {
  switch (ordinal.toLowerCase()) {
    case ~/i/:
      return 1
      break
    case ~/ii/:
      return 2
      break
    case ~/iii/:
      return 3
      break
    case ~/iv/:
      return 4
      break
    case ~/v/:
      return 5
      break
    case ~/vi/:
      return 6
      break
    case ~/vii/:
      return 7
      break
    case ~/viii/:
      return 8
      break
    case ~/ix/:
      return 9
      break
    case ~/x/:
      return 10
      break
    case ~/xi/:
      return 11
      break
    case ~/xii/:
      return 12
      break
    case ~/xiii/:
      return 13
      break
    case ~/xiv/:
      return 14
      break
    case ~/xv/:
      return 15
      break
    case ~/xvi/:
      return 16
      break
    case ~/xvii/:
      return 17
      break
    case ~/xviii/:
      return 18
      break
    case ~/xix/:
      return 19
      break
    case ~/xx/:
      return 20
      break
    default:
      return 99
  }
}

/**
 * Return the Word for numbers 1-10 (aka first, second etc)
 *
 * @param i A number from 1-10
 * @return The word ordinal
 */
String getWordNumber(int i) {
  switch (i) {
    case 1:
      return 'first'
    case 2:
      return 'second'
    case 3:
      return 'third'
    case 4:
      return 'fourth'
    case 5:
      return 'fifth'
    case 6:
      return 'sixth'
    case 7:
      return 'seventh'
    case 8:
      return 'eighth'
    case 9:
      return 'ninth'
    case 10:
      return 'tenth'
    default:
      return 'I did not want to add number verbs past tenth so there.'
  }
}

/**
 * Return the number for word numbers first, second etc
 *
 * @param i The number as a word from first to tenth
 * @return The word ordinal
 */
Integer getWordNumber(String i) {
  switch (i) {
    case ['first', 'one']:
      return 1
    case ['second', 'two']:
      return 2
    case ['third', 'three']:
      return 3
    case ['fourth', 'four']:
      return 4
    case ['fifth', 'five']:
      return 5
    case ['sixth', 'six']:
      return 6
    case ['seventh', 'seven']:
      return 7
    case ['eighth', 'eight']:
      return 8
    case ['ninth', 'nine']:
      return 9
    case ['tenth', 'ten']:
      return 10
    default:
      return 0
//      return 'I did not want to add number verbs past tenth so there.'
  }
}

/**
 * Return the Ordinal for an integer (1st, 2nd, 3rd etc)
 * from Stack Overflow
 *  https://stackoverflow.com/questions/6810336/is-there-a-way-in-java-to-convert-an-integer-to-its-ordinal-name
 *
 * @param i The number as a word from first to tenth
 * @return The word ordinal
 */
String getOrdinalNumber(int i) {
  ArrayList sufixes = ['th', 'st', 'nd', 'rd', 'th', 'th', 'th', 'th', 'th', 'th']
  switch (i % 100) {
  case 11:
  case 12:
  case 13:
      return i + 'th'
  default:
    return i + sufixes[i % 10]
  }
}

/**
 * Given an Array of files, which are all under the same directory, return the root directory that all files share
 * @param files
 * @return String - root directory shared by all files
 */
def getShortestSharedPath(ArrayList files){
  LinkedHashMap sharedDirectoryMap = files.collect { f ->
    def parentDirectoryLength = f.getParentFile().toString().length().toInteger()
    return ["${parentDirectoryLength}": f.getParentFile()]
  }.unique().sort { it.key }.first()
  def mapKey = sharedDirectoryMap.keySet()[0]
  return sharedDirectoryMap.get(mapKey)
}

/**
 * Given an Array of files, which are all under the same directory, return the longest directory path that all files share
 * As I'm on windows, I needed both a delim and join separator as I couldn't figure out how to get it to work using \ for
 * both
 * Based on code from here: https://rosettacode.org/wiki/Find_common_directory_path#Groovy
 * @param files
 * @return String - root directory shared by all files
 */
@SuppressWarnings('GroovyAssignabilityCheck')
def commonPath(String delim, String join, ArrayList<File> files){
  def pathParts = files.collect { it.toString().split(delim) }
  return pathParts.transpose().inject([match:true, commonParts:[]]) { aggregator, part ->
    aggregator.match = aggregator.match && part.every { it == part[0] }
    if (aggregator.match) { aggregator.commonParts << part[0] }
    aggregator
  }.commonParts.join(join)
}

/**
 * Given the number of seconds return the number of hours, minues, seconds
 * https://stackoverflow.com/questions/6118922/convert-seconds-value-to-hours-minutes-seconds
 *
 * @param seconds
 * @return String
 */
String convertSecondsToHMS(int seconds) {
  int h = seconds/ 3600;
  int m = (seconds % 3600) / 60;
  int s = seconds % 60;
  String sh = (h > 0 ? String.valueOf(h) + " " + "h" : "");
  String sm = (m < 10 && m > 0 && h > 0 ? "0" : "") + (m > 0 ? (h > 0 && s == 0 ? String.valueOf(m) : String.valueOf(m) + " " + "min") : "");
  String ss = (s == 0 && (h > 0 || m > 0) ? "" : (s < 10 && (h > 0 || m > 0) ? "0" : "") + String.valueOf(s) + " " + "sec");
  return sh + (h > 0 ? " " : "") + sm + (m > 0 ? " " : "") + ss;
}