package lib

import net.filebot.WebServices
import net.filebot.web.Episode
import net.filebot.web.SortOrder
import net.filebot.web.TheTVDBSeriesInfo

//--- VERSION 1.2.0

/**
 * Have filebot Search TVDB using a list, and return the results as a Set
 * This *should* as such only return unique results in the Set<TheTVDBSeriesInfo>
 *
 * @param  searchList   Set of Shows to search for. A Show can be the TVDBID # or a Show name.
 * @param  locale The Locale
 * @return  Set of shows from the filebot search.
 */
Set filebotTVDBSearch(Set searchList, locale) {
  resultsAsSet = [] as HashSet
  searchList.each { item ->
        myTVDBSearch = TheTVDB.search(item, locale)
        if (myTVDBSearch.isEmpty()) {
        } else {
          resultsAsSet += myTVDBSearch
        }
  }
  return resultsAsSet
}

/**
 * Have filebot get the Episode List for a TVDB Series and return it using the default series order (usually airdate)
 * and locale.ENGLISH
 *
 * @param  tvdbSeriesID The TVDB ID we want to get the Episode List for.
 * @return  Collection<Episode> of Episodes from TVDB
 */
Collection<Episode> filebotTVDBgetEpisodeList(Integer tvdbSeriesID) {
  TheTVDBSeriesInfo myTVDBseriesInfo = TheTVDB.getSeriesInfo(tvdbSeriesID, Locale.ENGLISH)
  return TheTVDB.getEpisodeList(myTVDBseriesInfo.id, myTVDBseriesInfo.order as SortOrder, Locale.ENGLISH)
}

/**
 * Return all TVDB Episodes in a specific Season of a TVDB Series by Series order (absolute or airdate)
 *
 * @param myTVDBEpisodes Collection<Episode> of TVDB Episodes (See filebotTVDBgetEpisodeList)
 * @param season The Season we want to return all episodes of
 * @param order The order type we want to use, defaults to absolute. The other option is airdate.
 * @return  Collection<Episode> of Episodes from TVDB
 */
ArrayList filebotTVDBSeasonEpisodeRange(Collection<Episode> myTVDBEpisodes, Integer season, String order = 'absolute') {
  return net.filebot.web.EpisodeUtilities.filterBySeason(myTVDBEpisodes, season).findAll { it."${order}" }
}

/**
 * Return all TVDB Episodes in a specific Season of a TVDB Series (ignoring series order)
 *
 * @param myTVDBEpisodes Collection<Episode> of TVDB Episodes (See filebotTVDBgetEpisodeList)
 * @param season The Season we want to return all episodes of
 * @return  Collection<Episode> of Episodes from TVDB
 */
ArrayList filebotTVDBSeasonEpisodes(Collection<Episode> myTVDBEpisodes, Integer season) {
  return net.filebot.web.EpisodeUtilities.filterBySeason(myTVDBEpisodes, season)
}

/**
 * Determine if an episode is contained in a collection of Episodes. With filebotTVDBSeasonEpisodes() or
 * filebotTVDBgetEpisodeList() usually being used to create a Collection<Episode> of episodes in which we will search
 *
 * @param myTVDBEpisodes Collection<Episode> of TVDB Episodes (See filebotTVDBSeasonEpisodes() or filebotTVDBgetEpisodeList() )
 * @param episodeNumber The Episode Number we are looking for
 * @param order The Episode Order we will use when searching, defaults to absolute, can also be set to airdate
 * @param returnEpisode Return the full Episode entry instead of a true/false Boolean
 * @param seasonNumber The Season number we will use when searching if order is airdate
 * @param whatAboutSpecials What do we do about specials in our search? Defaults to exclude, can also be include or only. When set to only, then ONLY specials will be searched.
 * @return  with returnEpisode = false (the default) returns Boolean, with returnEpisode = true returns net.filebot.web.Episode
 */
