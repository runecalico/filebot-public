package lib

import net.filebot.WebServices

//--- VERSION 1.1.0
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

// ---------- Return the "Primary Title" of an AniDB Entry from the anime-titles.xml ---------- //
def anidbXMLEntryGetAnimePrimaryTitle(xmlNode) {
  // log.finest "----  anidbXMLEntryGetAnimePrimaryTitle: xmlNode: ${xmlNode}"
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
}

// ---------- Return All English/x-* Official/Main Names of an AniDB Entry from the anime-titles.xml ---------- //
List anidbXMLEntryGetAnimeOMTitles(xmlNode) {
  // log.finest "----  anidbXMLEntryGetAnimeOMNames: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
    (((anidbEntryTitles['@type'] == 'main') || (anidbEntryTitles['@type'] == 'official') )  && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    myAnswer << anidbEntryTitles.text()
  }
  return myAnswer
}

// ---------- Return All English/x-* Synonyms Names of an AniDB Entry from the anime-titles.xml ---------- //
List anidbXMLEntryGetAnimeSynonyms(xmlNode) {
  // log.finest "----  anidbXMLEntryGetAnimeSynonyms: xmlNode: ${xmlNode}"
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

// ---------- Return All English/x-* Short Names of an AniDB Entry from the anime-titles.xml ---------- //
List anidbXMLEntryGetAnimeShorts(xmlNode) {
  // log.finest "----  anidbXMLEntryGetAnimeShorts: xmlNode: ${xmlNode}"
  def myQuery = xmlNode.children().findAll { anidbEntryTitles ->
    ((anidbEntryTitles['@type'] == 'short') && ((anidbEntryTitles['@xml:lang'] == 'en') || (anidbEntryTitles['@xml:lang'] =~ /^x-/) ) )
  }
  List myAnswer = []
  myQuery.each { anidbEntryTitles ->
    myAnswer << anidbEntryTitles.text()
  }
  return myAnswer
}


// ---------- Return the AniDB AID Entry from the anime-titles.xml ---------- //
def anidbXMLGetAnimeEntry(xmldoc, aid) {
  return xmldoc.children().find { entry ->
    entry['@aid'] == "${aid}"
  }
}

// ---------- Return theTVDB Name based on the AID entry in Scudd Lee's 'Anime Lists' ---------- //
// --- If there is an entry here, then there is a difinitive entry in TheTVDB for the given Anime
def animeListXMLGetTVDBNAme(fbCacheName, aid, locale) {
  theTVDBSearch = net.filebot.util.XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // Under normal circumstances as long as theTVDBSearch is not null, the rest should work fine
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { net.filebot.util.XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  if ( theTVDBID == null ) {
    return null
  }
  myOptionsTVDB = tryQuietly { TheTVDB.search(theTVDBID, locale) }
  if ( myOptionsTVDB == null ) {
    return null
  }
  return myOptionsTVDB[0].toString()
}

// ---------- Return theTVDB ID based on the AID entry in Scudd Lee's 'Anime Lists' ---------- //
// --- If there is an entry here, then there is a difinitive entry in TheTVDB for the given Anime
def animeListXMLGetTVDBID(fbCacheName, aid, locale) {
  theTVDBSearch = net.filebot.util.XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // Under normal circumstances as long as theTVDBSearch is not null, the rest should work fine
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { net.filebot.util.XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  return theTVDBID
}

// ---------- Return theTVDB Season based on the AID entry in Scudd Lee's 'Anime Lists' ---------- //
// --- Mapping to a season is less reliable, as the data relies on people manually mapping an AniDB
// --- Series to an TVDB Season
def animeListXMLGetTVDBSeason(fbCacheName, aid, locale) {
  theTVDBSearch = net.filebot.util.XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
  if ( theTVDBSearch == null ) {
    return null
  }
  // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
  theTVDBID = tryQuietly { net.filebot.util.XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
  if ( theTVDBID == null ) {
    return null
  }
  // See if we an find a season ..
  theTVDBSeason = tryQuietly { net.filebot.util.XPathUtilities.selectString("@defaulttvdbseason", theTVDBSearch).toInteger() }
  if ( theTVDBSeason == null ) {
    return null
  }
  return theTVDBSeason
}

// ---------- Return AniDB SID based on the TVDB ID, Season and Episode # ---------- //
// --- Mapping to a season is less reliable, as the data relies on people manually mapping an AniDB
// --- Series to an TVDB Season
// def animeListXMLGetAniDBID(fbCacheName, tvdb, season, locale) {
//   theTVDBSearch = net.filebot.util.XPathUtilities.selectNode("anime-list/anime[@anidbid='$aid']", fbCacheName)
//   if ( theTVDBSearch == null ) {
//     return null
//   }
//   // We convert to Integer, which should only work if it's a TV Series (movies etc return text)
//   theTVDBID = tryQuietly { net.filebot.util.XPathUtilities.selectString('@tvdbid', theTVDBSearch).toInteger() }
//   if ( theTVDBID == null ) {
//     return null
//   }
//   // See if we an find a season ..
//   theTVDBSeason = tryQuietly { net.filebot.util.XPathUtilities.selectString("@defaulttvdbseason", theTVDBSearch) }
//   if ( theTVDBSeason == null ) {
//     return null
//   }
//   return theTVDBSeason
// }

// --- Mandatory parameters must be defined prior to parameters with defaults.
// http://docs.groovy-lang.org/docs/groovy-2.5.0-beta-1/html/documentation/#_default_arguments
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
          log.error "Download FAILED, HTTP - ${resp.properties}"
          log.error "${body}"
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
          log.error "Download FAILED, HTTP - ${resp.properties}"
          log.error "${body}"
        }
    }
    // Decompress it
    decompressGzip(source, target)
  }
}

