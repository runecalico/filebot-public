#!/usr/bin/env filebot -script
//--- VERSION 2.1.1
// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
@Grapes(
    @Grab(group='org.apache.commons', module='commons-text', version='1.9')
)

import com.cedarsoftware.util.io.JsonObject
import org.apache.commons.text.similarity.JaroWinklerDistance
import net.filebot.media.AutoDetection
import net.filebot.util.FileSet
import net.filebot.Logging

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

// enable/disable features as specified via --def parameters
unsorted  = tryQuietly { unsorted.toBoolean() }
music     = tryQuietly { music.toBoolean() }
subtitles = tryQuietly  { subtitles.split(/\W+/ as Closure) as List }
artwork   = tryQuietly  { artwork.toBoolean() && !testRun }
extras    = tryQuietly { extras.toBoolean() }
clean     = tryQuietly  { clean.toBoolean() }
exec      = tryQuietly  { exec.toString() }

// array of kodi/plex/emby hosts
kodi = tryQuietly { any { kodi } { xbmc }.split(/[ ,;|]+/)*.split(/:(?=\d+$)/).collect { it.length >= 2 ? [host: it[0], port: it[1] as int] : [host: it[0]] } }
plex = tryQuietly { plex.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }
emby = tryQuietly { emby.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }
sonarr = tryQuietly { sonarr.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }

// extra options, myepisodes updates and email notifications
failOnError = any { failOnError.toBoolean() } { false } // Basically fail if nothing is renamed (which is not useful to me)
extractFolder      = tryQuietly { extractFolder as File }
skipExtract        = tryQuietly { skipExtract.toBoolean() }
deleteAfterExtract = tryQuietly { deleteAfterExtract.toBoolean() }
excludeList        = tryQuietly { def f = excludeList as File; f.isAbsolute() ? f : outputFolder.resolve(f.path) }
excludeLink        = tryQuietly { excludeLink.toBoolean() }
myepisodes         = tryQuietly { myepisodes.split(':', 2) as List }
gmail              = tryQuietly { gmail.split(':', 2) as List }
mail               = tryQuietly { mail.split(':', 5) as List }
pushover           = tryQuietly { pushover.split(':', 2) as List }
pushbullet         = tryQuietly { pushbullet.toString() }
discord            = tryQuietly { discord.toString() }
storeReport        = tryQuietly { def f = storeReport as File; f.isAbsolute() ? f : outputFolder.resolve(f.path) }
reportError        = tryQuietly { reportError.toBoolean() }
clearXattr         = tryQuietly { clearXattr.toBoolean() }
useXattrDB         = tryQuietly { useXattrDB.toBoolean() }
skipMovies         = tryQuietly { skipMovies.toBoolean() }
useFBAutoDetection = tryQuietly { useFBAutoDetection.toBoolean() }
useGroupByAutodection = tryQuietly { useGroupByAutodection.toBoolean() }
String aniDBuserAgent         = any { aniDBuserAgent.toString() } { 'nosuchuser/filebot' }
String aniDBTitleXMLFilename  = any { aniDBTitleXMLFilename.toString() } { 'anime-titles.xml' }
String aniDBSynonymXMLFilename  = any { aniDBSynonymXMLFilename.toString() } { 'anime-synonyms.xml' }
Integer aniDBXMLrefreshDays   = any { aniDBXMLrefreshDays.toInteger() } { 7 }
Integer aniDBSynonymRefreshDays   = any { aniDBSynonymRefreshDays.toInteger() } { 7 }
useFilebotAniDBAliases = any { useFilebotAniDBAliases.toBoolean() } { false }
showMediaInfo      = any { showMediaInfo.toBoolean() } { false }
ignoreOrphanSubtitles      = any { ignoreOrphanSubtitles.toBoolean() } { false }
// When filebot renames some files of a batch, normally the 2nd round inherits the strictness (by default strict)
// This changes it so the 2nd round is non-strict. The theory is that
// A) All the files in a batch share the same name
// B) As long as it matches one file on strict mode, then files that do not match are also in the same series
// A *usually* means all the files in a batch are from the same series.
// B) It's probably true that all the files are the same series from a TVDB standpoint, but
// --- it is also common that files might be from different seasons, which isn't going to be helped
// --- by this option
useNonStrictPartialRenames = any { useNonStrictPartialRenames.toBoolean() } { false }
// Not something to enable by default
// This is a work around to overcome instances where filebot will just not recognize a file
// when I've looked at the script output and know for certain the 1.0 match is in fact correct
// This option turns on non-strict ONLY for 1.0 matches in AniDB
// This is not always as simple as it seems. Frequently release groups will include specials
// in their releases and just increment the episode #.  This of course makes it not match
// as there is no episode of that number, and the filename doesn't indicate it's a special or anything
// Aka series x - 26.mkv where series x has 25 normal episodes and 1 special ...
useNonStrictOnAniDBFullMatch = any { useNonStrictOnAniDBFullMatch.toBoolean() } { false }
useNonStrictOnAniDBSpecials = any { useNonStrictOnAniDBSpecials.toBoolean() } { false }
useNonStrictOnTVDBSpecials = any { useNonStrictOnTVDBSpecials.toBoolean() } { false }
useNonStrictOnAniDBMovies = any { useNonStrictOnAniDBMovies.toBoolean() } { false }
ignoreVideoExtraFoldersRegex    = any { ignoreVideoExtraFoldersRegex } { /(?i)^(extra[s]?|bonus)$/ }
ignoreVideoExtraFilesRegex = any { ignoreVideoExtraFilesRegex } { /(?i:Sample|\b(NCOP|NCED)\d{0,3}\b|Clean\s*(ED|OP|Ending|Opening)|Creditless\s*(ED|OP|Ending|Opening)|Textless\s*(ED|OP|Ending|Opening)|\b(BD|Menu)\b\s*\b(BD|Menu)\b|Character\b.*\bPV\b.*\bCollection\b|ON\sAIR\sMaterials}Previews|PMV|\bPV\b|PV\d+)|Trailer|Extras|Featurettes|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|\b(OP|ED)\b(\d+)?|Formula.1.\d{4}(?=\b|_)/ }
useBaseAnimeNameWithSeriesSyntax = any { useBaseAnimeNameWithSeriesSyntax.toBoolean() } { false }
breakAfterGroups  = any { breakAfterGroups.toBoolean() } { false }
useIndividualFileRenaming = any { useIndividualFileRenaming.toBoolean() } { true }
//generateFilebotRenamePS  = any { generateFilebotRenamePS.toBoolean() } { false }
//filebotHatedFileNamesFileName = "RunMeToSeeIfThereAreHatedFileNamesByFilebotRenameMethod.ps1"
// TODO
// Allow non-strict for TVDB 1.0 Matches?
// aka useNonStrictOnTVDBFullMatch

// user-defined filters
ignore      = any { ignore } { null }
minFileSize = any { minFileSize.toLong() } { 50 * 1000L * 1000L }
minLengthMS = any { minLengthMS.toLong() } { 10 * 60 * 1000L }

