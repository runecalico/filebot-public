package lib
//--- VERSION 1.2.0

import net.filebot.web.TheTVDBSeriesInfo
import org.apache.commons.text.similarity.JaroWinklerDistance
import com.cedarsoftware.util.io.JsonObject
import net.filebot.web.Episode
import java.util.regex.Matcher

/**
 * Generate the base anime names we will be using to build possible Anime Series names to search for
 *
 * @param group The LinkedHashMap for this group of files
 * @param useBaseAnimeNameWithSeriesSyntax Should we add the Base Anime Name when the Series has "Series" Syntax
 * @return  ArrayList of [[group], baseGeneratedAnimeNames ]
 */
ArrayList basenameGenerator ( LinkedHashMap group, Boolean useBaseAnimeNameWithSeriesSyntax ) {
  tempBaseGeneratedAnimeNames = [] as HashSet
  baseGeneratedAnimeNames = [] as HashSet
  println '// START---------- Basename Generation ---------- //'
  //  log.finest "${groupInfoGenerator(group)}"
  // println "group.class:${group.getClass()}"
  println "--- group.anime - ${group.anime}"
  switch(group.anime) {
    // Because the actual romanization title has ... at the end in AniDB
    case ~/otome game no hametsu flag shika nai akuyaku reijou ni tensei shiteshimatta/:
      baseAnimeName =  'My Next Life as a Villainess: All Routes Lead to Doom!'
      break
    case ~/Pokemon XY/:
      baseAnimeName =  'Pocket Monsters XY'
      break
    case ~/motto! ojamajo doremi kaeru seki no himitsu/:
      baseAnimeName =  'Eiga Motto! Ojamajo Doremi Kaeru Ishi no Himitsu'
      break
    case ~/turning mecard w secret of vandine/:
      baseAnimeName =  'Turning Mecard W: Vandyne-ui Bimil'
      break
    // Spelling is important
    case ~/nantsu no taizai/:
      baseAnimeName =  'Nanatsu no Taizai'
      break
  // Spelling is important
    case ~/otona no bogaya san/:
      baseAnimeName =  'Otona no Bouguya-san'
      break
    case ~/girl gaku ~hijiri girls square gakuin~/:
      baseAnimeName =  'Girl Gaku. Sei Girls Square Gakuin'
      break
    case ~/girl gaku/:
      baseAnimeName =  'Girl Gaku. Sei Girls Square Gakuin'
      break
    case ~/hanyou no yashahime/:
      baseAnimeName =  'Han`you no Yashahime: Sengoku Otogizoushi'
      break
    case ~/sk/:
      // while yes, sk is a short for Shaman King, however more recently it's a widely used short for SK8 The Infinity, so let's go with that for now.
      // Tho why SK vs SK8 as the short name to use ... only the release groups using SK know ..
      baseAnimeName =  'SK8 the Infinity'
      break
    case ~/rezero kara hajimeru break time|re zero break time/:
      // AniDB has this as specials under Re:Zero kara Hajimeru Isekai Seikatsu
      baseAnimeName =  'Re:Zero kara Hajimeru Isekai Seikatsu'
      break
    case ~/digimon savers kyuukyoku power burst mode hatsudou!!/:
      baseAnimeName =  'Digimon Savers The Movie: Kyuukyoku Power! Burst Mode Hatsudou!!'
      break
    case ~/mushoku tensei/:
      baseAnimeName =  'Mushoku Tensei: Isekai Ittara Honki Dasu'
      break
    case ~/munou no nana/:
      baseAnimeName =  'Munou na Nana'
      break
    case ~/hanaukyo maid tai/:
      baseAnimeName =  'hanaukyou maid tai'
      break
    case ~/wixoss divalive/:
      baseAnimeName =  'Wixoss Diva(A)Live'
      break
    case ~/2 43 seiin volley bu/:
      baseAnimeName =  '2.43 Seiin Koukou Danshi Volley Bu'
      break
    case ~/the wonderful adventures of nils/:
      baseAnimeName =  'Nils no Fushigi na Tabi'
      break
    case ~/tatoeba last dungeon/:
      baseAnimeName = 'Suppose a Kid from the Last Dungeon Boonies Moved to a Starter Town'
      break
    case ~/evangelion 1 0 you are alone/:
      baseAnimeName =  'evangelion 1.0 you are not alone'
      break
    case ~/evangelion 1 11 - you are alone/:
      baseAnimeName =  'evangelion 1.0 you are not alone'
      break
    case ~/evangelion 2 0 you can advance/:
      baseAnimeName =  'evangelion 2.0 you can not advance'
      break
    case ~/evangelion 3 0 you can redo,/:
      baseAnimeName =  'evangelion 3.0 you can not redo,'
      break
    case ~/otome game/:
      baseAnimeName =  'My Next Life as a Villainess: All Routes Lead to Doom!'
      break
    case ~/otome game no hametsu/:
      baseAnimeName =  'My Next Life as a Villainess: All Routes Lead to Doom!'
      break
    case ~/gate - jieitai kanochi nite, kaku tatakaeri/:
      baseAnimeName =  'Gate: Thus the JSDF Fought There'
      break
    case ~/let's & go!! wgp/:
      baseAnimeName =  'Bakusou Kyoudai Lets Go!! WGP'
      break
    case ~/let s go wgp/:
      baseAnimeName =  'Bakusou Kyoudai Lets Go!! WGP'
      break
    case ~/recorder to randoseru re/:
      baseAnimeName =  'Recorder to Ransel Re'
      break
    case ~/recorder to randoseru do/:
      baseAnimeName =  'Recorder to Ransel Do'
      break
    case ~/recorder to randoseru mi/:
      baseAnimeName =  'Recorder to Ransel Mi'
      break
    case ~/pokmon journeys/:
      baseAnimeName =  'Pokemon Journeys: The Series'
      break
    case ~/carpe reborn/:
      baseAnimeName =  'Yuan Long'
      break
    case ~/yuan long - carp reborn/:
      baseAnimeName =  'Yuan Long'
      break
    case ~/maou-sama, petit retry!/:
      baseAnimeName =  'Maou-sama, Retry!'
      break
    case ~/ling long - ling cage - incarnation/:
      baseAnimeName =  'Ling Long: Incarnation Xia Ban Ji'
      break
    case ~/infinite stratos/:
      baseAnimeName =  'IS: Infinite Stratos'
      break
    case ~/pocket monsters twilight wings/:
      baseAnimeName =   'Pokemon: Twilight Wings'
      break
    case ~/anime kapibara san/:
      baseAnimeName =   'Anime Kabibarasan'
      break
    case ~/desolate era/:
      baseAnimeName =   'Mang Huang Ji'
      break
    case ~/kyou kara ore wa/:
      baseAnimeName =   'Kyou kara Ore wa!!'
      break
    case ~/higurashi no naku koro ni 2020/:
      baseAnimeName =   'Higurashi: When They Cry - Gou'
      break
    case ~/ace of diamond act/:
      baseAnimeName =  'Ace of the Diamond: Act'
      break
    case ~/isekai maou to shoukan/:
      baseAnimeName =  'Isekai Maou to Shoukan Shoujo no Dorei Majutsu'
      break
    case ~/Arifureta/:
      baseAnimeName =  'Arifureta Shokugyou de Sekai Saikyou'
      break
    case ~/honzuki no gekokujou - shisho ni naru tame ni wa shudan wo erandeiraremasen/:
      baseAnimeName =  'Ascendance of a Bookworm'
      break
    case ~/honzuki no gekokujou shisho ni naru tame ni wa shudan o erandeiraremasen/:
      baseAnimeName =  'Ascendance of a Bookworm'
      break
    case ~/milf isekai/:
      baseAnimeName =  'Do You Love Your Mom and Her Two-Hit Multi-Target Attacks'
      break
    case ~/my teenage romcom snafu climax/:
      baseAnimeName =  'My Teen Romantic Comedy SNAFU Climax'
      break
    case ~/redo of a healer/:
      baseAnimeName =  'Kaifuku Jutsushi no Yarinaoshi'
      break
    // As of 02/24/2021 AniDB has the TV episodes as type "other" in AniDB, which the Entry being a Movie. Filebot can't do anything with that.
    // Switch to the TVDB name (which probably will not match anything in AniDB)
    case ~/wave!! surfing yappe!!/:
      baseAnimeName =  'WAVE!! -Let\'s go surfing!!- (TV)'
      break
    default:
      baseAnimeName = jwdStringBlender(group.anime)
      break
  }
  println "----- baseAnimeName - ${baseAnimeName}"
  // ---------- Checking for Name Variations --- //
  // Anime names that don't really match well, so they need help
  // This could possibly be replaced by custom synonym file .. or maybe some kind of regex match this replace with this file?
  switch(baseAnimeName) {
    case ~/marulks daily life/:
      baseAnimeName = 'Made in Abyss: Dawn of the Deep Soul'
      group.isSpecialEpisode = true
      break
  }
  // Because mirai nikki (2011) is the AniDB name of the regular season, while mirai nikki is just the OVA.
  if ( baseAnimeName == 'mirai nikki' && !group.isSpecialType) {
    baseAnimeName = 'Mirai Nikki (2011)'
  }
  // Because relying on synonyms can be a hit or miss ..
  if ( baseAnimeName == 'shokugeki no souma' && ( group.seasonNumber == 4 || group.airdateSeasonNumber == 4 || group.seriesNumber == 4 || group.ordinalSeasonNumber == 4)  ) {
    baseAnimeName = 'Food Wars! The Fourth Plate'
    group.seasonNumber = null
    group.hasSeasonality = false
  }
  // unfortunately it's still 2020 ..
  if ( baseAnimeName == 'dragon quest dai no daibouken' && group.yearDateInName == "2021" ) {
    baseAnimeName = 'Dragon Quest: The Adventure of Dai'
    group.yearDateInName = null
  }
  if ( baseAnimeName == 'hunter x hunter' && group.yearDateInName == "2011" ) {
    baseAnimeName = ' Hunter x Hunter 2011'
    group.yearDateInName = null
  }
  // Helps for Mapping to BOTH AniDB & TVDB
  if ( baseAnimeName == 'dragon quest' && group.yearDateInName == "2020" ) {
    baseAnimeName = 'Dragon Quest: The Adventure of Dai'
    group.yearDateInName = null
  }
  // There is no such thing as a 3rd season on TheTVDB, or... technically on AniDB either.
  if ( baseAnimeName == 'mushishi' && group.airdateSeasonNumber == 3 ) {
    baseAnimeName = 'Mushishi Zoku Shou (2014)'
  }
  // There is no such thing as a 3rd season ..
  if ( baseAnimeName == 'strike witches dai 501 tougou sentou koukuudan road to berlin' && group.airdateSeasonNumber == 3) {
    baseAnimeName = 'strike witches'
  }
  // Close ... but not close enough to actually FIND it (it seems) on AniDB
  if ( group.anime == 'dungeon ni deai darou ka' && group.seriesNumber == 'iii' ) {
    baseAnimeName =  'Dungeon ni Deai o Motomeru no wa Machigatteiru Darou ka'
  }
  // Why use Roman numerals when the anime doesn't actually use them?
  if ( group.anime == 'to love ru darkness' && group.seriesNumber == 'ii' ) {
    baseAnimeName = 'To Love-Ru: Trouble - Darkness'
    group.seriesNumber = 2
  }
  // If it ends with Special or Bonus, remove that and add that as a basename.
  // VOID - myOVARegexMatcher = group.anime =~ /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?$/
  myOVARegexMatcher = group.anime =~ /(?i)([-\s(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s)]?$/
  if ( myOVARegexMatcher.find() ) {
    generatedAnimeName = group.anime.replaceAll(/(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?$/, '')
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    println "//--- TEMP: Adding to base Name List - [${generatedAnimeName}] "
  }
  if (!group.hasSeriesSyntax || (group.hasSeriesSyntax && useBaseAnimeNameWithSeriesSyntax) || (group.hasSeriesSyntax && group.order == 'airdate')) {
    println "//--- TEMP: Adding to base Name List - [${baseAnimeName}] "
    tempBaseGeneratedAnimeNames += ["${baseAnimeName}"]
  }
  if ( group.hasSeriesSyntax ) {
    println "//--- hasSeriesSyntax detected"
    hasSeasonality = true
    switch (group.seriesNumber) {
      case ~/[0-9]/:
        mySeasonalityNumber = group.seriesNumber.toInteger()
        println "----- Numerical Series - mySeasonalityNumber: ${mySeasonalityNumber}"
        hasRomanSeries = false
        break
      default:
        mySeasonalityNumber = group.seriesNumber
        println "----- Roman Series - mySeasonalityNumber: ${mySeasonalityNumber}"
        hasRomanSeries = true
        break
    }
    // baseAnimeName = "${jwdStringBlender(group.anime)}" // Always add the group.anime name
    // println "----- Adding Base Anime Name to base Name List - ${baseAnimeName}  - Season 1/0"
    // tempBaseGeneratedAnimeNames += ["${baseAnimeName}"]
    if ( !hasRomanSeries ) {
      if ( mySeasonalityNumber > 1 ) {
        // ---------- Add Series Name Varients as options ---------- //
        generatedAnimeName = baseAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
        println "----- Adding Ordinal Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
        tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
        if ( mySeasonalityNumber < 10 ) {
          generatedAnimeName = baseAnimeName + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
          println "----- Adding Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
          tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
        }
        generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime 2
        println "----- Adding Series # Anime Name to Anime Name List - ${generatedAnimeName}"
        tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
      }
    } else {
      generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime I/II/III/IV/V
      println "----- Adding Series Anime Name to Anime Name List - ${generatedAnimeName}"
      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
      switch (mySeasonalityNumber) {
        case ~/i/:
          mySeasonalityNumber = 1
          break
        case ~/ii/:
          mySeasonalityNumber = 2
          break
        case ~/iii/:
          mySeasonalityNumber = 3
          break
        case ~/iv/:
          mySeasonalityNumber = 4
          break
        case ~/v/:
          mySeasonalityNumber = 5
          break
        case ~/vi/:
          mySeasonalityNumber = 6
          break
        case ~/vii/:
          mySeasonalityNumber = 7
          break
        case ~/viii/:
          mySeasonalityNumber = 8
          break
        case ~/ix/:
          mySeasonalityNumber = 9
          break
        case ~/x/:
          mySeasonalityNumber = 10
          break
        default:
          mySeasonalityNumber = group.seriesNumber
          break
      }
      println "----- Roman Series - mySeasonalityNumber is now: ${mySeasonalityNumber}"
    }
  }
  if ( group.altTitle != null ) {
    println "----- Alternative Title Detected:[${group.altTitle}]"
    println "----- Adding Alternative Title to Anime Name List - ${group.altTitle}"
    tempBaseGeneratedAnimeNames += ["${group.altTitle}"]
  }
  tempBaseGeneratedAnimeNames.each { tempname ->
    println "----- BASE: Adding [${tempname}]"
    baseGeneratedAnimeNames += tempname
    baseGeneratedAnimeNames += ["${returnAniDBRomanization(tempname)}"]
  }
  // baseGeneratedAnimeNames = baseAnimeNameGenerator()
  //  log.finest "${groupInfoGenerator(group)}"
  println '// END---------- Basename Generation ---------- //'
  return [[group], baseGeneratedAnimeNames ]
}


/**
 * Generate a printable string that contains only the group values that are set.
 *
 * @param group The LinkedHashMap for this group of files
 * @return  String containing the group values that are set (not null)
 */
String groupInfoGenerator ( def group ) {
  def groupInfo = "Group: $group.anime, order: $group.order"
  if ( group.altTitle != null ) { groupInfo = groupInfo + ", altTitle: $group.altTitle" }
  if ( group.filebotMovieTitle != null ) { groupInfo = groupInfo + ", filebotMovieTitle: $group.filebotMovieTitle" }
  if ( group.order == 'airdate') { groupInfo = groupInfo + ", airdateSeasonNumber: $group.airdateSeasonNumber" }
  if ( group.isMovieType ) { groupInfo = groupInfo + ", mov: $group.isMovieType" }
  if ( group.isFileBotDetectedName ) { groupInfo = groupInfo + ", isFileBotDetectedName: $group.isFileBotDetectedName" }
  if ( group.hasSeriesSyntax ) { groupInfo = groupInfo + ", hasSeriesSyntax: $group.hasSeriesSyntax, seriesNumber: $group.seriesNumber" }
  if ( group.hasSeasonality ) { groupInfo = groupInfo + ", hasSeasonality: $group.hasSeasonality, seasonNumber: $group.seasonNumber" }
  if ( group.hasOrdinalSeasonality ) { groupInfo = groupInfo + ", hasOrdinalSeasonality: $group.hasOrdinalSeasonality, ordinalSeasonNumber: $group.ordinalSeasonNumber" }
  if ( group.hasPartialSeasonality ) { groupInfo = groupInfo + ", hasPartialSeasonality: $group.hasPartialSeasonality, partialSeasonNumber: $group.partialSeasonNumber" }
  if ( group.isSpecialType ) { groupInfo = groupInfo + ", isSpecialType: $group.isSpecialType" }
  if ( group.isSpecialEpisode ) { groupInfo = groupInfo + ", isSpecialEpisode: $group.isSpecialEpisode" }
  if ( group.specialType ) { groupInfo = groupInfo + ", specialType: $group.specialType" }
  if ( group.yearDateInName != null ) { groupInfo = groupInfo + ", yearDateInName: $group.yearDateInName" }
  if ( group.releaseGroup != null ) { groupInfo = groupInfo + ", releaseGroup: $group.releaseGroup" }
  return groupInfo
}

/**
 * Using a HashSet of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()] Search for
 * "matching" Anime in AniDB using JaroWinklerDistance to measure how close the Anime name is in AniDB to our search term.
 *
 * @param animeSeriesNames A Hashset of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()]
 * @param aniDBJWDResults A LinkedHashMap of the AniDB Results format we use
 * @param animeFoundInAniDB  Boolean representing if the Anime was found in AniDB
 * @param locale The Locale (aka Locale.English)
 * @param aniDBTitleXMLFilename  The local filename of the AniDB Offline XML file
 * @param aniDBSynonymXMLFilename The local filename of the AniDB Synonym XML File
 * @param useFilebotAniDBAliases Boolean: Should we use filebot Aliases for AniDB Series or use Synonyms/Aliases from AniDB XML files (I recommend not using Filebot Aliases)
 * @param animeOffLineDatabaseJsonObject  Json Object of the Anime Offline Database
 * @return  LinkedHashMap of [jwdresults: jwdResults, animeFoundInAniDB:animeFoundInAniDB]
 */
@SuppressWarnings('GrReassignedInClosureLocalVar')
LinkedHashMap filebotAnidbJWDSearch(HashSet animeSeriesNames, LinkedHashMap aniDBJWDResults, Boolean animeFoundInAniDB, Locale locale, String aniDBTitleXMLFilename, String aniDBSynonymXMLFilename, Boolean useFilebotAniDBAliases, JsonObject animeOffLineDatabaseJsonObject, LinkedHashMap aniDBCompleteXMLList) {
  aniDBTitleXML = new groovy.xml.XmlParser(false, false).parse(aniDBTitleXMLFilename) // XMLParser
  aniDBSynonymXML = new groovy.xml.XmlParser(false, false).parse(aniDBSynonymXMLFilename) // XMLParser
  LinkedHashMap jwdResults = aniDBJWDResults
  Boolean animeANIDBSearchFound = false
  String myQueryAniDB = ''
  HashSet myOptionsAniDB
  BigDecimal jwdcompare = 0
  BigDecimal jwdcompare2 = 0
//    def myTVDBseriesInfo
//    def myTBDBSeriesInfoAliasNames
//    def gotAniDBID
  ArrayList myAniDBOMTitles = []
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  animeSeriesNames.each { series ->
    // ---------------------------------- //
    // ---------- Search AniDB ---------- //
    // ---------------------------------- //
    // println "...AniDB"
    animeANIDBSearchFound = false
    myQueryAniDB = series
    myOptionsAniDB = AniDB.search(myQueryAniDB, locale) as HashSet
//    println "myOptionsAniDB Class : ${myOptionsAniDB.getClass()}"
    if (myOptionsAniDB.isEmpty()) {
//       log.finest "TV Series not found in AniDB by FileBot: $myQueryAniDB"
      // --- We are searching AniDB XML Titles because occationally filebot will not find something, but the Title search will.
      // --- And while it's slow as hell, I'm not going to do this all the time ..
//      myOptionsAniDB += anidbXMLTitleSearch(aniDBTitleXML, ["${myQueryAniDB}"] as Set, locale, false, false, false, 3)
//      myOptionsAniDB += anidbXMLTitleSearch(aniDBSynonymXML, ["${myQueryAniDB}"] as Set, locale, false, false, false, 3)
      myOptionsAniDB += anidbHashTitleSearch(aniDBCompleteXMLList, ["${myQueryAniDB}"] as Set, locale, false, false, false, 3)
      if (myOptionsAniDB.isEmpty()) {
//         log.finest "TV Series not found in AniDB by AniDB XML Title Search: $myQueryAniDB"
      } else {
//         log.fine "Our Query Returned from AniDB: ${myOptionsAniDB}"
        animeANIDBSearchFound = true
      }
    } else {
      // TODO
      // Filebot returns ' while AniDB returns `, so sometimes *effective* duplicates will occur in myOptionsAniDB
      // which waste processing time.
//       log.fine "Our Query Returned from AniDB: ${myOptionsAniDB}"
      animeANIDBSearchFound = true
//       println "Filebot Returned ${myOptionsAniDB.size()} Titles:${myOptionsAniDB} :::FOR::: ${myQueryAniDB}"
      // --- Return AID as there are a few edge cases where there are in fact multiple titles with the EXACT same words, but
      // --- one might be the Official title, while the Other the Main Title, or set as different languages etc.
      // --- So returning only the title will mean we *might* not get the actual AID from the query, so return the AID
      // --- This also means we don't have to lookup the AID in the next stage as well :)
//      myOptionsAniDB += anidbXMLTitleSearch(aniDBTitleXML, ["${myQueryAniDB}"] as Set, locale, true, false, false, 3)
//      myOptionsAniDB += anidbXMLTitleSearch(aniDBSynonymXML, ["${myQueryAniDB}"] as Set, locale, true, false, false, 3)
      myOptionsAniDB += anidbHashTitleSearch(aniDBCompleteXMLList, ["${myQueryAniDB}"] as Set, locale, true, false, false, 3)
//       println "After XMLTitleSearch ${myOptionsAniDB.size()} Titles:${myOptionsAniDB} :::FOR::: ${myQueryAniDB}"
    }
    if ( animeANIDBSearchFound ) {
      animeFoundInAniDB = true
      // ---------- Parse Series Results ---------- //
      myOptionsAniDB.each { results ->
//         log.fine "Comparing Search Result - ${results}"
        // ---------- START - Compile Aliases for Current Result ---------- //
        try {
          // println "Get Series Information for Aliases"
          // Between some wierdness with the Aliases returned and wanting to reduce the API call's to AniDB
          // Switch to getting the Series info from AniDB Title XML
          // Filebot doesn't return aliases for AniDB when using query by ID
          if ( useFilebotAniDBAliases ) {
            myTVDBseriesInfo = AniDB.getSeriesInfo(results, locale)
          }
          // myTVDBseriesInfo = AniDB.getSeriesInfo(results, locale)
          // Literal Match will not work as Filebot Search returns are *slightly* different (encoding differences?) aka filebot uses ', while AniDB XML is `
          // So we need to change it to suit (hopefully it will not end up being a ongoing and expanding issue"
          if ( results.toString().isInteger() ) {
            gotAniDBID = results as Integer
//            println "Got AID: ${gotAniDBID}"
          } else {
            // gotAniDBID = anidbXMLTitleSearch(aniDBTitleXML, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
            gotAniDBID = anidbHashTitleSearch(aniDBCompleteXMLList, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
            if ( gotAniDBID <= 0 ) {
//              gotAniDBID = anidbXMLTitleSearch(aniDBSynonymXML, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
              gotAniDBID = anidbHashTitleSearch(aniDBCompleteXMLList, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
//              println "Searching Synonyms.xml returned: ${gotAniDBID}"
            }
//            println "Got AID: ${gotAniDBID} for ${results}"
          }
          // println "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}" // No info on # of Episodes
          // myTVDBseriesInfo properties for Sword Art Online II: [runtime:null, startDate:2014-07-05, genres:[], certification:null, rating:5.26, id:10376, name:Sword Art Online II, network:null,
          // ratingCount:null, type:Anime, class:class net.filebot.web.SeriesInfo, spokenLanguages:[], order:Absolute, status:null, language:en,
          // aliasNames:[????????????II, ???????????II, Gun Art Online, Sword Art Online 2, Sword Art Online II: Calibur, Sword Art Online II: Mother's Rosario, Sword Art Online II: Phantom Bullet, GGO, SAO 2, SAO2, SAOII], database:AniDB]
          // println "${myTVDBseriesInfo.properties}"
          // I am not sure exactly why, but the alias info returned on AniDB series FREQUENTLY does not match what's in the anime-titles.xml
          // Sometimes WILDLY AND INCORRECTLY so (as in aliases for different series)
          // myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames // Unfortunately this does NOT always include Synonyms! (but sometimes it does)
          // println "myTBDBSeriesInfoAliasNames: ${myTBDBSeriesInfoAliasNames}"
          // println "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}"
//           println "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}"
//          println "myTBDBSeriesInfoAliasNames Class : ${myTBDBSeriesInfoAliasNames.getClass()}"
          // gotAniDBID = myTVDBseriesInfo.id
          // teir1JWDResults += [[db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, score: jwdcompare]]
        } catch (Exception ex) {
          myTVDBseriesInfo = []
          myTBDBSeriesInfoAliasNames = []
          gotAniDBID = 0
        }
        // log.fine "Our AniDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
        // log.fine "// -------------------- //"
        if ( useFilebotAniDBAliases ) {
          myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames // Unfortunately this does NOT always include Synonyms! (but sometimes it does)
        } else {
          // For some reason beyond my comprehension, Filebot includes aliases for multiple seasons of My Teen Romantic Comedy: SNAFU aka Yahari Ore no Seishun LoveCome wa Machigatte Iru.
          // So just zero them out and let the XML take over..
          myTBDBSeriesInfoAliasNames = []
        }
//        println "myTBDBSeriesInfoAliasNames Class : ${myTBDBSeriesInfoAliasNames.getClass()}" // java.util.ArrayList
        if ( gotAniDBID > 0 ) {
          def anidbAnimeEntrySearchResult = anidbXMLGetAnimeEntry(aniDBTitleXML, gotAniDBID)
//          println "anidbAnimeEntrySearchResult Class : ${anidbAnimeEntrySearchResult.getClass()}" // groovy.util.Node
          def anidbAnimeEntryTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntrySearchResult)
//          println "anidbAnimeEntryTitle Class : ${anidbAnimeEntryTitle.getClass()}" // groovy.util.Node
          // println "---> anidbAnimeEntryTitle: ${anidbAnimeEntryTitle.text()}"
          myTVDBseriesInfo = [ 'id': gotAniDBID, 'name': anidbAnimeEntryTitle.text()]
//          println "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}" // java.util.LinkedHashMap
          myAniDBOMTitles = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntrySearchResult)
//          println "myAniDBOMTitles Class : ${myAniDBOMTitles.getClass()}" // class java.util.ArrayList
          // println "-----> myAniDBOMTitles: ${myAniDBOMTitles}"
          List anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntrySearchResult)
          // println "-----> anidbAnimeEntrySynonyms: ${anidbAnimeEntrySynonyms}"
          List anidbAnimeEntryShorts = anidbXMLEntryGetAnimeShorts(anidbAnimeEntrySearchResult)
          // println "-----> anidbAnimeEntryShorts: ${anidbAnimeEntryShorts}"
          // println "XML Alias/Shorts: ${anidbAnimeEntryShorts.size() + anidbAnimeEntrySynonyms.size()}"
          // println "myTBDBSeriesInfoAliasNames: ${myTBDBSeriesInfoAliasNames.size()}"
          myTBDBSeriesInfoAliasNames += anidbAnimeEntrySynonyms
          myTBDBSeriesInfoAliasNames += anidbAnimeEntryShorts
          // log.fine "After XML we have AniDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
          // log.fine "// -------------------- //"
          // Additional Synonyms
          def anidbAnimeEntrySearchResultSynonyms = anidbXMLGetAnimeEntry(aniDBSynonymXML, gotAniDBID)
          // println "-----> anidbAnimeEntrySearchResultSynonyms: ${anidbAnimeEntrySearchResultSynonyms}"
          if ( anidbAnimeEntrySearchResultSynonyms != null) {
            // def anidbAnimeEntryTitleSynonyms = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntrySearchResultSynonyms)
            // println "-----> anidbAnimeEntryTitleSynonyms: ${anidbAnimeEntryTitleSynonyms}"
            anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntrySearchResultSynonyms)
            // println "-----> anidbAnimeEntrySynonyms: ${anidbAnimeEntrySynonyms}"
            myTBDBSeriesInfoAliasNames += anidbAnimeEntrySynonyms
          }
        }
        // ---------- END - Compile Aliases for Current Result ---------- //
        myAniDBOMTitles.each { myTitle ->
//           println "Running JWDComparision of BOM Title - ${myTitle} to ${series}"
          jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(myTitle.toString()), altjwdStringBlender(series.toString()))
//           log.finest "jaroWinklerDistance of ${jwdStringBlender(results.toString())} TO ${jwdStringBlender(series.toString())}: ${jwdcompare}"
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            // Get/Set AnimeType from AnimeOfflineDatabase
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            // println "lower"
          }
        }
        myTBDBSeriesInfoAliasNames.each { aliases ->
          // myRegexMatcher = aliases =~ /\?\?\?\?/ // This doesn't work, The string just doesn't display correctly .. ^[ -~]*$
          myRegexMatcher = aliases =~ /^[ -~]*$/
          if ( myRegexMatcher.find() ) {
            // println "AniDB Aliases: English Name: ${aliases}"
          } else {
            // println "AniDB Aliases: Not So English Name: ${aliases}"
            return
          }
//           log.finest "Running JWDCompare of  Alias - ${aliases} to ${series}"
          jwdcompare2 = jaroWinklerDistance.apply(altjwdStringBlender(aliases.toString()), altjwdStringBlender(series.toString()))
//           println "altjwdcompare2 of ${altjwdStringBlender(aliases.toString())} and ${altjwdStringBlender(series.toString())}"
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            // println "lower"
          }
        }
        // ---------- END Parse Aliases for Current Result ---------- //
      }
      // ---------- END Parse Aliases for Current Result ---------- //
    }
    // ---------- END Search AniDB ---------- //
  }
  return [jwdresults: jwdResults, animeFoundInAniDB:animeFoundInAniDB]
}

