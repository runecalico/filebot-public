# filebot datafiles & Scripts to share
## Data Files
- Synonyms & Short Names for AniDB entries [anime-synonyms.xml](datafiles/anime-synonyms.xml)
  - This file is created using a filebot script [generate_anidb_synonyms_from_aod.groovy](scripts/generate_anidb_synonyms_from_aod.groovy) 

## Scripts
- [generate_anidb_synonyms_from_aod.groovy](scripts/generate_anidb_synonyms_from_aod.groovy)
  - This filebot script parses the [Anime Offline Database](https://github.com/manami-project/anime-offline-database) json file, as well as the AniDB Title xml to generate a list of unique synonyms that do not exist currently in AniDB.  Why not use AOD directly? Because some of the data providers used to generate AOD will result in synonyms matching *multiple* AniDB series, this is a big problem for what I use this file for. It is in XML format mainly because at the time I was too lazy to implement a new search method, and so I made it xml and in the same format as the AnidB title XML so I could reuse that search code :)
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
  
  Step #1 - Clear out any existing filebot xattr (Frequently it's wrong, and filebot seems to automatically use cache it for use with some methods)
  ```powershell
  $ACTION = "move"
  $MYScriptPath = (Get-Location).Path
  $FileBotSourceDirectory = "FULL PATH TO DIRECTORY WITH FILES"
  $MyScript = "$MyScriptPath\clear_xattr_all_files.groovy"
  filebot -script $MyScript $FileBotSourceDirectory --log all
  ```  

  Step #2 - Run Filebot to move Only Series/Movies/Specials using strict!
  ```powershell
  $ACTION = "move"
  $MYScriptPath = (Get-Location).Path
  $MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
  $MYAnimeFormat= "$MYScriptPath\initialSort_strict_series.groovy"
  $FileBotSourceDirectory = "FULL PATH TO DIRECTORY WITH FILES"
  $FileBotDestDirectory = "FULL PATH TO OUTPUT DIRECTORY"
  filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def clearXattr=y aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1  $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English
  ```
 
  Step #3 - If you want to reduce false positivies, look at the output from step #2 prior to using non-strict with movies/specialTypes/Specials as Filebot doesn't have support for Movies, and OVA/ONA/OAD are .. kinda supported (even when the filename is useful) 
  ```powershell
    $ACTION = "move"
    $MYScriptPath = (Get-Location).Path
    $MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
    $MYAnimeFormat= "$MYScriptPath\initialSort_strict_series.groovy"
    $FileBotSourceDirectory = "FULL PATH TO DIRECTORY WITH FILES"
    $FileBotDestDirectory = "..."
    filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBSpecials=y useNonStrictOnAniDBMovies=y $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English
  ```  

  Step #4 - Look at the output from Step 2/3 if you want to reduce false matches on Anime Series. Run the script in NON-STRICT mode for "full AniDB matches". This is the step most likely to produce incorrect matches. I do not recommend using these switches as your first step. The primary reason to do the movies/specials non-strict prior is that movie detection kinda sucks and is primarily based on video length which can often not match series in AniDB that are classified as "movies", so they end up matching in the episode phase and using non-strict with episodes would match them :) this happens *more* often then the reverse.
  ```powershell
    $ACTION = "move"
    $MYScriptPath = (Get-Location).Path
    $MyScript = "$MyScriptPath\anime-raw-sorter-jwd.groovy"
    $MYAnimeFormat= "$MYScriptPath\initialSort_strict_series.groovy"
    $FileBotSourceDirectory = "FULL PATH TO DIRECTORY WITH FILES"
    $FileBotDestDirectory = "..."
    filebot -script $MyScript --action $ACTION -rename -no-xattr --conflict index -r --def aniDBuserAgent="nosuchuser/filebot" animeFormat=@$MYAnimeFormat  minFileSize=10 minLengthMS=5 aniDBXMLrefreshDays=1 useNonStrictOnAniDBFullMatch=y  $FileBotSourceDirectory --output $FileBotDestDirectory --log all --lang English
  ```  
  Step #4 - Use AMC Non-Strict on whatever is left :) Tho I use a different output path then normal, so I know those files are from AMC Non-Strict and require manual verification more often then not.