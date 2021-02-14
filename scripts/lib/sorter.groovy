package lib
//--- VERSION 1.0.6

import net.filebot.web.TheTVDBSeriesInfo
import org.apache.commons.text.similarity.JaroWinklerDistance
import com.cedarsoftware.util.io.JsonObject

import java.util.regex.Matcher

ArrayList basenameGenerator ( LinkedHashMap group, Boolean useBaseAnimeNameWithSeriesSyntax ) {
  tempBaseGeneratedAnimeNames = [] as HashSet
  baseGeneratedAnimeNames = [] as HashSet
  println '// START---------- Basename Generation ---------- //'
//  log.finest "${groupInfoGenerator(group)}"
  // println "group.class:${group.getClass()}"
  println "--- group.anime - ${group.anime}"
  switch(group.anime) {
    case ~/girl gaku ~hijiri girls square gakuin~/:
      baseAnimeName =  'Girl Gaku. Sei Girls Square Gakuin'
      break
    case ~/girl gaku/:
      baseAnimeName =  'Girl Gaku. Sei Girls Square Gakuin'
      break
    case ~/sk/:
      // while yes, sk is a short for Shaman King, however more recently it's a widely used short for SK8 The Infinity, so let's go with that for now.
      // Tho why SK vs SK8 as the short name to use ... only the release groups using SK know ..
      baseAnimeName =  'SK8 the Infinity'
      break
    case ~/rezero kara hajimeru break time/:
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
      baseAnimeName = 'Tatoeba Last Dungeon Mae no Mura no Shounen ga Joban no Machi de Kurasu You na Monogatari'
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
      group.isSpecial = true
      break
  }
  // Because relying on synonyms can be a hit or miss ..
  if ( baseAnimeName == 'shokugeki no souma' && group.seasonNumber == 4 ) {
    baseAnimeName = 'Food Wars! The Fourth Plate'
    group.seasonNumber = null
    group.hasSeasonality = false
  }
  // unfortunately it's still 2020 ..
  if ( baseAnimeName == 'dragon quest dai no daibouken' && group.yearDateInName == "2021" ) {
    group.yearDateInName = "2020"
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
    // https://wiki.anidb.net/AniDB_Definition:Romanisation
    // Particle は as wa (instead of ha)
    // Particle へ as e (instead of he)
    // Particle を as o (instead of wo)
    baseGeneratedAnimeNames += ["${returnAniDBRomanization(tempname)}"]
//    if (tempname =~ /(?i)(\swo\s)/) {
//      println '---------- Adding Hepburn Romanisation of Particle を as o (instead of wo)'
//      baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\swo\s)/, ' o ')
//    }
//    if (tempname =~ /(?i)(\she\s)/) {
//      println '---------- Adding Hepburn Romanisation of Particle へ as e (instead of he)'
//      baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\she\s)/, ' e ')
//    }
//    if (tempname =~ /(?i)(\sha\s)/) {
//      println '---------- Adding Hepburn Romanisation of Particle は as wa (instead of ha)'
//      baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\sha\s)/, ' wa ')
//    }
//    if (tempname =~ /(?i)(\sand\s)/) {
//      println '---------- Adding AniDBSyntax variation of & instead of and'
//      baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\sand\s)/, ' & ')
//    }
    // I really can't figure out why this doesn't work
//    switch(tempname) {
//      case ~/(?i)(\swo\s)/:
//        println '//--- Adding Hepburn Romanisation of Particle を as o (instead of wo)'
//        baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\swo\s)/, ' o ')
//      break
//      case ~/(?i)(\she\s)/:
//        println '//--- Adding Hepburn Romanisation of Particle へ as e (instead of he)'
//        baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\she\s)/, ' e ')
//      break
//      case ~/(?i)(\sha\s)/:
//        println '//--- Adding Hepburn Romanisation of Particle は as wa (instead of ha)'
//        baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\sha\s)/, ' wa ')
//      break
//      case ~/(?i)(\sand\s)/:
//        println '//--- Adding AniDBSyntax variation of & instead of and'
//        baseGeneratedAnimeNames += tempname.replaceAll(/(?i)(\sand\s)/, ' & ')
//      break
//    }
  }
  // baseGeneratedAnimeNames = baseAnimeNameGenerator()
//  log.finest "${groupInfoGenerator(group)}"
  println '// END---------- Basename Generation ---------- //'
//    return [[anime: group.anime, altTitle: group.altTitle, filebotMovieTitle: group.filebotMovieTitle, order: group.order, airdateSeasonNumber: group.airdateSeasonNumber, mov: group.mov, isFileBotDetectedName: group.isFileBotDetectedName, hasSeriesSyntax: group.hasSeriesSyntax, seriesNumber: group.seriesNumber, hasSeasonality: group.hasSeasonality, seasonNumber: group.seasonNumber, hasOrdinalSeasonality: group.hasOrdinalSeasonality, ordinalSeasonNumber: group.ordinalSeasonNumber, hasPartialSeasonality: group.hasPartialSeasonality, partialSeasonNumber: group.partialSeasonNumber, isSpecial: group.isSpecial, specialType: group.specialType, yearDateInName: group.yearDateInName], baseGeneratedAnimeNames ]
  return [[group], baseGeneratedAnimeNames ]
}