/**
 * Using a HashSet of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()] Search for
 * "matching" Anime in TheTVDB using JaroWinklerDistance to measure how close the Anime name is in TheTVDB to our search term.
 *
 * @param animeSeriesNames A Hashset of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()]
 * @param tvDBJWDResults A LinkedHashMap of the TheTVDB Results format we use
 * @param animeFoundInTVDB  Boolean representing if the Anime was found in AniDB
 * @param locale The Locale (aka Locale.English)
 * @return LinkedHashMap of [jwdresults: jwdResults, animeFoundInTVDB:animeFoundInTVDB]
 */
@SuppressWarnings('GrReassignedInClosureLocalVar')
LinkedHashMap filebotTVDBJWDSearch(HashSet animeSeriesNames, LinkedHashMap tvDBJWDResults, Boolean animeFoundInTVDB, Locale locale) {
  LinkedHashMap jwdResults = tvDBJWDResults
  Boolean animeTVDBSearchFound = false
  String myQueryTVDB = ''
  ArrayList myOptionsTVDB
  TheTVDBSeriesInfo myTVDBseriesInfo
  ArrayList myTBDBSeriesInfoAliasNames
  BigDecimal jwdcompare = 0
  BigDecimal jwdcompare2 = 0
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  animeSeriesNames.each { series ->
    animeTVDBSearchFound = false
    // println "Looking for Best Match for ${series}"
    // ---------- Start with TheTVDB ---------- //
    // println "...TheTVDB"
    myQueryTVDB = series
//    println "myQueryTVDB.getclass:[${myQueryTVDB.getClass()}]" // java.lang.String
    myOptionsTVDB = TheTVDB.search(myQueryTVDB, locale)
//    println "myOptionsTVDB.getclass:[${myOptionsTVDB.getClass()}]" // java.util.ArrayList
    if (myOptionsTVDB.isEmpty()) {
      // log.warning "TV Series not found in TheTVDB: $myQueryTVDB"
    } else {
      // println "Our Query Returned from TheTVDB: ${myOptionsTVDB}"
      animeTVDBSearchFound = true
    }
    if ( animeTVDBSearchFound ) {
      // ---------- Parse Series Results ---------- //
      //    --- Because it seems that TheTVDB can sometimes have "invalid" series return in the list from TheTVDB.Search, that you can't determine until you try a TheTVDB.getSeriesInfo on them
      //    --- We need to run the getSeriesInfo right away before any jaroWinklerDistance comparisions, so we can skip the Series if it's "invalid"
      myOptionsTVDB.each { results ->
        // println "Comparing Search Result - ${results}"
        // Need to replicate this kind of try/catch/check value for all TheTVDB/AniDB lookups
        try {
          // println "Get Series Information - ${results}"
          myTVDBseriesInfo = TheTVDB.getSeriesInfo(results, locale)
          myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames
          // println "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}" // No info on # of episodes
          // myTVDBseriesInfo properties for Sword Art Online: [runtime:25, startDate:2012-07-07, genres:[Action, Adventure, Animation, Anime, Fantasy, Romance, Science Fiction],
          // overview:In the near future, a Virtual Reality Massive Multiplayer Online Role-Playing Game (VRMMORPG) called Sword Art Online has been released where players control their avatars with their bodies using a piece of technology called Nerve Gear. One day, players discover they cannot log out, as the game creator is holding them captive unless they reach the 100th floor of the game's tower and defeat the final boss. However, if they die in the game, they die in real life. Their struggle for survival starts now...,
          // certification:TV-14, rating:7.9, id:259640, slug:sword-art-online, lastUpdated:1599661699, name:Sword Art Online, network:Tokyo MX, ratingCount:18128,
          // imdbId:tt2250192, type:TV Series, class:class net.filebot.web.TheTVDBSeriesInfo, spokenLanguages:[], status:Continuing,
          // order:null, language:en, airsTime:12:00 AM, aliasNames:[Sword Art Online II, S?do ?to Onrain, Sword Art Online Alicization, Sword Art Online Alicization: War of Underworld, SAO, S.A.O, Sword Art Online : Alicization, Sword Art Online : Alicization - War of Underground, Sword Art Online II , S.A.O 2, S.A.O 3, S.A.O 4, S.A.O II, S.A.O III, S.A.O IV, SAO 2, SAO 3, SAO 4, SAO II, SAO III, SAO IV, Sword Art Online 2, Sword Art Online 3, Sword Art Online 4, Sword Art Online III, Sword Art Online IV, ????, Sword Art Online (2012), ?????? ???? ???-????, ?????? ???? ???????, ????????????, ???????????, ?? ?? ???, ??????? ???? ??????, ????],
          // season:4, airsDayOfWeek:Sunday, database:TheTVDB]
          // println "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}"
//          println "myTBDBSeriesInfoAliasNames.getclass:[${myTBDBSeriesInfoAliasNames.getClass()}]" // java.util.ArrayList
        } catch (Exception ex) {
          myTVDBseriesInfo = []
          myTBDBSeriesInfoAliasNames = []
        }
        if (myTVDBseriesInfo == []) {
          // log.warning "Can not get Series Info for ${results} in TheTVDB - []"
          return // Skip to the next result
        }
        animeFoundInTVDB = true
        jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(results.toString()), altjwdStringBlender(series.toString()))
        if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
          // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
          hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false, hasAnimeListEntry: hasAnimeListEntry]]
        } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
          // println "higher"
          hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false, hasAnimeListEntry: hasAnimeListEntry]]
        } else {
          // println "lower"
        }
        // ---------- Parse Aliases for Current Result ---------- //
        myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames
        // log.fine "Our TheTVDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
        myTBDBSeriesInfoAliasNames.each { aliases ->
          // myRegexMatcher = aliases =~ /\?\?\?\?/
          myRegexMatcher = aliases =~ /^[ -~]*$/
          if ( myRegexMatcher.find() ) {
            // println "TheTVDB Aliases: English Name: ${aliases}"
          } else {
            // println "TheTVDB Aliases: Not So English Name: ${aliases}"
            return
          }
          // log.finest "Comparing Alias - ${aliases}"
          jwdcompare2 = jaroWinklerDistance.apply(altjwdStringBlender(aliases.toString()), altjwdStringBlender(series.toString()))
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            // println "lower"
          }
        }
        // ---------- END Parse Aliases for Current Result ---------- //
      }
      // ---------- END Parse Series Results ---------- //
    }
    // ---------- END Start with TheTVDB ---------- //
  }
  return [jwdresults: jwdResults, animeFoundInTVDB:animeFoundInTVDB]
}

