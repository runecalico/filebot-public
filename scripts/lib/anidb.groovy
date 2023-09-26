//file:noinspection unused
//file:noinspection GrMethodMayBeStatic
package lib

import com.cedarsoftware.util.io.JsonObject
import net.filebot.Logging
import net.filebot.WebServices
import net.filebot.util.XPathUtilities
import net.filebot.web.Episode
import net.filebot.web.SeriesInfo
import net.filebot.web.SortOrder
import org.apache.commons.lang3.StringUtils

//--- VERSION 1.5.2

// https://http-builder-ng.github.io/http-builder-ng/
// I couldn't figure out how to use the URL method and set a User Agent string (as required by AniDB to download the file)
// So we will use http-builder-ng instead.
// https://mvnrepository.com/artifact/io.github.http-builder-ng/http-builder-ng-core
@Grapes(
        @Grab(group='io.github.http-builder-ng', module='http-builder-ng-core', version='1.0.4')
)
// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
@Grapes(
    @Grab(group='org.apache.commons', module='commons-text', version='1.9')
)

import org.apache.commons.text.similarity.JaroWinklerDistance
import groovyx.net.http.HttpBuilder
import groovyx.net.http.optional.Download
import groovyx.net.http.FromServer
import java.nio.file.Path

/**
 * Return the "Primary" Title from an AniDB XML Offline title entry, The "Primary" Title is determined by this order:
 * Type = main & x-jat
 * Type = official & en
 * Type = main & en
 * Type = main & x-zht, then x-kot
 * Finally if it can't find any of the above it will return the first "title" in the entry (whatever that is)
 *
 * @param xmlNode The XML Node representing the AniDB XML Offline Title Entry [See anidbXMLTitleSearch()]
 * @return XML Title entry (So to get the text you would need to still use .text() on it)
 */
@SuppressWarnings('GrMethodMayBeStatic')
def anidbXMLEntryGetAnimePrimaryTitle(xmlNode) {
  Logging.log.finest "----  anidbXMLEntryGetAnimePrimaryTitle: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().find { anidbEntryTitles ->
    ((anidbEntryTitles['@type'] == 'main') && (anidbEntryTitles['@xml:lang'] == 'x-jat'))
  }
  if ( myQuery == null ) {
    myQuery = xmlNode.children().find { anidbEntryTitles ->
      ((anidbEntryTitles['@type'] == 'official') && (anidbEntryTitles['@xml:lang'] == 'en'))
    }
  } else {
    return myQuery
  }
  if ( myQuery == null ) {
    myQuery = xmlNode.children().find { anidbEntryTitles ->
      ((anidbEntryTitles['@type'] == 'main') && (anidbEntryTitles['@xml:lang'] == 'en'))
    }
  } else {
    return myQuery
  }
  if ( myQuery == null ) {
    myQuery = xmlNode.children().find { anidbEntryTitles ->
      ((anidbEntryTitles['@type'] == 'main') && (anidbEntryTitles['@xml:lang'] == 'x-zht'))
    }
  } else {
    return myQuery
  }
  if ( myQuery == null ) {
    myQuery = xmlNode.children().find { anidbEntryTitles ->
      ((anidbEntryTitles['@type'] == 'main') && (anidbEntryTitles['@xml:lang'] == 'x-kot'))
    }
  } else {
    return myQuery
  }
  if ( myQuery == null ) {
    myQuery = xmlNode.children().find { anidbEntryTitles ->
      return anidbEntryTitles
    }
  } else {
    return myQuery
  }
  return myQuery
}

/**
 * Return All English/x-* Official/Main Names of an AniDB Entry from the anime-titles.xml
 *
 * @param xmlNode The XML Node representing the AniDB XML Offline Title Entry [See anidbXMLTitleSearch()]
 * @return All English/x-* Official/Main Names of an AniDB Entry
 */
@SuppressWarnings('GrMethodMayBeStatic')
List anidbXMLEntryGetAnimeOMTitles(xmlNode) {
  Logging.log.finest "----  anidbXMLEntryGetAnimeOMNames: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
    (((anidbEntryTitles['@type'] == 'main') || (anidbEntryTitles['@type'] == 'official') )  && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    myAnswer << anidbEntryTitles.text()
  }
  return myAnswer
}

/**
 * Return All Official/Main Names of an AniDB Entry from the anime-titles.xml that are "romantic" languages (mostly)
 *
 * @param xmlNode The XML Node representing the AniDB XML Offline Title Entry [See anidbXMLTitleSearch()]
 * @return All English/x-* Official/Main Names of an AniDB Entry
 */
