package lib
//--- VERSION 1.0.0

// ---------- Have filebot Search TVDB using a list, and return the results as a HashSet ---------- //
// --- This *should* as such only return unique results
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
