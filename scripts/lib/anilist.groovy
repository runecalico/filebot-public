//file:noinspection unused
//file:noinspection GrMethodMayBeStatic
package lib

import net.filebot.Logging

//--- VERSION 1.1.2
// https://http-builder-ng.github.io/http-builder-ng/
@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')

// I'm not sure why these *NEED* to be import static, but it blows up otherwise.
import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.ContentTypes.JSON
import groovyx.net.http.*

/**
 * Function to LinkedHashMap of Anilist ID and Title
 *
 * @param animeSeason The Anime Season
 * @param animeYear The Year
 * @return Results
 */
Map anilistGetSeasonTitles(String animeSeason, Integer animeYear){
  // ---------- Variable Declaration ---------- //
  Integer queryPage = 1
  LinkedHashMap anilistSeasonTitles = [:]
  // ---------- Get Json Response to Request ---------- //
  Map anilistQueryResponseJson = queryAnilistForSeason(queryPage, animeSeason, animeYear)
  // ---------- Populate Season Titles ---------- //
  anilistQueryResponseJson.data.Page.media.each { title ->
    Logging.log.finest "ID: ${title.id}, Title: ${title.title.romaji}"
    anilistSeasonTitles.put(title.id, title.title.romaji)
  }
  // log.finest "${anilistSeasonTitles}"

  // ---------- Get/Parse additional pages as needed ---------- //
  while ( anilistQueryResponseJson.data.Page.pageInfo.hasNextPage == true ) {
    Logging.log.finest 'Need to fetch Another page'
    queryPage++
    Logging.log.finest "queryPage: ${queryPage}"
    anilistQueryResponseJson = queryAnilistForSeason(queryPage, animeSeason, animeYear)
    anilistQueryResponseJson.data.Page.media.each { title ->
      Logging.log.finest "ID: ${title.id}, Title: ${title.title.romaji}"
      anilistSeasonTitles.put(title.id, title.title.romaji)
    }
  }
  Logging.log.finest "${anilistSeasonTitles}"
  return anilistSeasonTitles
}

/**
 * Function to return JSON results of Season Query
 *
 * @param queryPage Query Page
 * @param animeSeason The Anime Season
 * @param animeYear The Year
 * @return Query results
 */
Map queryAnilistForSeason(Integer queryPage, String animeSeason, Integer animeYear ) {
  String query = """\
  query (\$season: MediaSeason, \$year: Int) {
    Page(page: ${queryPage}, perPage: 50) {
      pageInfo {
        total
        currentPage
        lastPage
        hasNextPage
        perPage
      }
      media(season: \$season, seasonYear: \$year) {
        id
        title {
          romaji
        }
      }
    }
  }
  """
  String queryVariables = """\
  {
    "season": "${animeSeason}",
    "year": ${animeYear}
  }
  """
  return postResponse = configure {
      request.uri = 'https://graphql.anilist.co/'
      request.contentType = JSON[0]
  }.post {
      request.body = [query: query, variables: queryVariables] // Works!

      response.failure { FromServer resp, Object body ->
        Logging.log.severe "POST request failed, HTTP - ${resp.properties}"
        Logging.log.severe "${body}"
      }
  }
}