@SuppressWarnings('GrMethodMayBeStatic')
List anidbXMLEntryGetAnimeTitles(xmlNode) {
  Logging.log.finest "----  anidbXMLEntryGetAnimeOMNames: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
//    (((anidbEntryTitles['@type'] == 'main') || (anidbEntryTitles['@type'] == 'official') )  && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
    (((anidbEntryTitles['@type'] == 'main') || (anidbEntryTitles['@type'] == 'official') ) )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    myRegexMatcher = anidbEntryTitles.text() =~ /^[ -~]*$/
    if ( myRegexMatcher.find() ) {
      myAnswer << anidbEntryTitles.text()
    }
  }
  return myAnswer
}

/**
 * Return All Synonyms Names of an AniDB Entry from the anime-titles.xml that are "romantic" languages (mostly)
 *
 * @param xmlNode The XML Node representing the AniDB XML Offline Title Entry [See anidbXMLTitleSearch()]
 * @return All English/x-* Synonyms Names of an AniDB Entry
 */
List anidbXMLEntryGetAnimeSynonyms(xmlNode) {
  Logging.log.finest "----  anidbXMLEntryGetAnimeSynonyms: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
    // ((anidbEntryTitles['@type'] == 'syn') && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
    ( anidbEntryTitles['@type'] == 'syn' )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    // Unfortunately the language field is not 100% accurate for Synonyms/Shorts
    myRegexMatcher = anidbEntryTitles.text() =~ /^[ -~]*$/
    if ( myRegexMatcher.find() ) {
      myAnswer << anidbEntryTitles.text()
    }
  }
  return myAnswer
}

/**
 * Return All English/x-* Short Names of an AniDB Entry from the anime-titles.xml
 *
 * @param xmlNode The XML Node representing the AniDB XML Offline Title Entry [See anidbXMLTitleSearch()]
 * @return All English/x-* Short Names of an AniDB Entry
 */
@SuppressWarnings('GrMethodMayBeStatic')
List anidbXMLEntryGetAnimeShorts(xmlNode) {
  Logging.log.finest "----  anidbXMLEntryGetAnimeShorts: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
    ((anidbEntryTitles['@type'] == 'short') && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    myAnswer << anidbEntryTitles.text()
  }
  return myAnswer
}

/**
 * Return the AniDB AID Entry from the anime-titles.xml
 *
 * @param xmldoc The XML Parser representation of the AniDB Offline Title xml
 * @param aid The Anime ID to retrieve the XML node entry for
 * @return The XML node entry
 */
@SuppressWarnings('GrMethodMayBeStatic')
def anidbXMLGetAnimeEntry(xmldoc, aid) {
  // We can't use net.filebot.util.XPathUtilities with groovy.xml.XmlParser which is groovy.util.node, while
  // net.filebot.util.XPathUtilities is org.w3c.dom.Node (Which is what Filebot Cached files are)
  return xmldoc.children().find { entry ->
    entry['@aid'] == "${aid}"
  }
}


/**
 * Return theTVDB Name based on the AID entry in Scudd Lee's 'Anime Lists'
 * If there is an entry here, then there is a definitive entry in TheTVDB for the given Anime
 *
 * @param fbCacheName Filebot Cache of Scudd Lee's Anime Lists
 * @param aid The Anime ID to retrieve the XML node entry for
 * @param locale the Locale (aka Locale.English)
 * @return The TVDB Entry Name (if it exists) or null (if it doesn't)
 */
def animeListXMLGetTVDBNAme(fbCacheName, aid, locale) {
  theTVDBSearch = XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // Under normal circumstances as long as theTVDBSearch is not null, the rest should work fine
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  if ( theTVDBID == null ) {
    return null
  }
  myOptionsTVDB = tryQuietly { WebServices.TheTVDB.search(theTVDBID, locale) }
  if ( myOptionsTVDB == null ) {
    return null
  }
  //noinspection GroovyAssignabilityCheck
  return myOptionsTVDB[0].toString()
}

/**
 * Return theTVDB ID based on the AID entry in Scudd Lee's 'Anime Lists'
 * If there is an entry here, then there is a definitive entry in TheTVDB for the given Anime
 *
 * @param fbCacheName Filebot Cache of Scudd Lee's Anime Lists
 * @param aid The Anime ID to retrieve the XML node entry for
 * @param locale the Locale (aka Locale.English)
 * @return The TVDB ID (if it exists) or null (if it doesn't)
 */
