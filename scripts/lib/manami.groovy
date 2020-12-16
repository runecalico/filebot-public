package lib
//--- VERSION 1.0.0
// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
@Grapes(
    @Grab(group='org.apache.commons', module='commons-text', version='1.9')
)
import org.apache.commons.text.similarity.JaroWinklerDistance
import com.cedarsoftware.util.io.JsonObject
import java.util.regex.Matcher

// ---------- Search Anime Offline Database for an AniDB title match using JWD ---------- //
// --- This *should* as such only return unique results
// --- Synonyms in Anime Offline Database are NOT to be trusted to be accurate for the Title Entry
// --- Even for AniDB entries. It is useful to get some possible variations on the Title Name, but
// --- Not for an authoritive match to AID.
Set aodJWDSearchOnlyAniDB(JsonObject fileBotJsonCacheObject, Set searchList, locale, Boolean includeSynonyms = false, Boolean returnAID = false, Boolean strictJWDMatch = false) {
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  fileBotJsonCacheObject.data.each { aodentry ->
    if (aodentry.sources =~ /anidb/) {
      Matcher matcher = aodentry.sources =~ /https:\/\/anidb\.net\/anime\/(\d+)/
      anidbID = matcher[0][1].toInteger()
      if ( includeSynonyms ) {
        aodtitles = []
        aodtitles += aodentry.title
        aodtitles += aodentry.synonyms.flatten()
      }
      searchList.each { searchItem ->
        if ( includeSynonyms ) {
          aodtitles.each { title ->
            jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem), altjwdStringBlender(title))
            // At some point I may add a "fuzzy" vs "strict" matcher, aka 1 vs > .98
            if ( jwdcompare == 1 ) {
              // println "Found this exact Match (1.0) in title entry? ${aodentry.title}"
              returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
              // resultsAsSet += aodentry.title
              } else if ( strictJWDMatch == false ) {
              if ( jwdcompare > 0.990000000 ) {
                // println "Found this Likely Match (0.99+)? ${aodentry.title}"
                returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
              // resultsAsSet += aodentry.title
              } else if ( jwdcompare > 0.980000000 ) {
                // println "Found this Possible Match (0.98+)? ${aodentry.title} "
                returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
              // resultsAsSet += aodentry.title
              }
            }
          }
        } else {
          jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem), altjwdStringBlender(aodentry.title))
          // At some point I may add a "fuzzy" vs "strict" matcher, aka 1 vs > .98
          if ( jwdcompare == 1 ) {
            // println "Found this exact Match (1.0) in title entry? ${aodentry.title}"
            returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
            // resultsAsSet += aodentry.title
            } else if ( strictJWDMatch == false ) {
            if ( jwdcompare > 0.990000000 ) {
              // println "Found this Likely Match (0.99+)? ${aodentry.title}"
              returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
            // resultsAsSet += aodentry.title
            } else if ( jwdcompare > 0.980000000 ) {
              // println "Found this Possible Match (0.98+)? ${aodentry.title} "
              returnAID == true ? (resultsAsSet += anidbID) : (resultsAsSet += aodentry.title)
            // resultsAsSet += aodentry.title
            }
          }
        }
      }
    } else {
      return
    }
  }
  return resultsAsSet
}

// ---------- Search Anime Offline Database for a NON-AniDB title match using JWD ---------- //
// --- This *should* as such only return unique results
// --- Synonyms in Anime Offline Database are NOT to be trusted to be accurate for the Title Entry
// --- Even for AniDB entries. It is useful to get some possible variations on the Title Name, but
// --- Not for an authoritive match to AID.
Set aodJWDSearchExcludeAniDB(JsonObject fileBotJsonCacheObject, Set searchList, locale, Boolean searchSynonyms = false, Boolean strictJWDMatch = false) {
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  resultsAsSet = [] as HashSet
  fileBotJsonCacheObject.data.each { aodentry ->
    if (aodentry.sources =~ /anidb/) {
      return
    }
    if ( searchSynonyms ) {
      aodtitles = []
      aodtitles += aodentry.title
      aodtitles += aodentry.synonyms.flatten()
    }
      searchList.each { searchItem ->
        if ( searchSynonyms ) {
        aodtitles.each { title ->
            jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem), altjwdStringBlender(title))
            // At some point I may add a "fuzzy" vs "strict" matcher, aka 1 vs > .98
            if ( jwdcompare == 1 ) {
            // println "Found this exact Match (1.0) in title entry? ${aodentry.title}"
            resultsAsSet += aodentry.title
            } else if ( strictJWDMatch == false ) {
            if ( jwdcompare > 0.990000000 ) {
              // println "Found this Likely Match (0.99+)? ${aodentry.title}"
              resultsAsSet += aodentry.title
              } else if ( jwdcompare > 0.980000000 ) {
              // println "Found this Possible Match (0.98+)? ${aodentry.title} "
              resultsAsSet += aodentry.title
            }
            }
        }
        } else {
        jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(searchItem), altjwdStringBlender(aodentry.title))
        // At some point I may add a "fuzzy" vs "strict" matcher, aka 1 vs > .98
        if ( jwdcompare == 1 ) {
          // println "Found this exact Match (1.0) in title entry? ${aodentry.title}"
          resultsAsSet += aodentry.title
            } else if ( strictJWDMatch == false ) {
          if ( jwdcompare > 0.990000000 ) {
            // println "Found this Likely Match (0.99+)? ${aodentry.title}"
            resultsAsSet += aodentry.title
              } else if ( jwdcompare > 0.980000000 ) {
            // println "Found this Possible Match (0.98+)? ${aodentry.title} "
            resultsAsSet += aodentry.title
          }
        }
        }
      }
  }
  return resultsAsSet
}

// ---------- Search Anime Offline Database for an AID ---------- //
// --- Return true if it's type is Not "TV"
// --- Return false if the type is "TV"
Boolean aodIsAIDTypeNotTV(JsonObject fileBotJsonCacheObject, Integer AID) {
//  println "The AID I'm Searching for is:[${AID}]"
  JsonObject myAniDBEntry = fileBotJsonCacheObject.data.find { aodentry ->
    // https://anidb.net/anime/1
    aodentry.sources.find { it ==~ /https:\/\/anidb\.net\/anime\/${AID}$/ }
  }
  //  println "myAniDBEntry.getclass:[${myAniDBEntry.getClass()}]"
//  println "myAniDBEntry:${myAniDBEntry}"
  // --- It will be null if an AID is not in the AOD List --- //
  if ( myAniDBEntry == null ) {
    return true
  } else {
    return myAniDBEntry.type != 'TV'
  }
}