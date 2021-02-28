$ACTION = "move"
$MYScriptPath = (Get-Location).Path
Remove-EnvPath -Path "C:\Program Files\FileBot\"
Add-EnvPath -PathAdd "C:\Users\xxxx\Desktop\Scripts\FileBot_4.9.3-portable-r8311"
$MyDate=Get-Date -Format "M_d_y_h_m_s"
$MyScriptOutput = "$MyScriptPath\anime-raw-sorter-jwd-$($MyDate).txt"

#I need to prune out the Known Non-English Crap
# Release Groups:
# [IDIF]
# [007nF]

# Seems that while I can clear the metadata from a file in the script, filebot will *cache* the metadata read and use it for certain commands like detectAnimeSeries ..
# So we need to clear all metadata from the files prior to trying to sort them.
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
$MyScript = "$MyScriptPath\clear_xattr_all_files.groovy"
write-output "//---                                                ---//" | Tee-Object -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Clear out all filebot metadata            ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript $FileBotSourceDirectory --log all | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

#$FileBotSourceDirectory = "Z:\FB-1\anime"
$FileBotSourceDirectory = "Z:\1-InboundRaw.PreSort\anime"
#$FileBotDestDirectory = "Z:\1-InitialSort"
$FileBotDestDirectory = "Z:\1-InitialSort.2"
$MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
$MYAnimeFormat= "$MYScriptPath\initialSort_strict_series.groovy"
# 1  - Strict Mode on Non-Movies WITH Xattr
write-output "//---                                                ---//" | Tee-Object -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Run Filebot to move Only Series - STRICT! ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION -rename --conflict index -r --def skipMovies=y clearXattr=y aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1  $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English | Tee-Object -Append -FilePath $MyScriptOutput
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
filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBSpecials=y useNonStrictOnAniDBMovies=y $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English | Tee-Object -Append -FilePath $MyScriptOutput
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
filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="runevitki/filebot" animeFormat=@$MYAnimeFormat  minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBFullMatch=y  $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

#Search for English Stuff (unfortunately it happens)
$FileBotSourceDirectory="Z:\1-InitialSort.2"
$FIleBotDestDirectory="Z:\1-InitialSort-English"
$MyScript = "$MYScriptPath/move_english_audio_files.groovy"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Sort out English Audio                    ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      W:\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot -script $MyScript --action $ACTION $FileBotSourceDirectory --conflict index --output $FIleBotDestDirectory --log all | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

# For now Back-fill 2021 stuff
$FileBotSourceDirectory="Z:\1-InitialSort.2\releases"
$FIleBotDestDirectory="Z:\1-InitialSort\2021\"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Back-Fill 2021                            ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      W:\1-InitialSort.2\releases               ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -script "$MyScriptPath\2-AniAdd-Condense.groovy" --conflict index $FileBotSourceDirectory --output $FIleBotDestDirectory --log fine | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}
# For now Back-fill 2020 stuff
$FileBotSourceDirectory="Z:\1-InitialSort.2\releases"
$FIleBotDestDirectory="Z:\1-InitialSort\2020\"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Back-Fill 2020                            ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      W:\1-InitialSort.2\releases               ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -script "$MyScriptPath\2-AniAdd-Condense.groovy" --conflict index $FileBotSourceDirectory --output $FIleBotDestDirectory --log fine | Tee-Object -Append -FilePath $MyScriptOutput
if ( $LastExitCode) {
    write-error "ERROR ERROR ERROR"
    exit
}

Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory

# Move everything left to Z:\1-InitialSort\
$FileBotSourceDirectory="Z:\1-InitialSort.2\"
$FIleBotDestDirectory="Z:\1-InitialSort\"
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//--- 1st: Move Everything                           ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---      W:\1-InitialSort.2                        ---//" | Tee-Object -Append -FilePath $MyScriptOutput
write-output "//---                                                ---//" | Tee-Object -Append -FilePath $MyScriptOutput
filebot --action $ACTION -rename --conflict index -r -non-strict $FileBotSourceDirectory --output $FileBotDestDirectory --format "{file.parent.substring($($FileBotSourceDirectory.Length))/fn}" --db file --log all | Tee-Object -Append -FilePath $MyScriptOutput
Remove-EmptyFolders -ParentFolder $FileBotSourceDirectory