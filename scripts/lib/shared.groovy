package lib
//--- VERSION 1.1.0

import groovy.time.TimeCategory
import java.nio.file.Path
import java.util.zip.GZIPInputStream

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
  stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|;|-|\||\[|]|((?![\.]+$)([\.]))|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/\s/, '')
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
  stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|;|-|\||\[|]|((?![\.]+$)([\.]))|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
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
  log.finer "\nFile: [$f]"
  MediaInfo.snapshot(f).each{ kind, streams ->
    // find optimal padding
    def pad = streams*.keySet().flatten().collect{ it.length() }.max()

    streams.each { properties ->
      log.fine "\nKind: [$kind]"
      properties.each{ k,v ->
        println "Property [${k.padRight(pad)}] : $v"
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
    println '---------- Adding Hepburn Romanisation of Particle を as o (instead of wo)'
    animeName = animeName.replaceAll(/(?i)(\swo\s)/, ' o ')
  }
  if (animeName =~ /(?i)(\she\s)/) {
    println '---------- Adding Hepburn Romanisation of Particle へ as e (instead of he)'
    animeName = animeName.replaceAll(/(?i)(\she\s)/, ' e ')
  }
  if (animeName =~ /(?i)(\sha\s)/) {
    println '---------- Adding Hepburn Romanisation of Particle は as wa (instead of ha)'
    animeName = animeName.replaceAll(/(?i)(\sha\s)/, ' wa ')
  }
  if (animeName =~ /(?i)(\sand\s)/) {
    println '---------- Adding AniDBSyntax variation of & instead of and'
    animeName = animeName.replaceAll(/(?i)(\sand\s)/, ' & ')
  }
  return animeName
}

// Step #1 - "Normalize" spacing character
// - Covert _ to Space ==> replaceAll(/_/, ' ')
// - Convert { to [ and } to ] ==> replaceAll(/{/, '[').replaceAll(/\/, ']')
// - Convert all but the last period to a space ==> replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ')
/**
 * Filename Mangling Regex Step #1
 * Step #1 - "Normalize" spacing character
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep1(String name) {
  return name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ')
}

// Step #2 - Remove data infomation
// - Remove File Extension ==> replaceAll(/(\.\d)?\.[^.]+$/, '')
// VOID - (rev)?(\.\d)?\.[^.]+$
// VOID - Remove Starting Bracket/Parenthesis info ==>  replaceAll(/^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&~.!#$%@]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@]*\)){0,10}/, '')
// VOID - replaceAll(/^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&\(\)~.!#$%@\+]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@\+]*\)){0,10}/, '')
// - replaceAll(/(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/, '')
// VOID - Remove all Ending Bracket/Parenthesis info unless it's likely a 4 digit date ==> replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&~.!#$%@]*\)){0,10}$/, '')
// - replaceAll(/((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/, '')
// - Remove any leftover metadata keywords ==> replaceAll(/(?i)(\bFLAC\b|\b480p\b|\bAC-3\b|\bPromo\b}\bcht\b|\bchs\b|\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, '')
// VOID - Remove all brackets/paren that are not YYYY ==> replaceAll(/([\[\(](?!\d\d\d\d).*[\]\)])/, '')
// - replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/, '')
// - Replace brackets around [YYYY] with (YYYY) ==> replaceAll(/\[/, '(').replaceAll(/\]/, ')')
/**
 * Filename Mangling Regex Step #2
 * Step #2 - Remove data infomation
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep2(String name) {
  return name.replaceAll(/(\.\d)?\.[^.]+$/, '').replaceAll(/(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/, '').replaceAll(/((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/, '').replaceAll(/(?i)(\bBlu-ray\b|\b-DE$|\bWebRip\b|\bJPTVTS\b|\b4k\b|\bENGSUB\b|\bRAW$|\bflac\b|\b480p\b|\bac-3\b|\bpromo\b|\bcht\b|\bchs\b|\bvp9\b|\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, ' ').replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/, '').replaceAll(/\[/, '(').replaceAll(/\]/, ')')
}

// Step #4 - Strip out leading/trailing spaces
// - Remove leading spaces/dash ==> replaceAll(/^([\s-])*/, '')
// - Remove trailing spaces/dash ==> replaceAll(/([\s-])*$/, '')
// - Shorten multiple spaces to a single space ==> replaceAll(/(\s){2,20}/, ' ')
// - Convert to all lowercase ==> toLowerCase()
/**
 * Filename Mangling Regex Step #4
 * Step #2 - Remove data infomation
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexStep4(String name) {
  return name.replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '').replaceAll(/(\s){2,20}/, ' ').toLowerCase()
}

/**
 * Remove specific keywords from a string (Usually a filename string)
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexRemoveKeywords(String name) {
  return name.replaceAll(/(?i)(\bBlu-ray\b|\b-DE$|\bWebRip\b|\bJPTVTS\b|\b4k\b|\bENGSUB\b|\bRAW$|\bflac\b|\b480p\b|\bac-3\b|\bpromo\b|\bcht\b|\bchs\b|\bvp9\b|\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, ' ').replaceAll(/(\s){2,20}/, ' ')
}


/**
 * Regex mangle a string (aka the name of a file) in preperation for a script to parse and figure out the Anime Series name, Seasonality Syntax etc.
 *
 * @param name The String we want to regex mangle
 * @return The mangled string
 */
String regexBlender(String name) {
  String step1 = regexStep1(name)
  String step2 = regexStep2(step1)
  //
// Step #3 - Remove Episode Information
// VOID - Remove the Episode Info from Series Formatted Portion SxxExx ==> replaceFirst(/(?i)(?<=\d)E\d{1,2}/, '')
// VOID - replaceFirst(/(?i)(?<=\d)E\d{1,2}v?\d?/, '')
// VOID - replaceFirst(/(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)/, '')
// TRIAL - Remove the Episode Info from Series Formatted Portion SxxExx and everything after it ==> replaceFirst(/(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)\b.*$/, '')
// VOID - Remove the Absolute Episode info when there is episode/ep and the # to end of line ==> replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '')
// VOID - replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|#)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '')
// VOID - replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|#|e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '')
// VOID - replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '')
// Trial - replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '')
// VOID - Remove the Absolute Episode info when it's a dash space(s) then digits to end of line==> replaceAll(/(?i)(?<!Season)(?<!Part)(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/, '')
// TRIAL - replaceAll(/(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/, '')
// - Remove - vol/sp in the name ==> replaceAll(/(?i)(?<!Season)(?<!Part)(-[\s]+(vol|sp)\b.*$)/, '')
// VOID - Remove trailing numbers from the end of the line, need to be very careful on exceptions ==> replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}\s?$)/, '')
// VOID - replaceAll(/(?i)(?<!\d\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{2,3}\s?$|\s[\d]{2,3}v[\d]{1,2}\s?)/, '')
//
  String step3 = step2.replaceFirst(/(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)\b.*$/, '').replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '').replaceAll(/(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/, '').replaceAll(/(?i)(?<!Season)(?<!Part)(-[\s]+(vol|sp)\b.*$)/, '')
  String step4 = regexStep4(step3)
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
    case 'first':
      return 1
    case 'second':
      return 2
    case 'third':
      return 3
    case 'fourth':
      return 4
    case 'fifth':
      return 5
    case 'sixth':
      return 6
    case 'seventh':
      return 7
    case 'eighth':
      return 8
    case 'ninth':
      return 9
    case 'tenth':
      return 10
    default:
      return 'I did not want to add number verbs past tenth so there.'
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

