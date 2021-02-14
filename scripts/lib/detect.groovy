package lib
//--- VERSION 1.1.2

Boolean detectAnimeMovie(File f) {
  return f.name =~ /(?i:Movie|Gekijouban)/ || ( f.isVideo() ? getMediaInfo(f, '{minutes}').toInteger() > 60 : false )
}

Boolean detectAirdateOrder(String filename) {
  // for text like:
  // Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
  // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
  // Kami-tachi ni Hirowareta Otoko - 01 E16, 01 x16, 01 S16 variations  (and 01 E16v2 or 01 E16_v2 variations
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
}

// Some Forum posts about group matching .. Maybe sometime I'll look at them
// {fn.replaceAll(/$n/,n.replaceAll(/[-]/,'.')).match(/(?:(?<=[-])(?!(cd|CD)[0-9]+)\w+$)|(?:^(?!(cd|CD)[0-9]+)[A-Za-z0-9]+(?=[-]))|(?:(?<=^[\[])[^\]]+(?=[\]]))|(?:(?<=[\[])[^\]]+(?=[\]]$))/)}

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