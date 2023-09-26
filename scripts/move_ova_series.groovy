#!/usr/bin/env filebot -script
@Grapes(
    @Grab(group='commons-io', module='commons-io', version='2.8.0')
)

import com.cedarsoftware.util.io.JsonObject
import net.filebot.Cache
import net.filebot.CacheType
import net.filebot.Logging
import org.apache.commons.io.FilenameUtils
import java.util.regex.Matcher

////--- VERSION 1.0.0
//// https://mvnrepository.com/artifact/commons-io/commons-io

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
onlylist  = any { onlylist.toBoolean() } { false }
movieFolder = tryQuietly { movieFolder as File}
releaseFolder = tryQuietly { releaseFolder as File }


// sanity checks
Logging.log.finest "movieFolder: ${movieFolder}"
if (movieFolder == null || !movieFolder.isDirectory()) {
  die "Invalid usage: movieFolder folder must exist and must be a directory: $movieFolder"
}
Logging.log.finest "releaseFolder: ${releaseFolder}"
if (releaseFolder == null || !releaseFolder.isDirectory()) {
  die "Invalid usage: releaseFolder folder must exist and must be a directory: $releaseFolder"
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
  new URL('https://raw.githubusercontent.com/manami-project/anime-offline-database/master/' + it) }.expire(Cache.ONE_WEEK).get()

// Simple get list of video files
//ArrayList input = args.getFiles{ f -> f.isVideo() }
Logging.log.finest "args: ${args}"
Logging.log.finest "_args: ${_args}"
ArrayList inputFolders = args.getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter

Logging.log.info "InputFolder Count: ${inputFolders.size()}"
inputFolders.each { File folderToMove ->
  String myFormat
  String myCompareName
  Matcher anidbIdTemp = folderToMove.name =~ /^(.+)\[.*\-(\d+)\]/
  Integer anidbId = anidbIdTemp[0][2].toInteger()
  def returnThing = setAnimeTypeFromAID(animeOfflineDatabase, anidbId, null, false, false)
  if ( returnThing.specialType == "OVA" ) {
    Integer myAniDBEpisodeCount = aniDBGetEpisodeNumberForAID(animeOfflineDatabase, anidbId)
    Logging.log.finest "OVA Episode Count: ${myAniDBEpisodeCount}"
    if ( myAniDBEpisodeCount == 1 ) {
      Logging.log.fine "folderToMove: ${folderToMove.name}"
      Logging.log.info "AniDB ID: ${anidbId}"
      Logging.log.info "    OVA Detected with with only 1 Episode:[${myAniDBEpisodeCount}]"
      myFormat = "{'movies'}/{\'${folderToMove.name.replaceAll(/'/, /\\'/)}\'}/{fn}"
      myCompareName = "${outputFolder}/movies/${folderToMove.name}"
      Logging.log.info "    Set Format to ${myFormat}"
    } else {
      Logging.log.fine "folderToMove: ${folderToMove.name}"
      Logging.log.info "AniDB ID: ${anidbId}"
      Logging.log.info "    OVA Detected with with more then 1 Episode:[${myAniDBEpisodeCount}]"
      myFormat = "{'releases'}/{\'${folderToMove.name.replaceAll(/'/, /\\'/)}\'}/{fn}"
      myCompareName = "${outputFolder}/releases/${folderToMove.name}"
      Logging.log.info "    Set Format to ${myFormat}"
    }
  } else if ( returnThing.isMovieType ) {
    Logging.log.fine "folderToMove: ${folderToMove.name}"
    Logging.log.info "AniDB ID: ${anidbId}"
    Logging.log.info "    Movie Detected"
    myFormat = "{'movies'}/{\'${folderToMove.name.replaceAll(/'/, /\\'/)}\'}/{fn}"
    myCompareName = "${outputFolder}/movies/${folderToMove.name}"
    Logging.log.info "    Set Format to ${myFormat}"
  } else {
    Logging.log.fine "folderToMove: ${folderToMove.name}"
    Logging.log.info "AniDB ID: ${anidbId}"
    Logging.log.info "    Release Detected"
    //.replaceAll(/`/, /\\'/)
    myFormat = "{'releases'}/{\'${folderToMove.name.replaceAll(/'/, /\\'/)}\'}/{fn}"
    myCompareName = "${outputFolder}/releases/${folderToMove.name}"
    Logging.log.info"    Set format to ${myFormat}"
  }
  if ( FilenameUtils.separatorsToSystem(folderToMove.toString()).equalsIgnoreCase(FilenameUtils.separatorsToSystem(myCompareName))) {
    Logging.log.info "    We are not moving as folder did not change"
    Logging.log.finest "    ${FilenameUtils.separatorsToSystem(folderToMove.toString())}:${FilenameUtils.separatorsToSystem(myCompareName)}"
  } else {
    Logging.log.fine  "\t rename(file: ${folderToMove}, format: myFormat, db: file)"
    rename(folder:folderToMove, format: myFormat, db: 'file')
  }
}