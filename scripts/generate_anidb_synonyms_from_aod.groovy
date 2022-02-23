#!/usr/bin/env filebot -script
//--- VERSION 1.0.1
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlUtil
import net.filebot.hash.Hash
import net.filebot.web.Link

import java.nio.charset.Charset

// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
@Grapes(
    @Grab(group='org.apache.commons', module='commons-text', version='1.9')
)

// ---- Imports ---- //
import java.time.format.DateTimeFormatter
import org.apache.commons.text.similarity.JaroWinklerDistance

// log input parameters
log.fine("Run script [$_args.script] at [$now]")

// Define a set list of Arguments?
_def.each { n, v -> log.finest('Parameter: ' + [n, n =~ /plex|kodi|pushover|pushbullet|discord|mail|myepisodes/ ? '*****' : v].join(' = ')) }
args.withIndex().each { f, i -> if (f.exists()) { log.finest "Argument[$i]: $f" } else { log.finest "Argument[$i]: File does not exist: $f" } }

// initialize variables
failOnError = _args.conflict.equalsIgnoreCase('fail')
testRun = _args.action.equalsIgnoreCase('test')
//testRun = license == null || _args.action.equalsIgnoreCase('test')

// extra options, myepisodes updates and email notifications
String outputFilename         = tryQuietly { outputFilename.toString() }
String aniDBuserAgent         = any { aniDBuserAgent.toString() } { 'ihavenoaccount/filebot' }
String aniDBTitleXMLFilename  = any { aniDBTitleXMLFilename.toString() } { 'anime-titles.xml' }
Integer aniDBXMLrefreshDays   = any { aniDBXMLrefreshDays.toInteger() } { 1 }

// --output folder must be a valid folder
//**// If you don't pass --output (so _args.output is null) it will default to Current Working Directory)
outputFolder = tryLogCatch { any { _args.output } { '.' }.toFile().getCanonicalFile() }

// Set the Locale to English by default.
def locale                    = any { _args.language.locale } { Locale.ENGLISH }

// Include Shared & Anidb Libraries
include('lib/shared') // Generic/Shared Functions
include('lib/anidb')  // Download of AniDB Titles XML file
include('lib/tvdb')  // tvdb Search file
include('lib/manami')  // Anime OFfline Database
include('lib/detect') // Detect Functions

// ---------- Download AniDB's Title XML ---------- //
log.finest "aniDBTitleXMLFilename: ${aniDBTitleXMLFilename}"
aniDBXMLDownload(aniDBuserAgent, aniDBTitleXMLFilename, aniDBXMLrefreshDays)
// --- You need to turn off the Namespace awareness else you will get this wierdness when trying to parse the languages for the titles.
// --- title[attributes={{http://www.w3.org/XML/1998/namespace}lang=en, type=short}; value=[CotS]]
def aniDBTitleXML = new groovy.xml.XmlParser(false, false).parse(aniDBTitleXMLFilename) // XMLParser

// ---------- Download/Cache Anime Offline database ---------- //
// https://github.com/manami-project/anime-offline-database
// Json - https://github.com/manami-project/anime-offline-database/raw/master/anime-offline-database.json
animeOfflineDatabase = Cache.getCache('animne-offline-database-json', CacheType.Persistent).json('anime-offline-database.json') {
  new URL('https://raw.githubusercontent.com/manami-project/anime-offline-database/master/' + it) }.expire(Cache.ONE_DAY).get()
// println "${animeOfflineDatabase.getClass()}" // com.cedarsoftware.util.io.JsonObject
// https://www.javadoc.io/doc/com.Thecedarsoftware/json-io/latest/com/cedarsoftware/util/io/JsonObject.html
//    --- Which means we can work directly with animeOfflineDatabase, effectively loading it into an object (just like with Groovy's JsonSlurper)
// def animeOfflineDatabase = new JsonSlurper().parse(new File('anime-offline-database-test.json'))
//def animeOfflineDatabase = new groovy.json.JsonSlurper().parse(new File('../3rdPartyLists/anime-offline-database/anime-offline-database.json'))




aniDBTitleEntries = [:]
aniDBTitleOfficial = [:]
aniDBTitleCompareOfficial = [:]
aniDBTitleSynonyms = [] as HashSet
aniDBTitleCompareSynonyms = [] as HashSet
aodAniDBEntries = [:]
aodAniDBSynonyms = [] as HashSet
aodAniDBCompareSynonyms = [] as HashSet
aodNotAniDBEntries = [:]
aodNotAniDBSynonyms = [] as HashSet
aodNotAniDBCompareSynonyms = [] as HashSet


