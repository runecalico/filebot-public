{
  if ("${self.media.Encoded_Date}" == 'null' ) {
    if ("${self.media.File_Created_Date_Local}" == 'null' ) {
      if ("${self.media.File_Modified_Date_Local}" == 'null' ) {
        myReleaseYear = "${today}".split('-')[0]
        } else {
        myReleaseYear = "${self.media.File_Modified_Date_Local('-')[0]}"
      }
      } else {
      myReleaseYear = "${self.media.File_Created_Date_Local.split('-')[0]}"
    }
    } else {
    myReleaseYear = "${self.media.Encoded_Date.replaceFirst(/UTC\s/, '').split('-')[0]}"
  }
  // This check is for when the binding is defined, but the UTC date data is screwed up.
  // Failed to read media characteristics: DateTimeParseException: Text 'UTC ' could not be parsed at index 4
  // Which gives UTC  as the entire binding value (useless)
  if ( myReleaseYear == '' ) {
    myReleaseYear = "${today}".split('-')[0]
  }
  myCurrentYear = "${today}".split('-')[0]
  // myShowYear/myShowMonth seems to frequently not populate
  myShowYear = any { "${ airdate.year }" } { "${ d.year }" } { "${ startdate.year }" } { "${ today }".split('-')[0] } { 'Year' }
  myShowMonth = any { "${ airdate.month }" } { "${ d.month }" } { "${ startdate.month }" } { "${ today }".split('-')[1] } { 'Month' }
  myShowSeason = """${ switch (myShowMonth) {
    case { myShowMonth.matches('0?[1-3]') }:
        return 'winter'
    case { myShowMonth.matches('0?[4-6]') }:
        return 'spring'
    case { myShowMonth.matches('0?[7-9]') }:
        return 'summer'
    case { myShowMonth.matches('10|11|12') }:
        return 'fall'
    default:
        return 'unknown'
    }}"""
  // Sometimes the metadata for media.Encoded_date is bogus. Or if you are HorribleSubs, seems to be stuck at 2010
  // So if myShowYear is less then or Equal to myReleaseYear, then assume the encoded date field is bogus.
  any
      {
        if ( "${myShowYear}" >=  "${myCurrentYear}" ) {
      myShowYear + '/' + myShowSeason
        } else {
        'releases'
        }
      }
      { 'Failure' }
  } /
    { any { db.AniDB.n.replaceAll(/\\|\//, '') } { db.AniDB.primaryTitle.replaceAll(/\\|\//, '') } { db.TheTVDB.n.colon(' - ').replaceTrailingBrackets().replaceAll(/\\|\//, '') } { n.replaceAll(/\\|\//, '') } }
    { any { if (db.AniDB.id) '[anidb-' + { db.AniDB.id } + ']' } { if (order.airdate.db.AniDB.id) '[anidb-' + { order.airdate.db.AniDB.id } + ']' } { if (order.absolute.db.AniDB.id) '[anidb-' + { order.absolute.db.AniDB.id } + ']' } { '[tvdb-' + db.TheTVDB.id + ']' } { '[tmdb-' + tmdbid + ']' } }
/ { fn }