// series / anime / movie format expressions
specialFormat  = any { specialFormat } { _args.format } { '{ \'specials\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
animeFormat    = any { animeFormat } { _args.format } { '{ fn }' }
specialTypeFormat = any { specialTypeFormat } { animeFormat } { _args.format } { '{ \'specialType\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
movieFormat    = any { movieFormat } { _args.format } { '{ \'movies\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
unsortedFormat = any { unsortedFormat } { 'Unsorted/{f.structurePathTail}' }
extrasFormat = any { extrasFormat } { 'extrasFormat/{f.structurePathTail}' }

locale = any { _args.language.locale } { Locale.ENGLISH }

// include artwork/nfo, pushover/pushbullet and ant utilities as required
//**// Uses groovy 'include' to source into this script other "library" scripts.
if (artwork || kodi || plex || emby) { include('lib/htpc') }
if (pushover || pushbullet || gmail || mail) { include('lib/web') }

// Include My Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // AniDB Stuff
include('lib/tvdb')  // TVDB Stuff
include('lib/manami')  // Anime offline Database Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/sorter') // Renamer Sorter methods

// check input parameters
/*def ut = _def.findAll { k, v -> k.startsWith('ut_') }.collectEntries { k, v ->
    if (v ==~ /[%$][\p{Alnum}\p{Punct}]+/) {
    log.warning "Bad $k value: $v"
    v = null
    }
    return [k.substring(3), v ? v : null]
}*/

if (_args.db) {
  log.warning 'Invalid usage: The --db option has no effect'
}

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}

/*if (ut.dir) {
  if (ut.state_allow && !(ut.state ==~ ut.state_allow)) {
    die "Invalid state: $ut.state != $ut.state_allow", ExitCode.NOOP
  }
  if (args.size() > 0) {
    die "Invalid usage: use either script parameters $ut or file arguments $args but not both"
  }
  if (ut.dir == '/') {
    die "Invalid usage: No! Are you insane? You can't just pass in the entire filesystem. Think long and hard about what you just tried to do."
  }
  if (ut.dir.toFile() in outputFolder.listPath()) {
    die "Invalid usage: output folder [$outputFolder] must be separate from input folder $ut"
  }
} else if (args.size() == 0) {
  die 'Invalid usage: no input'
} else if (args.any { f -> f in outputFolder.listPath() }) {
  die "Invalid usage: output folder [$outputFolder] must be separate from input arguments $args"
} else if (args.any { f -> f in File.listRoots() }) {
  die "Invalid usage: input $args must not include a filesystem root"
}*/

if (args.size() == 0) {
  die 'Invalid usage: no input'
} else if (args.any { f -> f in outputFolder.listPath() }) {
  die "Invalid usage: output folder [$outputFolder] must be separate from input arguments $args"
} else if (args.any { f -> //noinspection GroovyInArgumentCheck
  f in File.listRoots() }) {
  die "Invalid usage: input $args must not include a filesystem root"
}
// ---------- Download AniDB's Title XML ---------- //
log.finest "aniDBTitleXMLFilename: ${aniDBTitleXMLFilename}"
aniDBXMLDownload(aniDBuserAgent, aniDBTitleXMLFilename, aniDBXMLrefreshDays)
// --- You need to turn off the Namespace awareness else you will get this wierdness when trying to parse the languages for the titles.
// --- title[attributes={{http://www.w3.org/XML/1998/namespace}lang=en, type=short}; value=[CotS]]
aniDBTitleXML = new groovy.xml.XmlParser(false, false).parse(aniDBTitleXMLFilename) // XMLParser

// ---------- Anime Synonyms (Created From Anime Offline Database) ---------- //
// --- I am not using Anime Offline Database directly due to a factor of the data tht goes into creating it, that has synonyms that match multiple
// --- Entries (AniDB included), which will negtively impact the data matching this script does and introduce "incorrect" Series/Season matches.
// --- So I have parsed AOD and created a list of synonyms for AniDB that don't already exist for a title in AniDB.
log.finest "aniDBSynonymXMLFilename: ${aniDBSynonymXMLFilename}"
// --- You need to turn off the Namespace awareness else you will get this wierdness when trying to parse the languages for the titles.
// --- title[attributes={{http://www.w3.org/XML/1998/namespace}lang=en, type=short}; value=[CotS]]
aniDBSynonymDownload(aniDBSynonymXMLFilename, aniDBSynonymRefreshDays)
aniDBSynonymXML = new groovy.xml.XmlParser(false, false).parse(aniDBSynonymXMLFilename) // XMLParser

// ---------- Download/Cache Anime Offline database ---------- //
// This is used to validate AniDB AID's matched are for Movies (not TV)
// https://github.com/manami-project/anime-offline-database
// Json - https://github.com/manami-project/anime-offline-database/raw/master/anime-offline-database.json
// com.cedarsoftware.util.io.JsonObject
JsonObject animeOfflineDatabase = Cache.getCache('animne-offline-database-json', CacheType.Persistent).json('anime-offline-database.json') {
  new URL('https://raw.githubusercontent.com/manami-project/anime-offline-database/master/' + it) }.expire(Cache.ONE_DAY).get()
//println "animeOfflineDatabase.getClass:[${animeOfflineDatabase.getClass()}]"

// ---------- Download/Cache Scud Lee's Anime List of TheTVDB/AniDB Mappings database ---------- //
//scudlessAnimeTitles = Cache.getCache('scudlee-anime-list', CacheType.Persistent).xml('anime-list.xml') {
//    new URL('https://raw.githubusercontent.com/ScudLee/anime-lists/master/'+it) }.expire(Cache.ONE_DAY).get()
// --- Filebot's xml implementation is using the XPathUtilities library and is at net.filebot.util.XPathUtilities
// ----- Not entirely sure on docs, but this seems to kinda work: https://docs.oracle.com/javase/8/docs/api/index.html?javax/xml/xpath/package-summary.html

// collect input fileset as specified by the given --def parameters
// - This basically has a list of all files (fullpath)
roots = args

/*if (args.size() == 0) {
  // assume we're called with utorrent parameters (account for older and newer versions of uTorrents)
  if (ut.kind == 'single' || (ut.kind != 'multi' && ut.dir && ut.file)) {
    roots = [new File(ut.dir, ut.file).getCanonicalFile()] // single-file torrent
    } else {
    roots = [new File(ut.dir).getCanonicalFile()] // multi-file torrent
  }
}*/

// helper function to work with the structure relative path rather than the whole Absolute path
@SuppressWarnings('GrMethodMayBeStatic')
def relativeInputPath(f) {
  def r = roots.find { r -> f.path.startsWith(r.path) && r.isDirectory() && f.isFile() }
  if (r != null) {
    return f.path.substring(r.path.length() + 1)
  }
  return f.name
}

// define and load exclude list (e.g. to make sure files are only processed once)
excludePathSet = new FileSet()

if (excludeList) {
  if (excludeList.exists()) {
    try {
      excludePathSet.load(excludeList)
        } catch (e) {
      die "Failed to read excludes: $excludeList: $e"
    }
    log.fine "Use excludes: $excludeList (${excludePathSet.size()})"
    } else {
    log.fine "Use excludes: $excludeList"
    if ((!excludeList.parentFile.isDirectory() && !excludeList.parentFile.mkdirs()) || (!excludeList.isFile() && !excludeList.createNewFile())) {
      die "Failed to create excludes: $excludeList"
    }
  }
}

extractedArchives = []
temporaryFiles = []

def extract(f) {
  // avoid cyclical archives that extract to the same output folder over and over
  if (f in extractedArchives) {
    return []
  }

  def folder = new File(extractFolder ?: f.dir, f.nameWithoutExtension)
  def files = extract(file: f, output: folder.resolve(f.dir.name), conflict: 'auto', filter: { it.isArchive() || it.isVideo() || it.isSubtitle() || (music && it.isAudio()) }, forceExtractAll: true) ?: []

  extractedArchives += f
  temporaryFiles += folder
  temporaryFiles += files

  // resolve newly extracted files and deal with disk folders and hidden files correctly
  return [folder]
}

def acceptFile(f) {
  if (f.isHidden()) {
    log.finest "Ignore hidden: $f"
    return false
  }

  if (f.isDirectory() && f.name ==~ /[.@].+|bin|initrd|opt|sbin|var|dev|lib|proc|sys|var.defaults|etc|lost.found|root|tmp|etc.defaults|mnt|run|usr|System.Volume.Information/) {
    log.finest "Ignore system path: $f"
    return false
  }

  // Ignore anything where the parent folder is containing the text extra or extras (case insensitive)
  if (f.isVideo() && parentFilePath(f) =~ /(?i)^(extra[s]?|bonus)$/ ) {
    log.finest "Ignore Video Extra files in ${parentFilePath(f)} directory: $f"
    return false
  }

  // Updated with latest Video Extra RegEx, and making with Step 1 Regex on the file name.
  if ((f.isVideo() || f.isSubtitle()) && ( f.name.replaceAll(/_/, ' ').replaceAll(/\.(?!(\d\.[^.]+$|[^.]+$))/, ' ') =~ /(?i:Sample|\b(NCOP|NCED)\d{0,3}\b|Clean\s*(ED|OP|Ending|Opening)|Creditless\s*(ED|OP|Ending|Opening)|Textless\s*(ED|OP|Ending|Opening)|\b(BD|Menu)\b\s*\b(BD|Menu)\b|Character\b.*\bPV\b.*\bCollection\b|ON\sAIR\sMaterials}Previews|PMV|\bPV\b|PV\d+)|Trailer|Extras|Featurettes|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|\b(OP|ED)\b(\d+)?|Formula.1.\d{4}(?=\b|_)/ )) {
    log.finest "Ignore video extra: $f"
    return false
  }

  // ignore if the user-defined ignore pattern matches
  if (f.path.findMatch(ignore)) {
    log.finest "Ignore pattern: $f"
    return false
  }

  // ignore archives that are on the exclude path list
  if (excludePathSet.contains(f)) {
    return false
  }

  // accept folders right away and skip file sanity checks
  if (f.isDirectory()) {
    return true
  }

  // check if file exists
  if (!f.isFile()) {
    log.warning "File does not exist: $f"
    return false
  }

  // ignore previously linked files
  if (excludeLink && (f.symlink || f.linkCount != 1)) {
    log.finest "Exclude superfluous link: $f [$f.linkCount] $f.key"
    return false
  }

  // accept archives if the extract feature is enabled
  if (f.isArchive() || f.hasExtension('001')) {
    return !skipExtract
  }

  // ignore iso images that do not contain a video disk structure
  if (f.hasExtension('iso') && !f.isDisk()) {
    log.fine "Ignore disk image: $f"
    return false
  }

  // ignore small video files
  if (minFileSize > 0 && f.isVideo() && f.length() < minFileSize) {
    log.fine "Skip small video file: $f [$f.displaySize]"
    return false
  }

  // ignore short videos
  if (minLengthMS > 0 && f.isVideo() && any { f.mediaCharacteristics.duration.toMillis() < minLengthMS } { false }) {
    log.fine "Skip short video: $f [$f.mediaCharacteristics.duration]"
    return false
    }

  // ignore subtitle files without matching video file in the same or parent folder (in strict mode only)
  if ( ignoreOrphanSubtitles && f.isSubtitle() && ![f, f.dir].findResults{ it.dir }.any{ it.listFiles{ it.isVideo() && f.isDerived(it) }}) {
    log.fine "Ignore orphaned subtitles: $f"
    return false
  }

  // Ignore video files that have null/undefined minutes. This is an *indicator* of something wrong with the video file
  if (f.isVideo() && (getMediaInfo(f, '{minutes}') == null) ) {
      log.fine "$f - Video file has null minutes"
      if (showMediaInfo) {
          println ' File / Object / MediaInfo '.center(80, '-')
          println 'File:    ' + f
          println 'Object:  ' + f.xattr['net.filebot.metadata']
          println 'Media:   ' + any{ MediaInfo.snapshot(f) }{ null }
          if (f.metadata) {
              println ' Episode Metrics '.center(80, '-')
              EpisodeMetrics.defaultSequence(false).each{ m ->
                  println String.format('%-20s % 1.1f', m, m.getSimilarity(f, f.metadata))
              }
          }
      }
      return false
  }

  // process only media files (accept audio files only if music mode is enabled)
  return f.isVideo() || f.isSubtitle() || (music && f.isAudio())
}

// specify how to resolve input folders, e.g. grab files from all folders except disk folders and already processed folders (i.e. folders with movie/tvshow nfo files)
def resolveInput(f) {
  // resolve folder recursively, except disk folders
  if (f.isDirectory()) {
    if (f.isDisk()) {
      log.finest "Disk Folder: $f"
      return f
    }
    return f.listFiles { acceptFile(it) }.collect { resolveInput(it) }
  }

  if (f.isArchive() || f.hasExtension('001')) {
    return extract(f).findAll { acceptFile(it) }.collect { resolveInput(it) }
  }

  return f
}

// flatten nested file structure
def input = roots.findAll { acceptFile(it) }.flatten { resolveInput(it) }.toSorted()

// update exclude list with all input that will be processed during this run
if (excludeList && !testRun) {
  try {
    excludePathSet.append(excludeList, extractedArchives, input)
    } catch (e) {
    die "Failed to write excludes: $excludeList: $e"
  }
}

// print exclude and input sets for logging
input.each { f -> log.fine "Input: $f" }

// early abort if there is nothing to do
if (input.size() == 0) {
  die 'No files selected for processing', ExitCode.NOOP
}

String anime = ''
Boolean useDetectAnimeName = false

// TODO
// Subtitles for movies do not end up in the same group as the movie, actually external subtitles need work in general
LinkedHashMap groupGeneration( def input, Boolean useGroupByAutodection, Locale locale, LinkedHashMap  aniDBCompleteXMLList) {
  def groupsByManualThree = input.groupBy { f ->
    String order = 'Absolute'
    def airdateSeasonNumber = null
    Boolean isFileBotDetectedName = false
    Boolean hasSeasonality = false
    def seasonNumber = null
    Boolean isSpecialType = false
    Boolean isSpecialEpisode = false
    Boolean isMovieType = false
    def specialType = null
    Boolean hasOrdinalSeasonality = false
    def ordinalSeasonNumber = null
    Boolean hasPartialSeasonality = false
    def partialSeasonNumber = null
    Boolean hasSeriesSyntax = false
    def seriesNumber = null
    def yearDateInName = null
    def filebotMovieTitle = null
    def filebotTitle = null
    def altTitle = null
    useDetectAnimeName = false
    if (useGroupByAutodection) {
      return new AutoDetection([f] as Collection<File>, false, locale).group()
    }
    println "// ---------------- START -------------- //"
    println "//--- ${f.name}"
    String releaseGroup = detectAnimeReleaseGroupFromFile(f)
//    println "//--- Release Group:${releaseGroup}"
    // myDotNameMatcher = f.name =~ /^([\w\.-]*)$/
    myFileNameForParsing = regexRemoveKeywords(regexStep1(f.name))
    println "//--- myFileNameForParsing: ${myFileNameForParsing}"
    animeDetectedName = detectAnimeName(f)
    def animeParentFolder = f.getParentFile().getName()
    def animeRegexBlenderName = regexBlender(f.name)
    myDotNameMatcher = f.name =~ /^([a-zA-Z0-9\.-]*)$/
    if ( myDotNameMatcher.find() ) {
      println '//----- hasDotFileName = true'
      hasDotFileName = true
      useDetectAnimeName = true
    } else {
      // println '//--- Does not have dot filename'
      hasDotFileName = false
    }
    // If the filename starts with [ then it's likely from a release group and I can parse it (probably)
    myRegexMatcher = f.name =~ /^\[/
    if ( !myRegexMatcher.find() ) {
      // If it starts with a number .. I most likely can't parse it
      // VOID - myRegexMatcher = f.name =~ /^[0-9]/
      myRegexMatcher = f.name =~ /^([0-9]|#[0-9])/
      if ( myRegexMatcher.find() ) {
        println '//----- Filename starts with number'
        println '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
      //If it starts with TVDB Season Syntax SxxExx, I most likely can't parse it. or with Episode, Exx, epxx etc.
      // VOID - myRegexMatcher = f.name =~ /(?i)^\b((S\d{1,2})?(\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b))|^((ep\d{1,2})|episode[\s]?\d{1,2})/
      myRegexMatcher = f.name =~ /(?i)^\b((S\d{1,2})?(\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b))|^((ep-\d{1,2}|ep\d{1,2})|episode[\s]?\d{1,2}|S\d{1,2}\s)/
      if ( myRegexMatcher.find() ) {
        println '//----- Filename starts with TVDB Season Syntax'
        println '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
    }
    // Case where regexBlender doesn't work.. and you get Sxx or Sxx Something, both of which are unlikely to match anything
    myRegexMatcher = animeRegexBlenderName =~ /(?i)^([-\s]*S)([\d]{1,2})\b/
    if ( myRegexMatcher ) {
      println "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      println '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // Case where regexBlender doesn't work.. and you get empty/null (not sure how null, but empty can happen)
    if ( animeRegexBlenderName == null || animeRegexBlenderName == '') {
      println "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      println '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // println "animeParentFolder: ${animeParentFolder}"
    fileAge = 0
    // Check if the parent folder is "anime"
    // if ( animeParentFolder != 'anime' ) {
    // }
    // print xattr metadata
    if (f.metadata) {
      log.finest "--- xattr: [$f.name] => [$f.metadata]"
      if ( clearXattr ) {
        log.finest "Clearing file metadata- $f.name"
        tryQuietly { f.xattr.clear() }
      }
    }
    // MediaInfo related to "dates"
    //  --- Note it is not unusual to have 'invalid' (error kind) or 'invalid' (weird date kind) in Encoded_Date,
    //       File_Created_Date, File_Created_Date_Local, File_Modified_Date and File_Modified_Date_Local usually contain "good" data as those tend to be based on "local" data on the filesystem the file is currently on.
    // Property [Encoded_Date              ] : UTC 2020-09-08 16:26:49
    // Property [File_Created_Date         ] : UTC 2020-09-09 13:14:36.400
    // Property [File_Created_Date_Local   ] : 2020-09-09 08:14:36.400
    // Property [File_Modified_Date        ] : UTC 2020-09-08 17:31:48.000
    // Property [File_Modified_Date_Local  ] : 2020-09-08 12:31:48.000
    // Specific Releases/Patterns that don't play nice with other's.
    // This is also the wrong place to figure out an "age Filter" as we need to find the age from the group of files, not each individual file within a group (as they may vary in 'age')
    // if ( f.isVideo() ) {
    //   med = getMediaInfo(f, '{media.Encoded_Date}') // 2020-07-02 00:55:19.560 - Type String
    //   Date today = getNow()
    //   if ( med == null ) { med = 'UTC 2010-06-29 05:21:12' }
    //   def medFormatted = Date.parse('zzz yyyy-MM-dd HH:mm:ss', med)
    //   fileAge = daysBetween(medFormatted, today)
    //   println "${f} - med: ${med}, medFormatted: ${medFormatted}, today: ${today}, fileAge: ${fileAge}"
    // }
    // skip auto-detection if possible
    if ( useDetectAnimeName || useFBAutoDetection ) {
      println "--- Filebot filename evaluation"
      if ( animeDetectedName == null || animeDetectedName == '' || animeDetectedName == 'anime') {
        println "------ Filebot Returned name[${animeDetectedName}] not usable"
        // This means the autodetection effectively didn't work
        if (useDetectAnimeName) {
          // If this is set, then we know that RegexBlender on the filename does produce something usuable.
          anime = regexBlender(animeParentFolder)
          isFileBotDetectedName = false
          println "------ Using Parent Folder for Anime Name: ${anime}"
        } else {
          // If this is not set, then regexBlender *might* produce something useful.
          anime = animeRegexBlenderName
          isFileBotDetectedName = false
          println "------ Using regexBlender Anime Name: ${anime}"
        }
      } else {
        anime = animeDetectedName
        isFileBotDetectedName = true
        println "------ Using Filebot Anime Name: ${anime}"
      }
    } else {
      anime = animeRegexBlenderName
      isFileBotDetectedName = false
      println "------ Using regexBlender Anime Name: ${anime}"
    }
    // if ( useDetectAnimeName || useFBAutoDetection || hasDotFileName) {
    //     println "--- Filebot filename evaluation"
    //     anime = animeDetectedName
    //     isFileBotDetectedName = true
    //     if ( hasDotFileName ) {
    //       println "--- Dot Filename evaluation"
    //       anime = animeDetectedName
    //       isFileBotDetectedName = true
    //       if ( anime == null || anime == '' || anime == 'anime') {
    //         // This means the autodetection effectively didn't work, so some crap is still probably better then null
    //         anime = regexBlender(f.name)
    //         isFileBotDetectedName = false
    //         println "------ Filebot Returned name not usable"
    //         println "------ Using regexBlender Anime Name: ${anime}"
    //       } else {
    //         println "------ Using Filebot Anime Name: ${anime}"
    //       }
    //     } else {
    //       if ( anime == null || anime == '' || anime == 'anime') {
    //         // This means the autodetection returned null, so some crap is still probably better then null/anime
    //         def animeParentFolderName = regexBlender(animeParentFolder)
    //         anime = animeParentFolderName
    //         isFileBotDetectedName = false
    //         println "------ Filebot Returned name not usable"
    //         println "------ Using Parent Folder for Anime Name: ${anime}"
    //       } else {
    //         println "------ Using Filebot Anime Name: ${anime}"
    //       }
    //     }
    //   } else {
    //     anime = regexBlender(f.name)
    //     isFileBotDetectedName = false
    //     // Manual process failed, so try Filebot Autodection
    //     if ( anime == null || anime == '') {
    //       anime = animeDetectedName
    //       isFileBotDetectedName = true
    //       println "------ Using Filebot Anime Name: ${anime}"
    //     }
    //     // Case where regexBlender doesn't work.. and you get Sxx or Sxx Something, both of which are unlikely to match anything
    //     myRegexMatcher = anime ==~ /(?i)^([-\s]*S)([\d]{1,2})\b/
    //     if ( myRegexMatcher ) {
    //       anime = animeDetectedName
    //       isFileBotDetectedName = true
    //       println "------ Using Filebot Anime Name: ${anime}"
    //     }
    //     println "------ Using regexBlender Anime Name: ${anime}"
    // }
    // def animeEpisodeNumber = parseEpisodeNumber(f) // Only works when using SxE format
    // println "Detected Anime Episode Number: ${animeEpisodeNumber}"
    myRegexMatcher = f.name =~ /GattaiEarthGranner/
    if ( myRegexMatcher.find() ) {
      anime = 'Tomica Kizuna Gattai: Earth Granner'
      isFileBotDetectedName = false
      println "------ Forced Anime Name: ${anime}"
    }

    // -------------  Movies ---------------- //
    if (detectAnimeMovie(f)) {
      order = 'Absolute'
      isMovieType = true
      // ((^[a-z\s]+)(\d{4}))
      if ( hasDotFileName ) {
        println "-------- Anime Movie has dot filename pattern:[${f.name}]"
        myMovieRegexMatcher = myFileNameForParsing =~ /(?i)((^[a-z\s]+)(\d{4}))/
        if ( myMovieRegexMatcher.find() ) {
          println "---------- Anime Movie has known moviename <date> filename pattern"
          anime = myMovieRegexMatcher[0][2].replaceAll(/([\s-])*$/, '')
          yearDateInName = myMovieRegexMatcher[0][3]
          println "------------ Anime name is now ${anime}"
        } else {
          println "---------- We are not changing the name"
        }
      } else {
        // VOID myMovieRegexMatcher = myFileNameForParsing =~ /(?i)((^[a-z\s]+)\(?((19\d\d|20\d\d)\)?\s))/
        myMovieRegexMatcher = anime =~ /(?i)((^[a-z\s-]+)\(?((19\d\d|20\d\d)\)?\s))/
        if ( myMovieRegexMatcher.find() ) {
          println "---------- Anime Movie has known moviename (date) filename pattern"
          anime = myMovieRegexMatcher[0][2]
          yearDateInName = myMovieRegexMatcher[0][4]
          println "------------ Anime name is now ${anime}"
        }
      }
      println "-------- Anime Movie: ${anime}"
      filebotMovieTitle = detectMovie(f, false)
      println "-------- #1 - ${filebotMovieTitle}" // null
      println "-------- Minutes - ${getMediaInfo(f, '{minutes}').toInteger()}"
      // println "-------- #2 - ${detectMovie(f, true)}" // null
      // println "-------- #3 - ${MediaDetection.detectMovie(f, TheMovieDB, locale, true)}" // []
      // println "-------- #4 - ${MediaDetection.detectMovie(f, TheMovieDB, locale, false)}" // []
      // println "-------- #5 - ${animeDetectedName}" // Yu Yu Hakusho The Movie (when it's actually [Samir755] Yu Yu Hakusho - The Movie 2 - Meikai Shitou-Hen - Honoo No Kizuna.mkv )
    }
    if ( hasDotFileName && !isMovieType) {
      println "-------- Anime has dot filename pattern:[${f.name}]"
      myMovieRegexMatcher = myFileNameForParsing =~ /(?i)((^[a-z\s]+)(\d{4}))/
      if (myMovieRegexMatcher.find()) {
        println "---------- Anime has known moviename <date> filename pattern"
        anime = myMovieRegexMatcher[0][2].replaceAll(/([\s-])*$/, '')
        yearDateInName = myMovieRegexMatcher[0][3]
        println "------------ Anime name is now ${anime}"
      } else {
        println "---------- We are not changing the name"
      }
    }
    if (detectAirdateOrder(myFileNameForParsing) && !isMovieType) {
      println "-------- Anime: $anime has airdate order"
      order = 'airdate'
      // VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))
      // VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))
      // VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)
      myTVDBSeasonalityRegexMatcher = myFileNameForParsing =~ /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
      if ( myTVDBSeasonalityRegexMatcher.find() ) {
        airdateSeasonNumber = myTVDBSeasonalityRegexMatcher[0][2].replaceAll(/(S|s)/, '').toInteger()
      }
      if (!isFileBotDetectedName) {
        // Remove only the Season from the Filename if it's Airdate order.
        // VOID - (?i)([-\s]*S)([\d]{1,2})\b
        // VOID - (?i)\b([-\s]*S[\d]{1,2})|(\d{1,2}x\d{1,3}v\d{1,2}|\d{1,2}x\d{1,3})\b
        // VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?(?>[ExS]\d{1,3})[_]?(?>v\d{1,2})?)
        anime = anime.replaceAll(/(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/ , '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
        println "---------- Anime Name is now: $anime"
      }
    }

    // myDateRegexMatcher = myFileNameForParsing =~ /(?i)[\(|\s|\[]?([1|2][9|0]\d\d)[\)|\s|\]]?/ // Does it have a date in it?
    // myDateRegexMatcher = myFileNameForParsing =~ /(?i)[\(|\s|\[]?([1|2][9|0]\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
    // myDateRegexMatcher = myFileNameForParsing =~ /(?i)[\(|\s|\[]?((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
    myDateRegexMatcher = myFileNameForParsing =~ /(?i)[\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
    if ( myDateRegexMatcher.find() ) {
      yearDateInName = myDateRegexMatcher[0][1]
      myAnimeDateRegexMatcher = anime =~ /(?i)[\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
      if ( myAnimeDateRegexMatcher.find() ) {
        println "-------- myFileNameForParsing & anime both have the Date the name - ${anime} - ${yearDateInName} "
        if (!isFileBotDetectedName) {
          anime = anime.replaceAll(/(?i)[\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/, ' ').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
          println "-------- Anime name is now: $anime"
        }
      } else {
        println "-------- ONLY myFileNameForParsing have the date in the name:${myFileNameForParsing}:"
      }
    }

    // mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(?<!I|II|III|IV|V|VI|VII|VIII|IX)(\s[\d]{1,2}\s?$)/
    // mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,2}\s?$)/
    // mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}(v[\d]{1,2}|[a-zA-Z])?\s?)$/
    mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3})(v[\d]{1,2}|[a-zA-Z])?\s?$/
    // For text like:
    // anime nnn to end of line
    // anime nnnvnn to end of line
    if ( mySeasonalityRegexMatcher.find() && !isSpecialType) {
      println "-------- ${anime}: has Numerical series Syntax"
      // There is in fact at least ONE Anime where it is 02 vs 2 ..
      animeForSearching = [] as Set
      mySeasonalityNumber = mySeasonalityRegexMatcher[0][1].toInteger()
      animeForSearching += anime
      anime = anime.replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}(v[\d]{1,2}|[a-zA-Z])?\s?)$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      animeForSearching += anime + ' ' + mySeasonalityNumber
      // animeForSearching = anime + ' ' + mySeasonalityNumber // anime 2 (removes leading zero)
      // myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
      // myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
//      myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, animeForSearching as Set, locale, false, false, false, 1)
//      myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, animeForSearching as Set, locale, false, false, false, 1)
      myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, animeForSearching as Set, locale, false, false, false, 1)
//      if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
      if ( myGroup2AniDBOptions.isEmpty() ) {
        println "---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search: ${animeForSearching}"
        if ( mySeasonalityNumber >= 10 ) {
          println "---------- mySeasonalityNumber[${mySeasonalityNumber}] >= 10, will not Check Ordinal/Roman Syntax"
          hasSeriesSyntax = false
        } else {
          animeForSearching = []
          animeForSearching += anime + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
          animeForSearching += anime + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
          // animeForSearching = anime + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
          // myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
          // myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
//          myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, animeForSearching as Set, locale, false, false, false, 1)
//          myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, animeForSearching as Set, locale, false, false, false, 1)
          myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, animeForSearching as Set, locale, false, false, false, 1)
          // if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
          if ( myGroup2AniDBOptions.isEmpty() ) {
            // println "---------- TV Series not found in AniDB by AniDB XML Title Search: ${animeForSearching}"
            // animeForSearching = anime + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
            // myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
            // myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, ["${animeForSearching}"] as Set, locale, false, false, false, 1)
            // if ( myGroup2AniDBOptions.isEmpty() || myGroup2AniDBOptions2.isEmpty() ) {
            println "---------- TV Series not found in AniDB by AniDB XML Title Search: ${animeForSearching}"
            hasSeriesSyntax = false
            // } else {
            //   println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
            //   hasSeriesSyntax = true
            //   seriesNumber = mySeasonalityNumber
            //   println "---------- mySeriesNumber: ${seriesNumber}"
            // }
          } else {
//            println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
            println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
            hasSeriesSyntax = true
            seriesNumber = mySeasonalityNumber
            println "---------- mySeriesNumber: ${seriesNumber}"
          }
        }
      } else {
//        println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
        println "---------- TV Series found in AniDB by AniDB XML Title/Synonym Search: ${animeForSearching}"
        println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
        hasSeriesSyntax = true
        seriesNumber = mySeasonalityNumber
        println "---------- mySeriesNumber: ${seriesNumber}"
      }
      println "---------- Anime Name is now: $anime"
    }

    // mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(?<!I|II|III|IV|V|VI|VII|VIII|IX)(1st$|2nd$|3rd$|\dth$)/
    mySeasonalityRegexMatcher = anime =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s(1st|2nd|3rd|\dth|I|II|III|IV|V|VI|VII|VIII|IX))$/
    // for text
    // Anime II
    // Anime 2nd
    if ( mySeasonalityRegexMatcher.find() && !isSpecialType) {
      println "-------- ${anime}: has Ordinal/Roman series Syntax"
      hasSeriesSyntax = true
      mySeasonalityOrdinalMatcher = mySeasonalityRegexMatcher[0][1] =~ /(?i)(\d)(st|nd|rd|th)/
      if ( mySeasonalityOrdinalMatcher.find() )  {
        mySeasonalityNumber = mySeasonalityOrdinalMatcher[0][1].toInteger()
      } else {
        mySeasonalityNumber = mySeasonalityRegexMatcher[0][1].replaceAll(/\s/, '')
      }
      seriesNumber = mySeasonalityNumber
      anime = anime.replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s(1st|2nd|3rd|\dth|I|II|III|IV|V|VI|VII|VIII|IX))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "---------- mySeriesNumber: ${seriesNumber}"
      println "---------- Anime Name is now: $anime"
    }


    myOrdinalSeasonalityMatcher = anime =~ /(?i)(\s+(first|second|third|fourth|fith|sixth|seventh|eighth|ninth|tenth)\s+(Season|part))/ // first Season, second Season, third season etc.
    if ( myOrdinalSeasonalityMatcher.find() ) {
      println "--------${anime} name has word Seasonality (ugh)"
      def wordSeasonNumberTEMP = myOrdinalSeasonalityMatcher[0][2]
      hasOrdinalSeasonality = true
      ordinalSeasonNumber = getWordNumber(wordSeasonNumberTEMP)
      println "---------- ordinalSeasonNumber: ${ordinalSeasonNumber}"
      anime = anime.replaceAll(/(?i)(\s+(first|second|third|fourth|fith|sixth|seventh|eighth|ninth|tenth)\s+(Season|part))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "---------- Anime Name is now: $anime"
    }


    // void - myOrdinalSeasonalityMatcher = anime =~ /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)|\s+(part|season)\s*([\d]+))/ // 2nd Season, 3rd Season, Part 1, Part 2, 2nd part, Season 2 etc.
    // void - myOrdinalSeasonalityMatcher = anime =~ /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)|\s+(part)\s*([\d]+))/ // 2nd Season, 3rd Season, Part 1, Part 2, 2nd part, Season 2 etc.
    myOrdinalSeasonalityMatcher = anime =~ /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+))/ // 2nd Season, 3rd Season, Part 1, Part 2, 2nd part, Season 2 etc.
    if ( myOrdinalSeasonalityMatcher.find() ) {
      println "--------${anime} name has Ordinal and/or Partial/TVDB Seasonality"
      def partialSeasonNumberTEMP = tryQuietly { myOrdinalSeasonalityMatcher[1][6] } // It would be [0][6] if it was ONLY Partial..
      def myOrdinalNumberTEMP = myOrdinalSeasonalityMatcher[0][6]
      if ( myOrdinalNumberTEMP == null ) {
        hasOrdinalSeasonality = true
        myOrdinalNumber = myOrdinalSeasonalityMatcher[0][2]
        ordinalSeasonNumber = myOrdinalNumber.toInteger()
        println "---------- ordinalSeasonNumber: ${ordinalSeasonNumber}"
      } else {
        hasPartialSeasonality = true
        partialSeasonNumber = myOrdinalNumberTEMP.toInteger()
        println "---------- Partial seasonNumber:: ${partialSeasonNumber}"
      }
      if ( partialSeasonNumberTEMP != null ) {
        hasPartialSeasonality = true
        partialSeasonNumber = partialSeasonNumberTEMP.toInteger()
        println "---------- Partial seasonNumber: ${partialSeasonNumber}"
      }
//      myOrdinalNumber = myOrdinalSeasonalityMatcher[0][6]
////      println "---------- myOrdinalNumber: ${myOrdinalNumber}"
//      if ( myOrdinalNumber == null ) {
//        myOrdinalNumber = myOrdinalSeasonalityMatcher[0][2]
//        hasOrdinalSeasonality = true
//        ordinalSeasonNumber = myOrdinalNumber.toInteger()
//        println "---------- ordinalSeasonNumber: ${ordinalSeasonNumber}"
//      } else {
//        hasPartialSeasonality = true
//        partialSeasonNumber = myOrdinalNumber.toInteger()
//        println "---------- seasonNumber: ${partialSeasonNumber}"
//      }
      anime = anime.replaceAll(/(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "---------- Anime Name is now: $anime"
    }

    myTVDBSeasonalityRegexMatcher = anime =~ /(?i)(\s+(season)\s*([\d]+))/ // Season xx
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      println "-------- ANIME:${anime}: name has Seasonality (Season xx)"
      myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][3].toInteger()
      mySeasonalityNumber = myTVDBSeasonNumber
      hasSeasonality = true
      seasonNumber = mySeasonalityNumber
      anime = anime.replaceAll(/(?i)(\s+(season)\s*([\d]+))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "---------- Anime Name is now: $anime"
    }

    // VOID - myTVDBSeasonalityRegexMatcher = myFileNameForParsing =~ /(?i)([\s(]+(season)\s*([\d]+))\)?/ // Season xx or (Season xx)
    myTVDBSeasonalityRegexMatcher = myFileNameForParsing =~ /(?i)([\s(]+(?<!\s\d[a-z]{2}\s)(season)\s*([\d]+))\)?/ // Season xx or (Season xx)
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      println "-------- FILENAME:${myFileNameForParsing}: name has Seasonality (Season xx)"
      myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][3].toInteger()
      mySeasonalityNumber = myTVDBSeasonNumber
      hasSeasonality = true
      seasonNumber = mySeasonalityNumber
    }

    myTVDBSeasonalityRegexMatcher = anime =~ /(?i)([-\s]*S)([\d]{1,2})\b/ // Matches S0, S1, S12 as well as preceeding - and spaces (unlimited number of them)
    if ( myTVDBSeasonalityRegexMatcher.find() && !isMovieType) {
      println "-------- ${anime}: name has Seasonality (Sx)"
      myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][2].toInteger()
      mySeasonalityNumber = myTVDBSeasonNumber
      hasSeasonality = true
      seasonNumber = mySeasonalityNumber
      println "---------- mySeasonalityNumber: ${mySeasonalityNumber}"
      // VOID - myTVDBSeasonalityRegexMatcher = anime =~ /(?i)([-\s]*(?<!^)S)([\d]{1,2})\b$/ // Verify Sxx is at the end of the string, if not then we probably need to prune some info.
      myTVDBSeasonalityRegexMatcher = anime =~ /(?i)([-\s]*S)([\d]{1,2})\b(?!$)/
      if ( myTVDBSeasonalityRegexMatcher.find() ) {
        println "---------- ${anime} needs needs TVDB Seasonality pruning"
        println "---------- mySeasonalityNumber: ${mySeasonalityNumber}"
        // // myRegexMatcher = anime =~ /(?i)([-\s]*(?<!^)S[\d]{1,2})\b(.*)$/
        // // if ( myRegexMatcher.find() ) {
        // //   mySeason = myRegexMatcher[0][1]
        // //   myExtraTxt = myRegexMatcher[0][2]
        // // }
        tempAnimeName = anime.replaceFirst(/(?i)([-\s]*(?<!^)S[\d]{1,2})\b(.*)$/, '')
        // // myRegexMatcher = myExtraTxt =~ /(?i)([\(]?\d\d\d\d[\)]?)/
        // // if ( myRegexMatcher.find() ) {
        // //   // Add the date back in
        // //   tempAnimeName = tempAnimeName + ' ' + myRegexMatcher[0][1]
        // // }
        // // Add the TVDB Seasonality back in (for now)
        // // tempAnimeName = tempAnimeName + ' ' + mySeason
        // // println "--- tempAnimeName: ${tempAnimeName.replaceAll(/(\s){2,20}/, ' ')}, mySeason: ${mySeason}, myExtraTxt: ${myExtraTxt}"
        anime = tempAnimeName.replaceAll(/(\s){2,20}/, ' ')
      } else {
        anime = anime.replaceAll(/(?i)([-\s]*S)([\d]{1,2})\b/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      }
      println "---------- Anime Name is now: $anime"
    }

    // myOVARegexMatcher = myFileNameForParsing =~ /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/ // OVA, ONA or Special and all spaces, dashes etc around them. It also requires at least one around the word.
    // myOVARegexMatcher = myFileNameForParsing =~ /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bsp\d{1,2})(\b\d)?)[-\s\)]?/ // OVA, ONA or Special and all spaces, dashes etc around them. It also requires at least one around the word.
    // if ( myOVARegexMatcher.find() ) {
    //   // println "-------- ${myFileNameForParsing} has OVA/ONA/OAD/Special Syntax"
    //   isSpecial = true
    //   specialType = myOVARegexMatcher[0][2]
    //   isMovieType = false // While OVA/ONA/OAD are treated more like movies then series in AniDB, we will treat them like episodes then movies for lookup/renaming
    // }
    // VOID - (?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?
    myOVARegexMatcher = myFileNameForParsing =~ /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/ // OVA, ONA or Special and all spaces, dashes etc around them. It also requires at least one around the word.
    if ( myOVARegexMatcher.find() ) {
      println "-------- ${anime} has OVA/ONA/OAD/Special/Bonus Syntax"
      if ( isMovieType ) {
        altTitle = detectMovie(f, false)
        println "---------- And Detected as Anime Movie: ${altTitle}"
        isMovieType = false
        // println "-------- #1 - ${detectMovie(f, false)}" // null
        // println "-------- #2 - ${detectMovie(f, true)}" // null
        // println "-------- #3 - ${MediaDetection.detectMovie(f, TheMovieDB, locale, true)}" // []
        // println "-------- #4 - ${MediaDetection.detectMovie(f, TheMovieDB, locale, false)}" // []
        // println "-------- #5 - ${animeDetectedName}" // Yu Yu Hakusho The Movie (when it's actually [Samir755] Yu Yu Hakusho - The Movie 2 - Meikai Shitou-Hen - Honoo No Kizuna.mkv )
      }
      specialType = myOVARegexMatcher[0][2].replaceAll(/[0-9]/, '')
      switch(specialType) {
        case ~/(?i)special/:
          isSpecialEpisode = true
          println "---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
          break
        case ~/(?i)bonus/:
          isSpecialEpisode = true
          println "---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
          break
        case ~/(?i)sp/:
          isSpecialEpisode = true
          println "---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
          break
        default:
          isSpecialType = true
          println "---------- myOVAType[${specialType}] Detected. Pruning Anime Name"
          // anime = anime.replaceAll(/(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?).*?$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '') // Basically remove - ova*
          // It's a little silly, but when the OVA/ONA etc is in the middle of the string, what follows is often useful to locate the OVA/ONA etc.
          // anime = anime.replaceAll(/(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\s\d)?)[-\s\)]?/, ' - ').replaceAll(/\s-\s$/, '').replaceAll(/(\s){2,20}/, ' ')
          anime = anime.replaceAll(/(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\s?\d)?)[-\s\)]?/, ' - ').replaceAll(/\s-\s$/, '').replaceAll(/(\s){2,20}/, ' ')
          println "---------- New Anime name is ${anime}"
          break
      }
    }

    // VOID - (?i)(-\s(.*))$
    mySanityRegexMatcher = anime =~ /(?i)(-\s([^-]*))$/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '')
      println "-------- [${anime}] has possible additional text to remove: [${mySanityAltTxt}] using -"
      Set searchList = ["${anime}"]
      searchList += ["${returnAniDBRomanization(anime)}"]
      animeTemp = anime.replaceAll(/(?i)(-\s([^-]*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
//      myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 2)
//      myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 2)
      myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
      if ( myGroup2AniDBOptions.isEmpty() ) {
//      if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
        println "---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search:[${anime}]"
        anime = animeTemp
        println "---------- New Anime name is ${anime}"
        println "------------ Checking if [${mySanityAltTxt}] is Alternative Title"
        searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
//        myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 1)
//        myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 1)
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
//        if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
          println "-------------- Does not seem so"
        } else {
//          println "-------------- AniDB Returned results: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
          println "-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          println "-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
        }
      } else {
//        println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
        println "---------- TV Series found in AniDB by AniDB XML Title/Synonym Search:[${anime}]"
        println "------------ Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
        println '------------ We are not changing the Anime name'
      }
    }

    // VOID - (?i)(~\s(.*))$
    mySanityRegexMatcher = anime =~ /(?i)(~(.*))$/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '')
      println "-------- [${anime}] has possible additional text or Alternative Title: [${mySanityAltTxt}] using ~"
      Set searchList = ["${anime}"]
      searchList += ["${returnAniDBRomanization(anime)}"]
      animeTemp = anime.replaceAll(/(?i)(~(.*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
//      myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 2)
//      myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 2)
      myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
      if ( myGroup2AniDBOptions.isEmpty() ) {
//      if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
        println "---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search: ${anime}"
        anime = animeTemp
        println "---------- New Anime name is ${anime}"
        println "------------ Checking if [${mySanityAltTxt}] is Alternative Title"
        searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
//        myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 1)
//        myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 1)
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
//        if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
          println "-------------- Does not seem so"
        } else {
//          println "-------------- AniDB Returned results: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
          println "-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          println "-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
        }
      } else {
//        println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
        println "---------- TV Series found in AniDB by AniDB XML Title/Synonym Search: ${anime}"
        println "------------ Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
        println '------------ We are not changing the Anime name'
      }
    }

    // VOID - (?i)(\(([^)]*)\))
    // VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))
    // VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))(?!.\w\w\w)
    // VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))(?!\.\w\w\w)
    mySanityRegexMatcher = myFileNameForParsing =~ /(?i)(?<!^)(\(((?!((19|20)\d\d))[^)]*)\))(?!\.\w\w\w)/
    if (mySanityRegexMatcher.find()) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '')
      if ( mySanityAltTxt.size() >= 3) {
        if ( detectAnimeReleaseGroupFromFile(f) == "ASW" && mySanityAltTxt == "The Adventure of Dai") {
          println "-------- [${anime}] has malformed title [ASW] - Should be: Dragon Quest: The Adventure of Dai"
          anime = "Dragon Quest: The Adventure of Dai"
          println "------------ New Anime name is ${anime}"
        } else {
          println "-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using ()"
          Set searchList = ["${mySanityAltTxt}"]
          searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
//          myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 2)
//          myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 2)
          myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
          if ( myGroup2AniDBOptions.isEmpty() ) {
//          if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
            println "---------- Alternative Title not found in AniDB by AniDB XML Title/Synonym Search: ${mySanityAltTxt}"
            animeTemp=anime + " " + mySanityAltTxt
            searchList = ["${animeTemp}"]
            println "---------- Insanity Check for Groups that use () for title subtext aka [anime: subtext] - ${animeTemp}"
            searchList += ["${returnAniDBRomanization(animeTemp)}"]
//            myGroup2AniDBOptions = anidbXMLTitleSearch(aniDBTitleXML, searchList, locale, false, false, false, 1)
//            myGroup2AniDBOptions2 = anidbXMLTitleSearch(aniDBSynonymXML, searchList, locale, false, false, false, 1)
            myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
//            if ( myGroup2AniDBOptions.isEmpty() && myGroup2AniDBOptions2.isEmpty() ) {
            if ( myGroup2AniDBOptions.isEmpty() ) {
              println "------------ Instanity Check failed :)"
            } else {
              println "------------ Insanity Found"
              anime = "${anime} "+"${mySanityAltTxt}"
              println "------------ New Anime name is ${anime}"
            }
          } else {
//            println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}:${myGroup2AniDBOptions2}"
            println "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
            println "-------------- Setting altTitle:[${mySanityAltTxt}]"
            altTitle = mySanityAltTxt
          }
        }
      }
    }
    // Not a good solution, but I don't have any better ideas at the moment

    if ( altjwdStringBlender(anime) == altjwdStringBlender('dragon quest dai no daibouken') && releaseGroup =~ /ASW|DKB/) {
      baseAnimeName = 'dragon quest dai no daibouken 2020'
      println "--------------  Dragon Quest 2020 Fix: Setting title to ${baseAnimeName}"
    }

    // return [anime: anime.toLowerCase(), altTitle: altTitle, filebotMovieTitle: filebotMovieTitle, order: order, airdateSeasonNumber: airdateSeasonNumber, mov: mov, isFileBotDetectedName: isFileBotDetectedName, hasSeriesSyntax: hasSeriesSyntax, seriesNumber: seriesNumber, hasSeasonality: hasSeasonality, seasonNumber: seasonNumber, hasOrdinalSeasonality: hasOrdinalSeasonality, ordinalSeasonNumber: ordinalSeasonNumber, hasPartialSeasonality: hasPartialSeasonality, partialSeasonNumber: partialSeasonNumber, isSpecial: isSpecial, specialType: specialType, yearDateInName: yearDateInName, releaseGroup: releaseGroup]
    return [anime: anime.toLowerCase(), altTitle: altTitle, filebotMovieTitle: filebotMovieTitle, order: order, airdateSeasonNumber: airdateSeasonNumber, isMovieType: isMovieType, isFileBotDetectedName: isFileBotDetectedName, hasSeriesSyntax: hasSeriesSyntax, seriesNumber: seriesNumber, hasSeasonality: hasSeasonality, seasonNumber: seasonNumber, hasOrdinalSeasonality: hasOrdinalSeasonality, ordinalSeasonNumber: ordinalSeasonNumber, hasPartialSeasonality: hasPartialSeasonality, partialSeasonNumber: partialSeasonNumber, isSpecialType: isSpecialType, specialType: specialType, yearDateInName: yearDateInName, isSpecialEpisode: isSpecialEpisode]
  }
  return groupsByManualThree
}