//-----------  Load up Anime Offline Database -----------------//
animeOfflineDatabase.data.eachWithIndex { aodentry, aodIndex ->
  aodAniDBSynonyms = []
  aodAniDBCompareSynonyms = []
  aodNotAniDBSynonyms = []
  aodNotAniDBCompareSynonyms = []
  // --- Split entries into AniDB entries and Non-AniDB Entries --- //
  if (aodentry.sources =~ /anidb/) {
    // --- AniDB Entries --- //
    def matcher = aodentry.sources =~ /https:\/\/anidb\.net\/anime\/(\d+)/
    anidbID = matcher[0][1].toInteger()
//    log.finest "AniDB ID:[${anidbID}]"
    titleText = aodentry.title.toLowerCase()
    titleCompareText = altjwdStringBlender(aodentry.title)
//    log.finest "AOD Title:[${titleText}]"
//    log.finest "AOD Compare Title:[${titleCompareText}]"
    aodentry.synonyms.collect { it }.each {
      myRegexMatcher = it =~ /^[ -~]*$/
      if (myRegexMatcher.find()) {
//        log.finest "IT:[${it}]"
        aodAniDBSynonyms += ["${it.toLowerCase()}"]
        aodAniDBCompareSynonyms += ["${altjwdStringBlender(it)}"]
      }
    }
    entryYear = aodentry.animeSeason.year
//    println "AOD Synonyms:${aodAniDBSynonyms}"
//    println "AOD Compare Synonyms:${aodAniDBCompareSynonyms}"
//    log.finest "[title: ${titleText}, titlecompare: ${titleCompareText}, synonyms: ${aodAniDBSynonyms}, synonymscompare: ${aodAniDBCompareSynonyms}, entryyear: ${entryYear}]"
    aodAniDBEntries += [(anidbID): [title: titleText, titlecompare: titleCompareText, synonyms: aodAniDBSynonyms, synonymscompare: aodAniDBCompareSynonyms, entryyear: entryYear]]
  } else {
    // --- Non-AniDB Entries --- //
    myRegexMatcher = aodentry.title =~ /^[ -~]*$/
    if (myRegexMatcher.find()) {
      titleText = aodentry.title.toLowerCase()
//      log.finest "Non-AniDB Entry:[${titleText}]"
      titleCompareText = altjwdStringBlender(aodentry.title)
      entryYear = aodentry.animeSeason.year
//      log.finest "Non-AniDB Year:[${entryYear}]"
      aodentry.synonyms.collect { it }.each {
        myRegexMatcher = it =~ /^[ -~]*$/
        if (myRegexMatcher.find()) {
//          log.finest"IT:[${it}]"
          aodNotAniDBSynonyms += ["${it.toLowerCase()}"]
          aodNotAniDBCompareSynonyms += ["${altjwdStringBlender(it)}"]
        }
      }
//      log.finest "AOD Synonyms:${aodNotAniDBSynonyms}"
//      log.finest "AOD Compare Synonyms:${aodNotAniDBCompareSynonyms}"
      // For Titles that end with a number,  Season n, Ordinal Season or Part n
      // Create an alternative title tht replaces the seasonality with the year
      // This hopefully will assist in matching the correct AniDB entry where a season
      // is denoted by the Year and not Seasonality Syntax.
      //-----------  Create Alternative AniDB Title based on YEAR -----------------//
      altTitle = titleText
      altTitleCompare = titleCompareText
      if ( aodentry.type == "TV" ) {
        workingName = jwdStringBlender(titleText)
        mySeasonalityRegexMatcher = workingName =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3})(v[\d]{1,2}|[a-zA-Z])?\s?$/
        // aka "title": "Gate: Jieitai Kanochi nite, Kaku Tatakaeri 2"
        if ( mySeasonalityRegexMatcher.find() ) {
//          log.finest "-------- ${workingName}: has Numerical series Syntax (anime xx)"
          anime = workingName.replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}(v[\d]{1,2}|[a-zA-Z])?\s?)$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
          altTitleCompare = altjwdStringBlender(anime + ' ' + entryYear)
          altTitle = anime + ' ' + entryYear
//          log.finest "Generated altTitle:[${altTitle}] for Title:[${titleText}]"
        }
        myTVDBSeasonalityRegexMatcher = workingName =~ /(?i)(\s+(season)\s*([\d]+))/ // Season xx
        if ( myTVDBSeasonalityRegexMatcher.find() ) {
//          log.finest "-------- ${workingName}: name has Seasonality (Season xx)"
          anime = workingName.replaceAll(/(?i)(\s+(season)\s*([\d]+))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
          altTitleCompare = altjwdStringBlender(anime + ' ' + entryYear)
          altTitle = anime + ' ' + entryYear
//          log.finest "Generated altTitle:[${altTitle}] for Title:[${titleText}]"
        }
        myTVDBSeasonalityRegexMatcher = workingName =~ /(?i)([-\s]*S)([\d]{1,2})\b/ // Matches S0, S1, S12 as well as preceeding - and spaces (unlimited number of them)
        if ( myTVDBSeasonalityRegexMatcher.find() ) {
//          log.finest  "-------- ${workingName}: name has Seasonality (Sx)"
          anime = workingName.replaceAll(/(?i)([-\s]*S)([\d]{1,2})\b/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
          altTitleCompare = altjwdStringBlender(anime + ' ' + entryYear)
          altTitle = anime + ' ' + entryYear
//          log.finest "Generated altTitle:[${altTitle}] for Title:[${titleText}]"
        }
        myOrdinalSeasonalityMatcher = workingName =~ /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+|X|IX|VIII|VII|VI|V|IV|III|II|I))/ // 2nd Season, 3rd Season, Part 1, Part 2, 2nd part, Season 2 etc.
        if ( myOrdinalSeasonalityMatcher.find() ) {
//          log.finest  "--------${workingName} name has Ordinal or Partial/TVDB Seasonality"
          anime = workingName.replaceAll(/(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
          altTitleCompare = altjwdStringBlender(anime + ' ' + entryYear)
          altTitle = anime + ' ' + entryYear
//          log.finest "Generated altTitle:[${altTitle}] for Title:[${titleText}]"
        }
      }
//      log.finest "[title: ${titleText}, alttitle: ${altTitle} , alttitlecompare: ${altTitleCompare}, titlecompare: ${titleCompareText}, synonyms: ${aodNotAniDBSynonyms}, synonymscompare: ${aodNotAniDBCompareSynonyms}, entryyear:${entryYear}]"
      aodNotAniDBEntries += [ (titleText):[title: titleText, alttitle: altTitle , alttitlecompare: altTitleCompare, titlecompare: titleCompareText, synonyms: aodNotAniDBSynonyms, synonymscompare: aodNotAniDBCompareSynonyms, entryyear:entryYear] ]
    }
  }
}
//println "${ aodAniDBEntries }"
//println "${aodAniDBEntries[8606]}" // [title:?lDLIVE, titlecompare:?ldlive, synonyms:[Eldlive, elDLIVE], synonymscompare:[eldlive, eldlive]]
//println "${aodAniDBEntries.containsKey(11970)}" // true
//aodAniDBEntries.each {anidbentry ->
//  println "${anidbentry.key}" // AID # aka 11970
//  println "${anidbentry.value.title}" // Title aka ?lDLIVE
//}
//aodNotAniDBEntries.each {notAnidbentry ->
//  // println "${notAnidbentry}" // zuttosukidatta={title=Zutto Suki Datta, year=2017, titlecompare=zuttosukidatta, synonyms=[], synonymscompare=[]}
//  println "${notAnidbentry.key}" // zuttosukidatta
//  println "${notAnidbentry.value.title}" // Zutto Suki Datta
//}