def animeListXMLGetTVDBID(fbCacheName, aid, locale) {
  def theTVDBSearch = XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // Under normal circumstances as long as theTVDBSearch is not null, the rest should work fine
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  return theTVDBID
}

/**
 * Return the ANIDB ID based on the IMDB Entry in 'Anime Lists'
 *
 * @param fbCacheName Filebot Cache of Scudd Lee's Anime Lists
 * @param imdbid The IMDB ID to retrieve the XML node entry for
 * @return The AniDB ID (if it exists) or null (if it doesn't)
 */
def animeListXMLGetAniDBFromIMDBID(fbCacheName, def imdbid){
  String theIMDBID = "tt"+StringUtils.leftPad(imdbid.toString(), 7, "0")
//  def aniDBSearch = XPathUtilities.selectNode("anime-list/anime[@imdbid='$theIMDBID']", fbCacheName)
  def aniDBSearch = XPathUtilities.selectNode("anime-list//anime[contains(@imdbid, '${theIMDBID}')]", fbCacheName)
  Logging.log.finest "aniDBSearch:${aniDBSearch}"
  if (aniDBSearch == null) {
    return null
  }
  return tryQuietly { XPathUtilities.selectString('@anidbid', aniDBSearch).toInteger() }
}

/**
 * Return the ANIDB ID based on the TMDB Entry in 'Anime Lists'
 *
 * @param fbCacheName Filebot Cache of Scudd Lee's Anime Lists
 * @param tmdbid The IMDB ID to retrieve the XML node entry for
 * @return The AniDB ID (if it exists) or null (if it doesn't)
 */
def animeListXMLGetAniDBFromTMDBID(fbCacheName, Integer tmdbid){
//  def aniDBSearch = XPathUtilities.selectNode("anime-list/anime[@tmdbid='$tmdbid']", fbCacheName)
  def aniDBSearch = XPathUtilities.selectNode("anime-list//anime[contains(@tmdbid, '${tmdbid}')]", fbCacheName)
  Logging.log.finest "aniDBSearch:${aniDBSearch}"
  if (aniDBSearch == null) {
    return null
  }
  return tryQuietly { XPathUtilities.selectString('@anidbid', aniDBSearch).toInteger() }
}

/**
 * Return theTVDB Season based on the AID entry in Scudd Lee's 'Anime Lists'
 * Mapping to a season is less reliable, as the data relies on people manually mapping an AniDB
 * Series to an TVDB Season
 *
 * @param fbCacheName Filebot Cache of Scudd Lee's Anime Lists
 * @param aid The Anime ID to retrieve the XML node entry for
 * @param locale the Locale (aka Locale.English)
 * @return The TVDB Season (if it exists) or null (if it doesn't)
 */
def animeListXMLGetTVDBSeason(fbCacheName, aid, locale) {
  theTVDBSearch = XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  if ( theTVDBID == null ) {
    return null
  }
  // See if we an find a season ..
  theTVDBSeason = tryQuietly { XPathUtilities.selectString("@defaulttvdbseason", theTVDBSearch).toInteger() }
  if ( theTVDBSeason == null ) {
    return null
  }
  return theTVDBSeason
}

// --- Mandatory parameters must be defined prior to parameters with defaults.
// http://docs.groovy-lang.org/docs/groovy-2.5.0-beta-1/html/documentation/#_default_arguments
/**
 * Download the AniDB Offline Title XML file
 *
 * @param userAgent AniDB requires you include a userAgent header to download it ('username/filebot' for example)
 * @param xmlFileName The filename to download the XML to, defaults to 'anime-titles.xml'
 * @param refreshDays The number of day's to wait until downloading a new one, defaults to 7 days
 * @vaoid
 */
