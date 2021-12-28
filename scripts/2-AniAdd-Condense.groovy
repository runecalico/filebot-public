#!/usr/bin/env filebot -script

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
onlylist  = any { onlylist.toBoolean() } { false }

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}


// filebot --action $ACTION -script $MYScriptPath/initialsort_aniadd_add_mediainfo_movement.groovy $FileBotSourceDirectory Z:\2-AniAdd\ --log all --output Z:\2-AniAdd.2 --db AniDB --lang=en --order Absolute --def onlylist=true
// --format "{file.parent.substring($("Z:\2-AniAdd.3\".Length))/fn}"
// filebot --action test -script .\2-AniAdd-Condense.groovy W:\1-InitialSort.2 --output W:\2-AniAdd\AniAddSortedSeries
// clear;filebot --action test -script .\2-AniAdd-Condense.groovy W:\1-InitialSort.2\2020 --output W:\2-AniAdd\AniAddSortedSeries --log finest
// Assume the Output folder has directories following the naming convenction of anime series [anidb-####]
//
// Argument[0]: W:\1-InitialSort\2020\winter
// Output: W:\2-AniAdd\AniAddSortedSeries
// For every AID in Output, look for any matching AID in Argument[0], if there is a match then move all files from Argument[0] to directory in Output.
// Get's a list of folders from each location
def foldersA = args[0].getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\1-InitialSort\2020\winter
def foldersB = outputFolder.getFolders() { it =~ /(\[anidb\-\d+\])/ } // W:\2-AniAdd\AniAddSortedSeries
println "FoldersA count: ${foldersA.size()}"
println "FoldersB count: ${foldersB.size()}"
// Get a map of all folders by AniDB ID # in each location
def folderGroupsA = foldersA.groupBy {
  def anidbId = it.name =~ /^(.+)\[anidb\-(\d+)\]/
  anidbId[0][2]
}
def folderGroupsB = foldersB.groupBy {
  def anidbId = it.name =~ /^(.+)\[anidb\-(\d+)\]/
  anidbId[0][2]
}
folderGroupsB.each { zAid, zDir ->
  def existsInA = folderGroupsA.get(zAid)
  if ( existsInA == null) {
    return
  }
  log.fine "AniDB ID: ${zAid}" // 15268
  log.fine "\t Folders in B: - ${zDir}" // [W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
  log.fine "\t Folders in A: - ${existsInA}" // [W:\1-InitialSort.2\2020\fall\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600], W:\1-InitialSort.2\2020\summer\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]]
  if (!onlylist) {
    // It's unlikely (but possible) to have more then one folder in the Output Directory with the same AID ..
    zDir.each { outputDir ->
      log.finest "\t getName: ${outputDir.getName()}" // Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
      log.finest  "\t getNameCount of outputDir: [${outputDir.toPath().getNameCount()}] - ${outputDir}:" // [3] - W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
      def myNameCount = outputDir.toPath().getNameCount()
      log.finest  "\t getNameCount of outputFolder: [${outputFolder.toPath().getNameCount()}] - ${outputFolder}" // [2] - W:\2-AniAdd\AniAddSortedSeries
      def myArgNameCount = outputFolder.toPath().getNameCount()
      def myRelativePath = outputDir.toPath().subpath(myArgNameCount, myNameCount)
      log.finest  "\t Relative path of directory to outputFolder using subpath: ${myRelativePath}" // winter\Youkai Watch Jam Youkai Gakuen Y - N to no Souguu [anidb-15268]
      def aniAddFormat = "${myRelativePath}/{fn}"
      log.finest  "\t aniAddFormat: ${aniAddFormat}"
      existsInA.each { directory ->
        log.finest  "\t \t InputFolder: ${directory}"
        log.finest  "\t rename(folder: ${directory}, format: aniAddFormat, db: file)"
        rename(folder:directory, format: aniAddFormat, db: 'file')
      }
    }
  }
  println ""
}