// --- Mandatory parameters must be defined prior to parameters with defaults.
// http://docs.groovy-lang.org/docs/groovy-2.5.0-beta-1/html/documentation/#_default_arguments
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
          log.error "Download FAILED, HTTP - ${resp.properties}"
          log.error "${body}"
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
          log.error "Download FAILED, HTTP - ${resp.properties}"
          log.error "${body}"
        }
    }
  }
}

// ---------- Have Filebot search AniDB  ---------- //
// --- Return a HashSet with the unique responses
Set filebotAniDBSearch(Set searchList, locale) {
  resultsAsSet = [] as HashSet
  searchList.each { item ->
        myAniDBSearch = AniDB.search(item, locale)
        if (myAniDBSearch.isEmpty()) {
        } else {
          resultsAsSet << myAniDBSearch
        }
  }
  return resultsAsSet
}

// ---------- Search AniDB XML Titlesearch for a String using JWD to match  ---------- //
// ---  Allow for returning just AID (3rd param) or All Official/Main titles (en/x-*) (4th param)
Set anidbXMLTitleSearch(aniDBTitleXML, Set searchList, locale, Boolean returnAID = false, Boolean returnAllOM = false, Boolean literalMatch = false, Boolean strictJWDMatch = false) {
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  anidbID = 0
  aniDBTitleXML.children().each { anidbAnimeEntry ->
    if ( returnAID ) {
      anidbID = anidbAnimeEntry['@aid'].toInteger()
    } else {
      // println "ReturnAllOM: ${returnAllOM}"
      returnAllOM == false ? (officialTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntry)) : (officialTitle = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntry))
      // println "officialTitle: ${officialTitle}"
    }
    // println "Parsing AniDB ID: ${anidbID}"
    searchList.each { searchItem ->
      searchItemString = searchItem.toString()
      anidbAnimeEntry.each { title ->
        titleText = title.text()
        myRegexMatcher = title.text() =~ /^[ -~]*$/
        if ( myRegexMatcher.find() ) {
          // println "Processing Title: ${title.text()}"
        } else {
          return
        }
        if ( literalMatch ) {
          // If we are looking up something from a Filebot Search, it *should* be able to exact match.
          // This will deal with the issues involved in anime where a period/exlamationpoint etc are the ONLY differences between series
          // Removing all that stuff for the JWD Compare helps when dealing with filenames (which often have those bits missing/wrong), but
          // DOES NOT HELP when trying to do a title search, which might actually be correct in those punct.
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
          if ( jwdcompare == 1 ) {
            // println "Found this exact Match (${jwdcompare})? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${searchItem.toString()}"
            // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${searchItem.toString()}")
            returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          }
          if ( strictJWDMatch == false ) {
            if ( jwdcompare > 0.990000000 ){
              // println "Found this Likely Match (0.99+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
            } else if ( jwdcompare > 0.980000000 ){
              // println "Found this Possible Match (0.98+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
            }
          }
        }
      }
    }
  }
  return resultsAsSet
}