/**
 * Using a HashSet of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()] Search for
 * "matching" Anime in AniDB using JaroWinklerDistance to measure how close the Anime name is in AniDB to our search term.
 *
 * @param animeSeriesNames A Hashset of Anime Series Names [Generated by basenameGenerator(), then seriesnameGenerator()]
 * @param aniDBTitleXMLFilename  The local filename of the AniDB Offline XML file
 * @param aniDBSynonymXMLFilename The local filename of the AniDB Synonym XML File
 * @param useFilebotAniDBAliases Boolean: Should we use filebot Aliases for AniDB Series or use Synonyms/Aliases from AniDB XML files (I recommend not using Filebot Aliases)
 * @param locale The Locale (aka Locale.English)
 * @param animeOffLineDatabaseJsonObject  Json Object of the Anime Offline Database
 * @return  LinkedHashMap of [firstANIDBWTMatchNumber: firstANIDBWTMatchNumber, firstAniDBWTMatchName:firstAniDBWTMatchName, anidbFirstMatchDetails: anidbFirstMatchDetails, secondANIDBWTMatchNumber: secondANIDBWTMatchNumber, secondAniDBWTMatchName:secondAniDBWTMatchName, anidbSecondMatchDetails: anidbSecondMatchDetails, thirdANIDBWTMatchNumber: thirdANIDBWTMatchNumber, thirdAniDBWTMatchName:thirdAniDBWTMatchName, anidbThirdMatchDetails:anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails: fileBotANIDBJWDMatchDetails, fileBotANIDBJWDMatchNumber: fileBotANIDBJWDMatchNumber, animeFoundInAniDB: animeFoundInAniDB, statsANIDBJWDFilebotOnly:statsANIDBJWDFilebotOnly, statsANIDBFilebotMatchedScript:statsANIDBFilebotMatchedScript]
 */
