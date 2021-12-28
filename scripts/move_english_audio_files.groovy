#!/usr/bin/env filebot -script
import net.filebot.Logging

//--- VERSION 1.2.1
// https://mvnrepository.com/artifact/commons-io/commons-io
@Grapes(
    @Grab(group='commons-io', module='commons-io', version='2.8.0')
)

import org.apache.commons.io.FileUtils
import org.apache.ivy.plugins.version.Match

import java.util.regex.Matcher

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

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}

// Include My Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // AniDB Stuff
include('lib/tvdb')  // TVDB Stuff
include('lib/manami')  // Anime offline Database Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/sorter') // Renamer Sorter methods

// Simple get list of video files
//ArrayList input = args.getFiles{ f -> f.isVideo() }
ArrayList inputFolders = args[0].getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter

//String detectReleaseGroupFromFileName(File file) {
//  def myDetectedGroup = getMediaInfo(file, '{group}')
//  if ( myDetectedGroup == null ) {
//    def myRegexMatcher = file.name =~ /^\[([^)\]]*)\].*$/
//    if ( myRegexMatcher ) {
//      myDetectedGroup = myRegexMatcher[0][1]
//    }
//  }
//  groupsWithVariableFilenamePositionsUsingBrackets = /Rady|NHK/
//  if ( myDetectedGroup == null ) {
//    def myRegexMatcher = file.name =~ /\[(/ + groupsWithVariableFilenamePositionsUsingBrackets + /)\].*$/
//    if ( myRegexMatcher ) {
//      myDetectedGroup = myRegexMatcher[0][1]
//    }
//  }
//  if ( myDetectedGroup == null ) {
//    def myRegexMatcher = file.name =~ /^(.+)\s-\s(\d{1,3}|S\d{1,3})(?>v\d)?\s-\s(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
//    if ( myRegexMatcher ) {
//      // AniAdd Episode Format
//      myDetectedGroup = myRegexMatcher[0][5]
//    } else {
//      // AniAdd Movie Format
//      myRegexMatcher = file.name =~ /^(.+)(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\[([^)\]]*)\])(\.\d)?\.[^.]+$/
//      if ( myRegexMatcher ) {
//        myDetectedGroup = myRegexMatcher[0][3]
//      }
//    }
//  }
//  return myDetectedGroup
//}