void aniDBXMLDownload(String userAgent, String xmlFileName = 'anime-titles.xml', Integer refreshDays = 7) {
  // ---------- Variable Declaration ---------- //
  String gzipFileName = 'anime-titles.xml.gz'
  // I couldn't figure out how to get http://anidb.net/api/anime-titles.xml.gz to work with URL method due to the redirect.
  URL redirectedURL = findRealUrl(new URL('http://anidb.net/api/' + gzipFileName))
  File gzipFileNameFILE = new File(gzipFileName)
  Path source = Paths.get(gzipFileName)
  Path target = Paths.get(xmlFileName)
  // https://stackoverflow.com/questions/38917989/ant-groovy-how-to-delete-files-over-than-given-date-but-keep-a-maximum-of-3
  BigInteger oneWeekInMillis = refreshDays * 24 * 60 * 60 * 1000 // 24 hours x 60 minutes x 60 seconds x 1000 = 1 day
  BigInteger oneWeekAgo = System.currentTimeMillis() - oneWeekInMillis

  // ---------- Download/Decompress as needed ---------- //
  // Check if the files exists
  if ( gzipFileNameFILE.exists() ) {
    // Check if it's older then refreshDays days
    if ( gzipFileNameFILE.lastModified() < oneWeekAgo ) {
      // It is, so Download it
      HttpBuilder.configure {
        request.uri = redirectedURL
        request.headers['User-Agent'] = userAgent
      }.get {
        Download.toFile(delegate, new File(gzipFileName))
        response.failure { FromServer resp, Object body ->
          Logging.log.severe "Download FAILED, HTTP - ${resp.properties}"
          Logging.log.severe "${body}"
        }
      }
      // And decompress it
      decompressGzip(source, target)
    }
  } else {
    // Download the file since it doesn't exist
    HttpBuilder.configure {
        request.uri = redirectedURL
        request.headers['User-Agent'] = userAgent
    }.get {
        Download.toFile(delegate, new File(gzipFileName))
        response.failure { FromServer resp, Object body ->
          Logging.log.severe "Download FAILED, HTTP - ${resp.properties}"
          Logging.log.severe "${body}"
        }
    }
    // Decompress it
    decompressGzip(source, target)
  }
}

// --- Mandatory parameters must be defined prior to parameters with defaults.
// http://docs.groovy-lang.org/docs/groovy-2.5.0-beta-1/html/documentation/#_default_arguments
/**
 * Download the AniDB Synonyms XML file
 *
 * @param xmlFileName The filename to download the XML to, defaults to 'anime-synonyms.xml'
 * @param refreshDays The number of day's to wait until downloading a new one, defaults to 7 days
 * @vaoid
 */
void aniDBSynonymDownload(String xmlFileName = 'anime-synonyms.xml', Integer refreshDays = 7) {
  // ---------- Variable Declaration ---------- //
  File xmlFileNameFILE = new File(xmlFileName)
  URL synonymURL = new URL('https://raw.githubusercontent.com/runecalico/filebot-public/main/datafiles/' + xmlFileName)
  // https://stackoverflow.com/questions/38917989/ant-groovy-how-to-delete-files-over-than-given-date-but-keep-a-maximum-of-3
  // https://raw.githubusercontent.com/ScudLee/anime-lists/master/
  BigInteger oneWeekInMillis = refreshDays * 24 * 60 * 60 * 1000 // 24 hours x 60 minutes x 60 seconds x 1000 = 1 day
  BigInteger oneWeekAgo = System.currentTimeMillis() - oneWeekInMillis

  // ---------- Download/Decompress as needed ---------- //
  // Check if the files exists
  if ( xmlFileNameFILE.exists() ) {
    // Check if it's older then refreshDays days
    if ( xmlFileNameFILE.lastModified() < oneWeekAgo ) {
      // It is, so Download it
      HttpBuilder.configure {
        request.uri = synonymURL
      }.get {
        Download.toFile(delegate, new File(xmlFileName))
        response.failure { FromServer resp, Object body ->
          Logging.log.severe "Download FAILED, HTTP - ${resp.properties}"
          Logging.log.severe "${body}"
        }
      }
    }
  } else {
    // Download the file since it doesn't exist
    HttpBuilder.configure {
        request.uri = synonymURL
    }.get {
        Download.toFile(delegate, new File(xmlFileName))
        response.failure { FromServer resp, Object body ->
          Logging.log.severe "Download FAILED, HTTP - ${resp.properties}"
          Logging.log.severe "${body}"
        }
    }
  }
}

/**
 * Have Filebot search AniDB and return Return a HashSet with the unique responses (removing duplicates)
 *
 * @param searchList What we want filebot to search AniDB for
 * @param locale the locale (locale.English for example)
 * @return the search results as a set for all the search terms
 */
Set filebotAniDBSearch(Set searchList, locale) {
  HashSet resultsAsSet = []
  HashSet myAniDBSearch = []
  searchList.each { item ->
        myAniDBSearch = WebServices.AniDB.search(item as String, locale) as HashSet
        if ( myAniDBSearch.isEmpty() ) {
        } else {
          resultsAsSet << myAniDBSearch
        }
  }
  return resultsAsSet
}

