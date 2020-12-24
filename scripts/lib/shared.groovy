package lib
//--- VERSION 1.0.1

import groovy.time.TimeCategory

import java.nio.file.Path
import java.util.zip.GZIPInputStream

// ---------- Function to return the URL from a redirect ---------- //
// https://stackoverflow.com/questions/39718059/read-from-url-in-groovy-with-redirect?rq=1
// https://anidb.net/perl-bin/animedb.pl?show=cmt&id=90537
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

// ---------- Function to decompress a gzip file (leaving the original .gz file intact) ---------- //
// https://mkyong.com/java/how-to-decompress-file-from-gzip-file/
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

// ---------- Function to Strip various punctiation AND spaces from string for JWD comparisions ---------- //
// String altjwdStringBlender(String name, Boolean keepExlamation = true) {
//   // --- What I want it to do...
//   // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
//   // Strip out the following UNLESS they are at the end of the string `!
//   // Replace all characters removed with a space, and then squish it down by removing all spaces :)
//   // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/\s/, '')) : (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/\s/, ''))
//   // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/\s/, '')
//   stringMangled = name.toLowerCase().replaceAll(/(@|"|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|((?![`]+$)([`]))|\&|((?![!]+$)([!])))/, ' ').replaceAll(/\s/, '')
//   return stringMangled
// }

String altjwdStringBlender(String name) {
  // --- What I want it to do...
  // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
  // Strip out the following UNLESS they are at the end of the string `!
  // Replace all characters removed with a space, and then squish it down by removing all spaces :)
  // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/\s/, '')) : (stringMangled = name.toLowerCase().replaceAll(/(\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/\s/, ''))
  // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/\s/, '')
  stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|-|\||\[|]|\.|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/\s/, '')
  return stringMangled
}

// ---------- Function to Strip various punctiation but leave a space from string for JWD comparisions ---------- //
// String jwdStringBlender(String name, Boolean keepExlamation = true) {
//   // --- What I want it to do...
//   // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
//   // Strip out the following UNLESS they are at the end of the string `!
//   // Replace all characters removed with a space, and make sure there is a maximum of 1 space between non-spaces.
//   // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/(\s){2,20}/, ' ')) : (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/(\s){2,20}/, ' '))
//   // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/(\s){2,20}/, ' ')
//   stringMangled = name.toLowerCase().replaceAll(/(@|"|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|((?![`]+$)([`]))|\&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
//   return stringMangled
// }

String jwdStringBlender(String name) {
  // --- What I want it to do...
  // Strip out the following characters anywhere in the string @",_\/:-|[].~()?'
  // Strip out the following UNLESS they are at the end of the string `!
  // Replace all characters removed with a space, and make sure there is a maximum of 1 space between non-spaces.
  // keepExlamation == true ? (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&)/, ' ').replaceAll(/(\s){2,20}/, ' ')) : (stringMangled = name.toLowerCase().replaceAll(/(\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!)/, ' ').replaceAll(/(\s){2,20}/, ' '))
  // stringMangled = name.toLowerCase().replaceAll(/("|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|\`(?!$)(?!`)|\&|\!(?!$)(?!\!))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  // stringMangled = name.toLowerCase().replaceAll(/(@|"|,|\_|\\|\/|\:|\-|\||\[|\]|\.|\~|\(|\)|\?|\'|((?![`]+$)([`]))|\&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  stringMangled = name.toLowerCase().replaceAll(/(@|"|,|_|\\|\/|:|-|\||\[|]|\.|~|\(|\)|\?|'|((?![`]+$)([`]))|&|((?![!]+$)([!])))/, ' ').replaceAll(/(\s){2,20}/, ' ')
  return stringMangled
}

def daysBetween(def startDate, def endDate) {
    use(TimeCategory) {
        def duration = endDate - startDate
        return duration.days
    }
}

