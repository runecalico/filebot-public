#!/usr/bin/env filebot -script
//--- VERSION 1.3.0
// https://mvnrepository.com/artifact/commons-io/commons-io
@Grapes(
    @Grab(group='commons-io', module='commons-io', version='2.8.0')
)

import com.cedarsoftware.util.io.JsonObject
import net.filebot.Logging
import net.filebot.web.Link
import net.filebot.web.SortOrder
import org.apache.commons.io.FileUtils
import java.lang.reflect.Array
// log input parameters
Logging.log.fine("Run script [$_args.script] at [$now]")

// Define a set list of Arguments?
_def.each { n, v -> log.finest('Parameter: ' + [n, n =~ /plex|kodi|pushover|pushbullet|discord|mail|myepisodes/ ? '*****' : v].join(' = ')) }
args.withIndex().each { f, i -> if (f.exists()) { log.finest "Argument[$i]: $f" } else { log.warning "Argument[$i]: File does not exist: $f" } }

// initialize variables
testRun = _args.action.equalsIgnoreCase('test')
scriptAction = _args.action

// --output folder must be a valid folder
//**// If you don't pass --output (so _args.output is null) it will default to Current Working Directory)
outputFolder = tryLogCatch { any { _args.output } { '.' }.toFile().getCanonicalFile() }
locale = any { _args.language.locale } { Locale.ENGLISH }

// enable/disable features as specified via --def parameters
// The defaults are basically for "episodes"
pruneLargerSizes = any { pruneLargerSizes.toBoolean() } { false }
pruneLowerResolutions = any { pruneLowerResolutions.toBoolean() } { true }
pruneLowerResolutions4K = any { pruneLowerResolutions4K.toBoolean() } { false }
pruneEightBitX265 = any { pruneEightBitX265.toBoolean() } { false } // 1080p+ only
pruneUnknownGroups = any { pruneUnknownGroups.toBoolean() } { false }
pruneByGroupList  = any { pruneByGroupList.toBoolean() } { false }
pruneByCompleteness  = any { pruneByCompleteness.toBoolean() } { false }
prunePartiallyComplete  = any { prunePartiallyComplete.toBoolean() } { false } // By Default will only prune groups from groupsToKeepList, to prune *ALL* groups enable pruneAllPartiallyComplete
pruneAllPartiallyComplete = any { pruneAllPartiallyComplete.toBoolean() } { false }
pruneX264  = any { pruneX264.toBoolean() } { false } // 1080p+ only
//compareToDir = any { new File(compareToDir) } { false }
compareToDir = any { compareToDir } { false }
// pruneNonAniADD
numberOfGroupsToKeep = any { numberOfGroupsToKeep.toInteger() } { 5 }
//--- Purposely start Groups on 2nd line, as it will make the first group index 1 (vs zero), index zero is empty..
speedSubbersGroupRAW = """
AkihitoSubs
Judas
DKB
ASW
HR
AnimeRG
YakuboEncodes
Erai-raws
SSA
zza
ShowY
Raze
224
USD
EMBER
EDGE
FFA
mal lu zen
Rias
sLaX
CuaP
CBB
YuiSubs
SubsPlease
DeadFish
BakedFish
QCE
TRASH
ThiccThighs
naiyas
Anime-Releases
Rip Time
"""
//groupsToKeepList = groupsToKeepRAW.collect { it}
//groupsToKeepList = groupsToKeepRAW as String[]
allGroupRAW = """
Abystoma
Commie
AkihitoSubs
Judas
DKB
YURASUKA
ASW
HR
AnimeRG
Mysteria
Erai-raws
Doki
SSA
zza
ShowY
Raze
224
USD
EMBER
JacobSwaggedUp
EDGE
FFA
mal lu zen
Rias
sLaX
CuaP
CBB
HorribleRips
YuiSubs
YakuboEncodes
SubsPlease
DeadFish
BakedFish
QCE
TRASH
ThiccThighs
naiyas
Anime-Releases
Rip Time
"""
groupsToKeepList = []
speedSubbersGroupRAW.eachLine {line ->
  groupsToKeepList << line.toLowerCase().trim()
}
otherSubbersRAW = """
Mysteria
Aeenald
GJM
DmonHiro
kokus-rips
Kawaiika-Raws
Ryuuga
NewbSubs
HorribleRips
YakuboEncodes
Anime Time
Polarwindz
MoscowGolem
"""
moviesRAW = """
bonkai77
MNHD-FRDS
"""
//println "groupsToKeepList:${groupsToKeepList}"
//groupsToKeepList.eachWithIndex { line, index ->
//  println "groupsToKeepList:[${index}]:[${line}]"
//}
numberOfCompleteSeasonGroupsToKeep = any { numberOfCompleteSeasonGroupsToKeep.toInteger() } { 1 }

// enable/disable features as specified by arguments
episode = 'EPISODE'.equalsIgnoreCase _args.mode
movie = 'MOVIE'.equalsIgnoreCase _args.mode

// Include My Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // AniDB Stuff
include('lib/tvdb')  // TVDB Stuff
include('lib/manami')  // Anime offline Database Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/sorter') // Renamer Sorter methods

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}

if (!episode && !movie ) {
  die "Invalid usage: mode must be set, either movie or episode"
}

if ( !episode ) {
  pruneLargerSizes = false
  pruneLowerResolutions = true
//  pruneEightBitX265 = true
  pruneUnwantedGroups = false
//  pruneByGroupList = true
//  pruneX264 = true
  pruneLowerResolutions4K = true
  pruneByCompleteness = false
  prunePartiallyComplete = false
  pruneAllPartiallyComplete = false
}

// Include My Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // AniDB Stuff
include('lib/tvdb')  // TVDB Stuff
include('lib/manami')  // Anime offline Database Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/sorter') // Renamer Sorter methods


// ---------- Download/Cache Anime Offline database ---------- //
// This is used to validate AniDB AID's matched are for Movies (not TV)
// https://github.com/manami-project/anime-offline-database
// Json - https://github.com/manami-project/anime-offline-database/raw/master/anime-offline-database.json
// com.cedarsoftware.util.io.JsonObject
JsonObject animeOfflineDatabase = Cache.getCache('animne-offline-database-json', CacheType.Persistent).json('anime-offline-database.json') {
  new URL('https://raw.githubusercontent.com/manami-project/anime-offline-database/master/' + it) }.expire(Cache.ONE_DAY).get()
//println "animeOfflineDatabase.getClass:[${animeOfflineDatabase.getClass()}]"