@SuppressWarnings('GroovyUnusedAssignment')
LinkedHashMap searchForMoviesJWD(LinkedHashMap group, String aniDBTitleXMLFilename, String aniDBSynonymXMLFilename, Boolean useFilebotAniDBAliases, Locale locale, JsonObject animeOfflineDatabase, LinkedHashMap aniDBCompleteXMLList) {
  // ---------- Set Variables ---------- //
  anidbJWDResults = [:]
  filteredanidbJWDResults = [:]
  fileBotAniDBJWDResults = [:]
  filteredfileBotAniDBJWDResults = [:]
  animeFoundInAniDB = false
  fileBotAniDBMatchUsed = false
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
  HashSet tempBaseGeneratedAnimeNames = []
  HashSet baseGeneratedAnimeNames = []
  HashSet baseAnimeNames = []
  HashSet filebotBaseAnimeNames = []
  Integer statsANIDBJWDFilebotOnly = 0
  Integer statsANIDBFilebotMatchedScript = 0
  // Search AniDB for 1st, 2nd, 3rd match
  // Use Filebot AltTitle as fallback (since it's MovieDB)?
  // Rename (Add option to use Non-Strict on rename)
  // ---------- Basename Generation ---------- //
  println '// START---------- Basename Generation ---------- //'
  //  if ( group.altTitle != null ) {
  //    baseAnimeNames += ["${group.alttitle}"]
  //    baseAnimeNames += ["${ returnAniDBRomanization(group.alttitle) }"]
  //  }
  println "--- TEMP: Adding group.anime - [${group.anime}]"
  tempBaseGeneratedAnimeNames = ["${group.anime}"]
  // myMovieRegexMatcher = group.anime =~ /(?i)(\bmovie\s[\d]{1,3}|\bmovie)\b/
  println '--- Checking if we should add variations based on movie keyword'
  // VOID - Matcher myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}|\smovie))\b/
  Matcher myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/, '')
    println "----- TEMP: Adding 'movie' keyword variation #1 - [${animeTemp}]" // Removing The, Movie and any 3 digits after movie
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  // VOID - myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie)\s(?!\d))/
  myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie(?!\s\d)))/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(\s?(the)?(\smovie))/, '')
    println "----- TEMP: Adding 'movie' keyword variation #2 - [${animeTemp}]" // Removing The, Movie but leaving the digits
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(?<=movie\s)([\d]{1,3})/, '')
    println "----- TEMP: Adding 'movie' keyword variation #3 - [${animeTemp}]" // Removing the digits after movie
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  tempBaseGeneratedAnimeNames.each { tempname ->
    println "--- BASE: Adding [${tempname}]"
    baseGeneratedAnimeNames += tempname
    baseGeneratedAnimeNames += ["${returnAniDBRomanization(group.anime)}"]
  }
  println '// END---------- Basename Generation ---------- //'
  println '// START---------- SeriesName Generation ---------- //'
  baseGeneratedAnimeNames.each { basename ->
    println "//--- Generating Possible Anime Series Names for ${basename}"
    println "--- Adding ${basename}"
    baseAnimeNames += ["${basename}"]
    println '--- Checking if we should add variations based on Gekijouban keyword'
    myMovieRegexMatcher = basename =~ /(?i)(Gekijouban)/
    if ( !myMovieRegexMatcher.find() ) {
      animeTemp = 'Gekijouban ' + basename
      println "----- Adding 'Gekijouban' keyword variation - [${animeTemp}]"
      baseAnimeNames += ["${animeTemp}"]
    }
    // Taken from groupGeneration
    // VOID - (?i)(~\s(.*))$
    mySanityRegexMatcher = basename =~ /(?i)(~(.*))$/
    if (mySanityRegexMatcher.find() ) {
      mySanityAltTxt = mySanityRegexMatcher[0][2]
      println "----- Adding possible Alternative Title: [${mySanityAltTxt}] using ~"
      baseAnimeNames += ["${mySanityAltTxt}"]
      animeTemp = basename.replaceAll(/(?i)(~(.*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "----- Adding possible Alternative Title: [${animeTemp}] using ~"
      baseAnimeNames += ["${animeTemp}"]
    }
    // (?i)(-\s(.*))$
    mySanityRegexMatcher = basename =~ /(?i)(-\s(.*))$/
    if (mySanityRegexMatcher.find() ) {
//      mySanityAltTxt = mySanityRegexMatcher[0][2]
      animeTemp = basename.replaceAll(/(?i)(-\s(.*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      println "-----  Adding Title Text Variation [${animeTemp}] using -"
      baseAnimeNames += ["${animeTemp}"]
    }
  }
  if ( group.filebotMovieTitle != null ) {
//    println "group.filebotMovieTitle:[${group.filebotMovieTitle}]"
    // VOID myMovieRegexMatcher = myFileNameForParsing =~ /(?i)((^[a-z\s]+)\(?((19\d\d|20\d\d)\)?\s))/
    // VOID myMovieRegexMatcher = group.filebotMovieTitle =~ /(?i)((^[a-z\s-]+)\(?((19\d\d|20\d\d)\)?\b))/
    // VOID myMovieRegexMatcher = group.filebotMovieTitle =~ /(?i)((^[^\d\(]+)\(?((19\d\d|20\d\d)\)?\b))/ // Thank you Intellij, but the escape is needed
    // TODO
    // See if we can look for the date either at the end of the title or evalute all (), due to edge cases like this
    // Evangelion: 1.0 You Are (Not) Alone (2007) -it does't recognize the date ..
    myMovieRegexMatcher = group.filebotMovieTitle =~ /(?i)((^[^\(]+)\(?((19\d\d|20\d\d)\)?\b))/
    if ( myMovieRegexMatcher.find() ) {
      filebotBaseAnimeNames = ["${myMovieRegexMatcher[0][2]}"]
      filebotBaseAnimeNames += ["${group.filebotMovieTitle}"]
    } else {
      filebotBaseAnimeNames = ["${group.filebotMovieTitle}"]
    }
  }
  println '// END---------- Series Name Generation ---------- //'
  println '-----'
  println '-----'
  println "  We are going to be searching for these Anime Series Names: ${baseAnimeNames} with AniDB "
  if ( filebotBaseAnimeNames ) {
    println "  We are going to be searching for these Anime Series Names: ${filebotBaseAnimeNames} with AniDB from FileBot"
  }
  println '-----'
  println '-----'
  returnThing = filebotAnidbJWDSearch( baseAnimeNames, anidbJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  anidbJWDResults = returnThing.jwdresults
  animeFoundInAniDB = returnThing.animeFoundInAniDB
  if ( group.filebotMovieTitle != null ) {
    returnThing2 = filebotAnidbJWDSearch( filebotBaseAnimeNames, fileBotAniDBJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
    fileBotAniDBJWDResults = returnThing2.jwdresults
    animeFoundInAniDB = returnThing2.animeFoundInAniDB
  }
  if (animeFoundInAniDB) {
//    println "animeFoundInAniDB:[${animeFoundInAniDB}]"
//    println "anidbJWDResults:[${anidbJWDResults}]"
//    println "fileBotAniDBJWDResults:[${fileBotAniDBJWDResults}]"
    filteredanidbJWDResults = anidbJWDResults.findAll { results ->
      aodIsAIDTypeNotTV(animeOfflineDatabase, results.value.dbid as Integer)
    }
    filteredfileBotAniDBJWDResults = fileBotAniDBJWDResults.findAll { results ->
      aodIsAIDTypeNotTV(animeOfflineDatabase, results.value.dbid as Integer)
    }
    if ( filteredanidbJWDResults.isEmpty() && filteredfileBotAniDBJWDResults.isEmpty() ) {
      animeFoundInAniDB = false
    }
//    println "animeFoundInAniDB:[${animeFoundInAniDB}]"
//    println "filteredanidbJWDResults:[${filteredanidbJWDResults}]"
//    println "filteredfileBotAniDBJWDResults:[${filteredfileBotAniDBJWDResults}]"
  }
  if (animeFoundInAniDB) {
    anidbFirstMatchDetails = filteredanidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } as LinkedHashMap
    if ( anidbFirstMatchDetails == null ) {
      statsANIDBJWDFilebotOnly++
      println "//--- ONLY Filebot Anime Name Matched something in ANIDB ---///"
      fileBotAniDBMatchUsed = true
      anidbFirstMatchDetails = filteredfileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } as LinkedHashMap
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score as BigDecimal
      anidbSecondMatchDetails = filteredfileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value } as LinkedHashMap
      anidbThirdMatchDetails = filteredfileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value } as LinkedHashMap
    } else {
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score as BigDecimal
      firstAniDBWTMatchName = anidbFirstMatchDetails.primarytitle
      anidbSecondMatchDetails = filteredanidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value } as LinkedHashMap
      anidbThirdMatchDetails = filteredanidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value } as LinkedHashMap
    }
    if ( anidbSecondMatchDetails != null ) {
      secondANIDBWTMatchNumber = anidbSecondMatchDetails.score as BigDecimal
      secondAniDBWTMatchName = anidbSecondMatchDetails.primarytitle
    }
    if ( anidbThirdMatchDetails != null ) {
      thirdANIDBWTMatchNumber = anidbThirdMatchDetails.score as BigDecimal
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
    if ( filteredfileBotAniDBJWDResults ) {
      if ( fileBotAniDBMatchUsed ) {
        fileBotANIDBJWDMatchDetails = null
        fileBotANIDBJWDMatchNumber = 0
      } else {
        fileBotANIDBJWDMatchDetails = filteredfileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } as LinkedHashMap
        fileBotANIDBJWDMatchNumber = fileBotANIDBJWDMatchDetails.score as BigDecimal
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
  return [firstANIDBWTMatchNumber: firstANIDBWTMatchNumber, firstAniDBWTMatchName:firstAniDBWTMatchName, anidbFirstMatchDetails: anidbFirstMatchDetails, secondANIDBWTMatchNumber: secondANIDBWTMatchNumber, secondAniDBWTMatchName:secondAniDBWTMatchName, anidbSecondMatchDetails: anidbSecondMatchDetails, thirdANIDBWTMatchNumber: thirdANIDBWTMatchNumber, thirdAniDBWTMatchName:thirdAniDBWTMatchName, anidbThirdMatchDetails:anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails: fileBotANIDBJWDMatchDetails, fileBotANIDBJWDMatchNumber: fileBotANIDBJWDMatchNumber, animeFoundInAniDB: animeFoundInAniDB, statsANIDBJWDFilebotOnly:statsANIDBJWDFilebotOnly, statsANIDBFilebotMatchedScript:statsANIDBFilebotMatchedScript]
}

/**
 * Given an Array of files, process *each* file and group the files based on the rename options specific to that file.
 * This Method is only used when their is BOTH an AniDB ID and TheTVDB ID that "matches" for the files.
 * The file rename options are a result of calling renameOptionsForEpisodesUsingAnimeLists()
 *
 * @param input ArrrayList<File> of files to process and group by.
 * @param preferredDB  The preferred Database to use, acceptable values are anidb or tvdb
 * @param anidbMatchDetails The AniDB JWD Match details
 * @param tvdbMatchDetails The TheTVDB JWD Match details
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByAnimeLists(ArrayList<File> input, String preferredDB, LinkedHashMap anidbMatchDetails, LinkedHashMap tvdbMatchDetails, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber){
  LinkedHashMap groupsByEpisode
  Integer processPass = 1
  groupsByEpisode = input.groupBy { File f ->
    log.finest "// FILE:${f}"
    return renameOptionsForEpisodesUsingAnimeLists(f, preferredDB, anidbMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber )
  }
  return groupsByEpisode
}

/**
 * Given an Array of files, process *each* file and group the files based on the rename options specific to that file.
 * This Method is only used when their is only an TheTVDB ID that "matches" for the files.
 * The file rename options are a result of calling renameOptionForTVDBAirdateEpisodes() for airdate ordered files,
 * and renameOptionForTVDBAbsoluteEpisodes() for absolute ordered files
 *
 * @param input ArrrayList<File> of files to process and group by.
 * @param tvdbID  TheTVDB ID
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByTVDB( ArrayList<File> input, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber) {
  LinkedHashMap groupsByEpisode
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: '', animename: '', matchname: '', alias: false]
  switch (group.order) {
    case 'airdate':
      println "------- Using airdate Ordering"
      groupsByEpisode = input.groupBy { File f ->
        log.finest "// FILE:${f}"
        return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails)
      }
      break
    case 'absolute':
      println "------- Using absolute Odering"
      groupsByEpisode = input.groupBy { File f ->
        log.finest "// FILE:${f}"
        return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
      }
      break
    default:
      println "------- Using airdate Ordering"
      groupsByEpisode = input.groupBy { File f ->
        log.finest "// FILE:${f}"
        return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails )
      }
      break
  }
  return groupsByEpisode
}

/**
 * Given an Array of files, process *each* file and group the files based on the rename options specific to that file.
 * This Method is used when their is only an AniDB ID that "matches" for the files or we only want to use AniDB
 * The file rename options are a result of calling renameOptionForTVDBAirdateEpisodes() for airdate ordered files,
 * and renameOptionForTVDBAbsoluteEpisodes() for absolute ordered files
 *
 * @param input ArrrayList<File> of files to process and group by.
 * @param anidbMatchDetails  The AniDB JWD Match details
 * @param renamePass Rename Pass (1 or 2)
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param aniDBJWDMatchNumber The JWD Match Number
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param tvdbId TheTVDB ID
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByAniDB( ArrayList<File> input, LinkedHashMap anidbMatchDetails, Integer renamePass, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, BigDecimal aniDBJWDMatchNumber, JsonObject animeOffLineDatabaseJsonObject, Integer tvdbID, Boolean hasSeasonality, Integer mySeasonalityNumber) {
  def groupsByEpisode = input.groupBy { File f ->
    log.finest "// FILE:${f}"
    return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, aniDBJWDMatchNumber, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
  }
  return groupsByEpisode
}

/**
 * Using a Single AniDB ID and TheTVDB ID, determine based on Order (Airdate/Absolute), and AnimeList status
 * (None, Matching, misMatched) how to route that group of Anime Files to another method which will generate the
 * specific rename options for the files in that group.
 * renamePass is used to determine basically how many times this method has been called, and vary the options based on
 *     that. It is *assumed* that the AID/TVDBID do not change between renamePass 1 and 2
 * prefferredDB is *intended* to be used to give preferrence to using AniDB or TheTVDB for specific decision tree's.
 *      This is something that is intended to be set by the process calling this method to make "exceptions" for specific decision tree's.
 *
 * @param f The file we are processing the rename options for
 * @param preferredDB  The preferred Database to use, acceptable values are anidb or tvdb
 * @param anidbMatchDetails The AniDB JWD Match details
 * @param tvdbMatchDetails The TheTVDB JWD Match details
 * @param renamePass Rename Pass (1 or 2)
 * @param processPass  Process Pass (This is currently ignored)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionsForEpisodesUsingAnimeLists(File f, String preferredDB, LinkedHashMap anidbMatchDetails, LinkedHashMap tvdbMatchDetails, Integer renamePass, Integer processPass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber ){
  /*  Determine rename Options using AnimeLists
  Defaulting preference of TVDB or AniDB in case of AnimeList Failure
  Different options for Airdate vs Absolute Ordering
  Using renamePass for Determining options based on # of Rename passes
  Using processPass for Determining how many times renameOptionsForEpisodesUsingAnimeLists has been called (try to avoid infinite processing loops)
    --> processPass = 1 allow passing tvdbID/anidbID when calling other rename options
    --> processPass = 2+ do not pass tvdbID/anidbID when calling other rename options (or don't call renameOptionsForEpisodesUsingAnimeLists?)*/
  println "//----- renameOptionsForEpisodesUsingAnimeLists"
  // Setup local variables for easy references
  performRename = false
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode == true
  Boolean isMovieType = group.isMovieType
  Boolean isSpecialType = group.isSpecialType
  String specialType = group.specialType
  Integer anidbID = anidbMatchDetails.dbid
  Integer tvdbID = tvdbMatchDetails.dbid
  // Setup local variables to script scope
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Integer myEpisodeSeason
  def myAnimeListMapping
  String myanimeListGetTVDBSeason
  Collection<Episode> myTVDBSeriespisodes
  Episode doesItContainEpisode
  Boolean preferAniDB = true
  Boolean absoluteOrdering
  switch (preferredDB) {
    case 'anidb':
      preferAniDB = true
      println "------- We prefer database:[anidb]"
      break
    case 'tvdb':
      preferAniDB = false
      println "------- We prefer database:[TheTVDB]"
      break
    default:
      println "------- We prefer database:[anidb]"
      preferAniDB = true
      break
  }
  switch (group.order) {
    case 'airdate':
      absoluteOrdering = false
      println "------- Using airdate Ordering"
      break
    case 'absolute':
      absoluteOrdering = true
      println "------- Using absolute Odering"
      break
    default:
      println "------- Using absolute Odering"
      absoluteOrdering = true
      break
  }
  // Detect the episode number we are working with
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false)
  println "------- We have AniDB ID:[${anidbID}] and TVDB ID:[${tvdbID}]"
  println "--------- Consult AnimeList for AniDB ID:[${anidbID}] with TVDB ID:[${tvdbID}]"
  def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)

  // It can be an Integer if the AniDB maps to a series
  // Multi-episode titles not found on theTVDB.com are marked as "unknown"
  // One-off titles that won't ever be added to theTVDB.com (movies, TV specials, one-shot OVAs) are marked by their AniDb.net type
  // Pornographic titles are marked by "hentai" regardless of episode count as they will never appear on theTVDB.com.
  // It can be an Integer if the AniDB maps to a series
  // Multi-episode titles not found on theTVDB.com are marked as "unknown"
  // One-off titles that won't ever be added to theTVDB.com (movies, TV specials, one-shot OVAs) are marked by their AniDb.net type
  // Pornographic titles are marked by "hentai" regardless of episode count as they will never appear on theTVDB.com.
  log.finest "------- We have got myanimeListGetTVDBID:[${myanimeListGetTVDBID}] from AnimeList"

  // Check if the TVDB ID Matches from the AniDB ID
  //  Null: (AID has no Map in AnimeList)
  //    preferAniDB: True
  //      1: - tvdbMatchDetails.score == 1 && !tvdbMatchDetails.alias
  //           Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass,tvdbid
  //           N: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, no tvdbid
  //      2: - Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, no AniDB
  //      2: - Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, no AniDB
  //    preferAniDB: false
  //       - Parse each episode #, lookup AID from TVDBID AnimeList Map
  //        Has Map:
  //          1: renameOptionsForEpisodesUsingAnimeLists, renamePass, AnimeList AID, tvdbid, preferAniDB
  //          2: renameOptionsForEpisodesUsingAnimeLists, renamePass, AnimeList AID, tvdbid, prefertvdb
  //        No Map:
  //          1: - Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, no AniDB
  //          1: - Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, no AniDB
  //          2: - Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, no tvdbid
  if ( myanimeListGetTVDBID == null ) {
    log.finest "--------- There is no AnimeList MAP for AniDB ID:[${anidbID}]"
    switch (renamePass){
      case 1:
        if ( preferAniDB ) {
          //    preferAniDB: True
          //      1: - tvdbID > 0
          //           Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass,tvdbid
          //           N: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, no tvdbid
          if ( tvdbID > 0 ) {
            log.finest "----------- Send to AniDB with ${anidbID}, tvdbID of ${tvdbID}, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbMatchDetails.dbid, hasSeasonality, mySeasonalityNumber)
          }
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        } else {
          //    preferAniDB: false
          //      - Parse each episode #, lookup AID from TVDBID AnimeList Map
          //        Has Map:
          //          1: renameOptionsForEpisodesUsingAnimeLists, renamePass, AnimeList AID, tvdbid, preferAniDB
          //        No Map:
          //          1: - Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, no AniDB
          //          1: - Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, no AniDB
          if ( hasSeasonality ) {
            if ( myEpisodeNumber.isNumber() ) {
              returnThing = filebotAnimeListReturnAIDEntry(tvdbID, mySeasonalityNumber, myEpisodeNumber.toInteger())
              if ( returnThing != null) {
                log.finest "----------- Send to renameOptionsForEpisodesUsingAnimeLists with AniDB:[${returnThing.anidbid}], tvdbID:[${tvdbID}], renamePass:[${renamePass}], Prefer AniDB"
                LinkedHashMap JWDMatchDetails = [score: 1, db:'anidb', dbid: "${returnThing.anidbid}", primarytitle: "${returnThing.name}", animename: "${returnThing.name}", matchname: "${returnThing.name}", alias: false]
                return renameOptionsForEpisodesUsingAnimeLists(f, 'anidb', JWDMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber )
              }
            }
          }
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails)
          }
        }
        break
      case 2:
        if ( preferAniDB ) {
          //    preferAniDB: True
          //      2: - Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, no AniDB
          //      2: - Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, no AniDB
          if ( absoluteOrdering ) {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[0]"
            return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[0]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails)
          }
        } else {
          //    preferAniDB: false
          //      - Parse each episode #, lookup AID from TVDBID AnimeList Map
          //        Has Map:
          //          2: renameOptionsForEpisodesUsingAnimeLists, renamePass, AnimeList AID, tvdbid, prefertvdb
          //        No Map:
          //          2: - Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, no tvdbid
          if ( hasSeasonality ) {
            if ( myEpisodeNumber.isNumber() ) {
              returnThing = filebotAnimeListReturnAIDEntry(tvdbID, mySeasonalityNumber, myEpisodeNumber.toInteger())
              if ( returnThing != null) {
                log.finest "----------- Send to renameOptionsForEpisodesUsingAnimeLists with AniDB:[${returnThing.anidbid}], tvdbID:[${tvdbID}], renamePass:[${renamePass}], Prefer tvdb"
                LinkedHashMap JWDMatchDetails = [score: 1, db:'anidb', dbid: "${returnThing.anidbid}", primarytitle: "${returnThing.name}", animename: "${returnThing.name}", matchname: "${returnThing.name}", alias: false]
                return renameOptionsForEpisodesUsingAnimeLists(f, 'tvdb', JWDMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber )
              }
            }
          }
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = tvdbID
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  // Check if the TVDB ID Matches from the AniDB ID
  //  Match: Determine TVDB Options
  //   Episode # is null?
  //      1: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
  //      2: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
  //      2: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
  //   Episode # has a dot in it (aka 5.5, 6.5 etc known as a special episode)
  //      1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, AniDB
  //      1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, AniDB
  //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
  //    absolute
  //      1: Is SpecialType?
  //          Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
  //          N: Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, AniDB
  //      2: is SpecialType?
  //          Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
  //          N: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
  //    airdate
  //      1: Send to renameOptionForTVDBAirdateEpisodes, renamePass, AniDB
  //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
  if (myanimeListGetTVDBID == tvdbID) {
    println '--------- AnimeList AniDB to TVDB ID mapping found and matched.'
    if ( myEpisodeNumber == null ) {
      //   Episode # is null?
      //      1: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
      //      2: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
      //      2: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
      println '----------- Episode # detected as null'
      switch (renamePass) {
        case 1:
          log.finest "------------- Checking AniDB ${myanimeListGetTVDBID} - with tvdbID:[${tvdbID}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          break
        case 2:
          if ( absoluteOrdering ) {
            log.finest "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:{${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:{${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ ) {
      println "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special episode (Dot Syntax)"
      //   Episode # has a dot in it (aka 5.5, 6.5 etc known as a special episode)
      //      1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, AniDB
      //      1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, AniDB
      //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
//      group.isSpecialEpisode = true
      switch (renamePass) {
        case 1:
          if ( absoluteOrdering) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[0], tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials)
          }
          break
        case 2:
          log.finest "----------- Send to AniDB with ${anidbID}, tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( absoluteOrdering ) {
      //    absolute
      //      1: Is SpecialType?
      //          Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
      //          N: Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, AniDB
      //      2: is SpecialType?
      //          Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
      //          N: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
      isSpecialType = group.isSpecialType || anidbMatchDetails.isSpecialType
      switch (renamePass) {
        case 1:
          if ( isSpecialType ) {
            log.finest "----------- Special Type Detected: Send to AniDB with ${anidbID}, tvdbID, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          }
          log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
          return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          break
        case 2:
          if ( isSpecialType ) {
            log.finest "----------- Special Type Detected: Send to AniDB with ${anidbID}, tvdbID, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          }
          log.finest "----------- Send to AniDB with ${anidbID}, tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    } else {
      //    airdate
      //      1: Send to renameOptionForTVDBAirdateEpisodes, renamePass, AniDB
      //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
      switch (renamePass) {
        case 1:
          log.finest "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[${anidbID}], tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
          return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          break
        case 2:
          log.finest "----------- Send to AniDB with ${anidbID}, tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
  } else {
    // Check if the TVDB ID Matches from the AniDB ID
    //  Invalid Match: (AID has Map, but Not to TVDBID provided)
    //  Meaning:  The two anime aren't actually the "same"
    //      - Perhaps one is the "correct" one or neither is.
    //    preferAniDB: True
    //      1: -  Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, no tvdbid
    //      2: -  Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, no tvdbid, Reset isSpecialEpisode
    //      2021/03/02 - Possible Alternative?  Send to renameOptionForTVDBAbsoluteEpisodes, renamePass[1], TVDBID it is matched to (so not from our script), Reset isSpecialEpisode
    //    preferAniDB: false
    //      1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, no AniDB
    //      1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, no AniDB
    //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, no tvdbid, Reset isSpecialEpisode (AniDB matches tend to be a single Series, while TVDB Matches can often map to multiple AniDB Series)
    println "--------- Mapping didn't match, returned TVDBID: ${myanimeListGetTVDBID}"
    switch (renamePass){
      case 1:
        if ( preferAniDB ) {
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        }
        if ( absoluteOrdering ) {
          log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
          return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails ,tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
        } else {
          log.finest "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[0], tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
          return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails)
        }
        break
      case 2:
        if ( preferAniDB ) {
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
//          log.finest "----------- Reset isSpecialEpisode"
//          group.isSpecialEpisode = false
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        }
        log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
//        log.finest "----------- Reset isSpecialEpisode"
//        group.isSpecialEpisode = false
        return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = tvdbID
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
}

/**
 * Given a File, TheTVDB ID and possibly an AID determine the rename options for that file using 'Absolute' ordering
 *
 * @param f The file we are processing the rename options for
 * @param anidbMatchDetails The AniDB JWD Match details
 * @param tvdbID TheTVDB ID
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForTVDBAbsoluteEpisodes(File f, LinkedHashMap anidbMatchDetails, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, Boolean processAsSpecial = false) {
  println "//----- renameOptionForTVDBAbsoluteEpisodes"
  performRename = false
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode || processAsSpecial
  Boolean isMovieType = group.isMovieType || anidbMatchDetails.isMovieType
  Boolean isSpecialType = group.isSpecialType || anidbMatchDetails.isSpecialType
  Integer myEpisodeSeason
  def myAnimeListMapping
  String myanimeListGetTVDBSeason = 'n'
  Collection<Episode> myTVDBSeriespisodes
  Episode doesItContainEpisode
  Integer anidbID = anidbMatchDetails.dbid
  // Detect the episode number we are working with
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false)
  println "------- We have TVDB ID:[${tvdbID}]"
  if ( anidbID > 0) {
    // Not all Mappings include a default Season, so filebotAnimeListReturnFromAID will return n if there is no mapping (vs null)
    myAnimeListMapping = filebotAnimeListReturnFromAID(anidbID, false, false)
    //  println "--------- myAnimeListMapping ${myAnimeListMapping}"
    myanimeListGetTVDBSeason = filebotAnimeListReturnFromAID(anidbID, false, true)
    // defaulttvdbseason - The corresponding theTVDB.com season.
    // For one-off titles it will be 1 unless associated to a multi-episode series, in which case it will be 0.
    // Series that span multiple seasons on theTVDB.com may be marked as a if the absolute episode numbering is defined and matches AniDb.net.
    // If there is NO entry it will return n
    println "--------- myanimeListGetTVDBSeason ${myanimeListGetTVDBSeason}"
  }
  if ( myEpisodeNumber == null ) {
    // EP # is Null
    //    1:  Rename using TVDB
    //        - Filter to Seasonality or myanimeListGetTVDBSeason
    //    2:  AniDB?
    //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
    //        N: hasSeasonality?
    //            Y: Rename using TVDB
    //               - Filter to myanimeListGetTVDBSeason
    //            N: STOP
    println '----------- Episode # detected as null'
    switch (renamePass) {
      case 1:
        // EP # is Null
        //    1:  Rename using TVDB
        //        - Filter to Seasonality or myanimeListGetTVDBSeason
        myEpisodeNumber = 0 // Else the checks following will blow up
        renameOptionsSet = true
        renameQuery = tvdbID
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Airdate'
        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        renameFilter = ''
        if ( myanimeListGetTVDBSeason == 'n' || myanimeListGetTVDBSeason == 'a' ) {
          println '--------- Animelist Mapping indicates no default TVDB Season or Animelist Mapping indicates Absolute Ordering in TVDB'
          renameFilter = ''
        }
        if ( myanimeListGetTVDBSeason.isNumber() ) {
          if ( myanimeListGetTVDBSeason.toInteger() == 0 ) {
            println "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB"
            isSpecialEpisode = true
          } else {
            if ( !hasSeasonality && !isSpecialEpisode) {
              println "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${mySeasonalityNumber}"
              renameStrict = true
            }
          }
        }
        if (isSpecialEpisode ) {
          println "--------- Specials however use filter of episode.special"
          renameFilter = "episode.special"
          if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        if ( hasSeasonality && !isSpecialEpisode) {
          println "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
          renameFilter = "s == ${mySeasonalityNumber}"
          renameStrict = true
        }
        break
      case 2:
        // EP # is Null
        //    2:  AniDB?
        //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
        //        N: hasSeasonality?
        //            Y: Rename using TVDB
        //               - Filter to myanimeListGetTVDBSeason
        //            N: STOP
        if (anidbID > 0) {
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        }
        if ( hasSeasonality ) {
          myEpisodeNumber = 0 // Else the checks following will blow up
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = '[AnimeList.AniDB, episode, order.absolute.episode]'
          renameFilter = ''
          if ( myanimeListGetTVDBSeason == 'n' || myanimeListGetTVDBSeason == 'a' ) {
            println '--------- Animelist Mapping indicates no default TVDB Season or Animelist Mapping indicates Absolute Ordering in TVDB'
            renameFilter = ''
          }
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( myanimeListGetTVDBSeason.toInteger() == 0 ) {
              println "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB"
              isSpecialEpisode = true
            } else {
              if ( !isSpecialEpisode) {
                println "----- Using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
                renameFilter = "s == ${mySeasonalityNumber}"
                renameStrict = true
              }
            }
          }
          if (isSpecialEpisode ) {
            println "--------- Specials however use filter of episode.special"
            renameFilter = "episode.special"
            if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              println '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
        } else {
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ ) {
    println "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special (dot Syntax)"
    //    1: Rename using TVDB
    //    2:  AniDB?
    //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
    //        N: Stop
    switch (renamePass) {
      case 1:
        isSpecialEpisode = true
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Airdate'
        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        renameFilter = "episode.special"
        if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        if ( anidbID ) {
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        } else {
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( !renameOptionsSet  ) {
    myTVDBSeriespisodes = filebotTVDBgetEpisodeList(tvdbID)
    doesItContainEpisode = filebotTVDBSeasonContainsEpisodeNumber(myTVDBSeriespisodes, myEpisodeNumber.toInteger(), 'absolute', true, 0, 'exclude')
    if ( doesItContainEpisode != null ) {
      if ( doesItContainEpisode.season == null ) {
        println "----------- Detected TVDB Absolute Episode Does not have a Season[${doesItContainEpisode.season}], Perhaps a Special?"
        myEpisodeSeason = 0
      } else {
        println "----------- Detected TVDB Absolute Episode Season ${doesItContainEpisode.season}"
        myEpisodeSeason =  doesItContainEpisode.season
      }
    } else {
      println "----------- Detected TVDB Absolute Episode Number:[${myEpisodeNumber}] does not seem to be in TVDB Absolute Episode List:[${myTVDBSeriespisodes.findAll { it.absolute != null  }.size()}]"
      if ( anidbID > 0 ) {
        if ( myanimeListGetTVDBSeason != null ) {
          println "--------- Checking if we can determine if Episode Number:[${myEpisodeNumber}] is a special for Season ${myanimeListGetTVDBSeason}"
        } else {
          println "----------- We have AniDBID:[${anidbID}], Checking if we can determine if Episode Number:[${myEpisodeNumber}] is a special"
        }
        def myAniDBEpisodeCount = aniDBGetEpisodeNumberForAID(animeOffLineDatabaseJsonObject, anidbID)
        if (myEpisodeNumber.toInteger() <= myAniDBEpisodeCount ) {
          println "----------- Episode Number:[${myEpisodeNumber}] is within AniDB Episode Count range:[${myAniDBEpisodeCount}] for AniDBID:[${anidbID}]"
          println "----------- Perhaps this is due to differences in how each service counts episodes?"
          println "----------- Set no options to allow for futher processing.."
        }
        if (myEpisodeNumber.toInteger() > myAniDBEpisodeCount ) {
          println "----------- Episode Number:[${myEpisodeNumber}] is GREATER then Regular AniDB Episode Count range:[${myAniDBEpisodeCount}]"
          def myAniDBEpisodeCountWithSpecials = filebotAniDBEpisodeCount(filebotAniDBgetEpisodeList(anidbID, 'include'))
          if ( myEpisodeNumber.toInteger() > myAniDBEpisodeCountWithSpecials ) {
            println "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
            println "----------- Likely TVDB Absolute Ordered"
            println "----------- Set no options to allow for futher processing.."
          } else {
            println "----------- Episode Number:[${myEpisodeNumber}] is less then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
            println "----------- Episode Number:[${myEpisodeNumber}] is probably a special"
            //    1: Rename using TVDB
            //    2:  AniDB?
            //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
            //        N: Stop
            switch (renamePass) {
              case 1:
                isSpecialEpisode = true
                renameQuery = tvdbID
                renameOptionsSet = true
                performRename = true
                renameDB = 'TheTVDB'
                renameOrder = 'Airdate'
                renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
                renameFilter = "episode.special"
                if ( (useNonStrictOnAniDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
                  renameStrict = false
                  println '------------- Set non-Strict renaming'
                } else {
                  renameStrict = true
                }
                break
              case 2:
                log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
                return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
                break
              default:
                println '//-----------------------------//'
                println '//            STOP             //'
                println '//-----------------------------//'
                renameOptionsSet = true
                renameQuery = ''
                performRename = false
                renameDB = ''
                renameOrder = ''
                renameMapper = ''
                renameFilter = ''
                renameStrict = true
                break
            }
          }
        }
      } else {
        println "----------- Allow for further processing.."
      }
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber.toInteger() > 99) {
    //    1: Rename using TVDB
    //    2:  AniDB?
    //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
    //        N: Stop
    println '----------- 3-Digit Episode detected, use Episode Filtering'
    // For some reason 3 digit episodes match incorrectly a good portion of the time..
    // For These episodes just set the filter to the episode # and see if that at least cuts down
    // the incorrect matches
    switch (renamePass) {
      case 1:
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Absolute'
        renameMapper = ''
        renameFilter = "e == ${myEpisodeNumber}" // e == # doesn't work when using Airdate order, would need S and E notation ..
        if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnAniDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        if ( anidbID ) {
          log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        } else {
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( !renameOptionsSet && anidbID > 0 && myanimeListGetTVDBSeason != null) {
    if ( myanimeListGetTVDBSeason == 'n' && !renameOptionsSet ) {
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      println '--------- Animelist Mapping indicates no default TVDB Season'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              println "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${mySeasonalityNumber}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            println "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            println "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score  == 1 ) || (useNonStrictOnAniDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( myanimeListGetTVDBSeason == 'a' && !renameOptionsSet ) {
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      println '--------- Animelist Mapping indicates Absolute Ordering in TVDB'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              println "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${mySeasonalityNumber}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            println "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            println "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( !renameOptionsSet && myEpisodeSeason == null ) {
      println "--------- Episode Season:[${myEpisodeSeason}], But AnimeListSeason:[${myanimeListGetTVDBSeason}] indicates a Season "
      println "--------- Possible Special for that Season (dicey with TVDB)"
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      switch (renamePass) {
        case 1:
          isSpecialEpisode = true
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, episode, AnimeList.AniDB]'
          renameFilter = "episode.special"
          if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( !renameOptionsSet && myanimeListGetTVDBSeason.toInteger() == 0  || myEpisodeSeason == 0) {
      println "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB or myEpisodeSeason:[${myEpisodeSeason}] does"
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      switch (renamePass) {
        case 1:
          isSpecialEpisode = true
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "episode.special"
          if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( !renameOptionsSet && myEpisodeSeason == myanimeListGetTVDBSeason.toInteger() ) {
      println "--------- Episode Season:[${myEpisodeSeason}] matches AnimeListSeason:[${myanimeListGetTVDBSeason}]"
      switch (renamePass) {
        case 1:
          // Possibly TVDB Absolute Ordering with the same Season as the map (Why didn't it already match?)
          // Unfortunately you can't use Season filter when order = Absolute with TVDB
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "s == ${myEpisodeSeason}"
          renameStrict = true
          break
        case 2:
          renameOptionsSet = true
          println "------------- Checking AniDB ${myanimeListGetTVDBID} - no tvdbID"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( !renameOptionsSet && myEpisodeSeason < myanimeListGetTVDBSeason.toInteger() ) {
      println "--------- Episode Season:[${myEpisodeSeason}] < AnimeListSeason:[${myanimeListGetTVDBSeason}]"
      // Possibly NOT TVDB Absolute Ordering. So it's either Relative Season absolute or Normal AniDB Absolute or wrong AID or just a special..
      if ( myAnimeListMapping.episodeoffset != null ) {
        println "----------- myAnimeListMapping indicates an Episode Offset"
        if ( myEpisodeNumber.toInteger() <= myAnimeListMapping.episodeoffset.toInteger() ) {
          // Possibly normal AniDB Absolute Ordering. (Why didn't it already match?)
          renameOptionsSet = true
          println "------------- Probably Normal AniDB Absolute Ordering, which will likely not match correctly using TVDB"
          println "------------- Send to renameOptionForAniDBAbsoluteEpisodes - anidbID:[${anidbID}], renamePass:[1], tvdbID:[${tvdbID}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber)
        }
      } else {
        println "--------- No Episode Offset for Animelist Season"
        if ( isSpecialEpisode ) {
          println "----------- Because it is a Special (perhaps AniDB Absolute Ordered Special)"
          switch (renamePass) {
            case 1:
              renameQuery = tvdbID
              renameOptionsSet = true
              performRename = true
              renameDB = 'TheTVDB'
              renameOrder = 'Airdate'
              renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
              renameFilter = "episode.special"
              if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
                renameStrict = false
                println '------------- Set non-Strict renaming'
              } else {
                renameStrict = true
              }
              break
            case 2:
              if ( anidbID > 0 ) {
                log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
                return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
              } else {
                println '//-----------------------------//'
                println '//            STOP             //'
                println '//-----------------------------//'
                renameOptionsSet = true
                renameQuery = ''
                performRename = false
                renameDB = ''
                renameOrder = ''
                renameMapper = ''
                renameFilter = ''
                renameStrict = true
              }
              break
            default:
              println '//-----------------------------//'
              println '//            STOP             //'
              println '//-----------------------------//'
              renameOptionsSet = true
              renameQuery = ''
              performRename = false
              renameDB = ''
              renameOrder = ''
              renameMapper = ''
              renameFilter = ''
              renameStrict = true
              break
          }
        }
      }
      if ( !renameOptionsSet ) {
        // Possibly relative absolute ordering to TVDB Season Start (but not TVDB Series start) or wrong AID :) (so the season mapping is wrong)
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'airdate'
        switch (renamePass) {
          case 1:
            renameMapper = 'order.absolute.episode.derive(e)'
            renameFilter =  "s == ${myanimeListGetTVDBSeason}"
            break
          case 2:
            renameMapper = 'order.absolute.episode.derive(e)'
            renameFilter = hasSeasonality == true ? "s == ${mySeasonalityNumber}" : "s == ${myEpisodeSeason}"
            break
          default:
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = tvdbID
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
            break
        }
        renameStrict = true
      }
    }
    if ( !renameOptionsSet && myEpisodeSeason > myanimeListGetTVDBSeason.toInteger() ) {
      println "--------- Episode Season:[${myEpisodeSeason}] > AnimeListSeason:[${myanimeListGetTVDBSeason}]"
      //    Possibilities:
      //    1. TVDB Absolute Ordered Episode, which is no longer in the same "season" as the AniDB Series
      //    2.
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      println '--------- Animelist Mapping indicates Absolute Ordering in TVDB'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              println "----- No Seasonality Detected, using myEpisodeSeason:[${myEpisodeSeason}] as a Season Filter"
              renameFilter = "s == ${myEpisodeSeason}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            println "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            println "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnAniDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
  }
  if ( !renameOptionsSet && hasSeasonality) {
    if ( !renameOptionsSet && myEpisodeSeason == mySeasonalityNumber ) {
      println "--------- Episode Season:[${myEpisodeSeason}] matches mySeasonalityNumber:[${mySeasonalityNumber}]"
      switch (renamePass) {
        case 1:
          // Possibly TVDB Absolute Ordering with the same Season as the map (Why didn't it already match?)
          // Unfortunately you can't use Season filter when order = Absolute with TVDB
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "s == ${myEpisodeSeason}"
          renameStrict = true
          break
        case 2:
          renameOptionsSet = true
          println "------------- Checking AniDB ${myanimeListGetTVDBID} - no tvdbID"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
    if ( !renameOptionsSet && myEpisodeSeason < mySeasonalityNumber ) {
      println "--------- Episode Season:[${myEpisodeSeason}] < mySeasonalityNumber:[${myanimeListGetTVDBSeason}]"
      renameQuery = tvdbID
      renameOptionsSet = true
      performRename = true
      renameDB = 'TheTVDB'
      renameOrder = 'airdate'
      switch (renamePass) {
        case 1:
          renameMapper = 'order.absolute.episode.derive(e)'
          renameFilter =  "s == ${myanimeListGetTVDBSeason}"
          break
        case 2:
          renameMapper = 'order.absolute.episode'
          renameFilter = "s == ${myEpisodeSeason}"
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
      renameStrict = true
    }
    if ( !renameOptionsSet && myEpisodeSeason > mySeasonalityNumber ) {
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      println "--------- Episode Season:[${myEpisodeSeason}] > mySeasonalityNumber:[${mySeasonalityNumber}]"
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !isSpecialEpisode) {
              println "----------- using myEpisodeSeason:[${myEpisodeSeason}] as a Season Filter"
              renameFilter = "s == ${myEpisodeSeason}"
            }
          }
          if (isSpecialEpisode ) {
            println "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            log.finest "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = ''
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }
          break
        default:
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
  }
  if ( !renameOptionsSet ) {
    println "--------- Fall Thru TVDB Options - myEpisodeSeason:[${myEpisodeSeason}]: renamePass:[${renamePass}]"
    switch (renamePass) {
      case 1:
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Airdate'
        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        if (isSpecialEpisode ) {
          println "--------- Specials Episodes however use filter of episode.special"
          renameFilter = "episode.special"
        } else {
          renameFilter = myEpisodeSeason == null ? '' : "s == ${myEpisodeSeason}"
        }
        if ( (useNonStrictOnAniDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Airdate'
        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        if (isSpecialEpisode ) {
          println "--------- Specials Episodes however use filter of episode.special"
          renameFilter = "episode.special"
        } else {
          renameFilter = myEpisodeSeason == null ? '' : "s == ${myEpisodeSeason}"
        }
        if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = tvdbID
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
}

/**
 * Given a File, TheTVDB ID and possibly an AID determine the rename options for that file using 'airdate' ordering
 *
 * @param f The file we are processing the rename options for
 * @param tvdbID TheTVDB ID
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @param anidbMatchDetails The AniDB JWD Match details
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForTVDBAirdateEpisodes(File f, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap anidbMatchDetails, Boolean processAsSpecial = false) {
  println "------- renameOptionForTVDBAirdateEpisodes"
  performRename = false
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode || processAsSpecial
  Boolean isMovieType = group.isMovieType
  Boolean isSpecialType = group.isSpecialType
  Integer myEpisodeSeason
  Collection<Episode> myTVDBSeriespisodes
  Episode doesItContainEpisode
  // Detect the episode number we are working with
  LinkedHashMap returnThing = detectEpisodeNumberFromFile(f, false, true, true, false)
  String myEpisodeNumber = returnThing.myDetectedEpisodeNumber
  String mySeasonNumber = returnThing.myDetectedSeasonNumber
  Integer anidbID = anidbMatchDetails.dbid
  println "------- We have TVDB ID:[${tvdbID}] and AniDB ID:[${anidbID}]"
  if ( myEpisodeNumber == null ) {
    println '----------- But we could not detect the episode number'
    myEpisodeNumber = 0 // Else the checks following will blow up
    renameOptionsSet = true
    renameQuery = tvdbID
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = ''
    if (isSpecialEpisode || mySeasonalityNumber == 0) {
      println "--------- Specials however use filter of episode.special"
      renameFilter = "episode.special"
    } else {
      println "--------- using ${mySeasonalityNumber} as a Season Filter"
      renameFilter = "s == ${mySeasonalityNumber}"
    }
    if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      println '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && (myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ || !myEpisodeNumber.isNumber()) ) {
    println "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special (dot Syntax)"
    isSpecialEpisode = true
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "episode.special"
    if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      println '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber.toInteger() > 99) {
    println '----------- 3-Digit Episode detected..'
    renameOptionsSet = true
    renameQuery = tvdbID
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "e == ${myEpisodeNumber} && s == ${mySeasonNumber}"
    renameStrict = true
  }
  if ( !renameOptionsSet ) {
    myTVDBSeriespisodes = filebotTVDBgetEpisodeList(tvdbID)
    doesItContainEpisode = filebotTVDBSeasonContainsEpisodeNumber(myTVDBSeriespisodes, myEpisodeNumber.toInteger(), 'airdate', true, mySeasonalityNumber, 'exclude')
    if ( doesItContainEpisode != null ) {
      println "----------- Detected Episode Season ${doesItContainEpisode.season}"
      myEpisodeSeason =  doesItContainEpisode.season
    } else {
      println "----------- Detected Episode Number:[${myEpisodeNumber}] does not seem to be in TVDB Episode List:[${myTVDBSeriespisodes.findAll { it.regular  }.size()}] for Season ${mySeasonalityNumber}"
      println '----------- Perhaps it is a Special?'
      isSpecialEpisode = true
      renameQuery = tvdbID
      renameOptionsSet = true
      performRename = true
      renameDB = 'TheTVDB'
      renameOrder = 'Airdate'
      renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
      renameFilter = "episode.special"
      if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
        renameStrict = false
        println '------------- Set non-Strict renaming'
      } else {
        renameStrict = true
      }
    }
  }
  if ( !renameOptionsSet && myEpisodeSeason == 0) {
    println "--------- myEpisodeSeason lookup indicates episode:[${myEpisodeNumber}] maps to Specials in TVDB"
    isSpecialEpisode = true
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "episode.special"
    if ( (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      println '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && myEpisodeSeason == mySeasonalityNumber ) {
    println "--------- Episode Season:[${myEpisodeSeason}] matches mySeasonalityNumber:[${mySeasonalityNumber}]"
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "s == ${myEpisodeSeason}"
    renameStrict = true
  }
  if ( !renameOptionsSet && myEpisodeSeason < mySeasonalityNumber ) {
    println "--------- Episode Season:[${myEpisodeSeason}] < mySeasonalityNumber:[${mySeasonalityNumber}]"
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "s == ${mySeasonalityNumber}"
    renameStrict = true
  }
  if ( !renameOptionsSet && myEpisodeSeason > mySeasonalityNumber ) {
    println "--------- Episode Season:[${myEpisodeSeason}] > mySeasonalityNumber:[${mySeasonalityNumber}]"
    // Possibly TVDB Absolute Ordering with multiple seasons, all with the same (or no?) map?
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "s == ${mySeasonalityNumber}"
    renameStrict = true
  }
  if ( !renameOptionsSet ) {
    println "--------- Using Episode Season:[${myEpisodeSeason}]"
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "s == ${myEpisodeSeason}"
    renameStrict = true
  }
  return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
}

/**
 * Given a File, AniDB ID and possibly an TheTVDB ID determine the rename options for that file using 'airdate' ordering
 *
 * @param f The file we are processing the rename options for
 * @param tvdbID TheTVDB ID
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForAniDBAbsoluteEpisodes(File f, LinkedHashMap anidbMatchDetails, Integer renamePass, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, BigDecimal aniDBJWDMatchNumber, JsonObject animeOffLineDatabaseJsonObject, Integer tvdbID, Boolean hasSeasonality, Integer mySeasonalityNumber){
  println "//----- renameOptionForAniDBAbsoluteEpisodes"
  Integer anidbID = anidbMatchDetails.dbid
  switch (group.order) {
    case 'airdate':
      absoluteOrdering = false
      println "------- Using airdate Ordering"
      break
    case 'absolute':
      absoluteOrdering = true
      println "------- Using absolute Odering"
      break
    default:
      println "------- Using absolute Odering"
      absoluteOrdering = true
      break
  }
  performRename = false
  renameQuery = false
  renameDB = false
  renameOrder = false
  renameMapper = false
  renameFilter = false
  renameStrict = true
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode == true
  Boolean isMovieType = group.isMovieType || anidbMatchDetails.isMovieType
  Boolean isSpecialType = group.isSpecialType || anidbMatchDetails.isSpecialType
  Integer myAniDBEpisodeCount = 0
  // Detect the episode number we are working with
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false)
  if ( myEpisodeNumber == "0" && !group.isSpecialType) {
    // Episode # indicates Special Episode or "other" type of episode
    // Except for OVA/ONA/OAD .. Filenames are just too varied to assume just because it's episode zero it's a special IN the OVA series (tho that probably does happen).
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    println "--------- Episode # of ${myEpisodeNumber} indicates a special/recap/other or at least not a normal Absolute Ordering (which starts at 1)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode
        //      1: Rename using AniDB and episode.special filter
        println "----------- Using AniDB (Set Filter to episode.special)"
        isSpecialEpisode = true
        renameQuery = anidbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        renameFilter = 'episode.special'
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        // Episode # indicates Special Episode
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: Rename without episode.special filter
        if ( tvdbID > 0 ) {
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          println "----------- Using AniDB (Do not set Filter)"
          isSpecialEpisode = true
          renameQuery = anidbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( myEpisodeNumber == null ) {
    //   Episode # is null?
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    println '----------- Episode # detected as null'
    // We can't detect an Episode # .. Use AniDB as a fallback..
    myEpisodeNumber = 0 // Else the checks following will blow up
    switch (renamePass) {
      case 1:
        //   Episode # is null?
        //      1: Rename using AniDB
        renameOptionsSet = true
        renameQuery = anidbID
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        if ( isSpecialType ) {
          println "------------- SpecialType Detected. Set Filter = episode.special"
          renameFilter = 'episode.special'
        } else {
          renameFilter = ''
        }
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        //   Episode # is null?
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: STOP
        if ( tvdbID > 0 ) {
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          if ( isSpecialType ) {
            println "------------- SpecialType Detected. Set no Filter"
            renameOptionsSet = true
            renameQuery = anidbID
            performRename = true
            renameDB = 'AniDB'
            renameOrder = 'Absolute'
            renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
            renameFilter = ''
            if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              println '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          } else {
            println '//-----------------------------//'
            println '//            STOP             //'
            println '//-----------------------------//'
            renameOptionsSet = true
            renameQuery = ''
            performRename = false
            renameDB = 'AniDB'
            renameOrder = ''
            renameMapper = ''
            renameFilter = ''
            renameStrict = true
          }

        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ ) {
    // Episode # indicates Special Episode (dot syntax)
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    println "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special/recap or at least not how AniDB orders things (Dot Syntax)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        println "----------- Using AniDB (Set Filter to episode.special)"
        isSpecialEpisode = true
        renameQuery = anidbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        renameFilter = 'episode.special'
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        // Episode # indicates Special Episode (dot syntax)
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: Rename without episode.special filter
        if ( tvdbID > 0 ) {
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          println "----------- Using AniDB (Do not set Filter)"
          isSpecialEpisode = true
          renameQuery = anidbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if ( !renameOptionsSet && isSpecialEpisode ) {
    // Episode # indicates Special Episode
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    println "--------- Detected as Special Episode (isSpecialEpisode:[${isSpecialEpisode}]) indicates a special/recap or at least not how AniDB orders things :)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        println "----------- Using AniDB (Set Filter to episode.special)"
        isSpecialEpisode = true
        renameQuery = anidbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        renameFilter = 'episode.special'
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        // Episode # indicates Special Episode (dot syntax)
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: Rename without episode.special filter
        if ( tvdbID > 0 ) {
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          println "----------- Using AniDB (Do not set Filter)"
          isSpecialEpisode = true
          renameQuery = anidbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  myAniDBEpisodeCount = aniDBGetEpisodeNumberForAID(animeOffLineDatabaseJsonObject, anidbID)
  // Hmmm.. How to Handle when myAniDBEpisodeCount is Zero? (Not in AOD, and Not returned by Filebot)
  if (!renameOptionsSet && myEpisodeNumber.toInteger() <= myAniDBEpisodeCount ) {
    // Ep # < AniDB Ep #
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    // --> Probably a normal AniDB Absolute Episode Order, Set basic AniDB rename options
    println "----------- Episode Number:[${myEpisodeNumber}] is within AniDB Episode Count range:[${myAniDBEpisodeCount}]"
    switch (renamePass) {
      case 1:
        println "------------- Using AniDB"
        // Ep # < AniDB Ep #
        //      1: Rename using AniDB
        if ( myEpisodeNumber.toInteger() > 99) {
          println '------------- 3-Digit Episode detected, use Episode Filtering'
          // For some reason 3 digit episodes match incorrectly a good portion of the time..
          // For These episodes just set the filter to the episode # and see if that at least cuts down
          // the incorrect matches
          renameFilter = "e == ${myEpisodeNumber}"
        } else {
          renameFilter = ''
        }
        renameOptionsSet = true
        renameQuery = anidbID
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        // Ep # < AniDB Ep #
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: STOP
        if ( tvdbID > 0 ) {
          println "------------- Using TVDBID Provided"
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          println "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
          if ( anidbMatchDetails.hasAnimeListEntry) {
            def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
            println "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
            if (absoluteOrdering) {
              log.finest "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
            } else {
              log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
            }
          }
          println "------------- No AnimeList Entry. AniDB Fallback will be used (good luck with that)"
          renameOptionsSet = true
          renameStrict = true
          renameQuery = anidbID
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if (!renameOptionsSet && group.isSpecialType ) {
    // The filename indicates a Special Type (OVA/ONA/OAD), which often means if it even has an "episode" number, that number is often .. incorrect for AniDB or TVDB.
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    println "--------- Detected as Special Type by group (group.isSpecialType:[${group.isSpecialType}]) indicates a Special type of [${group.specialType}]"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        println "----------- Using AniDB (Do not set Filter)"
        renameQuery = anidbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        renameFilter = ''
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          println '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        // Group indicates a Special Type
        //      2: TVDBID?
        //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
        //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
        //        N: Rename without episode.special filter
        if ( tvdbID > 0 ) {
          if ( absoluteOrdering ) {
            log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
          } else {
            log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
          }
        } else {
          println "----------- Using AniDB (Do not set Filter)"
          renameQuery = anidbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            println '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        println '//-----------------------------//'
        println '//            STOP             //'
        println '//-----------------------------//'
        renameOptionsSet = true
        renameQuery = ''
        performRename = false
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        break
    }
  }
  if (!renameOptionsSet && myEpisodeNumber.toInteger() > myAniDBEpisodeCount ) {
    // --> Could be TVDB Absolute Ordered, or Relative Seasonal Absolute Ordered or just an "Special" episode (aka Episode 12 ==> Special 1 on 11 episode series)
    //  //                  -- Without being able to use TVDB (We wouldn't be using this method if TVDB match was above threshold)
    //  //                  -- Trying to rename is likely to result in an incorrect match.
    //  //                  -- Set performRename to false unless useNonStrictOnAniDBFullMatch is true. If so, then
    //  //                  -- performRename true, and set renameStrict to false and set a renameFilter to the Episode #
    // So we can look to see if there is an TVDB this is mapped to, and do the same stuff as in groupGenerationByTVDBAbsoluteEpisodes?
    // Maybe we need a renameOptionForTVDBAbsoluteEpisodesUsingAnimeLists to return the rename options, so it can be used here and in groupGenerationByTVDBAbsoluteEpisodes?
    // Ep # > AniDB Ep #?
    //    EP # > AniDB EP # + Specials?
    //      Y: TVDBID?
    //        Y: 1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //           2: STOP
    //        N: STOP
    //      N:  1: Rename with AniDB as "Special"
    //          2: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //          2: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    println "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count range:[${myAniDBEpisodeCount}]"
    println "----------- Likely TVDB Absolute Ordered, or Relative Seasonal Absolute Ordered or Special."
    renameOptionsSet = true
    def myAniDBEpisodeCountWithSpecials = filebotAniDBEpisodeCount(filebotAniDBgetEpisodeList(anidbID, 'include'))
    if ( myEpisodeNumber.toInteger() > myAniDBEpisodeCountWithSpecials ) {
      println "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
      switch (renamePass) {
        case 1:
          // Ep # > AniDB Ep #?
          //    EP # > AniDB EP # + Specials?
          //      Y: TVDBID?
          //        Y: 1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
          //           1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
          //        N: Animelist Entry?
          //           Y: 1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB, TVDBID from Animelist (not from our match)
          //              1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB, TVDBID from Animelist (not from our match)
          //              2: STOP
          //              2: STOP
          //           N: 1: AniDB Fallback
          //              2: STOP
          if (tvdbID > 0) {
            println "----------- Using TVDB ID Supplied."
            if (absoluteOrdering) {
              log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
            } else {
              log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
            }
          } else {
            println "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
            if ( anidbMatchDetails.hasAnimeListEntry) {
              def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
              println "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
              if (absoluteOrdering) {
                log.finest "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
                return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials)
              } else {
                log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
                return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails)
              }
            }
            println "------------- No AnimeList Entry. AniDB Fallback will be used (good luck with that)"
            renameOptionsSet = true
            renameStrict = true
            renameQuery = anidbID
            performRename = true
            renameDB = 'AniDB'
            renameOrder = 'Absolute'
            renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
            renameFilter = ''
            if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              println '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
          break
        case 2:
          println "----------- Renamepass:2 Can't match to AniDB"
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
        default:
          println "----------- unknown:1-2.3"
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    } else {
      //      N:  1: Rename with AniDB as "Special"
      //          2: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
      //          2: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
      println "----------- Episode Number:[${myEpisodeNumber}] is less then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
      println "----------- Episode Number:[${myEpisodeNumber}] is probably a special"
      switch (renamePass) {
        case 1:
          println '----------- Using AniDB (Filter as Special)'
          isSpecialEpisode = true
          renameQuery = anidbID
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = 'episode.special'
          renameStrict = true
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( tvdbID > 0 ) {
            println "----------- Using TVDB ID Supplied."
            if (absoluteOrdering) {
              log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, true)
            } else {
              log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
              return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, true)
            }
          } else {
            println "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
            if ( anidbMatchDetails.hasAnimeListEntry) {
              def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
              println "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
              if (absoluteOrdering) {
                log.finest "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
                return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, true)
              } else {
                log.finest "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
                return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, true)
              }
            }
            println "------------- No AnimeList Entry. AniDB Fallback will be used (No Episode Filter)"
            renameOptionsSet = true
            renameStrict = true
            renameQuery = anidbID
            performRename = true
            renameDB = 'AniDB'
            renameOrder = 'Absolute'
            renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
            renameFilter = ''
            if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              println '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
          break
        default:
          println "----------- unknown:1-2.4"
          println '//-----------------------------//'
          println '//            STOP             //'
          println '//-----------------------------//'
          renameOptionsSet = true
          renameQuery = ''
          performRename = false
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          break
      }
    }
  }
  return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
}

/**
 * Decision Tree using the input group of files and the JWD Matches for AniDB/TheTVDB to determine which groupGenerationBy method to use for the 1st and 2nd Episode passes
 * The available groupGenerationBy methods are:
 *  groupGenerationByAnimeLists  - When we have an AID and TVDBID
 *  groupGenerationByTVDB - When we don't have an AID OR want to use TVDB to do the renaming (even if we have an AID)
 *  groupGenerationByAniDB - When we don't have an TVDBID OR want to use AniDB to do the renaming (even if we have an TVDBID)
 *
 *
 * @param f The file we are processing the rename options for
 * @param tvdbID TheTVDB ID
 * @param renamePass Rename Pass (1 or 2)
 * @param animeOffLineDatabaseJsonObject Json Object for Anime Offline Database
 * @param group The LinkedHashMap for this group of files this file is part of
 * @param hasSeasonality Boolean: Does this file have "Seasonality"?
 * @param mySeasonalityNumber If hasSeasonality = true, this will be the Season as indicated by the *filename*
 * @param useNonStrictOnAniDBFullMatch Boolean: Do we use Non-Strict when we have a  JWD "Full Match" for the AniDB ID
 * @param useNonStrictOnAniDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap episodeRenameOptionPassOne(Integer renamePass, LinkedHashMap group, ArrayList<File> files, Boolean hasSeasonality, Integer mySeasonalityNumber, BigDecimal firstANIDBWTMatchNumber, BigDecimal secondANIDBWTMatchNumber, BigDecimal thirdANIDBWTMatchNumber, BigDecimal fileBotANIDBJWDMatchNumber, LinkedHashMap anidbFirstMatchDetails, LinkedHashMap anidbSecondMatchDetails, LinkedHashMap anidbThirdMatchDetails, LinkedHashMap fileBotANIDBJWDMatchDetails, BigDecimal firstTVDBDWTMatchNumber, BigDecimal secondTVDBDWTMatchNumber, BigDecimal thirdTVDBDWTMatchNumber, BigDecimal fileBotTheTVDBJWDMatchNumber, LinkedHashMap theTVDBFirstMatchDetails, LinkedHashMap theTVDBSecondMatchDetails, LinkedHashMap theTVDBThirdMatchDetails, LinkedHashMap fileBotTheTVDBJWDMatchDetails, Boolean performRename, Boolean fileBotAniDBMatchUsed, Boolean animeFoundInAniDB, Boolean animeFoundInTVDB, Boolean fileBotTheTVDBMatchUsed, Integer statsRenamedUsingScript, Integer statsRenamedUsingFilebot, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, JsonObject animeOffLineDatabaseJsonObject) {
  LinkedHashMap groupByRenameOptions
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Boolean firstPassOptionsSet = false
  println '// ---------- deliberations on order, DB, filter ---------- //'
  // --- airdate Syntax --- //
  if (group.order == 'airdate') {
    println '//--- Airdate Syntax'
    if ( animeFoundInTVDB ) {
      if ( fileBotTheTVDBMatchUsed ){
        renamerSource = 'filebot'
      }
      println '--- Anime found in TheTVDB'
      if ( firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
        println '----- 1st TVDB match 0.98+'
        if ( theTVDBFirstMatchDetails.alias == true ) {
          println '------- 1st TVDB match is an Alias (Increased Chance AniDB Series is not Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            println "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            println "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            println '------- We can use AnimeLists as 1st AniDB match 0.98+'
            println "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
        if ( theTVDBFirstMatchDetails.alias == false ) {
          println '------- 1st TVDB match is NOT an alias (Increased Chance AniDB Series is Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            println "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            println "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            println '------- We can use AnimeLists as  1st AniDB match 0.98+'
            println "------- Sending to groupGenerationByAnimeLists, PreferedDB: tvdb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'tvdb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && fileBotTheTVDBJWDMatchNumber > 0.9800000000000000000 )   {
        println '----- Filebot TVDB match 0.98+, 1st TVDB Match 0.98-'
        if ( fileBotTheTVDBJWDMatchDetails.alias == true ) {
          println '------- filebot TVDB match is an Alias (Increased Chance AniDB Series is not Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            println "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            println "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingFilebot++
            renamerSource = 'filebot'
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            println '------- We can use AnimeLists as 1st AniDB match 0.98+'
            println "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, fileBotTheTVDBJWDMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
        if ( fileBotTheTVDBJWDMatchDetails.alias == false ) {
          println '------- filebot TVDB match is NOT an alias (Increased Chance AniDB Series is Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            println "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            println "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingFilebot++
            renamerSource = 'filebot'
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            println '------- We can use AnimeLists as 1st AniDB match 0.98+'
            println "------- Sending to groupGenerationByAnimeLists, PreferedDB: tvdb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'tvdb', anidbFirstMatchDetails, fileBotTheTVDBJWDMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && (fileBotTheTVDBJWDMatchNumber < 0.9800000000000000000 || fileBotTheTVDBMatchUsed || fileBotTheTVDBJWDMatchNumber == 0) ) {
        println '------ None of our TVDB Options are above 0.98+, exploring ANIDB'
        if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
          println "------- using 1st AniDB match as it's 0.98+"
          println "------- Sending to groupGenerationByAniDB"
          groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          firstPassOptionsSet = true
          statsRenamedUsingScript++
        }
        if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 && fileBotANIDBJWDMatchDetails.dbid != anidbFirstMatchDetails.dbid ) {
          println "------- using Filebot match as it's 0.98+"
          println "------- Sending to groupGenerationByAniDB"
          statsRenamedUsingFilebot++
          groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, fileBotANIDBJWDMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          firstPassOptionsSet = true
        }
        if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && firstPassOptionsSet == false) {
          println "------- using 2nd AniDB match as it's 0.98+"
          println "------- Sending to groupGenerationByAniDB"
          groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, secondANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
          firstPassOptionsSet = true
          statsRenamedUsingScript++
        }
      }
      if ( !firstPassOptionsSet ) {
        println '//-----------------------------//'
        println '//  STOP - airdate.1-1st.4    //'
        println '//-----------------------------//'
        firstPassOptionsSet = true
        groupByRenameOptions = files.groupBy { File f ->
          performRename = false
          renameQuery = ''
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          isSpecialEpisode = false
          isMovieType = false
          isSpecialType = false
          [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
        }
      }
    }
    if ( animeFoundInAniDB && !animeFoundInTVDB ) {
      println '--- Anime found Only in AniDB'
      if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
        println "------- using 1st AniDB match as it's 0.98+"
        println "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 ) {
        println "------- using Filebot match as it's 0.98+"
        println "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, fileBotANIDBJWDMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
        renamerSource = 'filebot'
      }
      if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && firstPassOptionsSet == false) {
        println "------- using 2nd AniDB match as it's 0.98+"
        println "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, secondANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( !firstPassOptionsSet ) {
        println '//-----------------------------//'
        println '//  STOP - airdate.2-1st    //'
        println '//-----------------------------//'
        firstPassOptionsSet = true
        groupByRenameOptions = files.groupBy { File f ->
          performRename = false
          renameQuery = ''
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          isSpecialEpisode = false
          isMovieType = false
          isSpecialType = false
          [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
        }
      }
    }
  }
  // --- Absolute Syntax --- //
  if (group.order == 'Absolute') {
    if ( fileBotAniDBMatchUsed ){
      renamerSource = 'filebot'
    }
    println "//--- Absolute Ordering Detected"
    if (( !animeFoundInAniDB || (firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber < 0.9800000000000000000 )) && animeFoundInTVDB) {
      println '--- Anime found Only in TVDB with matches above 0.98+'
      if ( firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
        println '----- Using 1st TVDB Match as it 0.98+'
        println '----- Sending to groupGenerationByTVDB'
        groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && fileBotTheTVDBJWDMatchNumber > 0.9800000000000000000 ) {
        println '----- Using Filebot TVDB Match as it 0.98+'
        println '----- Sending to groupGenerationByTVDB'
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
        groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
      }
      if ( secondTVDBDWTMatchNumber > 0.9800000000000000000 && !firstPassOptionsSet ) {
        println '----- Using 2nd TVDB Match as it 0.98+ (wow)'
        println '----- Sending to groupGenerationByTVDB'
        firstPassOptionsSet = true
        groupByRenameOptions = groupGenerationByTVDB(files, theTVDBSecondMatchDetails.dbid, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
        statsRenamedUsingScript++
      }
      if ( firstPassOptionsSet == false ) {
        println '----- No Suitable TVDB Options found'
        println '//-----------------------------//'
        println '//  STOP - absolute.1-1st.4    //'
        println '//-----------------------------//'
        firstPassOptionsSet = true
        groupByRenameOptions = files.groupBy { File f ->
          performRename = false
          renameQuery = ''
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          isSpecialEpisode = false
          isMovieType = false
          isSpecialType = false
          [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
        }
      }
    }
    if ( firstANIDBWTMatchNumber > 0.9800000000000000000 && firstTVDBDWTMatchNumber > 0.9800000000000000000 && group.isSpecialType ) {
      println '------- 1st AniDB match 0.98+ && 1st TVDB match 0.98+ and group.isSpecialType = true'
      println '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( firstANIDBWTMatchNumber > 0.9800000000000000000 && firstTVDBDWTMatchNumber > 0.9800000000000000000 && !group.isSpecialType) {
      println '------- 1st AniDB match 0.98+ && 1st TVDB match 0.98+ and NOT a special'
      println '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 && firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
      println '------- Filebot match 0.98+'
      println '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', fileBotANIDBJWDMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( animeFoundInAniDB && firstTVDBDWTMatchNumber < 0.9800000000000000000 ) {
      println '----- Anime in ANIDB and 1st TVDB < 0.98'
      if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
        println "------- using 1st AniDB match as it's 0.98+"
        println '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 ) {
        println "------- using Filebot match as it's 0.98+"
        println '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
      }
      if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && firstPassOptionsSet == false) {
        println "------- using 2nd AniDB match as it's 0.98+"
        println '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstPassOptionsSet == false ) {
        println '--- 1st, 2nd AniDB < 0.98 && Filebot < 0.98'
        println '//-----------------------------//'
        println '//  STOP - absolute.2-1st.4      //'
        println '//-----------------------------//'
        firstPassOptionsSet = true
        groupByRenameOptions = files.groupBy { File f ->
          performRename = false
          renameQuery = ''
          renameDB = ''
          renameOrder = ''
          renameMapper = ''
          renameFilter = ''
          renameStrict = true
          isSpecialEpisode = false
          isMovieType = false
          isSpecialType = false
          [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
        }
      }
    }
    if ( firstPassOptionsSet == false ) {
      println '//-----------------------------//'
      println '//  STOP - absolute.3-1st.4    //'
      println '//-----------------------------//'
      println "animeFoundInAniDB:[${animeFoundInAniDB}], firstANIDBWTMatchNumber:[${firstANIDBWTMatchNumber}], group.isSpecialType:[${group.isSpecialType}]"
      groupByRenameOptions = files.groupBy { File f ->
        performRename = false
        renameQuery = ''
        renameDB = ''
        renameOrder = ''
        renameMapper = ''
        renameFilter = ''
        renameStrict = true
        isSpecialEpisode = false
        isMovieType = false
        isSpecialType = false
        [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
      }
    }
  }
  return [groupByRenameOptions: groupByRenameOptions, statsRenamedUsingScript: statsRenamedUsingScript, statsRenamedUsingFilebot: statsRenamedUsingFilebot]
}