//-----------  Load up AniDB's Title XML -----------------//
aniDBTitleXML.children().each { anidbAnimeEntry ->
  aniDBTitleOfficial = []  as HashSet
  aniDBTitleCompareOfficial = [] as HashSet
  aniDBTitleSynonyms = []  as HashSet
  aniDBTitleCompareSynonyms = []  as HashSet
  anidbID = anidbAnimeEntry['@aid'].toInteger()
  aniDBTitleForSynonymXML = ''
//  log.finest "anidbID:[${anidbID}]"
  anidbAnimeEntry.children().each { anidbEntryTitles ->
    if ((anidbEntryTitles['@type'] == 'main') || (anidbEntryTitles['@type'] == 'official') ) {
      titleText = anidbEntryTitles.text()
      myRegexMatcher = titleText =~ /^[ -~]*$/
      if (myRegexMatcher.find()) {
//        log.finest "Main/Official - anidbEntryTitles:[${titleText}]"
        aniDBTitleOfficial += ["${titleText.toLowerCase()}"]
        aniDBTitleCompareOfficial += ["${altjwdStringBlender(titleText)}"]
        //  ((anidbEntryTitles['@type'] == 'main') && (anidbEntryTitles['@xml:lang'] == 'x-jat'))
        // ((anidbEntryTitles['@type'] == 'official') && (anidbEntryTitles['@xml:lang'] == 'en'))
        if ( anidbEntryTitles['@xml:lang'] =~ /^x-/ || anidbEntryTitles['@xml:lang'] == 'en') {
          aniDBTitleForSynonymXML = "${titleText.toLowerCase()}"
        }
      }
    }
    if ( anidbEntryTitles['@type'] == 'syn' || anidbEntryTitles['@type'] == 'short' ) {
      titleText = anidbEntryTitles.text()
      myRegexMatcher = titleText =~ /^[ -~]*$/
      if (myRegexMatcher.find()) {
//        log.finest "Synonym - anidbEntryTitles:[${titleText}]"
        aniDBTitleSynonyms += ["${titleText.toLowerCase()}"]
        aniDBTitleCompareSynonyms += ["${altjwdStringBlender(titleText)}"]
      }
    }
    if (aniDBTitleOfficial != [] ) {
      aniDBTitleEntries += [(anidbID): [aniDBTitleForSynonymXML: aniDBTitleForSynonymXML, title: aniDBTitleOfficial, titlecompare: aniDBTitleCompareOfficial, synonyms: aniDBTitleSynonyms, synonymscompare: aniDBTitleCompareSynonyms]]
    }
  }
}
//println "${aniDBTitleEntries[2]}" // [title:[3x3 eyes, 3x3 occhi, 3x3 augen (ova 1), 3x3 ojos [1-4], 3x3 ulls], titlecompare:[3x3ulls, 3x3augenova1, 3x3eyes, 3x3ojos14, 3x3occhi], synonyms:[3x3 eyes - immortals, sazan eyes, southern eyes], synonymscompare:[sazaneyes, southerneyes, 3x3eyesimmortals]]
//println "${aniDBTitleEntries.containsKey(2)}" // true
//aniDBTitleEntries.each {aid, animetitles ->
//  println "${aid}"
//  println "${animetitles.title}"
//}