// taken from mediainfo.groovy script: https://github.com/filebot/scripts/blob/master/mediainfo.groovy
def printAllMediaInfo(f) {
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

// "Manual 3" - Process (Winner?) - Using Regex to prune something that might the start of an Anime Series Name
// Pre-Step #1 - Skip if it's something that just doesn't work ..
//  - If it's "I use periods instead of spaces and brackets" format ..
//  - Is the DetectedAnimeName from Filebot (assuming it's not null)
//
//
// Step #1 - "Normalize" spacing character
// - Covert _ to Space ==> replaceAll(/_/, ' ')
// - Convert { to [ and } to ] ==> replaceAll(/{/, '[').replaceAll(/\/, ']')
// - Convert all but the last period to a space ==> replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ')
//
// Step #2 - Remove data infomation
// - Remove File Extension ==> replaceAll(/(\.\d)?\.[^.]+$/, '')
// VOID - Remove Starting Bracket/Parenthesis info ==>  replaceAll(/^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&~.!#$%@]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@]*\)){0,10}/, '')
// VOID - replaceAll(/^(\s?\[(?!\[\d\d\d\d\])[\w-\s\'&\(\)~.!#$%@\+]*\]|\s?\((?!\(\d\d\d\d\))[\w-\s\'&~.!#$%@\+]*\)){0,10}/, '')
// TEST - replaceAll(/(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/, '')
// VOID - Remove all Ending Bracket/Parenthesis info unless it's likely a 4 digit date ==> replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&~.!#$%@]*\)){0,10}$/, '')
// TEST - replaceAll(/((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/, '')
// - Remove any leftover metadata keywords ==> replaceAll(/(?i)(\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, '')
// VOID - Remove all brackets/paren that are not YYYY ==> replaceAll(/([\[\(](?!\d\d\d\d).*[\]\)])/, '')
// TRIAL - replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/, '')
// - Replace brackets around [YYYY] with (YYYY) ==> replaceAll(/\[/, '(').replaceAll(/\]/, ')')
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
// Step #4 - Strip out leading/trailing spaces
// - Remove leading spaces/dash ==> replaceAll(/^([\s-])*/, '')
// - Remove trailing spaces/dash ==> replaceAll(/([\s-])*$/, '')
// - Shorten multiple spaces to a single space ==> replaceAll(/(\s){2,20}/, ' ')
// - Convert to all lowercase ==> toLowerCase()
//
// Step #5 - Review the name to see if we need some additional attention

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

String regexStep1(String name) {
  return name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ')
}

String regexStep4(String name) {
  return name.replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '').replaceAll(/(\s){2,20}/, ' ').toLowerCase()
}

// TODO
// Figure out how to use a variable for the keyword removal, and use that single variable here, and in RegexBlender
String regexRemoveKeywords(String name) {
  return name.replaceAll(/(?i)(\bvp9\b|\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, ' ').replaceAll(/(\s){2,20}/, ' ')
}

String regexBlender(String name) {
  // println "${name}"
  // step1 = name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ')
  step1 = regexStep1(name)
  // println "--- Step1: ${step1}"
  step2 = step1.replaceAll(/(\.\d)?\.[^.]+$/, '').replaceAll(/(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/, '').replaceAll(/((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/, '').replaceAll(/(?i)(\bvp9\b|\bbd remux\b|\bremux\b|\brestored\b|\bkorean\b|\b1448x1080\b|\b10-bit\b|\bh\s265\b|\bNVENC\b|\b1280x720\b|\sbd\sbox\s|\btri-audio\b|\buhd\b|\btruehd\b|\bttga\b|\bdvdrip\b|\bhr-sr\b|\bh264\b|\bh265\b|\brencode\b|\br2fr\b|\bdivx\b|\bvostfr\b|\brus\b|\bjap\b|\bdeadmauvlad\b|\bhd1080\b|\bnooped\b|\bMULTi\b|-sLaX|\b2ch\b|WEB-DL|800p|\bsubtitles\b|\btv\b|\bhr-rg\b|hd720blu|\bhd720\b|\bweb\b|\bopus\b|\bopus-m3d\b|\bDVD\b|\bAC3\b|\bAAC\b|1080p|BDRip|BluRay|720p|BD720p|x265|x264|10bit|8bit|english|60fps|HEVC|HDTV|Subbed|\bdts\b|\bita\b|\bEng\b|MultiSub|\bsub\b)/, ' ').replaceAll(/(\s?(?!\[\d\d\d\d\])\[[\w-\s\'&~.,!#$%@]*\]|\s?(?!\(\d\d\d\d\))\([\w-\s\'&,~.!#$%@]*\))/, '').replaceAll(/\[/, '(').replaceAll(/\]/, ')')
  // println "--- Step2: ${step2}"
  step3 = step2.replaceFirst(/(?i)(?<=\d)(E\d{1,3}\s*v[\d]{1,2}\b|E\d{1,3}\b)\b.*$/, '').replaceAll(/(?i)([-\s]+Episode[s]?|[-\s]+ep|[-\s]+eps[s]?|[-\s]+#|[-\s]+e)(?<!Season)(?<!Part)([-\s#]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|[-\s#]*[\d]{1,3}\b.*$)/, '').replaceAll(/(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*[\d]{1,3}\s{0,1}v[\d]{1,2}\b.*$|-[\s]*[\d]{1,3}\b.*$)/, '').replaceAll(/(?i)(?<!Season)(?<!Part)(-[\s]+(vol|sp)\b.*$)/, '')
  // println "--- Step3: ${step3}"
  // step4 = step3.replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '').replaceAll(/(\s){2,20}/, ' ').toLowerCase()
  step4 = regexStep4(step3)
  // println "--- Step4: ${step4}"
  return step4
}

def parentFilePath(f) {
  return f.getParentFile().getName()
}

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
      return 'eigth'
    case 9:
      return 'ninth'
    case 10:
      return 'tenth'
    default:
      return 'I did not want to add number verbs past tenth so there.'
  }
}

// from Stack Overflow
// https://stackoverflow.com/questions/6810336/is-there-a-way-in-java-to-convert-an-integer-to-its-ordinal-name
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