String groupInfoGenerator ( def group ) {
  def groupInfo = "Group: $group.anime, order: $group.order"
  if ( group.altTitle != null ) { groupInfo = groupInfo + ", altTitle: $group.altTitle" }
  if ( group.filebotMovieTitle != null ) { groupInfo = groupInfo + ", filebotMovieTitle: $group.filebotMovieTitle" }
  if ( group.order == 'airdate') { groupInfo = groupInfo + ", airdateSeasonNumber: $group.airdateSeasonNumber" }
  if ( group.mov ) { groupInfo = groupInfo + ", mov: $group.mov" }
  if ( group.isFileBotDetectedName ) { groupInfo = groupInfo + ", isFileBotDetectedName: $group.isFileBotDetectedName" }
  if ( group.hasSeriesSyntax ) { groupInfo = groupInfo + ", hasSeriesSyntax: $group.hasSeriesSyntax, seriesNumber: $group.seriesNumber" }
  if ( group.hasSeasonality ) { groupInfo = groupInfo + ", hasSeasonality: $group.hasSeasonality, seasonNumber: $group.seasonNumber" }
  if ( group.hasOrdinalSeasonality ) { groupInfo = groupInfo + ", hasOrdinalSeasonality: $group.hasOrdinalSeasonality, ordinalSeasonNumber: $group.ordinalSeasonNumber" }
  if ( group.hasPartialSeasonality ) { groupInfo = groupInfo + ", hasPartialSeasonality: $group.hasPartialSeasonality, partialSeasonNumber: $group.partialSeasonNumber" }
  if ( group.isSpecial ) { groupInfo = groupInfo + ", isSpecial: $group.isSpecial, specialType: $group.specialType" }
  if ( group.yearDateInName != null ) { groupInfo = groupInfo + ", yearDateInName: $group.yearDateInName" }
  if ( group.releaseGroup != null ) { groupInfo = groupInfo + ", releaseGroup: $group.releaseGroup" }
  return groupInfo
}

@SuppressWarnings('GrReassignedInClosureLocalVar')
LinkedHashMap filebotAnidbJWDSearch(HashSet animeSeriesNames, LinkedHashMap aniDBJWDResults, Boolean animeFoundInAniDB, Locale locale, String aniDBTitleXMLFilename, String aniDBSynonymXMLFilename, Boolean useFilebotAniDBAliases) {
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
      myOptionsAniDB += anidbXMLTitleSearch(aniDBTitleXML, ["${myQueryAniDB}"] as Set, locale, false, false, false, false)
      myOptionsAniDB += anidbXMLTitleSearch(aniDBSynonymXML, ["${myQueryAniDB}"] as Set, locale, false, false, false, false)
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
      myOptionsAniDB += anidbXMLTitleSearch(aniDBTitleXML, ["${myQueryAniDB}"] as Set, locale, true, false, false, false)
      myOptionsAniDB += anidbXMLTitleSearch(aniDBSynonymXML, ["${myQueryAniDB}"] as Set, locale, true, false, false, false)
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
            gotAniDBID = anidbXMLTitleSearch(aniDBTitleXML, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, false)[0] // It returns a linkedHashSet
            if ( gotAniDBID <= 0 ) {
              gotAniDBID = anidbXMLTitleSearch(aniDBSynonymXML, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, false)[0] // It returns a linkedHashSet
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
            // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false]]
          } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false]]
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
            // println "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true]]
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
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false]]
        } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
          // println "higher"
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false]]
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
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            // println "higher"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true]]
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

@SuppressWarnings('GroovyUnusedAssignment')
LinkedHashMap searchForMoviesJWD(LinkedHashMap group, String aniDBTitleXMLFilename, String aniDBSynonymXMLFilename, Boolean useFilebotAniDBAliases, Locale locale, JsonObject animeOfflineDatabase) {
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
  returnThing = filebotAnidbJWDSearch( baseAnimeNames, anidbJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases)
  anidbJWDResults = returnThing.jwdresults
  animeFoundInAniDB = returnThing.animeFoundInAniDB
  if ( group.filebotMovieTitle != null ) {
    returnThing2 = filebotAnidbJWDSearch( filebotBaseAnimeNames, fileBotAniDBJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases)
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