/**
 * Search AniDB XML titlesearch for a String using JWD to match
 * Allow for returning just AID (3rd param) or All Official/Main titles (en/x-*) (4th param)
 *
 * @param aniDBTitleXML XML Parser doc for AniDB Offline Title XML file
 * @param searchList A Set of names we will be searching for
 * @param returnAID Boolean: Return just the AID, defaults to false
 * @param returnAllOM Boolean: Return all Official/Main titles, defaults to false
 * @param literalMatch Boolean: Perform a literal match instead of jaroWinklerDistance, defaults to false
 * @param jwdStrictness How strict will we be on what we consider a match?, defaults to 3.
 * 1 is JWD match of 1.0000000000000, 2 is JWD match of 0.9900000000000+, 3 is JWD match of 0.9800000000000+
 * @return the search results as a set for all the search terms
 */
Set anidbXMLTitleSearch(aniDBTitleXML, Set searchList, locale, Boolean returnAID = false, Boolean returnAllOM = false, Boolean literalMatch = false, Integer jwdStrictness = 3) {
  BigDecimal jwdCutoff = 0
  BigDecimal jwdcompare = 0
  switch (jwdStrictness) {
    case 1:
      jwdCutoff = 1.0000000000000
      break
    case 2:
      jwdCutoff = 0.9900000000000
      break
    case 3:
      jwdCutoff = 0.9800000000000
      break
    default:
      jwdCutoff = 0.9800000000000
      break
  }
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  anidbID = 0
  aniDBTitleXML.children().each { anidbAnimeEntry ->
    if ( returnAID ) {
      anidbID = anidbAnimeEntry['@aid'].toInteger()
    } else {
      Logging.log.finest "ReturnAllOM: ${returnAllOM}"
      returnAllOM == false ? (officialTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntry)) : (officialTitle = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntry))
      Logging.log.finest "officialTitle: ${officialTitle}"
    }
    // println "Parsing AniDB ID: ${anidbID}"
    searchList.each { searchItem ->
      searchItemString = searchItem.toString()
      anidbAnimeEntry.each { title ->
        titleText = title.text()
        myRegexMatcher = title.text() =~ /^[ -~]*$/
        if ( myRegexMatcher.find() ) {
          Logging.log.finest "Processing Title: ${title.text()}"
        } else {
          return
        }
        if ( literalMatch ) {
          // If we are looking up something from a Filebot Search, it *should* be able to exact match.
          // This will deal with the issues involved in anime where a period/exclamation point etc are the ONLY differences between series
          // Removing all that stuff for the JWD Compare helps when dealing with filenames (which often have those bits missing/wrong), but
          // DOES NOT HELP when trying to do a title search, which might actually be correct.
          // if ( searchItem.toString() == title.text() ) {
          //   returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          // }
          if ( searchItemString == titleText ) {
            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          }
        } else {
          // JWD Comparison
          // jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem.toString()), altjwdStringBlender(title.text()))
          jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItemString), altjwdStringBlender(titleText))
          if ( jwdcompare >= jwdCutoff ) {
            // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${searchItem.toString()}")
            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          }
//          if ( jwdcompare == 1 ) {
//            // println "Found this exact Match (${jwdcompare})? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${searchItem.toString()}"
//            // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${searchItem.toString()}")
//            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
//          }
//          if ( strictJWDMatch == false ) {
//            if ( jwdcompare > 0.990000000 ){
//              // println "Found this Likely Match (0.99+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
//              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
//              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
//            } else if ( jwdcompare > 0.980000000 ){
//              // println "Found this Possible Match (0.98+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
//              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
//              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
//            }
//          }
        }
      }
    }
  }
  return resultsAsSet
}

/**
 * Search filebot Cached Anime List for AID entry
 * returns null if there is no entry or net.filebot.web.AnimeLists$Entry (DEFAULT)
 * returns n instead of null if there is no TVDB Season else returns defaulttvdbseason entry if tvdbSeasonOnly = true
 * returns TVDB ID if tvdbIDOnly = true
 *
 * @param aniDBID The Anime ID we want to find an entry for.
 * @param tvdbIDOnly Boolean: Return just the TVDB ID, defaults to false
 * @param tvdbSeasonOnly Boolean: Return just the TVDB Season Entry, defaults to false
 * @return depending on options returns null, net.filebot.web.AnimeLists$Entry, Integer or String
 */