def filebotTVDBSeasonContainsEpisodeNumber(Collection<Episode> myTVDBEpisodes, Integer episodeNumber, String order = 'absolute', Boolean returnEpisode = false, Integer seasonNumber = 0, String whatAboutSpecials = 'exclude') {
  switch(order) {
    case 'absolute':
      switch (whatAboutSpecials) {
        case 'exclude':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { it.regular && (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) }
          } else {
            return myTVDBEpisodes.find { it.regular && (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) } != null
          }
          break
        case 'include':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) }
          } else {
            return myTVDBEpisodes.find { (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) } != null
          }
          break
        case 'only':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { it.special && (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) }
          } else {
            return myTVDBEpisodes.find { it.special && (it.absolute == episodeNumber || ( it.absolute == null && it.episode == episodeNumber ) ) } != null
          }
          break
        default:
          println '1:ERROR in filebotTVDBSeasonContainsEpisodeNumber'
          return []
          break
      }
      break
    case 'airdate':
      switch (whatAboutSpecials) {
        case 'exclude':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { it.regular && (it.episode == episodeNumber && it.season == seasonNumber) }
          } else {
            return myTVDBEpisodes.find { it.regular && (it.episode == episodeNumber && it.season == seasonNumber) } != null
          }
          break
        case 'include':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { (it.episode == episodeNumber && it.season == seasonNumber) }
          } else {
            return myTVDBEpisodes.find { (it.episode == episodeNumber && it.season == seasonNumber) } != null
          }
          break
        case 'only':
          if ( returnEpisode ) {
            return myTVDBEpisodes.find { it.special && (it.episode == episodeNumber && it.season == seasonNumber) }
          } else {
            return myTVDBEpisodes.find { it.special && (it.episode == episodeNumber && it.season == seasonNumber) } != null
          }
          break
        default:
          println '2:ERROR in filebotTVDBSeasonContainsEpisodeNumber'
          return []
          break
      }
      break
    default:
      println '3:ERROR in filebotTVDBSeasonContainsEpisodeNumber'
      return []
      break
  }
}

/**
 * Search Filebot's cache of AnimeLists for the specific TVDB ID.
 *
 * @param tvDBID The TVDB ID we are searching for in Filebot's AnimeLists cache
 * @return  Will return an ArrayList<net.filebot.web.AnimeLists$Entry> of all maps if the search succeeds, else it will return null
 */
def filebotAnimeListReturnFromTVDBID(Integer tvDBID) {
  def mySearchResult = tryQuietly {
    WebServices.AnimeList.model.anime.findAll {
      it.tvdbid == tvDBID
    }
  }
  if (mySearchResult == null || mySearchResult.isEmpty() ) {
    return null
  }
  return mySearchResult
}

/**
 * Try to determine the AniDB AID for a specific episode using TVDB ID # and Filebot's AnimeLists cache
 * This method is NOT meant to be used unless you already know there is an AnimeList entry in which to try to match.
 *
 * @param tvDBID The TVDB ID we are searching for in Filebot's AnimeLists cache
 * @param seasonNumber The TVDB Season we will use
 * @param episodeNumber The Episode # we will use
 * @return  Will return a single AnimeList Entry (net.filebot.web.AnimeLists$Entry)
 */
def filebotAnimeListReturnAIDEntry(Integer tvDBID, Integer seasonNumber, Integer episodeNumber = 0){
  // Will always return the non-offset (1st), AND offset for Episode # as 2nd assuming you give an Episode #
  ArrayList returnThing = filebotAnimeListReturnFromTVDBID(tvDBID).findAll { it.defaulttvdbseason == seasonNumber && (it.episodeoffset != null ? it.episodeoffset < episodeNumber : true) }
  if ( returnThing.size() > 1 && episodeNumber > 0 ) {
    return returnThing[1] // Class is net.filebot.web.AnimeLists$Entry
  } else {
    return returnThing[0]  // Class is net.filebot.web.AnimeLists$Entry
  }
}