LinkedHashMap aniDBCompleteXMLList = loadAniDBOfflineXML(aniDBTitleXMLFilename, aniDBSynonymXMLFilename)
def groupsByManualThree = groupGeneration(input, useGroupByAutodection, locale, aniDBCompleteXMLList)
def groupsByManualThreeMovies = groupsByManualThree.findAll { it.key.isMovieType == true}
def groupsByManualThreeEpisodes = groupsByManualThree.findAll { it.key.isMovieType == false}

println ''
println ''
println '***********************************'
println '***           Episodes            ***'
println '***********************************'
println ''
println ''
groupsByManualThreeEpisodes.each { group, files ->
  log.finest "${groupInfoGenerator(group)}"
}
println ''
println ''
println '***********************************'
println '***           Movies            ***'
println '***********************************'
println ''
println ''
// println "groupsByManualThreeMovies:${groupsByManualThreeMovies.getClass()}"
groupsByManualThreeMovies.each { group, files ->
  log.finest "${groupInfoGenerator(group)}"
}
println ''

if (breakAfterGroups) {
  die "breakAfterGroups Enabled"
}

// keep track of files that have been processed successfully
destinationFiles = []
destinationFilesFilebot = []
destinationFilesScript = []
failedFilesFilebot = []
failedFilesScript = []
renameMissedFiles1stPass = []
renameMissedFiles2ndPass = []

// keep track of unsorted files or files that could not be processed for some reason
unsortedFiles = []
partialFiles = []

/**
 * Wrapper for Filebot rename
 *
 * @param group Group info for the files we are renaming
 * @param files  The files we are renaming
 * @param renameStrict  Rename Strict option, true or false?
 * @param renameQuery The Rename Query
 * @param renameDB The Rename Database
 * @param renameOrder The Episode Order we will use
 * @param renameFilter The Filter we will use when renaming
 * @param renameMapper The Mapper we will use when renaming
 * @return generally nothing, but using void would occasionally cause the script to error out.
 */
def renameWrapper(LinkedHashMap group, def files, LinkedHashMap renameOptions) {
  rfsPartial = false
  rfsPartialFiles = []
  rfs = []
  wrapperArgs = [:]
  wrapperArgs.put('file', files)
  // Our "Default" format is animeFormat so set that, and then change it for other situations.
  wrapperArgs.put('format', animeFormat)
  if (group.isSpecialType || renameOptions.isSpecialType) {
    wrapperArgs.put('format', specialTypeFormat)
  }
  if (group.isSpecialEpisode || renameOptions.isSpecialEpisode) {
    wrapperArgs.put('format', specialFormat)
  }
  // Filebot doesn't support Movies with TheTVDB.. if it renames with TheTVDB then it's
  // Either an incorrect match OR the AniDB Entry is a movie, but the "TV" episodes are for some reason
  // included in the "movie" entry on AniDB, usually as kind "other" .. Which Filebot doesn't support :)
  if ((group.isMovieType || renameOptions.isMovieType) && renameOptions.renameDB =~ /(?i)AniDB/) {
    wrapperArgs.put('format', movieFormat)
  }
  wrapperArgs.put('strict', renameOptions.renameStrict)
  if (renameOptions.renameQuery) {
    wrapperArgs.put('query', "${renameOptions.renameQuery}")
  }
  if (renameOptions.renameDB) {
    wrapperArgs.put('db', renameOptions.renameDB)
  }
  if (renameOptions.renameOrder) {
    wrapperArgs.put('order', "${renameOptions.renameOrder}")
  }
  if (renameOptions.renameFilter) {
    wrapperArgs.put('filter', "${renameOptions.renameFilter}")
  }
  if (renameOptions.renameMapper) {
    wrapperArgs.put('mapper', "${renameOptions.renameMapper}")
  }
  // println "Running: Rename"
  // println "rename - ${[*:wrapperArgs]}"
  println '// ---------- RENAME: 1st Run ---------- //'
  if ( useIndividualFileRenaming ) {
    files.each { fileToRename ->
      rftsTemp = []
      wrapperArgs.file = fileToRename
      try {
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      } catch (Exception IllegalStateException) {
        println 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
    }
  } else {
    try {
      rfs = rename(*:wrapperArgs)
    } catch (Exception IllegalStateException) {
      println 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
      aniDBBanHammer = true as Boolean
      rfsIncomplete = false as Boolean
      rfs = []
    }
  }
  // println "RFS is class: ${rfs.getClass()}"
  if (rfs) {
    println "--- Successfully Renamed files - ${rfs}"
    switch(renamerSource) {
      case ~/filebot/:
        destinationFilesFilebot += rfs
        break
      case ~/script/:
        destinationFilesScript += rfs
        break
    }
    destinationFiles += rfs
    if ( rfs.size() == files.size() ) {
      println "----- Renamed all ${rfs.size()} files out of ${files.size()}"
      rfsIncomplete = false as Boolean
    } else {
      println "--- Renamed ${rfs.size()} files out of ${files.size()}"
      rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles1stPass += rfsLeftOver
      println "----- Leaving ${rfsLeftOver}"
      rfsPartial = true
    }
  } else if (failOnError && rfs == null) {
    println '*****************************'
    println '***  FAILURE! FAILURE!    ***'
    println '*****************************'
    die "Failed to process group: $group"
  } else {
    rfsIncomplete = true  as Boolean
    // TODO
    // Collecting stats on failure here is not useful as these files *could* get renamed in a different stage
    switch(renamerSource) {
      case ~/filebot/:
        failedFilesFilebot += files
        break
      case ~/script/:
        failedFilesScript += files
        break
    }
  }
  // ------ Rename again if there are Leftover files from the First rename attempt              ------- //
  // ------ try to rename each file individually unless useNonStrictPartialRenames is set       ------- //
  // ------ This sometimes overcomes the behavior that evaluating multiple files results        ------- //
  // ------ In no matches, while evaluating a file singularly will result in a match (strictly) ------- //
  // ------ Non-Strict usually doesn't have this issue, but does increase the probability of    ------- //
  // ------ Incorrect matches
  if ( rfsPartial && rfsLeftOver ) {
    println '// ---------- RENAME: 2nd Run ---------- //'
    println "--- 2nd Attempt to rename files missed during the first rename - ${rfsLeftOver}"
    if ( useNonStrictPartialRenames ) {
      println "--- Enabling Non-Strict 2nd Pass"
      wrapperArgs.strict = false
      wrapperArgs.file = rfsLeftOver
      rfs = rename(*:wrapperArgs)
    } else {
      rfs = []
      rfsLeftOver.each { fileToRename ->
        rfsTemp = []
        wrapperArgs.file = fileToRename
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      }
    }
    if (rfs) {
      println '--- Successfully Renamed files'
      destinationFiles += rfs
      switch(renamerSource) {
        case ~/filebot/:
          destinationFilesFilebot += rfs
          break
        case ~/script/:
          destinationFilesScript += rfs
          break
      }
      if ( rfs.size() == rfsLeftOver.size() ) {
        println "----- Renamed all ${rfs.size()} files out of ${files.size()}"
        rfsIncomplete = false  as Boolean
        rfsLeftOver = []
      } else {
        println "--- Renamed ${rfs.size()} files out of ${rfsLeftOver.size()}"
        rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
        renameMissedFiles2ndPass += rfsLeftOver
        println "----- Leaving ${rfsLeftOver}"
        rfsIncomplete = false  as Boolean
      }
    } else if (failOnError && rfs == null) {
      println '*****************************'
      println '***  FAILURE! FAILURE!    ***'
      println '*****************************'
      die "Failed to process group: $group"
    } else {
      println "--- Failed to rename any more files from group: $group"
      rfsIncomplete = false  as Boolean
      rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles2ndPass += rfsLeftOver
      switch(renamerSource) {
        case ~/filebot/:
          failedFilesFilebot += renameMissedFiles2ndPass
          break
        case ~/script/:
          failedFilesScript += renameMissedFiles2ndPass
          break
      }
    }
  }
}
def renameMovieWrapper(LinkedHashMap group, def files, Boolean renameStrict, def renameQuery = false, def renameDB = false, def renameOrder = false, def renameFilter = false, def renameMapper = false) {
  rfsPartial = false
  rfsPartialFiles = []
  rfs = []
  wrapperArgs = [:]
  wrapperArgs.put('file', files)
  // Our "Default" format is animeFormat so set that, and then change it for other situations.
  wrapperArgs.put('format', animeFormat)
  if (group.isSpecialType) {
    wrapperArgs.put('format', specialTypeFormat)
  }
  if (group.isSpecialEpisode) {
    wrapperArgs.put('format', specialFormat)
  }
  // Filebot doesn't support Movies with TheTVDB.. if it renames with TheTVDB then it's
  // Either an incorrect match OR the AniDB Entry is a movie, but the "TV" episodes are for some reason
  // included in the "movie" entry on AniDB, usually as kind "other" .. Which Filebot doesn't support :)
  if (group.isMovieType && renameDB =~ /(?i)AniDB/) {
    wrapperArgs.put('format', movieFormat)
  }
  wrapperArgs.put('strict', renameStrict)
  if (renameQuery) {
    wrapperArgs.put('query', "${renameQuery}")
  }
  if (renameDB) {
    wrapperArgs.put('db', renameDB)
  }
  if (renameOrder) {
    wrapperArgs.put('order', "${renameOrder}")
  }
  if (renameFilter) {
    wrapperArgs.put('filter', "${renameFilter}")
  }
  if (renameMapper) {
    wrapperArgs.put('mapper', "${renameMapper}")
  }
  // println "Running: Rename"
  // println "rename - ${[*:wrapperArgs]}"
  println '// ---------- RENAME: 1st Run ---------- //'
  if ( useIndividualFileRenaming ) {
    files.each { fileToRename ->
      rftsTemp = []
      wrapperArgs.file = fileToRename
      try {
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      } catch (Exception IllegalStateException) {
        println 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
    }
  } else {
      try {
        rfs = rename(*:wrapperArgs)
      } catch (Exception IllegalStateException) {
        println 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
  }
  // println "RFS is class: ${rfs.getClass()}"
  if (rfs) {
    println "--- Successfully Renamed files - ${rfs}"
    switch(renamerSource) {
      case ~/filebot/:
        destinationFilesFilebot += rfs
        break
      case ~/script/:
        destinationFilesScript += rfs
        break
    }
    destinationFiles += rfs
    if ( rfs.size() == files.size() ) {
      println "----- Renamed all ${rfs.size()} files out of ${files.size()}"
      rfsIncomplete = false as Boolean
    } else {
      println "--- Renamed ${rfs.size()} files out of ${files.size()}"
      rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles1stPass += rfsLeftOver
      println "----- Leaving ${rfsLeftOver}"
      rfsPartial = true
    }
  } else if (failOnError && rfs == null) {
    println '*****************************'
    println '***  FAILURE! FAILURE!    ***'
    println '*****************************'
    die "Failed to process group: $group"
  } else {
    rfsIncomplete = true  as Boolean
    // TODO
    // Collecting stats on failure here is not useful as these files *could* get renamed in a different stage
    switch(renamerSource) {
      case ~/filebot/:
        failedFilesFilebot += files
        break
      case ~/script/:
        failedFilesScript += files
        break
    }
  }
  // ------ Rename again if there are Leftover files from the First rename attempt              ------- //
  // ------ try to rename each file individually unless useNonStrictPartialRenames is set       ------- //
  // ------ This sometimes overcomes the behavior that evaluating multiple files results        ------- //
  // ------ In no matches, while evaluating a file singularly will result in a match (strictly) ------- //
  // ------ Non-Strict usually doesn't have this issue, but does increase the probability of    ------- //
  // ------ Incorrect matches
  if ( rfsPartial && rfsLeftOver ) {
    println '// ---------- RENAME: 2nd Run ---------- //'
    println "--- 2nd Attempt to rename files missed during the first rename - ${rfsLeftOver}"
    if ( useNonStrictPartialRenames ) {
      println "--- Enabling Non-Strict 2nd Pass"
      wrapperArgs.strict = false
      wrapperArgs.file = rfsLeftOver
      rfs = rename(*:wrapperArgs)
    } else {
      rfs = []
      rfsLeftOver.each { fileToRename ->
        rfsTemp = []
        wrapperArgs.file = fileToRename
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      }
    }
    if (rfs) {
      println '--- Successfully Renamed files'
      destinationFiles += rfs
      switch(renamerSource) {
        case ~/filebot/:
          destinationFilesFilebot += rfs
          break
        case ~/script/:
          destinationFilesScript += rfs
          break
      }
      if ( rfs.size() == rfsLeftOver.size() ) {
        println "----- Renamed all ${rfs.size()} files out of ${files.size()}"
        rfsIncomplete = false  as Boolean
        rfsLeftOver = []
      } else {
        println "--- Renamed ${rfs.size()} files out of ${rfsLeftOver.size()}"
        rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
        renameMissedFiles2ndPass += rfsLeftOver
        println "----- Leaving ${rfsLeftOver}"
        rfsIncomplete = false  as Boolean
      }
    } else if (failOnError && rfs == null) {
      println '*****************************'
      println '***  FAILURE! FAILURE!    ***'
      println '*****************************'
      die "Failed to process group: $group"
    } else {
      println "--- Failed to rename any more files from group: $group"
      rfsIncomplete = false  as Boolean
      rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles2ndPass += rfsLeftOver
      switch(renamerSource) {
        case ~/filebot/:
          failedFilesFilebot += renameMissedFiles2ndPass
          break
        case ~/script/:
          failedFilesScript += renameMissedFiles2ndPass
          break
      }
    }
  }
}

// Class/Global Variables Changed/Referenced
// hasSeasonality
// mySeasonalityNumber
// tier1AnimeNames
// tier2AnimeNames
// tier3AnimeNames
// statsTierFilebotNameNull
// statsTierFilebotNameIncluded
// statsGroupsFromFilebot
// statsGroupsFromScript
// statsTier3FilebotNameAdded
// animeDetectedName
ArrayList seriesnameGenerator ( LinkedHashMap group, HashSet baseGeneratedAnimeNames ) {
  println '// START---------- Series Name Generation ---------- //'
  Boolean addGroupAnimeNameToList = true
   baseGeneratedAnimeNames.each { basename ->
     println "//--- Generating Possible Anime Series Names for ${basename}"
//     log.finest "${groupInfoGenerator(group)}"
//     log.finest "${group}"
     // ---------- Does it have the Year?  ---------- //
     if (group.yearDateInName != null) {
       generatedAnimeName = basename + ' ' + group.yearDateInName
       tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
       println "------- Tier1: Adding ${generatedAnimeName}"
       if (group.hasSeriesSyntax && !useBaseAnimeNameWithSeriesSyntax) {
         addGroupAnimeNameToList = false
       }
       switch (group.specialType) {
         case ~/(?i)(OVA|ONA|OAD)/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- AirDate Syntax  ---------- //
     if ( (group.airdateSeasonNumber != null || group.hasSeasonality)  && !group.hasOrdinalSeasonality ) {
       if ( group.airdateSeasonNumber != null) {
         println '----- Airdate Syntax Detected'
         mySeasonalityNumber = group.airdateSeasonNumber.toInteger()
         hasSeasonality = true
       } else {
         println '----- Seasonality Syntax Detected'
         hasSeasonality = true
         mySeasonalityNumber = group.seasonNumber.toInteger()
       }
       println "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         println "------- Tier1: Adding ${basename} - Season is 0/1"
         tier1AnimeNames += ["${basename}"]
       } else {
         generatedSesonalAnimeNames = [] as HashSet
         addGroupAnimeNameToList = false
         println "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         println "------- Seasonal: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         println "------- Seasonal: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         println "------- Seasonal: Adding ${generatedAnimeName} - Ordinal Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           println "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
           println "------- Tier1: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           println "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         println "------- Seasonal: Adding ${generatedAnimeName} - Seasonality Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         println "------- Seasonal: Adding ${generatedAnimeName} - Series Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         println "------- Seasonal: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         switch (group.specialType) {
           case ~/(?i)(OVA|ONA|OAD)/:
             // ---------- Add Full list of alternative Season names as options ending with SpecialType (ugh)---------- //
             generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
               println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime II
               println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
               println "------- Tier1: [${group.specialType}]:: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         if ( group.hasPartialSeasonality ) {
           println "------- AND Partial Seasonality Detected (Ugh)"
           hasSeasonality = true
           mySeasonalityNumber = group.partialSeasonNumber.toInteger()
           println "------- mySeasonalityNumber: ${mySeasonalityNumber}"
           if (mySeasonalityNumber > 1) {
             generatedSesonalAnimeNames.each { ordinalAnimeName ->
               // ---------- Add Full list of alternative Season names as options ---------- //
               generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber // anime part 2
               println "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
               println "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
               println "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               if ( mySeasonalityNumber < 10 ) {
                 generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
                 println "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
                 println "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
               //---
               generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber // anime season 2
               println "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber // anime 2
               println "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber // anime s2
               println "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               switch (group.specialType) {
                 case ~/(?i)(OVA|ONA|OAD)/:
                   // ---------- Add Full list of alternative Season names as options ---------- //
                   generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   if ( mySeasonalityNumber < 10 ) {
                     generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                     println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                     tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                     //---
                     generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                     println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                     tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   }
                   //---
                   generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
             }
           }
         } else {
           // println "generatedOrdinalAnimeNames: $generatedOrdinalAnimeNames"
           generatedSesonalAnimeNames.each { ordinalAnimeName ->
             println "------- Tier1: Adding ${ordinalAnimeName}"
             tier1AnimeNames += ["${ordinalAnimeName}"]
           }
         }
       }
       switch (group.specialType) {
         case ~/(?i)(OVA|ONA|OAD)/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- Ordinal ---------- //
     if (group.hasOrdinalSeasonality ) {
       println '----- Ordinal Seasonality Syntax Detected'
       hasSeasonality = true
       mySeasonalityNumber = group.ordinalSeasonNumber.toInteger()
       println "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       generatedOrdinalAnimeNames = [] as HashSet
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         println "------- Ordinal: Adding ${basename} - Season is 0/1"
         generatedOrdinalAnimeNames += ["${basename}"]
       } else {
         addGroupAnimeNameToList = false
         println "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         println "------- Ordinal: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         println "------- Ordinal: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         println "------- Ordinal: Adding ${generatedAnimeName} - Ordinal Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           println "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
           println "------- Tier1: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           println "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         //---
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         println "------- Ordinal: Adding ${generatedAnimeName} - Seasonality Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         println "------- Ordinal: Adding ${generatedAnimeName} - Series Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         println "------- Ordinal: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( !group.hasPartialSeasonality) {
           switch (group.specialType) {
             case ~/(?i)(OVA|ONA|OAD)/:
               // ---------- Add Full list of alternative Season names as options and OVA Syntax (ugh)---------- //
               generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               if ( mySeasonalityNumber < 10 ) {
                 generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                 println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime II
                 println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                 println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
               //---
               generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
               println "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           }
         }
       }
       if ( group.hasPartialSeasonality ) {
         println "------- AND Partial Seasonality Detected (Ugh)"
         hasSeasonality = true
         mySeasonalityNumber = group.partialSeasonNumber.toInteger()
         println "------- mySeasonalityNumber: ${mySeasonalityNumber}"
         if (mySeasonalityNumber > 1) {
           generatedOrdinalAnimeNames.each { ordinalAnimeName ->
             // ---------- Add Full list of alternative Season names as options ---------- //
             generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber // anime part 2
             println "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
             println "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
             println "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
               println "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
               println "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             //---
             generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber // anime season 2
             println "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber // anime 2
             println "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber // anime s2
             println "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             switch (group.specialType) {
               case ~/(?i)(OVA|ONA|OAD)/:
                 // ---------- Add Full list of alternative Season names as options ---------- //
                 generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 if ( mySeasonalityNumber < 10 ) {
                   generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                   println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 }
                 //---
                 generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
                 println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
           }
         }
       } else {
         // println "generatedOrdinalAnimeNames: $generatedOrdinalAnimeNames"
         generatedOrdinalAnimeNames.each { ordinalAnimeName ->
           println "------- Tier1: Adding ${ordinalAnimeName}"
           tier1AnimeNames += ["${ordinalAnimeName}"]
         }
       }
     }
     // ---------- Partial ---------- //
     if (group.hasPartialSeasonality && !group.hasOrdinalSeasonality && !group.hasSeasonality ) {
       println '----- Partial Seasonality Syntax Detected (NO Ordinal and/or No Season Detected)'
       hasSeasonality = true
       mySeasonalityNumber = group.partialSeasonNumber.toInteger()
       println "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         println "------- Tier1: Adding ${basename} - Season is 0/1"
         tier1AnimeNames += ["${basename}"]
       } else {
         addGroupAnimeNameToList = false
         println "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         println "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         println "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         println "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           println "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           println "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         println "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         println "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         println "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         switch (group.specialType) {
           case ~/(?i)(OVA|ONA|OAD)/:
             // ---------- Add Full list of alternative Season names as options ---------- //
             generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
               println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
               println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             //---
             generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
             println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
       }
     }
     // ---------- Specials ---------- //
     if ( group.isSpecialEpisode || group.isSpecialType ) {
       println '----- OVA/ONA/OAD/Special Syntax Detected'
       switch (group.specialType) {
         case ~/(?i)(OVA|ONA|OAD)/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           println "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- Checking if Anime Name already in list ---------- //
     if ( tier1AnimeNames.contains("${basename}") ) {
       println "----- Anime name already in list ${basename}"
     } else if (addGroupAnimeNameToList) {
       println "------- Tier1: Adding Group Anime Name to Anime Name List - ${basename}"
       tier1AnimeNames += ["${basename}"]
     }
   }
   // ---------- Show us what we will be searching for ---------- //
//   println "------- Filebot Detected Anime Name: ${animeDetectedName}"
   if ( animeDetectedName != null ) {
     println "------- Filebot Detected Anime Name: ${animeDetectedName}"
     if ( group.isFileBotDetectedName == false ) {
       statsGroupsFromScript++
       fbdetectName = "${jwdStringBlender(animeDetectedName)}"
       if ( hasSeasonality ) {
         println "--------- We have Seasonality, checking Tier 1 and Tier2 lists"
         if ( !tier1AnimeNames.contains("${fbdetectName}") && !tier2AnimeNames.contains("${fbdetectName}") ) {
           println "--------- Tier3: Adding ${fbdetectName} FileBot Detected name to Tier 3 List"
           tier3AnimeNames += animeDetectedName
           statsTier3FilebotNameAdded++
         } else {
           statsTierFilebotNameIncluded++
           println "--------- ${fbdetectName} Already in Tier1 or Tier2 list"
         }
       } else {
         println "--------- Checking Tier 1 list"
         if ( !tier1AnimeNames.contains("${fbdetectName}") ) {
           println "--------- Tier3: Adding ${fbdetectName} FileBot Detected name to Tier 3 List"
           tier3AnimeNames += animeDetectedName
           statsTier3FilebotNameAdded++
         } else {
           statsTierFilebotNameIncluded++
           println "--------- ${fbdetectName} Already in Tier1 list"
         }
       }
     } else {
       println "--------- Do not compute jaroWinklerDistance as Group name is from Filebot"
       statsGroupsFromFilebot++
     }
   } else {
     println "------- Filebot Detected Anime Name: ${animeDetectedName}"
     statsTierFilebotNameNull++
     println "--------- Can't compute jaroWinklerDistance as detected Anime Name is null"
   }
  println '// END---------- Series Name Generation ---------- //'
  return [group]
 }



String order = ''
animeDetectedName = ''
renamerSource = 'script' as String
rfsIncomplete = false as Boolean// Global variable?
Boolean hasTVDBSeasonalitySyntax = false
Boolean hasOrdinalSeasonalitySyntax = false
hasSeasonality = false as Boolean
firstPassOptionsSet = false
secondPassOptionsSet = false
myXMLTVDBSeasonLookupSetup = false
myXMLTVDBSeasonLookup = 0
Boolean hasOVAONASyntax = false
animeFoundInTVDB = false
animeFoundInAniDB = false
statsGroupsFromFilebot = 0 as Integer
statsGroupsFromScript = 0 as Integer
statsRenamedUsingFilebot = 0 as Integer
statsRenamedUsingScript = 0 as Integer
statsTVDBJWDFilebotOnly = 0 as Integer
statsANIDBJWDFilebotOnly = 0 as Integer
statsTierFilebotNameIncluded = 0 as Integer
statsTierFilebotNameNull = 0 as Integer
statsTier3FilebotNameAdded = 0 as Integer
statsTVDBFilebotMatchedScript = 0 as Integer
statsANIDBFilebotMatchedScript = 0 as Integer
rfsLeftOver = []
rfsFinalLeftOver = []
rfs = []
tier1AnimeNames = [] as HashSet
tier2AnimeNames = [] as HashSet
tier3AnimeNames = [] as HashSet
myAniDBOMTitles = [] as HashSet
mySeasonalityNumber = 0
aniDBBanHammer = false as Boolean
println ''
println ''
println '***********************************'
println '***   Start Episode Renaming    ***'
println '***********************************'
println ''
println ''
groupsByManualThreeEpisodes.each { group, files ->
  // ---------- Reset Variables ---------- //
  BigDecimal myMatchNumber = 0.9800000000000000000
  mySeasonalityNumber = 0
  renamerSource = 'script'
  gotAniDBID = 0
  rfsLeftOver = []
  myAniDBOMTitles = []
  anidbJWDResults = [:]
  fileBotAniDBJWDResults = [:]
  baseGeneratedAnimeNames = [] as HashSet
  tier1AnimeNames = [] as HashSet
  animeFoundInAniDB = false
  animeANIDBSearchFound = false
  performRename = true
  BigDecimal firstANIDBWTMatchNumber = 0
  BigDecimal secondANIDBWTMatchNumber = 0
  BigDecimal thirdANIDBWTMatchNumber = 0
  BigDecimal fileBotANIDBJWDMatchNumber = 0
  String firstAniDBWTMatchName = ''
  String secondAniDBWTMatchName = ''
  String thirdAniDBWTMatchName = ''
  LinkedHashMap anidbFirstMatchDetails = [:]
  LinkedHashMap anidbSecondMatchDetails = [:]
  LinkedHashMap anidbThirdMatchDetails = [:]
  LinkedHashMap fileBotANIDBJWDMatchDetails = [score: 0.00000000, db:'AniDB', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Set myTBDBSeriesInfoAliasNames = []
  String renameMapper = ''
  String renameQuery = group.anime
  String renameFilter = ''
  Boolean renameStrict = true
  String renameDB = 'AniDB'
  String renameOrder = group.order
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  if (aniDBBanHammer) {
    println ''
    println '//----------------'
    println '//-------------------------------------------'
    println "Stop Processing due to AniDB Ban"
    println '//-------------------------------------------'
    println '//----------------'
    return
  }
  println ''
  println '//-------------------------------------------'
  log.finest "${groupInfoGenerator(group)} => ${files*.name}"
  // ---------- START TV Mode ---------- //
  // ---------- Reset Variables ---------- //
  // TODO
  // Implement actions or options to allow changing the "default" Match threshold
  animeFoundInTVDB = false
  animeTVDBSearchFound = false
  fileBotAniDBMatchUsed = false
  fileBotTheTVDBMatchUsed = false
  thetvdbJWDResults = [:]
  fileBotThetvDBJWDResults = [:]
  tier2AnimeNames = [] as HashSet
  tier3AnimeNames = [] as HashSet
  tempBaseGeneratedAnimeNames = [] as HashSet
  BigDecimal firstTVDBDWTMatchNumber = 0
  BigDecimal secondTVDBDWTMatchNumber = 0
  BigDecimal thirdTVDBDWTMatchNumber = 0
  BigDecimal fileBotTheTVDBJWDMatchNumber = 0
  String firstTVDBDWTMatchName = ''
  String secondTVDBDWTMatchName = ''
  String thirdTVDBDWTMatchName = ''
  LinkedHashMap theTVDBFirstMatchDetails = [:]
  LinkedHashMap theTVDBSecondMatchDetails = [:]
  LinkedHashMap theTVDBThirdMatchDetails = [:]
  LinkedHashMap renameOptions = [isSpecialEpisode: false, isSpecialType: false, isMovieType: false, renameStrict: false, renameQuery: false, renameDB: false, renameOrder: false, renameFilter: false, renameMapper: false]
  LinkedHashMap fileBotTheTVDBJWDMatchDetails = [score: 0.00000000, db:'TheTVDB', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Boolean addGroupAnimeNameToList = true
  firstPassOptionsSet = false
  secondPassOptionsSet = false
  thirdPassOptionsSet = false
  animeDetectedName = detectAnimeName(files)
  hasSeriesSyntax = false
  hasRomanSeries = false
  hasSeasonality = false
  hasOrdinalSeasonality = false
  hasPartialSeasonality = false
  isSpecial = false
  hasOVAONASyntax = false
  // ---------- Basename Generation ---------- //
  returnThing = basenameGenerator(group, useBaseAnimeNameWithSeriesSyntax)
  // println "returnThing.class:${returnThing.getClass()}"
  // println "/---"
  // println "returnThing:${returnThing}"
  // println "/---"
  // println "groupBEFORE:${group}"
  // println "/---"
  group = returnThing[0] as LinkedHashMap
  //     println "groupAFTER:${group}"
  baseGeneratedAnimeNames = returnThing[1] as HashSet
  // println "/---"
  // println "baseGeneratedAnimeNames:${baseGeneratedAnimeNames}"
  // END---------- Basename Generation ---------- //
  // START---------- Series Name Generation ---------- //
  returnThing = seriesnameGenerator(group, baseGeneratedAnimeNames)
  // END---------- Series Name Generation ---------- //
  //    log.finest "${groupInfoGenerator(group)}"
//      log.finest "hasSeasonlity:${hasSeasonality}"
//      log.finest "mySeasonalityNumber:${mySeasonalityNumber}"
  println '-----'
  println '-----'
  println "  We are going to be searching for these Anime Series Names: ${tier1AnimeNames} with TheTVDB and AniDB"
  if ( tier2AnimeNames ) {
    println "  We are going to be searching for these Anime Series Names: ${tier2AnimeNames} with TheTVDB"
  }
  if ( tier3AnimeNames ) {
    println "  We are going to be searching for these Anime Series Names: ${tier3AnimeNames} with TheTVDB, AniDB from FileBot"
  }
  println '-----'
  println '-----'
  // ---------- Find "best" matched Name using JWD Matching ---------- //
  // ---------- START with TheTVDB ---------- //
  returnThing = filebotTVDBJWDSearch(tier1AnimeNames, thetvdbJWDResults, animeFoundInTVDB, locale)
  thetvdbJWDResults = returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  //    println "thetvdbJWDResults:->${thetvdbJWDResults}"
  returnThing2 = filebotAnidbJWDSearch(tier1AnimeNames, anidbJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  anidbJWDResults = returnThing2.jwdresults
  animeFoundInAniDB = returnThing2.animeFoundInAniDB
  //    println "anidbJWDResults:->${anidbJWDResults}"
  // ----------------------- //
  // ---  TIER 2         --- //
  // ----------------------- //
  returnThing = filebotTVDBJWDSearch(tier2AnimeNames, thetvdbJWDResults, animeFoundInTVDB, locale)
  thetvdbJWDResults += returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  //    println "thetvdbJWDResults:->${thetvdbJWDResults}"
  // ----------------------- //
  // ---  TIER 3         --- //
  // ----------------------- //
  returnThing = filebotTVDBJWDSearch(tier3AnimeNames, fileBotThetvDBJWDResults, animeFoundInTVDB, locale)
  fileBotThetvDBJWDResults = returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  //    println "fileBotThetvDBJWDResults:->${fileBotThetvDBJWDResults}"
  returnThing2 = filebotAnidbJWDSearch(tier3AnimeNames, fileBotAniDBJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  fileBotAniDBJWDResults = returnThing2.jwdresults
  animeFoundInAniDB = returnThing2.animeFoundInAniDB
  //    println "fileBotAniDBJWDResults:->${fileBotAniDBJWDResults}"
  if (animeFoundInTVDB) {
    theTVDBFirstMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } // works.
    // println "${thetvdbJWDResults}"
    // println "${theTVDBFirstMatchDetails}"
    // println "${fileBotThetvDBJWDResults}"
    if ( theTVDBFirstMatchDetails == null ) {
      statsTVDBJWDFilebotOnly++
      println "//--- ONLY Filebot Anime Name Matched something in TheTVDB ---///"
      fileBotTheTVDBMatchUsed = true
      theTVDBFirstMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
      theTVDBSecondMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      theTVDBThirdMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
      firstTVDBDWTMatchNumber = theTVDBFirstMatchDetails.score
    } else {
      firstTVDBDWTMatchNumber = theTVDBFirstMatchDetails.score
      firstTVDBDWTMatchName = theTVDBFirstMatchDetails.primarytitle
      theTVDBSecondMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      theTVDBThirdMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    }
    if ( theTVDBSecondMatchDetails != null ) {
      secondTVDBDWTMatchNumber = theTVDBSecondMatchDetails.score
      secondTVDBDWTMatchName = theTVDBSecondMatchDetails.primarytitle
    }
    if ( theTVDBThirdMatchDetails != null ) {
      thirdTVDBDWTMatchNumber = theTVDBThirdMatchDetails.score
      thirdTVDBDWTMatchName = theTVDBThirdMatchDetails.primarytitle
    }
    // This was just easier for "Sorting" the 1st/2nd with higher DBID in 1st
    if ( (firstTVDBDWTMatchNumber == 1 && secondTVDBDWTMatchNumber == 1) && (theTVDBFirstMatchDetails.dbid < theTVDBSecondMatchDetails.dbid ) ) {
      println "//---- Switch 1st/2nd TVDB"
      tmpTVDBWTMatchNumber = secondTVDBDWTMatchNumber
      tmpTVDBWTMatchName = secondTVDBDWTMatchName
      tmpTVDBMatchDetails = theTVDBSecondMatchDetails
      secondTVDBDWTMatchNumber = firstTVDBDWTMatchNumber
      secondTVDBDWTMatchName = firstTVDBDWTMatchName
      theTVDBSecondMatchDetails = theTVDBFirstMatchDetails
      firstTVDBDWTMatchNumber = tmpTVDBWTMatchNumber
      firstTVDBDWTMatchName = tmpTVDBWTMatchName
      theTVDBFirstMatchDetails = tmpTVDBMatchDetails
    }
    println "firstTVDBDWTMatchNumber: ${firstTVDBDWTMatchNumber}"
    println "firstTVDBDWTMatchName: ${firstTVDBDWTMatchName}"
    println "theTVDBFirstMatchDetails: ${theTVDBFirstMatchDetails}"
    println "secondTVDBDWTMatchNumber: ${secondTVDBDWTMatchNumber}"
    println "secondTVDBDWTMatchName: ${secondTVDBDWTMatchName}"
    println "theTVDBSecondMatchDetails: ${theTVDBSecondMatchDetails}"
    println "thirdTVDBDWTMatchNumber: ${thirdTVDBDWTMatchNumber}"
    println "thirdTVDBDWTMatchName: ${thirdTVDBDWTMatchName}"
    println "theTVDBThirdMatchDetails: ${theTVDBThirdMatchDetails}"
    if ( fileBotThetvDBJWDResults ) {
      if ( fileBotTheTVDBMatchUsed ) {
        fileBotTheTVDBJWDMatchDetails = [score: 0.00000000, db:'TheTVDB', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
        fileBotTheTVDBJWDMatchNumber = 0
      } else {
        // println "fileBotThetvDBJWDResults: ${fileBotThetvDBJWDResults}"
        fileBotTheTVDBJWDMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
        fileBotTheTVDBJWDMatchNumber = fileBotTheTVDBJWDMatchDetails.score
        if ( fileBotTheTVDBJWDMatchDetails.dbid == theTVDBFirstMatchDetails.dbid ) {
          statsTVDBFilebotMatchedScript++
        }
        println ""
        println "fileBotTheTVDBJWDMatchNumber: ${fileBotTheTVDBJWDMatchNumber}"
        println "fileBotTheTVDBJWDMatchDetails: ${fileBotTheTVDBJWDMatchDetails}"
      }
    }
    println ''
  } else {
    println '//-------------------------------------------//'
    println "Nothing was found for ${group.anime} in TheTVDB"
    println '//-------------------------------------------//'
    firstTVDBDWTMatchNumber = 0
  }
  if (animeFoundInAniDB) {
    anidbFirstMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
    if ( anidbFirstMatchDetails == null ) {
      statsANIDBJWDFilebotOnly++
      println "//--- ONLY Filebot Anime Name Matched something in ANIDB ---///"
      fileBotAniDBMatchUsed = true
      anidbFirstMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score
      anidbSecondMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      anidbThirdMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    } else {
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score
      firstAniDBWTMatchName = anidbFirstMatchDetails.primarytitle
      anidbSecondMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      anidbThirdMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    }
    if ( anidbSecondMatchDetails != null ) {
      secondANIDBWTMatchNumber = anidbSecondMatchDetails.score
      secondAniDBWTMatchName = anidbSecondMatchDetails.primarytitle
    }
    if ( anidbThirdMatchDetails != null ) {
      thirdANIDBWTMatchNumber = anidbThirdMatchDetails.score
      thirdAniDBWTMatchName = anidbThirdMatchDetails.primarytitle
    }
    // This was just easier for "Sorting" the 1st/2nd with higher AID in 1st
    if ( (firstANIDBWTMatchNumber == 1 && secondANIDBWTMatchNumber == 1) && (anidbFirstMatchDetails.dbid < anidbSecondMatchDetails.dbid ) ) {
      println "//---- Switch 1st/2nd AniDB"
      tmpANIDBWTMatchNumber = secondANIDBWTMatchNumber
      tmpAniDBWTMatchName = secondAniDBWTMatchName
      tmpAniDBMatchDetails = anidbSecondMatchDetails
      secondANIDBWTMatchNumber = firstANIDBWTMatchNumber
      secondAniDBWTMatchName = firstAniDBWTMatchName
      anidbSecondMatchDetails = anidbFirstMatchDetails
      firstANIDBWTMatchNumber = tmpANIDBWTMatchNumber
      firstAniDBWTMatchName = tmpAniDBWTMatchName
      anidbFirstMatchDetails = tmpAniDBMatchDetails
    }
    println "firstANIDBWTMatchNumber: ${firstANIDBWTMatchNumber}"
    println "firstAniDBWTMatchName: ${firstAniDBWTMatchName}"
    println "anidbFirstMatchDetails: ${anidbFirstMatchDetails}"
    println "secondANIDBWTMatchNumber: ${secondANIDBWTMatchNumber}"
    println "secondAniDBWTMatchName: ${secondAniDBWTMatchName}"
    println "anidbSecondMatchDetails: ${anidbSecondMatchDetails}"
    println "thirdANIDBWTMatchNumber: ${thirdANIDBWTMatchNumber}"
    println "thirdAniDBWTMatchName: ${thirdAniDBWTMatchName}"
    println "anidbThirdMatchDetails: ${anidbThirdMatchDetails}"
    if ( fileBotAniDBJWDResults ) {
      if ( fileBotAniDBMatchUsed ) {
        fileBotANIDBJWDMatchDetails = null
        fileBotANIDBJWDMatchNumber = 0
      } else {
        fileBotANIDBJWDMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
        fileBotANIDBJWDMatchNumber = fileBotANIDBJWDMatchDetails.score
        if ( fileBotANIDBJWDMatchDetails.dbid == anidbFirstMatchDetails.dbid ) {
          statsANIDBFilebotMatchedScript++
        }
        println "fileBotANIDBJWDMatchNumber: ${fileBotANIDBJWDMatchNumber}"
        println "fileBotANIDBJWDMatchDetails: ${fileBotANIDBJWDMatchDetails}"
      }
    }
  } else {
    println '//-----------------------------------------//'
    println "Nothing was found for ${group.anime} in AniDB"
    println '//-----------------------------------------//'
    firstANIDBWTMatchNumber = 0
  }
  if (!animeFoundInAniDB && !animeFoundInTVDB) {
    println "Since we can't find anything, we should skip.."
    performRename = false
    rfsIncomplete = false as Boolean
  }
  // ------------------------------ //
  // ---------- 1st Pass ---------- //
  // ------------------------------ //
  if ( performRename ) {
    returnThing = episodeRenameOptionPassOne(1, group, files, hasSeasonality, mySeasonalityNumber, firstANIDBWTMatchNumber, secondANIDBWTMatchNumber, thirdANIDBWTMatchNumber, fileBotANIDBJWDMatchNumber, anidbFirstMatchDetails, anidbSecondMatchDetails, anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails, firstTVDBDWTMatchNumber, secondTVDBDWTMatchNumber, thirdTVDBDWTMatchNumber, fileBotTheTVDBJWDMatchNumber, theTVDBFirstMatchDetails, theTVDBSecondMatchDetails, theTVDBThirdMatchDetails, fileBotTheTVDBJWDMatchDetails, performRename, fileBotAniDBMatchUsed, animeFoundInAniDB, animeFoundInTVDB, fileBotTheTVDBMatchUsed, statsRenamedUsingScript, statsRenamedUsingFilebot, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, animeOfflineDatabase, useNonStrictOnTVDBSpecials)
    groupByRenameOptions = returnThing.groupByRenameOptions
    statsRenamedUsingScript = returnThing.statsRenamedUsingScript
    statsRenamedUsingFilebot = returnThing.statsRenamedUsingFilebot
    // ---------- 1st pass Renaming---------- //
    groupByRenameOptions.each { groupRenameOptions, renameFiles ->
      performRename = groupRenameOptions.performRename
      renameOptions.renameStrict = groupRenameOptions.renameStrict
      renameOptions.renameQuery = groupRenameOptions.renameQuery
      renameOptions.renameDB = groupRenameOptions.renameDB
      renameOptions.renameOrder = groupRenameOptions.renameOrder
      renameOptions.renameFilter = groupRenameOptions.renameFilter
      renameOptions.renameMapper = groupRenameOptions.renameMapper
      renameOptions.isSpecialEpisode = groupRenameOptions.isSpecialEpisode
      renameOptions.isSpecialType = groupRenameOptions.isSpecialType
      renameOptions.isMovieType = groupRenameOptions.isMovieType
      // ---------- Stupid filename/lookup handling aka OVERRIDES---------- //
      if (renameOptions.renameQuery == '9517' && renameOptions.renameDB == 'AniDB' && hasSeasonality && mySeasonalityNumber == 1 ) {
        // The current Regex process used removes all `, which is a problem for those series that END in ` and use multiple ` to denote different seasons
        // aka Dog Days` and Dog Days``. So until I can figure out how to not replace ` when it's at the end .. Need an override ..
        println "--- Dog Days Season 1 override"
        renameOptions.renameQuery = '8206'
      }
      if (renameOptions.renameQuery == '7432' && renameOptions.renameOrder == 'Absolute') {
        // Mirai Nikki (2011) is the actual Season name in AniDB, however there *IS* a single episode OVA named Mirai Nikki .. Groups seem to forget about it ..
        // Mirai Nikki on TheTVDB is an alias, so it probably didn't trip the logic to use TheTVDB despite the 1.0 match (since it's an alias)
        // We will just switch it then to TheTVDB :)
        println "--- TVDB: Complete: Mirai Nikki (AniDB: 7432) override"
        renameOptions.renameQuery = theTVDBFirstMatchDetails.dbid
        renameOptions.renameDB = 'TheTVDB'
        renameOptions.renameOrder = 'Airdate'
        renameOptions.renameStrict = true
        renameOptions.renameFilter = ''
        //        renameMapper = '[AnimeList.AniDB, episode]'
        renameOptions.renameMapper = 'any {AnimeList.AniDB}, {order.absolute.episode}, {episode},  {XEM.AniDB}'
      }
      // TheTVDB has a Ikebukuro West Gate Park  from 2000, but AniDB/MAL doesn't .. because it's not Anime (sigh).. AniDB doesn't have this problem.
      if (renameOptions.renameQuery == '82348' && renameOptions.renameDB == 'TheTVDB') {
        println "--- TVDB: renameQuery: : Ikebukuro West Gate Park override"
        renameOptions.renameQuery = '381769'
      }
      // Noblesse matches the wrong Series .. of the exact same name! (WTF TheTVDB?). AniDB doesn't have this problem.
      if (renameOptions.renameQuery == '327859' && renameOptions.renameDB == 'TheTVDB' ) {
        println "--- TVDB: renameQuery: Noblesse override"
        renameOptions.renameQuery = '386818'
      }
      // Nisekoi (AID:9903) vs Nisekoi: (AID:10859)
      if (renameOptions.renameQuery == '275670' && renameOptions.renameDB == 'TheTVDB' && !hasSeasonality) {
        println "--- TVDB: renameFilter: Nisekoi override"
        renameOptions.renameFilter = 's == 1'
      }
      if (!performRename && renameOptions.isMovieType) {
        println "--- MOVIE Detected, Moving to Movie Processing Stage"
        groupsByManualThreeMovies += [(group): renameFiles]
      }
      if ( performRename ) {
        println '// -------------------------------------- //'
        println '// ---------- 1st pass Renaming---------- //'
        println "group: ${group}, files: ${renameFiles}"
        println "renameStrict: ${renameOptions.renameStrict}, renameQuery: ${renameOptions.renameQuery}, renameDB: ${renameOptions.renameDB}, renameOrder: ${renameOptions.renameOrder}, renameFilter: ${renameOptions.renameFilter}, renameMapper: ${renameOptions.renameMapper}"
        renameWrapper(group, renameFiles, renameOptions )
        println "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
        println '// -------------------------------------- //'
      }
    }
  }
  rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
  println "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
  // ------------------------------ //
  // ---------- 2nd Pass ---------- //
  // ------------------------------ //
  // ---------- Setup 2nd Pass Options for Specific "types" ---------- //
  if ( performRename && rfsLeftOver.size() >= 1 ) {
    sleep(2000) // Pause 2 seconds in between Stages
    returnThing = episodeRenameOptionPassOne(2, group, rfsLeftOver, hasSeasonality, mySeasonalityNumber, firstANIDBWTMatchNumber, secondANIDBWTMatchNumber, thirdANIDBWTMatchNumber, fileBotANIDBJWDMatchNumber, anidbFirstMatchDetails, anidbSecondMatchDetails, anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails, firstTVDBDWTMatchNumber, secondTVDBDWTMatchNumber, thirdTVDBDWTMatchNumber, fileBotTheTVDBJWDMatchNumber, theTVDBFirstMatchDetails, theTVDBSecondMatchDetails, theTVDBThirdMatchDetails, fileBotTheTVDBJWDMatchDetails, performRename, fileBotAniDBMatchUsed, animeFoundInAniDB, animeFoundInTVDB, fileBotTheTVDBMatchUsed, statsRenamedUsingScript, statsRenamedUsingFilebot, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, animeOfflineDatabase, useNonStrictOnTVDBSpecials)
    groupByRenameOptions = returnThing.groupByRenameOptions
    statsRenamedUsingScript = returnThing.statsRenamedUsingScript
    statsRenamedUsingFilebot = returnThing.statsRenamedUsingFilebot
    // ---------- 2nd pass Renaming---------- //
    groupByRenameOptions.each { groupRenameOptions, renameFiles ->
      performRename = groupRenameOptions.performRename
      renameOptions.renameStrict = groupRenameOptions.renameStrict
      renameOptions.renameQuery = groupRenameOptions.renameQuery
      renameOptions.renameDB = groupRenameOptions.renameDB
      renameOptions.renameOrder = groupRenameOptions.renameOrder
      renameOptions.renameFilter = groupRenameOptions.renameFilter
      renameOptions.renameMapper = groupRenameOptions.renameMapper
      renameOptions.isSpecialEpisode = groupRenameOptions.isSpecialEpisode
      renameOptions.isSpecialType = groupRenameOptions.isSpecialType
      renameOptions.isMovieType = groupRenameOptions.isMovieType
      if (!performRename && renameOptions.isMovieType) {
        println "--- MOVIE Detected, Moving to Movie Processing Stage"
        groupsByManualThreeMovies += [(group): renameFiles]
      }
      if ( performRename ) {
        println '// -------------------------------------- //'
        println '// ---------- 2nd pass Renaming---------- //'
        println "group: ${group}, files: ${renameFiles}"
        println "renameStrict: ${renameOptions.renameStrict}, renameQuery: ${renameOptions.renameQuery}, renameDB: ${renameOptions.renameDB}, renameOrder: ${renameOptions.renameOrder}, renameFilter: ${renameOptions.renameFilter}, renameMapper: ${renameOptions.renameMapper}"
        renameWrapper(group, renameFiles, renameOptions )
        println "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
        println '// -------------------------------------- //'
        rfsLeftOver = files.getFiles { it.isFile() }
        if (rfsLeftOver.size() >= 1 && renameOptions.isMovieType) {
          println "--- MOVIE Detected, Moving to Movie Processing Stage"
          groupsByManualThreeMovies += [(group): renameFiles]
        }
      }
    }
  }

  // ------------------------------ //
  // ---------- 3rd Pass ---------- //
  // ------------------------------ //

  // ------------------------------ //
  // ---------- 4th Pass ---------- //
  // ------------------------------ //

  // ---------- Stat Collection -----------//
  if ( rfsLeftOver ) {
    partialFiles += rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
  }
  if ( rfsIncomplete ) {
    unsortedFiles += files.getFiles { it.isFile() && it.isVideo() }
  }
  // ---------- FINISH TV Mode ---------- //
}

println '***********************************'
println '***    Start Movie Renaming     ***'
println '***********************************'
println ''
groupsByManualThreeMovies.each { group, files ->
  // ---------- Reset Variables ---------- //
  BigDecimal myMatchNumber = 0.9800000000000000000
  renamerSource = 'script'
  gotAniDBID = 0
  rfsLeftOver = []
  myAniDBOMTitles = []
  anidbJWDResults = [:]
  fileBotAniDBJWDResults = [:]
  baseGeneratedAnimeNames = [] as HashSet
  tier1AnimeNames = [] as HashSet
  animeFoundInAniDB = false
  animeANIDBSearchFound = false
  performRename = true
  BigDecimal firstANIDBWTMatchNumber = 0
  BigDecimal secondANIDBWTMatchNumber = 0
  BigDecimal thirdANIDBWTMatchNumber = 0
  BigDecimal fileBotANIDBJWDMatchNumber = 0
  String firstAniDBWTMatchName = ''
  String secondAniDBWTMatchName = ''
  String thirdAniDBWTMatchName = ''
  LinkedHashMap anidbFirstMatchDetails = [:]
  LinkedHashMap anidbSecondMatchDetails = [:]
  LinkedHashMap anidbThirdMatchDetails = [:]
  LinkedHashMap fileBotANIDBJWDMatchDetails = [score: 0.00000000, db: 'AniDB', dbid: 0, primarytitle: null, animename: null, matchname: null, alias: true]
  Set myTBDBSeriesInfoAliasNames = []
  String renameMapper = ''
  String renameQuery = group.anime
  String renameFilter = ''
  Boolean renameStrict = true
  String renameDB = 'AniDB'
  String renameOrder = group.order
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  firstPassOptionsSet = false
  if (aniDBBanHammer) {
    println ''
    println '//----------------'
    println '//-------------------------------------------'
    println "Stop Processing due to AniDB Ban"
    println '//-------------------------------------------'
    println '//----------------'
    return
  }
  if (skipMovies) {
    println ''
    println '//----------------'
    println '//-------------------------------------------'
    println "Stop Processing due to skipMovies"
    println '//-------------------------------------------'
    println '//----------------'
    return
  }
  println ''
  println '//-------------------------------------------'
  log.finest "${groupInfoGenerator(group)} => ${files*.name}"
//  log.finest "${group}"
  // ---------- START Movie Mode ---------- //
  returnThing = searchForMoviesJWD(group, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, locale, animeOfflineDatabase, aniDBCompleteXMLList)
  animeFoundInAniDB = returnThing.animeFoundInAniDB
  firstANIDBWTMatchNumber = returnThing.firstANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  firstAniDBWTMatchName = returnThing.firstAniDBWTMatchName
  anidbFirstMatchDetails = returnThing.anidbFirstMatchDetails
  //noinspection GroovyUnusedAssignment
  secondANIDBWTMatchNumber = returnThing.secondANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  secondAniDBWTMatchName = returnThing.secondAniDBWTMatchName
  //noinspection GroovyUnusedAssignment
  anidbSecondMatchDetails = returnThing.anidbSecondMatchDetails
  //noinspection GroovyUnusedAssignment
  thirdANIDBWTMatchNumber = returnThing.thirdANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  thirdAniDBWTMatchName = returnThing.thirdAniDBWTMatchName
  //noinspection GroovyUnusedAssignment
  anidbThirdMatchDetails = returnThing.anidbThirdMatchDetails
  fileBotANIDBJWDMatchNumber = returnThing.fileBotANIDBJWDMatchNumber
  fileBotANIDBJWDMatchDetails = returnThing.fileBotANIDBJWDMatchDetails
  if (!animeFoundInAniDB) {
    println "Since we can't find anything, we should skip.."
    performRename = false
    rfsIncomplete = false as Boolean
  }
  println '// ---------- deliberations on order, DB, filter ---------- //'
  // --- airdate Syntax --- //
  if (performRename) {
    sleep (2000) // Pause 2 seconds (minimum) between renames
    if (firstANIDBWTMatchNumber > 0.9800000000000000000) {
      println '------- 1st AniDB match 0.98+'
      println '--------- We are going to try 1st AniDB match'
      firstPassOptionsSet = true
      renameQuery = anidbFirstMatchDetails.dbid
      renameDB = 'AniDB'
      renameOrder = 'Absolute'
      if (useNonStrictOnAniDBMovies) {
        renameStrict = false
      } else {
        renameStrict = true
      }
      renameFilter = ''
      renameMapper = ''
      println "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    }
    if (firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000) {
      println '------- 1st AniDB match < 0.98 and Filebot 0.98+'
      println '--------- We are going to try Filebot match'
      firstPassOptionsSet = true
      renameQuery = fileBotANIDBJWDMatchDetails.dbid
      renameDB = 'AniDB'
      renameOrder = 'Absolute'
      if (useNonStrictOnAniDBMovies) {
        renameStrict = false
      } else {
        renameStrict = true
      }
      renameFilter = ''
      renameMapper = ''
      println "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    }
    if (!firstPassOptionsSet) {
      println '//-----------------------------//'
      println '//  STOP - Movie.1-1st.1      //'
      println '//-----------------------------//'
      performRename = false
      firstPassOptionsSet = true
    }
  }
  // ---------- Start Renaming---------- //
  if (performRename) {
    println '// -------------------------------------- //'
    println '// ---------- 1st pass Renaming---------- //'
    println "group: ${group}, files: ${files}"
    println "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    renameMovieWrapper(group, files, renameStrict, renameQuery, renameDB, renameOrder, renameFilter, renameMapper)
    println "rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
    println '// -------------------------------------- //'
  }

  // ---------- Stat Collection -----------//
  if ( rfsLeftOver ) {
    partialFiles += rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
  }
  if ( rfsIncomplete ) {
    unsortedFiles += files.getFiles { it.isFile() && it.isVideo() }
  }
}

// ---------- END Renaming ---------- //
println '// ---------- END Renaming ---------- //'
println "Total Files: ${input.size()-1}"
println "   Processed files: ${destinationFiles.size()}"
renameLog.sort{it.value.metadata}.each { from, to ->
  println "       [$to.metadata];[${to.name}];[${from.name}];[${to.parent}]"
}
println '// ----------             ---------- //'
println "      Script Name Renamed files: ${destinationFilesScript.size()}"
destinationFilesScript.each { file ->
  println "       [${file.name}, ${file.parent}]"
}
println '// ----------             ---------- //'
println "      Filebot Name Renamed files: ${destinationFilesFilebot.size()}"
destinationFilesFilebot.each { file ->
  println "       [${file.name}, ${file.parent}]"
}
println '// ----------             ---------- //'
println "   1st Pass Rename Missed Files: ${renameMissedFiles1stPass.size()}"
println "   2nd Pass Rename Missed Files: ${renameMissedFiles2ndPass.size()}"
println "   Total Rename failure: ${unsortedFiles.size()}"
unsortedFiles.each { file ->
  println "       [${file.name}, ${file.parent}]"
}
println '// ----------             ---------- //'
println "Stats:"
println "   Tier Names included Filebot Detected Anime Name:[${statsTierFilebotNameIncluded}]"
println "   TVDB Filebot JWD Matched 1st TVDB JWD:[${statsTVDBFilebotMatchedScript}]"
println "   AniDB Filebot JWD Matched 1st AniDB JWD:[${statsANIDBFilebotMatchedScript}]"
println "   Tier 3 Filebot Detected Anime Name used:[${statsTier3FilebotNameAdded}]"
println "   Tier Filebot Detected Anime Name null:[${statsTierFilebotNameNull}]"
println "-----"
println "   Groups using Script Name:[${statsGroupsFromScript}]"
println "   Rename actions using Script JWD:[${statsRenamedUsingScript}]"
println "   Script Rename Failure:[${failedFilesScript.size()}]"
println "-----"
println "   Groups using Filebot Name:[${statsGroupsFromFilebot}]"
println "   TVDB JWD found only from Filebot:[${statsTVDBJWDFilebotOnly}]"
println "   AniDB JWD found only from Filebot:[${statsANIDBJWDFilebotOnly}]"
println "   Rename actions using Filebot JWD:[${statsRenamedUsingFilebot}]"
println "   Filebot Rename Failure:[${failedFilesFilebot.size()}]"

// abort and skip clean-up logic if we didn't process any files
if (destinationFiles.size() == 0) {
  log.fine 'Finished without processing any files'
}