def filebotAnimeListReturnFromAID(Integer aniDBID, Boolean tvdbIDOnly = false, Boolean tvdbSeasonOnly = false) {
  def mySearchResult = tryQuietly {
    WebServices.AnimeList.model.anime.find{
      it.anidbid == aniDBID
    }
  }
  if ( mySearchResult == null ) {
    return null
  }
  if (tvdbIDOnly) {
    return mySearchResult.tvdbid
  }
  if (tvdbSeasonOnly) {
    if ( mySearchResult.defaulttvdbseason == null ) {
      return "n"
    } else {
      return mySearchResult.defaulttvdbseason
    }
  }
  return mySearchResult
}

/**
 * Get the Episode List for an AID from Filebot
 * returns [] if the AID can not be found by Filebot
 * returns All episodes with episode = true if whatAboutSpecials is exclude (the default)
 * returns all episodes with special = true if whatAboutSpecials is only
 * returns all episodes if whatAboutSpecials = include
 *
 * @param aniDBSeriesID The Anime ID we want the episode list for
 * @param whatAboutSpecials What about including specials? defaults to exclude, possible values are exclude, include, only
 * @return Collection<Episode> of matching Episodes
 */
Collection<Episode> filebotAniDBgetEpisodeList(Integer aniDBSeriesID, String whatAboutSpecials = 'exclude') {
  SeriesInfo myAniDBseriesInfo
  try {
    myAniDBseriesInfo = WebServices.AniDB.getSeriesInfo(aniDBSeriesID, Locale.ENGLISH)
  } catch (e) {
//    Collection<Episode> myAniDBseriesInfo
//    return myAniDBseriesInfo
    return []
  }
  switch (whatAboutSpecials) {
    case 'exclude':
      return WebServices.AniDB.getEpisodeList(myAniDBseriesInfo.id, myAniDBseriesInfo.order as SortOrder, Locale.ENGLISH).findAll { it.episode }
      break
    case 'include':
      return WebServices.AniDB.getEpisodeList(myAniDBseriesInfo.id, myAniDBseriesInfo.order as SortOrder, Locale.ENGLISH)
      break
    case 'only':
      return WebServices.AniDB.getEpisodeList(myAniDBseriesInfo.id, myAniDBseriesInfo.order as SortOrder, Locale.ENGLISH).findAll { it.special }
      break
    default:
      return WebServices.AniDB.getEpisodeList(myAniDBseriesInfo.id, myAniDBseriesInfo.order as SortOrder, Locale.ENGLISH).findAll { it.episode }
      break
  }
}

/**
 * Determine if an Episode is in an Collection of Episodes.
 * returns true/false if returnEpisode = false (the default)
 * returns <Episode> if returnEpisode = true
 *
 * @param myAniDBEpisodes A Collection<Episode> we want to search [See filebotAniDBgetEpisodeList()]
 * @param episodeNumber The Episode # we are looking for
 * @param order The Episode Order we will use, defaults to Absolute
 * @param returnEpisode Boolean: Are we returning the <Episode>? defaults to false
 * @return <Episode> of matching Episode or Boolean
 */
def filebotAniDBSeasonContainsEpisodeNumber(Collection<Episode> myAniDBEpisodes, Integer episodeNumber, String order = 'Absolute', Boolean returnEpisode = false) {
  if ( returnEpisode ) {
    return myAniDBEpisodes.find { it."${order}" == episodeNumber }
  }
  return myAniDBEpisodes.find { it."${order}" == episodeNumber } != null
}

/**
 * Count the # of Episodes
 *
 * @param myAniDBEpisodes A Collection<Episode> we want to search [See filebotAniDBgetEpisodeList()]
 * @return # of Episodes
 */
Integer filebotAniDBEpisodeCount(Collection<Episode> myAniDBEpisodes) {
  return myAniDBEpisodes.size()
}

/**
 * For an AID, return the # of Episodes in that AID.
 * First we query Anime Offline Database, if it doesn't exist in AOD OR is not FINISHED we then query Filebot
 * The Theory is that filebot should have "up-to-date" Episode information over AOD.
 * We do not query filebot by default so as to cut down on queries made to AniDB for Episode information
 *
 * @param fileBotJsonCacheObject JsonObject of Anime Offline Database
 * @param animeID The AniDB ID we are interested in
 * @return # of Episodes
 */
