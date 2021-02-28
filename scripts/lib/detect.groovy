package lib
//--- VERSION 1.2.0

/**
 * Determine if a file is a Movie.
 *  If it is longer then 60 minutes or has Movie/Gekijouban in the filename we basically consider it a movie
 *
 * @param f The file we want to check
 * @return  True/false
 */
Boolean detectAnimeMovie(File f) {
  return f.name =~ /(?i:Movie|Gekijouban)/ || ( f.isVideo() ? getMediaInfo(f, '{minutes}').toInteger() > 60 : false )
}

/**
 * Determine if a file uses Airdate Ordering
 *
 * @param filename The file we want to check
 * @return  True/false
 */
Boolean detectAirdateOrder(String filename) {
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations
  // However it will not match if it's encased in () [] or ..
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
  // VOID - return filename =~ /(?i)\b(?<!\[)((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
  // VOID - return filename =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
  return filename =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
}

// Some Forum posts about group matching .. Maybe sometime I'll look at them
// {fn.replaceAll(/$n/,n.replaceAll(/[-]/,'.')).match(/(?:(?<=[-])(?!(cd|CD)[0-9]+)\w+$)|(?:^(?!(cd|CD)[0-9]+)[A-Za-z0-9]+(?=[-]))|(?:(?<=^[\[])[^\]]+(?=[\]]))|(?:(?<=[\[])[^\]]+(?=[\]]$))/)}

/**
 * Determine the Release Group for any given file
 *
 * @param file The file we want to check
 * @return  The detected Release Group (without [], () etc)
 */
String detectAnimeReleaseGroupFromFile(File file) {
  def myDetectedGroup = null
  def myRegexMatcher = file.name =~ /^[\[(]([^)\]]*)[\])].*$/
  // For any file starting with [SomeReleaseGroupHere] or (SomeReleaseGroupHere)
  if ( myRegexMatcher ) {
    myDetectedGroup = myRegexMatcher[0][1]
  }

  groupsWithVariableFilenamePositionsUsingBrackets = /Rady|NHK|Central Anime|Netflix-Subs|peachflavored|Persona99.GSG|Uppi|tv.dtv.mere|ManifestX|h-b|sxales|AnimDL.ir|glm8892|tempsubs|tempsub|SevensRoadFansubs|DarkDream/
  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /(?i)[\[(](/ + groupsWithVariableFilenamePositionsUsingBrackets + /)[\])].*$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  groupsThatDoNotFollowEasyRules = /MNHD-FRDS/
  // - MNHD-FRDS aka Howl_s.Moving.Castle.2004.BluRay.1080p.x265.10bit.4Audio.MNHD-FRDS.mkv
  if ( myDetectedGroup == null ) {
    myRegexMatcher = file.name =~ /(?i)\.(/ + groupsThatDoNotFollowEasyRules + /)(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  groupsThatEndFileNamesWithDash = /slax|YURASUKA|RONiN|FGT|Rapta|EMBER|Li0N|SvM/
  if ( myDetectedGroup == null ) {
//    myRegexMatcher = file.name =~ /(?i)-(slax)(\.\d)?\.[^.]+$/
    myRegexMatcher = file.name =~ /(?i)-(/ + groupsThatEndFileNamesWithDash + /)(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][1]
    }
  }

  if ( myDetectedGroup == null ) {
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
    myRegexMatcher = file.name =~ /^(.+)\s-\s(\d{1,3}|S\d{1,3})(?>v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedGroup = myRegexMatcher[0][5]
    } else {
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
      myRegexMatcher = file.name =~ /^(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
      if ( myRegexMatcher ) {
        myDetectedGroup = myRegexMatcher[0][3]
      }
    }
  }
  if ( myDetectedGroup == null ) {
    myDetectedGroup = getMediaInfo(file, '{group}')
  }
  return myDetectedGroup
}

/**
 * Determine the Episode # of a file
 *
 * @param file The file we want to check
 * @param preferFilebotMetadata Boolean: Should we prefer Filebot Metadata instead of detection? defaults to false
 * @param episodeProcessing Boolean: Are we processing what we think is an episode? defaults to true
 * @param returnSeasonToo Boolean: Are we going to return the Season (only works for Airdate)? defaults to false
 * @param returnSpecialsType Boolean: Are we going to return the SpecialType along with the #(aka OVA1 vs 1)? defaults to true
 * @return The detected episode #
 */
def detectEpisodeNumberFromFile(File file, Boolean preferFilebotMetadata = false, Boolean episodeProcessing = true, Boolean returnSeasonToo = false, Boolean returnSpecialsType = true) {
  Boolean fileBotMetadataUsed = false
  if (preferFilebotMetadata && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
      log.finest "detectEpisodeNumberFromFile() - Prefer Filebot Metadata"
    }
  }
  String myFileNameForParsing = regexStep2(file.name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')).replaceAll(/([\s-])*$/, '').replaceAll(/(\s|\.)*$/, '')
  //    println "myFileNameForParsing:[${myFileNameForParsing}]"
  def myDetectedEpisodeNumber = null
  def myDetectedSeasonNumber = null

  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})\s[^-]*$/
  //  myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(\s|\s-\s)[^-]*$/
  //  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})($|(\s|\s-\s)[^-]*$)/
  def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(?>v\d)?($|\s-\s|\s)/
  // For text like:
  // 01 The Lesson for Squirrel
  // 055 - The Stone Flower And Shippo s First Love
  // 04 - The Self-Styled Young Man And The Giant Wolf
  // 01
  // Yes this will falsely match titles that START with numbers, aka 11 Eyes. So I rely on later regex to match the episode correctly
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\s([\d]{1,3}\s?$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,3}$|[\d]{1,3})$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!\d\s)(?<!Season)(?<!\bS)(?<!Part)\sE?P?([\d]{1,3})$/
  // for text like:
  // Assault Lily - Bouquet 01
  // Wolfs Rain E01
  // Aria the Natural - Ep26
  // Keishichou Zero-gakari Season 4 EP03
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<=\d[a-z][a-z]\sSeason)\s([\d]{1,3})$/
  // for text like:
  // ReZero kara Hajimeru Break Time 2nd Season 01 and variations of ordinal season syntax 1st, 2nd, 3rd, 4th, 5th, 6th, 7th, 8th, 9th
  if (myRegexMatcher) {
//    log.finest "\t Ordinal Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
  }



  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])-[\s]*([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3}\.\d|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
  // for text like:
  // Grisaia no Kajitsu - SP1
  // Grisaia no Kajitsu - 1
  // Grisaia no Kajitsu - #1
  // Grisaia no Kajitsu - Episode 13
  // Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5
  // Will also match some edge cases of TVDB detected Episodes
  // aka Re Zero kara Hajimeru Isekai Seikatsu - 2x08
  if (myRegexMatcher) {
//    log.finest "\t #2 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    //  myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(?i)(?![a-z0-9])([^-]*$)/, '').replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(?i)(?![a-z0-9])([^-.]*$)/, '').replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
    // println "2nd:myDetectedEpisodeNumber:${myDetectedEpisodeNumber}"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}|[\d]{1,3})(\s-\s)/
  // myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-)/
  // For text like:
  // Cap Kakumei Bottleman - 07 - A Battle! Clash between Soft and Hard!
  // Zoids Wild Zero - 01- Birth! Beast Liger
  // ZENONZARDTHE ANIMATION Episode - 04 -Ash Undercover
  if ( myRegexMatcher ) {
    //      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}-[\d]{1,3})\s{0,1}(?>v\d)?(\s?-\s)/
  // for text like:
  // Taiko no Tatsujin - 01-07 - The Birth of Don and Katsu!!
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,3})(?>v\d)?$/
  // For text like:
  // To LOVE-Ru Darkness Season S2 14
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "")
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)Special\s(\d{1,3})(?>v\d)?$/
  myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,3}|SP\s?-?\s?\d{1,3}|OVA\s?-?\s?\d{1,3}|OAD\s?-?\s?\d{1,3})(?>v\d)?$/
  // for text like:
  // Hyakka Ryouran Samurai Bride Special 1
  // Hyakka Ryouran Samurai Bride Sp 1
  // OVA - 1
  // OVA 1
  if (myRegexMatcher) {
    if ( returnSpecialsType ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '')
    } else {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '').replaceAll(/[a-zA-Z]/, '')
    }

  }

  myRegexMatcher = file.name =~ /(?i)(\s-[\s]*)([\d]{1,3}.\d)\s{0,1}(?>v\d)?(\s?-\s)/
  // For text like:
  // [DeadFish] Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5 - Special [720p][AAC].mp4
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
  }

  if (!episodeProcessing) {
    // AniAdd Format #2 - Movies
    myRegexMatcher = file.name =~ /^(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1]
    }
  }

  myRegexMatcher = myFileNameForParsing =~ /(?i)\s(\d{1,3})\s*-\s[^-0-9]*$/
  // For text like:
  // Code Geass - Boukoku no Akito 1 - Yokuryuu wa Maiorita
  // the Garden of sinners 08 - Epilogue
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "")
//    log.finest "\t #13 Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  }

  // myRegexMatcher = myFileNameForParsing =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  myRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  if (myRegexMatcher) {
    myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/^0+(?!$)/, "")  //.replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
    myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/^0+(?!$)/, "")  //.replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