// --- Search AniDB, Return a set of matching AniDB IDs --- //
Set aniDBTitleSearch(LinkedHashMap aniDBTitleEntries, Set searchList) {
  HashSet resultsAsSet = []
  Integer anidbID = 0
  def myRegexMatcher
  def searchItemString
  def titleText
  aniDBEntryTitles = [] as HashSet
  aniDBTitleEntries.each {aid, anidbAnimeEntry ->
//    log.finest "${aid}"
//    log.finest "${anidbAnimeEntry}"
    aniDBEntryTitles = anidbAnimeEntry.titlecompare
    aniDBEntryTitles += anidbAnimeEntry.synonymscompare
//    log.finest "aniDBEntryTitles:${aniDBEntryTitles}"
    searchList.each { searchItem ->
//      log.finest "searchItem:[${searchItem}]"
      aniDBEntryTitles.each { title ->
        if ( searchItem == title ) {
          resultsAsSet << aid
        }
      }
    }
  }
//  log.finest "resultsAsSet:${resultsAsSet}"
  return resultsAsSet
}

// --- Search AOD AniDB Entries --- //
static Set aodAniDBSearch(LinkedHashMap aodAniDBEntries, Set searchList, Boolean searchSynonyms = false, Boolean returnAID = false) {
  HashSet resultsAsSet = []
  def aodtitles
  aodAniDBEntries.each { anidbID, aodentry ->
    if ( searchSynonyms ) {
      aodtitles = []
      aodtitles += aodentry.titlecompare
//      log.finest "aodentry.titlecompare.class()[${aodentry.titlecompare.getClass()}" // java.lang.String
      aodtitles += aodentry.synonymscompare.flatten()
//      log.finest "aodentry.synonymscompare.class()[${aodentry.synonymscompare.getClass()}" // java.util.ArrayList
    }
      searchList.each { searchItem ->
//       log.finest "searchItem.class()[${searchItem.getClass()}" // java.lang.String
        if ( searchSynonyms ) {
          aodtitles.each { titlecompare ->
//            log.finest "titlecompare.class()[${titlecompare.getClass()}" // org.codehaus.groovy.runtime.GStringImpl
            if ( searchItem == titlecompare ) {
//              log.finest "Found this exact Match (1.0) [${searchItem}] to [${titlecompare}]"
              returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
            }
          }
        } else {
          if ( searchItem == aodentry.titlecompare ) {
//          log.finest "Found this exact Match (1.0) [${searchItem}] to [${aodentry.titlecompare}]"
            returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
          }
        }
      }
    }
  return resultsAsSet
}