Integer aniDBGetEpisodeNumberForAID(JsonObject fileBotJsonCacheObject, Integer animeID) {
  JsonObject myAniDBEntry = fileBotJsonCacheObject.data.find { aodentry ->
    aodentry.sources.find { it ==~ /https:\/\/anidb\.net\/anime\/${animeID}$/ }
  } as JsonObject
  // --- It will be null if an AID is not in the AOD List --- //
  if ( myAniDBEntry == null ) {
    Collection<Episode> myAniDBEpisodeList = filebotAniDBgetEpisodeList(animeID)
    return filebotAniDBEpisodeCount(myAniDBEpisodeList)
  } else {
    if ( myAniDBEntry.episodes == 0 || myAniDBEntry.status != "FINISHED" ) {
      Collection<Episode> myAniDBEpisodeList = filebotAniDBgetEpisodeList(animeID)
      return filebotAniDBEpisodeCount(myAniDBEpisodeList)
    } else {
      return myAniDBEntry.episodes as Integer
    }
  }
}

/**
 * Search compiled AniDB Title/Synonym LinkedHashMap for matching titles.
 * The compiled LinkedHashMap was created using loadAniDBOfflineXML()
 * This method is a *little* faster then anidbXMLTitleSearch() and it is a single
 * invocation for all Titles/Synonyms vs two invocations.
 *
 * @param aniDBCompleteXMLList Compiled AniDB Title/Synonyms/Shorts LinkedHashMap [See loadAniDBOfflineXML()]
 * @param searchList A Set of names we will be searching for
 * @param returnAID Boolean: Return just the AID, defaults to false
 * @param returnAllOM Boolean: Return all Official/Main titles, defaults to false
 * @param literalMatch Boolean: Perform a literal match instead of jaroWinklerDistance, defaults to false
 * @param jwdStrictness How strict will we be on what we consider a match?, defaults to 3.
 * 1 is JWD match of 1.0000000000000, 2 is JWD match of 0.9900000000000+, 3 is JWD match of 0.9800000000000+
 * @return the search results as a set for all the search terms
 */
Set anidbHashTitleSearch(LinkedHashMap aniDBCompleteXMLList, Set searchList, locale, Boolean returnAID = false, Boolean returnAllOM = false, Boolean literalMatch = false, Integer jwdStrictness = 3) {
  BigDecimal jwdCutoff = 0
  BigDecimal jwdcompare = 0
  switch (jwdStrictness) {
    case 1:
      jwdCutoff = 1.0000000000000
      break
    case 2:
      jwdCutoff = 0.9900000000000
      break
    case 3:
      jwdCutoff = 0.9800000000000
      break
    default:
      jwdCutoff = 0.9800000000000
      break
  }
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  HashSet titlesToSearch
  anidbID = 0
  aniDBCompleteXMLList.each { aid, values ->
    if ( returnAID ) {
      anidbID = aid
    } else {
      returnAllOM == false ? (officialTitle = values.primaryTitle ) : (officialTitle = values.titles)
    }
    searchList.each { searchItem ->
      searchItemString = searchItem.toString()
      searchItemStringCompare = altjwdStringBlender(searchItemString)
      if ( values.titlescompare != null ) {
        titlesToSearch = values.titlescompare
      }
      if ( values.synonymscompare != null ) {
        titlesToSearch.addAll(values.synonymscompare)
      }
      if ( values.shortscompare != null ) {
        titlesToSearch.addAll(values.shortscompare)
      }
      titlesToSearch.each { titleText ->
//        titleText = title
//        myRegexMatcher = title =~ /^[ -~]*$/
//        if ( myRegexMatcher.find() ) {
//          // println "Processing Title: ${title.text()}"
//        } else {
//          return
//        }
        if ( literalMatch ) {
          // If we are looking up something from a Filebot Search, it *should* be able to exact match.
          // This will deal with the issues involved in anime where a period/exclamation point etc are the ONLY differences between series
          // Removing all that stuff for the JWD Compare helps when dealing with filenames (which often have those bits missing/wrong), but
          // DOES NOT HELP when trying to do a title search, which might actually be correct.
          // if ( searchItem.toString() == title.text() ) {
          //   returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          // }
          if ( searchItemStringCompare == titleText ) {
            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle}") : (resultsAsSet <<  officialTitle)
          }
        } else {
          // JWD Comparison
          // jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem.toString()), altjwdStringBlender(title.text()))
          jwdcompare = jaroWinklerDistance.apply(searchItemStringCompare, titleText as CharSequence)
          if ( jwdcompare >= jwdCutoff ) {
            // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${searchItem.toString()}")
            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle}") : (resultsAsSet <<  officialTitle)
          }
        }
      }
    }
  }
  return resultsAsSet
}

