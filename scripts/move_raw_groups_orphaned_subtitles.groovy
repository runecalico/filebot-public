import net.filebot.Logging
import net.filebot.hash.HashType
import net.filebot.hash.VerificationUtilities
import org.apache.commons.io.FilenameUtils

import java.nio.file.Path
import java.nio.file.Paths


// Include My Libraries
include('lib/anidb')  // AniDB Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/manami')  // Anime offline Database Stuff
include('lib/shared') // Generic/Shared Functions
include('lib/sorter') // Renamer Sorter methods
include('lib/tvdb')  // TVDB Stuff

// sanity checks
if (args.size() == 0) {
  die "Illegal usage: no input"
} else if (args.any{ it in File.listRoots() }) {
  die "Illegal usage: input $args must not include a filesystem root"
}

// List all the Release Groups that frequently have the English Subtitles in a subfolder
def foldersA = args[0].getFolders() { it.getName() =~ /(Beatrice-Raws|Kawaiika-Raws)/ }
println "Eligable folder count: ${foldersA.size()}"

foldersA.each { folder ->
    def delim = $/\\/$
    def join = '\\' // Because it doesn't like dollar slashy for the join
    Logging.log.info "//-----------------"
    // Get a list of our Video Files
    def videos = folder.getFiles { it.isVideo() }
    // if there is only one file, don't bother with CommonPath()
    String myInputFolder = videos.size() == 1 ? FilenameUtils.getFullPathNoEndSeparator(commonPath(delim, join, videos as ArrayList<File>)) : commonPath(delim, join, videos as ArrayList<File>)
    Path inputFolderPath = Paths.get(myInputFolder)
    Logging.log.info "//--- Input Folder: ${myInputFolder}"
    Logging.log.info "//--- Video count: ${videos.size()}"
    // Now Get all of the Subtitle files that are "orphaned"
    def subtitles = folder.getFiles { it.isSubtitle() || (!it.isVideo() && (it.getExtension() == 'mks') )}
    Logging.log.info "//--- subtitles count: ${subtitles.size()}"
    def aniAddFormat = "${myInputFolder}/{fn}"
    Logging.log.info "//--- Move subtitles to ${myInputFolder}"
    rename(file:subtitles, format: aniAddFormat, db: 'file')
    Logging.log.info "//-----------------"
}