LinkedHashMap inputAidFolders = [:]
ArrayList inputFolders
ArrayList<File> baseDir = compareToDir ? Arrays.asList(compareToDir.split(",")).collect {new File(it)} : null
log.finest "Source:${args}"
if ( compareToDir ) {
  log.finest "Comparing to:${baseDir}"
  inputFolders = args.getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter
  def folderGroupsA = inputFolders.groupBy {
    def anidbId = it.name =~ /^(.+)\[anidb\-(\d+)\]/
    anidbId[0][2]
  }
  println "folderGroupsA: ${folderGroupsA.size()}"
  def foldersB = baseDir.getFolders() { it =~ /(\[anidb\-\d+\])/ } // Z:\video\TV\Anime (Subbed)
  def folderGroupsB = foldersB.groupBy {
    def anidbId = it.name =~ /^(.+)\[anidb\-(\d+)\]/
    anidbId[0][2]
  }
  println "folderGroupsB: ${folderGroupsB.size()}"
  folderGroupsA.each { zAid, zDir ->
    def isNewAID = folderGroupsB.get(zAid)
    if (isNewAID != null) {
      zDir += isNewAID
      inputAidFolders += [(zAid): zDir]
      return
    }
  }
} else {
  if ( episode ) {
//  inputFolders = args[0].getFolders() { it =~ /(\[anidb\-\d+\]|\[tvdb\-\d+\])/ } // W:\1-InitialSort\2020\winter
    inputFolders = args.getFolders() { it =~ /(\[anidb\-\d+\]|\[tvdb\-\d+\])/ } // W:\1-InitialSort\2020\winter
  } else {
//  inputFolders = args[0].getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter
    inputFolders = args.getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter
  }
  inputAidFolders = inputFolders.groupBy {
//    def anidbId = it.name =~ /^(.+)\[anidb\-(\d+)\]/
    def anidbId = it.name =~ /^(.+)(\[anidb\-|\[tvdb\-)(\d+)\]/
    anidbId[0][3]
  }
}
println "inputAidFolders: ${inputAidFolders.size()}"
inputAidFolders.each { zAID, zDIR ->
//  println "zAID:${zAID}"
  if ( zAID == null ) {
    die "zAID is null!"
  }
//  println "zDIR:${zDIR}"
}

/*String aodGetTypeForAID(JsonObject fileBotJsonCacheObject, Integer AnimeID) {
  JsonObject myAniDBEntry = fileBotJsonCacheObject.data.find { aodentry ->
    aodentry.sources.find { it ==~ /https:\/\/anidb\.net\/anime\/${AnimeID}$/ }
  }
  // --- It will be null if an AID is not in the AOD List --- //
  if ( myAniDBEntry == null ) {
    return true
  } else {
    // Types are "Special", "Movie", "OVA", "ONA", "TV" - It is unknown if it matches AniDB 100% however.
    return myAniDBEntry.type
  }
}*/



LinkedHashMap groupEpisodeGeneration(ArrayList files, Boolean episodeProcessing, String animeType, Integer animeEpisodeCount, Boolean preferFilebotMetadata = false, Boolean ignoreSpecials = false) {
//  println "files.getClass():[${files.getClass()}]" // java.util.ArrayList
//  def slurper = new JsonSlurper()
  Boolean fileBotMetadataUsed = false
  LinkedHashMap groupedFiles = files.groupBy { f ->
    myDetectedEpisodeNumber = detectEpisodeNumberFromFile(f, false, episodeProcessing, false, true)
/*    fileBotMetadataUsed = false
    if (preferFilebotMetadata && episodeProcessing) {
      if ( f.metadata ) {
        myDetectedEpisodeNumber = any { f.metadata.episode } {null}
        fileBotMetadataUsed = true
        log.finest "Prefer Filebot Metadata"
      }
    }*/
//    net.filebot.web.Episode filebotMetadata
//    def metadataEpisodeNumber // java.lang.integer when it's got a real value
//    if ( f.metadata ) {
//      filebotMetadata = f.metadata
//      metadataEpisodeNumber = filebotMetadata.episode
////      println "filebotMetadata.getClass():[${filebotMetadata.getClass()}]"
////      println "fbe.getClass():[${fbe.getClass()}]"
//    } else {
//      metadataEpisodeNumber = null
//    }
//    def filebotEpisodeNumber = parseEpisodeNumber(f.name) // This only works on TVDB Named files :(
//    def filebotSeasonNumber  // java.lang.integer when it's got a real value
////    println "filebotEpisodeNumber.getClass():[${filebotEpisodeNumber.getClass()}]" // net.filebot.similarity.SeasonEpisodeMatcher$SxE or org.codehaus.groovy.runtime.NullObject
//    if (filebotEpisodeNumber != null) {
//      myRegexMatcher = filebotEpisodeNumber =~ /(?i)(\d+)x(\d+)/
//      if ( myRegexMatcher ) {
//        filebotEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
//        filebotSeasonNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "")
//      }
//    }
//    def guessitJsonResult = slurper.parseText(executeGuessit(f))
//    f.name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')
//    myFileNameForParsing = regexStep2(regexStep1(f.name)).replaceAll(/([\s-])*$/, '')
    // Add - .replaceAll(/(\s|\.)*$/, '') for "dot" files, which can lead to spurious dots ..
    // aka myFileNameForParsing:[Higurashi no Naku Koro ni E01.     . .] RAW:[Higurashi.no.Naku.Koro.ni.E01.1080p.WEB-DL.x265.10Bit.2CH.HEVC-sLaX.mkv]
/*    myFileNameForParsing = regexStep2(f.name.replaceAll(/_/, ' ').replaceAll(/\{/, '[').replaceAll(/\}/, ']').replaceAll(/(?<!\s\d\d\d)(?<!\s\d\d)(?<!\s\d)\.(?!(\d\.[^.]+$|[^.]+$))(?!\d)/, ' ')).replaceAll(/([\s-])*$/, '').replaceAll(/(\s|\.)*$/, '')
    //    println "myFileNameForParsing:[${myFileNameForParsing}]"

    def myDetectedEpisodeNumber = null
    //    myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})\s[^-]*$/
    //    myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(\s|\s-\s)[^-]*$/
//    def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})($|(\s|\s-\s)[^-]*$)/
    def myRegexMatcher = myFileNameForParsing =~ /(?i)(^\d{1,3})(?>v\d)?($|\s-\s|\s)/
    // For text like: 01 The Lesson for Squirrel or 055 - The Stone Flower And Shippo s First Love or 04 - The Self-Styled Young Man And The Giant Wolf or 01
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

//    myRegexMatcher = myFileNameForParsing =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
    myRegexMatcher = myFileNameForParsing =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)/
    // for text like: Kami-tachi ni Hirowareta Otoko - S01E02 also S01x02, S01S16 variations (and S01E02v2 or S01E02_v2 variations)
    // Kami-tachi ni Hirowareta Otoko - 01E16, 01x16, 01S16 variations  (and 01E16v2 or 01E16_v2 variations)
    // Kami-tachi ni Hirowareta Otoko - 01.E16, 01.x16, 01.S16 variations  (and 01.E16v2 or 01.E16_v2 variations
    if (myRegexMatcher) {
      myDetectedEpisodeNumber = myRegexMatcher[0][3].replaceFirst(/(?i)(E|x)/, "").replaceFirst(/^0+(?!$)/, "")  //.replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
      myDetectedSeasonNumber = myRegexMatcher[0][2].replaceFirst(/(?i)(s)/, "").replaceFirst(/^0+(?!$)/, "")  //.replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
      if ( myDetectedSeasonNumber.toString().toInteger() > 1 ) {
        if ( f.metadata ) {
          myDetectedEpisodeNumber = any { f.metadata.episode } {null}
          fileBotMetadataUsed = true
          log.finest "\t Season[${myDetectedSeasonNumber}]: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
        }
      }
    }

    //    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])-[\s]*([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
    //    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
    // myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}\b[^-]*$|[\d]{1,3}\b[^-]*$)/
//    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
    myRegexMatcher = myFileNameForParsing =~ /(?i)(?<!Season)(?<!Part)(?<![a-z0-9])(-[\s]*|-[\s]*Episode[\s]*|-[\s]*#[\s]*)(SP\d{1,3}|\d{1,3}\.\d|\d{1,3})[_\s]?(?>v\d)?[^-]*$/
    // for text like: Grisaia no Kajitsu - SP1
    // Grisaia no Kajitsu - 1
    // Grisaia no Kajitsu - #1
    // Grisaia no Kajitsu - Episode 13
    // Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5
    if (myRegexMatcher) {
//      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(?i)(?![a-z0-9])([^-]*$)/, '').replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(?i)(?![a-z0-9])([^-.]*$)/, '').replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
//      println "2nd:myDetectedEpisodeNumber:${myDetectedEpisodeNumber}"
    }

    //    myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}\s{0,1}v[\d]{1,2}|[\d]{1,3})(\s-\s)/
//    myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-\s)/
    myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,4})\s{0,1}(?>v\d)?(\s?-)/
    // For text like: Cap Kakumei Bottleman - 07 - A Battle! Clash between Soft and Hard!
    // Zoids Wild Zero - 01- Birth! Beast Liger
    // ZENONZARDTHE ANIMATION Episode - 04 -Ash Undercover
    if ( myRegexMatcher ) {
    //      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "").replaceFirst(/(v[\d]{1,2}\b[^-]*$)/, '')
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
    }

    myRegexMatcher = myFileNameForParsing =~ /(?i)(\s-[\s]*)([\d]{1,3}-[\d]{1,3})\s{0,1}(?>v\d)?(\s?-\s)/
    // for text like: Taiko no Tatsujin - 01-07 - The Birth of Don and Katsu!!
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
    }

    myRegexMatcher = myFileNameForParsing =~ /(?i)S\d\s(\d{0,3})(?>v\d)?$/
    // For text like: To LOVE-Ru Darkness Season S2 14
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceFirst(/^0+(?!$)/, "")
    }

//    myRegexMatcher = myFileNameForParsing =~ /(?i)Special\s(\d{1,3})(?>v\d)?$/
    myRegexMatcher = myFileNameForParsing =~ /(?i)(Special\s?-?\s?\d{1,3}|SP\s?-?\s?\d{1,3}|OVA\s?-?\s?\d{1,3}|OAD\s?-?\s?\d{1,3})(?>v\d)?$/
    // for text like: Hyakka Ryouran Samurai Bride Special 1 or Hyakka Ryouran Samurai Bride Sp 1 or OVA - 1 or OVA 1
    if (myRegexMatcher) {
      myDetectedEpisodeNumber = myRegexMatcher[0][1].replaceAll(/\s/, '')
    }

    myRegexMatcher = f.name =~ /(?i)(\s-[\s]*)([\d]{1,3}.\d)\s{0,1}(?>v\d)?(\s?-\s)/
    // For text like: [DeadFish] Kono Yo no Hate de Koi wo Utau Shoujo YU-NO - 26.5 - Special [720p][AAC].mp4
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "")
    }
    if (!episodeProcessing) {
      // AniAdd Format #2 - Movies
      myRegexMatcher = f.name =~ /^(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
      if ( myRegexMatcher ) {
        myDetectedEpisodeNumber = myRegexMatcher[0][1]
      }
    }
    // AniAdd Format #1 - Series
    //    myRegexMatcher = f.name =~ /^(.+)\s-\s(\d{1,3}|S\d{1,3})(?>v\d)?\s-\s(.+)(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\[[\w-\s\'&~.!#$%@]*\])(\.\d)?\.[^.]+$/
    myRegexMatcher = f.name =~ /^(.+)\s-\s(\d{1,4}|S\d{1,4})(?>v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
    if ( myRegexMatcher ) {
      myDetectedEpisodeNumber = myRegexMatcher[0][2].replaceFirst(/^0+(?!$)/, "") // Remove leading Zero's
    }
    if (myDetectedEpisodeNumber == null && episodeProcessing) {
      if ( f.metadata ) {
        myDetectedEpisodeNumber = any { f.metadata.episode } {null}
        fileBotMetadataUsed = true
        log.finest "\t 1: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
      }
    }
    switch (animeType) {
      case ["TV"]:
        if (myDetectedEpisodeNumber == null) {
          if ( ignoreSpecials ) {
            if ( !f.name =~ /(?i:\bOVA\b|\bONA\b|\bOAD\b|\bNCOP\b)/ ) {
              log.finest "groupEpisodeGeneration - myFileNameForParsing:[${myFileNameForParsing}]"
              log.finest "myFilename:[${f}]"
            }
          }
        }
        if ( ignoreSpecials && !myDetectedEpisodeNumber.toString().isInteger() ) {
          log.finest "Setting Episode to Null due to Non-Integer myDetectedEpisodeNumber:[${myDetectedEpisodeNumber}]"
          myDetectedEpisodeNumber = null
        }
        break
      case ["Special","OVA","ONA"]:
        if (myDetectedEpisodeNumber == null || myDetectedEpisodeNumber == []) {
          log.finest "groupEpisodeGeneration - myFileNameForParsing:[${myFileNameForParsing}]"
          log.finest "myFilename:[${f}]"
        }
        break
      case ["Movie"]:
        if (myDetectedEpisodeNumber == null || myDetectedEpisodeNumber == []) {
          myDetectedEpisodeNumber = "Movie"
        }
        break
      default:
        log.fine "Unknown AnimeType[${animeType}] from AOD"
        break
    }
    if ( myDetectedEpisodeNumber.toString().isInteger() && myDetectedEpisodeNumber.toString().toInteger() > animeEpisodeCount ) {
      log.finest "\t PreMeta: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}] with Episode Count of [${animeEpisodeCount}]"
      if ( !fileBotMetadataUsed ) {
        if ( f.metadata ) {
          myDetectedEpisodeNumber = any { f.metadata.episode } {null}
          fileBotMetadataUsed = true
          log.finest "\t 2: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
        } else {
//          log.finest "3: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
        }
      } else {
//        log.finest "4: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
      }
    }
//    if (myDetectedEpisodeNumber == null || myDetectedEpisodeNumber == []) {
//      if (episodeProcessing) {
//        switch (animeType) {
//          case ["TV"]:sorting_ideas.groovy
//            if ( !f.name =~ /(?i:\bOVA\b|\bONA\b|\bOAD\b|\bNCOP\b)/ ) {
//              log.finest "myFileNameForParsing:[${myFileNameForParsing}]"
//              log.finest "myFilename:[${f}]"
//            }
//            break
//          case ["Special","OVA","ONA"]:
//            log.finest "myFileNameForParsing:[${myFileNameForParsing}]"
//            log.finest "myFilename:[${f}]"
//            break
//          default:
//            log.fine "Unknown AnimeType[${animeType}] from AOD"
//            break
//        }
//      } else {
//        myDetectedEpisodeNumber = "Movie"
//      }
//    } else {
//      if ( ignoreSpecials && !myDetectedEpisodeNumber.toString().isInteger() ) {
////        println "Setting Episode to Null due to Non-Integer myDetectedEpisodeNumber:[${myDetectedEpisodeNumber}]"
//        myDetectedEpisodeNumber = null
//      }
//    }
//    if ( ignoreSpecials && f.name =~ /(?i:\bOVA\b|\bONA\b|\bOAD\b|\bNCOP\b)/) {
////      println "Setting Episode to null due to filename containing OVA|ONA|OAD|NCOP"
//      myDetectedEpisodeNumber = null
//    }*/
//    log.finest "Final: Detected episode[${myDetectedEpisodeNumber}]:(Filebot?${fileBotMetadataUsed}) for myFileNameForParsing:[${myFileNameForParsing}] RAW:[${f.name}]"
    return [myDetectedEpisodeNumber: myDetectedEpisodeNumber]
  }
  return groupedFiles
}

Integer lookupVFQualityMetric(String videoFrameResolution ) {
  switch (videoFrameResolution) {
    case '2160p':
      return 10
      break
    case '1080p':
      return 20
      break
    case '800p':
      return 40
      break
    case '720p':
      return 40
      break
    case '576p':
      return 60
      break
    case '480p':
      return 60
      break
    case '360p':
      return 70
      break
    case '240p':
      return 80
      break
    default:
      log.finest "videoFrameResolution not found:[$videoFrameResolution]"
      return 0
      break
  }
}

// MPEG Codec
Integer lookupVCQualityMetric(String videoCodec ) {
  switch (videoCodec) {
    case ["x265","HEVC","HEVCM"]:
      return 10
      break
    case ["AVC","x264"]:
      return 20
      break
    default:
      log.finest "videoCodec not found:[$videoCodec]"
      return 0
      break
  }
}

Integer lookupBDQualityMetric(String bitDepth ) {
  switch (bitDepth) {
    // 12 bit is not that common, so for the moment treat it like 10bit
    case ["10bit","10","12bit","12"]:
      return 10
      break
    case ["8bit","8"]:
      return 20
      break
    default:
      log.finest "bitDepth not found:[$bitDepth]"
      return 0
      break
  }
}

Integer lookupSZQualityMetric(String fileSizeReadable, String videoCodec) {
  switch (videoCodec) {
    case ["x265","HEVC"]:
      if (fileSizeReadable.contains('GB') || fileSizeReadable =~ /^[9]/) {
        return 25
      }
      if (fileSizeReadable =~ /^[7-8]/ && fileSizeReadable.contains('MB')) {
        return 15
      }
      if (fileSizeReadable =~ /^[1-6]/ && fileSizeReadable.contains('MB')) {
        return 5
      }
      break
    case ["AVC","x264"]:
      if (fileSizeReadable.contains('GB')) {
        return 40
      }
      if (fileSizeReadable =~ /^[8-9]/ && fileSizeReadable.contains('MB')) {
        return 30
      }
      if (fileSizeReadable =~ /^[1-7]/ && fileSizeReadable.contains('MB')) {
        return 20
      }
      break
    default:
      return 0
      break
  }
}

//    def vc = getMediaInfo(f, '{vc.replace("AVC","x264").replace("x265","HEVC")}')
//    def vr = getMediaInfo(f, '{resolution}')
//    def bd = getMediaInfo(f, '{bitdepth}')
//    String fileSizeReadable = FileUtils.byteCountToDisplaySize(f.length());
//    println "fileSizeReadable:[${fileSizeReadable}]"
//def videoDuration = getMediaInfo(f, '{minutes}')
//    if ( videoDuration == null ) {
//      videoDuration = 1
//    }
// This sounds good, but easily breaks up Group Release packs for an entire series.. Will need to revisit.
//    if (vf == '1080p' && fileSizeReadable.contains('GB') && videoDuration.toInteger() < 30) {
//      myQualityMetric = 5
//    }
LinkedHashMap groupQualityVFGeneration(ArrayList files) {
  LinkedHashMap groupedFiles = files.groupBy { f ->
    def vf = getMediaInfo(f, '{vf}')
//    println "VideoFrameSize: ${vf}"
    Integer myQualityMetric = 0
    if (vf == '2160p') {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '1080p') {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '800p') {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '720p') {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '576p' ) {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '480p' ) {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '360p' ) {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if (vf == '240p' ) {
      myQualityMetric = lookupVFQualityMetric(vf)
    }
    if ( myQualityMetric == 0 ) {
      log.finest "Quality not found: VF:[$vf]"
    }
    return myQualityMetric
  }
  return groupedFiles
}

LinkedHashMap groupQualityBDGeneration(ArrayList files) {
  LinkedHashMap groupedFiles = files.groupBy { f ->
    def bd = getMediaInfo(f, '{bitdepth}')
    Integer myQualityMetric = lookupBDQualityMetric(bd)
    return myQualityMetric
  }
  return groupedFiles
}

LinkedHashMap groupQualityVCGeneration(ArrayList files) {
//  println "files:${files}"
  LinkedHashMap groupedFiles = files.groupBy { f ->
//    println "f:${f}"
    def vc = getMediaInfo(f, '{vc.replace("AVC","x264").replace("x265","HEVC")}')
    Integer myQualityMetric = lookupVCQualityMetric(vc)
    return myQualityMetric
  }
  return groupedFiles
}

LinkedHashMap groupQualitySZGeneration(ArrayList files) {
  LinkedHashMap groupedFiles = files.groupBy { f ->
    Integer myQualityMetric = 0
    def vf = getMediaInfo(f, '{vf}')
    def vc = getMediaInfo(f, '{vc.replace("AVC","x264").replace("x265","HEVC")}')
    if (lookupVFQualityMetric(vf) == lookupVFQualityMetric("1080p")) {
      String fileSizeReadable = FileUtils.byteCountToDisplaySize(f.length());
//      println "fileSizeReadable:[${fileSizeReadable}]"
      myQualityMetric = lookupSZQualityMetric(fileSizeReadable, vc)
    }
    return myQualityMetric
  }
  return groupedFiles
}

LinkedHashMap groupQualityReleaseGroups(ArrayList files, ArrayList groupsToKeepList, Boolean pruneUnknownGroups, Boolean keepAllGroups = false) {
  LinkedHashMap groupedFiles = files.groupBy { f ->
    def myReleaseGroup = detectAnimeReleaseGroupFromFile(f)
//    println "Detected Release Group:[${myReleaseGroup}] for file:[${f.name}]"
    if ( myReleaseGroup == null  ) {
      println "Could not detect Release Group:[${f.name}]"
      if (pruneUnknownGroups) {
        return null
      } else {
        return "SkipThisGroup"
      }
    } else {
      myReleaseGroup = myReleaseGroup.toLowerCase()
      if ( keepAllGroups ) {
        return myReleaseGroup
      }
      if ( groupsToKeepList.contains(myReleaseGroup)) {
       return myReleaseGroup
      } else {
        if (pruneUnknownGroups) {
         return null
        } else {
         return "SkipThisGroup"
        }
      }
    }
  }
  return groupedFiles
}

//--- Process each Folder
inputAidFolders.each { String zaid, ArrayList zfoldersToPrune ->
  println "zaid:${zaid}"
  println "zfoldersToPrune:${zfoldersToPrune}"
  File folderToPrune = zfoldersToPrune[0]
//  println "folderToPrune:${folderToPrune}"
//  if ( compareToDir ) {
//    zfoldersToPrune.each { zfolder ->
//      println "zfolder: ${zfolder}"
//    }
//  }
//  ArrayList filesToPrune = zfoldersToPrune.getFiles{ f -> f.isVideo() }
//  println "filesToPrune: ${filesToPrune}"
//  die "end test"
  def pruneLargerSizesTEMP = pruneLargerSizes
  def pruneLowerResolutionsTEMP = pruneLowerResolutions
  def pruneEightBitX265TEMP = pruneEightBitX265
  def pruneUnknownGroupsTEMP = pruneUnknownGroups
  def pruneByGroupListTEMP = pruneByGroupList
  def pruneX264TEMP = pruneX264
  def episodeTEMP = episode
  def pruneLowerResolutions4KTEMP = pruneLowerResolutions4K
  def pruneByCompletenessTEMP = pruneByCompleteness
  def prunePartiallyCompleteTEMP = prunePartiallyComplete
  def pruneAllPartiallyCompleteTEMP = pruneAllPartiallyComplete
  log.fine "folderToPrune: ${folderToPrune}"
  def myRegexMatcher = folderToPrune.name =~ /^(.+)\[(.*)\-(\d+)\]/
  Integer AnimeID = myRegexMatcher[0][3].toInteger()
  String AnimeDB = myRegexMatcher[0][2]
  if ( AnimeDB == "tvdb" ) {
    log.finest "TVDB FOLDER! Using Filebot to lookup AID from TVDBID:[${AnimeID}]"
    def tempAnimeID = filebotAnimeListReturnFromTVDBID(AnimeID)
    if ( tempAnimeID == null ) {
      println ''
      println '//----------------'
      println '//-------------------------------------------'
      println "Stop Processing due to Not Anime Directory"
      println '//-------------------------------------------'
      println '//----------------'
      return
    } else {
      if ( tempAnimeID.size() > 1 ) {
        println ''
        println '//----------------'
        println '//-------------------------------------------'
        println "Stop Processing due to TVDB Map to multiple AniDB IDs"
        println '//-------------------------------------------'
        println '//----------------'
        return
      } else {
        AnimeID = tempAnimeID.anidbid[0]
      }
    }
  }
  ArrayList filesToDelete = []
  ArrayList filestoMove = []
  ArrayList remainingFiles = []
  ArrayList skippedFiles = []
  ArrayList filesNotPruned = []
  ArrayList fileToCompare = []
//  ArrayList filesToPrune = folderToPrune.getFiles{ f -> f.isVideo() }
  ArrayList filesToPrune = zfoldersToPrune.getFiles{ f -> f.isVideo() }

  //--- Set options on if this is a Movie/ONA/OVA or Special/TV
  def getAnimeType = aodGetTypeForAID(animeOfflineDatabase, AnimeID)
  switch (getAnimeType) {
    case ["Movie","OVA"]:
      if ( episodeTEMP ) {
        log.fine "...MOVIE/OVA/ONA Folder, Setting Movie Options (Because we are in Episode Mode)"
        pruneLargerSizesTEMP = false
        pruneLowerResolutionsTEMP = true
        pruneEightBitX265TEMP = true
        pruneUnwantedGroupsTEMP = false
        pruneByGroupListTEMP = false
        pruneX264TEMP = true
        episodeTEMP = false
        pruneLowerResolutions4KTEMP = true
        pruneByCompletenessTEMP = false
        prunePartiallyCompleteTEMP = false
        pruneAllPartiallyCompleteTEMP = false
      }
      break
    case ["TV","Special","ONA"]:
      if ( !episodeTEMP ) {
        log.fine "...TV/Special Folder, Setting Episode Options (Because we are in Movie Mode)"
        pruneLargerSizesTEMP = false
        pruneLowerResolutionsTEMP = true
        pruneEightBitX265TEMP = true
        pruneUnwantedGroupsTEMP = false
        pruneByGroupListTEMP = false
        pruneX264TEMP = true
        episodeTEMP = true
        pruneLowerResolutions4KTEMP = false
        pruneByCompletenessTEMP = true
        prunePartiallyCompleteTEMP = true
        pruneAllPartiallyCompleteTEMP = false
      }
      break
    default:
      log.fine "Unknown AnimeType[${getAnimeType}] from AOD"
      break
  }
  Integer seriesEpisodeList = aodGetEpisodeNumberForAID(animeOfflineDatabase, AnimeID)
  if (seriesEpisodeList == 0 ) {
    log.finest "...Could not get Episode List from AOD, query Filebot DB provider"
    // TODO:
    //  Need to know if this is a AniDB or TVDB Series, as FB will error out trying to lookup TVDB to Anidb.
    def db = getService("AniDB")
    def el = db.getEpisodeList(AnimeID, "Absolute" as SortOrder, locale)
    def eplist = el.findAll { it.regular }
    seriesEpisodeList = eplist.size()
  }
  //--- Group Remaining files by Release Group to check for "completeness"
  remainingFiles = filesToPrune
  if ( pruneByCompletenessTEMP && episodeTEMP ) {
    LinkedHashMap groupByReleaseGroupComplete = [:]
    LinkedHashMap groupByReleaseGroupPartial = [:]
    log.fine "...Checking for Completed Groups for AID#[$AnimeID] with ${seriesEpisodeList} Episodes"
    LinkedHashMap groupByReleaseGroup = groupQualityReleaseGroups(remainingFiles, groupsToKeepList, pruneUnknownGroupsTEMP, true)
//    println "${groupByReleaseGroup}"
//    println "${getAnimeType}"
    if (groupByReleaseGroup.size() > 1) {
      groupByReleaseGroup.each { releaseGroup, groupFiles ->
        log.finest "\t groupByReleaseGroup:[${releaseGroup}]"
//        println "\t Files:${groupFiles.size()}"
        groupByEpisodeNumber = groupEpisodeGeneration(groupFiles, episodeTEMP, getAnimeType, seriesEpisodeList, false, true)
//      groupByEpisodeNumber.each {gbekey, gbevalue ->
//        println "gbekey:[${gbekey}]"
//        println "gbevalue:[${gbevalue}]"
//      }
        if (groupByEpisodeNumber.containsKey([myDetectedEpisodeNumber:null])) {
//        println "Got Null"
          groupByEpisodeNumber.remove([myDetectedEpisodeNumber:null])
        }
        println "\t Episode Count:${groupByEpisodeNumber.size()}"
        if (groupByEpisodeNumber.size() >= seriesEpisodeList) {
//          println "This Group has Complete Episode Coverage"
          ArrayList groupFilesTemp = []
          groupByEpisodeNumber.each { key, value ->
            groupFilesTemp += value.collect{ it }
          }
          groupByReleaseGroupComplete += [(releaseGroup):groupFilesTemp]
//          println "groupByReleaseGroupComplete:${groupByReleaseGroupComplete}"
        } else {
//          println "This Group has Partial Episode Coverage"
          ArrayList groupFilesTemp = []
          groupByEpisodeNumber.each { key, value ->
            groupFilesTemp += value.collect{ it }
          }
          groupByReleaseGroupPartial += [(releaseGroup):groupFilesTemp]
//          println "groupByReleaseGroupPartial:${groupByReleaseGroupPartial}"
        }
      }
      if ( groupByReleaseGroupComplete.size() >= 1  ) {
        if ( groupByReleaseGroupComplete.size() > numberOfCompleteSeasonGroupsToKeep ) {
          log.fine "......We have groups[${groupByReleaseGroupComplete.size()}] with Complete Coverage over the limit:[${numberOfCompleteSeasonGroupsToKeep}]"
//          Integer numberOfGroupsInDataset = groupByReleaseGroupComplete.size()
          Integer numberOfGroupsKept = 0
          Integer numberOfGroupsToKeepInList = groupsToKeepList.size()-1
          Integer onGroupNumber = 0
          while (( onGroupNumber <= numberOfGroupsToKeepInList) ) {
            onGroupNumber+=1
//              println "Looking for Release Group:[${groupsToKeepList[onGroupNumber]}]"
            if (groupByReleaseGroupComplete.containsKey(groupsToKeepList[onGroupNumber])) {
              if ( numberOfGroupsKept < numberOfCompleteSeasonGroupsToKeep ) {
                numberOfGroupsKept+=1
                log.finest "......Keeping Release Group #${numberOfGroupsKept-1}:[${groupsToKeepList[onGroupNumber]}]"
              } else {
                log.fine "......Found Release Group we can prune:[${groupsToKeepList[onGroupNumber]}]"
                groupByReleaseGroupComplete[groupsToKeepList[onGroupNumber]].each { dFile ->
                  log.fine ".........Delete: ${dFile}"
                }
                filesToDelete += groupByReleaseGroupComplete[groupsToKeepList[onGroupNumber]]
                groupByReleaseGroupComplete.remove(groupsToKeepList[onGroupNumber])
              }
            }
          }
        }
        if ( groupByReleaseGroupPartial.size() >= 1 && prunePartiallyCompleteTEMP) {
          log.fine "......We have groups with Partial Coverage due to Complete Coverage Groups"
          if ( pruneAllPartiallyCompleteTEMP ) {
            groupByReleaseGroupPartial.each { gbrgpkey, gbrgpvalue ->
              log.fine "......Found Release Group we can prune:[${gbrgpkey}]"
              groupByReleaseGroupPartial[gbrgpkey].each { dFile ->
                log.fine ".........Delete: ${dFile}"
              }
              filesToDelete += gbrgpvalue
            }
          } else {
            Integer numberOfGroupsToKeepInList = groupsToKeepList.size()-1
            Integer onGroupNumber = 0
            while (( onGroupNumber <= numberOfGroupsToKeepInList) ) {
              onGroupNumber+=1
//              println "Looking for Release Group:[${groupsToKeepList[onGroupNumber]}]"
              if (groupByReleaseGroupPartial.containsKey(groupsToKeepList[onGroupNumber])) {
                log.fine "......Found Release Group we can prune:[${groupsToKeepList[onGroupNumber]}]"
                groupByReleaseGroupPartial[groupsToKeepList[onGroupNumber]].each { dFile ->
                  log.fine ".........Delete: ${dFile}"
                }
                filesToDelete += groupByReleaseGroupPartial[groupsToKeepList[onGroupNumber]]
                groupByReleaseGroupPartial.remove(groupsToKeepList[onGroupNumber])
              }
            }
            if ( groupByReleaseGroupPartial.size() >= 1) {
              log.fine "......We did not prune these groups"
              groupByReleaseGroupPartial.each { akey, avalue ->
                log.fine "\t ${akey}"
              }
            }
          }
        }
      } else {
        log.fine "...No Release Groups with Completeness"
      }
    } else {
      log.fine "...Skipping Check as there is only one Release Group"
    }
  }

  //--- Group by Episode #
  remainingFiles = filesToPrune.minus(filesToDelete)
  LinkedHashMap groupByEpisodeNumber = groupEpisodeGeneration(remainingFiles, episodeTEMP, getAnimeType, seriesEpisodeList, false)
  groupByEpisodeNumber.each { LinkedHashMap group, ArrayList files ->
    compareQuality = true
    fileToCompare = []
    if ( compareToDir && files.size() > 1 ) {
      println "Verify if there are files to Compare To"
      files.each { File eFile ->
        baseDir.each {baseCompareToDir ->
          if ( eFile.canonicalPath.startsWith(baseCompareToDir.canonicalPath)) {
            fileToCompare += eFile
          }
        }
//        if ( eFile.canonicalPath.startsWith(compareToDir.canonicalPath)) {
//          fileToCompare += eFile
//        }
      }
      if ( fileToCompare.size() >= 1 ) {
        println "...YES[${fileToCompare.size()}]"
        compareQuality = true
      } else {
        println "   NO[${fileToCompare.size()}]"
        compareQuality = false
      }
    }
    if (files.size() > 1 && group.myDetectedEpisodeNumber != null && compareQuality) {
      remainingFiles = files
      Integer remainingFilesCount = remainingFiles.size()
      log.fine '//-----------------------\\\\'
      log.fine "Processing Group: ${group}:[${remainingFiles.size()}]"


      //--- Prune Larger Sized Episodes (but always keep at least one file per episode, which could be that large file)
      if (pruneLargerSizesTEMP && remainingFiles.size() > 1) {
        log.fine "...Checking for Large file sizes (1080p only)"
        LinkedHashMap groupByQualityVF = groupQualityVFGeneration(remainingFiles)
        if ( groupByQualityVF.containsKey(lookupVFQualityMetric("1080p")) ) {
          LinkedHashMap groupByQualitySZ = groupQualitySZGeneration(remainingFiles) // I really should just parse the 1080p files, but that would make creating the remainingFiles at the end more complex.
//            groupByQualitySZ.each {key, value ->
//              println "key: ${key}"
//              println "value: ${value}"
//            }
          //--- Remove HEVC 1g+ only if HEVC of smaller sizes exist
          if ( groupByQualitySZ.containsKey(25) && ( groupByQualitySZ.containsKey(15) || groupByQualitySZ.containsKey(5) )) {
            log.fine "......Found Large video we can prune (HEVC 1g+/900MB+)"
            groupByQualitySZ[25].each { dFile ->
              log.fine "......Delete: ${dFile}"
            }
            filesToDelete += groupByQualitySZ[25]
            groupByQualitySZ.remove(25)
          }
          //--- Remove H264 1g+ only if Smaller Sizes exist (H264 and HEVC)
          if ( groupByQualitySZ.containsKey(40) && ( groupByQualitySZ.containsKey(30) || groupByQualitySZ.containsKey(20) || groupByQualitySZ.containsKey(15) || groupByQualitySZ.containsKey(5) )) {
            log.fine "......Found Large video we can prune (H264 1g+/900MB+)"
            groupByQualitySZ[40].each { dFile ->
              log.fine "......Delete: ${dFile}"
            }
            filesToDelete += groupByQualitySZ[40]
            groupByQualitySZ.remove(40)
          }
          //--- Remove H264 800MB only if Smaller Sizes exist (H264 and HEVC)
          if ( groupByQualitySZ.containsKey(30) && ( groupByQualitySZ.containsKey(20) || groupByQualitySZ.containsKey(15) || groupByQualitySZ.containsKey(5) )) {
            log.fine "......Found Large video we can prune (H264 700/800M+)"
            groupByQualitySZ[30].each { dFile ->
              log.fine "......Delete: ${dFile}"
            }
            filesToDelete += groupByQualitySZ[30]
            groupByQualitySZ.remove(30)
          }
//            groupByQualitySZ.each {key, value ->
//              println "key: ${key}"
//              println "value: ${value}"
//            }
          remainingFiles = []
          groupByQualitySZ.each { key, value ->
            remainingFiles += value.collect{ it }
          }
//          def rftemp = groupByQualitySZ.collect {key, value -> value.collect{ it } }
//          remainingFiles = rftemp[0]
//            remainingFiles = (groupByQualitySZ.collect {key, value -> value.collect{ it } })[0]
        }
      }
      log.finest "After pruneLargerSizesTemp Group: ${group}:[${remainingFiles.size()}]"
      log.finest "After pruneLargerSizesTemp filesToDelete:[${filesToDelete.size()}]"
//        log.finest "remainingFiles:"
//        remainingFiles.each { rfile ->
//          log.finest "\t files: ${rfile}"
//        }

      //--- Prune lower Resolutions for individual episodes
      if (pruneLowerResolutionsTEMP && remainingFiles.size() > 1) {
        log.fine "...Checking for Lower Resolutions"
        LinkedHashMap groupByQualityVF = groupQualityVFGeneration(remainingFiles)
        if ((pruneLowerResolutions4KTEMP && groupByQualityVF.containsKey(lookupVFQualityMetric("1080p")) ) && ( groupByQualityVF.containsKey(lookupVFQualityMetric("2160p")) )) {
          groupByQualityVF[lookupVFQualityMetric("1080p")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVF[lookupVFQualityMetric("1080p")]
          groupByQualityVF.remove(lookupVFQualityMetric("1080p"))
        }
        if (groupByQualityVF.containsKey(lookupVFQualityMetric("720p")) && (groupByQualityVF.containsKey(30) || groupByQualityVF.containsKey(20) || groupByQualityVF.containsKey(10)) ) {
          groupByQualityVF[lookupVFQualityMetric("720p")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVF[lookupVFQualityMetric("720p")]
          groupByQualityVF.remove(lookupVFQualityMetric("720p"))
        }
//        if (groupByQualityVF.containsKey(50) && (groupByQualityVF.containsKey(40) || groupByQualityVF.containsKey(30) || groupByQualityVF.containsKey(20) || groupByQualityVF.containsKey(10)) ) {
//          groupByQualityVF[50].each { dFile ->
//            log.finest "......Delete: ${dFile}"
//          }
//          filesToDelete += groupByQualityVF[50]
//          groupByQualityVF.remove(50)
//        }
        if (groupByQualityVF.containsKey(lookupVFQualityMetric("480p")) && (groupByQualityVF.containsKey(50) || groupByQualityVF.containsKey(40) || groupByQualityVF.containsKey(30) || groupByQualityVF.containsKey(20) || groupByQualityVF.containsKey(10)) ) {
          groupByQualityVF[lookupVFQualityMetric("480p")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVF[lookupVFQualityMetric("480p")]
          groupByQualityVF.remove(lookupVFQualityMetric("480p"))
        }
        if (groupByQualityVF.containsKey(lookupVFQualityMetric("360p")) && (groupByQualityVF.containsKey(60) || groupByQualityVF.containsKey(50) || groupByQualityVF.containsKey(40) || groupByQualityVF.containsKey(30) || groupByQualityVF.containsKey(20) || groupByQualityVF.containsKey(10)) ) {
          groupByQualityVF[lookupVFQualityMetric("360p")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVF[lookupVFQualityMetric("360p")]
          groupByQualityVF.remove(lookupVFQualityMetric("360p"))
        }
        if (groupByQualityVF.containsKey(lookupVFQualityMetric("240p")) && (groupByQualityVF.containsKey(70) || groupByQualityVF.containsKey(60) || groupByQualityVF.containsKey(50) || groupByQualityVF.containsKey(40) || groupByQualityVF.containsKey(30) || groupByQualityVF.containsKey(20) || groupByQualityVF.containsKey(10)) ) {
          groupByQualityVF[lookupVFQualityMetric("240p")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVF[lookupVFQualityMetric("240p")]
          groupByQualityVF.remove(lookupVFQualityMetric("240p"))
        }
        //--- At this point we have removed all the files that do not pass the VF Quality Metric - Aka Resolution Quality Metric
        remainingFiles = []
        groupByQualityVF.each { key, value ->
          remainingFiles += value.collect{ it }
        }
//          remainingFiles = groupByQualityVF.collect {key, value -> value.collect{ it } }[0]
//        groupByQualityVF.each {key, value ->
//          log.finest "groupByQualityVF:[${key}]"
//          value.each { rfile ->
//            log.finest "\t files: ${rfile}"
//          }
//        }
//        log.finest "remainingFiles:"
//        remainingFiles.each { rfile ->
//          log.finest "\t files: ${rfile}"
//        }
      }
      log.finest "After pruneLowerResolutionsTEMP Group: ${group}:[${remainingFiles.size()}]"
      log.finest "After pruneLowerResolutionsTEMP filesToDelete:[${filesToDelete.size()}]"

      //--- We want to prune 8bit files, only for X265/HEVC and X264/AVC but not other codecs
      //--- But no X264/AVC (where it's not common) or codecs other then X265/HEVC
      if ( pruneEightBitX265TEMP  && remainingFiles.size() > 1) {
//        log.finest "Starting Files:"
//        remainingFiles.each { rfile ->
//          log.finest "\t files: ${rfile}"
//        }
        log.fine "...Checking for 8bit Video"
        //--- Group files by Video Codec
//        println "remainingFiles.getClass():[${remainingFiles.getClass()}]"
        LinkedHashMap groupByQualityVC = groupQualityVCGeneration(remainingFiles)
        remainingFiles = []
//        println "remainingFiles.getClass():[${remainingFiles.getClass()}]"
        if (groupByQualityVC.containsKey(lookupVCQualityMetric("HEVC")) ) {
          //--- Group files by Bit Depth
          LinkedHashMap groupByQualityBD = groupQualityBDGeneration(groupByQualityVC[lookupVCQualityMetric("HEVC")])
          //--- Remove 8bit only if we have 10bit files
          if (groupByQualityBD.containsKey(lookupBDQualityMetric("8bit")) && (groupByQualityBD.containsKey(lookupBDQualityMetric("10bit"))) ) {
            log.fine "......Found 8bit video we can prune"
            groupByQualityBD[lookupBDQualityMetric("8bit")].each { dFile ->
              log.fine "......Delete: ${dFile}"
            }
            filesToDelete += groupByQualityBD[lookupBDQualityMetric("8bit")]
            groupByQualityBD.remove(lookupBDQualityMetric("8bit"))
          }
          //--- At this point we have removed all the files that do not pass the VF Quality Metric - Aka Resolution Quality Metric
          groupByQualityBD.each { key, value ->
            remainingFiles += value.collect{ it }
          }
          groupByQualityVC.remove(lookupVCQualityMetric("HEVC"))
        }
        if (groupByQualityVC.containsKey(lookupVCQualityMetric("AVC")) ) {
          //--- Group files by Bit Depth
          LinkedHashMap groupByQualityBD = groupQualityBDGeneration(groupByQualityVC[lookupVCQualityMetric("AVC")])
          //--- Remove 8bit only if we have 10bit files
          if (groupByQualityBD.containsKey(lookupBDQualityMetric("8bit")) && (groupByQualityBD.containsKey(lookupBDQualityMetric("10bit"))) ) {
            log.fine "......Found 8bit video we can prune"
            groupByQualityBD[lookupBDQualityMetric("8bit")].each { dFile ->
              log.fine "......Delete: ${dFile}"
            }
            filesToDelete += groupByQualityBD[lookupBDQualityMetric("8bit")]
            groupByQualityBD.remove(lookupBDQualityMetric("8bit"))
          }
          //--- At this point we have removed all the files that do not pass the VF Quality Metric - Aka Resolution Quality Metric
          groupByQualityBD.each { key, value ->
            remainingFiles += value.collect{ it }
          }
          groupByQualityVC.remove(lookupVCQualityMetric("AVC"))
        }
        //--- At this point we have removed all the files that do not pass the VF Quality Metric - Aka Resolution Quality Metric
        groupByQualityVC.each { key, value ->
          remainingFiles += value.collect{ it }
        }
      }
//      log.finest "remainingFiles:"
//      remainingFiles.each { rfile ->
//        log.finest "\t files: ${rfile}"
//      }
      log.finest "After pruneEightBitX265TEMP Group: ${group}:[${remainingFiles.size()}]"
      log.finest "After pruneEightBitX265TEMP filesToDelete:[${filesToDelete.size()}]"


      //--- We want to prune X264 when we have HEVC versions
      if ( pruneX264TEMP  && remainingFiles.size() > 1) {
//        log.finest "Starting Files:"
//        remainingFiles.each { rfile ->
//          log.finest "\t files: ${rfile}"
//        }
      log.fine "...Checking for H264 Video (prefer HEVC)"
      //--- Group files by Resolution
      LinkedHashMap groupByQualityVF = groupQualityVFGeneration(remainingFiles)
      //--- Group those by Video Codec
      remainingFiles = []
      groupByQualityVF.each {key, value ->
        LinkedHashMap groupByQualityVC = groupQualityVCGeneration(value)
        //--- Prune out all X264 files if there are HEVC files
//          groupByQualityVC.each {key2, value2 ->
//          log.finest "groupByQualityVC:[${key2}]"
//          log.finest "\t files: ${value2}"
//          }
        if (groupByQualityVC.containsKey(lookupVCQualityMetric("AVC")) && groupByQualityVC.containsKey(lookupVCQualityMetric("HEVC")) ) {
          log.fine "......Found X264/AVC video we can prune"
          groupByQualityVC[lookupVCQualityMetric("AVC")].each { dFile ->
            log.fine "......Delete: ${dFile}"
          }
          filesToDelete += groupByQualityVC[lookupVCQualityMetric("AVC")]
          groupByQualityVC.remove(lookupVCQualityMetric("AVC"))
        }
        //--- At this point we have removed all the files that do not pass the VF Quality Metric - Aka Resolution Quality Metric
        groupByQualityVC.each { tempkey, tempvalue ->
          remainingFiles += tempvalue.collect{ it }
        }
      }
        log.finest "After pruneX264TEMP Group: ${group}:[${remainingFiles.size()}]"
        log.finest "After pruneX264TEMP filesToDelete:[${filesToDelete.size()}]"
//        groupByQualityVF.each {key, value ->
//          log.finest "groupByQualityVF:[${key}]"
//          log.finest "\t files: ${value}"
//        }
//        remainingFiles.each { rfile ->
//          log.finest "remainingFiles:"
//          log.finest "\t files: ${rfile}"
//        }
      }
//      log.finest "remainingFiles:"
//      remainingFiles.each { rfile ->
//        log.finest "\t files: ${rfile}"
//      }

      //--- Prune by Groups
      if ( pruneByGroupListTEMP && remainingFiles.size() > 1 ) {
        log.fine "...Checking for Priority Groups to keep:[${numberOfGroupsToKeep}]"
        LinkedHashMap groupByQualityVF = groupQualityVFGeneration(remainingFiles)
        remainingFiles = []
        groupByQualityVF.each { qualityNumber, vfFiles ->
          LinkedHashMap groupByReleaseGroup = groupQualityReleaseGroups(vfFiles, groupsToKeepList, pruneUnknownGroupsTEMP)
//            groupByReleaseGroup.each {releaseGroup, groupFiles ->
//              println "groupByReleaseGroup:[${releaseGroup}]"
//              println "\t Files:${groupFiles}"
//            }
          Integer numberOfGroupsInDataset = groupByReleaseGroup.size()
          Integer numberOfGroupsKept = 0
          Integer numberOfGroupsToKeepInList = groupsToKeepList.size()-1
          Integer onGroupNumber = 0
          while (( onGroupNumber <= numberOfGroupsToKeepInList) && (numberOfGroupsInDataset > numberOfGroupsToKeep)) {
            onGroupNumber+=1
//              println "Looking for Release Group:[${groupsToKeepList[onGroupNumber]}]"
            if (groupByReleaseGroup.containsKey(groupsToKeepList[onGroupNumber])) {
              if ( numberOfGroupsKept < numberOfGroupsToKeep ) {
                numberOfGroupsKept+=1
                log.finest "......Keeping Release Group #${numberOfGroupsKept-1}:[${groupsToKeepList[onGroupNumber]}]"
              } else {
                log.fine "......Found Release Group we can prune:[${groupsToKeepList[onGroupNumber]}]"
                groupByReleaseGroup[groupsToKeepList[onGroupNumber]].each { dFile ->
                  log.fine "......Delete: ${dFile}"
                }
                filesToDelete += groupByReleaseGroup[groupsToKeepList[onGroupNumber]]
                groupByReleaseGroup.remove(groupsToKeepList[onGroupNumber])
              }
            }
          }
          groupByReleaseGroup.each { key, value ->
            remainingFiles += value.collect{ it }
          }
        }
      }
      log.finest "After pruneByGroupList Group: ${group}:[${remainingFiles.size()}]"
      log.finest "After pruneByGroupListTEMP filesToDelete:[${filesToDelete.size()}]"
      if ( remainingFiles.size() != remainingFilesCount) {
        filesNotPruned += remainingFiles
      }
    } else {
      log.fine "...Skipping Group (only 1 video): ${group}"
      skippedFiles += files
//      files.each {
//        log.finest "\t file: $it"
//      }
    }
  }

  remainingFiles = filesToPrune.minus(filesToDelete)
  if ( compareToDir && skippedFiles.size() >= 1 ) {
    println "skippedFiles: ${skippedFiles}"
    skippedFiles.each { File dFile ->
      Boolean moveFile = true
      baseDir.each {baseCompareToDir ->
        if ( dFile.canonicalPath.startsWith(baseCompareToDir.canonicalPath)) {
          moveFile = false
        }
      }
      if ( moveFile ) {
        log.finest "We have files that we skipped and *might* be new - [${dFile}]"
        filestoMove += dFile
      }
//      if ( ! dFile.canonicalPath.startsWith(compareToDir.canonicalPath)) {
//        log.finest "We have files that we skipped and *might* be new - [${dFile}]"
//        filestoMove += dFile
//      }
    }
  }
  if ( compareToDir && filesToDelete.size() >= 1) {
    println "filesNotPruned: ${filesNotPruned}"
    filesNotPruned.each { File dFile ->
      Boolean moveFile = true
      baseDir.each {baseCompareToDir ->
        if ( dFile.canonicalPath.startsWith(baseCompareToDir.canonicalPath)) {
          moveFile = false
        }
      }
      if ( moveFile ) {
        log.finest "We have files we didn't prune that are *higher* quality - [${dFile}]"
        filestoMove += dFile
      }
//      if ( ! dFile.canonicalPath.startsWith(compareToDir.canonicalPath)) {
//        log.fine "We have files we didn't prune that are *higher* quality - [${dFile}]"
//        filestoMove += dFile
//      }
    }
  }
  if ( compareToDir ) {
    filesToDelete = filestoMove
  }
  if (filesToDelete.size() >= 1 ) {
    log.fine '//-----------------------\\\\'
    log.fine "We have files to delete - [${filesToDelete.size()}]"
    filesToDelete.each { dFile ->
      log.fine "\t Delete: ${dFile}"
    }
//    log.finest "\t getName: ${folderToPrune.getName()}" // Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
    def myNameCount = folderToPrune.toPath().getNameCount()
//    log.finest  "\t getNameCount of folderToPrune: [${folderToPrune.toPath().getNameCount()}] - ${args[0]}:" // [3] - W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
//    log.finest  "\t myNameCount of folderToPrune: [${myNameCount}] - ${folderToPrune}:"
    def myArgNameCount = args[0].toPath().getNameCount()
//    log.finest  "\t myArgNameCount of args[0]: [${myArgNameCount}] - ${args[0]}:"
//    log.finest  "\t getNameCount of args[0]: [${args[0].toPath().getNameCount()}] - ${args[0]}:" // [1] - W:\2-AniAdd\
//    log.finest  "\t myArgNameCount of outputFolder: [${myArgNameCount}] - ${outputFolder}:"
//    log.finest  "\t getNameCount of outputFolder: [${outputFolder.toPath().getNameCount()}] - ${outputFolder}:" // [2] - W:\2-AniAdd\AniAddSortedSeries
    def myRelativePath
    if ( myArgNameCount != myNameCount ) {
      myRelativePath = folderToPrune.toPath().subpath(myArgNameCount, myNameCount)
    } else {
      myRelativePath = folderToPrune.getName()
    }
//    log.finest  "\t Relative path of directory to outputFolder using subpath: ${myRelativePath}" // winter\Youkai Watch Jam Youkai Gakuen Y - N to no Souguu [anidb-15268]
    def aniAddFormat = "${myRelativePath}/{fn}"
//    log.finest  "\t \t InputFiles: ${filesToDelete}"
//    log.fine  "\t aniAddFormat: ${aniAddFormat}"
//    log.fine  "\t rename(file: ${filesToDelete}, format: aniAddFormat, db: file)"
    rename(file:filesToDelete, format: aniAddFormat, db: 'file')
  } else {
    log.fine '//-----------------------\\\\'
    log.fine "Nothing to Delete"
  }
}

//     Check if it has a menu with getMediaInfo(f, '{menu}')? - Granted it seems to only see the "last" menu (not sure why somne files have 2 menus
//     // def hd = getMediaInfo(f, '{hd}') // returns UHD/HD/SD
//     // def vc = getMediaInfo(f, '{vc}') // returns AVC OR x264/HVEC or x265 - Similiar to  mediainfo.video.format (but doesn't seem 100%)
//     // def cf = getMediaInfo(f, '{cf}') // returns matroska - Similiar to mediainfo.general.format
//     // def vf = getMediaInfo(f, '{vf}') // returns 1080p, 720p
//     // def resolution = getMediaInfo(f, '{resolution}') // returns 1920x1080, 1280x720
//     // def bitdepth = getMediaInfo(f, '{bitdepth}') // returns 8/10 etc
//     // def hdr = getMediaInfo(f, '{hdr}') // returns null if not HDR (which most anime is not HDR)
//     // def vs =  getMediaInfo(f, '{vs}') // returns null if it can't figure out video source (which looks to be based on filename, not very useful for anime as it's ... wierd) like WEB-DL etc.
//     // def tags = getMediaInfo(f, '{tags}') // returns tags .. which don't seem to exist on any anime?
//     // def group = getMediaInfo(f, '{group}') // Returns release group, but only for "supported release groups" aka it works when it recognizes
//     // // the release group, but it doesn't really figure it out from the filename or anything. Often null for Anime.
//     // def metadata = f.metadata // Bofuri: I Don`t Want to Get Hurt, So I`ll Max Out My Defense. - 09 - Defense and Fourth Event
//     // def sizeinMB = getMediaInfo(f, '{megabytes}')