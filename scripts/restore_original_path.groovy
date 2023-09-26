#!/usr/bin/env filebot -script

@Grapes([
    @Grab(group='org.apache.commons', module='commons-lang3', version='3.12.0')
])


import groovy.transform.Field
import net.filebot.Logging
import org.apache.commons.io.FilenameUtils

// $ACTION="move"
// filebot --action $ACTION -script .\restore_original_path.groovy $FileBotSourceDirectory --output $FileBotDestDirectory

//--- VERSION 0.1.0
// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
//file:noinspection GroovyAssignabilityCheck
//file:noinspection GrReassignedInClosureLocalVar

@Field String removeFileExtensionRegex = /((?<!\s\d{1,4})(?>\.\d)?\.\w{3}$|\.\w{3}$)/

// log input parameters
Logging.log.info("Run script [$_args.script] at [$now]")

args.withIndex().each { f, i -> if (f.exists()) { Logging.log.info "Argument[$i]: $f" } else { Logging.log.warning "Argument[$i]: File does not exist: $f" } }

// initialize variables
failOnError = _args.conflict.equalsIgnoreCase('fail')
testRun = _args.action.equalsIgnoreCase('test')
clearxttr =  tryQuietly { clearxttr.toBoolean() }

// --output folder must be a valid folder
outputFolder = _args.absoluteOutputFolder

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
if (args.size() == 0) {
  die 'Invalid usage: no input'
} else if (args.any { f -> f in outputFolder.listPath() }) {
  die "Invalid usage: output folder [$outputFolder] must be separate from input arguments $args"
} else if (args.any { f -> //noinspection GroovyInArgumentCheck
  f in File.listRoots() }) {
  die "Invalid usage: input $args must not include a filesystem root"
}
String aniAddFormat
// We want to check all folders underneath our argument[0] path
log.info "Start"
def files = args.collectMany{ it.getFiles{ it.isVideo() } }
log.info "${files.size()} Input Files"
files.each { f ->
  String folderpath = FilenameUtils.separatorsToUnix(f.xattr['originalfolder'] == null ? "/" : f.xattr['originalfolder'])
  String filename = f.xattr['originalfilename'] == null ? f.xattr['net.filebot.filename'] == null ? f.name.replaceAll(/${removeFileExtensionRegex}/, '')  : f.xattr['net.filebot.filename'].replaceAll(/${removeFileExtensionRegex}/, '') : f.xattr['originalfilename'].replaceAll(/${removeFileExtensionRegex}/, '')
  log.finest "--- [$f.name] =>"
  log.finest "                folderpath:[${folderpath}]"
  log.finest "                filename:[${filename}]"
  if ( folderpath == "/" ) {
    aniAddFormat = "{\'\'\'${filename}\'\'\'}"
  } else {
    aniAddFormat = "{\'\'\'${folderpath}/${filename}\'\'\'}"
  }
  log.finest  "\t aniAddFormat: ${aniAddFormat}"
  if ( clearxttr ) {
    tryQuietly { f.xattr.clear() }
  }
  rename(file:f, format: aniAddFormat, db: 'file')
}