// --- Search AOD Non-AniDB Entries --- //
Set aodNotAniDBSearch(LinkedHashMap aodNotAniDBEntries, Set searchList, Boolean searchSynonyms = false) {
//  log.finest "aodNotAniDBEntries.getClass()[${aodNotAniDBEntries.getClass()}" // java.util.LinkedHashMap
//  log.finest "searchList.class()[${searchList.getClass()}" // java.util.HashSet
  HashSet resultsAsSet = []
  HashSet aodtitles = []
  aodNotAniDBEntries.each { title, aodentry ->
//    log.finest "title.class()[${title.getClass()}" // java.lang.String
//    log.finest "aodentry.class()[${aodentry.getClass()}" //  java.util.LinkedHashMap
    if ( searchSynonyms ) {
      aodtitles = []
      aodtitles += aodentry.titlecompare
      aodtitles += aodentry.alttitlecompare
//      log.finest "aodentry.titlecompare.class()[${aodentry.titlecompare.getClass()}"
      aodtitles += aodentry.synonymscompare.flatten()
//      log.finest "aodentry.synonymscompare.class()[${aodentry.synonymscompare.getClass()}"
    }
    searchList.each { searchItem ->
//       log.finest "searchItem.class()[${searchItem.getClass()}" // java.lang.String
      if ( searchSynonyms ) {
        aodtitles.each { titlecompare ->
//           log.finest "titlecompare.class()[${titlecompare.getClass()}"
          if ( searchItem == titlecompare ) {
//            log.finest "Found this exact Match (1.0) [${searchItem}] to [${titlecompare}]"
            resultsAsSet += aodentry.title
          }
        }
      } else {
        if ( searchItem == aodentry.titlecompare || searchItem == aodentry.alttitlecompare) {
//          log.finest "Found this exact Match (1.0) [${searchItem}] to [${aodentry.titlecompare}] or [${aodentry.alttitlecompare}]"
          resultsAsSet += aodentry.title
        }
      }
    }
  }
  return resultsAsSet
}
// Log levels from lowest to highest aka least to most verbose (I think)
// log.warning
// log.fine
// log.finest

// TODO
// Treat these as the same for Synonyms aka ` and '
    // <title xml:lang="en" type="syn">so, i can't play h</title>
    // <title xml:lang="en" type="syn">so, i can`t play h</title>