//    log.finest "\t TVDB Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    tvdbSeasonalityDetected = true
    if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
      if ( file.metadata ) {
        myDetectedEpisodeNumber = any { file.metadata.episode } {null}
        fileBotMetadataUsed = true
//        log.finest "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
      }
    }
  }

  // AniAdd Format #1 - Series
  //    myRegexMatcher = file.name =~ /^(.+)\s-\s(\d{1,3}|S\d{1,3})(?>v\d)?\s-\s(.+)(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\.\d)?\.[^.]+$/
  myRegexMatcher = file.name =~ /^(.+)\s-\s(\d{1,4}|S\d{1,4})(?>v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
  if ( myRegexMatcher ) {
    myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "") // Remove leading Zero's
  }
  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    if ( file.metadata ) {
      myDetectedEpisodeNumber = any { file.metadata.episode } {null}
      fileBotMetadataUsed = true
//      log.finest "\t Metadata: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
    }
  }
  if (myDetectedEpisodeNumber == null && episodeProcessing) {
    myRegexMatcher = myFileNameForParsing =~ /(\d{1,3}$)/
    // Matches up to 3 digits at the end of the line
    // TomicaKizunaGattaiEarthGranner45
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "") // Remove leading Zero's
    }
  }
  log.fine "Final: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${file.name}]"
  if ( returnSeasonToo ) {
    return [myDetectedEpisodeNumber: myDetectedEpisodeNumber, myDetectedSeasonNumber: myDetectedSeasonNumber]
  } else {
    return myDetectedEpisodeNumber as String
  }
}