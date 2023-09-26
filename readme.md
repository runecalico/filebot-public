# filebot datafiles & Scripts to share
## Data Files
- Synonyms & Short Names for AniDB entries [anime-synonyms.xml](datafiles/anime-synonyms.xml)
  - This file is created using a filebot script [generate_anidb_synonyms_from_aod.groovy](scripts/generate_anidb_synonyms_from_aod.groovy)
- local override files for [anime-raw-sorter-jwd.groovy](scripts/anime-raw-sorter-jwd.groovy) to set specific variable/data values
  - [series_basename_overrides.json](datafiles/series_basename_overrides.json) - Our Custom Overrides to use during Series Basename Generation (See seriesBasenameGenerator() in lib/sorter.groovy
  - [movies_basename_overrides.json](datafiles/movies_basename_overrides.json) - Our Custom Overrides to use during Movie Basename Generation (See searchForMoviesJWD() in lib/sorter.groovy
- Local file containing the default renaming rules to use with filebot rename() function
  - [initialSort_strict_series.groovy](datafiles/initialSort_strict_series.groovy)
  
## Currently only Filebot 4.9.x is supported. Filebot 5.x WILL NOT WORK!!!

## Scripts
- [generate_anidb_synonyms_from_aod.groovy](scripts/generate_anidb_synonyms_from_aod.groovy)
  - This filebot script parses the [Anime Offline Database](https://github.com/manami-project/anime-offline-database) json file, as well as the AniDB Title xml to generate a list of unique synonyms that do not exist currently in AniDB.  Why not use AOD directly? Because some of the data providers used to generate AOD will result in synonyms matching *multiple* AniDB series, this is a big problem for what I use this file for. It is in XML format mainly because at the time I was too lazy to implement a new search method, and so I made it xml and in the same format as the AnidB title XML so I could reuse that search code :)
- [anime-raw-sorter-jwd.ps1](scripts/anime-raw-sorter-jwd.ps1)
  - My pwsh (PowerShell) script that I use to manage the sorting and manipulation of downloaded Anime files.
- [anime-raw-sorter-jwd.groovy](scripts/anime-raw-sorter-jwd.groovy)
  - This filebot script is my <ins>D</ins>isaster <ins>I</ins>n <ins>P</ins>rogress that I use to match Anime (directly from release groups) Series and Seasons against AniDB. This script requires several "library" scripts in the [lib folder](scripts/lib/), and has minimal support for matching Series, OVA's, ONA's, OAD's and Movies to AniDB. 
  - WHY CREATE THIS WHEN AMC IS SO GOOD?
    - AMC (and filebot) is actually very good at matching *most* things, and fairly decent at getting the right *series* for Anime if you want to use TheTVDB, Seasons .. Not so much in part because Anime Release Groups seem to be really really bad at numbering and consistancy with the occasional inability to spell.
    - Add in a desire to have a fully automated 1st stage that organizes the raw incoming files (which for me can be thousands a week), and a general purpose script like AMC starts to show an unacceptable (to me) error rate (incorrect Series/Season).
    - AMC in STRICT mode has fairly low error rate, but also a fairly low match rate.
    - AMC in NON-STRICT mode has a high error rate, but will likely match *something* for almost every single file... This is especially notable for things not in TVDB or MovieDB, which tends to covery quite a few OVA, ONA, OAD and Movies.
  - SHOULD I USE THIS INSTEAD OF AMC?
    - Not really. If AMC works for you right now, please continue to use it. Heck if AMC *mostly* works for you, please continue to use it. 
  - ERR, OKAY. SO WHY DO YOU USE IT?
    - I use it to parse the inbound Anime from various Release Groups to sort that into series, movies and specials by the AniDB name (and ID). I DO NOT RENAME THE FILE! I DO NOT ADD XATTR INFO!
    - I use it instead of AMC because of the following:
      - DECREASED INCORRECT SERIES/SEASON matches compared to AMC Non-Strict, and to be in-line or beat AMC in Strict mode as well
      - Match more then AMC/Filebot does - Filebot/AMC are great tools, and do an amazing job in dealing with the insane combinations of file names, formats etc out there. However this means that it is likely tuned to be "good at all things", so a specific category like Release Group Anime works well, but not as well as something that is tuned ONLY for Release Group Anime.
      - The script is likely to "match" the "correct" Anime in AniDB much more often then AMC, however by default will not attempt to rename the files unless the match is "likely" a good one.
  - SO IT DOESN'T HAVE ANY FALSE MATCHES?
    - It does, tho at the moment (12/18/2020) it is within my initial scope of much lower then AMC in Non-Strict and in-line or better then AMC Strict mode.
    - False matches tend to be "data" mismatches more often then not. I'm well onto my 3rd+ re-write of most of the script, and it really isn't much more then a data matching script. The primary lookup data is the AniDB Title XML and the Anime Synonym XML that I create from AOD. If what the script uses for the Anime Series name doesn't match something in the lookup data, then it will not work.  So when Release Groups use names for the Anime that isn't from AniDB, or in AOD (and can be associated to an AniDB entry), then it's out of luck. This happens quite a bit, especially early in a season or for a series with ... Interesting naming conventions.
  - WELL I WANT TO RUN IT EVEN THO IT LIKELY WILL DISAPPOINT ME, HOW DO I?
    - WARNING!!!!! I HIGHLY recommend against changing the filename in your format expression,this is especially important for Movies!
    - WARNING!!!!! I HIGHLY recommend not adding XATTR information to the files (which adds episode info to them, and is likely not accurate)
    - I use this in steps (Powershell 7 btw)
- [2-AniAdd-Condense.groovy](scripts/2-AniAdd-Condense.groovy)
  - This script requires that directories follow a specific aniDB centric naming convention
    - `anime series [anidb-id]` for example:  `Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]`
  - What this script does is it looks for directories in one or more directories that ALSO exist in the output directory, and then moves all the files in to the corresponding output AniDB directory
  - I do this because sometimes the script doesn't properly detect the airdate of the episode, and I like to organize the current year's episodes into seasons.
  - So any series that it couldn't determine the airdate properly, will end up in the correct "season" folder eventually.
- [clear_xattr_all_files.groovy](scripts/clear_xattr_all_files.groovy)
  - Simple script to clears out all extended attributes on the files in the input path.
  - The extended info on downloaded files is **often** inconplete, or worse WRONG.
- [move_cjk_filenames.groovy](scripts/move_cjk_filenames.groovy)
  - Simple script to detect files that are in Chinese, Korean or Japanese and move them to another folder.
  - This is until I can create a good solution for translation, and keeping the original filename for later rework (much like rehydration in the main script)
- [move_english_audio_files.groovy](scripts/move_english_audio_files.groovy)
  - Script to detect files that *indicate* they have an English audio track.
  - At this time I have no way to actually determine if the audio track is actually english or labelled incorrectly.
- [move_ova_series.groovy](scripts/move_ova_series.groovy)
  - somewhat vain attempt to recreate the "ova" vs "series" that AniAdd uses, so when plex tries to lookup stuff in AniDB is matches the type.
  - It is really specific to my setup.
- [move_raw_groups_orphaned_subtitles.groovy](scripts/move_raw_groups_orphaned_subtitles.groovy)
  - Initial attempt to handle subtitles that are in child/nested directories from the video files that filebot doesn't detect normally.
- [restore_original_path.groovy](scripts/restore_original_path.groovy)
  - I add some xttr info to each file moved by [anime-raw-sorter-jwd.groovy](scripts/anime-raw-sorter-jwd.groovy) so that if I later determine the file was matched incorrectly I can have the ORIGINAL filename/directory recreated so I can try matching it again (usually to determine why it failed to match correctly)

## How to use these scripts?
  Step #1 - Update the [anime-raw-sorter-jwd.ps1](scripts/anime-raw-sorter-jwd.ps1) script to suit your needs.
  
Step #2 - Put all the scripts, and datafiles (excepting the xml as that would be downloaded automatically) into the same directory (I recommend a different directory then the one with your video files)

  Step #3 - Run it using pwsh
  ```powershell
  ./anime-raw-sorter-jwd.ps1
  ```
  Step #4 - Use AMC Non-Strict on whatever is left :) Tho I use a different output path then normal, so I know those files are from AMC Non-Strict and require manual verification more often then not.