// --- MAIN --- //
animeSynonyms = [:]
animeTitles = [:]
aniDBTitle = ''
log.warning "---------- Start Processing AniDB Entries from AOD ----------"
/* Parse my list of AniDB Entries from AOD */
aodAniDBEntries.each { Integer anidbID, aodentry ->
  LinkedHashMap searchMasterList = [:]
  HashSet newLeadSearchSet = []
  HashSet newLeadGeneratorSet  = []
  log.finest "anidbID:[${anidbID}], aodentry.title:[${aodentry.title}]"
  /*  Get the Title from AniDB XML AND validate there is an entry
    If there is no entry, then skip work on this AOD Entry.*/
  if ( aniDBTitleEntries.containsKey(anidbID) ) {
    aniDBTitle = aniDBTitleEntries.get(anidbID).aniDBTitleForSynonymXML.toLowerCase()
    log.finest "Found AID[${anidbID}] ANIDB Title: ${aniDBTitle} with AOD Title: ${aodentry.title}"
  } else {
    log.finest "Aod Entry ${aodentry.title} with AID ${anidbID} does not exist in AniDB Title XML"
    return
  }
  /* searchMasterList contains the titles that we will be checking to see if they do not already exist as
  * an title/synonym/short for an AniDB Title. As we are doing lots of compares on it, we add a "compare"
  * version to the  map as well as the "normal" version. This allows for doing a simple == comparision vs
  * a more complex/costly regex comparision */
  searchMasterList << [(aodentry.titlecompare): aodentry.title]
  /* newLeadGeneratorSet will be used to look for AOD entries that haven't been merged with an AniDB Entry and basically
  *  assume if the title "matches" the AniDB entry that it's the same Anime, and then grab that entries Title/Synonyms
  *  for consideration as new Synonyms for the AniDB entry we are currently working on. As we are using the AniDB
  *  Synonyms/Short, which are of low validity, there is certainly going to be "false" matches. If it was as simple as
  *  this to get high validity matches, AOD would have likely already merged the entries :) I am not using any of the
  *  Synonyms from AOD as it is known to have duplicate matching synonyms, which would guarantee "false" matches. */
  newLeadGeneratorSet += ["${aodentry.title}"]
  newLeadGeneratorSet += aniDBTitleEntries.get(anidbID).title // All the AniDB XML Official/Main that don't use foreign character sets
  newLeadGeneratorSet += aniDBTitleEntries.get(anidbID).synonyms // All the AniDB XML Synonyms/Shorts that don't use foreign character sets
  log.finest "=====>newLeadGeneratorSet:${newLeadGeneratorSet}"
  /* newLeadSearchSet is what we will use when doing the search for new Leads using the "compare" formatted entries*/
  newLeadSearchSet += altjwdStringBlender(aodentry.title)
  newLeadSearchSet += aniDBTitleEntries.get(anidbID).titlecompare // All the AniDB XML Official/Main that don't use foreign character sets
  newLeadSearchSet += aniDBTitleEntries.get(anidbID).synonymscompare // All the AniDB XML Synonyms/Shorts that don't use foreign character sets
  log.finest "=====>newLeadSearchSet(1st):${newLeadGeneratorSet}"
  /* Start looking for new Leads */
  newLeadGeneratorSet.each { anime ->
    /*  workingName is what we will be using to evaluate if we should create alternative versions of the name that are normal variations of it
    *  aka anime season 2, which is often synonoymous with anime 2nd season, anime s2 and anime 2 etc. */
    workingName = jwdStringBlender(anime)
    mySeasonalityRegexMatcher = workingName =~ /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3})(v[\d]{1,2}|[a-zA-Z])?\s?$/
    if ( mySeasonalityRegexMatcher.find() ) {
      log.finest "-------- ${workingName}: has Numerical series Syntax (anime xx)"
      // There is in fact at least ONE Anime where it is 02 vs 2 ..
      mySeasonalityNumber = mySeasonalityRegexMatcher[0][1].toInteger()
      log.finest "---------- mySeasonalityNumber: ${mySeasonalityNumber}"
      anime = anime.replaceAll(/(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}(v[\d]{1,2}|[a-zA-Z])?\s?)$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      newLeadSearchSet += altjwdStringBlender(anime + ' ' + mySeasonalityNumber)
      if ( mySeasonalityNumber >= 10 ) {
        log.finest "---------- mySeasonalityNumber[${mySeasonalityNumber}] >= 10, will not Check Ordinal/Roman Syntax"
      } else {
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber)) // anime 2nd
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getRomanOrdinal(mySeasonalityNumber)) // anime II
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' Season') // anime 2nd Season
        newLeadSearchSet += altjwdStringBlender(anime + ' Season ' + mySeasonalityNumber) // anime Season 2
        newLeadSearchSet += altjwdStringBlender(anime + ' S' + mySeasonalityNumber) // anime S2
      }
    }
    myTVDBSeasonalityRegexMatcher = workingName =~ /(?i)(\s+(season)\s*([\d]+))/ // Season xx
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      log.finest "-------- ${workingName}: name has Seasonality (Season xx)"
      mySeasonalityNumber = myTVDBSeasonalityRegexMatcher[0][3].toInteger()
      log.finest "---------- mySeasonalityNumber: ${mySeasonalityNumber}"
      anime = anime.replaceAll(/(?i)(\s+(season)\s*([\d]+))/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      newLeadSearchSet += altjwdStringBlender(anime + ' ' + mySeasonalityNumber)
      if ( mySeasonalityNumber >= 10 ) {
        log.finest "---------- mySeasonalityNumber[${mySeasonalityNumber}] >= 10, will not Check Ordinal/Roman Syntax"
      } else {
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber)) // anime 2nd
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getRomanOrdinal(mySeasonalityNumber)) // anime II
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' Season') // anime 2nd Season
        newLeadSearchSet += altjwdStringBlender(anime + ' S' + mySeasonalityNumber) // anime S2
      }
    }
    myTVDBSeasonalityRegexMatcher = workingName =~ /(?i)([-\s]*S)([\d]{1,2})\b/ // Matches S0, S1, S12 as well as preceeding - and spaces (unlimited number of them)
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      log.finest "-------- ${workingName}: name has Seasonality (Sx)"
      myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][2].toInteger()
      log.finest "---------- mySeasonalityNumber: ${mySeasonalityNumber}"
      anime = anime.replaceAll(/(?i)([-\s]*S)([\d]{1,2})\b/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      newLeadSearchSet += altjwdStringBlender(anime + ' ' + mySeasonalityNumber) // anime 2
      if ( mySeasonalityNumber >= 10 ) {
        log.finest "---------- mySeasonalityNumber[${mySeasonalityNumber}] >= 10, will not Check Ordinal/Roman Syntax"
      } else {
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber)) // anime 2nd
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getRomanOrdinal(mySeasonalityNumber)) // anime II
        newLeadSearchSet += altjwdStringBlender(anime + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' Season') // anime 2nd Season
        newLeadSearchSet += altjwdStringBlender(anime + ' Season ' + mySeasonalityNumber) // anime Season 2
      }
    }
  }
  log.finest "=====>newLeadSearchSet(2nd):${newLeadGeneratorSet}"
  /* Search the AOD Entries that didn't include AniDB for any matches */
  Set newLead = aodNotAniDBSearch(aodNotAniDBEntries, newLeadSearchSet as Set, false)
  log.finest "=====>newLead: ${newLead}"
  if (newLead.size() >= 1 ) {
    log.fine "Found AID[${anidbID}] ANIDB Title: ${aniDBTitle} with AOD Title: ${aodentry.title}"
    log.fine "--------- Which matches Non-AniDB Title(s)${newLead}"
    newLead.each { String lead ->
      // Unfortuntely Year data is not that accurate :( and is nullable from AOD
//        log.finest "${lead}"
//        log.finest "${aodNotAniDBEntries[lead]}"
//        log.finest "${anidbID}"
//        log.finest "${aodAniDBEntries[anidbID]}"
//        newLeadYear = aodNotAniDBEntries.get(lead).entryyear
//        currentYear = aodAniDBEntries.get(anidbID).entryyear
//        if ( aodNotAniDBEntries[lead].entryyear == aodAniDBEntries[anidbID].entryyear ) {
//          println "Found AID[${anidbID}] ANIDB Title: ${aniDBTitle} with AOD Title: ${aodentry.title}"
//          println "--------- Which matches Non-AniDB Title(s)${newLead}"
//          searchMasterList << [(aodNotAniDBEntries["${lead}"].titlecompare): aodNotAniDBEntries["${lead}"].title]
//          searchListAsSet << aodNotAniDBEntries["${lead}"].titlecompare
//          aodNotAniDBEntries["${lead}"].synonymscompare.eachWithIndex { synonymscompare, index ->
//            searchMasterList << [(synonymscompare): aodNotAniDBEntries["${lead}"].synonyms[index]]
//            searchListAsSet << synonymscompare
//          }
//        }
//        log.finest "${aodNotAniDBEntries["${lead}"]}"
      /* Add the matching entries title to searchMasterList  */
      searchMasterList << [(aodNotAniDBEntries["${lead}"].titlecompare): aodNotAniDBEntries["${lead}"].title]
//        searchListAsSet << aodNotAniDBEntries["${lead}"].titlecompare
      /* Add the matching entries synonyms to searchMasterList  */
      aodNotAniDBEntries["${lead}"].synonymscompare.eachWithIndex { synonymscompare, index ->
        searchMasterList << [(synonymscompare): aodNotAniDBEntries["${lead}"].synonyms[index]]
//          searchListAsSet << synonymscompare
      }
    }
  }
  /* Unfortunately there are some Anime which END with punctuation that is ALSO necessary to determine different seasons
  *  aka Dog Days` and Dog Days`` which in this specific case are Seasons 2 and 3 .. Ugh.. So I will create versions
  *  without the punctuation, and if it's a title that doesn't use it for multiple seasons it should end up as a new
  *  synonym, and if it is then it shouldn't. Why do this? Because quite a few release groups often don't add that
  *  punctation into their filenames, making it difficult to match seasons correctly if you don't use the punctuation
  *  in the compare */
  aniDBTitleEntries.get(anidbID).title.each { title ->
    if ( title =~ /(`|!){1,4}$/ ) {
      addThisTitle = title.replaceAll(/(`|!){1,4}$/, '').toLowerCase()
      log.finest "--------- Auto-Adding Synonym [${addThisTitle}]"
      searchMasterList << [ (altjwdStringBlender(addThisTitle)): addThisTitle]
    }
    if ( title =~ /^the\s/ ) {
      addThisTitle = title.replaceAll(/(?i)^the\s/, '').toLowerCase()
      log.finest "--------- Auto-Adding Synonym [${addThisTitle}]"
      searchMasterList << [ (altjwdStringBlender(addThisTitle)): addThisTitle]
    }
  }
  /* Adding in variations on official Synonyms */
  aniDBTitleEntries.get(anidbID).synonyms.each { synonym ->
    if ( synonym =~ /^the\s/ ) {
      addThisTitle = synonym.replaceAll(/(?i)^the\s/, '').toLowerCase()
      log.finest "--------- Auto-Adding Synonym [${addThisTitle}]"
      searchMasterList << [ (altjwdStringBlender(addThisTitle)): addThisTitle]
    }
  }
  /* Adding in the Synonyms from the AOD Entry */
  log.finest "--- With Synonyms: ${aodentry.synonymscompare.flatten()}"
  aodentry.synonymscompare.eachWithIndex { synonymscompare, index ->
    searchMasterList << [(synonymscompare): aodentry.synonyms[index]]
  }
  log.finest "----- Master Search List ${searchMasterList}"
  HashSet newSynonym = []
  /* Take every entry and determine if it's a new synonym for the series in AniDB, AND doesn't match multiple series */
  searchMasterList.each { titlecompare, title ->
    log.finest "Searching for title:${title}"
    gotAniDBID = aniDBTitleSearch(aniDBTitleEntries, ["${titlecompare}"] as Set)
    log.finest "--- gotAniDBID[${gotAniDBID}]"
    if ( gotAniDBID ) {
      // If we have more then 1 match, then the title is invalid (not unique)
      if (gotAniDBID.size() > 1 ) {
        log.finest "----- INVALID1: Found AID${gotAniDBID} with Title[${title}]"
      } else {
        // If the AID we got back is NOT the one we are working on, then the title is invalid
        if (gotAniDBID[0] != anidbID ) {
          log.finest "----- INVALID2: Found AID[${gotAniDBID[0]}] with Search[${title}]"
        } else {
          // Since it matches something in AniDB, it's not Unique (but at least valid)
          log.finest "----- Found AID[${gotAniDBID[0]}] with Title[${title}]"
         }
      }
    } else {
      // Since it didn't match anything in AniDB, we can check AOD
      newLead2 = aodAniDBSearch(aodAniDBEntries, ["${titlecompare}"] as Set, true, true)
      if (newLead2 && newLead2.size() > 1 ) {
        log.finest "------- INVALID3: found different AID in AOD${newLead2} with Search[${title}]"
      } else {
        if (newLead2[0] != anidbID && newLead2[0] != null ) {
          log.finest "------- INVALID4: Found AID[${newLead[0]}] with Search[${title}]"
        } else {
          /* Since I don't want to create duplicates in the file this script is generating, we need to also check our
          *  current "new" synonyms */
          newLead3 = animeSynonyms.findAll { Integer aid, HashSet synonyms ->
            synonyms.grep(title)
          }
          log.finest "=====> newLead3:${newLead3}"
          if (!newLead3.isEmpty() ) {
            log.finest "------- INVALID5: found existing Synonym Entry:${newLead3} with Search[${title}] when processing AID[${anidbID}] ANIDB Title: ${aniDBTitle} with AOD Title: ${aodentry.title}"
          } else {
            log.finest "------- New Synonym?[${title}] for AID[${anidbID}]"
            newSynonym.add("${title}")
          }
        }
      }
    }
  }
  if (newSynonym) {
    //  if newLead.size() >= 1 then we already printed the first line
    if (newLead.size() >= 1 ) {
      log.fine "--- We will add these new Synonyms${newSynonym} for AID[${anidbID}]"
    } else {
      log.fine "Found AID[${anidbID}] ANIDB Title: ${aniDBTitle} with AOD Title: ${aodentry.title}"
      log.fine "--- We will add these new Synonyms${newSynonym} for AID[${anidbID}]"
    }
    animeSynonyms << [(anidbID): newSynonym ]
    animeTitles << [(anidbID): aniDBTitle ]
  }
}
log.warning "---------- Complete ----------"

// ---------- Write Output file ---------- //
outputFilename = outputFilename ?: 'anime-synonyms.xml'
animeSeasonTitleFilename = "${outputFolder}/${outputFilename}"
StreamingMarkupBuilder xmlBuilder = new StreamingMarkupBuilder()
xmlBuilder.encoding = 'UTF-8'
def animetitles = xmlBuilder.bind{
  mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
  animetitles() {
    animeSynonyms.sort().each { aid ->
      anime(aid: aid.key) {
        title( 'xml:lang': 'en', type: 'official', "${animeTitles[aid.key]}")
        aid.value.each {
          title( 'xml:lang': 'en', type: 'syn', "${it}")
        }
      }
    }
  }
}


new File(animeSeasonTitleFilename).withWriter('utf-8') {out ->
  XmlUtil.serialize(animetitles, out)
}