/**
 * Create a LinkedHashMap for easier searching then XML Parser implementation for AniDB Offline Title XML and AniDB Synonym XML
 * The compiled linkedHashMap merges all entries from both XML files, as well as creates a "compare" version of the
 * Titles, Shorts and Synonyms that can be used when doing jaroWinklerDistance compares with altjwdStringBlender() already
 * applied to the values so that regex cost is only done once (by this method) and not on every search.
 *
 * @param aniDBTitleXMLFilename The filename for AniDB Title Offline XML
 * @param aniDBSynonymXMLFilename The filename for AniDB Synonym XML
 * @return Compiled/Merged entries from both AniDB Title/Synonym XML files
 */
@SuppressWarnings('GrReassignedInClosureLocalVar')
LinkedHashMap loadAniDBOfflineXML(String aniDBTitleXMLFilename, String aniDBSynonymXMLFilename) {
  LinkedHashMap aniDBTitleEntries = [:]
  LinkedHashMap aniDBSynonymEntries = [:]
  HashSet anidbAnimeEntryShorts = []
  HashSet anidbAnimeEntryShortsCompare = []
  HashSet anidbAnimeEntryTitles = []
  HashSet anidbAnimeEntryTitlesCompare = []
  HashSet anidbAnimeEntrySynonyms = []
  LinkedHashMap anidbAnimeEntrySynonyms2 = []
  HashSet anidbAnimeEntrySynonymsCompare = []
  def aniDBSynonymXML = new groovy.xml.XmlParser(false, false).parse(aniDBSynonymXMLFilename)
  aniDBSynonymXML.children().each { anidbAnimeEntry ->
    anidbID = anidbAnimeEntry['@aid'].toInteger()
    anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntry)
    anidbAnimeEntrySynonymsCompare = []
    anidbAnimeEntrySynonyms.each { animeSynonyms ->
      anidbAnimeEntrySynonymsCompare += ["${altjwdStringBlender(animeSynonyms)}"]
    }
    aniDBSynonymEntries += [(anidbID): [synonyms: anidbAnimeEntrySynonyms, synonymscompare: anidbAnimeEntrySynonymsCompare]]
  }
  def aniDBTitleXML = new groovy.xml.XmlParser(false, false).parse(aniDBTitleXMLFilename)
  aniDBTitleXML.children().each {anidbAnimeEntry ->
    anidbID = anidbAnimeEntry['@aid'].toInteger()
    Logging.log.finest "anidbID:[${anidbID}]"
    def anidbAnimeEntryPrimaryTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntry).text()
//    anidbAnimeEntryTitles = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntry)
    anidbAnimeEntryTitles = anidbXMLEntryGetAnimeTitles(anidbAnimeEntry)
    anidbAnimeEntryTitlesCompare = []
    anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntry)
    anidbAnimeEntrySynonyms2 = []
    anidbAnimeEntrySynonyms2 = aniDBSynonymEntries[anidbID]
    if ( anidbAnimeEntrySynonyms2 != null ) {
      anidbAnimeEntrySynonyms += anidbAnimeEntrySynonyms2.synonyms
    }
    anidbAnimeEntrySynonymsCompare = []
    anidbAnimeEntryShorts = anidbXMLEntryGetAnimeShorts(anidbAnimeEntry)
    anidbAnimeEntryShortsCompare = []
    anidbAnimeEntryShorts.each { animeShorts ->
      anidbAnimeEntryShortsCompare += ["${altjwdStringBlender(animeShorts)}"]
    }
    anidbAnimeEntrySynonyms.each { animeSynonyms ->
      anidbAnimeEntrySynonymsCompare += ["${altjwdStringBlender(animeSynonyms)}"]
    }
    anidbAnimeEntryTitles.each { animeTitles ->
      anidbAnimeEntryTitlesCompare += ["${altjwdStringBlender(animeTitles)}"]
    }
    if (anidbAnimeEntryPrimaryTitle != [] ) {
      aniDBTitleEntries += [(anidbID): [primaryTitle: anidbAnimeEntryPrimaryTitle, titles: anidbAnimeEntryTitles, titlescompare: anidbAnimeEntryTitlesCompare, synonyms: anidbAnimeEntrySynonyms, synonymscompare: anidbAnimeEntrySynonymsCompare, shorts: anidbAnimeEntryShorts, shortscompare:  anidbAnimeEntryShortsCompare]]
    }
  }
  return aniDBTitleEntries
}