// Problem #1 - Mislabelled Audio
// - Some groups do not release English Audio, but sometimes label the Japanese audio as English
// [BakedFish], [USD], [QCE], [DeadFish]
//     // def group = getMediaInfo(f, '{group}') // Returns release group, but only for "supported release groups" aka it works when it recognizes
//     // // the release group, but it doesn't really figure it out from the filename or anything. Often null for Anime.
inputFolders.each { folderToPrune ->
  log.fine "folderToPrune: ${folderToPrune}"
  def filesToDelete = []
  def filesToPrune = folderToPrune.getFiles{ f -> f.isVideo() }
  filesToPrune.each { file ->
    Boolean hasEnglishAudio = false
    Boolean hasAudio = false
    def mediaAudioCount = getMediaInfo(file, '{media.AudioCount}')
    def mediaAudioLangList = getMediaInfo(file, '{media.Audio_Language_List}')
//    def mediaTextCount = getMediaInfo(f, '{media.TextCount}')
//    def mediaTextLangList = getMediaInfo(f, '{media.Text_Language_List}')
//    def mediaAudioDefault = getMediaInfo(f, '{audio.Default}')
//    x = getMediaInfo(f, '{media.AudioCount}')
//    y = getMediaInfo(f, '{media.Audio_Language_List}')
//    w = getMediaInfo(f, '{media.TextCount}')
//    z = getMediaInfo(f, '{media.Text_Language_List}')
//    v = getMediaInfo(f, '{audio.Default}')
    if (mediaAudioCount != null) {
      // println "\t $filename has $x Audio Count. "
      if (mediaAudioLangList != null) {
        hasAudio = true
//         println "\t The Audio Languages are $mediaAudioLangList"
        if (mediaAudioLangList.matches(".*([eE]nglish).*")) {
          // println "\t MATCH ENGLISH"
          // println "\t English Audio found (Languages are: $y)"
          hasEnglishAudio = true
        } else {
          // println "\t No English Audio found (Languages are: $y)"
          hasEnglishAudio = false
        }
      } else {
        // Means mediaAudioLangList is null
        hasAudio = true
        hasEnglishAudio = false
        // println "\t Unknown Audio Language(s)"
      }
    } else {
      // MeansmediaAudioCount is null
      hasAudio = false
      // println "\t has NO Audio (detected)"
    }
    if ( hasEnglishAudio ) {
      myReleaseGroup = detectAnimeReleaseGroupFromFile(file)
      if ( myReleaseGroup == null ) {
        log.finest "Release Group Not Detected:[$file.name]"
      }
//      println "MyReleaseGroup:[${myReleaseGroup}]"
      if ( mediaAudioCount.toInteger() > 1 ) {
//        log.fine '//-----------------------\\\\'
//        println "multiAudioRequired Match"
//        println "MyReleaseGroup:[${myReleaseGroup}]"
//        println "mediaAudioCount:[${mediaAudioCount}]"
        filesToDelete += file
      } else {
        def singleAudioRequiredGroups = /Moviejunkie2009|ARR|KaiDubs|a4e|Golumpa|Tomica Wiki|Samir755|Zeph|ShadowFox|kuchikirukia|Darkulime|Fullmetal|Abystoma|Fete Rider|Exiled-Destiny|Dodgy|Rady|DragsterPS|LostYears|Saier_404|Zweeflol|tlacatlc6/
        if (file.name.findMatch(/\[/ + singleAudioRequiredGroups + /\]/) ) {
//          log.fine '//-----------------------\\\\'
//          println "singleAudioRequiredGroups Match"
//          println "MyReleaseGroup:[${myReleaseGroup}]"
//          println "mediaAudioCount:[${mediaAudioCount}]"
          filesToDelete += file
        } else {
          log.fine '//-----------------------\\\\'
          println "Single Audio match FAIL"
          println "MyReleaseGroup:[${myReleaseGroup}]"
          log.fine '//-----------------------\\\\'
        }
      }
//      if ( myReleaseGroup == null && mediaAudioCount.toInteger() > 1  ) {
//        log.fine '//-----------------------\\\\'
//        println "mediaAudioCount:[${mediaAudioCount}]"
//        filesToDelete += file
//      }
//      def multiAudioRequiredGroups = /NoobSubs|Rias|Hakata Ramen|HR|edge/
//      if (file.name.findMatch(/\[/ + multiAudioRequiredGroups + /\]/) && mediaAudioCount.toInteger() > 1 ) {
//        log.fine '//-----------------------\\\\'
//        println "multiAudioRequiredGroups Match"
//        println "MyReleaseGroup:[${myReleaseGroup}]"
//        println "mediaAudioCount:[${mediaAudioCount}]"
//        filesToDelete += file
//      }
//      def singleAudioRequiredGroups = /Tomica Wiki|Samir755|Zeph|ShadowFox|kuchikirukia|Darkulime|Fullmetal|Abystoma|Fete Rider|Exiled-Destiny|Dodgy|Rady|DragsterPS|LostYears|Saier_404|Zweeflol|tlacatlc6|Samir755/
//      if (file.name.findMatch(/\[/ + singleAudioRequiredGroups + /\]/) ) {
//        log.fine '//-----------------------\\\\'
//        println "singleAudioRequiredGroups Match"
//        println "MyReleaseGroup:[${myReleaseGroup}]"
//        println "mediaAudioCount:[${mediaAudioCount}]"
//        filesToDelete += file
//      }
    }
  }
  if (filesToDelete.size() >= 1 ) {
    log.fine '//-----------------------\\\\'
    log.fine "We have files to move - [${filesToDelete.size()}]"
    filesToDelete.each { dFile ->
      log.fine "\t Delete: ${dFile}"
    }
//    log.finest "\t getName: ${folderToPrune.getName()}" // Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
//    log.finest  "\t getNameCount of outputDir: [${folderToPrune.toPath().getNameCount()}] - ${args[0]}:" // [3] - W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
    def myNameCount = folderToPrune.toPath().getNameCount()
//    log.finest  "\t getNameCount of outputFolder: [${outputFolder.toPath().getNameCount()}] - ${outputFolder}" // [2] - W:\2-AniAdd\AniAddSortedSeries
    def myArgNameCount = outputFolder.toPath().getNameCount()
    def myRelativePath = folderToPrune.toPath().subpath(myArgNameCount, myNameCount)
//    log.finest  "\t Relative path of directory to outputFolder using subpath: ${myRelativePath}" // winter\Youkai Watch Jam Youkai Gakuen Y - N to no Souguu [anidb-15268]
    def aniAddFormat = "${myRelativePath}/{fn}"
//    log.finest  "\t \t InputFiles: ${filesToDelete}"
//    log.fine  "\t aniAddFormat: ${aniAddFormat}"
//    log.fine  "\t rename(file: ${filesToDelete}, format: aniAddFormat, db: file)"
    rename(file:filesToDelete, format: aniAddFormat, db: 'file')
  } else {
    log.fine "Nothing to Move"
  }
}