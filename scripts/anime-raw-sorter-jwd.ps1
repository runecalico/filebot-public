[CmdletBinding()]
Param(
    [Parameter(Mandatory = $false)] [switch]$noCopy = $false,
    [Parameter(Mandatory = $false)] [switch]$breakAfterGroups = $false,
    [Parameter(Mandatory = $false)] [string]$LoggingLevel = "info"
)

Function GenerateFolder($path) {
    $global:foldPath = $null
    foreach($foldername in $path.split("\")) {
        $global:foldPath += ($foldername+"\")
        if (!(Test-Path $global:foldPath)){
            New-Item -ItemType Directory -Path $global:foldPath
            # Write-Host "$global:foldPath Folder Created Successfully"
        }
    }
}


$ACTION = "move"
$MYScriptPath = (Get-Location).Path
$MyDate=Get-Date -Format "M_d_y_h_m_s"
$MyYear=Get-Date -Format "yyyy"
$MyScriptOutput = "$MyScriptPath\anime-raw-sorter-jwd-$($MyDate).txt"

if ($breakAfterGroups) {
    write-output "//---             breakAfterGroups SET TO y         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    $breakAfterGroupsValue="y"
} else {
    $breakAfterGroupsValue="n"
}

#I need to prune out the Known Non-English Crap
# Release Groups:
# [IDIF]
# [007nF]

# Record the version of filebot I'm using ..
$FilebotVersion=filebot -version
write-output "//---                                                ---//" | Tee-Object -FilePath $MyScriptOutput
write-output "//---               FILEBOT VERSION                  ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- $FilebotVersion" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput

# Move Audio (Maybe I'll do something with it later)
$ACTION = "move"
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "Z:\1-InboundAudio"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move Everything Audio                         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
--log all --filter "( f.isAudio() )" --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput

# Move anything that isn't considered Video by Filebot (basically)
$ACTION = "move"
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "Z:\1-InboundNotVideo"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move Everything 'Not' Video/Subtitle           ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
--log all --filter "!( f.isVideo() || f.isSubtitle() )" --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

# I can only assume it's a typo, but some Chineese fansubs are using Episdio vs Episode .. So I'll just rename those ..
$ACTION = "move"
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "Z:\1-InboundRaw.PreSort\anime"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Rename any 'episdio' files                     ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
--log all --filter "(f.getName().toLowerCase().contains('episdio'))" `
--format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn.replaceAll(/(?i)Episdio/,'Episode')}"  | Tee-Object -Append -FilePath $MyScriptOutput

# Seems that while I can clear the metadata from a file in the script, filebot will *cache* the metadata read and use it for certain commands like detectAnimeSeries ..
# So we need to clear all metadata from the files prior to trying to sort them.
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$MyScript = "$MyScriptPath\clear_xattr_all_files.groovy"

write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Clear out all filebot metadata            ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript $FileBotSourceDirectory --log all *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "Z:\1-InitialSort.2"
$MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
$MYAnimeFormat= "$MYScriptPath\initialSort_strict_series.groovy"
# 1  - Strict Mode on Non-Movies WITH Xattr
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Run Filebot to move Only Series - STRICT! ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename --conflict index -r --def skipMovies=y clearXattr=y `
aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 `
renameExtras=y breakAfterGroups=$breakAfterGroupsValue  $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

#2 - Non-Strict on Specials/Movies NO XATTR
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 2nd: Run Filebot to move Specials/Movies - NONSTRICT! ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="nosuchuser/filebot" `
animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 enableFileBotMovieLookups=y useNonStrictOnAniDBSpecials=y `
useNonStrictOnAniDBMovies=y breakAfterGroups=$breakAfterGroupsValue $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

# 3. - Allow Non-Strict on 1.0 Matches
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 3rd: Run Filebot to move Series - NONSTRICT!         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="nosuchuser/filebot" `
animeFormat=@$MYAnimeFormat  minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBFullMatch=y `
 breakAfterGroups=$breakAfterGroupsValue $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

if ($noCopy) {
    write-output "//---             NO COPY SET TO TRUE                   ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    exit
}

#Search for English Stuff
$FileBotSourceDirectory="Z:\1-InitialSort.2"
$FIleBotDestDirectory="Z:\1-InitialSort-English"
$MyScript = "$MYScriptPath/move_english_audio_files.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Sort out English Audio                    ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      Z:\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION $FileBotSourceDirectory --conflict index --output $FIleBotDestDirectory `
--log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

# Add Prune for Release Groups that I don't ever want ..
# JustBLThings-aarinfantasy (Boy's Love)
# Prune for lower resolutions Initial Sort
$ACTION="move"
$MODE="episode"
$MyScript = "$MYScriptPath/sorting_ideas.groovy"

$FileBotSourceDirectory="Z:\1-InitialSort.2\releases"
$FIleBotDestDirectory="Z:\1-InitialSort-LowRes\releases"
GenerateFolder($FIleBotDestDirectory)
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Low Res Prune: 1-InitialSort.2\releases        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION --mode $MODE --def pruneLargerSizes=false pruneLowerResolutions=true `
pruneLowerResolutions4K=false pruneEightBitX265=false pruneByCompleteness=false prunePartiallyComplete=false pruneX264=false  `
$FileBotSourceDirectory --output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

$FileBotSourceDirectory="Z:\1-InitialSort.2\specials"
$FIleBotDestDirectory="Z:\1-InitialSort-LowRes\specials"
GenerateFolder($FIleBotDestDirectory)
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Low Res Prune: 1-InitialSort.2\specials        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION --mode $MODE --def pruneLargerSizes=false pruneLowerResolutions=true `
pruneLowerResolutions4K=false pruneEightBitX265=false pruneByCompleteness=false prunePartiallyComplete=false pruneX264=false  `
$FileBotSourceDirectory --output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

$FileBotSourceDirectory="Z:\1-InitialSort.2\$MyYear"
$FIleBotDestDirectory="Z:\1-InitialSort-LowRes\$MyYear"
GenerateFolder($FIleBotDestDirectory)
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Low Res Prune: 1-InitialSort.2\$MyYear        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION --mode $MODE --def pruneLargerSizes=false pruneLowerResolutions=true pruneLowerResolutions4K=false `
pruneEightBitX265=false pruneByCompleteness=false prunePartiallyComplete=false pruneX264=false  $FileBotSourceDirectory `
--output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

$MODE="movie"
$FileBotSourceDirectory="Z:\1-InitialSort.2\movies"
$FIleBotDestDirectory="Z:\1-InitialSort-LowRes\movies"
GenerateFolder($FIleBotDestDirectory)
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Low Res Prune: 1-InitialSort.2\releases        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION --mode $MODE --def pruneLargerSizes=false pruneLowerResolutions=true `
pruneLowerResolutions4K=false pruneEightBitX265=false pruneByCompleteness=false prunePartiallyComplete=false pruneX264=false  `
$FileBotSourceDirectory --output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

# For now Back-fill 2021 stuff
$FileBotSourceDirectory="Z:\1-InitialSort.2\releases"
$FIleBotDestDirectory="Z:\1-InitialSort\$MyYear\"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Back-Fill $MyYear                         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      Z:\1-InitialSort.2\releases               ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -script "$MyScriptPath\2-AniAdd-Condense.groovy" --conflict index $FileBotSourceDirectory `
--output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

# Move everything left to Z:\1-InitialSort\
$FileBotSourceDirectory="Z:\1-InitialSort.2\"
$FIleBotDestDirectory="Z:\1-InitialSort\"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Move Everything                           ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      Z:\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory `
--format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" --db file --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory