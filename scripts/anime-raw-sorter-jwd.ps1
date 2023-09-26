[CmdletBinding()]
Param(
    [Parameter(Mandatory = $false)] [switch]$noCopy = $false,
    [Parameter(Mandatory = $false)] [switch]$breakAfterGroups = $false,
    [Parameter(Mandatory = $false)] [switch]$skipDrain = $false,
    [Parameter(Mandatory = $false)] [string]$LoggingLevel = "info",
    [Parameter(Mandatory = $false)] [string]$MySourceDriveLetter = "Z"
)

Function GenerateFolder($path) {
    $global:foldPath = $null
    foreach ($foldername in $path.split("\")) {
        $global:foldPath += ($foldername + "\")
        if (!(Test-Path $global:foldPath)) {
            New-Item -ItemType Directory -Path $global:foldPath
            # Write-Host "$global:foldPath Folder Created Successfully"
        }
    }
}

Function Remove-EmptyFolders {
    Param
    (
        [Parameter(Mandatory = $true)]
        $ParentFolder
    )
    try {
        Get-ChildItem -LiteralPath "$($ParentFolder)" -Recurse -Force | Where-Object { $_.PSIsContainer -and (Get-ChildItem -Literalpath "$($_.FullName)" -Recurse -Force | Where-Object { !$_.PSIsContainer }) -eq $null } | ForEach-Object { Remove-Item -LiteralPath "$($_.FullName)" -Force -Recurse }
    }
    catch {
        Write-Error "Error: $($_.Exception.Message)"
        Write-Error $_
    }
    #Delete Parent Folder if empty
    #If((Get-ChildItem -Path $Folder -force | Select-Object -First 1 | Measure-Object).Count -eq 0) {Remove-Item -Path $CATSFolder -Force -Recurse}
}

Function Assert-FolderIsEmpty {
    Param
    (
        [Parameter(Mandatory = $true)]
        $Folder
    )
    try {
        $TestPath = Get-ChildItem -File -Recurse $Folder -ErrorAction SilentlyContinue -ErrorVariable MsgErrTest -Force | Select-Object -First 1
    }
    catch {}
    If ($MsgErrTest) {
        Write-Error "Error accessing folder"
    }
    Else {
        # Folder can be accessed or is empty
        return [string]::IsNullOrEmpty($TestPath)
    }
}

$ACTION = "move"
$MYScriptPath = (Get-Location).Path
$MyDate = Get-Date -Format "M_d_y_h_m_s"
$MyYear = Get-Date -Format "yyyy"
$MyScriptOutput = "$MyScriptPath\logs\anime-raw-sorter-jwd-$($MyDate).txt"
$ENV:JAVA_OPTS = "-Dhttp.proxyHost=xxx.xxx.xxx.xxx -Dhttp.proxyPort=3128" # Remove if you are not using an HTTP proxy

if ($breakAfterGroups) {
    write-output "//---             breakAfterGroups SET TO y         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    $breakAfterGroupsValue = "y"
}
else {
    $breakAfterGroupsValue = "n"
}

if ($skipDrain) {
    write-output "//---             skipDrain SET TO y         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
}

# Record the version of filebot I'm using ..
$FilebotVersion = filebot -version
write-output "//---                                                ---//" | Tee-Object -FilePath $MyScriptOutput
write-output "//---               FILEBOT VERSION                  ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- $FilebotVersion" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput

## -------------------------------------------------------------------
## Tasks that are done to Incoming Download Folder
## -------------------------------------------------------------------
# Seems that while I can clear the metadata from a file in the script, filebot will *cache* the metadata read and use it for certain commands like detectAnimeSeries ..
# So we need to clear all metadata from the files prior to trying to sort them.
# Note: Files downloaded will often have *incorrect* metadata in them.
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.Incoming"
$MyScript = "$MyScriptPath\clear_xattr_all_files.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Clear out all filebot metadata (Incoming)  --//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript $FileBotSourceDirectory --log all *>&1 | Tee-Object -Append -FilePath $MyScriptOutput

####################################
# Signs of Special Chineese files
# All [GuodongSubs] files (so far)
# - https://guodongsubs.com/
# Any file where *everything* is brackets (except the extension of course)
# This Regex effectively finds all files that match this EXACTLY
# ^(\[[\w-\s\'&~]*\]|\([\w-\s]*\)){0,10}\.[^.]+$
# [Chinese Dub][EP12][1080P][Hardsub][English][MKV].mkv
# [GuodongSubs][The Beauty Blogger][Sheng Shi Zhuang Niang][ED][MKV].mkv
# Filebot/AMC tends to be bad with looking these up, so for now they likely will need "special" Care.
####################################
$ACTION = "move"
$FileBotSourceDirectory = "Z:\1-InboundRaw.Incoming"
$FileBotDestDirectory = "Z:\1-BracketNameFormat"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move [BracketName] Format                      ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "f.name =~ /^(\[[\w-\s\'&~.,_]*\]|\([\w-\s]*\)){0,10}(\.\d)?\.[^.]+$/"  --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput

$ACTION = "move"
$FileBotSourceDirectory = "Z:\1-InboundRaw.Incoming"
$FileBotDestDirectory = "Z:\1-[One Pace]"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move [One Pace] Format                         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "f.name =~ /\[One Pace\]/"  --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput

if ( -not $skipDrain ) {
    $ACTION = "move"
    $FileBotSourceDirectory = "Z:\1-InboundRaw.Incoming"
    $FileBotDestDirectory = "Z:\1-InboundRaw.PreSort\anime"
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- Move to Presort                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all  --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput
}
# Always remove any empty directories from the Input folder
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

## -------------------------------------------------------------------
## Filename/Data Manipulation on PreSort Folder
## -------------------------------------------------------------------

# Check for any CJK files and then translate them to english using Google Translate.
# Since I am doing this on the PreSort folder, the [Bracket] Files moved from Incoming folder may not
# be translated.
# This frequently results in valid english, but poor anime title matches.
# Aka the translated files are generally not accurate enough even for *me* to figure out the Anime title.
#$ACTION = "move"
#$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
#$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
## For now just move them instead of translating them.
#$MyScript = "$MyScriptPath\rename_cjk_filenames.groovy"
#write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
#write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
#write-output "//--- Translate CJK file names                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
#write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
#write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
#filebot --action $ACTION -rename --conflict index  -non-strict --def translateUrl=$ENV:TRANSLATE_URL `
#    -script $MyScript $FileBotSourceDirectory --output $FileBotDestDirectory  | Tee-Object -Append -FilePath $MyScriptOutput

# Move Audio (Maybe I'll do something with it later)
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundAudio"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move Everything Audio                         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "( f.isAudio() )" --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput

# Move anything that isn't considered Video by Filebot (basically)
# Unfortunately .mks looks to be a Subtitle format that Filebot doesn't recognize as one
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundNotVideo"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move Everything 'Not' Video/Subtitle           ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "( !f.isVideo() && !f.isSubtitle() && f.getExtension() != 'mks' )" --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" | Tee-Object -Append -FilePath $MyScriptOutput
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

# When we have a good translate process worked out, delete this move and enable the translate process
# earlier in the script.
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\foreign_characters"
# For now just move them instead of translating them
$MyScript = "$MyScriptPath\move_cjk_filenames.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Move CJK file names                            ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index  -non-strict --def translateUrl=$ENV:TRANSLATE_URL `
    -script $MyScript $FileBotSourceDirectory --output $FileBotDestDirectory  | Tee-Object -Append -FilePath $MyScriptOutput

# Check to see if there are any files we manually set to restore to their original file path/structure using FileBot metadata we created using the sorter script.
# The Sorter script will automatically add information on the ORIGINAL filename and relative ORIGINAL PATH.
# This is useful if the series was not matched correctly, I can rehydrate it in which to determine why (and run against updated code)
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime.rehydrate"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$MyScript = "$MyScriptPath\restore_original_path.groovy"
if (  -not (Assert-FolderIsEmpty $FileBotSourceDirectory) ) {
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- Rehydrate files                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot --action $ACTION --def clearxttr=yes -script $MyScript $FileBotSourceDirectory --output $FileBotDestDirectory --log all *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
}
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

# I can only assume it's a typo (Or spanish?), but some fansubs are using Episdio vs Episode .. So I'll just rename those ..
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Rename any 'episdio' files                     ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "(f.getName().toLowerCase().contains('episdio'))" `
    --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn.replaceAll(/(?i)Episdio/,'Episode')}"  | Tee-Object -Append -FilePath $MyScriptOutput

# Not entirely sure if it's typo or what, but -part is not very helpful.. So change <space>-part<space> to <space>- part<space>
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Rename any ' -part ' files                     ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --db file `
    --log all --filter "(f.getName().toLowerCase().contains(' -part '))" `
    --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn.replaceAll(/(?i)(\s-part\s)/,' - part ')}"  | Tee-Object -Append -FilePath $MyScriptOutput

# It's annoying but some RAW groups will include external subtitle files, but in a subfolder which makes filebot/sorter unable to associate the subtitle file with the video(s)
$ACTION = "move"
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$MyScript = "$MyScriptPath\move_raw_groups_orphaned_subtitles.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- Rename any Orphaned Subtitles from RAW Group   ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename --conflict index -non-strict $FileBotSourceDirectory --db file `
    --log all | Tee-Object -Append -FilePath $MyScriptOutput

## -------------------------------------------------------------------
## Anime Matching on PreSort Folder
## -------------------------------------------------------------------

$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InboundRaw.PreSort\anime"
$FileBotDestDirectory = "$($MySourceDriveLetter):\1-InitialSort.2"
$MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
$MYAnimeFormat = "$MYScriptPath\initialSort_strict_series.groovy"
# 1  - Strict Mode on Non-Movies WITH Xattr
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Run Filebot to move Only Series - STRICT! ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename --conflict index -r --def skipMovies=y clearXattr=y `
    aniDBuserAgent="runevitki/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 `
    renameExtras=y breakAfterGroups=$breakAfterGroupsValue  $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

if ( -not (Assert-FolderIsEmpty $FileBotSourceDirectory) ) {
    #2 - Non-Strict on Specials/Movies NO XATTR
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- 2nd: Run Filebot to move Specials/Movies - NONSTRICT! ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="runevitki/filebot" `
        animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 enableFileBotMovieLookups=y `
        useNonStrictOnAniDBSpecials=y useNonStrictOnAniDBMovies=y breakAfterGroups=$breakAfterGroupsValue $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
    if ($LastExitCode) {
        write-error "ERROR ERROR ERROR"
        exit
    }
}

if (  -not (Assert-FolderIsEmpty $FileBotSourceDirectory) ) {
#     3. - Allow Non-Strict on 1.0 Matches
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- 3rd: Run Filebot to move Series - NONSTRICT!         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                       ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="runevitki/filebot" `
        animeFormat=@$MYAnimeFormat  minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBFullMatch=y `
        breakAfterGroups=$breakAfterGroupsValue $FileBotSourceDirectory --output $FileBotDestDirectory --log $LoggingLevel --lang English *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
    if ($LastExitCode) {
        write-error "ERROR ERROR ERROR"
        exit
    }
    Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory
}

if ($noCopy) {
    write-output "//---             NO COPY SET TO TRUE                   ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    exit
}

## -------------------------------------------------------------------
## Anime Sorting on InitalSort Temp location
## -------------------------------------------------------------------

# Sort Movies/OVAs similiar to how I sort them in AniAdd. Not accurate with Specials on Movies/OVAs, but at lest it will sort
# Movies into the movies folder (based on the AniDB Entry)
$FileBotMovieDirectory = "$($MySourceDriveLetter):\1-InitialSort.2\movies"
$FileBotTVDirectory = "$($MySourceDriveLetter):\1-InitialSort.2\releases"
$FileBotOutputDirectory = "$($MySourceDriveLetter):\1-InitialSort.2\"
$MyScript = "$MYScriptPath/move_ova_series.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Sort out Subtitled Movies/OVA             ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      $($MySourceDriveLetter):\1-InitialSort.2\                  ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename --conflict index --def movieFolder=$FileBotMovieDirectory releaseFolder=$FileBotTVDirectory $FileBotMovieDirectory $FileBotTVDirectory --output $FileBotOutputDirectory --db file --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput


#Search for English Stuff
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InitialSort.2"
$FIleBotDestDirectory = "$($MySourceDriveLetter):\1-InitialSort-English"
$MyScript = "$MYScriptPath/move_english_audio_files.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Sort out English Audio                    ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      $($MySourceDriveLetter):\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION $FileBotSourceDirectory --conflict index --output $FIleBotDestDirectory `
    --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

# For now Back-fill "Year" stuff
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InitialSort.2\releases"
$FIleBotDestDirectory = "$($MySourceDriveLetter):\1-InitialSort\$MyYear\"
if (  -not (Assert-FolderIsEmpty $FileBotSourceDirectory) ) {
    GenerateFolder($FIleBotDestDirectory)
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- 1st: Back-Fill $MyYear                         ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---      $($MySourceDriveLetter):\1-InitialSort.2\releases               ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot --action $ACTION -script "$MyScriptPath\2-AniAdd-Condense.groovy" --conflict index $FileBotSourceDirectory `
        --output $FIleBotDestDirectory --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
    if ($LastExitCode) {
        write-error "ERROR ERROR ERROR"
        exit
    }
}

## -------------------------------------------------------------------
## Move to Initial Sort Location
## -------------------------------------------------------------------

# Move everything left to $($MySourceDriveLetter):\1-InitialSort\
$FileBotSourceDirectory = "$($MySourceDriveLetter):\1-InitialSort.2\"
$FIleBotDestDirectory = "$($MySourceDriveLetter):\1-InitialSort\"
if (  -not (Assert-FolderIsEmpty $FileBotSourceDirectory) ) {
    GenerateFolder($FIleBotDestDirectory)
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//--- 1st: Move Everything                           ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---      $($MySourceDriveLetter):\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
    filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory `
        --format "{file.parent.substring($( $FileBotSourceDirectory.Length ))/fn}" --db file --log $LoggingLevel *>&1 | Tee-Object -Append -FilePath $MyScriptOutput
}

Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory