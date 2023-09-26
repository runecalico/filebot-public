#!/usr/bin/env filebot -script
//--- VERSION 1.0.0

@Grapes([
    @Grab(group='org.apache.commons', module='commons-lang3', version='3.12.0'),
])

import net.filebot.Logging
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

// $ACTION="move"
// $FileBotSourceDirectory="W:\1-InboundRaw.PreSort\anime"
// $FileBotDestDirectory="W:\1-InboundRaw.PreSort\foreign_characters"
// $MYScriptPath = (Get-Location).Path
// $MyScript = "$MyScriptPath\move_cjk_filenames.groovy"
// filebot --action $ACTION -rename --conflict index  -non-strict -script $MyScript $FileBotSourceDirectory --output $FileBotDestDirectory --log all


// log input parameters
Logging.log.info("Run script [$_args.script] at [$now]")

args.withIndex().each { f, i -> if (f.exists()) { Logging.log.info "Argument[$i]: $f" } else { Logging.log.warning "Argument[$i]: File does not exist: $f" } }

// initialize variables
failOnError = _args.conflict.equalsIgnoreCase('fail')
testRun = _args.action.equalsIgnoreCase('test')

// enable/disable features as specified via --def parameters
translateUrl      = any { translateUrl.toString() } { }

// --output folder must be a valid folder
outputFolder = _args.absoluteOutputFolder

// Include My Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // AniDB Stuff
include('lib/tvdb')  // TVDB Stuff
include('lib/manami')  // Anime offline Database Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/sorter') // Renamer Sorter methods
include('lib/translate') // Translate methods

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}
if (args.size() == 0) {
  die 'Invalid usage: no input'
} else if (args.any { f -> //noinspection GroovyInArgumentCheck
  f in File.listRoots() }) {
  die "Invalid usage: input $args must not include a filesystem root"
}

// We want to check all files underneath our argument[0] path
Logging.log.info "Start"
//def files = args.collectMany{ it.getFiles{ it.isVideo() || it.isSubtitle() } }
def files = args.collectMany{ it.getFiles() }
Logging.log.info "${files.size()} Input Files"
files.each { File f ->
  String renameFormat = ""
  String filename = f.name
  String fileType = f.isDirectory() ? "directory" : "file"
  if ( detectCJKCharacters(filename)) {
    Logging.log.info "Checking ${fileType} ${filename}"
    String translatedText = ""
    Logging.log.info "...CJK Language detected"
    Logging.log.info ".....Moving"
    Logging.log.finest "\t getName: ${f.getName()}" // Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
    Logging.log.finest "\t parentFolder: ${f.dir}"
    Logging.log.finest "\t getNameCount of outputDir: [${f.dir.toPath().getNameCount()}] - ${f.dir.toPath()}:"
    // [3] - W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
    def myNameCount = f.dir.toPath().getNameCount()
    Logging.log.finest "\t getNameCount of outputFolder: [${outputFolder.toPath().getNameCount()}] - ${outputFolder}"
    // [2] - W:\2-AniAdd\AniAddSortedSeries
    def myArgNameCount = outputFolder.toPath().getNameCount()
    if ( myArgNameCount == myNameCount ) {
      Logging.log.finest "\t $f.name is in same folder as outputFolder"
      renameFormat = "{ fn }"
    } else {
      def myRelativePath = FilenameUtils.separatorsToUnix(f.toPath().subpath(myArgNameCount, myNameCount).toString())
      Logging.log.finest "\t Relative path of directory to outputFolder using subpath: ${myRelativePath}"
      // winter\Youkai Watch Jam Youkai Gakuen Y - N to no Souguu [anidb-15268]
      renameFormat = "{ \'${myRelativePath}\'/fn }"
    }
    Logging.log.finest "\t \t InputFiles: ${f}"
    Logging.log.info   "\t renameFormat: ${renameFormat}"
    Logging.log.info   "\t rename(file: ${f}, format: renameFormat, db: file)"
    rename(file:f, format: renameFormat, db: 'file') // Why is it adding .mkv to the end of the file? Only windows 11 (yes)?
  } else {
//    Logging.log.finest "Checking ${fileType} ${filename}"
  }
}