// ---------- Search AniDB XML Titlesearch for a String using JWD to match  ---------- //
// ---  Allow for returning just AID (3rd param) or All Official/Main titles (en/x-*) (4th param)
Set anidbXMLTitleSearch2(aniDBTitleXML, Set searchList, locale, Boolean returnAID = false, Boolean returnAllOM = false, Boolean literalMatch = false, Boolean strictJWDMatch = false) {
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  anidbID = 0
  aniDBTitleXML.children().each { anidbAnimeEntry ->
    if ( returnAID ) {
      anidbID = anidbAnimeEntry['@aid'].toInteger()
    } else {
      // println "ReturnAllOM: ${returnAllOM}"
      returnAllOM == false ? (officialTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntry)) : (officialTitle = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntry))
      // println "officialTitle: ${officialTitle}"
    }
    // println "Parsing AniDB ID: ${anidbID}"
    searchList.each { searchItem ->
      searchItemString = searchItem.toString()
      anidbAnimeEntry.each { title ->
        titleText = title.text()
        myRegexMatcher = titleText =~ /^[ -~]*$/
        if ( myRegexMatcher.find() ) {
          // println "Processing Title: ${title.text()}"
        } else {
          return
        }
        if ( literalMatch ) {
          // If we are looking up something from a Filebot Search, it *should* be able to exact match.
          // This will deal with the issues involved in anime where a period/exlamationpoint etc are the ONLY differences between series
          // Removing all that stuff for the JWD Compare helps when dealing with filenames (which often have those bits missing/wrong), but
          // DOES NOT HELP when trying to do a title search, which might actually be correct in those punct.
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
          if ( jwdcompare == 1 ) {
            // println "Found this exact Match (${jwdcompare})? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${searchItem.toString()}"
            // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${searchItem.toString()}")
            returnAID == true ? (resultsAsSet <<anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
          }
          if ( strictJWDMatch == false ) {
            if ( jwdcompare > 0.990000000 ){
              // println "Found this Likely Match (0.99+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
            } else if ( jwdcompare > 0.980000000 ){
              // println "Found this Possible Match (0.98+)? ${title.text()} for AID: ${anidbID} with OfficialTitle: ${officialTitle.text()}"
              // returnAID == true ? (resultsAsSet << anidbID) : (resultsAsSet << "${officialTitle.text()}")
              returnAID == true ? (resultsAsSet << anidbID) : returnAllOM == false ? (resultsAsSet << "${officialTitle.text()}") : (resultsAsSet <<  officialTitle)
            }
          }
        }
      }
    }
  }
  return resultsAsSet
}

Set aodReturnAllTitlesAndSynonyms(animeOfflinejsonObject, Set searchList) {
  HashSet returnAsSet = []
  searchList.each { lead ->
    // println "lead:${lead}"
    returnAsSet << lead
    animeOfflinejsonObject.data.find { it.title == lead }.synonyms.collect { it }.each {
    returnAsSet << it.toLowerCase()
    }
  }
  return returnAsSet
}

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
    return mySearchResult.defaulttvdbseason
  }
  return mySearchResult
}