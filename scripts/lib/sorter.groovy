package lib

import groovy.transform.Field
import net.filebot.Logging
import net.filebot.WebServices
import net.filebot.web.SearchResult

//--- VERSION 1.7.2

import net.filebot.web.TheTVDBSeriesInfo
import org.apache.commons.text.similarity.JaroWinklerDistance
import com.cedarsoftware.util.io.JsonObject
import net.filebot.web.Episode
import java.util.regex.Matcher

// VOID - /(?i)((^[a-z\s]+)\(?((19\d\d|20\d\d)\)?\s))/
// VOID - /(?i)((^[a-z\s-]+)\(?((19\d\d|20\d\d)\)?\b))/
// VOID - /(?i)((^[^\d\(]+)\(?((19\d\d|20\d\d)\)?\b))/ // Thank you Intellij, but the escape is needed
// VOID - /(?i)((^[^\(]+)\(?((19\d\d|20\d\d)\)?\b))/
@Field String stripYearDateRegex = /(?i)[\.\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
// alt matcher - (?i)((^[^\(]+)\(?((19\d\d|20\d\d)\)?\b))
// alt matcher 2 - (?i)((^[a-z\s-]+)\(?((19\d\d|20\d\d)\)?\s))
// OVA, ONA or Special and all spaces, dashes etc around them. It also requires at least one around the word.
// Match 1 - The "Type" aka ova, ONA, OAD, SPECIAL, bsp, bspe, bonus etc. with spaces
// Match 2 - The "Type" aka ova, ONA, OAD, SPECIAL, bsp, bspe, bonus etc. without spaces :)
// VOID - /(?i)([-\s(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bspe\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/
@Field String ovaOnaOadSpecialBonusSyntaxMatcher = /(?i)([-\s(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bspe\d{1,2}|\bbonus)([\s]\d)?)[-\s\)]?/
@Field String matchEndOfLineVersion = /v(\d{1,2})\s?$/
@Field String stripTrailingSpacesDashRegex = /([\s-])*$/

/**
 * Generate the possible "series" anime names
 * Interger Series Syntax (series 1), Roman Ordinal Series Syntax (series i), or Ordinal Series Syntax (series 2nd)
 *
 * @param String animeName
 * @return  HashSet of [seriesGeneratedNames]
 */
HashSet seriesNameGenerator ( String baseAnimeName, Integer mySeasonalityNumber, Boolean hasRomanSeries ) {
  HashSet tempBaseGeneratedAnimeNames = []
  String generatedAnimeName
  if ( mySeasonalityNumber > 1 ) {
    // ---------- Add Series Name Varients as options ---------- //
    generatedAnimeName = baseAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
    Logging.log.info "----- Adding Ordinal Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    if ( mySeasonalityNumber < 10 ) {
      generatedAnimeName = baseAnimeName + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
      Logging.log.info "----- Adding Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    }
    generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime 2
    Logging.log.info "----- Adding Series # Anime Name to Anime Name List - ${generatedAnimeName}"
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
  } else if ( hasRomanSeries )  {
    generatedAnimeName = baseAnimeName + ' ' + group.seriesNumber // anime I/II/III/IV/V
    Logging.log.info "----- Adding Series Anime Name to Anime Name List - ${generatedAnimeName}"
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
  }
  return tempBaseGeneratedAnimeNames
}
/**
 * Generate the base anime names we will be using to build possible Anime Series names to search for
 * Basenames are the base part of the series name, later we add/modify based on things like the various seasonality syntax (Partial, Ordinal etc)
 *
 * @param group The LinkedHashMap for this group of files
 * @param useBaseAnimeNameWithSeriesSyntax Should we add the Base Anime Name when the Series has "Series" Syntax
 * @return  ArrayList of [[group], baseGeneratedAnimeNames ]
 */
ArrayList basenameGenerator ( LinkedHashMap group, Boolean useBaseAnimeNameWithSeriesSyntax ) {
  tempBaseGeneratedAnimeNames = [] as HashSet
  baseGeneratedAnimeNames = [] as HashSet
  String baseAnimeName
  Logging.log.info '// START---------- Basename Generation ---------- //'
  Logging.log.finest "${groupInfoGenerator(group)}"
  Logging.log.finest "group.class:${group.getClass()}"
  Logging.log.info "--- group.anime - ${group.anime}"
  switch(group.anime) {
    // How is this supposed to actually find something?
    case ~/fanrenxiuxianzhuan mdzf/:
      baseAnimeName =  'Fanren Xiuxian Chuan Mo Dao Zheng Feng'
      break
    // Huh.  It actually DOES have TV in the official name
    case ~/yarou nanana kaibutsu kraken o oe!/:
      baseAnimeName =  'TV Yarou Nanana: Kaibutsu Kraken o Oe!'
      break
    // the duke and his maid
    case ~/the duke and his maid/:
      baseAnimeName =  'The Duke of Death and His Maid'
      break
    // tdg
    case ~/tdg/:
      baseAnimeName =  'Tales of Demons and Gods'
      break
    // Taishou Otome Otogibanashi
    case ~/taishou otome/:
      baseAnimeName =  'Taishou Otome Otogibanashi'
      break
    // Lion Force
    case ~/lion force/:
      baseAnimeName =  'Hyakujuu Ou Golion'
      break
    // Iruma-kun is not an accepted short
    case ~/iruma-kun/:
      baseAnimeName =  'Mairimashita! Iruma-kun'
      break
    // Mini Dragon (Miss Kobayashi's Dragon Maid S Short Animation Series) is considered special episodes
    case ~/mini dragon/:
      baseAnimeName =  'Kobayashi-san Chi no Maidragon S'
      group.isSpecialEpisode = true
      break
    case ~/stellar transformations/:
      baseAnimeName =  'stellar transformation'
      break
    // Monster Farm in TVDB is not anime
    case ~/monster farm/:
      baseAnimeName =  'monster rancher'
      break
    // Filebot detected name is *almost* there ..
    case ~/kmplx m3 the dark metal/:
      baseAnimeName =  'M3 the dark metal'
      break
    // Close ..
    case ~/granblue/:
      baseAnimeName =  'Granblue Fantasy The Animation'
      break
    // Close ..
    case ~/hige wo soru soshite joshikousei wo hirou/:
      baseAnimeName =  'Hige o Soru. Soshite Joshikousei o Hirou.'
      break
    // Considered a Special, not a "Real" series
    case ~/mobile suit gundam zz frag/:
      baseAnimeName =  'Kidou Senshi Gundam ZZ'
      group.isSpecialEpisode = true
      break
    // Not a common short name for it..
    case ~/kono sekai the animation/:
      baseAnimeName =  'The World Ends with You the Animation'
      break
    // Close, and yet so far ..
    case ~/jiranaide, nagatoro-san/:
      baseAnimeName =  'Ijiranaide, Nagatoro-san'
      break
    // Special Name != Anime name
    case ~/sk crazy rock jam/:
      baseAnimeName =  'SK8 the Infinity'
      group.isSpecialEpisode = true
      break
    // Special Name != Anime name
    case ~/himouto! umaru-chans/:
      baseAnimeName =  'Himouto! Umaru-chan'
      break
    // Spelling is important
    case ~/nejimaki seirei senki tenkyo no aruderamin/:
      baseAnimeName =  'Nejimaki Seirei Senki: Tenkyou no Alderamin'
      break
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
  // ---------- Checking for Name Variations --- //
  // Anime names that don't really match well, so they need help
  // This could possibly be replaced by custom synonym file .. or maybe some kind of regex match this replace with this file?
  switch(baseAnimeName) {
    case ~/marulks daily life/:
      baseAnimeName = 'Made in Abyss: Dawn of the Deep Soul'
      group.isSpecialEpisode = true
      break
  }
  // Because the way I parse filenames nomad - megalo box 2 ends up nomad with alt title of megalo box ..
  if ( baseAnimeName == 'nomad' && group.altTitle == 'megalo box') {
    baseAnimeName = 'nomad: megalo box'
    group.altTitle == null
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
  Logging.log.info "----- baseAnimeName - ${baseAnimeName}"
  // If it ends with Special or Bonus, remove that and add that as a basename.
  // VOID - myOVARegexMatcher = group.anime =~ /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?$/
  // VOID - myOVARegexMatcher = group.anime =~ /(?i)([-\s(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s)]?$/
  myOVARegexMatcher = group.anime =~ /${ovaOnaOadSpecialBonusSyntaxMatcher}/
  if ( myOVARegexMatcher.find() ) {
    generatedAnimeName = group.anime.replaceAll(/${ovaOnaOadSpecialBonusSyntaxMatcher}/, '')
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    Logging.log.info "----- TEMP: OVAMatcher - Adding to base Name List - [${generatedAnimeName}] "
  }
  // Unfortunately there is at *least* one anime that officially ends with v3, so I can't just ignore the "version" and the end.
  // - https://anidb.net/anime/5078
  myRegexMatcher = group.anime =~ /${matchEndOfLineVersion}/
  if ( myRegexMatcher.find() ) {
    generatedAnimeName = group.anime.replaceAll(/${matchEndOfLineVersion}/, '')
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    Logging.log.info "----- TEMP: End of Line Version - Adding to base Name List - [${generatedAnimeName}] "
  }
  if (!group.hasSeriesSyntax || (group.hasSeriesSyntax && useBaseAnimeNameWithSeriesSyntax) || (group.hasSeriesSyntax && group.order == 'airdate')) {
    Logging.log.info "----- TEMP: Adding to base Name List - [${baseAnimeName}] "
    tempBaseGeneratedAnimeNames += ["${baseAnimeName}"]
  }
  if ( baseAnimeName =~ /(?i)^the\s/ ) {
    Logging.log.info "----- TEMP: 'the' prefix - Adding to base Name List - [${baseAnimeName}] "
    tempBaseGeneratedAnimeNames += ["${baseAnimeName.replaceFirst(/(?i)^the\s/, '')}"]
  }
  // If it has "series" syntax that can mean series 1 (Interger Series Syntax), series i (Roman Ordinal Series Syntax), or series 2nd (Ordinal Series Syntax)
  // Note: Ordinal Series Syntax is NOT the same as Ordinal Seasonality Syntax (series 2nd Season).  While it's more common to see Ordinal Seasonality Syntax
  // There are some anime that actually use Ordinal Series Syntax.
  if ( group.hasSeriesSyntax ) {
    Logging.log.info "----- hasSeriesSyntax detected"
    hasSeasonality = true
    switch (group.seriesNumber) {
      case ~/[0-9]/:
        mySeasonalityNumber = group.seriesNumber.toInteger()
        Logging.log.info "----- Numerical Series - mySeasonalityNumber: ${mySeasonalityNumber}"
        hasRomanSeries = false
        break
      default:
//        mySeasonalityNumber = group.seriesNumber
        mySeasonalityNumber = getNumberFromRomanOrdinal(group.seriesNumber)
        Logging.log.info "----- Roman Series - Ordinal: ${group.seriesNumber}"
        Logging.log.info "----- Roman Series - mySeasonalityNumber: ${mySeasonalityNumber}"
        hasRomanSeries = true
        break
    }
    tempBaseGeneratedAnimeNames += seriesNameGenerator(baseAnimeName, mySeasonalityNumber, hasRomanSeries)
    if ( group.altTitle != null ) {
      Logging.log.info "----- Alternative Title Detected:[${group.altTitle}]"
//      Logging.log.info "----- Adding Alternative Title to Anime Name List - ${group.altTitle}"
      tempBaseGeneratedAnimeNames += seriesNameGenerator(group.altTitle as String, mySeasonalityNumber, hasRomanSeries)
//      tempBaseGeneratedAnimeNames += ["${group.altTitle}"]
      // Unfortunately there is at *least* one anime that officially ends with v3, so I can't just ignore the "version" and the end.
      // - https://anidb.net/anime/5078
      myRegexMatcher = group.altTitle =~ /${matchEndOfLineVersion}/
      if ( myRegexMatcher.find() ) {
        Logging.log.info "----- TEMP: End of Line Version - Adding to base Name List"
        generatedAnimeName = group.altTitle.replaceAll(/${matchEndOfLineVersion}/, '')
        tempBaseGeneratedAnimeNames += seriesNameGenerator(generatedAnimeName as String, mySeasonalityNumber, hasRomanSeries)
      }
    }
    if ( baseAnimeName =~ /(?i)^the\s/ ) {
      Logging.log.info "----- 'the' prefix detected':[${baseAnimeName}]"
      tempBaseGeneratedAnimeNames += seriesNameGenerator(baseAnimeName.replaceFirst(/(?i)^the\s/, ''), mySeasonalityNumber, hasRomanSeries)
      // Unfortunately there is at *least* one anime that officially ends with v3, so I can't just ignore the "version" and the end.
      // - https://anidb.net/anime/5078
      myRegexMatcher = baseAnimeName =~ /${matchEndOfLineVersion}/
      if ( myRegexMatcher.find() ) {
        Logging.log.info "----- TEMP: End of Line Version - Adding to base Name List"
        generatedAnimeName = baseAnimeName.replaceAll(/${matchEndOfLineVersion}/, '')
        tempBaseGeneratedAnimeNames += seriesNameGenerator(generatedAnimeName as String, mySeasonalityNumber, hasRomanSeries)

      }
    }
    // baseAnimeName = "${jwdStringBlender(group.anime)}" // Always add the group.anime name
    // Logging.log.info "----- Adding Base Anime Name to base Name List - ${baseAnimeName}  - Season 1/0"
    // tempBaseGeneratedAnimeNames += ["${baseAnimeName}"]
//    if ( mySeasonalityNumber > 1 ) {
//      // ---------- Add Series Name Varients as options ---------- //
//      generatedAnimeName = baseAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
//      Logging.log.info "----- Adding Ordinal Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
//      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//      if ( mySeasonalityNumber < 10 ) {
//        generatedAnimeName = baseAnimeName + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
//        Logging.log.info "----- Adding Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
//        tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//      }
//      generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime 2
//      Logging.log.info "----- Adding Series # Anime Name to Anime Name List - ${generatedAnimeName}"
//      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//    } else if ( hasRomanSeries )  {
//      generatedAnimeName = baseAnimeName + ' ' + group.seriesNumber // anime I/II/III/IV/V
//      Logging.log.info "----- Adding Series Anime Name to Anime Name List - ${generatedAnimeName}"
//      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//    }
//    if ( !hasRomanSeries ) {
//      if ( mySeasonalityNumber > 1 ) {
//        // ---------- Add Series Name Varients as options ---------- //
//        generatedAnimeName = baseAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
//        Logging.log.info "----- Adding Ordinal Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
//        tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//        if ( mySeasonalityNumber < 10 ) {
//          generatedAnimeName = baseAnimeName + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
//          Logging.log.info "----- Adding Seasonality Anime Name to Anime Name List - ${generatedAnimeName}"
//          tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//        }
//        generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime 2
//        Logging.log.info "----- Adding Series # Anime Name to Anime Name List - ${generatedAnimeName}"
//        tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//      }
//    } else {
//      generatedAnimeName = baseAnimeName + ' ' + mySeasonalityNumber // anime I/II/III/IV/V
//      Logging.log.info "----- Adding Series Anime Name to Anime Name List - ${generatedAnimeName}"
//      tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
//      switch (mySeasonalityNumber) {
//        case ~/i/:
//          mySeasonalityNumber = 1
//          break
//        case ~/ii/:
//          mySeasonalityNumber = 2
//          break
//        case ~/iii/:
//          mySeasonalityNumber = 3
//          break
//        case ~/iv/:
//          mySeasonalityNumber = 4
//          break
//        case ~/v/:
//          mySeasonalityNumber = 5
//          break
//        case ~/vi/:
//          mySeasonalityNumber = 6
//          break
//        case ~/vii/:
//          mySeasonalityNumber = 7
//          break
//        case ~/viii/:
//          mySeasonalityNumber = 8
//          break
//        case ~/ix/:
//          mySeasonalityNumber = 9
//          break
//        case ~/x/:
//          mySeasonalityNumber = 10
//          break
//        default:
//          mySeasonalityNumber = group.seriesNumber
//          break
//      }
//      Logging.log.info "----- Roman Series - mySeasonalityNumber is now: ${mySeasonalityNumber}"
//    }
  }
//  if ( group.altTitle != null ) {
//    Logging.log.info "----- Alternative Title Detected:[${group.altTitle}]"
//    Logging.log.info "----- Adding Alternative Title to Anime Name List - ${group.altTitle}"
//    tempBaseGeneratedAnimeNames += ["${group.altTitle}"]
//  }
  tempBaseGeneratedAnimeNames.each { tempname ->
    Logging.log.info "----- BASE: Adding [${tempname}]"
    baseGeneratedAnimeNames += tempname
    baseGeneratedAnimeNames += ["${returnAniDBRomanization(tempname)}"]
  }
  // baseGeneratedAnimeNames = baseAnimeNameGenerator()
  Logging.log.finest "${groupInfoGenerator(group)}"
  Logging.log.info '// END---------- Basename Generation ---------- //'
  return [[group], baseGeneratedAnimeNames ]
}


/**
 * Generate a printable string that contains only the group values that are set.
 *
 * @param group The LinkedHashMap for this group of files
 * @return  String containing the group values that are set (not null)
 */
@SuppressWarnings('GrMethodMayBeStatic')
String groupInfoGenerator ( def group ) {
  def groupInfo = "Group: $group.anime, order: $group.order"
  if ( group.altTitle != null ) { groupInfo = groupInfo + ", altTitle: $group.altTitle" }
  if ( group.animeDetectedName != null ) { groupInfo = groupInfo + ", animeDetectedName: $group.animeDetectedName" }
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
 * Generate the filebot mapper(s) to use when we perform a rename. The Idea is not use mappers that do not have the necessary data behind them to work (AnimeList.AniDB etc)
 *
 * @param order The Episode ordering used, absolute or airdate
 * @param db The rename database, anidb or tvdb
 * @param dbHasAbsoluteNumbering Does it have Absolute Numbering (important really only for tvdb)
 * @param hasAnimeListMapping Does it have an AnimeList mapping entry?
 * @return renameMapper String [ mapper1, mapper2 ]
 */
@SuppressWarnings('GrMethodMayBeStatic')
String renameMapperGenerator( String order, String db , Boolean dbHasAbsoluteNumbering, Boolean hasAnimeListMapping) {
  String renameMapper = ""
  switch (db){
    case 'tvdb':
      if ( order.toLowerCase() == 'airdate' ) { renameMapper = renameMapper + ' episode '}
      if ( hasAnimeListMapping ) { renameMapper = renameMapper + ' AnimeList.AniDB '}
      if ( order.toLowerCase() == 'absolute' && dbHasAbsoluteNumbering ) { renameMapper = renameMapper + ' order.absolute.episode ' }
      if ( order.toLowerCase() == 'absolute' && !dbHasAbsoluteNumbering && !hasAnimeListMapping) { renameMapper = renameMapper + ' order.absolute.episode.derive(e) ' }
      break
    case 'anidb':
      if ( order.toLowerCase() == 'absolute' ) { renameMapper = renameMapper + ' episode '}
      if ( hasAnimeListMapping ) { renameMapper = renameMapper + ' AnimeList.TheTVDB '}
      break
  }
  renameMapper = "[" + renameMapper.replaceAll(/^([\s-])*/, '').replaceAll(/([\s-])*$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/\s/, ',') + "]"
  return renameMapper
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
    Logging.log.finest "...AniDB"
    animeANIDBSearchFound = false
    myQueryAniDB = series
    myOptionsAniDB = WebServices.AniDB.search(myQueryAniDB, locale) as HashSet
    Logging.log.finest "myOptionsAniDB Class : ${myOptionsAniDB.getClass()}"
    if (myOptionsAniDB.isEmpty()) {
      Logging.log.finest "TV Series not found in AniDB by FileBot: $myQueryAniDB"
      // --- We are searching AniDB XML Titles because occationally filebot will not find something, but the Title search will.
      // --- And while it's slow as hell, I'm not going to do this all the time ..
      myOptionsAniDB += anidbHashTitleSearch(aniDBCompleteXMLList, ["${myQueryAniDB}"] as Set, locale, false, false, false, 3)
      if (myOptionsAniDB.isEmpty()) {
        Logging.log.finest "TV Series not found in AniDB by AniDB XML Title Search: $myQueryAniDB"
      } else {
        Logging.log.finest "Our Query Returned from AniDB: ${myOptionsAniDB}"
        animeANIDBSearchFound = true
      }
    } else {
      // TODO
      // Filebot returns ' while AniDB returns `, so sometimes *effective* duplicates will occur in myOptionsAniDB
      // which waste processing time.
      Logging.log.finest "Our Query Returned from AniDB: ${myOptionsAniDB}"
      animeANIDBSearchFound = true
      Logging.log.finest "Filebot Returned ${myOptionsAniDB.size()} Titles:${myOptionsAniDB} :::FOR::: ${myQueryAniDB}"
      // --- Return AID as there are a few edge cases where there are in fact multiple titles with the EXACT same words, but
      // --- one might be the Official title, while the Other the Main Title, or set as different languages etc.
      // --- So returning only the title will mean we *might* not get the actual AID from the query, so return the AID
      // --- This also means we don't have to lookup the AID in the next stage as well :)
      myOptionsAniDB += anidbHashTitleSearch(aniDBCompleteXMLList, ["${myQueryAniDB}"] as Set, locale, true, false, false, 3)
      Logging.log.finest "After XMLTitleSearch ${myOptionsAniDB.size()} Titles:${myOptionsAniDB} :::FOR::: ${myQueryAniDB}"
    }
    if ( animeANIDBSearchFound ) {
      animeFoundInAniDB = true
      // ---------- Parse Series Results ---------- //
      myOptionsAniDB.each { results ->
        Logging.log.finer "Comparing Search Result - ${results}"
        // ---------- START - Compile Aliases for Current Result ---------- //
        try {
          // Logging.log.info "Get Series Information for Aliases"
          // Between some wierdness with the Aliases returned and wanting to reduce the API call's to AniDB
          // Switch to getting the Series info from AniDB Title XML
          // Filebot doesn't return aliases for AniDB when using query by ID
          if ( useFilebotAniDBAliases ) {
            try {
              myTVDBseriesInfo = WebServices.AniDB.getSeriesInfo(results as SearchResult, locale)
            } catch (e) {
              Logging.log.severe "filebotAnidbJWDSearch() - getSeriesInfo() - Caught error:[${e}]"
              myTVDBseriesInfo = []
            }
          }
          if ( useFilebotAniDBAliases && myTVDBseriesInfo != [] ) {
            myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames // Unfortunately this does NOT always include Synonyms! (but sometimes it does)
          } else {
            // For some reason beyond my comprehension, Filebot includes aliases for multiple seasons of My Teen Romantic Comedy: SNAFU aka Yahari Ore no Seishun LoveCome wa Machigatte Iru.
            // So just zero them out and let the XML take over..
            myTBDBSeriesInfoAliasNames = []
          }
          // Literal Match will not work as Filebot Search returns are *slightly* different (encoding differences?) aka filebot uses ', while AniDB XML is `
          // So we need to change it to suit (hopefully it will not end up being a ongoing and expanding issue"
          if ( results.toString().isInteger() ) {
            gotAniDBID = results as Integer
            Logging.log.finest "Got AID: ${gotAniDBID}"
          } else {
            //noinspection GroovyAssignabilityCheck
            gotAniDBID = anidbHashTitleSearch(aniDBCompleteXMLList, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
            if ( gotAniDBID <= 0 ) {
              //noinspection GroovyAssignabilityCheck
              gotAniDBID = anidbHashTitleSearch(aniDBCompleteXMLList, ["${results.toString().replaceAll(/'/, '`')}"] as Set, locale, true, false, true, 3)[0] // It returns a linkedHashSet
              Logging.log.finest "Searching Synonyms.xml returned: ${gotAniDBID}"
            }
            Logging.log.finest "Got AID: ${gotAniDBID} for ${results}"
          }
          // Logging.log.info "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}" // No info on # of Episodes
          // myTVDBseriesInfo properties for Sword Art Online II: [runtime:null, startDate:2014-07-05, genres:[], certification:null, rating:5.26, id:10376, name:Sword Art Online II, network:null,
          // ratingCount:null, type:Anime, class:class net.filebot.web.SeriesInfo, spokenLanguages:[], order:Absolute, status:null, language:en,
          // aliasNames:[????????????II, ???????????II, Gun Art Online, Sword Art Online 2, Sword Art Online II: Calibur, Sword Art Online II: Mother's Rosario, Sword Art Online II: Phantom Bullet, GGO, SAO 2, SAO2, SAOII], database:AniDB]
          // Logging.log.info "${myTVDBseriesInfo.properties}"
          // I am not sure exactly why, but the alias info returned on AniDB series FREQUENTLY does not match what's in the anime-titles.xml
          // Sometimes WILDLY AND INCORRECTLY so (as in aliases for different series)
          // myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames // Unfortunately this does NOT always include Synonyms! (but sometimes it does)
          // Logging.log.info "myTBDBSeriesInfoAliasNames: ${myTBDBSeriesInfoAliasNames}"
          // Logging.log.info "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}"
//           Logging.log.info "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}"
//          Logging.log.info "myTBDBSeriesInfoAliasNames Class : ${myTBDBSeriesInfoAliasNames.getClass()}"
          // gotAniDBID = myTVDBseriesInfo.id
          // teir1JWDResults += [[db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, score: jwdcompare]]
        } catch (e) {
          Logging.log.severe "filebotAnidbJWDSearch() - Caught error:[${e}]"
          myTVDBseriesInfo = []
          myTBDBSeriesInfoAliasNames = []
          gotAniDBID = 0
        }
        Logging.log.finest "// -------------------- //"
        Logging.log.finest "Our AniDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
        Logging.log.finest "myTBDBSeriesInfoAliasNames Class : ${myTBDBSeriesInfoAliasNames.getClass()}" // java.util.ArrayList
        if ( gotAniDBID > 0 ) {
          def anidbAnimeEntrySearchResult = anidbXMLGetAnimeEntry(aniDBTitleXML, gotAniDBID)
          Logging.log.finest "anidbAnimeEntrySearchResult Class : ${anidbAnimeEntrySearchResult.getClass()}" // groovy.util.Node
          def anidbAnimeEntryTitle = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntrySearchResult)
          Logging.log.finest "anidbAnimeEntryTitle Class : ${anidbAnimeEntryTitle.getClass()}" // groovy.util.Node
          Logging.log.finest "---> anidbAnimeEntryTitle: ${anidbAnimeEntryTitle.text()}"
          myTVDBseriesInfo = [ 'id': gotAniDBID, 'name': anidbAnimeEntryTitle.text()]
          Logging.log.finest "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}" // java.util.LinkedHashMap
          myAniDBOMTitles = anidbXMLEntryGetAnimeOMTitles(anidbAnimeEntrySearchResult)
          Logging.log.finest "myAniDBOMTitles Class : ${myAniDBOMTitles.getClass()}" // class java.util.ArrayList
          Logging.log.finest "-----> myAniDBOMTitles: ${myAniDBOMTitles}"
          List anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntrySearchResult)
          Logging.log.finest "-----> anidbAnimeEntrySynonyms: ${anidbAnimeEntrySynonyms}"
          List anidbAnimeEntryShorts = anidbXMLEntryGetAnimeShorts(anidbAnimeEntrySearchResult)
          Logging.log.finest "-----> anidbAnimeEntryShorts: ${anidbAnimeEntryShorts}"
          Logging.log.finest "XML Alias/Shorts: ${anidbAnimeEntryShorts.size() + anidbAnimeEntrySynonyms.size()}"
          Logging.log.finest "myTBDBSeriesInfoAliasNames: ${myTBDBSeriesInfoAliasNames.size()}"
          myTBDBSeriesInfoAliasNames += anidbAnimeEntrySynonyms
          myTBDBSeriesInfoAliasNames += anidbAnimeEntryShorts
          Logging.log.finest "After XML we have AniDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
          Logging.log.finest "// -------------------- //"
          // Additional Synonyms
          def anidbAnimeEntrySearchResultSynonyms = anidbXMLGetAnimeEntry(aniDBSynonymXML, gotAniDBID)
          Logging.log.finest "-----> anidbAnimeEntrySearchResultSynonyms: ${anidbAnimeEntrySearchResultSynonyms}"
          if ( anidbAnimeEntrySearchResultSynonyms != null) {
            // def anidbAnimeEntryTitleSynonyms = anidbXMLEntryGetAnimePrimaryTitle(anidbAnimeEntrySearchResultSynonyms)
//            Logging.log.finest "-----> anidbAnimeEntryTitleSynonyms: ${anidbAnimeEntryTitleSynonyms}"
            anidbAnimeEntrySynonyms = anidbXMLEntryGetAnimeSynonyms(anidbAnimeEntrySearchResultSynonyms)
            Logging.log.finest "-----> anidbAnimeEntrySynonyms: ${anidbAnimeEntrySynonyms}"
            myTBDBSeriesInfoAliasNames += anidbAnimeEntrySynonyms
          }
        }
        // ---------- END - Compile Aliases for Current Result ---------- //
        myAniDBOMTitles.each { myTitle ->
          Logging.log.finest "Running JWDComparision of BOM Title - ${myTitle} to ${series}"
          jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(myTitle.toString()), altjwdStringBlender(series.toString()))
          Logging.log.finest "jaroWinklerDistance of ${jwdStringBlender(results.toString())} TO ${jwdStringBlender(series.toString())}: ${jwdcompare}"
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            // Get/Set AnimeType from AnimeOfflineDatabase
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            Logging.log.finest "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
            Logging.log.finest "higher"
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: myTitle, alias: false, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            Logging.log.finest "lower"
          }
        }
        myTBDBSeriesInfoAliasNames.each { aliases ->
          // myRegexMatcher = aliases =~ /\?\?\?\?/ // This doesn't work, The string just doesn't display correctly .. ^[ -~]*$
          myRegexMatcher = aliases =~ /^[ -~]*$/
          if ( myRegexMatcher.find() ) {
            Logging.log.finest "AniDB Aliases: English Name: ${aliases}"
          } else {
            Logging.log.finest "AniDB Aliases: Not So English Name: ${aliases}"
            return
          }
          Logging.log.finest "Running JWDCompare of  Alias - ${aliases} to ${series}"
          jwdcompare2 = jaroWinklerDistance.apply(altjwdStringBlender(aliases.toString()), altjwdStringBlender(series.toString()))
          Logging.log.finest "altjwdcompare2 of ${altjwdStringBlender(aliases.toString())} and ${altjwdStringBlender(series.toString())}"
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            Logging.log.finest"Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            Logging.log.finest"higher"
            returnThing = setAnimeTypeFromAID(animeOffLineDatabaseJsonObject, myTVDBseriesInfo.id, null, false, false)
            isSpecialType = returnThing.isSpecialType
            specialType = returnThing.specialType
            isMovieType = returnThing.isMovieType
            hasAnimeListEntry = filebotAnimeListReturnFromAID(myTVDBseriesInfo.id,) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'AniDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, isSpecialType: isSpecialType, isMovieType: isMovieType, specialType: specialType, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            Logging.log.finest "lower"
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
@SuppressWarnings(['GrReassignedInClosureLocalVar', 'unused'])
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
    Logging.log.finest "Looking for Best Match for ${series}"
    // ---------- Start with TheTVDB ---------- //
    Logging.log.finest "...TheTVDB"
    myQueryTVDB = series
    Logging.log.finest "myQueryTVDB.getclass:[${myQueryTVDB.getClass()}]" // java.lang.String
    myOptionsTVDB = WebServices.TheTVDB.search(myQueryTVDB, locale)
    Logging.log.finest "myOptionsTVDB.getclass:[${myOptionsTVDB.getClass()}]" // java.util.ArrayList
    if (myOptionsTVDB.isEmpty()) {
      Logging.log.warning "TV Series not found in TheTVDB: $myQueryTVDB"
    } else {
      Logging.log.finest "Our Query Returned from TheTVDB: ${myOptionsTVDB}"
      animeTVDBSearchFound = true
    }
    if ( animeTVDBSearchFound ) {
      // ---------- Parse Series Results ---------- //
      //    --- Because it seems that TheTVDB can sometimes have "invalid" series return in the list from TheTVDB.Search, that you can't determine until you try a TheTVDB.getSeriesInfo on them
      //    --- We need to run the getSeriesInfo right away before any jaroWinklerDistance comparisions, so we can skip the Series if it's "invalid"
      myOptionsTVDB.each { results ->
        Logging.log.finer "Get Series Information - ${results}"
        // Need to replicate this kind of try/catch/check value for all TheTVDB/AniDB lookups
        try {
          myTVDBseriesInfo = WebServices.TheTVDB.getSeriesInfo(results as SearchResult, locale)
          // Logging.log.info "myTVDBseriesInfo properties for ${results}: ${myTVDBseriesInfo.properties}" // No info on # of episodes
          // myTVDBseriesInfo properties for Sword Art Online: [runtime:25, startDate:2012-07-07, genres:[Action, Adventure, Animation, Anime, Fantasy, Romance, Science Fiction],
          // overview:In the near future, a Virtual Reality Massive Multiplayer Online Role-Playing Game (VRMMORPG) called Sword Art Online has been released where players control their avatars with their bodies using a piece of technology called Nerve Gear. One day, players discover they cannot log out, as the game creator is holding them captive unless they reach the 100th floor of the game's tower and defeat the final boss. However, if they die in the game, they die in real life. Their struggle for survival starts now...,
          // certification:TV-14, rating:7.9, id:259640, slug:sword-art-online, lastUpdated:1599661699, name:Sword Art Online, network:Tokyo MX, ratingCount:18128,
          // imdbId:tt2250192, type:TV Series, class:class net.filebot.web.TheTVDBSeriesInfo, spokenLanguages:[], status:Continuing,
          // order:null, language:en, airsTime:12:00 AM, aliasNames:[Sword Art Online II, S?do ?to Onrain, Sword Art Online Alicization, Sword Art Online Alicization: War of Underworld, SAO, S.A.O, Sword Art Online : Alicization, Sword Art Online : Alicization - War of Underground, Sword Art Online II , S.A.O 2, S.A.O 3, S.A.O 4, S.A.O II, S.A.O III, S.A.O IV, SAO 2, SAO 3, SAO 4, SAO II, SAO III, SAO IV, Sword Art Online 2, Sword Art Online 3, Sword Art Online 4, Sword Art Online III, Sword Art Online IV, ????, Sword Art Online (2012), ?????? ???? ???-????, ?????? ???? ???????, ????????????, ???????????, ?? ?? ???, ??????? ???? ??????, ????],
          // season:4, airsDayOfWeek:Sunday, database:TheTVDB]
          // Logging.log.info "myTVDBseriesInfo Class : ${myTVDBseriesInfo.getClass()}"
//          Logging.log.info "myTBDBSeriesInfoAliasNames.getclass:[${myTBDBSeriesInfoAliasNames.getClass()}]" // java.util.ArrayList
        } catch (e) {
          Logging.log.severe "filebotTVDBJWDSearch() = WebServices.TheTVDB.getSeriesInfo - Caught error:[${e}]"
          Logging.log.severe "results:[${results}]"
          myTVDBseriesInfo = []
          myTBDBSeriesInfoAliasNames = []
        }
        //noinspection GrEqualsBetweenInconvertibleTypes
        if (myTVDBseriesInfo == []) {
          Logging.log.warning "Can not get Series Info for ${results} in TheTVDB - []"
          return // Skip to the next result
        } else {
          myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames
        }
        animeFoundInTVDB = true
        jwdcompare = jaroWinklerDistance.apply(altjwdStringBlender(results.toString()), altjwdStringBlender(series.toString()))
        if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
          Logging.log.finest "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
          hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false, hasAnimeListEntry: hasAnimeListEntry]]
        } else if ( jwdcompare > jwdResults[(myTVDBseriesInfo.id)].score ) {
          Logging.log.finest "higher"
          hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
          jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: results, alias: false, hasAnimeListEntry: hasAnimeListEntry]]
        } else {
          Logging.log.finest "lower"
        }
        // ---------- Parse Aliases for Current Result ---------- //
        myTBDBSeriesInfoAliasNames = myTVDBseriesInfo.aliasNames
        Logging.log.finest "Our TheTVDB Aliases: ${myTBDBSeriesInfoAliasNames} for ${results}"
        myTBDBSeriesInfoAliasNames.each { aliases ->
          // myRegexMatcher = aliases =~ /\?\?\?\?/
          myRegexMatcher = aliases =~ /^[ -~]*$/
          if ( myRegexMatcher.find() ) {
            Logging.log.finest "TheTVDB Aliases: English Name: ${aliases}"
          } else {
            Logging.log.finest "TheTVDB Aliases: Not So English Name: ${aliases}"
            return
          }
          Logging.log.finest"Comparing Alias - ${aliases}"
          jwdcompare2 = jaroWinklerDistance.apply(altjwdStringBlender(aliases.toString()), altjwdStringBlender(series.toString()))
          if ( jwdResults[(myTVDBseriesInfo.id)] == null ) {
            Logging.log.finest "Adding to jwdResults because it is null for AID: ${myTVDBseriesInfo.id} - ${jwdResults[(myTVDBseriesInfo.id)]} - ${jwdResults[myTVDBseriesInfo.id]} - ${jwdResults["${myTVDBseriesInfo.id}"]}"
            hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, hasAnimeListEntry: hasAnimeListEntry]]
          } else if ( jwdcompare2 > jwdResults[(myTVDBseriesInfo.id)].score ) {
            Logging.log.finest "higher"
            hasAnimeListEntry = filebotAnimeListReturnFromTVDBID(myTVDBseriesInfo.id) != null
            jwdResults += [(myTVDBseriesInfo.id):[score: jwdcompare2, db:'TheTVDB', dbid:myTVDBseriesInfo.id, primarytitle: myTVDBseriesInfo.name, animename: series, matchname: aliases, alias: true, hasAnimeListEntry: hasAnimeListEntry]]
          } else {
            Logging.log.finest "lower"
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
@SuppressWarnings(['GroovyUnusedAssignment', 'unused'])
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
  String tempAnimeName
  String tempFileBotName
  Matcher mySanityRegexMatcher
  // Search AniDB for 1st, 2nd, 3rd match
  // Use Filebot AltTitle as fallback (since it's MovieDB)?
  // Rename (Add option to use Non-Strict on rename)
  // ---------- Basename Generation ---------- //
  Logging.log.info '// START---------- Basename Generation ---------- //'
  switch(group.anime) {
  // Close...
    case ~/eiyuu banku koushi-den/:
      tempAnimeName = jwdStringBlender('Eiyuu Banka Koushi-den')
      break
    default:
      tempAnimeName = jwdStringBlender(group.anime)
  }
  //  if ( group.altTitle != null ) {
  //    baseAnimeNames += ["${group.alttitle}"]
  //    baseAnimeNames += ["${ returnAniDBRomanization(group.alttitle) }"]
  //  }
//  Logging.log.info "--- TEMP: Adding group.anime - [${group.anime}]"
  //  tempBaseGeneratedAnimeNames = ["${group.anime}"]
  // group.filebotMovieTitle is a net.filebot.web.Movie object!
  tempFileBotName = group.filebotMovieTitle != null ? jwdStringBlender(group.filebotMovieTitle.toString()) : ""
  myExceptionMatcher = tempAnimeName =~ /(?i)(fate[ ]?stay night)/
  if ( myExceptionMatcher.find() ) {
    tempAnimeName = jwdStringBlender("Fate/Stay Night: Heaven`s Feel")
    Logging.log.info "//----- AniDB Entry is a multi-part movie -  ${tempAnimeName}"
  }
  Logging.log.info "----- TEMP: Adding jwdStringBlender(group.anime) - [${tempAnimeName}]"
  tempBaseGeneratedAnimeNames = ["${tempAnimeName}"]
  // Unfortunately there is at *least* one anime that officially ends with v3, so I can't just ignore the "version" and the end.
  // - https://anidb.net/anime/5078
  myRegexMatcher = group.anime =~ /${matchEndOfLineVersion}/
  if ( myRegexMatcher.find() ) {
    generatedAnimeName = group.anime.replaceAll(/${matchEndOfLineVersion}/, '')
    tempBaseGeneratedAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
    Logging.log.info "----- TEMP: End of Line Version - Adding to base Name List - [${generatedAnimeName}] "
  }
  // myMovieRegexMatcher = group.anime =~ /(?i)(\bmovie\s[\d]{1,3}|\bmovie)\b/
  Logging.log.info '--- Checking if we should add variations based on movie keyword'
  // VOID - Matcher myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}|\smovie))\b/
  Matcher myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/, '')
    Logging.log.info "----- TEMP: Adding 'movie' keyword variation #1 - [${animeTemp}]" // Removing The, Movie and any 3 digits after movie
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  // VOID - myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie)\s(?!\d))/
  myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie(?!\s\d)))/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(\s?(the)?(\smovie))/, '')
    Logging.log.info "----- TEMP: Adding 'movie' keyword variation #2 - [${animeTemp}]" // Removing The, Movie but leaving the digits
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  myMovieRegexMatcher = group.anime =~ /(?i)(\s?(the)?(\smovie\s[\d]{1,3}))\b/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)(?<=movie\s)([\d]{1,3})/, '')
    Logging.log.info "----- TEMP: Adding 'movie' keyword variation #3 - [${animeTemp}]" // Removing the digits after movie
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  myMovieRegexMatcher = group.anime =~ /(?i)(\s?(?<!the)(\smovie(?!\s\d)))/
  if ( myMovieRegexMatcher.find() ) {
    animeTemp = group.anime.replaceAll(/(?i)([\s]?movie[\s]?)/, ' the movie ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "----- TEMP: Adding 'movie' keyword variation #4 - [${animeTemp}]" // Adding 'the' before 'movie' (but only if it has movie)
    tempBaseGeneratedAnimeNames += ["${animeTemp}"]
  }
  tempBaseGeneratedAnimeNames.each { tempname ->
    Logging.log.info "--- BASE: Adding [${tempname}]"
    baseGeneratedAnimeNames += tempname
    baseGeneratedAnimeNames += ["${returnAniDBRomanization(group.anime)}"]
  }
  Logging.log.info '// END---------- Basename Generation ---------- //'
  Logging.log.info '// START---------- SeriesName Generation ---------- //'
  baseGeneratedAnimeNames.each { basename ->
    Logging.log.info "//--- Generating Possible Anime Series Names for ${basename}"
    Logging.log.info "--- Adding ${basename}"
    baseAnimeNames += ["${basename}"]
    Logging.log.info '--- Checking if we should add variations based on Gekijouban keyword'
    myMovieRegexMatcher = basename =~ /(?i)(Gekijouban)/
    if ( !myMovieRegexMatcher.find() ) {
      animeTemp = 'Gekijouban ' + basename
      Logging.log.info "----- Adding 'Gekijouban' keyword variation - [${animeTemp}]"
      baseAnimeNames += ["${animeTemp}"]
    }
    // Taken from groupGeneration
    // VOID - (?i)(~\s(.*))$
    mySanityRegexMatcher = basename =~ /(?i)(~(.*))$/
    if (mySanityRegexMatcher.find() ) {
      //noinspection GroovyAssignabilityCheck
      mySanityAltTxt = mySanityRegexMatcher[0][2]
      Logging.log.info "----- Adding possible Alternative Title: [${mySanityAltTxt}] using ~"
      baseAnimeNames += ["${mySanityAltTxt}"]
      animeTemp = basename.replaceAll(/(?i)(~(.*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      Logging.log.info "----- Adding possible Alternative Title: [${animeTemp}] using ~"
      baseAnimeNames += ["${animeTemp}"]
    }
    // (?i)(-\s(.*))$
    mySanityRegexMatcher = basename =~ /(?i)(-\s(.*))$/
    if (mySanityRegexMatcher.find() ) {
//      mySanityAltTxt = mySanityRegexMatcher[0][2]
      animeTemp = basename.replaceAll(/(?i)(-\s(.*))$/, '').replaceAll(/(\s){2,20}/, ' ').replaceAll(/([\s-])*$/, '')
      Logging.log.info "-----  Adding Title Text Variation [${animeTemp}] using -"
      baseAnimeNames += ["${animeTemp}"]
    }
  }
  if ( group.filebotMovieTitle != null ) {
    Logging.log.fine "group.filebotMovieTitle:[${group.filebotMovieTitle}]"
    myMovieRegexMatcher = group.filebotMovieTitle =~ /${stripYearDateRegex}/
    if ( myMovieRegexMatcher.find() ) {
      Logging.log.info "-----  Adding Filebot Title Text Variation [${group.filebotMovieTitle.toString().replaceAll(/${stripYearDateRegex}/, '')}]"
      filebotBaseAnimeNames = ["${group.filebotMovieTitle.toString().replaceAll(/${stripYearDateRegex}/, '')}"]
      Logging.log.info "-----  Adding Filebot Title Text Variation [${group.filebotMovieTitle}]"
      filebotBaseAnimeNames += ["${group.filebotMovieTitle}"]
    } else {
      Logging.log.info "-----  Adding Filebot Title Text Variation [${group.filebotMovieTitle}]"
      filebotBaseAnimeNames = ["${group.filebotMovieTitle}"]
    }
  }
  Logging.log.info '// END---------- Series Name Generation ---------- //'
  Logging.log.info '-----'
  Logging.log.info '-----'
  Logging.log.info "  We are going to be searching for these Anime Series Names: ${baseAnimeNames} with AniDB "
  if ( filebotBaseAnimeNames ) {
    Logging.log.info "  We are going to be searching for these Anime Series Names: ${filebotBaseAnimeNames} with AniDB from FileBot"
  }
  Logging.log.info '-----'
  Logging.log.info '-----'
  returnThing = filebotAnidbJWDSearch( baseAnimeNames, anidbJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  anidbJWDResults = returnThing.jwdresults
  animeFoundInAniDB = returnThing.animeFoundInAniDB
  if ( group.filebotMovieTitle != null ) {
    returnThing2 = filebotAnidbJWDSearch( filebotBaseAnimeNames, fileBotAniDBJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
    fileBotAniDBJWDResults = returnThing2.jwdresults
    animeFoundInAniDB = returnThing2.animeFoundInAniDB
  }
  if (animeFoundInAniDB) {
    Logging.log.finest "animeFoundInAniDB:[${animeFoundInAniDB}]"
    Logging.log.finest "anidbJWDResults:[${anidbJWDResults}]"
    Logging.log.finest "fileBotAniDBJWDResults:[${fileBotAniDBJWDResults}]"
    filteredanidbJWDResults = anidbJWDResults.findAll { results ->
      aodIsAIDTypeNotTV(animeOfflineDatabase, results.value.dbid as Integer)
    }
    filteredfileBotAniDBJWDResults = fileBotAniDBJWDResults.findAll { results ->
      aodIsAIDTypeNotTV(animeOfflineDatabase, results.value.dbid as Integer)
    }
    if ( filteredanidbJWDResults.isEmpty() && filteredfileBotAniDBJWDResults.isEmpty() ) {
      animeFoundInAniDB = false
    }
    Logging.log.finest "animeFoundInAniDB:[${animeFoundInAniDB}]"
    Logging.log.finest "filteredanidbJWDResults:[${filteredanidbJWDResults}]"
    Logging.log.finest "filteredfileBotAniDBJWDResults:[${filteredfileBotAniDBJWDResults}]"
  }
  if (animeFoundInAniDB) {
    anidbFirstMatchDetails = filteredanidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } as LinkedHashMap
    if ( anidbFirstMatchDetails == null ) {
      statsANIDBJWDFilebotOnly++
      Logging.log.info "//--- ONLY Filebot Anime Name Matched something in ANIDB ---///"
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
      Logging.log.info "//---- Switch 1st/2nd AniDB"
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
    Logging.log.info "firstANIDBWTMatchNumber: ${firstANIDBWTMatchNumber}"
    Logging.log.info "firstAniDBWTMatchName: ${firstAniDBWTMatchName}"
    Logging.log.info "anidbFirstMatchDetails: ${anidbFirstMatchDetails}"
    Logging.log.info "secondANIDBWTMatchNumber: ${secondANIDBWTMatchNumber}"
    Logging.log.info "secondAniDBWTMatchName: ${secondAniDBWTMatchName}"
    Logging.log.info "anidbSecondMatchDetails: ${anidbSecondMatchDetails}"
    Logging.log.info "thirdANIDBWTMatchNumber: ${thirdANIDBWTMatchNumber}"
    Logging.log.info "thirdAniDBWTMatchName: ${thirdAniDBWTMatchName}"
    Logging.log.info "anidbThirdMatchDetails: ${anidbThirdMatchDetails}"
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
        Logging.log.info "fileBotANIDBJWDMatchNumber: ${fileBotANIDBJWDMatchNumber}"
        Logging.log.info "fileBotANIDBJWDMatchDetails: ${fileBotANIDBJWDMatchDetails}"
      }
    }
  } else {
    Logging.log.info '//-----------------------------------------//'
    Logging.log.info "Nothing was found for ${group.anime} in AniDB"
    Logging.log.info '//-----------------------------------------//'
    firstANIDBWTMatchNumber = 0
  }
  return [firstANIDBWTMatchNumber: firstANIDBWTMatchNumber, firstAniDBWTMatchName:firstAniDBWTMatchName, anidbFirstMatchDetails: anidbFirstMatchDetails, secondANIDBWTMatchNumber: secondANIDBWTMatchNumber, secondAniDBWTMatchName:secondAniDBWTMatchName, anidbSecondMatchDetails: anidbSecondMatchDetails, thirdANIDBWTMatchNumber: thirdANIDBWTMatchNumber, thirdAniDBWTMatchName:thirdAniDBWTMatchName, anidbThirdMatchDetails:anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails: fileBotANIDBJWDMatchDetails, fileBotANIDBJWDMatchNumber: fileBotANIDBJWDMatchNumber, animeFoundInAniDB: animeFoundInAniDB, statsANIDBJWDFilebotOnly:statsANIDBJWDFilebotOnly, statsANIDBFilebotMatchedScript:statsANIDBFilebotMatchedScript]
}


@SuppressWarnings(['GroovyUnusedAssignment', 'unused'])
def filebotMovieFallBack(LinkedHashMap group, def animeListsXML) {
  // ---------- Set Variables ---------- //
  // Doesn't seem to be null, but can be -1 and needs to be padded..
  Integer myIMDBId = group.filebotMovieTitle.getImdbId()
  Logging.log.info "myIMDBId:${myIMDBId}"
  Integer myTMDBId = group.filebotMovieTitle.getTmdbId()
  Logging.log.info "myTMDBId:${myTMDBId}"
  def myAniDBId // Can be null
  if (myIMDBId > 0) {
    myAniDBId = animeListXMLGetAniDBFromIMDBID(animeListsXML, myIMDBId)
    Logging.log.finer "ImDBID 2 AniDBID:${myAniDBId}"
  }
  if (myAniDBId == null && myTMDBId > 0) {
    myAniDBId = animeListXMLGetAniDBFromTMDBID(animeListsXML, myTMDBId)
    Logging.log.finer "TMDBID 2 ANIDBID:${myAniDBId}"
  }
  Logging.log.info "AniDBID:${myAniDBId}"
  return myAniDBId
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByAnimeLists(ArrayList<File> input, String preferredDB, LinkedHashMap anidbMatchDetails, LinkedHashMap tvdbMatchDetails, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnTVDBSpecials){
  LinkedHashMap groupsByEpisode
  Integer processPass = 1
  groupsByEpisode = input.groupBy { File f ->
    Logging.log.finest "// FILE:${f}"
    return renameOptionsForEpisodesUsingAnimeLists(f, preferredDB, anidbMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials )
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByTVDB( ArrayList<File> input, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnTVDBSpecials) {
  LinkedHashMap groupsByEpisode
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: '', animename: '', matchname: '', alias: false]
  switch (group.order) {
    case ['airdate','Airdate']:
      Logging.log.info "------- Using airdate Ordering"
      groupsByEpisode = input.groupBy { File f ->
        Logging.log.finest "// FILE:${f}"
        return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials)
      }
      break
    case ['absolute','Absolute']:
      Logging.log.info "------- Using absolute Odering"
      groupsByEpisode = input.groupBy { File f ->
        Logging.log.finest "// FILE:${f}"
        return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
      }
      break
    default:
      Logging.log.info "------- Using airdate Ordering"
      groupsByEpisode = input.groupBy { File f ->
        Logging.log.finest "// FILE:${f}"
        return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials )
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return groupsByEpisode (input.GroupBy)
 */
LinkedHashMap groupGenerationByAniDB( ArrayList<File> input, LinkedHashMap anidbMatchDetails, Integer renamePass, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, BigDecimal aniDBJWDMatchNumber, JsonObject animeOffLineDatabaseJsonObject, Integer tvdbID, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnTVDBSpecials) {
  def groupsByEpisode = input.groupBy { File f ->
    Logging.log.finest "// FILE:${f}"
    return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, aniDBJWDMatchNumber, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionsForEpisodesUsingAnimeLists(File f, String preferredDB, LinkedHashMap anidbMatchDetails, LinkedHashMap tvdbMatchDetails, Integer renamePass, Integer processPass, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnTVDBSpecials ){
  /*  Determine rename Options using AnimeLists
  PreferredDB is used to determine if AniDB or TVDB ID is used when there is an invalid/no AnimeList Entry/Match (Assuming there IS an AniDB and TVDB ID)
  Different options for Airdate vs Absolute Ordering
  Different options if Episode # denotes a "Special" aka Episode 0, #.# (5.5, 6.5 etc) - TVDB Specials frequently do not map to AniDB, so  since we want AniDB metadata ..
    - Use AniDB for Special Episodes
  Using renamePass for Determining options based on # of Rename passes
  Using processPass for Determining how many times renameOptionsForEpisodesUsingAnimeLists has been called (try to avoid infinite processing loops)
    --> processPass = 1 allow passing tvdbID/anidbID when calling other rename options
    --> processPass = 2+ do not pass tvdbID/anidbID when calling other rename options (or don't call renameOptionsForEpisodesUsingAnimeLists?)*/
  Logging.log.info "//----- renameOptionsForEpisodesUsingAnimeLists"
  // Setup local variables for easy references
  performRename = false
  //noinspection GroovyUnusedAssignment
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode == true
  Boolean isMovieType = group.isMovieType
  Boolean isSpecialType = group.isSpecialType
  //noinspection GroovyUnusedAssignment
  String specialType = group.specialType
  Integer anidbID = anidbMatchDetails.dbid as Integer
  Integer tvdbID = tvdbMatchDetails.dbid as Integer
  // Setup local variables to script scope
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
//  Integer myEpisodeSeason
//  def myAnimeListMapping
//  String myanimeListGetTVDBSeason
//  Collection<Episode> myTVDBSeriespisodes
//  Episode doesItContainEpisode
  Boolean preferAniDB
  Boolean absoluteOrdering
  switch (preferredDB) {
    case 'anidb':
      preferAniDB = true
      Logging.log.info "------- We prefer database:[anidb]"
      break
    case 'tvdb':
      preferAniDB = false
      Logging.log.info "------- We prefer database:[TheTVDB]"
      break
    default:
      Logging.log.info "------- We prefer database:[anidb]"
      preferAniDB = true
      break
  }
  switch (group.order) {
    case 'airdate':
      absoluteOrdering = false
      Logging.log.info "------- Using airdate Ordering"
      break
    case 'absolute':
      absoluteOrdering = true
      Logging.log.info "------- Using absolute Odering"
      break
    default:
      Logging.log.info "------- Using absolute Odering"
      absoluteOrdering = true
      break
  }
  // Detect the episode number we are working with
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false, true)
  Logging.log.info "------- We have AniDB ID:[${anidbID}] and TVDB ID:[${tvdbID}]"
  Logging.log.info "--------- Consult AnimeList for AniDB ID:[${anidbID}] with TVDB ID:[${tvdbID}]"
  def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)

  // It can be an Integer if the AniDB maps to a series
  // Multi-episode titles not found on theTVDB.com are marked as "unknown"
  // One-off titles that won't ever be added to theTVDB.com (movies, TV specials, one-shot OVAs) are marked by their AniDb.net type
  // Pornographic titles are marked by "hentai" regardless of episode count as they will never appear on theTVDB.com.
  // It can be an Integer if the AniDB maps to a series
  // Multi-episode titles not found on theTVDB.com are marked as "unknown"
  // One-off titles that won't ever be added to theTVDB.com (movies, TV specials, one-shot OVAs) are marked by their AniDb.net type
  // Pornographic titles are marked by "hentai" regardless of episode count as they will never appear on theTVDB.com.
  Logging.log.info "------- We have got myanimeListGetTVDBID:[${myanimeListGetTVDBID}] from AnimeList"

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
    Logging.log.info "--------- There is no AnimeList MAP for AniDB ID:[${anidbID}]"
    switch (renamePass){
      case 1:
        if ( preferAniDB ) {
          //    preferAniDB: True
          //      1: - tvdbID > 0
          //           Y: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass,tvdbid
          //           N: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, no tvdbid
          if ( tvdbID > 0 ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID of ${tvdbID}, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbMatchDetails.dbid as Integer, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          }
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
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
                Logging.log.info "----------- Send to renameOptionsForEpisodesUsingAnimeLists with AniDB:[${returnThing.anidbid}], tvdbID:[${tvdbID}], renamePass:[${renamePass}], Prefer AniDB"
                LinkedHashMap JWDMatchDetails = [score: 1, db:'anidb', dbid: "${returnThing.anidbid}", primarytitle: "${returnThing.name}", animename: "${returnThing.name}", matchname: "${returnThing.name}", alias: false]
                return renameOptionsForEpisodesUsingAnimeLists(f, 'anidb', JWDMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials )
              }
            }
          }
          if ( absoluteOrdering ) {
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails as Boolean, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials)
          }
        }
        break
      case 2:
        if ( preferAniDB ) {
          //    preferAniDB: True
          //      2: - Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, no AniDB
          //      2: - Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, no AniDB
          if ( absoluteOrdering ) {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[0]"
            return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[0]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials)
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
                Logging.log.info "----------- Send to renameOptionsForEpisodesUsingAnimeLists with AniDB:[${returnThing.anidbid}], tvdbID:[${tvdbID}], renamePass:[${renamePass}], Prefer tvdb"
                LinkedHashMap JWDMatchDetails = [score: 1, db:'anidb', dbid: "${returnThing.anidbid}", primarytitle: "${returnThing.name}", animename: "${returnThing.name}", matchname: "${returnThing.name}", alias: false]
                return renameOptionsForEpisodesUsingAnimeLists(f, 'tvdb', JWDMatchDetails, tvdbMatchDetails, renamePass, processPass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials )
              }
            }
          }
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info '--------- AnimeList AniDB to TVDB ID mapping found and matched.'
    if ( myEpisodeNumber == null ) {
      //   Episode # is null?
      //      1: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass, tvdbID
      //      2: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
      //      2: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
      Logging.log.info '----------- Episode # detected as null'
      switch (renamePass) {
        case 1:
          Logging.log.info "------------- Checking AniDB ${myanimeListGetTVDBID} - with tvdbID:[${tvdbID}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        case 2:
          if ( absoluteOrdering ) {
            Logging.log.info "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:{${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:{${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special episode (Dot Syntax)"
      //   Episode # has a dot in it (aka 5.5, 6.5 etc known as a special episode)
      //      1: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass, AniDB
      //      1: Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass, AniDB
      //      2: Send to renameOptionForAniDBAbsoluteEpisodes, renamePass:1, tvdbID
//      group.isSpecialEpisode = true
      switch (renamePass) {
        case 1:
          if ( absoluteOrdering) {
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[0], tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
          break
        case 2:
          Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
            Logging.log.info "----------- Special Type Detected: Send to AniDB with ${anidbID}, tvdbID, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          }
          Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
          return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          break
        case 2:
          if ( isSpecialType ) {
            Logging.log.info "----------- Special Type Detected: Send to AniDB with ${anidbID}, tvdbID, renamePass:[${renamePass}]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          }
          Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID:[${tvdbID}], renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
          Logging.log.info "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[${anidbID}], tvdbID:[${tvdbID}], renamePass:[${renamePass}]"
          return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          break
        case 2:
          Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID:[${tvdbID}], renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
    Logging.log.info "--------- Mapping didn't match, returned TVDBID: ${myanimeListGetTVDBID}"
    switch (renamePass){
      case 1:
        if ( preferAniDB ) {
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        }
        if ( absoluteOrdering ) {
          Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
          return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails ,tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
        } else {
          Logging.log.info "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[0], tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
          return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials)
        }
        break
      case 2:
        if ( preferAniDB ) {
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[${renamePass}]"
//          log.finest "----------- Reset isSpecialEpisode"
//          group.isSpecialEpisode = false
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        }
        Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
//        log.finest "----------- Reset isSpecialEpisode"
//        group.isSpecialEpisode = false
        return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
//        if ( absoluteOrdering ) {
//          log.finest "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
//          return renameOptionForTVDBAbsoluteEpisodes(f, emptyJWDMatchDetails ,tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
//        } else {
//          log.finest "----------- Send to renameOptionForTVDBAirdateEpisodes with anidb:[0], tvdbID:[${tvdbID}], renamePass:[${renamePass}], anidb:[0]"
//          return renameOptionForTVDBAirdateEpisodes(f, tvdbID, renamePass, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, emptyJWDMatchDetails, useNonStrictOnTVDBSpecials)
//        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForTVDBAbsoluteEpisodes(File f, LinkedHashMap anidbMatchDetails, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, Boolean useNonStrictOnTVDBSpecials, Boolean processAsSpecial = false) {
  Logging.log.info "//----- renameOptionForTVDBAbsoluteEpisodes"
  performRename = false
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode || processAsSpecial
  Boolean isMovieType = group.isMovieType || anidbMatchDetails.isMovieType
  Boolean isSpecialType = group.isSpecialType || anidbMatchDetails.isSpecialType
  Integer myEpisodeSeason = null
  def myAnimeListMapping
  Boolean hasAnimeListMapping = null
  String myanimeListGetTVDBSeason = 'n'
  Collection<Episode> myTVDBSeriespisodes = filebotTVDBgetEpisodeList(tvdbID)
  Episode doesItContainEpisode
  Integer anidbID = anidbMatchDetails.dbid as Integer
  // Detect the episode number we are working with
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false, true)
  Logging.log.info "------- We have TVDB ID:[${tvdbID}]"
  Boolean doesItHaveAbsoluteNumbering = myTVDBSeriespisodes.findAll { it.absolute }.size() != 0
  if ( anidbID > 0) {
    // Not all Mappings include a default Season, so filebotAnimeListReturnFromAID will return n if there is no mapping (vs null)
    myAnimeListMapping = filebotAnimeListReturnFromAID(anidbID, false, false) // Will return null if there is no Mapping
    hasAnimeListMapping = myAnimeListMapping != null
    Logging.log.info "--------- myAnimeListMapping ${myAnimeListMapping}"
    Logging.log.info "--------- hasAnimeListMapping ${hasAnimeListMapping}"
    myanimeListGetTVDBSeason = filebotAnimeListReturnFromAID(anidbID, false, true) // Will return null if there is no Mapping
    // defaulttvdbseason - The corresponding theTVDB.com season.
    // For one-off titles it will be 1 unless associated to a multi-episode series, in which case it will be 0.
    // Series that span multiple seasons on theTVDB.com may be marked as a if the absolute episode numbering is defined and matches AniDb.net.
    // If there is NO entry it will return n
    Logging.log.info "--------- myanimeListGetTVDBSeason ${myanimeListGetTVDBSeason}"
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
    Logging.log.info '----------- Episode # detected as null'
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
        renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
        renameStrict = true
//        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        renameFilter = ''
        if ( myanimeListGetTVDBSeason == 'n' || myanimeListGetTVDBSeason == 'a' ) {
          Logging.log.info '--------- Animelist Mapping indicates no default TVDB Season or Animelist Mapping indicates Absolute Ordering in TVDB'
          renameFilter = ''
        }
        if ( myanimeListGetTVDBSeason.isNumber() ) {
          if ( myanimeListGetTVDBSeason.toInteger() == 0 ) {
            Logging.log.info "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB"
            isSpecialEpisode = true
          } else {
            if ( !hasSeasonality && !isSpecialEpisode) {
              Logging.log.info "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${myanimeListGetTVDBSeason}"
              renameStrict = true
            }
          }
        }
        if (isSpecialEpisode ) {
          Logging.log.info "--------- Specials however use filter of episode.special"
          renameFilter = "episode.special"
          if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        if ( hasSeasonality && !isSpecialEpisode) {
          Logging.log.info "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
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
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        }
        if ( hasSeasonality ) {
          myEpisodeNumber = 0 // Else the checks following will blow up
          renameOptionsSet = true
          renameQuery = tvdbID
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = '[AnimeList.AniDB, episode, order.absolute.episode]'
          renameFilter = ''
          if ( myanimeListGetTVDBSeason == 'n' || myanimeListGetTVDBSeason == 'a' ) {
            Logging.log.info '--------- Animelist Mapping indicates no default TVDB Season or Animelist Mapping indicates Absolute Ordering in TVDB'
            renameFilter = ''
          }
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( myanimeListGetTVDBSeason.toInteger() == 0 ) {
              Logging.log.info "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB"
              isSpecialEpisode = true
            } else {
              if ( !isSpecialEpisode) {
                Logging.log.info "----- Using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
                renameFilter = "s == ${myanimeListGetTVDBSeason}"
                renameStrict = true
              }
            }
          }
          if (isSpecialEpisode ) {
            Logging.log.info "--------- Specials however use filter of episode.special"
            renameFilter = "episode.special"
            if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              Logging.log.info '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
        } else {
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
  if ( !renameOptionsSet && (myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ || myEpisodeNumber.toInteger() == 0)) {
    Logging.log.info "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special"
    //    1: Rename using TVDB
    //    2:  AniDB?
    //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
    //        N: Stop
    switch (renamePass) {
      case 1:
        if ( anidbID ) {
          Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbid:[${tvdbID}], renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        } else {
          Logging.log.info "----------- Use TVDB"
          isSpecialEpisode = true
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "episode.special"
          if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      case 2:
        if ( anidbID ) {
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        } else {
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
        Logging.log.info "----------- Detected TVDB Absolute Episode Does not have a Season[${doesItContainEpisode.season}], Perhaps a Special?"
        myEpisodeSeason = 0
      } else {
        Logging.log.info "----------- Detected TVDB Absolute Episode Season ${doesItContainEpisode.season}"
        myEpisodeSeason =  doesItContainEpisode.season
      }
    } else {
      Logging.log.info "----------- Detected TVDB Absolute Episode Number:[${myEpisodeNumber}] does not seem to be in TVDB Absolute Episode List:[${myTVDBSeriespisodes.findAll { it.absolute != null  }.size()}]"
      if ( anidbID > 0 ) {
        if ( myanimeListGetTVDBSeason != null ) {
          Logging.log.info "--------- Checking if we can determine if Episode Number:[${myEpisodeNumber}] is a special for Season ${myanimeListGetTVDBSeason}"
        } else {
          Logging.log.info "----------- We have AniDBID:[${anidbID}], Checking if we can determine if Episode Number:[${myEpisodeNumber}] is a special"
        }
        def myAniDBEpisodeCount = aniDBGetEpisodeNumberForAID(animeOffLineDatabaseJsonObject, anidbID)
        if (myEpisodeNumber.toInteger() <= myAniDBEpisodeCount ) {
          Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is within AniDB Episode Count range:[${myAniDBEpisodeCount}] for AniDBID:[${anidbID}]"
          Logging.log.info "----------- Perhaps this is due to differences in how each service counts episodes?"
          Logging.log.info "----------- Set no options to allow for futher processing.."
        }
        if (myEpisodeNumber.toInteger() > myAniDBEpisodeCount ) {
          Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is GREATER then Regular AniDB Episode Count range:[${myAniDBEpisodeCount}]"
          def myAniDBEpisodeCountWithSpecials = filebotAniDBEpisodeCount(filebotAniDBgetEpisodeList(anidbID, 'include'))
          if ( myEpisodeNumber.toInteger() > myAniDBEpisodeCountWithSpecials ) {
            Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
            Logging.log.info "----------- Likely TVDB Absolute Ordered"
            Logging.log.info "----------- Set no options to allow for futher processing.."
          } else {
            Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is less then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
            Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is probably a special"
            //    1: Rename using TVDB
            //    2:  AniDB?
            //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
            //        N: Stop
            switch (renamePass) {
              case 1:
                Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID:[${tvdbID}], renamePass:[1]"
                return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
//                isSpecialEpisode = true
//                renameQuery = tvdbID
//                renameOptionsSet = true
//                performRename = true
//                renameDB = 'TheTVDB'
//                renameOrder = 'Airdate'
//                renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
//                renameFilter = "episode.special"
//                if ( (useNonStrictOnTVDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
//                  renameStrict = false
//                  Logging.log.info '------------- Set non-Strict renaming'
//                } else {
//                  renameStrict = true
//                }
                break
              case 2:
                Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
                return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
                break
              default:
                Logging.log.info '//-----------------------------//'
                Logging.log.info '//            STOP             //'
                Logging.log.info '//-----------------------------//'
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
        Logging.log.info "----------- Allow for further processing.."
      }
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber.toInteger() > 99) {
    //    1: Rename using TVDB
    //    2:  AniDB?
    //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
    //        N: Stop
    Logging.log.info '----------- 3-Digit Episode detected, use Episode Filtering'
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
        if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnTVDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          Logging.log.info '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      case 2:
        if ( anidbID ) {
          Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        } else {
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
      Logging.log.info '--------- Animelist Mapping indicates no default TVDB Season'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = ""
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              Logging.log.info "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${myanimeListGetTVDBSeason}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            Logging.log.info "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            Logging.log.info "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score  == 1 ) || (useNonStrictOnTVDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info '--------- Animelist Mapping indicates Absolute Ordering in TVDB'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              Logging.log.info "----- No Seasonality Detected, using myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}] as a Season Filter"
              renameFilter = "s == ${mySeasonalityNumber}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            Logging.log.info "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            Logging.log.info "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}], But AnimeListSeason:[${myanimeListGetTVDBSeason}] indicates a Season "
      Logging.log.info "--------- Possible Special for that Season (TVDB Specials usually don't map to AniDB)"
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      switch (renamePass) {
        case 1:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, tvdbID:[0], renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "--------- Use TVDB"
            isSpecialEpisode = true
            renameQuery = tvdbID
            renameOptionsSet = true
            performRename = true
            renameDB = 'TheTVDB'
            renameOrder = 'Airdate'
            renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//            renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, episode, AnimeList.AniDB]'
            renameFilter = "episode.special"
            if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              Logging.log.info '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Animelist Mapping indicates AID:[${anidbID}] maps to Specials in TVDB or myEpisodeSeason:[${myEpisodeSeason}] does"
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
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "episode.special"
          if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] matches myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}]"
//      if (hasSeasonality) {
//        if ( myEpisodeSeason < mySeasonalityNumber ) {
//          Logging.log.info "----------- Episode Season:[${myEpisodeSeason}] less then mySeasonalityNumber:[${mySeasonalityNumber}]"
          if ( myAnimeListMapping.episodeoffset != null ) {
            Logging.log.info "----------- myAnimeListMapping indicates an Episode Offset"
            if ( myEpisodeNumber.toInteger() <= myAnimeListMapping.episodeoffset.toInteger() ) {
              renameOptionsSet = true
              Logging.log.info "------------- Probably Normal AniDB Absolute Ordering, which will likely not match correctly using TVDB"
              Logging.log.info "------------- Send to renameOptionForAniDBAbsoluteEpisodes - anidbID:[${anidbID}], renamePass:[${renamePass}], tvdbID:[${tvdbID}]"
              return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            }
          }
//        }
//      }
      switch (renamePass) {
        case 1:
          // Possibly TVDB Absolute Ordering with the same Season as the map (Why didn't it already match?)
          // Unfortunately you can't use Season filter when order = Absolute with TVDB
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "s == ${myEpisodeSeason}"
          renameStrict = true
          break
        case 2:
          renameOptionsSet = true
          Logging.log.info "------------- Checking AniDB ${myanimeListGetTVDBID} - no tvdbID"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] < myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}]"
      // Possibly NOT TVDB Absolute Ordering. So it's either Relative Season absolute or Normal AniDB Absolute or wrong AID or just a special..
      if ( myAnimeListMapping.episodeoffset != null ) {
        Logging.log.info "----------- myAnimeListMapping indicates an Episode Offset"
        if ( myEpisodeNumber.toInteger() <= myAnimeListMapping.episodeoffset.toInteger() ) {
          // Possibly normal AniDB Absolute Ordering. (Why didn't it already match?)
          renameOptionsSet = true
          Logging.log.info "------------- Probably Normal AniDB Absolute Ordering, which will likely not match correctly using TVDB"
          Logging.log.info "------------- Send to renameOptionForAniDBAbsoluteEpisodes - anidbID:[${anidbID}], renamePass:[1], tvdbID:[${tvdbID}]"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, tvdbID, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        }
      } else {
        Logging.log.info "--------- No Episode Offset for Animelist Season"
        if ( isSpecialEpisode ) {
          Logging.log.info "----------- Because it is a Special (perhaps AniDB Absolute Ordered Special)"
          switch (renamePass) {
            case 1:
              renameQuery = tvdbID
              renameOptionsSet = true
              performRename = true
              renameDB = 'TheTVDB'
              renameOrder = 'Airdate'
              renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//              renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
              renameFilter = "episode.special"
              if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
                renameStrict = false
                Logging.log.info '------------- Set non-Strict renaming'
              } else {
                renameStrict = true
              }
              break
            case 2:
              if ( anidbID > 0 ) {
                Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
                return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
              } else {
                Logging.log.info '//-----------------------------//'
                Logging.log.info '//            STOP             //'
                Logging.log.info '//-----------------------------//'
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
              Logging.log.info '//-----------------------------//'
              Logging.log.info '//            STOP             //'
              Logging.log.info '//-----------------------------//'
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
            renameFilter = hasSeasonality ? "s == ${mySeasonalityNumber}" : "s == ${myEpisodeSeason}"
            break
          default:
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] > myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}]"
      //    Possibilities:
      //    1. TVDB Absolute Ordered Episode, which is no longer in the same "season" as the AniDB Series
      //    2.
      //    1: Rename using TVDB
      //    2:  AniDB?
      //        Y: Send to renameOptionForAniDBAbsoluteEpisodes, anidbID, renamePass:1, no tvdbID
      //        N: Stop
      Logging.log.info '--------- Animelist Mapping indicates Absolute Ordering in TVDB'
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !hasSeasonality && !isSpecialEpisode) {
              Logging.log.info "----- No Seasonality Detected, using myEpisodeSeason:[${myEpisodeSeason}] as a Season Filter"
              renameFilter = "s == ${myEpisodeSeason}"
              renameStrict = true
            }
          }
          if (isSpecialEpisode ) {
            Logging.log.info "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( hasSeasonality && !isSpecialEpisode) {
            Logging.log.info "----- Seasonality Detected, using ${mySeasonalityNumber} as a Season Filter"
            renameFilter = "s == ${mySeasonalityNumber}"
          }
          if ( (useNonStrictOnAniDBFullMatch && anidbMatchDetails.score == 1 ) || (useNonStrictOnTVDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] matches mySeasonalityNumber:[${mySeasonalityNumber}]"
      switch (renamePass) {
        case 1:
          // Possibly TVDB Absolute Ordering with the same Season as the map (Why didn't it already match?)
          // Unfortunately you can't use Season filter when order = Absolute with TVDB
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          renameFilter = "s == ${myEpisodeSeason}"
          renameStrict = true
          break
        case 2:
          renameOptionsSet = true
          Logging.log.info "------------- Checking AniDB ${myanimeListGetTVDBID} - no tvdbID"
          return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
//      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] < myanimeListGetTVDBSeason:[${myanimeListGetTVDBSeason}]"
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] < mySeasonalityNumber:[${mySeasonalityNumber}]"
      renameQuery = tvdbID
      renameOptionsSet = true
      performRename = true
      renameDB = 'TheTVDB'
      renameOrder = 'airdate'
      switch (renamePass) {
        case 1:
          renameMapper = 'order.absolute.episode.derive(e)'
//          renameFilter =  "s == ${myanimeListGetTVDBSeason}"
          renameFilter =  "s == ${mySeasonalityNumber}"
          break
        case 2:
          renameMapper = 'order.absolute.episode'
          renameFilter = "s == ${myEpisodeSeason}"
          break
        default:
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] > mySeasonalityNumber:[${mySeasonalityNumber}]"
      switch (renamePass) {
        case 1:
          renameQuery = tvdbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'TheTVDB'
          renameOrder = 'Airdate'
          renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//          renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
          if ( myanimeListGetTVDBSeason.isNumber() ) {
            if ( !isSpecialEpisode) {
              Logging.log.info "----------- using myEpisodeSeason:[${myEpisodeSeason}] as a Season Filter"
              renameFilter = "s == ${myEpisodeSeason}"
            }
          }
          if (isSpecialEpisode ) {
            Logging.log.info "--------- Specials Episodes however use filter of episode.special"
            renameFilter = "episode.special"
          }
          if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( anidbID ) {
            Logging.log.info "----------- Send to AniDB with ${anidbID}, No tvdbID, renamePass:[1]"
            return renameOptionForAniDBAbsoluteEpisodes(f, anidbMatchDetails, 1, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, anidbMatchDetails.score as BigDecimal, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
    Logging.log.info "--------- Fall Thru TVDB Options - myEpisodeSeason:[${myEpisodeSeason}]: renamePass:[${renamePass}]"
    switch (renamePass) {
      case 1:
        renameQuery = tvdbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'TheTVDB'
        renameOrder = 'Airdate'
        renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        if (isSpecialEpisode ) {
          Logging.log.info "--------- Specials Episodes however use filter of episode.special"
          renameFilter = "episode.special"
        } else {
          renameFilter = myEpisodeSeason == null ? '' : "s == ${myEpisodeSeason}"
        }
        if ( (useNonStrictOnTVDBSpecials && ( isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          Logging.log.info '------------- Set non-Strict renaming'
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
        renameMapper = renameMapperGenerator(group.order as String, 'tvdb' , doesItHaveAbsoluteNumbering, hasAnimeListMapping)
//        renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
        if (isSpecialEpisode ) {
          Logging.log.info "--------- Specials Episodes however use filter of episode.special"
          renameFilter = "episode.special"
        } else {
          renameFilter = myEpisodeSeason == null ? '' : "s == ${myEpisodeSeason}"
        }
        if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          Logging.log.info '------------- Set non-Strict renaming'
        } else {
          renameStrict = true
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForTVDBAirdateEpisodes(File f, Integer tvdbID, Integer renamePass, JsonObject animeOffLineDatabaseJsonObject, LinkedHashMap group, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap anidbMatchDetails, Boolean useNonStrictOnTVDBSpecials, Boolean processAsSpecial = false) {
  Logging.log.info "------- renameOptionForTVDBAirdateEpisodes"
  performRename = false
  Boolean renameOptionsSet = false
  Boolean isSpecialEpisode = group.isSpecialEpisode || processAsSpecial
  Boolean isMovieType = group.isMovieType
  Boolean isSpecialType = group.isSpecialType
  Integer myEpisodeSeason = null
  Collection<Episode> myTVDBSeriespisodes
  Episode doesItContainEpisode
  // Detect the episode number we are working with
  LinkedHashMap returnThing = detectEpisodeNumberFromFile(f, false, true, true, false)
  String myEpisodeNumber = returnThing.myDetectedEpisodeNumber
  String mySeasonNumber = returnThing.myDetectedSeasonNumber
  Integer anidbID = anidbMatchDetails.dbid as Integer
  Logging.log.info "------- We have TVDB ID:[${tvdbID}] and AniDB ID:[${anidbID}]"
  if ( myEpisodeNumber == null ) {
    Logging.log.info '----------- But we could not detect the episode number'
    myEpisodeNumber = 0 // Else the checks following will blow up
    renameOptionsSet = true
    renameQuery = tvdbID
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = ''
    if (isSpecialEpisode || mySeasonalityNumber == 0) {
      Logging.log.info "--------- Specials however use filter of episode.special"
      renameFilter = "episode.special"
    } else {
      Logging.log.info "--------- using ${mySeasonalityNumber} as a Season Filter"
      renameFilter = "s == ${mySeasonalityNumber}"
    }
    if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      Logging.log.info '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && (myEpisodeNumber =~ /\d{1,3}\.\d{1,3}/ || !myEpisodeNumber.isNumber()) ) {
    Logging.log.info "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special (dot Syntax)"
    isSpecialEpisode = true
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "episode.special"
    if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      Logging.log.info '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && myEpisodeNumber.toInteger() > 99) {
    Logging.log.info '----------- 3-Digit Episode detected..'
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
      Logging.log.info "----------- Detected Episode Season ${doesItContainEpisode.season}"
      myEpisodeSeason =  doesItContainEpisode.season
    } else {
      Integer mySeasonNumberofEpisodes = filebotTVDBSeasonEpisodes(myTVDBSeriespisodes, mySeasonalityNumber).size()
      if (mySeasonNumberofEpisodes == 0) {
        Logging.log.info "----------- Invalid Season:[${mySeasonalityNumber}] for Anime."
        isSpecialEpisode = false
        renameFilter = ""
      } else {
        Logging.log.info "----------- Detected Episode Number:[${myEpisodeNumber}] does not seem to be in TVDB Episode List:[${mySeasonNumberofEpisodes}] for Season ${mySeasonalityNumber}"
        Logging.log.info '----------- Perhaps it is a Special?'
        isSpecialEpisode = true
        renameFilter = "episode.special"
      }
      renameQuery = tvdbID
      renameOptionsSet = true
      performRename = true
      renameDB = 'TheTVDB'
      renameOrder = 'Airdate'
      renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
      if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
        renameStrict = false
        Logging.log.info '------------- Set non-Strict renaming'
      } else {
        renameStrict = true
      }
    }
  }
  if ( !renameOptionsSet && myEpisodeSeason == 0) {
    Logging.log.info "--------- myEpisodeSeason lookup indicates episode:[${myEpisodeNumber}] maps to Specials in TVDB"
    isSpecialEpisode = true
    renameQuery = tvdbID
    renameOptionsSet = true
    performRename = true
    renameDB = 'TheTVDB'
    renameOrder = 'Airdate'
    renameMapper = group.order == 'airdate' ? '[episode, AnimeList.AniDB]' : '[order.absolute.episode, AnimeList.AniDB, episode]'
    renameFilter = "episode.special"
    if ( (useNonStrictOnTVDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
      renameStrict = false
      Logging.log.info '------------- Set non-Strict renaming'
    } else {
      renameStrict = true
    }
  }
  if ( !renameOptionsSet && myEpisodeSeason == mySeasonalityNumber ) {
    Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] matches mySeasonalityNumber:[${mySeasonalityNumber}]"
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
    Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] < mySeasonalityNumber:[${mySeasonalityNumber}]"
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
    Logging.log.info "--------- Episode Season:[${myEpisodeSeason}] > mySeasonalityNumber:[${mySeasonalityNumber}]"
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
    Logging.log.info "--------- Using Episode Season:[${myEpisodeSeason}]"
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
 * @param useNonStrictOnTVDBSpecials Boolean: Do we use Non-Strict when trying to rename "Specials", This covers both Special Episodes as well as SpecialTypes (OVA/ONA/OAD etc)
 * @return [performRename: performRename, renameQuery: renameQuery, renameDB: renameDB, renameOrder: renameOrder, renameMapper: renameMapper, renameFilter: renameFilter, renameStrict: renameStrict, isSpecialEpisode: isSpecialEpisode, isSpecialType: isSpecialType, isMovieType: isMovieType]
 */
LinkedHashMap renameOptionForAniDBAbsoluteEpisodes(File f, LinkedHashMap anidbMatchDetails, Integer renamePass, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, LinkedHashMap group, BigDecimal aniDBJWDMatchNumber, JsonObject animeOffLineDatabaseJsonObject, Integer tvdbID, Boolean hasSeasonality, Integer mySeasonalityNumber, Boolean useNonStrictOnTVDBSpecials){
  Logging.log.info "//----- renameOptionForAniDBAbsoluteEpisodes"
  Integer anidbID = anidbMatchDetails.dbid as Integer
  switch (group.order) {
    case 'airdate':
      absoluteOrdering = false
      Logging.log.info "------- Using airdate Ordering"
      break
    case 'absolute':
      absoluteOrdering = true
      Logging.log.info "------- Using absolute Odering"
      break
    default:
      Logging.log.info "------- Using absolute Odering"
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
  String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false, true)
  if ( myEpisodeNumber == "0" && !group.isSpecialType) {
    // Episode # indicates Special Episode or "other" type of episode
    // Except for OVA/ONA/OAD .. Filenames are just too varied to assume just because it's episode zero it's a special IN the OVA series (tho that probably does happen).
    //      1: Rename using AniDB
    //      2: TVDBID?
    //        Y: Absolute Ordering, Send to renameOptionForTVDBAbsoluteEpisodes, renamePass:1, AniDB
    //           Airdate Ordering, Send to renameOptionForTVDBAirdateEpisodes, renamePass:1, AniDB
    //        N: STOP
    Logging.log.info "--------- Episode # of ${myEpisodeNumber} indicates a special/recap/other or at least not a normal Absolute Ordering (which starts at 1)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode
        //      1: Rename using AniDB and episode.special filter
        Logging.log.info "----------- Using AniDB (Set Filter to episode.special)"
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
          Logging.log.info '------------- Set non-Strict renaming'
          Logging.log.info '------------- Remove episode.special Filter'
          renameFilter = ''
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
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          Logging.log.info "----------- Using AniDB (Do not set Filter)"
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
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info '----------- Episode # detected as null'
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
//        if ( isSpecialType ) {
//          Logging.log.info "------------- SpecialType Detected. Set Filter = episode.special"
//          renameFilter = 'episode.special'
//        } else {
//          renameFilter = ''
//        }
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          Logging.log.info '------------- Set non-Strict renaming'
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
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          if ( isSpecialType ) {
            Logging.log.info "------------- SpecialType Detected. Set no Filter"
            renameOptionsSet = true
            renameQuery = anidbID
            performRename = true
            renameDB = 'AniDB'
            renameOrder = 'Absolute'
            renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
            renameFilter = ''
            if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
              renameStrict = false
              Logging.log.info '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          } else {
            Logging.log.info '//-----------------------------//'
            Logging.log.info '//            STOP             //'
            Logging.log.info '//-----------------------------//'
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
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "--------- myEpisodeNumber:[${myEpisodeNumber}] indicates a special/recap or at least not how AniDB orders things (Dot Syntax)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        Logging.log.info "----------- Using AniDB (Set Filter to episode.special)"
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
          Logging.log.info '------------- Set non-Strict renaming'
          Logging.log.info '------------- Remove episode.special Filter'
          renameFilter = ''
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
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          Logging.log.info "----------- Using AniDB (Do not set Filter)"
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
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "--------- Detected as Special Episode (isSpecialEpisode:[${isSpecialEpisode}]) indicates a special/recap or at least not how AniDB orders things :)"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        Logging.log.info "----------- Using AniDB (Set Filter to episode.special)"
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
          Logging.log.info '------------- Set non-Strict renaming'
          Logging.log.info '------------- Remove episode.special Filter'
          renameFilter = ''
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
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          Logging.log.info "----------- Using AniDB (Do not set Filter)"
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
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is within AniDB Episode Count range:[${myAniDBEpisodeCount}]"
    switch (renamePass) {
      case 1:
        Logging.log.info "------------- Using AniDB"
        // Ep # < AniDB Ep #
        //      1: Rename using AniDB
        if ( myEpisodeNumber.toInteger() > 99) {
          Logging.log.info '------------- 3-Digit Episode detected, use Episode Filtering'
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
          Logging.log.info '------------- Set non-Strict renaming'
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
          Logging.log.info "------------- Using TVDBID Provided"
          if ( absoluteOrdering ) {
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          Logging.log.info "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
          if ( anidbMatchDetails.hasAnimeListEntry) {
            def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
            Logging.log.info "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
            if (absoluteOrdering) {
              Logging.log.info "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
            } else {
              Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
            }
          }
          Logging.log.info "------------- No AnimeList Entry. AniDB Fallback will be used (good luck with that)"
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
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "--------- Detected as Special Type by group (group.isSpecialType:[${group.isSpecialType}]) indicates a Special type of [${group.specialType}]"
    switch (renamePass) {
      case 1:
        // Episode # indicates Special Episode (dot syntax)
        //      1: Rename using AniDB and episode.special filter
        Logging.log.info "----------- Using AniDB (Do not set Filter)"
        renameQuery = anidbID
        renameOptionsSet = true
        performRename = true
        renameDB = 'AniDB'
        renameOrder = 'Absolute'
        renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
        renameFilter = ''
        if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
          renameStrict = false
          Logging.log.info '------------- Set non-Strict renaming'
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
            Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
          } else {
            Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidbID:[${anidbID}]"
            return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
          }
        } else {
          Logging.log.info "----------- Using AniDB (Do not set Filter)"
          renameQuery = anidbID
          renameOptionsSet = true
          performRename = true
          renameDB = 'AniDB'
          renameOrder = 'Absolute'
          renameMapper = group.order == 'airdate' ? '[AnimeList.TheTVDB, episode]' : '[AnimeList.TheTVDB, episode]'
          renameFilter = ''
          if ( (useNonStrictOnAniDBFullMatch && aniDBJWDMatchNumber == 1 ) || (useNonStrictOnAniDBSpecials && (isSpecialEpisode || isSpecialType)) ) {
            renameStrict = false
            Logging.log.info '------------- Set non-Strict renaming'
          } else {
            renameStrict = true
          }
        }
        break
      default:
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//            STOP             //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count range:[${myAniDBEpisodeCount}]"
    Logging.log.info "----------- Likely TVDB Absolute Ordered, or Relative Seasonal Absolute Ordered or Special."
    renameOptionsSet = true
    def myAniDBEpisodeCountWithSpecials = filebotAniDBEpisodeCount(filebotAniDBgetEpisodeList(anidbID, 'include'))
    if ( myEpisodeNumber.toInteger() > myAniDBEpisodeCountWithSpecials ) {
      Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is GREATER then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
      switch (renamePass) {
        case 1:
            Logging.log.info "------------- AniDB Fallback will be used (good luck with that)"
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
              Logging.log.info '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          break
        case 2:
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
            Logging.log.info "----------- Using TVDB ID Supplied."
            if (absoluteOrdering) {
              Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
            } else {
              Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}]"
              return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
            }
          } else {
            Logging.log.info "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
            if (anidbMatchDetails.hasAnimeListEntry) {
              def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
              Logging.log.info "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
              if (absoluteOrdering) {
                Logging.log.info "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
                return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials)
              } else {
                Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}]"
                return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials)
              }
            }
          }
          Logging.log.info "----------- Renamepass:2 Can't match to AniDB"
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
          Logging.log.info "----------- unknown:1-2.3"
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
      Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is less then AniDB Episode Count WITH Specials range:[${myAniDBEpisodeCountWithSpecials}]"
      Logging.log.info "----------- Episode Number:[${myEpisodeNumber}] is probably a special"
      switch (renamePass) {
        case 1:
          Logging.log.info '----------- Using AniDB (Filter as Special)'
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
            Logging.log.info '------------- Set Not-Strict'
            Logging.log.info '------------- Remove episode.special Filter'
            renameFilter = ''
          } else {
            renameStrict = true
          }
          break
        case 2:
          if ( tvdbID > 0 ) {
            Logging.log.info "----------- Using TVDB ID Supplied."
            if (absoluteOrdering) {
              Logging.log.info "----------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
              return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials, true)
            } else {
              Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${tvdbID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
              return renameOptionForTVDBAirdateEpisodes(f, tvdbID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials, true)
            }
          } else {
            Logging.log.info "----------- No TVDB ID Supplied. Checking if Anime has Animelist Entry"
            if ( anidbMatchDetails.hasAnimeListEntry) {
              def myanimeListGetTVDBID = filebotAnimeListReturnFromAID(anidbID, true)
              Logging.log.info "------------- Has AnimeList Entry with TVDBID:[${myanimeListGetTVDBID}]"
              if (absoluteOrdering) {
                Logging.log.info "------------- Send to renameOptionForTVDBAbsoluteEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
                return renameOptionForTVDBAbsoluteEpisodes(f, anidbMatchDetails, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, useNonStrictOnTVDBSpecials, true)
              } else {
                Logging.log.info "------------- Send to renameOptionForTVDBAirdateEpisodes with tvdbID:[${myanimeListGetTVDBID}], renamePass:[1], anidb:[${anidbID}], and processAsSpecial:[true]"
                return renameOptionForTVDBAirdateEpisodes(f, myanimeListGetTVDBID, 1, animeOffLineDatabaseJsonObject, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnAniDBSpecials, anidbMatchDetails, useNonStrictOnTVDBSpecials, true)
              }
            }
            Logging.log.info "------------- No AnimeList Entry. AniDB Fallback will be used (No Episode Filter)"
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
              Logging.log.info '------------- Set non-Strict renaming'
            } else {
              renameStrict = true
            }
          }
          break
        default:
          Logging.log.info "----------- unknown:1-2.4"
          Logging.log.info '//-----------------------------//'
          Logging.log.info '//            STOP             //'
          Logging.log.info '//-----------------------------//'
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
 * @param renamePass
 * @param group
 * @param files
 * @param hasSeasonality
 * @param mySeasonalityNumber
 * @param firstANIDBWTMatchNumber
 * @param secondANIDBWTMatchNumber
 * @param thirdANIDBWTMatchNumber
 * @param fileBotANIDBJWDMatchNumber
 * @param anidbFirstMatchDetails
 * @param anidbSecondMatchDetails
 * @param anidbThirdMatchDetails
 * @param fileBotANIDBJWDMatchDetails
 * @param firstTVDBDWTMatchNumber
 * @param secondTVDBDWTMatchNumber
 * @param thirdTVDBDWTMatchNumber
 * @param fileBotTheTVDBJWDMatchNumber
 * @param theTVDBFirstMatchDetails
 * @param theTVDBSecondMatchDetails
 * @param theTVDBThirdMatchDetails
 * @param fileBotTheTVDBJWDMatchDetails
 * @param performRename
 * @param fileBotAniDBMatchUsed
 * @param animeFoundInAniDB
 * @param animeFoundInTVDB
 * @param fileBotTheTVDBMatchUsed
 * @param statsRenamedUsingScript
 * @param statsRenamedUsingFilebot
 * @param useNonStrictOnAniDBFullMatch
 * @param useNonStrictOnAniDBSpecials
 * @param animeOffLineDatabaseJsonObject
 * @param useNonStrictOnTVDBSpecials
 * @return [groupByRenameOptions: groupByRenameOptions, statsRenamedUsingScript: statsRenamedUsingScript, statsRenamedUsingFilebot: statsRenamedUsingFilebot]
 */
@SuppressWarnings('unused')
LinkedHashMap episodeRenameOptionPassOne(Integer renamePass, LinkedHashMap group, ArrayList<File> files, Boolean hasSeasonality, Integer mySeasonalityNumber, BigDecimal firstANIDBWTMatchNumber, BigDecimal secondANIDBWTMatchNumber, BigDecimal thirdANIDBWTMatchNumber, BigDecimal fileBotANIDBJWDMatchNumber, LinkedHashMap anidbFirstMatchDetails, LinkedHashMap anidbSecondMatchDetails, LinkedHashMap anidbThirdMatchDetails, LinkedHashMap fileBotANIDBJWDMatchDetails, BigDecimal firstTVDBDWTMatchNumber, BigDecimal secondTVDBDWTMatchNumber, BigDecimal thirdTVDBDWTMatchNumber, BigDecimal fileBotTheTVDBJWDMatchNumber, LinkedHashMap theTVDBFirstMatchDetails, LinkedHashMap theTVDBSecondMatchDetails, LinkedHashMap theTVDBThirdMatchDetails, LinkedHashMap fileBotTheTVDBJWDMatchDetails, Boolean performRename, Boolean fileBotAniDBMatchUsed, Boolean animeFoundInAniDB, Boolean animeFoundInTVDB, Boolean fileBotTheTVDBMatchUsed, Integer statsRenamedUsingScript, Integer statsRenamedUsingFilebot, Boolean useNonStrictOnAniDBFullMatch, Boolean useNonStrictOnAniDBSpecials, JsonObject animeOffLineDatabaseJsonObject, Boolean useNonStrictOnTVDBSpecials) {
  LinkedHashMap groupByRenameOptions = [:]
  LinkedHashMap emptyJWDMatchDetails = [score: 0.00000000, db:'', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Boolean firstPassOptionsSet = false
  Logging.log.info '// ---------- deliberations on order, DB, filter ---------- //'
  // --- airdate Syntax --- //
  if (group.order == 'airdate') {
    Logging.log.info '//--- Airdate Syntax'
    if ( animeFoundInTVDB ) {
      if ( fileBotTheTVDBMatchUsed ){
        renamerSource = 'filebot'
      }
      Logging.log.info '--- Anime found in TheTVDB'
      if ( firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info '----- 1st TVDB match 0.98+'
        if ( theTVDBFirstMatchDetails.alias == true ) {
          Logging.log.info '------- 1st TVDB match is an Alias (Increased Chance AniDB Series is not Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            Logging.log.info "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            Logging.log.info "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid as Integer, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            Logging.log.info '------- We can use AnimeLists as 1st AniDB match 0.98+'
            Logging.log.info "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
        if ( theTVDBFirstMatchDetails.alias == false ) {
          Logging.log.info '------- 1st TVDB match is NOT an alias (Increased Chance AniDB Series is Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            Logging.log.info "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            Logging.log.info "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid as Integer, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            Logging.log.info '------- We can use AnimeLists as  1st AniDB match 0.98+'
            Logging.log.info "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && fileBotTheTVDBJWDMatchNumber > 0.9800000000000000000 )   {
        Logging.log.info '----- Filebot TVDB match 0.98+, 1st TVDB Match 0.98-'
        if ( fileBotTheTVDBJWDMatchDetails.alias == true ) {
          Logging.log.info '------- filebot TVDB match is an Alias (Increased Chance AniDB Series is not Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            Logging.log.info "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            Logging.log.info "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid as Integer, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingFilebot++
            renamerSource = 'filebot'
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            Logging.log.info '------- We can use AnimeLists as 1st AniDB match 0.98+'
            Logging.log.info "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, fileBotTheTVDBJWDMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
        if ( fileBotTheTVDBJWDMatchDetails.alias == false ) {
          Logging.log.info '------- filebot TVDB match is NOT an alias (Increased Chance AniDB Series is Season 1 for TVDB Series)'
          if ( firstANIDBWTMatchNumber < 0.9800000000000000000 ) {
            Logging.log.info "------- Can't use AnimeLists as 1st AniDB match 0.98-"
            Logging.log.info "------- Sending to groupGenerationByTVDB"
            groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid as Integer, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingFilebot++
            renamerSource = 'filebot'
            firstPassOptionsSet = true
          }
          if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
            Logging.log.info '------- We can use AnimeLists as 1st AniDB match 0.98+'
            Logging.log.info "------- Sending to groupGenerationByAnimeLists, PreferedDB: anidb"
            groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, fileBotTheTVDBJWDMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
            statsRenamedUsingScript++
            firstPassOptionsSet = true
          }
        }
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && (fileBotTheTVDBJWDMatchNumber < 0.9800000000000000000 || fileBotTheTVDBMatchUsed || fileBotTheTVDBJWDMatchNumber == 0) ) {
        Logging.log.info '------ None of our TVDB Options are above 0.98+, exploring ANIDB'
        if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
          Logging.log.info "------- using 1st AniDB match as it's 0.98+"
          Logging.log.info "------- Sending to groupGenerationByAniDB"
          groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          firstPassOptionsSet = true
          statsRenamedUsingScript++
        }
        if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 && fileBotANIDBJWDMatchDetails.dbid != anidbFirstMatchDetails.dbid ) {
          Logging.log.info "------- using Filebot match as it's 0.98+"
          Logging.log.info "------- Sending to groupGenerationByAniDB"
          statsRenamedUsingFilebot++
          groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, fileBotANIDBJWDMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          firstPassOptionsSet = true
        }
        if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && !firstPassOptionsSet) {
          Logging.log.info "------- using 2nd AniDB match as it's 0.98+"
          Logging.log.info "------- Sending to groupGenerationByAniDB"
          groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, secondANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
          firstPassOptionsSet = true
          statsRenamedUsingScript++
        }
      }
      if ( !firstPassOptionsSet ) {
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//  STOP - airdate.1-1st.4    //'
        Logging.log.info '//-----------------------------//'
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
      Logging.log.info '--- Anime found Only in AniDB'
      if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info "------- using 1st AniDB match as it's 0.98+"
        Logging.log.info "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info "------- using Filebot match as it's 0.98+"
        Logging.log.info "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, fileBotANIDBJWDMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
        renamerSource = 'filebot'
      }
      if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && !firstPassOptionsSet) {
        Logging.log.info "------- using 2nd AniDB match as it's 0.98+"
        Logging.log.info "------- Sending to groupGenerationByAniDB"
        groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, secondANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( !firstPassOptionsSet ) {
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//  STOP - airdate.2-1st    //'
        Logging.log.info '//-----------------------------//'
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
    Logging.log.info "//--- Absolute Ordering Detected"
    if (( !animeFoundInAniDB || (firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber < 0.9800000000000000000 )) && animeFoundInTVDB) {
      Logging.log.info '--- Anime found Only in TVDB with matches above 0.98+'
      if ( firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info '----- Using 1st TVDB Match as it 0.98+'
        Logging.log.info '----- Sending to groupGenerationByTVDB'
        groupByRenameOptions = groupGenerationByTVDB(files, theTVDBFirstMatchDetails.dbid as Integer, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstTVDBDWTMatchNumber < 0.9800000000000000000 && fileBotTheTVDBJWDMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info '----- Using Filebot TVDB Match as it 0.98+'
        Logging.log.info '----- Sending to groupGenerationByTVDB'
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
        groupByRenameOptions = groupGenerationByTVDB(files, fileBotTheTVDBJWDMatchDetails.dbid as Integer, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
      }
      if ( secondTVDBDWTMatchNumber > 0.9800000000000000000 && !firstPassOptionsSet ) {
        Logging.log.info '----- Using 2nd TVDB Match as it 0.98+ (wow)'
        Logging.log.info '----- Sending to groupGenerationByTVDB'
        firstPassOptionsSet = true
        groupByRenameOptions = groupGenerationByTVDB(files, theTVDBSecondMatchDetails.dbid as Integer, renamePass,  animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        statsRenamedUsingScript++
      }
      if (!firstPassOptionsSet) {
        Logging.log.info '----- No Suitable TVDB Options found'
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//  STOP - absolute.1-1st.4    //'
        Logging.log.info '//-----------------------------//'
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
      Logging.log.info '------- 1st AniDB match 0.98+ && 1st TVDB match 0.98+ and group.isSpecialType = true'
      Logging.log.info '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( firstANIDBWTMatchNumber > 0.9800000000000000000 && firstTVDBDWTMatchNumber > 0.9800000000000000000 && !group.isSpecialType) {
      Logging.log.info '------- 1st AniDB match 0.98+ && 1st TVDB match 0.98+ and NOT a special'
      Logging.log.info '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', anidbFirstMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 && firstTVDBDWTMatchNumber > 0.9800000000000000000 ) {
      Logging.log.info '------- Filebot match 0.98+'
      Logging.log.info '------- Sending to groupGenerationByAnimeLists, preferredDB: anidb'
      groupByRenameOptions = groupGenerationByAnimeLists(files, 'anidb', fileBotANIDBJWDMatchDetails, theTVDBFirstMatchDetails, renamePass, animeOffLineDatabaseJsonObject, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
      firstPassOptionsSet = true
      statsRenamedUsingScript++
    }
    if ( animeFoundInAniDB && firstTVDBDWTMatchNumber <= 0.9800000000000000000 ) {
      Logging.log.info '----- Anime in ANIDB and 1st TVDB <= 0.98'
      if ( firstANIDBWTMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info "------- using 1st AniDB match as it's 0.98+"
        Logging.log.info '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, anidbFirstMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if ( firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000 ) {
        Logging.log.info "------- using Filebot match as it's 0.98+"
        Logging.log.info '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, fileBotANIDBJWDMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingFilebot++
      }
      if ( secondANIDBWTMatchNumber > 0.9800000000000000000  && !firstPassOptionsSet) {
        Logging.log.info "------- using 2nd AniDB match as it's 0.98+"
        Logging.log.info '------- Sending to groupGenerationByAniDB'
        groupByRenameOptions = groupGenerationByAniDB( files, anidbSecondMatchDetails, renamePass, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, group, firstANIDBWTMatchNumber, animeOffLineDatabaseJsonObject, 0, hasSeasonality, mySeasonalityNumber, useNonStrictOnTVDBSpecials)
        firstPassOptionsSet = true
        statsRenamedUsingScript++
      }
      if (!firstPassOptionsSet) {
        Logging.log.info '--- 1st, 2nd AniDB < 0.98 && Filebot < 0.98'
        Logging.log.info '//-----------------------------//'
        Logging.log.info '//  STOP - absolute.2-1st.4      //'
        Logging.log.info '//-----------------------------//'
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
    if (!firstPassOptionsSet) {
      Logging.log.info '//-----------------------------//'
      Logging.log.info '//  STOP - absolute.3-1st.4    //'
      Logging.log.info '//-----------------------------//'
      Logging.log.info "animeFoundInAniDB:[${animeFoundInAniDB}], firstANIDBWTMatchNumber:[${firstANIDBWTMatchNumber}], group.isSpecialType:[${group.isSpecialType}]"
      Logging.log.info "animeFoundInTVDB:[${animeFoundInTVDB}], firstTVDBDWTMatchNumber:[${firstTVDBDWTMatchNumber}], group.isSpecialType:[${group.isSpecialType}]"
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

