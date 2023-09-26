#!/usr/bin/env filebot -script
//--- VERSION 3.11.0
// http://docs.groovy-lang.org/latest/html/documentation/grape.html
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
//file:noinspection GroovyAssignabilityCheck
//file:noinspection GrReassignedInClosureLocalVar
@Grapes([
    @Grab(group='org.apache.commons', module='commons-text', version='1.9'),
    @Grab(group='org.apache.commons', module='commons-lang3', version='3.12.0')
])

import com.cedarsoftware.util.io.JsonObject
import groovy.json.JsonSlurper
import groovy.transform.Field
import net.filebot.Cache
import net.filebot.CacheType
import org.apache.commons.io.FilenameUtils
import org.apache.commons.text.similarity.JaroWinklerDistance
import net.filebot.media.AutoDetection
import net.filebot.util.FileSet
import net.filebot.Logging
import net.filebot.hash.HashType
import net.filebot.hash.VerificationUtilities
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import org.apache.commons.lang3.StringUtils

import static org.apache.commons.lang3.StringUtils.removeEnd

// Match a Calendar Date
// VOID - /(?i)((^[a-z\s]+)\(?((19\d\d|20\d\d)\)?\s))/
// VOID - /(?i)((^[a-z\s-]+)\(?((19\d\d|20\d\d)\)?\s))/
// VOID - /(?i)((^[a-z\s]+)(\d{4}))/
// VOID - /(?i)[\(|\s|\[]?([1|2][9|0]\d\d)[\)|\s|\]]?/
// VOID - /(?i)[\(|\s|\[]?([1|2][9|0]\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
// VOID - /(?i)[\(|\s|\[]?((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
@Field String stripYearDateRegex = /(?i)[\.\(|\s|\[]((19|20)\d\d)(?!\w)[\)|\s|\]|\r|\n|\t|\W]?/
@Field String stripTrailingSpacesDashRegex = /([\s-])*$/
@Field String stripMultipleAdjacentSpaces = /(\s){2,20}/
@Field String stripTVDBSeasonalityAndEverythingAfter = /(?i)([-\s]*(?<!^)S[\d]{1,2})\b(.*)$/
@Field String stripLeadingSpacesDashRegex = /^([\s-])*/
// VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))
// VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))
// VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)
// VOID - (?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])
@Field String airDateOrderMatcherRegex = /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
@Field String airDateOrderMatcherRegexStrict = /(?i)\b((S\d{1,2})(?>\.|\s)?([ExS]\d{1,4})[_]?(?>v\d{1,2})?)\b/
// Matches S0, S1, S12 as well as preceding - and spaces (unlimited number of them)
// Match 1 - Sxx
// Group 1 - S
// Group 2 - xx
// VOID - /(?i)([-\s]*S)([\d]{1,2})\b/
@Field String tvdbSeasonalityMatcher = /(?i)([-\s]*S)([\d]{1,2})\b/
// VOID - /(?i)([-\s]*(?<!^)S)([\d]{1,2})\b$/
// VOID - /(?i)([-\s]*S)([\d]{1,2})\b(?!$)/
@Field String tvdbSeasonalityPruningMatcher = /(?i)([-\s]*S)([\d]{1,2})\b(?!$)/
// VOID - /(?i)^\b((S\d{1,2})?(\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b))|^((ep\d{1,2})|episode[\s]?\d{1,2})/
// VOID - /(?i)^\b((S\d{1,2})?(\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b))|^((ep-\d{1,2}|ep\d{1,2})|episode[\s]?\d{1,2}|S\d{1,2}\s)/
@Field String tvdbOrEpisodeStartingMatcher = /(?i)^\b((S\d{1,2})?(\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b))|^((ep-\d{1,2}|ep\d{1,2})|episode[\s]?\d{1,2}|S\d{1,2}\s|(S\d{1,2})?(OVA\d{1,3}\b))/
// VOID - (?i)([-\s]*S)([\d]{1,2})\b
// VOID - (?i)\b([-\s]*S[\d]{1,2})|(\d{1,2}x\d{1,3}v\d{1,2}|\d{1,2}x\d{1,3})\b
// VOID - (?i)\b((S\d{1,2}|\d{1,2})(?>\.|\s)?(?>[ExS]\d{1,3})[_]?(?>v\d{1,2})?)
@Field String stripJustTvdbSyntax = /(?i)\b(?<![\.\[(])((S\d{1,2}|\d{1,2})(?>\.|\s)?([ExS]\d{1,3})[_]?(?>v\d{1,2})?)\b(?![\.\])=])/
// OVA, ONA or Special and all spaces, dashes etc around them. It also requires at least one around the word.
// Match 1 - The "Type" aka ova, ONA, OAD, SPECIAL, bsp, bspe, bonus etc. with spaces
// Match 2 - The "Type" aka ova, ONA, OAD, SPECIAL, bsp, bspe, bonus etc. without spaces :)
// VOID - (?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?
// VOID - /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/
// VOID - /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bspe\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/
// VOID - /(?i)([-\s(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL\b|\bsp\d{1,2}|\bspe\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/
// VOID - /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\b\d)?).*?$/
// VOID - /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\s\d)?)[-\s\)]?/
// VOID - /(?i)([-\s\(]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL|\bsp\d{1,2}|\bbonus)(\s?\d)?)[-\s\)]?/
@Field String ovaOnaOadSpecialBonusSyntaxMatcher = /(?i)([-\s(\[]+(\bOAV|\bOVA|\bONA|\bOAD|\bSPECIAL[s]?\b|\bsp\d{1,2}|\bspe\d{1,2}|\bbonus)(\b\d)?)[-\s\)]?/
// VOID - /(?i:Sample|\b(NCOP|NCED)\d{0,3}\b|Clean\s*(ED|OP|Ending|Opening)|Creditless\s*(ED|OP|Ending|Opening)|Textless\s*(ED|OP|Ending|Opening)|\b(BD|Menu)\b\s*\b(BD|Menu)\b|Character\b.*\bPV\b.*\bCollection\b|ON\sAIR\sMaterials}Previews|PMV|\bPV\b|PV\d+)|Trailer|Extras|Featurettes|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|\b(OP|ED)\b(\d+)?|Formula.1.\d{4}(?=\b|_)/
// VOID - /(?<=\b|_)(?i:Sample|\b(NCOP|NCED)\d{0,3}\b|Clean\s*(ED|OP|Ending|Opening)|Creditless\s*(ED|OP|Ending|Opening)|Textless\s*(ED|OP|Ending|Opening)|\b(BD|Menu)\b\s*\b(BD|Menu)\b|Character\b.*\bPV\b.*\bCollection\b|ON\sAIR\sMaterials}Previews|PMV|\bPV\b|PV\d+)|Trailer|Extras|Featurettes|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|\b(OP|ED)\b(\d+)?|Formula.1.\d{4}(?=\b|_)/
@Field String videoExtraFilesMatcher = /(?<=\b|_)(?i:Omake|Sample|^(?:ed|op)[0-9](?<!\s\d{1,4})(?>\.\d)?\.\w{3}$|\b(NCOP|NCED)\d{0,3}\b|Clean\s*(ED|OP|Ending|Opening)|Creditless\s*(ED|OP|Ending|Opening)|Textless\s*(ED|OP|Ending|Opening)|\b(BD|Menu)\b\s*\b(BD|Menu)\b|Character\b.*\bPV\b.*\bCollection\b|ON\sAIR\sMaterials}Previews|PMV|\bPV\b|PV\d+)|Trailer|Extras|Featurettes|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|\b(OP|ED)\b(\d+)?|Formula.1.\d{4}(?=\b|_)/
// VOID - /(?i)(\b(NCOP|NCED)[\d\\b]|\b(OP|ED)(\b\d+|\d[\s\\b]|\d{1,4}v\d{0,4}|\d+)|(ending$|opening$|amv$)|(^amv|^ending|^opening|^extra|^trailer|^preview|^scripts)|((character|series|episode)\s?preview[s]?)|(commercial|Promo(tion)?))/
@Field String looseVideoExtraFilesMatcher = /(?i)(\b(NCOP|NCED)[\d\\b]|\b(OP|ED)(\b\d+|\d[\s\\b]|\d{1,4}v\d{0,4}|\d+)|(ending$|opening$|amv$)|(^amv|^ending|^opening|^extra|^trailer|^preview|^scripts)|((character|series|episode)\s?preview[s]?)|(commercial|Promo(tion)?|Credits))/

// VOID - /(?i)^(extra[s]?|bonus|menu[s]?|TV Ad[s]?)$/
// VOID - /(?i)(^extra[s]?|bonus|special[s]?|preview[s]?|menu[s]?|TV Ad[s]?)$/
@Field String videoExtraDirectoryMatcher = /(?i)(^teaser[s]?|^extra[s]?|bonus|preview[s]?|menu[s]?|TV Ad[s]?)$/
@Field String removeStartingBracketParenthesis = /(^(\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10})/
@Field String removeEndingBracketParenthesis= /((\s?\[(?!\[\d\d\d\d\])[^\]]*\]|\s?\((?!\(\d\d\d\d\))[^)]*\)){0,10}$)/
@Field String removeFileExtensionRegex = /((?<!\s\d{1,4})(?>\.\d)?\.\w{3}$|\.\w{3}$)/
// Strips out all periods except last, or last two
// aka .mkv or .1.mkv
@Field String stripAllPeriodsExceptLast = /\.(?!(\d\.[^.]+$|[^.]+$|$))/
// VOID - /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(?<!I|II|III|IV|V|VI|VII|VIII|IX)(\s[\d]{1,2}\s?$)/
// VOID - /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,2}\s?$)/
// VOID - /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3}(v[\d]{1,2}|[a-zA-Z])?\s?)$/
@Field String numericalSeasonSyntaxMatcher = /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s[\d]{1,3})(v[\d]{1,2}|[a-zA-Z])?\s?$/
// VOID - (?i)(-\s(.*))$
// VOID - (?i)(-\s([^-]*))$
@Field String altTitleUsingLastDashMatcher = /(?i)(-\s([^-]*?))$/
// VOID - (?i)(~\s(.*))$
// VOID - /(?i)(~(.*))$/
@Field String altTitleUsingLastTildeMatcher = /(?i)(~\s([^~]*?))$/
// VOID - (?i)(\(([^)]*)\))
// VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))
// VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))(?!.\w\w\w)
// VOID - (?i)(\(((?!((19|20)\d\d))[^)]*)\))(?!\.\w\w\w)
@Field String altExtendedTitleUsingParenthesisMatcher = /(?i)(?<!^)(\(((?!((19|20)\d\d))[^)]*)\))(?!\.\w\w\w)/
// VOID - /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(?<!I|II|III|IV|V|VI|VII|VIII|IX)(1st$|2nd$|3rd$|\dth$)/
// VOID - /(?i)(?<!\d)(?<!Season)(?<!\bS)(?<!Part)(\s(1st|2nd|3rd|\dth|I|II|III|IV|V|VI|VII|VIII|IX))$/
@Field String ordinalSeriesMatcher = /(?i)(?<!\d)(?<!the)(?<!Season)(?<!\bS)(?<!Part)(\s(1st|2nd|3rd|\dth|I|II|III|IV|V|VI|VII|VIII|IX))$/
@Field String wordSeasonalityMatcher = /(?i)(?<!the)(\s+(first|second|third|fourth|fith|sixth|seventh|eighth|ninth|tenth)\s+(Season|part))/
// void - /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)|\s+(part|season)\s*([\d]+))/
// void - /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)|\s+(part)\s*([\d]+))/
// void - /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+))/
@Field String ordinalPartialSeasonalityMatcher = /(?i)(\s+(\d|\d\d)[a-z]{2}\s+(Season|part)(\s\d{1,2})?|\s+(part)\s*([\d]+|(one|two|three|four|five|six|seven|eight|nine|ten)))/
@Field String nameSeasonalitySyntaxMatcherOne = /(?i)(\s+(season)\s*([\d]+))/
@Field String parentFolderNameSeasonalitySyntaxMatcherOne = /(?i)((season)\s*([\d]+))/
// VOID - /(?i)([\s(]+(season)\s*([\d]+))\)?/
@Field String nameSeasonalityMatcherTwo= /(?i)([\s(]+(?<!\s\d[a-z]{2}\s)(season)\s*([\d]+))\)?/
@Field String specialTypeOvaOnaOadWordMatcher = /(?i)(OVA|ONA|OAD)/
@Field String matchNumberDashtoEndofLine = /(?i)(?<!Season)(?<!Part)([\s][\d]{1,4}[\s]*-[^-]*$)/


// log input parameters
Logging.log.info("Run script [$_args.script] at [$now]")

// Define a set list of Arguments?
_def.each { n, v -> Logging.log.info('Parameter: ' + [n, n =~ /plex|kodi|emby|pushover|pushbullet|discord|mail|myepisodes/ ? '*****' : v].join(' = ')) }
args.withIndex().each { f, i -> if (f.exists()) { Logging.log.info "Argument[$i]: $f" } else { Logging.log.warning "Argument[$i]: File does not exist: $f" } }

// initialize variables
failOnError = _args.conflict.equalsIgnoreCase('fail')
testRun = _args.action.equalsIgnoreCase('test')
//scriptAction = _args.action

// --output folder must be a valid folder
//**// If you don't pass --output (so _args.output is null) it will default to Current Working Directory)
//outputFolder = tryLogCatch { any { _args.output } { '.' }.toFile().getCanonicalFile() }
outputFolder = _args.absoluteOutputFolder


// enable/disable features as specified via --def parameters
unsorted  = tryQuietly { unsorted.toBoolean() && !testRun }
music     = tryQuietly { music.toBoolean() }
subtitles = tryQuietly { subtitles.split(/\W+/ as Closure) as List }
artwork   = tryQuietly { artwork.toBoolean() && !testRun }
extras    = tryQuietly { extras.toBoolean() }
clean     = tryQuietly { clean.toBoolean() }
exec      = tryQuietly { exec.toString() }

// array of kodi/plex/emby hosts
kodi = tryQuietly { any { kodi } { xbmc }.split(/[ ,;|]+/)*.split(/:(?=\d+$)/).collect { it.length >= 2 ? [host: it[0], port: it[1] as int] : [host: it[0]] } }
plex = tryQuietly { plex.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }
emby = tryQuietly { emby.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }
sonarr = tryQuietly { sonarr.split(/[ ,;|]+/)*.split(/:/).collect { it.length >= 2 ? [host: it[0], token: it[1]] : [host: it[0]] } }

// extra options, myepisodes updates and email notifications
//failOnError = any { failOnError.toBoolean() } { false } // Basically fail if nothing is renamed (which is not useful to me)
extractFolder      = tryQuietly { extractFolder as File }
skipExtract        = tryQuietly { skipExtract.toBoolean() }
deleteAfterExtract = tryQuietly { deleteAfterExtract.toBoolean() }
excludeList        = tryQuietly { def f = excludeList as File; f.isAbsolute() ? f : outputFolder.resolve(f.path) }
excludeLink        = tryQuietly { excludeLink.toBoolean() }
myepisodes         = tryQuietly { myepisodes.split(':', 2) as List }
gmail              = tryQuietly { gmail.split(':', 2) as List }
mail               = tryQuietly { mail.split(':', 5) as List }
pushover           = tryQuietly { pushover.split(':', 2) as List }
pushbullet         = tryQuietly { pushbullet.toString() }
discord            = tryQuietly { discord.toString() }
storeReport        = tryQuietly { def f = storeReport as File; f.isAbsolute() ? f : outputFolder.resolve(f.path) }
reportError        = tryQuietly { reportError.toBoolean() }

// My Options :)
clearXattr         = tryQuietly { clearXattr.toBoolean() }
useXattrDB         = tryQuietly { useXattrDB.toBoolean() }
skipMovies         = tryQuietly { skipMovies.toBoolean() }
useFBAutoDetection = tryQuietly { useFBAutoDetection.toBoolean() }
useGroupByAutodection = tryQuietly { useGroupByAutodection.toBoolean() }
String aniDBuserAgent         = any { aniDBuserAgent.toString() } { 'nosuchuser/filebot' }
String aniDBTitleXMLFilename  = any { aniDBTitleXMLFilename.toString() } { 'anime-titles.xml' }
String aniDBSynonymXMLFilename  = any { aniDBSynonymXMLFilename.toString() } { 'anime-synonyms.xml' }
Integer aniDBXMLrefreshDays   = any { aniDBXMLrefreshDays.toInteger() } { 7 }
Integer aniDBSynonymRefreshDays   = any { aniDBSynonymRefreshDays.toInteger() } { 7 }
useFilebotAniDBAliases = any { useFilebotAniDBAliases.toBoolean() } { false }
showMediaInfo      = any { showMediaInfo.toBoolean() } { false }
ignoreOrphanSubtitles      = any { ignoreOrphanSubtitles.toBoolean() } { false }
// When filebot renames some files of a batch, normally the 2nd round inherits the strictness (by default strict)
// This changes it so the 2nd round is non-strict. The theory is that
// A) All the files in a batch share the same name
// B) As long as it matches one file on strict mode, then files that do not match are also in the same series
// A *usually* means all the files in a batch are from the same series.
// B) It's probably true that all the files are the same series from a TVDB standpoint, but
// --- it is also common that files might be from different seasons, which isn't going to be helped
// --- by this option
useNonStrictPartialRenames = any { useNonStrictPartialRenames.toBoolean() } { false }
// Not something to enable by default
// This is a work around to overcome instances where filebot will just not recognize a file
// when I've looked at the script output and know for certain the 1.0 match is in fact correct
// This option turns on non-strict ONLY for 1.0 matches in AniDB
// This is not always as simple as it seems. Frequently release groups will include specials
// in their releases and just increment the episode #.  This of course makes it not match
// as there is no episode of that number, and the filename doesn't indicate it's a special or anything
// Aka series x - 26.mkv where series x has 25 normal episodes and 1 special ...
useNonStrictOnAniDBFullMatch = any { useNonStrictOnAniDBFullMatch.toBoolean() } { false }
useNonStrictOnAniDBSpecials = any { useNonStrictOnAniDBSpecials.toBoolean() } { false }
useNonStrictOnTVDBSpecials = any { useNonStrictOnTVDBSpecials.toBoolean() } { false }
useNonStrictOnAniDBMovies = any { useNonStrictOnAniDBMovies.toBoolean() } { false }
useBaseAnimeNameWithSeriesSyntax = any { useBaseAnimeNameWithSeriesSyntax.toBoolean() } { false }
useIndividualFileRenaming = any { useIndividualFileRenaming.toBoolean() } { true }
breakAfterGroups  = any { breakAfterGroups.toBoolean() } { false }
enableFileBotMovieLookups = any { enableFileBotMovieLookups.toBoolean() } { false }
renameExtras  = any { renameExtras.toBoolean() } { true }
renameInvalid  = any { renameInvalid.toBoolean() } { true }
invalidOutputFolder = any { invalidOutputFolder } { outputFolder }
checkCRC           = any { checkCRC.toBoolean() } { true }


// user-defined filters
ignore      = any { ignore } { null }
minFileSize = any { minFileSize.toLong() } { 50 * 1000L * 1000L }
minLengthMS = any { minLengthMS.toLong() } { 10 * 60 * 1000L } // 10 minutes
minLengthExtraMS = any { minLengthExtraMS.toLong() } { 6 * 60 * 1000L } // 6 Minutes
ignoreVideoExtraFoldersRegex    = any { ignoreVideoExtraFoldersRegex } { videoExtraDirectoryMatcher }
ignoreVideoExtraFilesRegex = any { ignoreVideoExtraFilesRegex } { videoExtraFilesMatcher }


// series / anime / movie format expressions
specialFormat  = any { specialFormat } { _args.format } { '{ \'specials\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
animeFormat    = any { animeFormat } { _args.format } { '{ fn }' }
specialTypeFormat = any { specialTypeFormat } { animeFormat } { _args.format } { '{ \'specialType\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
movieFormat    = any { movieFormat } { _args.format } { '{ \'movies\' }/\n' +
    '    { any { db.AniDB.n.replaceAll(/\\\\|\\//, \'\') } { db.AniDB.primaryTitle.replaceAll(/\\\\|\\//, \'\') } { db.TheTVDB.n.colon(\' - \').replaceTrailingBrackets().replaceAll(/\\\\|\\//, \'\') } { n.replaceAll(/\\\\|\\//, \'\') } }\n' +
    '    { any { if (db.AniDB.id) \'[anidb-\' + { db.AniDB.id } + \']\' } { if (order.airdate.db.AniDB.id) \'[anidb-\' + { order.airdate.db.AniDB.id } + \']\' } { if (order.absolute.db.AniDB.id) \'[anidb-\' + { order.absolute.db.AniDB.id } + \']\' } { \'[tvdb-\' + db.TheTVDB.id + \']\' } { \'[tmdb-\' + tmdbid + \']\' } }\n' +
    '/ { fn }' }
unsortedFormat = any { unsortedFormat } { 'Unsorted/{ fn }' }
extrasFormat = any { extrasFormat } { '{media.File_Created_Date_Local.split(\'-\')[0]}_video_extras/{fn}' }
invalidFormat = any { invalidFormat } { 'invalid/{ fn }' }

locale = any { _args.language.locale } { Locale.ENGLISH }

// include artwork/nfo, pushover/pushbullet and ant utilities as required
//**// Uses groovy 'include' to source into this script other "library" scripts.
if (artwork || kodi || plex || emby) { include('lib/htpc') }
if (pushover || pushbullet || gmail || mail || discord) { include('lib/web') }

// Include My Libraries
include('lib/anidb')  // AniDB Stuff
include('lib/detect') // Renamer Detect Functions
include('lib/manami')  // Anime offline Database Stuff
include('lib/shared') // Generic/Shared Functions
include('lib/sorter') // Renamer Sorter methods
include('lib/tvdb')  // TVDB Stuff

if (_args.db) {
  Logging.log.warning 'Invalid usage: The --db option has no effect'
}

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
  die "Invalid usage: output folder must exist and must be a directory: $outputFolder"
}

if ( renameInvalid && (invalidOutputFolder == null || !invalidOutputFolder.isDirectory())) {
  die "Invalid usage: invalid output folder must exist and must be a directory: $invalidOutputFolder"
}

if (args.size() == 0) {
  die 'Invalid usage: no input'

} else if (args.any { f -> f in outputFolder.listPath() }) {
  die "Invalid usage: output folder [$outputFolder] must be separate from input arguments $args"
} else if (args.any { f -> //noinspection GroovyInArgumentCheck
  f in File.listRoots() }) {
  die "Invalid usage: input $args must not include a filesystem root"
}

// ---------- Download AniDB's Title XML ---------- //
Logging.log.info "aniDBTitleXMLFilename: ${aniDBTitleXMLFilename}"
aniDBXMLDownload(aniDBuserAgent, aniDBTitleXMLFilename, aniDBXMLrefreshDays)
// --- You need to turn off the Namespace awareness else you will get this wierdness when trying to parse the languages for the titles.
// --- title[attributes={{http://www.w3.org/XML/1998/namespace}lang=en, type=short}; value=[CotS]]
//aniDBTitleXML = new groovy.xml.XmlParser(false, false).parse(aniDBTitleXMLFilename) // XMLParser

// ---------- Anime Synonyms (Created From Anime Offline Database) ---------- //
// --- I am not using Anime Offline Database directly due to a factor of the data tht goes into creating it, that has synonyms that match multiple
// --- Entries (AniDB included), which will negtively impact the data matching this script does and introduce "incorrect" Series/Season matches.
// --- So I have parsed AOD and created a list of synonyms for AniDB that don't already exist for a title in AniDB.
Logging.log.info "aniDBSynonymXMLFilename: ${aniDBSynonymXMLFilename}"
// --- You need to turn off the Namespace awareness else you will get this wierdness when trying to parse the languages for the titles.
// --- title[attributes={{http://www.w3.org/XML/1998/namespace}lang=en, type=short}; value=[CotS]]
aniDBSynonymDownload(aniDBSynonymXMLFilename, aniDBSynonymRefreshDays)
//aniDBSynonymXML = new groovy.xml.XmlParser(false, false).parse(aniDBSynonymXMLFilename) // XMLParser

// ---------- Download/Cache Anime Offline database ---------- //
// This is used to validate AniDB AID's matched are for Movies (not TV)
// https://github.com/manami-project/anime-offline-database
// Json - https://github.com/manami-project/anime-offline-database/raw/master/anime-offline-database.json
// com.cedarsoftware.util.io.JsonObject
JsonObject animeOfflineDatabase = Cache.getCache('animne-offline-database-json', CacheType.Persistent).json('anime-offline-database.json') {
  new URL('https://raw.githubusercontent.com/manami-project/anime-offline-database/master/' + it) }.expire(Cache.ONE_WEEK).get()
//Logging.log.info "animeOfflineDatabase.getClass:[${animeOfflineDatabase.getClass()}]"

// ---------- Download/Cache Anime List of TheTVDB/AniDB Mappings database ---------- //
animeListsXML = Cache.getCache('anime-lists-anime-lists', CacheType.Persistent).xml('anime-list.xml') {
    new URL('https://raw.githubusercontent.com/Anime-Lists/anime-lists/master/'+it) }.expire(Cache.ONE_WEEK).get()
// --- Filebot's xml implementation is using the XPathUtilities library and is at net.filebot.util.XPathUtilities
// ----- Not entirely sure on docs, but this seems to kinda work: https://docs.oracle.com/javase/8/docs/api/index.html?javax/xml/xpath/package-summary.html

// ---------- Compile into LinkedHashMap the AniDB XML Lists  ---------- //
LinkedHashMap aniDBCompleteXMLList = loadAniDBOfflineXML(aniDBTitleXMLFilename, aniDBSynonymXMLFilename)

// ---------- Our Custom Overrides to use during Series Basename Generation (See seriesBasenameGenerator() in lib/sorter.groovy ---------- //
def seriesBasenameGeneratorOverrideJsonFile = new JsonSlurper().parse(new File('./series_basename_overrides.json'))

// ---------- Our Custom Overrides to use during Series Basename Generation (See seriesBasenameGenerator() in lib/sorter.groovy ---------- //
def moviesBasenameGeneratorOverrideJsonFile = new JsonSlurper().parse(new File('./movies_basename_overrides.json'))


// collect input fileset as specified by the given --def parameters
// - This basically has a list of all files (fullpath)
roots = args

// helper function to work with the structure relative path rather than the whole Absolute path
@SuppressWarnings('GrMethodMayBeStatic')
def relativeInputPath(f) {
  def r = roots.find { r -> f.path.startsWith(r.path) && r.isDirectory() && f.isFile() }
  if (r != null) {
    return f.path.substring(r.path.length() + 1)
  }
  return f.name
}

// define and load exclude list (e.g. to make sure files are only processed once)
excludePathSet = new FileSet()

if (excludeList) {
  if (excludeList.exists()) {
    try {
      excludePathSet.load(excludeList)
    } catch(e) {
      die "Failed to read excludes: $excludeList: $e.message"
    }
    Logging.log.fine "Use excludes: $excludeList (${excludePathSet.size()})"
  } else {
    Logging.log.fine "Use excludes: $excludeList"
    try {
      if ((!excludeList.parentFile.isDirectory() && !excludeList.parentFile.mkdirs()) || (!excludeList.isFile() && !excludeList.createNewFile())) {
        die "Failed to create excludes: $excludeList"
      }
    } catch(e) {
      die "Failed to create excludes: $excludeList: $e.message"
    }
  }
}

extractedArchives = []
temporaryFiles = []
extraFiles = []
invalidFiles = []

def extract(f) {
  // avoid cyclical archives that extract to the same output folder over and over
  if (f in extractedArchives) {
    return []
  }

  def folder = new File(extractFolder ?: f.dir, f.nameWithoutExtension)
  def files = extract(file: f, output: folder.resolve(f.dir.name), conflict: 'auto', filter: { it.isArchive() || it.isVideo() || it.isSubtitle() || it.getExtension() == 'mks' || (music && it.isAudio()) }, forceExtractAll: true) ?: []

  extractedArchives += f
  temporaryFiles += folder
  temporaryFiles += files

  // resolve newly extracted files and deal with disk folders and hidden files correctly
  return [folder]
}

def acceptFile(f) {

  if (f.isHidden()) {
    Logging.log.fine "Ignore hidden: $f"
    return false
  }

  if (f.isDirectory() && f.name ==~ /[.@].+|bin|initrd|opt|sbin|var|dev|lib|proc|sys|var.defaults|etc|lost.found|root|tmp|etc.defaults|mnt|run|usr|System.Volume.Information/) {
    Logging.log.fine "Ignore system path: $f"
    return false
  }



  // ignore if the user-defined ignore pattern matches
  if (f.path.findMatch(ignore)) {
    Logging.log.fine "Ignore pattern: $f"
    return false
  }

  // ignore archives that are on the exclude path list
  if (excludePathSet.contains(f)) {
    return false
  }

  // accept folders right away and skip file sanity checks
  if (f.isDirectory()) {
    return true
  }

  // check if file exists
  if (!f.isFile()) {
    Logging.log.warning "File does not exist: $f"
    return false
  }

  // ignore previously linked files
  if (excludeLink && (f.symlink || f.linkCount != 1)) {
    Logging.log.fine "Exclude superfluous link: $f [$f.linkCount] $f.key"
    return false
  }

  // accept archives if the extract feature is enabled
  if (f.isArchive() || f.hasExtension('001')) {
    return !skipExtract
  }

  // ignore iso images that do not contain a video disk structure
  if (f.hasExtension('iso') && !f.isDisk()) {
    Logging.log.fine "Ignore disk image: $f"
    return false
  }

  // ignore small video files
  if (minFileSize > 0 && f.isVideo() && f.length() < minFileSize) {
    Logging.log.info "Skip small video file: $f [$f.displaySize]"
    return false
  }

  // ignore short videos
  if (minLengthMS > 0 && f.isVideo() && any { f.mediaCharacteristics.duration.toMillis() < minLengthMS } { false }) {
    Logging.log.info "Skip short video: $f [$f.mediaCharacteristics.duration]"
    return false
  }

  // ignore subtitle files without matching video file in the same or parent folder (in strict mode only)
  if ( ignoreOrphanSubtitles && ( f.isSubtitle() || f.getExtension() == 'mks') && ![f, f.dir].findResults{ it.dir }.any{ it.listFiles{ it.isVideo() && f.isDerived(it) }}) {
    Logging.log.info "Ignore orphaned subtitles: $f"
    return false
  }

  // Ignore video files that have null/undefined minutes. This is an *indicator* of something wrong with the video file
  if (f.isVideo() && (getMediaInfo(f, '{minutes}') == null) ) {
      Logging.log.info "$f - Video file has null minutes"
      if (showMediaInfo) {
          Logging.log.info ' File / Object / MediaInfo '.center(80, '-')
          Logging.log.info 'File:    ' + f
          Logging.log.info 'Object:  ' + f.xattr['net.filebot.metadata']
          Logging.log.info 'Media:   ' + any{ MediaInfo.snapshot(f) }{ null }
          if (f.metadata) {
              Logging.log.info ' Episode Metrics '.center(80, '-')
              EpisodeMetrics.defaultSequence(false).each{ m ->
                  Logging.log.info String.format('%-20s % 1.1f', m, m.getSimilarity(f, f.metadata))
              }
          }
      }
      if ( renameInvalid ) {
        invalidFiles += f
        Logging.log.info "Moving Invalid File: $f"
      } else {
        Logging.log.info "Ignore Invalid File: $f"
      }
      return false
  }

  // Ignore video files where there is a mismatch between the reported duration and the shortest duration reported.
  // This This is an *indicator* of something wrong with the video file, this only applies to Matroska Video
  if (f.isVideo() && f.getExtension() == 'mkv' && ( getMediaInfo(f, '{media["duration/string4"]}') != null ) ) {
    def mediaDuration3 = getMediaInfo(f, '{media["duration/string3"]}').replaceAll(/(?i)([.;]\d+$)/, '')
    def mediaDuration4 = getMediaInfo(f, '{media["duration/string4"]}').replaceAll(/(?i)([.;]\d+$)/, '')
    def mediaDurationList = mediaDuration3.split(':')
    Integer mediaDuration3Seconds = (mediaDurationList[0].toInteger() * 60 * 60) + (mediaDurationList[1].toInteger() * 60) + mediaDurationList[2].toInteger()
    mediaDurationList = mediaDuration4.split(':')
    Integer mediaDuration4Seconds = (mediaDurationList[0].toInteger() * 60 * 60) + (mediaDurationList[1].toInteger() * 60) + mediaDurationList[2].toInteger()
    if ( mediaDuration4Seconds <= ( mediaDuration3Seconds - 30 ) || mediaDuration4Seconds >= ( mediaDuration3Seconds + 30 ) ) {
      Logging.log.info "File ${f.name} has a duration mismatch of ${convertSecondsToHMS(mediaDuration3Seconds-mediaDuration4Seconds)}"
      if ( renameInvalid ) {
        invalidFiles += f
        Logging.log.info "Moving Invalid File: $f"
      } else {
        Logging.log.info "Ignore Invalid File: $f"
      }
      return false
    } else {
      Logging.log.finest "File ${f.name} is valid duration"
    }
  }

  // If checkCRC is enabled, then verify if it's a video file and the name has the CRC in it
  // THEN check the file (and compare CRC)
  if (checkCRC) {
    if ( f.isVideo() && (match = f.name =~ /.*([\[(]([0-f]{8})[\])]).*$/) ) {
      Logging.log.info "Checking CRC32 for:[${f.name}]"
      HashType hashType = HashType.SFV
      String xattrkey = 'CRC32'
      String file_hash = match.group(2).toLowerCase()
      String calc_hash = VerificationUtilities.computeHash(f, hashType).toLowerCase()
      if ( file_hash != calc_hash ) {
        Logging.log.info "$f.name - Failed CRC32 Check[${calc_hash}]"
        if (showMediaInfo) {
          println ' File / Object / MediaInfo '.center(80, '-')
          println 'File:    ' + f
          println 'Object:  ' + f.xattr['net.filebot.metadata']
          println 'Media:   ' + any{ MediaInfo.snapshot(f) }{ null }
          if (f.metadata) {
            println ' Episode Metrics '.center(80, '-')
            EpisodeMetrics.defaultSequence(false).each{ m ->
              println String.format('%-20s % 1.1f', m, m.getSimilarity(f, f.metadata))
            }
          }
        }
        if ( renameInvalid ) {
          invalidFiles += f
          Logging.log.info "Moving Invalid File: $f"
        } else {
          Logging.log.info "Ignore Invalid File: $f"
        }
        return false
      } else {
        Logging.log.info "...CRC32 Check[${calc_hash}] verified"
        Logging.log.info  "...Set xattr $xattrkey for [${f.name}]"
        f.xattr[xattrkey] = calc_hash
        // verify that xattr has been set correctly
        if (f.xattr[xattrkey] != calc_hash) {
          Logging.log.severe "Failed to set xattr $xattrkey for [$f]"
        }
      }
    }
  }

  /*
        "Extras" Checks
  */
  // Updated with latest Video Extra RegEx, and making with Step 1 Regex on the file name.
  if ((f.isVideo() || f.isSubtitle() || f.getExtension() == 'mks') && ( f.name.replaceAll(/_/, ' ').replaceAll(/${stripAllPeriodsExceptLast}/, ' ') =~ /${videoExtraFilesMatcher}/ )) {
    if ( renameExtras ) {
      extraFiles += f
      Logging.log.info "Moving video extra: $f"
    } else {
      Logging.log.info "Ignore video extra: $f"
    }
    return false
  }

  // Allow for casting a wider net on extras regex *if* the length is under 5 minutes
  if (minLengthExtraMS > 0 && f.isVideo() && any { f.mediaCharacteristics.duration.toMillis() < minLengthExtraMS } { false }) {
    // Strip out _ and . from the Filename
    // Strip out file extension
    // Strip out text in [] or ()
    Logging.log.finer "Checking Short video $f if it's an 'extra'"
    def myCheckName = f.nameWithoutExtension.replaceAll(/_/, ' ').replaceAll(/\./, ' ').replaceAll(/${removeStartingBracketParenthesis}/, '').replaceAll(/${removeEndingBracketParenthesis}/, '')
    Logging.log.finer "...Extra Check Name: [${myCheckName}]"
    if ( f.nameWithoutExtension.replaceAll(/_/, ' ').replaceAll(/\./, ' ').replaceAll(/${removeStartingBracketParenthesis}/, '').replaceAll(/${removeEndingBracketParenthesis}/, '') =~ /${looseVideoExtraFilesMatcher}/) {
      if ( renameExtras ) {
        extraFiles += f
        Logging.log.info "Moving video extra: $f"
      } else {
        Logging.log.fine "Ignore *likely* video extra: $f"
      }
      return false
    } else {
      Logging.log.finer ".....Not extra video: $f"
    }
  } else {
    Logging.log.finer ".....Not short video: $f [${any { f.mediaCharacteristics.duration.toMillis() } { "no duration" }}] is more then minLengthExtraMS[${minLengthExtraMS}]"
  }

  /*
      process only media files (accept audio files only if music mode is enabled)
  */
  return f.isVideo() || f.isSubtitle() || f.getExtension() == 'mks' || (music && f.isAudio())
}


// specify how to resolve input folders, e.g. grab files from all folders except disk folders and already processed folders (i.e. folders with movie/tvshow nfo files)
def resolveInput(f) {
  // resolve folder recursively, except disk folders
  if (f.isDirectory()) {
    if (f.isDisk()) {
      Logging.log.finest "Disk Folder: $f"
      return f
    }
    return f.listFiles { acceptFile(it) }.collect { resolveInput(it) }
  }

  if (f.isArchive() || f.hasExtension('001')) {
    return extract(f).findAll { acceptFile(it) }.collect { resolveInput(it) }
  }

  return f
}

// flatten nested file structure
Logging.log.info "-------------------------- File Input Acceptance Checks ---------------------------"
def input = roots.findAll { acceptFile(it) }.flatten { resolveInput(it) }.toSorted()
Logging.log.info "--------------------------         COMPLETE             ---------------------------"
// update exclude list with all input that will be processed during this run
if (excludeList && !testRun) {
  try {
    excludePathSet.append(excludeList, extractedArchives, input)
    } catch (e) {
    die "Failed to write excludes: $excludeList: $e"
  }
}

// print exclude and input sets for logging
input.each { f -> Logging.log.info "Input: $f" }

// early abort if there is nothing to do
if (input.size() == 0) {
  die 'No files selected for processing', ExitCode.NOOP
}

String anime = ''
Boolean useDetectAnimeName = false

LinkedHashMap groupGenerationNew( def input, Boolean useFBAutoDetection, Boolean useFilebotGroupByAutoDetection, Locale locale, LinkedHashMap  aniDBCompleteXMLList) {
  def delim = $/\\/$
  def join = '\\' // Because it doesn't like dollar slashy for the join
  // if there is only one file, don't bother with commonPath()
  String myInputFolder = args.size() == 1 ? FilenameUtils.getFullPathNoEndSeparator(commonPath(delim, join, args as ArrayList<File>)) : commonPath(delim, join, args as ArrayList<File>)
  Path inputFolderPath = Paths.get(myInputFolder)
  Integer inputFolderNameCount = inputFolderPath.getNameCount()
  Logging.log.info "//-----------------"
  Logging.log.info "//--- Input Folder: ${myInputFolder}"
  Logging.log.info "//--- Input Folder Name Count: ${inputFolderNameCount}"
  Logging.log.info "//-----------------"
  def groupsByManualFour = input.groupBy { f ->
    // Declare local only variables
    Boolean useDetectAnimeName = false
    String anime = ""
    String order = 'Absolute'
    def yearDateInName = null
    def filebotMovieTitle = null
    def airdateSeasonNumber = null
    def seasonNumber = null
    def specialType = null
    def ordinalSeasonNumber = null
    def partialSeasonNumber = null
    def seriesNumber = null
    def altTitle = null
    Boolean isSpecialType = false
    Boolean isSpecialEpisode = false
    Boolean isFileBotDetectedName
    Boolean isMovieType = false
    Boolean hasSeasonality = false
    Boolean hasNoSpacesFileName
    Boolean hasOrdinalSeasonality = false
    Boolean hasPartialSeasonality = false
    Boolean hasSeriesSyntax = false
    if (useFilebotGroupByAutoDetection) {
      return new AutoDetection([f] as Collection<File>, false, locale).group()
    }
    Logging.log.info "// ---------------- START -------------- //"
    String myFileName = "${StringUtils.stripAccents(f.name)}"
    Logging.log.info "//--- myFileName: [${myFileName}]"
    tryQuietly {
      f.xattr["originalfilename"] = myFileName
    }
    String myFileNameForParsing = regexRemoveKeywords(regexStep1(f.name))
    Logging.log.info "//--- myFileNameForParsing: [${myFileNameForParsing}]"
    String releaseGroup = detectAnimeReleaseGroupFromFile(f)
    Logging.log.info "//--- Release Group: [${releaseGroup}]"
    def animeDetectedName = detectAnimeName(f)
    Logging.log.info "//--- animeDetectedName: [${animeDetectedName}]"
    String animeRegexBlenderName = regexBlender(myFileName)
    Logging.log.info "//--- animeRegexBlenderName:${animeRegexBlenderName}"
    String myEpisodeNumber = detectEpisodeNumberFromFile(f, false, true, false, false, true)
    Boolean myEpisodeNumberIsInteger = ( myEpisodeNumber != null && myEpisodeNumber.isInteger() )
    Boolean myEpisodeNumberIsUnderHundred = myEpisodeNumberIsInteger ? ( myEpisodeNumber.toInteger() <= 100 ) : false
    Logging.log.info "//--- myEpisodeNumber:${myEpisodeNumber}"
    Logging.log.info "//----- myEpisodeNumberIsUnderHundred:${myEpisodeNumberIsUnderHundred}"
    String animeParentFolder = f.getParentFile().toString() == myInputFolder ? null : f.getParentFile().getName()
    String animeBaseFolder = null
    if ( animeParentFolder != null ) {
      animeBaseFolder = f.getAbsoluteFile().toPath().subpath(inputFolderNameCount, f.getAbsoluteFile().toPath().getNameCount()-1)
      if ( animeParentFolder ==~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        animeParentFolder = removeEnd(animeBaseFolder, "\\${animeParentFolder}")
      }
      Logging.log.info "//--- animeParentFolder: [${animeParentFolder}]"
    }
    if ( animeBaseFolder != null ) {
      Logging.log.info "//--- animeBaseFolder: [${animeBaseFolder}]"
      tryQuietly {
        f.xattr["originalfolder"] = animeBaseFolder
      }
    }
    /*
      Static Checks for known cases where regexBlender() based Series name matching generation will not work.
    */
    /*
          Directory Checks
    */
    // When the parent directory is extras, bonus or specials it very rarely uses the Series name in the filename
    // ReleaseGroup of null is also an extra check.
    if ( animeParentFolder != null ) {
      Matcher myRegexMatcher = regexRemoveKeywords(animeParentFolder) =~ /${videoExtraDirectoryMatcher}/
      if ( myRegexMatcher.find() && releaseGroup == null) {
        Logging.log.info '//----- Parent Folder indicates Bonus/Extras/Specials & releaseGroup is null'
        animeParentFolder =  f.toPath().subpath(inputFolderNameCount, inputFolderNameCount+1)
        Logging.log.info "//----- Set animeParentFolder: [${animeParentFolder}]"
        Logging.log.info '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
    }
    /*
          Filename Checks
    */
    // If the filename has no Spaces, most often that means:
    //   1. It uses something other then a space as a seperator
    //          period (.) underscore (_) are common
    //   2. It has NO seperator, for example: GattaiEarthGranner01.mkv
    // VOID - myDotNameMatcher = myFileName =~ /^([a-zA-Z0-9\.-]*)$/
    Matcher myDotNameMatcher = myFileName =~ /^([^ ]*)$/
    if ( myDotNameMatcher.find() ) {
      // - Remove File Extension
      // - Remove Starting Bracket/Parenthesis info
      // - Remove all Ending Bracket/Parenthesis info unless it's likely a 4 digit date
      // VOID - /^(([_\w-]*)$|(LCA_Sub_-_))/
      myDotNameMatcher = myFileName.replaceAll(/${removeFileExtensionRegex}/, '')
              .replaceAll(/${removeStartingBracketParenthesis}/, '')
              .replaceAll(/${removeEndingBracketParenthesis}/, '') =~ /^(([_\w-()\[\]]*)$|(LCA_Sub_-_))/
      if ( myDotNameMatcher.find() ) {
        // Sigh .. Why do people hate the space?
        Logging.log.info '//----- Detected "underscores are better then spaces group"'
        hasNoSpacesFileName = false
      } else {
        Logging.log.info '//----- hasNoSpacesFileName = true'
        hasNoSpacesFileName = true
        useDetectAnimeName = true
      }
    } else {
      hasNoSpacesFileName = false
    }
    // If the filename does not start with [ then it may not be parsable using regexBlender()
    Matcher myRegexMatcher = myFileName =~ /^\[/
    if ( !myRegexMatcher.find() ) {
      // If it starts with a number .. I most likely can't parse it
      // VOID - myRegexMatcher = f.name =~ /^[0-9]/
      myRegexMatcher = myFileName =~ /^([0-9]|#[0-9])/
      if ( myRegexMatcher.find() ) {
        Logging.log.info '//----- Filename starts with number'
        Logging.log.info '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
      //If it starts with TVDB Season Syntax SxxExx, I most likely can't parse it. or with Episode, Exx, epxx etc.
      myRegexMatcher = f.name =~ /${tvdbOrEpisodeStartingMatcher}/
      if ( myRegexMatcher.find() ) {
        Logging.log.info '//----- Filename starts with TVDB Season or Episode Name Syntax'
        Logging.log.info '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
    } else {
      // However *some* groups start with the episode # in brackets
      // example: [26] {480} Waste Not, Want Not.mp4
      myRegexMatcher = myFileName =~ /^\[([0-9]{1,3})]/
      if ( myRegexMatcher.find() ) {
        Logging.log.info '//----- Filename starts with [number]'
        Logging.log.info '//----- useDetectAnimeName = true'
        useDetectAnimeName = true
      }
    }


    /*
                animeRegexBlenderName Checks
    */
    // if animeRegexBlenderName starts with 1-3 digits space dash space, then it's highly unlikely it includes the Anime Series Name in the filename
    myRegexMatcher = animeRegexBlenderName =~ /(?i)(^\d{1,4})(?<!(19\d\d|20\d\d))(?>v\d)?($|\s-\s)/
    if ( myRegexMatcher ) {
      Logging.log.info "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      Logging.log.info '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // If animeRegexBlenderName starts with TVDB/Seasonality Syntax - aka S##, which are unlikely to match anything
    // Matches
    // S## or s## at the beginning of the line, with or without <space> or <dash> in front of it.
    myRegexMatcher = animeRegexBlenderName =~ /(?i)^([-\s]*S)([\d]{1,2})\b/
    if ( myRegexMatcher ) {
      Logging.log.info "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      Logging.log.info '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // Case where regexBlender doesn't work.. and you get empty/null (not sure how null, but empty can happen)
    if ( animeRegexBlenderName == null || animeRegexBlenderName == '' || animeRegexBlenderName == ' ') {
      Logging.log.info "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      Logging.log.info '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // Case where regexBlender doesn't work.. and you get basically just the word episode ...
    myRegexMatcher = animeRegexBlenderName.replaceAll(/${stripMultipleAdjacentSpaces}/, '') =~ /(?i)(episode)(?!\s\w)/
    if ( myRegexMatcher ) {
      Logging.log.info "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      Logging.log.info '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // If animeRegexBlenderName starts with special<space>- or ova<space>-, then we are not going to be able to use the filename
    // To find the Anime series nme
    myRegexMatcher = animeRegexBlenderName.replaceAll(/${stripMultipleAdjacentSpaces}/, '') =~ /(?i)^(ova|special)($|\s-\s)/
    if ( myRegexMatcher ) {
      Logging.log.info "//----- RegexBlender Name [${animeRegexBlenderName}] not usable"
      Logging.log.info '//----- useDetectAnimeName = true'
      useDetectAnimeName = true
    }
    // print xattr metadata
    if (f.metadata) {
      Logging.log.finest "--- xattr: [$f.name] => [$f.metadata]"
      if ( clearXattr ) {
        Logging.log.finest "Clearing file metadata- $f.name"
        tryQuietly { f.xattr.clear() }
      }
    }
    /*
       Set the initial anime name from either Filebot or regexBlender()
    */
    if ( useDetectAnimeName || useFBAutoDetection ) {
      Logging.log.info "//--- Filebot AutoDetection evaluation"
      if ( animeDetectedName == null || animeDetectedName == '' || animeDetectedName == 'anime') {
        Logging.log.info "//------ Filebot Returned name[${animeDetectedName}] not usable"
        // This means the autodetection effectively didn't work
        if (useDetectAnimeName) {
          if ( animeParentFolder != null ) {
            // If this is set, then we know that RegexBlender on the filename does not produce something usuable.
            anime = regexBlender(animeParentFolder)
            myFileNameForParsing = regexRemoveKeywords(regexStep1(animeParentFolder))
            isFileBotDetectedName = false
            Logging.log.info "//------ Using Parent Folder for Anime Name: ${anime}"
            Logging.log.info "//------ myFileNameForParsing: [${myFileNameForParsing}]"
          } else {
            // well poo .. Can we skip this file?
            isFileBotDetectedName = false
            Logging.log.info "//------ No parent folder other then Anime and useDetectAnimeName == true"
            Logging.log.info "//------ Using animeRegexBlenderName:[${animeRegexBlenderName}] as anime name"
            anime = animeRegexBlenderName
          }
        } else {
          // If this is not set, then regexBlender *might* produce something useful.
          anime = animeRegexBlenderName
          isFileBotDetectedName = false
          if ( animeParentFolder != null ) {
            altTitle = regexBlender(animeParentFolder)
            Logging.log.info "//------ Using AltTitle: ${altTitle}"
          }
          Logging.log.info "//------ Using regexBlender Anime Name: ${anime}"
        }
      } else {
        if ( animeParentFolder != null ) {
          anime = regexBlender(animeParentFolder)
          myFileNameForParsing = regexRemoveKeywords(regexStep1(animeParentFolder))
          Logging.log.info "//------ Using Parent Folder for Anime Name: ${anime}"
          Logging.log.info "//------ myFileNameForParsing: [${myFileNameForParsing}]"
          isFileBotDetectedName = false
        } else {
          anime = animeDetectedName
          isFileBotDetectedName = true
          Logging.log.info "//------ Using Filebot Anime Name: ${anime}"
        }
      }
    } else {
      // This is our normal route
      anime = animeRegexBlenderName
      isFileBotDetectedName = false
      Logging.log.info "//------ Using regexBlender Anime Name: ${anime}"
    }
    /*
        Static Checks where I know we should override the anime name
    */
    myRegexMatcher = myFileName =~ /GattaiEarthGranner/
    if ( myRegexMatcher.find() ) {
      anime = 'Tomica Kizuna Gattai: Earth Granner'
      isFileBotDetectedName = false
      Logging.log.info "//------ Forced Anime Name: ${anime}"
    }
    myRegexMatcher = myFileName =~ /(?i)^\[Anime\-Releases\]/
    if ( myRegexMatcher.find() ) {
      // Bastards have a tendancy to include the word feat .. just in there like [Anime-Releases] feat [JapariSub] ... Anime Name .. why?
      anime = anime.replaceFirst(/feat/, '').replaceAll(/${stripLeadingSpacesDashRegex}/, '')
      Logging.log.info "//------ Forced Anime Name: ${anime}"
    }
    myRegexMatcher = myFileName =~ /(?i)^\[LightNovel\]|^\[LNSubs\]/
    if ( myRegexMatcher.find() ) {
      // Wierd naming scheme, [ReleaseGroup] anime-name-using-hyphens-instead-of-spaces-for-everything.mp4
      anime = anime.replaceFirst(/(-[\d]{1,3}\b.*$)/, '').replaceAll(/-/, ' ').replaceAll(/${stripLeadingSpacesDashRegex}/, '')
      Logging.log.info "//------ Forced Anime Name: ${anime}"
    }
    myRegexMatcher = myFileName =~ /(?i)^(\[TDG Season [\w]*\])(\[[^.]*\])/
    if ( myRegexMatcher.find() ) {
      // Chineese Subs [TDG Season x][###][1080p]EN Kurina Official.mp4
      anime = regexBlender("${myRegexMatcher[0][1]}${myRegexMatcher[0][2]}".replaceAll(/(?i)(\]\[)/, ' - ').replaceAll(/(?i)([\]\[])/ ,''))
      Logging.log.info "//------ Forced Anime Name: ${anime}"
    }
    // -------------  Movies ---------------- //
    // Case Closed/Gintama has special episodes which are 60+ minutes long and will detect as a movie.
    // Sometimes even by filebot :)
    // This is a lame check (myEpisodeNumberIsUnderHundred), but in general if the episode # detected is greater then 100 it (maybe?) isn't a movie?
    if ( detectAnimeMovie(f) && ( !myEpisodeNumberIsInteger || myEpisodeNumberIsUnderHundred ) ) {
      order = 'Absolute'
      isMovieType = true
      // ((^[a-z\s]+)(\d{4}))
      if ( hasNoSpacesFileName ) {
        Logging.log.info "//-------- Anime Movie has dot filename pattern:[${myFileName}]"
        Matcher myMovieRegexMatcher = myFileNameForParsing =~ /(?i)((^[a-z\s]+)(\d{4}))/
        if ( myMovieRegexMatcher.find() ) {
          Logging.log.info "//---------- Anime Movie has known moviename <date> filename pattern"
          anime = myMovieRegexMatcher[0][2].replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          yearDateInName = myMovieRegexMatcher[0][3]
          Logging.log.info "//------------ Anime name is now ${anime}"
        } else {
          Logging.log.info "//---------- We are not changing the name"
        }
      } else {
        myMovieRegexMatcher = anime =~ /${stripYearDateRegex}/
        if ( myMovieRegexMatcher.find() ) {
          Logging.log.info "//---------- Anime Movie has known moviename (date) filename pattern"
          anime = anime.replaceAll(/${stripYearDateRegex}/, ' ').replaceAll(/${stripMultipleAdjacentSpaces}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          yearDateInName = myMovieRegexMatcher[0][1]
          Logging.log.info "//------------ Anime name is now ${anime} and yearDateInName is ${yearDateInName}"
        }
      }
      Logging.log.info "//-------- Anime Movie: ${anime}"
      filebotMovieTitle = detectMovie(f, false)
      Logging.log.info "//-------- Filebot Movie Title - ${filebotMovieTitle}" // null
      Logging.log.info "//-------- Minutes - ${getMediaInfo(f, '{minutes}').toInteger()}"
    }

    if ( hasNoSpacesFileName && !isMovieType) {
      Logging.log.info "//-------- Anime has no spaces file pattern:[${f.name}]"
      Matcher myMovieRegexMatcher = myFileNameForParsing =~ /${stripYearDateRegex}/
      if (myMovieRegexMatcher.find()) {
        Logging.log.info "//---------- Anime has known Movie <date> filename pattern"
        anime = anime.replaceAll(/${stripYearDateRegex}/, ' ').replaceAll(/${stripMultipleAdjacentSpaces}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
        yearDateInName = myMovieRegexMatcher[0][1]
        Logging.log.info "//------------ Anime name is now ${anime} and yearDateInName is ${yearDateInName}"
      } else {
        Logging.log.info "//---------- We are not changing the name"
      }
    }
    // TODO: Allow setting OVA on/off to treat as movietype for renaming? (aka put movies and OVA into same folders)
    LinkedHashMap seriesOptions = [
        anime: anime,
        altTitle: altTitle,
        animeDetectedName: animeDetectedName,
        filebotMovieTitle: filebotMovieTitle,
        order: order,
        airdateSeasonNumber: airdateSeasonNumber,
        isMovieType: isMovieType,
        isFileBotDetectedName: isFileBotDetectedName,
        hasSeriesSyntax: hasSeriesSyntax,
        seriesNumber: seriesNumber,
        hasSeasonality: hasSeasonality,
        seasonNumber: seasonNumber,
        hasOrdinalSeasonality: hasOrdinalSeasonality,
        ordinalSeasonNumber: ordinalSeasonNumber,
        hasPartialSeasonality: hasPartialSeasonality,
        partialSeasonNumber: partialSeasonNumber,
        isSpecialType: isSpecialType,
        specialType: specialType,
        yearDateInName: yearDateInName,
        isSpecialEpisode: isSpecialEpisode,
        myFileNameForParsing: myFileNameForParsing,
        releaseGroup: releaseGroup,
        myEpisodeNumber: myEpisodeNumber
    ]
    ArrayList returnThing = animeNameGroupGenerator(seriesOptions, aniDBCompleteXMLList)
    Logging.log.info "//--- Pass 1:Group Anime Name:[${returnThing[0].anime}]"
    ArrayList returnThing2 = animeNameGroupGenerator(returnThing[0], aniDBCompleteXMLList)
    Logging.log.info "//--- Pass 2:Group Anime Name:[${returnThing2[0].anime}]"
    Logging.log.info "// ---------------- END -------------- //"
    return returnThing2[0]
  } // End groupsByManualFour
  return groupsByManualFour
}

ArrayList animeNameGroupGenerator(LinkedHashMap seriesOptions, LinkedHashMap  aniDBCompleteXMLList){
  // Declare local only variables
  Boolean iDidSomething = false
  //noinspection GroovyUnusedAssignment
  def mySeasonalityNumber = null
  //noinspection GroovyUnusedAssignment
  def myTVDBSeasonNumber = null
  //noinspection GroovyUnusedAssignment
  String mySanityAltTxt = ""

  // Grab variables from seriesOptions input
  String order = seriesOptions.order
  String anime = seriesOptions.anime
  String myFileNameForParsing = seriesOptions.myFileNameForParsing
  String animeDetectedName = seriesOptions.animeDetectedName
  String myEpisodeNumber = seriesOptions.myEpisodeNumber
  Boolean isMovieType = seriesOptions.isMovieType
  Boolean isFileBotDetectedName = seriesOptions.isFileBotDetectedName
  Boolean isSpecialType = seriesOptions.isSpecialType
  Boolean hasSeriesSyntax = seriesOptions.hasSeriesSyntax
  Boolean hasSeasonality = seriesOptions.hasSeasonality
  Boolean isSpecialEpisode =  seriesOptions.isSpecialEpisode
  Boolean hasOrdinalSeasonality =  seriesOptions.hasOrdinalSeasonality
  Boolean hasPartialSeasonality =  seriesOptions.hasPartialSeasonality
  def seriesNumber = seriesOptions.seriesNumber
  def yearDateInName = seriesOptions.yearDateInName
  def airdateSeasonNumber = seriesOptions.airdateSeasonNumber
  def altTitle = seriesOptions.altTitle
  def filebotMovieTitle = seriesOptions.filebotMovieTitle
  def partialSeasonNumber = seriesOptions.partialSeasonNumber
  def ordinalSeasonNumber = seriesOptions.ordinalSeasonNumber
  def specialType = seriesOptions.specialType
  def seasonNumber = seriesOptions.seasonNumber
  def releaseGroup = seriesOptions.releaseGroup

  Logging.log.info "//---------------- animeNameMatcher --------------"
  /*
   Airdate (TVDB) Syntax
   -- vs myFileNameForParsing
  */
  if (detectAirdateOrder(myFileNameForParsing) && !isMovieType) {
    Logging.log.info "//-------- Anime: $anime has airdate order - myFileNameForParsing"
    order = 'airdate'
    Matcher myTVDBSeasonalityRegexMatcher = myFileNameForParsing =~ /${airDateOrderMatcherRegex}/
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      airdateSeasonNumber = myTVDBSeasonalityRegexMatcher[0][2].replaceAll(/(S|s)/, '').toInteger()
    }
    if (!isFileBotDetectedName) {
      // Remove only the Season from the Filename if it's Airdate order.
      anime = anime.replaceAll(/${airDateOrderMatcherRegex}/ , '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      Logging.log.info "//---------- Anime Name is now: $anime"
    }
  }

  /*
   Calendar Year Syntax
   -- vs myFileNameForParsing
  */
  Matcher myDateRegexMatcher = myFileNameForParsing =~ /${stripYearDateRegex}/
  if ( myDateRegexMatcher.find() ) {
    yearDateInName = myDateRegexMatcher[0][1]
    Matcher myAnimeDateRegexMatcher = anime =~ /${stripYearDateRegex}/
    if ( myAnimeDateRegexMatcher.find() ) {
      Logging.log.info "//-------- myFileNameForParsing & anime both have the Date the name - ${anime} - ${yearDateInName} "
      if (!isFileBotDetectedName) {
        anime = anime.replaceAll(/${stripYearDateRegex}/, ' ').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
        Logging.log.info "//-------- Anime name is now: $anime"
      }
    } else {
      Logging.log.info "//-------- ONLY myFileNameForParsing have the date in the name:${myFileNameForParsing}:"
    }
  }

  /*
   Numerical series Syntax
   -- vs anime
  */
  Matcher mySeasonalityRegexMatcher = anime =~ /${numericalSeasonSyntaxMatcher}/
  // For text like:
  // anime nnn to end of line
  // anime nnnvnn to end of line
  // anime - nnn to end of line
  // anime - nnnvnn to end of line
  if ( mySeasonalityRegexMatcher.find() && !isSpecialType) {
    Logging.log.info "//-------- ${anime}: has Numerical series Syntax"
    // Sometimes the alternative title text will ALSO have numerical series syntax (and be valid)
    Matcher mySanityRegexMatcher = anime =~ /${altTitleUsingLastDashMatcher}/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      Logging.log.info "//-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using -"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Set searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
          String animeTemp = anime.replaceAll(/${altTitleUsingLastDashMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          anime = animeTemp
          Logging.log.info "//---------- New Anime name is ${anime}"
        }
      }
    }
    mySanityRegexMatcher = anime =~ /${altTitleUsingLastTildeMatcher}/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      Logging.log.info "//-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using ~"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Set searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
          String animeTemp = anime.replaceAll(/${altTitleUsingLastTildeMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          anime = animeTemp
          Logging.log.info "//---------- New Anime name is ${anime}"
        }
      }
    }
    // There is in fact at least ONE Anime where it is 02 vs 2 ..
    Set animeForSearching = [] as Set
    mySeasonalityNumber = mySeasonalityRegexMatcher[0][1].toInteger()
    animeForSearching += anime
    anime = anime.replaceAll(/${numericalSeasonSyntaxMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    animeForSearching += anime + ' ' + mySeasonalityNumber
    Set myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, animeForSearching as Set, locale, false, false, false, 1)
    if ( myGroup2AniDBOptions.isEmpty() ) {
      Logging.log.info "//---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search: ${animeForSearching}"
      if ( mySeasonalityNumber >= 10 ) {
        Logging.log.info "//---------- mySeasonalityNumber[${mySeasonalityNumber}] >= 10, will not Check Ordinal/Roman Syntax"
        hasSeriesSyntax = false
      } else {
        animeForSearching = []
        animeForSearching += anime + ' ' + getOrdinalNumber(mySeasonalityNumber) // anime 2nd
        animeForSearching += anime + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, animeForSearching as Set, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//---------- TV Series not found in AniDB by AniDB XML Title Search: ${animeForSearching}"
          hasSeriesSyntax = false
        } else {
          Logging.log.info "---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
          hasSeriesSyntax = true
          seriesNumber = mySeasonalityNumber
          Logging.log.info "//---------- mySeriesNumber: ${seriesNumber}"
        }
      }
    } else {
      Logging.log.info "//---------- TV Series found in AniDB by AniDB XML Title/Synonym Search: ${animeForSearching}"
      Logging.log.info "//---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
      hasSeriesSyntax = true
      seriesNumber = mySeasonalityNumber
      Logging.log.info "//---------- mySeriesNumber: ${seriesNumber}"
    }
    Logging.log.info "//---------- Anime Name is now: $anime"
  }

  /*
   Ordinal/Roman series Syntax
   -- vs anime
  */
  mySeasonalityRegexMatcher = anime =~ /${ordinalSeriesMatcher}/
  // for text
  // Anime II
  // Anime 2nd
  if ( mySeasonalityRegexMatcher.find() && !isSpecialType) {
    Logging.log.info "//-------- ${anime}: has Ordinal/Roman series Syntax"
    hasSeriesSyntax = true
    Matcher mySeasonalityOrdinalMatcher = mySeasonalityRegexMatcher[0][1] =~ /(?i)(\d)(st|nd|rd|th)/
    if ( mySeasonalityOrdinalMatcher.find() )  {
      mySeasonalityNumber = mySeasonalityOrdinalMatcher[0][1].toInteger()
    } else {
      mySeasonalityNumber = mySeasonalityRegexMatcher[0][1].replaceAll(/\s/, '')
    }
    seriesNumber = mySeasonalityNumber
    anime = anime.replaceAll(/${ordinalSeriesMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//---------- mySeriesNumber: ${seriesNumber}"
    Logging.log.info "//---------- Anime Name is now: $anime"
  }

  /*
   Word Seasonality Syntax
   -- vs anime
  */
  Matcher myOrdinalSeasonalityMatcher = anime =~ /${wordSeasonalityMatcher}/
  // first Season, second Season, third season, first part, second part, third part etc.
  if ( myOrdinalSeasonalityMatcher.find() ) {
    Logging.log.info "//--------${anime} name has word Seasonality (ugh)"
    def wordSeasonNumberTEMP = myOrdinalSeasonalityMatcher[0][2]
    hasOrdinalSeasonality = true
    ordinalSeasonNumber = getWordNumber(wordSeasonNumberTEMP)
    Logging.log.info "//---------- ordinalSeasonNumber: ${ordinalSeasonNumber}"
    anime = anime.replaceAll(/${wordSeasonalityMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//---------- Anime Name is now: $anime"
  }


  /*
   Ordinal/Partial Seasonality Syntax
   -- vs anime
  */
  myOrdinalSeasonalityMatcher = anime =~ /${ordinalPartialSeasonalityMatcher}/
  // 2nd Season, 3rd Season, Part 1, Part 2, 2nd part, Season 2 etc.
  if ( myOrdinalSeasonalityMatcher.find() ) {
    Logging.log.info "//--------${anime} name has Ordinal and/or Partial/TVDB Seasonality"
    def partialSeasonNumberTEMP = tryQuietly { myOrdinalSeasonalityMatcher[1][6] } // It would be [0][6] if it was ONLY Partial..
    Logging.log.fine "//---------- partialSeasonNumberTEMP: [${partialSeasonNumberTEMP}]"
    def myOrdinalNumberTEMP = myOrdinalSeasonalityMatcher[0][6]
    Logging.log.fine "//---------- myOrdinalNumberTEMP: [${myOrdinalNumberTEMP}]"
    if ( myOrdinalNumberTEMP == null ) {
      hasOrdinalSeasonality = true
      def myOrdinalNumber = myOrdinalSeasonalityMatcher[0][2]
      ordinalSeasonNumber = myOrdinalNumber.toInteger()
      Logging.log.info "//---------- ordinalSeasonNumber: ${ordinalSeasonNumber}"
    } else {
      hasPartialSeasonality = true
//      partialSeasonNumber = myOrdinalNumberTEMP.toInteger()
      Logging.log.fine "//---------- myOrdinalNumberTEMP.isInteger(): [${myOrdinalNumberTEMP.isInteger()}]"
      Logging.log.fine "//---------- getWordNumber(myOrdinalNumberTEMP): [${getWordNumber(myOrdinalNumberTEMP)}]"
      partialSeasonNumber = myOrdinalNumberTEMP.isInteger() ? myOrdinalNumberTEMP.toInteger() : getWordNumber(myOrdinalNumberTEMP)
      Logging.log.info "//---------- Partial seasonNumber:: ${partialSeasonNumber}"
    }
    if ( partialSeasonNumberTEMP != null ) {
      hasPartialSeasonality = true
//      partialSeasonNumber = partialSeasonNumberTEMP.toInteger()
      partialSeasonNumber = partialSeasonNumberTEMP.isInteger() ? partialSeasonNumberTEMP.toInteger() : getWordNumber(partialSeasonNumberTEMP)
      Logging.log.info "//---------- Partial seasonNumber: ${partialSeasonNumber}"
    }
    anime = anime.replaceAll(/${ordinalPartialSeasonalityMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//---------- Anime Name is now: $anime"
  }

  /*
   Name Seasonality Syntax One
   -- vs anime
  */
  Matcher myTVDBSeasonalityRegexMatcher = anime =~ /${nameSeasonalitySyntaxMatcherOne}/ // Season xx
  if ( myTVDBSeasonalityRegexMatcher.find() ) {
    Logging.log.info "//-------- ANIME:${anime}: name has Seasonality (Season xx)"

    // Sometimes the alternative title text will ALSO have numerical series syntax (and be valid)
    Matcher mySanityRegexMatcher = anime =~ /${altTitleUsingLastDashMatcher}/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      Logging.log.info "//-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using -"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Set searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
          String animeTemp = anime.replaceAll(/${altTitleUsingLastDashMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          anime = animeTemp
          Logging.log.info "//---------- New Anime name is ${anime}"
        }
      }
    }
    mySanityRegexMatcher = anime =~ /${altTitleUsingLastTildeMatcher}/
    if (mySanityRegexMatcher.find() && !isMovieType) {
      mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
      Logging.log.info "//-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using ~"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Set searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
          String animeTemp = anime.replaceAll(/${altTitleUsingLastTildeMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
          anime = animeTemp
          Logging.log.info "//---------- New Anime name is ${anime}"
        }
      }
    }
    myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][3].toInteger()
    mySeasonalityNumber = myTVDBSeasonNumber
    hasSeasonality = true
    seasonNumber = mySeasonalityNumber
    anime = anime.replaceAll(/${nameSeasonalitySyntaxMatcherOne}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//---------- Anime Name is now: $anime"
  }

  /*
  Name Seasonality Syntax Two
  -- vs myFileNameForParsing
  */
  myTVDBSeasonalityRegexMatcher = myFileNameForParsing =~ /${nameSeasonalityMatcherTwo}/
  // Season xx or (Season xx)
  if ( myTVDBSeasonalityRegexMatcher.find() ) {
    Logging.log.info "//-------- FILENAME:${myFileNameForParsing}: name has Seasonality (Season xx)"
    myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][3].toInteger()
    mySeasonalityNumber = myTVDBSeasonNumber
    hasSeasonality = true
    seasonNumber = mySeasonalityNumber
  }

  /*
   Seasonality Syntax
   -- vs anime
  */
  myTVDBSeasonalityRegexMatcher = anime =~ /${tvdbSeasonalityMatcher}/
  // Matches S0, S1, S12 as well as preceeding - and spaces (unlimited number of them)
  if ( myTVDBSeasonalityRegexMatcher.find() && !isMovieType) {
    if ( myEpisodeNumber == null ) {
      Logging.log.info "//-------- ${anime}: name has Seasonality (Sx), BUT no detected episode"
      Logging.log.info "//-------- Treat as a Special instead"
      isSpecialEpisode = true
    } else {
      Logging.log.info "//-------- ${anime}: name has Seasonality (Sx)"
      myTVDBSeasonNumber = myTVDBSeasonalityRegexMatcher[0][2].toInteger()
      mySeasonalityNumber = myTVDBSeasonNumber
      hasSeasonality = true
      seasonNumber = mySeasonalityNumber
      Logging.log.info "//---------- mySeasonalityNumber: ${mySeasonalityNumber}"
    }
    myTVDBSeasonalityRegexMatcher = anime =~ /${tvdbSeasonalityPruningMatcher}/
    // Verify Sxx is at the end of the string, if not then we probably need to prune some info.
    if ( myTVDBSeasonalityRegexMatcher.find() ) {
      Logging.log.info "//---------- ${anime} needs needs TVDB Seasonality pruning"
      Logging.log.info "//---------- mySeasonalityNumber: ${mySeasonalityNumber}"
      anime = anime.replaceAll(/${stripTVDBSeasonalityAndEverythingAfter}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    } else {
      // This .. shouldn't happen?
      anime = anime.replaceAll(/${stripTVDBSeasonalityAndEverythingAfter}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    }
    Logging.log.info "//---------- Anime Name is now: $anime"
  }

  /*
   OVA Syntax
   -- vs myFileNameForParsing
  */
  Matcher myOVARegexMatcher = myFileNameForParsing =~  /${ovaOnaOadSpecialBonusSyntaxMatcher}/
  if ( myOVARegexMatcher.find() ) {
    Logging.log.info "//-------- ${anime} has OVA/ONA/OAD/Special/Bonus Syntax - myFileNameForParsing"
    if ( isMovieType ) {
      altTitle = filebotMovieTitle
      Logging.log.info "//---------- And Detected as Anime Movie: ${altTitle}"
      isMovieType = false
    }
    specialType = myOVARegexMatcher[0][2].replaceAll(/[0-9]/, '')
    switch(specialType) {
      case ~/(?i)special/:
        isSpecialEpisode = true
        Logging.log.info "//---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
        break
      case ~/(?i)bonus/:
        isSpecialEpisode = true
        Logging.log.info "//---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
        break
      case ~/(?i)sp/:
        isSpecialEpisode = true
        Logging.log.info "//---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
        break
      case ~/(?i)spe/:
        isSpecialEpisode = true
        Logging.log.info "//---------- myOVAType[${specialType}] Detected. Not pruning Anime Name"
        break
      default:
        isSpecialType = true
        Logging.log.info "//---------- myOVAType[${specialType}] Detected. Pruning Anime Name"
        anime = anime.replaceAll(/${ovaOnaOadSpecialBonusSyntaxMatcher}/, ' - ').replaceAll(/\s-\s$/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ')
        Logging.log.info "//---------- New Anime name is ${anime}"
        break
    }
  }

  /*
   Alternative Title using - Syntax
   -- vs anime
  */
  Matcher mySanityRegexMatcher = anime =~ /${altTitleUsingLastDashMatcher}/
  if (mySanityRegexMatcher.find() && !isMovieType) {
    mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//-------- [${anime}] has possible additional text to remove: [${mySanityAltTxt}] using -"
    Set searchList = ["${anime}"]
    searchList += ["${returnAniDBRomanization(anime)}"]
    String animeTemp = anime.replaceAll(/${altTitleUsingLastDashMatcher}/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Set myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
    if ( myGroup2AniDBOptions.isEmpty() ) {
      Logging.log.info "//---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search:[${anime}]"
      anime = animeTemp
      Logging.log.info "//---------- New Anime name is ${anime}"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Logging.log.info "//------------ Checking if [${mySanityAltTxt}] is Alternative Title"
        searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
        }
      }
    } else {
      Logging.log.info "//---------- TV Series found in AniDB by AniDB XML Title/Synonym Search:[${anime}]"
      Logging.log.info "//----------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
      Logging.log.info '//------------ We are not changing the Anime name'
    }
  }

  /*
   Alternative Title using ~ Syntax
   -- vs anime
  */
  mySanityRegexMatcher = anime =~ /${altTitleUsingLastTildeMatcher}/
  if (mySanityRegexMatcher.find() && !isMovieType) {
    mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Logging.log.info "//-------- [${anime}] has possible additional text or Alternative Title: [${mySanityAltTxt}] using ~"
    Set searchList = ["${anime}"]
    searchList += ["${returnAniDBRomanization(anime)}"]
    String animeTemp = anime.replaceAll(/(?i)(~(.*))$/, '').replaceAll(/${stripMultipleAdjacentSpaces}/, ' ').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    Set myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
    if ( myGroup2AniDBOptions.isEmpty() ) {
      Logging.log.info "//---------- TV Series not found in AniDB by AniDB XML Title/Synonym Search: ${anime}"
      anime = animeTemp
      Logging.log.info "//---------- New Anime name is ${anime}"
      if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
        Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
      } else {
        Logging.log.info "//------------ Checking if [${mySanityAltTxt}] is Alternative Title"
        searchList = ["${mySanityAltTxt}"]
        searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
        myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//-------------- Does not seem so"
        } else {
          Logging.log.info "//-------------- AniDB Returned results: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
          altTitle = mySanityAltTxt
        }
      }
    } else {
      Logging.log.info "//---------- TV Series found in AniDB by AniDB XML Title/Synonym Search: ${anime}"
      Logging.log.info "//------------ Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
      Logging.log.info '//------------ We are not changing the Anime name'
    }
  }


  /*
   Alternative/Extended Title using () Syntax
   -- vs myFileNameForParsing
  */
  mySanityRegexMatcher = myFileNameForParsing =~ /${altExtendedTitleUsingParenthesisMatcher}/
  if (mySanityRegexMatcher.find()) {
    mySanityAltTxt = mySanityRegexMatcher[0][2].replaceAll(/${stripLeadingSpacesDashRegex}/, '').replaceAll(/${stripTrailingSpacesDashRegex}/, '')
    // While titles of less then 2 characters DO exist, it's not common, but alt titles of 2 or less are more common, fairly error prone (to match) and
    // just a waste of time.
    if ( mySanityAltTxt.size() >= 3) {
      if ( releaseGroup == "ASW" && mySanityAltTxt == "The Adventure of Dai") {
        Logging.log.info "//-------- [${anime}] has malformed title from Release Group:[${releaseGroup}] - Should be: Dragon Quest: The Adventure of Dai"
        anime = "Dragon Quest: The Adventure of Dai"
        Logging.log.info "//------------ New Anime name is ${anime}"
      } else {
        Logging.log.info "//-------- [${anime}] has possible Extended Title: [${anime} (${mySanityAltTxt})] using () - myFileNameForParsing"
        Set searchList = ["${anime} (${mySanityAltTxt})"]
        Set myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
        if ( myGroup2AniDBOptions.isEmpty() ) {
          Logging.log.info "//---------- Insanity Check for Groups that use () for season [anime (season xx)]"
          if ( mySanityAltTxt =~ /${parentFolderNameSeasonalitySyntaxMatcherOne}/ ) {
            Logging.log.info "//------------ Insanity Found, ignoring [${mySanityAltTxt}] as an Alternative Title"
          } else {
            Logging.log.info "//-------- [${anime}] has possible Alternative Title: [${mySanityAltTxt}] using () - myFileNameForParsing"
            searchList = ["${mySanityAltTxt}"]
            searchList += ["${returnAniDBRomanization(mySanityAltTxt)}"]
            myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 2)
            if ( myGroup2AniDBOptions.isEmpty() ) {
              Logging.log.info "//---------- Alternative Title not found in AniDB by AniDB XML Title/Synonym Search: ${mySanityAltTxt}"
              String animeTemp=anime + " " + mySanityAltTxt
              searchList = ["${animeTemp}"]
              Logging.log.info "//---------- Insanity Check for Groups that use () for title subtext aka [anime: subtext] - ${animeTemp}"
              searchList += ["${returnAniDBRomanization(animeTemp)}"]
              myGroup2AniDBOptions = anidbHashTitleSearch(aniDBCompleteXMLList, searchList, locale, false, false, false, 1)
              if ( myGroup2AniDBOptions.isEmpty() ) {
                Logging.log.info "//------------ Instanity Check failed :)"
              } else {
                Logging.log.info "//------------ Insanity Found"
                anime = "${anime} "+"${mySanityAltTxt}"
                Logging.log.info "//------------ New Anime name is ${anime}"
              }
            } else {
              Logging.log.info "//---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
              Logging.log.info "//-------------- Setting altTitle:[${mySanityAltTxt}]"
              altTitle = mySanityAltTxt
            }
          }
        } else {
          Logging.log.info "//---------- Our Query Returned from AniDB: ${myGroup2AniDBOptions}"
          Logging.log.info "//-------------- Setting anime:[${anime} (${mySanityAltTxt})]"
          anime = "${anime} (${mySanityAltTxt})"
        }
      }
    }
  }


  /*
  Movie only Removal
  -- vs anime
  */
  mySanityRegexMatcher = anime =~ /${matchNumberDashtoEndofLine}/
  if (mySanityRegexMatcher.find() && isMovieType) {
    Logging.log.info "//-------- Movie [${anime}] has likely non title text [${mySanityRegexMatcher[0][0]}]"
    String animeTemp = anime.replaceAll(/${matchNumberDashtoEndofLine}/, '')
    anime = animeTemp
    Logging.log.info "//-------------- Setting anime:[${anime}]"
  }

  /*
   Manual Overrides
   -- Not a good solution, but I don't have any better ideas at the moment
  */
  if ( altjwdStringBlender(anime) == altjwdStringBlender('dragon quest dai no daibouken') && releaseGroup =~ /ASW|DKB/) {
    baseAnimeName = 'dragon quest dai no daibouken 2020'
    Logging.log.info "//--------------  Dragon Quest 2020 Fix: Setting title to ${baseAnimeName}"
  }

  return [
      [anime: anime.toLowerCase(),
       altTitle: altTitle,
       animeDetectedName: animeDetectedName,
       filebotMovieTitle: filebotMovieTitle,
       order: order,
       airdateSeasonNumber: airdateSeasonNumber,
       isMovieType: isMovieType,
       isFileBotDetectedName: isFileBotDetectedName,
       hasSeriesSyntax: hasSeriesSyntax,
       seriesNumber: seriesNumber,
       hasSeasonality: hasSeasonality,
       seasonNumber: seasonNumber,
       hasOrdinalSeasonality: hasOrdinalSeasonality,
       ordinalSeasonNumber: ordinalSeasonNumber,
       hasPartialSeasonality: hasPartialSeasonality,
       partialSeasonNumber: partialSeasonNumber,
       isSpecialType: isSpecialType,
       specialType: specialType,
       yearDateInName: yearDateInName,
       isSpecialEpisode: isSpecialEpisode,
       releaseGroup: releaseGroup,
      ],
      [iDidSomething: iDidSomething]]
}

LinkedHashMap groupsByManualThree = groupGenerationNew(input, useFBAutoDetection, useGroupByAutodection, locale, aniDBCompleteXMLList)
def groupsByManualThreeMovies = groupsByManualThree.findAll { it.key.isMovieType == true}
def groupsByManualThreeEpisodes = groupsByManualThree.findAll { it.key.isMovieType == false}

Logging.log.info ''
Logging.log.info ''
Logging.log.info '***********************************'
Logging.log.info '***           Episodes            ***'
Logging.log.info '***********************************'
Logging.log.info ''
Logging.log.info ''
groupsByManualThreeEpisodes.each { group, files ->
  Logging.log.info "${groupInfoGenerator(group)}"
}
Logging.log.info ''
Logging.log.info ''
Logging.log.info '***********************************'
Logging.log.info '***           Movies            ***'
Logging.log.info '***********************************'
Logging.log.info ''
Logging.log.info ''
groupsByManualThreeMovies.each { group, files ->
  Logging.log.info "${groupInfoGenerator(group)}"
}
Logging.log.info ''

if (breakAfterGroups) {
  die "breakAfterGroups Enabled"
}

// keep track of files that have been processed successfully
destinationFiles = []
destinationFilesFilebot = []
destinationFilesScript = []
failedFilesFilebot = []
failedFilesScript = []
renameMissedFiles1stPass = []
renameMissedFiles2ndPass = []

// keep track of unsorted files or files that could not be processed for some reason
unsortedFiles = []
partialFiles = []

/**
 * Wrapper for Filebot rename
 *
 * @param group Group info for the files we are renaming
 * @param files  The files we are renaming
 * @param renameStrict  Rename Strict option, true or false?
 * @param renameQuery The Rename Query
 * @param renameDB The Rename Database
 * @param renameOrder The Episode Order we will use
 * @param renameFilter The Filter we will use when renaming
 * @param renameMapper The Mapper we will use when renaming
 * @return generally nothing, but using void would occasionally cause the script to error out.
 */
def renameWrapper(LinkedHashMap group, def files, LinkedHashMap renameOptions) {
  rfsPartial = false
  rfsPartialFiles = []
  rfs = []
  wrapperArgs = [:]
  wrapperArgs.put('file', files)
  // Our "Default" format is animeFormat so set that, and then change it for other situations.
  wrapperArgs.put('format', animeFormat)
  if (group.isSpecialType || renameOptions.isSpecialType) {
    wrapperArgs.put('format', specialTypeFormat)
  }
  if (group.isSpecialEpisode || renameOptions.isSpecialEpisode) {
    wrapperArgs.put('format', specialFormat)
  }
  // Filebot doesn't support Movies with TheTVDB.. if it renames with TheTVDB then it's
  // Either an incorrect match OR the AniDB Entry is a movie, but the "TV" episodes are for some reason
  // included in the "movie" entry on AniDB, usually as kind "other" .. Which Filebot doesn't support :)
  if ((group.isMovieType || renameOptions.isMovieType) && renameOptions.renameDB =~ /(?i)AniDB/) {
    wrapperArgs.put('format', movieFormat)
  }
  wrapperArgs.put('strict', renameOptions.renameStrict)
  if (renameOptions.renameQuery) {
    wrapperArgs.put('query', "${renameOptions.renameQuery}")
  }
  if (renameOptions.renameDB) {
    wrapperArgs.put('db', renameOptions.renameDB)
  }
  if (renameOptions.renameOrder) {
    wrapperArgs.put('order', "${renameOptions.renameOrder}")
  }
  if (renameOptions.renameFilter) {
    wrapperArgs.put('filter', "${renameOptions.renameFilter}")
  }
  if (renameOptions.renameMapper) {
    wrapperArgs.put('mapper', "${renameOptions.renameMapper}")
  }
  // Logging.log.info "Running: Rename"
  // Logging.log.info "rename - ${[*:wrapperArgs]}"
  Logging.log.info '// ---------- RENAME: 1st Run ---------- //'
  if ( useIndividualFileRenaming ) {
    files.each { fileToRename ->
      Logging.log.info "// ----Individual Rename:${fileToRename}"
      rftsTemp = []
      wrapperArgs.file = fileToRename
      try {
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      } catch (e) {
        // Filebot doesn't return an error when you have been banned
        // It looks to return text?
        // AniDB has already banned your IP. Please stop hitting AniDB for at least 24 hours.
        //    Failed to fetch resource: AniDB has already banned your IP. Please stop hitting AniDB for at least 24 hours.
        Logging.log.severe "renameWrapper() - Caught error:[${e}]"
//      } catch (Exception IllegalStateException) {
        Logging.log.info 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
    }
  } else {
    Logging.log.info "// ----Group Rename"
    try {
      rfs = rename(*:wrapperArgs)
    } catch (e) {
      Logging.log.severe "renameWrapper() - Caught error:[${e}]"
//      } catch (Exception IllegalStateException) {
      Logging.log.info 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
      aniDBBanHammer = true as Boolean
      rfsIncomplete = false as Boolean
      rfs = []
    }
  }
  // Logging.log.info "RFS is class: ${rfs.getClass()}"
  if (rfs) {
    Logging.log.info "--- Successfully Renamed files - ${rfs}"
    switch(renamerSource) {
      case ~/filebot/:
        destinationFilesFilebot += rfs
        break
      case ~/script/:
        destinationFilesScript += rfs
        break
    }
    destinationFiles += rfs
    if ( rfs.size() == files.size() ) {
      Logging.log.info "----- Renamed all ${rfs.size()} files out of ${files.size()}"
      rfsIncomplete = false as Boolean
    } else {
      Logging.log.info "--- Renamed ${rfs.size()} files out of ${files.size()}"
      rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles1stPass += rfsLeftOver
      Logging.log.info "----- Leaving ${rfsLeftOver}"
      rfsPartial = true
    }
  } else if (failOnError && rfs == null) {
    Logging.log.info '*****************************'
    Logging.log.info '***  FAILURE! FAILURE!    ***'
    Logging.log.info '*****************************'
    die "Failed to process group: $group"
  } else {
    rfsIncomplete = true  as Boolean
    // TODO
    // Collecting stats on failure here is not useful as these files *could* get renamed in a different stage
    switch(renamerSource) {
      case ~/filebot/:
        failedFilesFilebot += files
        break
      case ~/script/:
        failedFilesScript += files
        break
    }
  }
  // ------ Rename again if there are Leftover files from the First rename attempt              ------- //
  // ------ try to rename each file individually unless useNonStrictPartialRenames is set       ------- //
  // ------ This sometimes overcomes the behavior that evaluating multiple files results        ------- //
  // ------ In no matches, while evaluating a file singularly will result in a match (strictly) ------- //
  // ------ Non-Strict usually doesn't have this issue, but does increase the probability of    ------- //
  // ------ Incorrect matches
  if ( rfsPartial && rfsLeftOver ) {
    Logging.log.info '// ---------- RENAME: 2nd Run ---------- //'
    Logging.log.info "--- 2nd Attempt to rename files missed during the first rename - ${rfsLeftOver}"
    if ( useNonStrictPartialRenames ) {
      Logging.log.info "--- Enabling Non-Strict 2nd Pass"
      wrapperArgs.strict = false
      wrapperArgs.file = rfsLeftOver
      rfs = rename(*:wrapperArgs)
    } else {
      rfs = []
      rfsLeftOver.each { fileToRename ->
        rfsTemp = []
        wrapperArgs.file = fileToRename
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      }
    }
    if (rfs) {
      Logging.log.info '--- Successfully Renamed files'
      destinationFiles += rfs
      switch(renamerSource) {
        case ~/filebot/:
          destinationFilesFilebot += rfs
          break
        case ~/script/:
          destinationFilesScript += rfs
          break
      }
      if ( rfs.size() == rfsLeftOver.size() ) {
        Logging.log.info "----- Renamed all ${rfs.size()} files out of ${files.size()}"
        rfsIncomplete = false  as Boolean
        rfsLeftOver = []
      } else {
        Logging.log.info "--- Renamed ${rfs.size()} files out of ${rfsLeftOver.size()}"
        rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
        renameMissedFiles2ndPass += rfsLeftOver
        Logging.log.info "----- Leaving ${rfsLeftOver}"
        rfsIncomplete = false  as Boolean
      }
    } else if (failOnError && rfs == null) {
      Logging.log.info '*****************************'
      Logging.log.info '***  FAILURE! FAILURE!    ***'
      Logging.log.info '*****************************'
      die "Failed to process group: $group"
    } else {
      Logging.log.info "--- Failed to rename any more files from group: $group"
      rfsIncomplete = false  as Boolean
      rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles2ndPass += rfsLeftOver
      switch(renamerSource) {
        case ~/filebot/:
          failedFilesFilebot += renameMissedFiles2ndPass
          break
        case ~/script/:
          failedFilesScript += renameMissedFiles2ndPass
          break
      }
    }
  }
}

def renameMovieWrapper(LinkedHashMap group, def files, Boolean renameStrict, def renameQuery = false, def renameDB = false, def renameOrder = false, def renameFilter = false, def renameMapper = false) {
  rfsPartial = false
  rfsPartialFiles = []
  rfs = []
  wrapperArgs = [:]
  wrapperArgs.put('file', files)
  // Our "Default" format is animeFormat so set that, and then change it for other situations.
  wrapperArgs.put('format', animeFormat)
  if (group.isSpecialType) {
    wrapperArgs.put('format', specialTypeFormat)
  }
  if (group.isSpecialEpisode) {
    wrapperArgs.put('format', specialFormat)
  }
  // Filebot doesn't support Movies with TheTVDB.. if it renames with TheTVDB then it's
  // Either an incorrect match OR the AniDB Entry is a movie, but the "TV" episodes are for some reason
  // included in the "movie" entry on AniDB, usually as kind "other" .. Which Filebot doesn't support :)
  if (group.isMovieType && renameDB =~ /(?i)AniDB/) {
    wrapperArgs.put('format', movieFormat)
  }
  wrapperArgs.put('strict', renameStrict)
  if (renameQuery) {
    wrapperArgs.put('query', "${renameQuery}")
  }
  if (renameDB) {
    wrapperArgs.put('db', renameDB)
  }
  if (renameOrder) {
    wrapperArgs.put('order', "${renameOrder}")
  }
  if (renameFilter) {
    wrapperArgs.put('filter', "${renameFilter}")
  }
  if (renameMapper) {
    wrapperArgs.put('mapper', "${renameMapper}")
  }
  // Logging.log.info "Running: Rename"
  // Logging.log.info "rename - ${[*:wrapperArgs]}"
  Logging.log.info '// ---------- RENAME: 1st Run ---------- //'
  if ( useIndividualFileRenaming ) {
    files.each { fileToRename ->
      Logging.log.info "// ----Individual Rename:${fileToRename}"
      rftsTemp = []
      wrapperArgs.file = fileToRename
      try {
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      } catch (e) {
        // Filebot doesn't return an error when you have been banned
        // It looks to return text?
        // AniDB has already banned your IP. Please stop hitting AniDB for at least 24 hours.
        //    Failed to fetch resource: AniDB has already banned your IP. Please stop hitting AniDB for at least 24 hours.
        Logging.log.severe "renameMovieWrapper() - Caught error:[${e}]"
//      } catch (Exception IllegalStateException) {
        Logging.log.info 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
    }
  } else {
      Logging.log.info "// ----Group Rename"
      try {
        rfs = rename(*:wrapperArgs)
      } catch (e) {
        Logging.log.severe "renameMovieWrapper() - Caught error:[${e}]"
//      } catch (Exception IllegalStateException) {
        Logging.log.info 'AniDB BanHammer Detected. Please stop hitting AniDB for at least 24 hours'
        aniDBBanHammer = true as Boolean
        rfsIncomplete = false as Boolean
        rfs = []
      }
  }
  // Logging.log.info "RFS is class: ${rfs.getClass()}"
  if (rfs) {
    Logging.log.info "--- Successfully Renamed files - ${rfs}"
    switch(renamerSource) {
      case ~/filebot/:
        destinationFilesFilebot += rfs
        break
      case ~/script/:
        destinationFilesScript += rfs
        break
    }
    destinationFiles += rfs
    if ( rfs.size() == files.size() ) {
      Logging.log.info "----- Renamed all ${rfs.size()} files out of ${files.size()}"
      rfsIncomplete = false as Boolean
    } else {
      Logging.log.info "--- Renamed ${rfs.size()} files out of ${files.size()}"
      rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles1stPass += rfsLeftOver
      Logging.log.info "----- Leaving ${rfsLeftOver}"
      rfsPartial = true
    }
  } else if (failOnError && rfs == null) {
    Logging.log.info '*****************************'
    Logging.log.info '***  FAILURE! FAILURE!    ***'
    Logging.log.info '*****************************'
    die "Failed to process group: $group"
  } else {
    rfsIncomplete = true  as Boolean
    // TODO
    // Collecting stats on failure here is not useful as these files *could* get renamed in a different stage
    switch(renamerSource) {
      case ~/filebot/:
        failedFilesFilebot += files
        break
      case ~/script/:
        failedFilesScript += files
        break
    }
  }
  // ------ Rename again if there are Leftover files from the First rename attempt              ------- //
  // ------ try to rename each file individually unless useNonStrictPartialRenames is set       ------- //
  // ------ This sometimes overcomes the behavior that evaluating multiple files results        ------- //
  // ------ In no matches, while evaluating a file singularly will result in a match (strictly) ------- //
  // ------ Non-Strict usually doesn't have this issue, but does increase the probability of    ------- //
  // ------ Incorrect matches
  if ( rfsPartial && rfsLeftOver ) {
    Logging.log.info '// ---------- RENAME: 2nd Run ---------- //'
    Logging.log.info "--- 2nd Attempt to rename files missed during the first rename - ${rfsLeftOver}"
    if ( useNonStrictPartialRenames ) {
      Logging.log.info "--- Enabling Non-Strict 2nd Pass"
      wrapperArgs.strict = false
      wrapperArgs.file = rfsLeftOver
      rfs = rename(*:wrapperArgs)
    } else {
      rfs = []
      rfsLeftOver.each { fileToRename ->
        rfsTemp = []
        wrapperArgs.file = fileToRename
        rfsTemp = rename(*:wrapperArgs)
        if (rfsTemp) {
          rfs += rfsTemp
        }
      }
    }
    if (rfs) {
      Logging.log.info '--- Successfully Renamed files'
      destinationFiles += rfs
      switch(renamerSource) {
        case ~/filebot/:
          destinationFilesFilebot += rfs
          break
        case ~/script/:
          destinationFilesScript += rfs
          break
      }
      if ( rfs.size() == rfsLeftOver.size() ) {
        Logging.log.info "----- Renamed all ${rfs.size()} files out of ${files.size()}"
        rfsIncomplete = false  as Boolean
        rfsLeftOver = []
      } else {
        Logging.log.info "--- Renamed ${rfs.size()} files out of ${rfsLeftOver.size()}"
        rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
        renameMissedFiles2ndPass += rfsLeftOver
        Logging.log.info "----- Leaving ${rfsLeftOver}"
        rfsIncomplete = false  as Boolean
      }
    } else if (failOnError && rfs == null) {
      Logging.log.info '*****************************'
      Logging.log.info '***  FAILURE! FAILURE!    ***'
      Logging.log.info '*****************************'
      die "Failed to process group: $group"
    } else {
      Logging.log.info "--- Failed to rename any more files from group: $group"
      rfsIncomplete = false  as Boolean
      rfsLeftOver = rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
      renameMissedFiles2ndPass += rfsLeftOver
      switch(renamerSource) {
        case ~/filebot/:
          failedFilesFilebot += renameMissedFiles2ndPass
          break
        case ~/script/:
          failedFilesScript += renameMissedFiles2ndPass
          break
      }
    }
  }
}

// Class/Global Variables Changed/Referenced
// hasSeasonality
// mySeasonalityNumber
// tier1AnimeNames
// tier2AnimeNames
// tier3AnimeNames
// statsTierFilebotNameNull
// statsTierFilebotNameIncluded
// statsGroupsFromFilebot
// statsGroupsFromScript
// statsTier3FilebotNameAdded
// animeDetectedName
ArrayList seriesnameGenerator ( LinkedHashMap group, HashSet baseGeneratedAnimeNames ) {
  Logging.log.info '// START---------- Series Name Generation ---------- //'
  Boolean addGroupAnimeNameToList = true
   baseGeneratedAnimeNames.each { basename ->
     Logging.log.info "//--- Generating Possible Anime Series Names for ${basename}"
     Logging.log.finest "${groupInfoGenerator(group)}"
     Logging.log.finest "${group}"
     // ---------- Does it have the Year?  ---------- //
     if (group.yearDateInName != null) {
       generatedAnimeName = basename + ' ' + group.yearDateInName
       tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
       Logging.log.info "------- Tier1: Adding ${generatedAnimeName}"
       if (group.hasSeriesSyntax && !useBaseAnimeNameWithSeriesSyntax) {
         addGroupAnimeNameToList = false
       }
       switch (group.specialType) {
         case ~/${specialTypeOvaOnaOadWordMatcher}/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- AirDate Syntax  ---------- //
     if ( (group.airdateSeasonNumber != null || group.hasSeasonality)  && !group.hasOrdinalSeasonality ) {
       if ( group.airdateSeasonNumber != null) {
         Logging.log.info '----- Airdate Syntax Detected'
         mySeasonalityNumber = group.airdateSeasonNumber.toInteger()
         hasSeasonality = true
       } else {
         Logging.log.info '----- Seasonality Syntax Detected'
         hasSeasonality = true
         mySeasonalityNumber = group.seasonNumber.toInteger()
       }
       Logging.log.info "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         Logging.log.info "------- Tier1: Adding ${basename} - Season is 0/1"
         tier1AnimeNames += ["${basename}"]
       } else {
         generatedSesonalAnimeNames = [] as HashSet
         addGroupAnimeNameToList = false
         Logging.log.info "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Ordinal Seasonality"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Seasonality Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Series Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         Logging.log.info "------- Seasonal: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         generatedSesonalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         switch (group.specialType) {
           case ~/${specialTypeOvaOnaOadWordMatcher}/:
             // ---------- Add Full list of alternative Season names as options ending with SpecialType (ugh)---------- //
             generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
               Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime II
               Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
               Logging.log.info "------- Tier1: [${group.specialType}]:: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         if ( group.hasPartialSeasonality ) {
           Logging.log.info "------- AND Partial Seasonality Detected (Ugh)"
           hasSeasonality = true
           mySeasonalityNumber = group.partialSeasonNumber.toInteger()
           Logging.log.info "------- mySeasonalityNumber: ${mySeasonalityNumber}"
           if (mySeasonalityNumber > 1) {
             generatedSesonalAnimeNames.each { ordinalAnimeName ->
               // ---------- Add Full list of alternative Season names as options ---------- //
               generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber // anime part 2
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               if ( mySeasonalityNumber < 10 ) {
                 generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
                 Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
                 Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
               //---
               generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber // anime season 2
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber // anime 2
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber // anime s2
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               switch (group.specialType) {
                 case ~/${specialTypeOvaOnaOadWordMatcher}/:
                   // ---------- Add Full list of alternative Season names as options ---------- //
                   generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   if ( mySeasonalityNumber < 10 ) {
                     generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                     Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                     tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                     //---
                     generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                     Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                     tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   }
                   //---
                   generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
             }
           }
         } else {
           // Logging.log.info "generatedOrdinalAnimeNames: $generatedOrdinalAnimeNames"
           generatedSesonalAnimeNames.each { ordinalAnimeName ->
             Logging.log.info "------- Tier1: Adding ${ordinalAnimeName}"
             tier1AnimeNames += ["${ordinalAnimeName}"]
           }
         }
       }
       switch (group.specialType) {
         case ~/${specialTypeOvaOnaOadWordMatcher}/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- Ordinal ---------- //
     if (group.hasOrdinalSeasonality ) {
       Logging.log.info '----- Ordinal Seasonality Syntax Detected'
       hasSeasonality = true
       mySeasonalityNumber = group.ordinalSeasonNumber.toInteger()
       Logging.log.info "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       generatedOrdinalAnimeNames = [] as HashSet
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         Logging.log.info "------- Ordinal: Adding ${basename} - Season is 0/1"
         generatedOrdinalAnimeNames += ["${basename}"]
       } else {
         addGroupAnimeNameToList = false
         Logging.log.info "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Ordinal Seasonality"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) // anime II
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           //---
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         //---
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Seasonality Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Series Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         //---
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         Logging.log.info "------- Ordinal: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( !group.hasPartialSeasonality) {
           switch (group.specialType) {
             case ~/${specialTypeOvaOnaOadWordMatcher}/:
               // ---------- Add Full list of alternative Season names as options and OVA Syntax (ugh)---------- //
               generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               if ( mySeasonalityNumber < 10 ) {
                 generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                 Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = basename + ' ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime II
                 Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Roman Ordinal Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                 Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                 generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               }
               //---
               generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
               Logging.log.info "------- Ordinal: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
               generatedOrdinalAnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           }
         }
       }
       if ( group.hasPartialSeasonality ) {
         Logging.log.info "------- AND Partial Seasonality Detected (Ugh)"
         hasSeasonality = true
         mySeasonalityNumber = group.partialSeasonNumber.toInteger()
         Logging.log.info "------- mySeasonalityNumber: ${mySeasonalityNumber}"
         if (mySeasonalityNumber > 1) {
           generatedOrdinalAnimeNames.each { ordinalAnimeName ->
             // ---------- Add Full list of alternative Season names as options ---------- //
             generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber // anime part 2
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
               Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             //---
             generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber // anime season 2
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber // anime 2
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber // anime s2
             Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             switch (group.specialType) {
               case ~/${specialTypeOvaOnaOadWordMatcher}/:
                 // ---------- Add Full list of alternative Season names as options ---------- //
                 generatedAnimeName = ordinalAnimeName + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 if ( mySeasonalityNumber < 10 ) {
                   generatedAnimeName = ordinalAnimeName + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                   //---
                   generatedAnimeName = ordinalAnimeName + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
                   Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
                   tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 }
                 //---
                 generatedAnimeName = ordinalAnimeName + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
                 //---
                 generatedAnimeName = ordinalAnimeName + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
                 Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
                 tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
           }
         }
       } else {
         // Logging.log.info "generatedOrdinalAnimeNames: $generatedOrdinalAnimeNames"
         generatedOrdinalAnimeNames.each { ordinalAnimeName ->
           Logging.log.info "------- Tier1: Adding ${ordinalAnimeName}"
           tier1AnimeNames += ["${ordinalAnimeName}"]
         }
       }
     }
     // ---------- Partial ---------- //
     if (group.hasPartialSeasonality && !group.hasOrdinalSeasonality && !group.hasSeasonality ) {
       Logging.log.info '----- Partial Seasonality Syntax Detected (NO Ordinal and/or No Season Detected)'
       hasSeasonality = true
       mySeasonalityNumber = group.partialSeasonNumber.toInteger()
       Logging.log.info "------- mySeasonalityNumber: ${mySeasonalityNumber}"
       if ( mySeasonalityNumber == 1 || mySeasonalityNumber == 0 ) {
         Logging.log.info "------- Tier1: Adding ${basename} - Season is 0/1"
         tier1AnimeNames += ["${basename}"]
       } else {
         addGroupAnimeNameToList = false
         Logging.log.info "------- Tier2: Adding ${basename} - Season is 1+"
         tier2AnimeNames += ["${basename}"]
         // ---------- Add Full list of alternative Season names as options ---------- //
         generatedAnimeName = basename + ' part ' + mySeasonalityNumber // anime part 2
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' // anime 2nd part
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' // anime 2nd season
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Ordinal Seasonality"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         if ( mySeasonalityNumber < 10 ) {
           generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' // anime Second season
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - English  Verb Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) // anime part II
           Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
         generatedAnimeName = basename + ' season ' + mySeasonalityNumber // anime season 2
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Seasonality Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' ' + mySeasonalityNumber // anime 2
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Series Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         generatedAnimeName = basename + ' s' + mySeasonalityNumber // anime s2
         Logging.log.info "------- Tier1: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
         tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         switch (group.specialType) {
           case ~/${specialTypeOvaOnaOadWordMatcher}/:
             // ---------- Add Full list of alternative Season names as options ---------- //
             generatedAnimeName = basename + ' part ' + mySeasonalityNumber + ' ' + group.specialType // anime part 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Numerical) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' part' + ' ' + group.specialType // anime 2nd part
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Ordinal) Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + getOrdinalNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime 2nd season
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Ordinal Seasonality"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             if ( mySeasonalityNumber < 10 ) {
               generatedAnimeName = basename + ' ' + getWordNumber(mySeasonalityNumber) + ' season' + ' ' + group.specialType // anime Second season
               Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - English  Verb Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
               //---
               generatedAnimeName = basename + ' part ' + getRomanOrdinal(mySeasonalityNumber) + ' ' + group.specialType // anime part II
               Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Partial (Roman) Seasonality"
               tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             }
             //---
             generatedAnimeName = basename + ' season ' + mySeasonalityNumber + ' ' + group.specialType // anime season 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' ' + mySeasonalityNumber + ' ' + group.specialType // anime 2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Series Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
             //---
             generatedAnimeName = basename + ' s' + mySeasonalityNumber + ' ' + group.specialType // anime s2
             Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName} - Alternative Seasonality Syntax"
             tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
         }
       }
     }
     // ---------- Specials ---------- //
     if ( group.isSpecialEpisode || group.isSpecialType ) {
       Logging.log.info '----- OVA/ONA/OAD/Special Syntax Detected'
       switch (group.specialType) {
         case ~/${specialTypeOvaOnaOadWordMatcher}/:
           generatedAnimeName = basename + ' ' + group.specialType
           tier1AnimeNames += ["${jwdStringBlender(generatedAnimeName)}"]
           Logging.log.info "------- Tier1: [${group.specialType}]: Adding ${generatedAnimeName}"
       }
     }
     // ---------- Checking if Anime Name already in list ---------- //
     if ( tier1AnimeNames.contains("${basename}") ) {
       Logging.log.info "----- Anime name already in list ${basename}"
     } else if (addGroupAnimeNameToList) {
       Logging.log.info "------- Tier1: Adding Group Anime Name to Anime Name List - ${basename}"
       tier1AnimeNames += ["${basename}"]
     }
   }
   // ---------- Show us what we will be searching for ---------- //
//   Logging.log.info "------- Filebot Detected Anime Name: ${animeDetectedName}"
   if ( animeDetectedName != null ) {
     Logging.log.info "------- Filebot Detected Anime Name: ${animeDetectedName}"
     if ( group.isFileBotDetectedName == false ) {
       statsGroupsFromScript++
       fbdetectName = "${jwdStringBlender(animeDetectedName)}"
       if ( hasSeasonality ) {
         Logging.log.info "--------- We have Seasonality, checking Tier 1 and Tier2 lists"
         if ( !tier1AnimeNames.contains("${fbdetectName}") && !tier2AnimeNames.contains("${fbdetectName}") ) {
           Logging.log.info "--------- Tier3: Adding ${fbdetectName} FileBot Detected name to Tier 3 List"
           tier3AnimeNames += animeDetectedName
           statsTier3FilebotNameAdded++
         } else {
           statsTierFilebotNameIncluded++
           Logging.log.info "--------- ${fbdetectName} Already in Tier1 or Tier2 list"
         }
       } else {
         Logging.log.info "--------- Checking Tier 1 list"
         if ( !tier1AnimeNames.contains("${fbdetectName}") ) {
           Logging.log.info "--------- Tier3: Adding ${fbdetectName} FileBot Detected name to Tier 3 List"
           tier3AnimeNames += animeDetectedName
           statsTier3FilebotNameAdded++
         } else {
           statsTierFilebotNameIncluded++
           Logging.log.info "--------- ${fbdetectName} Already in Tier1 list"
         }
       }
     } else {
       Logging.log.info "--------- Do not compute jaroWinklerDistance as Group name is from Filebot"
       statsGroupsFromFilebot++
     }
   } else {
     Logging.log.info "------- Filebot Detected Anime Name: ${animeDetectedName}"
     statsTierFilebotNameNull++
     Logging.log.info "--------- Can't compute jaroWinklerDistance as detected Anime Name is null"
   }
  Logging.log.info '// END---------- Series Name Generation ---------- //'
  return [group]
 }

if ( renameExtras && extraFiles.size() >= 1 ) {
  Logging.log.info ''
  Logging.log.info '***********************************'
  Logging.log.info '***    Start Extras Renaming     ***'
  Logging.log.info '***********************************'
  Logging.log.info ''
  Logging.log.info "   Total Extra Files: ${extraFiles.size()}"
  Logging.log.info '// -------------------------------------- //'
  rename(file:extraFiles, format: extrasFormat, db: 'file')
  Logging.log.info '// -------------------------------------- //'
}

if ( renameInvalid && invalidFiles.size() >= 1 ) {
  Logging.log.info ''
  Logging.log.info '***********************************'
  Logging.log.info '***    Start Invalid File Renaming     ***'
  Logging.log.info '***********************************'
  Logging.log.info ''
  Logging.log.info "   Total Invalid Files: ${invalidFiles.size()}"
  Logging.log.info '// -------------------------------------- //'
  rename(file:invalidFiles, format: invalidFormat, db: 'file')
  Logging.log.info '// -------------------------------------- //'
}
animeDetectedName = ''
renamerSource = 'script' as String
rfsIncomplete = false as Boolean
hasSeasonality = false as Boolean
firstPassOptionsSet = false
secondPassOptionsSet = false
Boolean hasOVAONASyntax = false
animeFoundInTVDB = false
animeFoundInAniDB = false
statsGroupsFromFilebot = 0 as Integer
statsGroupsFromScript = 0 as Integer
statsRenamedUsingFilebot = 0 as Integer
statsRenamedUsingScript = 0 as Integer
statsTVDBJWDFilebotOnly = 0 as Integer
statsANIDBJWDFilebotOnly = 0 as Integer
statsTierFilebotNameIncluded = 0 as Integer
statsTierFilebotNameNull = 0 as Integer
statsTier3FilebotNameAdded = 0 as Integer
statsTVDBFilebotMatchedScript = 0 as Integer
statsANIDBFilebotMatchedScript = 0 as Integer
rfsLeftOver = []
tier1AnimeNames = [] as HashSet
tier2AnimeNames = [] as HashSet
tier3AnimeNames = [] as HashSet
myAniDBOMTitles = [] as HashSet
mySeasonalityNumber = 0
aniDBBanHammer = false as Boolean
Logging.log.info ''
Logging.log.info ''
Logging.log.info '***********************************'
Logging.log.info '***   Start Episode Renaming    ***'
Logging.log.info '***********************************'
Logging.log.info ''
Logging.log.info ''
groupsByManualThreeEpisodes.each { group, files ->
  // ---------- Reset Variables ---------- //
  BigDecimal myMatchNumber = 0.9800000000000000000
  mySeasonalityNumber = 0
  renamerSource = 'script'
  gotAniDBID = 0
  rfsLeftOver = []
  myAniDBOMTitles = []
  anidbJWDResults = [:]
  fileBotAniDBJWDResults = [:]
  baseGeneratedAnimeNames = [] as HashSet
  tier1AnimeNames = [] as HashSet
  animeFoundInAniDB = false
  animeANIDBSearchFound = false
  performRename = true
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
  Set myTBDBSeriesInfoAliasNames = []
  String renameMapper = ''
  String renameQuery = group.anime
  String renameFilter = ''
  Boolean renameStrict = true
  String renameDB = 'AniDB'
  String renameOrder = group.order
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  if (aniDBBanHammer) {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "Stop Processing due to AniDB Ban"
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    return
  }
  if (group.anime == null | group.anime == "") {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "DO NOT PROCESS EMPTY Anime GROUP!"
    files.each { thisfile ->
      Logging.log.info "       [${thisfile.name}, ${thisfile.parent}]"
    }
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    return
  }
  Logging.log.info ''
  Logging.log.info '//-------------------------------------------'
  Logging.log.info "${groupInfoGenerator(group)} => ${files*.name}"
  // ---------- START TV Mode ---------- //
  // ---------- Reset Variables ---------- //
  // TODO
  // Implement actions or options to allow changing the "default" Match threshold
  animeFoundInTVDB = false
  animeTVDBSearchFound = false
  fileBotAniDBMatchUsed = false
  fileBotTheTVDBMatchUsed = false
  thetvdbJWDResults = [:]
  fileBotThetvDBJWDResults = [:]
  tier2AnimeNames = [] as HashSet
  tier3AnimeNames = [] as HashSet
  tempBaseGeneratedAnimeNames = [] as HashSet
  BigDecimal firstTVDBDWTMatchNumber = 0
  BigDecimal secondTVDBDWTMatchNumber = 0
  BigDecimal thirdTVDBDWTMatchNumber = 0
  BigDecimal fileBotTheTVDBJWDMatchNumber = 0
  String firstTVDBDWTMatchName = ''
  String secondTVDBDWTMatchName = ''
  String thirdTVDBDWTMatchName = ''
  LinkedHashMap theTVDBFirstMatchDetails = [:]
  LinkedHashMap theTVDBSecondMatchDetails = [:]
  LinkedHashMap theTVDBThirdMatchDetails = [:]
  LinkedHashMap renameOptions = [isSpecialEpisode: false, isSpecialType: false, isMovieType: false, renameStrict: false, renameQuery: false, renameDB: false, renameOrder: false, renameFilter: false, renameMapper: false]
  LinkedHashMap fileBotTheTVDBJWDMatchDetails = [score: 0.00000000, db:'TheTVDB', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
  Boolean addGroupAnimeNameToList = true
  firstPassOptionsSet = false
  secondPassOptionsSet = false
  thirdPassOptionsSet = false
  animeDetectedName = detectAnimeName(files)
  if ( ! animeDetectedName.equals(group.animeDetectedName) ) {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "Filebot Detected Name:[${animeDetectedName}] differs from Filebot Group Generation:[${group.animeDetectedName}]"
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    Logging.log.info ''
  }
  hasSeriesSyntax = false
  hasRomanSeries = false
  hasSeasonality = false
  hasOrdinalSeasonality = false
  hasPartialSeasonality = false
  isSpecial = false
  hasOVAONASyntax = false
  // ---------- Basename Generation ---------- //
  returnThing = seriesBasenameGenerator(group, useBaseAnimeNameWithSeriesSyntax, seriesBasenameGeneratorOverrideJsonFile)
  Logging.log.finest "returnThing.class:${returnThing.getClass()}"
  Logging.log.finest "/---"
  Logging.log.finest "returnThing:${returnThing}"
  Logging.log.finest "/---"
  Logging.log.finest "groupBEFORE:${group}"
  Logging.log.finest "/---"
  group = returnThing[0] as LinkedHashMap
  Logging.log.finest "groupAFTER:${group}"
  baseGeneratedAnimeNames = returnThing[1] as HashSet
  Logging.log.finest "/---"
  Logging.log.finest "baseGeneratedAnimeNames:${baseGeneratedAnimeNames}"
  // END---------- Basename Generation ---------- //
  // START---------- Series Name Generation ---------- //
  returnThing = seriesnameGenerator(group, baseGeneratedAnimeNames)
  // END---------- Series Name Generation ---------- //
  Logging.log.finest "${groupInfoGenerator(group)}"
  Logging.log.finest "hasSeasonlity:${hasSeasonality}"
  Logging.log.finest "mySeasonalityNumber:${mySeasonalityNumber}"
  Logging.log.info '-----'
  Logging.log.info '-----'
  Logging.log.info "  We are going to be searching for these Anime Series Names: ${tier1AnimeNames} with TheTVDB and AniDB"
  if ( tier2AnimeNames ) {
    Logging.log.info "  We are going to be searching for these Anime Series Names: ${tier2AnimeNames} with TheTVDB"
  }
  if ( tier3AnimeNames ) {
    Logging.log.info "  We are going to be searching for these Anime Series Names: ${tier3AnimeNames} with TheTVDB, AniDB from FileBot"
  }
  Logging.log.info '-----'
  Logging.log.info '-----'
  // ---------- Find "best" matched Name using JWD Matching ---------- //
  // ---------- START with TheTVDB ---------- //
  returnThing = filebotTVDBJWDSearch(tier1AnimeNames, thetvdbJWDResults, animeFoundInTVDB, locale)
  thetvdbJWDResults = returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  Logging.log.finest "thetvdbJWDResults:->${thetvdbJWDResults}"
  returnThing2 = filebotAnidbJWDSearch(tier1AnimeNames, anidbJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  anidbJWDResults = returnThing2.jwdresults
  animeFoundInAniDB = returnThing2.animeFoundInAniDB
  Logging.log.finest "anidbJWDResults:->${anidbJWDResults}"
  // ----------------------- //
  // ---  TIER 2         --- //
  // ----------------------- //
  returnThing = filebotTVDBJWDSearch(tier2AnimeNames, thetvdbJWDResults, animeFoundInTVDB, locale)
  thetvdbJWDResults += returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  Logging.log.finest "thetvdbJWDResults:->${thetvdbJWDResults}"
  // ----------------------- //
  // ---  TIER 3         --- //
  // ----------------------- //
  returnThing = filebotTVDBJWDSearch(tier3AnimeNames, fileBotThetvDBJWDResults, animeFoundInTVDB, locale)
  fileBotThetvDBJWDResults = returnThing.jwdresults
  animeFoundInTVDB = returnThing.animeFoundInTVDB
  Logging.log.finest "fileBotThetvDBJWDResults:->${fileBotThetvDBJWDResults}"
  returnThing2 = filebotAnidbJWDSearch(tier3AnimeNames, fileBotAniDBJWDResults, animeFoundInAniDB, locale, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, animeOfflineDatabase, aniDBCompleteXMLList)
  fileBotAniDBJWDResults = returnThing2.jwdresults
  animeFoundInAniDB = returnThing2.animeFoundInAniDB
  Logging.log.finest "fileBotAniDBJWDResults:->${fileBotAniDBJWDResults}"
  if (animeFoundInTVDB) {
    theTVDBFirstMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value } // works.
     Logging.log.finest "${thetvdbJWDResults}"
     Logging.log.finest "${theTVDBFirstMatchDetails}"
     Logging.log.finest "${fileBotThetvDBJWDResults}"
    if ( theTVDBFirstMatchDetails == null ) {
      statsTVDBJWDFilebotOnly++
      Logging.log.info "//--- ONLY Filebot Anime Name Matched something in TheTVDB ---///"
      fileBotTheTVDBMatchUsed = true
      theTVDBFirstMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
      theTVDBSecondMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      theTVDBThirdMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
      firstTVDBDWTMatchNumber = theTVDBFirstMatchDetails.score
    } else {
      firstTVDBDWTMatchNumber = theTVDBFirstMatchDetails.score
      firstTVDBDWTMatchName = theTVDBFirstMatchDetails.primarytitle
      theTVDBSecondMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      theTVDBThirdMatchDetails = thetvdbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    }
    if ( theTVDBSecondMatchDetails != null ) {
      secondTVDBDWTMatchNumber = theTVDBSecondMatchDetails.score
      secondTVDBDWTMatchName = theTVDBSecondMatchDetails.primarytitle
    }
    if ( theTVDBThirdMatchDetails != null ) {
      thirdTVDBDWTMatchNumber = theTVDBThirdMatchDetails.score
      thirdTVDBDWTMatchName = theTVDBThirdMatchDetails.primarytitle
    }
    // This was just easier for "Sorting" the 1st/2nd with higher DBID in 1st
    if ( (firstTVDBDWTMatchNumber == 1 && secondTVDBDWTMatchNumber == 1) && (theTVDBFirstMatchDetails.dbid < theTVDBSecondMatchDetails.dbid ) ) {
      Logging.log.info "//---- Switch 1st/2nd TVDB"
      tmpTVDBWTMatchNumber = secondTVDBDWTMatchNumber
      tmpTVDBWTMatchName = secondTVDBDWTMatchName
      tmpTVDBMatchDetails = theTVDBSecondMatchDetails
      secondTVDBDWTMatchNumber = firstTVDBDWTMatchNumber
      secondTVDBDWTMatchName = firstTVDBDWTMatchName
      theTVDBSecondMatchDetails = theTVDBFirstMatchDetails
      firstTVDBDWTMatchNumber = tmpTVDBWTMatchNumber
      firstTVDBDWTMatchName = tmpTVDBWTMatchName
      theTVDBFirstMatchDetails = tmpTVDBMatchDetails
    }
    Logging.log.info "firstTVDBDWTMatchNumber: ${firstTVDBDWTMatchNumber}"
    Logging.log.info "firstTVDBDWTMatchName: ${firstTVDBDWTMatchName}"
    Logging.log.info "theTVDBFirstMatchDetails: ${theTVDBFirstMatchDetails}"
    Logging.log.info "secondTVDBDWTMatchNumber: ${secondTVDBDWTMatchNumber}"
    Logging.log.info "secondTVDBDWTMatchName: ${secondTVDBDWTMatchName}"
    Logging.log.info "theTVDBSecondMatchDetails: ${theTVDBSecondMatchDetails}"
    Logging.log.info "thirdTVDBDWTMatchNumber: ${thirdTVDBDWTMatchNumber}"
    Logging.log.info "thirdTVDBDWTMatchName: ${thirdTVDBDWTMatchName}"
    Logging.log.info "theTVDBThirdMatchDetails: ${theTVDBThirdMatchDetails}"
    if ( fileBotThetvDBJWDResults ) {
      if ( fileBotTheTVDBMatchUsed ) {
        fileBotTheTVDBJWDMatchDetails = [score: 0.00000000, db:'TheTVDB', dbid:0, primarytitle: null, animename: null, matchname: null, alias: true]
        fileBotTheTVDBJWDMatchNumber = 0
      } else {
         Logging.log.finest "fileBotThetvDBJWDResults: ${fileBotThetvDBJWDResults}"
        fileBotTheTVDBJWDMatchDetails = fileBotThetvDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
        fileBotTheTVDBJWDMatchNumber = fileBotTheTVDBJWDMatchDetails.score
        if ( fileBotTheTVDBJWDMatchDetails.dbid == theTVDBFirstMatchDetails.dbid ) {
          statsTVDBFilebotMatchedScript++
        }
        Logging.log.info ""
        Logging.log.info "fileBotTheTVDBJWDMatchNumber: ${fileBotTheTVDBJWDMatchNumber}"
        Logging.log.info "fileBotTheTVDBJWDMatchDetails: ${fileBotTheTVDBJWDMatchDetails}"
      }
    }
    Logging.log.info ''
  } else {
    Logging.log.info '//-------------------------------------------//'
    Logging.log.info "Nothing was found for ${group.anime} in TheTVDB"
    Logging.log.info '//-------------------------------------------//'
    firstTVDBDWTMatchNumber = 0
  }
  if (animeFoundInAniDB) {
    anidbFirstMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
    if ( anidbFirstMatchDetails == null ) {
      statsANIDBJWDFilebotOnly++
      Logging.log.info "//--- ONLY Filebot Anime Name Matched something in ANIDB ---///"
      fileBotAniDBMatchUsed = true
      anidbFirstMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score
      anidbSecondMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      anidbThirdMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    } else {
      firstANIDBWTMatchNumber = anidbFirstMatchDetails.score
      firstAniDBWTMatchName = anidbFirstMatchDetails.primarytitle
      anidbSecondMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(1).take(1).findResult { it.value }
      anidbThirdMatchDetails = anidbJWDResults.sort { a, b -> b.value.score <=> a.value.score }.drop(2).take(1).findResult { it.value }
    }
    if ( anidbSecondMatchDetails != null ) {
      secondANIDBWTMatchNumber = anidbSecondMatchDetails.score
      secondAniDBWTMatchName = anidbSecondMatchDetails.primarytitle
    }
    if ( anidbThirdMatchDetails != null ) {
      thirdANIDBWTMatchNumber = anidbThirdMatchDetails.score
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
    if ( fileBotAniDBJWDResults ) {
      if ( fileBotAniDBMatchUsed ) {
        fileBotANIDBJWDMatchDetails = null
        fileBotANIDBJWDMatchNumber = 0
      } else {
        fileBotANIDBJWDMatchDetails = fileBotAniDBJWDResults.sort { a, b -> b.value.score <=> a.value.score }.take(1).findResult { it.value }
        fileBotANIDBJWDMatchNumber = fileBotANIDBJWDMatchDetails.score
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
  if (!animeFoundInAniDB && !animeFoundInTVDB) {
    Logging.log.info "Since we can't find anything, we should skip.."
    performRename = false
    rfsIncomplete = false as Boolean
  }
  // ------------------------------ //
  // ---------- 1st Pass ---------- //
  // ------------------------------ //
  if ( performRename ) {
    returnThing = episodeRenameOptionPassOne(1, group, files, hasSeasonality, mySeasonalityNumber, firstANIDBWTMatchNumber, secondANIDBWTMatchNumber, thirdANIDBWTMatchNumber, fileBotANIDBJWDMatchNumber, anidbFirstMatchDetails, anidbSecondMatchDetails, anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails, firstTVDBDWTMatchNumber, secondTVDBDWTMatchNumber, thirdTVDBDWTMatchNumber, fileBotTheTVDBJWDMatchNumber, theTVDBFirstMatchDetails, theTVDBSecondMatchDetails, theTVDBThirdMatchDetails, fileBotTheTVDBJWDMatchDetails, performRename, fileBotAniDBMatchUsed, animeFoundInAniDB, animeFoundInTVDB, fileBotTheTVDBMatchUsed, statsRenamedUsingScript, statsRenamedUsingFilebot, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, animeOfflineDatabase, useNonStrictOnTVDBSpecials)
    groupByRenameOptions = returnThing.groupByRenameOptions
    statsRenamedUsingScript = returnThing.statsRenamedUsingScript
    statsRenamedUsingFilebot = returnThing.statsRenamedUsingFilebot
    // ---------- 1st pass Renaming---------- //
    groupByRenameOptions.each { groupRenameOptions, renameFiles ->
      performRename = groupRenameOptions.performRename
      renameOptions.renameStrict = groupRenameOptions.renameStrict
      renameOptions.renameQuery = groupRenameOptions.renameQuery
      renameOptions.renameDB = groupRenameOptions.renameDB
      renameOptions.renameOrder = groupRenameOptions.renameOrder
      renameOptions.renameFilter = groupRenameOptions.renameFilter
      renameOptions.renameMapper = groupRenameOptions.renameMapper
      renameOptions.isSpecialEpisode = groupRenameOptions.isSpecialEpisode
      renameOptions.isSpecialType = groupRenameOptions.isSpecialType
      renameOptions.isMovieType = groupRenameOptions.isMovieType
      // ---------- Stupid filename/lookup handling aka OVERRIDES---------- //
      if (renameOptions.renameQuery == '9517' && renameOptions.renameDB == 'AniDB' && hasSeasonality && mySeasonalityNumber == 1 ) {
        // The current Regex process used removes all `, which is a problem for those series that END in ` and use multiple ` to denote different seasons
        // aka Dog Days` and Dog Days``. So until I can figure out how to not replace ` when it's at the end .. Need an override ..
        Logging.log.info "--- Dog Days Season 1 override"
        renameOptions.renameQuery = '8206'
      }
      if (renameOptions.renameQuery == '7432' && renameOptions.renameOrder == 'Absolute') {
        // Mirai Nikki (2011) is the actual Season name in AniDB, however there *IS* a single episode OVA named Mirai Nikki .. Groups seem to forget about it ..
        // Mirai Nikki on TheTVDB is an alias, so it probably didn't trip the logic to use TheTVDB despite the 1.0 match (since it's an alias)
        // We will just switch it then to TheTVDB :)
        Logging.log.info "--- TVDB: Complete: Mirai Nikki (AniDB: 7432) override"
        renameOptions.renameQuery = theTVDBFirstMatchDetails.dbid
        renameOptions.renameDB = 'TheTVDB'
        renameOptions.renameOrder = 'Airdate'
        renameOptions.renameStrict = true
        renameOptions.renameFilter = ''
        //        renameMapper = '[AnimeList.AniDB, episode]'
        renameOptions.renameMapper = 'any {AnimeList.AniDB}, {order.absolute.episode}, {episode},  {XEM.AniDB}'
      }
      // TheTVDB has a Ikebukuro West Gate Park  from 2000, but AniDB/MAL doesn't .. because it's not Anime (sigh).. AniDB doesn't have this problem.
      if (renameOptions.renameQuery == '82348' && renameOptions.renameDB == 'TheTVDB') {
        Logging.log.info "--- TVDB: renameQuery: : Ikebukuro West Gate Park override"
        renameOptions.renameQuery = '381769'
      }
      // Noblesse matches the wrong Series .. of the exact same name! (WTF TheTVDB?). AniDB doesn't have this problem.
      if (renameOptions.renameQuery == '327859' && renameOptions.renameDB == 'TheTVDB' ) {
        Logging.log.info "--- TVDB: renameQuery: Noblesse override"
        renameOptions.renameQuery = '386818'
      }
      // Nisekoi (AID:9903) vs Nisekoi: (AID:10859)
      if (renameOptions.renameQuery == '275670' && renameOptions.renameDB == 'TheTVDB' && !hasSeasonality) {
        Logging.log.info "--- TVDB: renameFilter: Nisekoi override"
        renameOptions.renameFilter = 's == 1'
      }
      if (!performRename && renameOptions.isMovieType) {
        Logging.log.info "--- MOVIE Detected, Moving to Movie Processing Stage"
        groupsByManualThreeMovies += [(group): renameFiles]
      }
      if ( performRename ) {
        Logging.log.info '// -------------------------------------- //'
        Logging.log.info '// ---------- 1st pass Renaming---------- //'
        Logging.log.info "group: ${group}, files: ${renameFiles}"
        Logging.log.info "renameStrict: ${renameOptions.renameStrict}, renameQuery: ${renameOptions.renameQuery}, renameDB: ${renameOptions.renameDB}, renameOrder: ${renameOptions.renameOrder}, renameFilter: ${renameOptions.renameFilter}, renameMapper: ${renameOptions.renameMapper}"
        renameWrapper(group, renameFiles, renameOptions )
        Logging.log.info "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
        Logging.log.info '// -------------------------------------- //'
      }
    }
  }
  rfsLeftOver = files.getFiles { it.isFile() && it.isVideo() }
  Logging.log.info "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
  // ------------------------------ //
  // ---------- 2nd Pass ---------- //
  // ------------------------------ //
  // ---------- Setup 2nd Pass Options for Specific "types" ---------- //
  if ( performRename && rfsLeftOver.size() >= 1 ) {
    sleep(2000) // Pause 2 seconds in between Stages
    returnThing = episodeRenameOptionPassOne(2, group, rfsLeftOver, hasSeasonality, mySeasonalityNumber, firstANIDBWTMatchNumber, secondANIDBWTMatchNumber, thirdANIDBWTMatchNumber, fileBotANIDBJWDMatchNumber, anidbFirstMatchDetails, anidbSecondMatchDetails, anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails, firstTVDBDWTMatchNumber, secondTVDBDWTMatchNumber, thirdTVDBDWTMatchNumber, fileBotTheTVDBJWDMatchNumber, theTVDBFirstMatchDetails, theTVDBSecondMatchDetails, theTVDBThirdMatchDetails, fileBotTheTVDBJWDMatchDetails, performRename, fileBotAniDBMatchUsed, animeFoundInAniDB, animeFoundInTVDB, fileBotTheTVDBMatchUsed, statsRenamedUsingScript, statsRenamedUsingFilebot, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, animeOfflineDatabase, useNonStrictOnTVDBSpecials)
    groupByRenameOptions = returnThing.groupByRenameOptions
    statsRenamedUsingScript = returnThing.statsRenamedUsingScript
    statsRenamedUsingFilebot = returnThing.statsRenamedUsingFilebot
    // ---------- 2nd pass Renaming---------- //
    groupByRenameOptions.each { groupRenameOptions, renameFiles ->
      performRename = groupRenameOptions.performRename
      renameOptions.renameStrict = groupRenameOptions.renameStrict
      renameOptions.renameQuery = groupRenameOptions.renameQuery
      renameOptions.renameDB = groupRenameOptions.renameDB
      renameOptions.renameOrder = groupRenameOptions.renameOrder
      renameOptions.renameFilter = groupRenameOptions.renameFilter
      renameOptions.renameMapper = groupRenameOptions.renameMapper
      renameOptions.isSpecialEpisode = groupRenameOptions.isSpecialEpisode
      renameOptions.isSpecialType = groupRenameOptions.isSpecialType
      renameOptions.isMovieType = groupRenameOptions.isMovieType
      if (!performRename && renameOptions.isMovieType) {
        Logging.log.info "--- MOVIE Detected, Moving to Movie Processing Stage"
        groupsByManualThreeMovies += [(group): renameFiles]
      }
      if ( performRename ) {
        Logging.log.info '// -------------------------------------- //'
        Logging.log.info '// ---------- 2nd pass Renaming---------- //'
        Logging.log.info "group: ${group}, files: ${renameFiles}"
        Logging.log.info "renameStrict: ${renameOptions.renameStrict}, renameQuery: ${renameOptions.renameQuery}, renameDB: ${renameOptions.renameDB}, renameOrder: ${renameOptions.renameOrder}, renameFilter: ${renameOptions.renameFilter}, renameMapper: ${renameOptions.renameMapper}"
        renameWrapper(group, renameFiles, renameOptions )
        Logging.log.info "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
        Logging.log.info '// -------------------------------------- //'
        rfsLeftOver = files.getFiles { it.isFile() }
        if (rfsLeftOver.size() >= 1 && renameOptions.isMovieType) {
          Logging.log.info "--- MOVIE Detected, Moving to Movie Processing Stage"
          groupsByManualThreeMovies += [(group): renameFiles]
        }
      }
    }
  }

  // ------------------------------ //
  // ---------- 3rd Pass ---------- //
  // ------------------------------ //
  if ( performRename && rfsLeftOver.size() >= 1 ) {
    sleep(2000) // Pause 2 seconds in between Stages
    returnThing = episodeRenameOptionPassOne(3, group, rfsLeftOver, hasSeasonality, mySeasonalityNumber, firstANIDBWTMatchNumber, secondANIDBWTMatchNumber, thirdANIDBWTMatchNumber, fileBotANIDBJWDMatchNumber, anidbFirstMatchDetails, anidbSecondMatchDetails, anidbThirdMatchDetails, fileBotANIDBJWDMatchDetails, firstTVDBDWTMatchNumber, secondTVDBDWTMatchNumber, thirdTVDBDWTMatchNumber, fileBotTheTVDBJWDMatchNumber, theTVDBFirstMatchDetails, theTVDBSecondMatchDetails, theTVDBThirdMatchDetails, fileBotTheTVDBJWDMatchDetails, performRename, fileBotAniDBMatchUsed, animeFoundInAniDB, animeFoundInTVDB, fileBotTheTVDBMatchUsed, statsRenamedUsingScript, statsRenamedUsingFilebot, useNonStrictOnAniDBFullMatch, useNonStrictOnAniDBSpecials, animeOfflineDatabase, useNonStrictOnTVDBSpecials)
    groupByRenameOptions = returnThing.groupByRenameOptions
    statsRenamedUsingScript = returnThing.statsRenamedUsingScript
    statsRenamedUsingFilebot = returnThing.statsRenamedUsingFilebot
    // ---------- 3rd pass Renaming---------- //
    groupByRenameOptions.each { groupRenameOptions, renameFiles ->
      performRename = groupRenameOptions.performRename
      renameOptions.renameStrict = groupRenameOptions.renameStrict
      renameOptions.renameQuery = groupRenameOptions.renameQuery
      renameOptions.renameDB = groupRenameOptions.renameDB
      renameOptions.renameOrder = groupRenameOptions.renameOrder
      renameOptions.renameFilter = groupRenameOptions.renameFilter
      renameOptions.renameMapper = groupRenameOptions.renameMapper
      renameOptions.isSpecialEpisode = groupRenameOptions.isSpecialEpisode
      renameOptions.isSpecialType = groupRenameOptions.isSpecialType
      renameOptions.isMovieType = groupRenameOptions.isMovieType
      if (!performRename && renameOptions.isMovieType) {
        Logging.log.info "--- MOVIE Detected, Moving to Movie Processing Stage"
        groupsByManualThreeMovies += [(group): renameFiles]
      }
      if ( performRename ) {
        Logging.log.info '// -------------------------------------- //'
        Logging.log.info '// ---------- 3rd pass Renaming---------- //'
        Logging.log.info "group: ${group}, files: ${renameFiles}"
        Logging.log.info "renameStrict: ${renameOptions.renameStrict}, renameQuery: ${renameOptions.renameQuery}, renameDB: ${renameOptions.renameDB}, renameOrder: ${renameOptions.renameOrder}, renameFilter: ${renameOptions.renameFilter}, renameMapper: ${renameOptions.renameMapper}"
        renameWrapper(group, renameFiles, renameOptions )
        Logging.log.info "\t rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
        Logging.log.info '// -------------------------------------- //'
        rfsLeftOver = files.getFiles { it.isFile() }
        if (rfsLeftOver.size() >= 1 && renameOptions.isMovieType) {
          Logging.log.info "--- MOVIE Detected, Moving to Movie Processing Stage"
          groupsByManualThreeMovies += [(group): renameFiles]
        }
      }
    }
  }

  // ------------------------------ //
  // ---------- 4th Pass ---------- //
  // ------------------------------ //

  // ---------- Stat Collection -----------//
  if ( rfsLeftOver ) {
    partialFiles += rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
  }
  if ( rfsIncomplete ) {
    unsortedFiles += files.getFiles { it.isFile() && it.isVideo() }
  }
  // ---------- FINISH TV Mode ---------- //
}

Logging.log.info ''
Logging.log.info '***********************************'
Logging.log.info '***    Start Movie Renaming     ***'
Logging.log.info '***********************************'
Logging.log.info ''
groupsByManualThreeMovies.each { group, files ->
  // ---------- Reset Variables ---------- //
  BigDecimal myMatchNumber = 0.9800000000000000000
  renamerSource = 'script'
  gotAniDBID = 0
  rfsLeftOver = []
  myAniDBOMTitles = []
  anidbJWDResults = [:]
  fileBotAniDBJWDResults = [:]
  baseGeneratedAnimeNames = [] as HashSet
  tier1AnimeNames = [] as HashSet
  animeFoundInAniDB = false
  animeANIDBSearchFound = false
  performRename = true
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
  LinkedHashMap fileBotANIDBJWDMatchDetails = [score: 0.00000000, db: 'AniDB', dbid: 0, primarytitle: null, animename: null, matchname: null, alias: true]
  Set myTBDBSeriesInfoAliasNames = []
  String renameMapper = ''
  String renameQuery = group.anime
  String renameFilter = ''
  Boolean renameStrict = true
  String renameDB = 'AniDB'
  String renameOrder = group.order
  JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance()
  firstPassOptionsSet = false
  if (aniDBBanHammer) {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "Stop Processing due to AniDB Ban"
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    return
  }
  if (skipMovies) {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "Stop Processing due to skipMovies"
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    return
  }
  if (group.anime == null | group.anime == "") {
    Logging.log.info ''
    Logging.log.info '//----------------'
    Logging.log.info '//-------------------------------------------'
    Logging.log.info "DO NOT PROCESS EMPTY Anime GROUP!"
    files.each { thisfile ->
      Logging.log.info "       [${thisfile.name}, ${thisfile.parent}]"
    }
    Logging.log.info '//-------------------------------------------'
    Logging.log.info '//----------------'
    return
  }
  Logging.log.info ''
  Logging.log.info '//-------------------------------------------'
  Logging.log.info "${groupInfoGenerator(group)} => ${files*.name}"
  Logging.log.fine "${group}"
  // ---------- START Movie Mode ---------- //
  returnThing = searchForMoviesJWD(group, aniDBTitleXMLFilename, aniDBSynonymXMLFilename, useFilebotAniDBAliases, locale, animeOfflineDatabase, aniDBCompleteXMLList, moviesBasenameGeneratorOverrideJsonFile)
  animeFoundInAniDB = returnThing.animeFoundInAniDB
  firstANIDBWTMatchNumber = returnThing.firstANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  firstAniDBWTMatchName = returnThing.firstAniDBWTMatchName
  anidbFirstMatchDetails = returnThing.anidbFirstMatchDetails
  //noinspection GroovyUnusedAssignment
  secondANIDBWTMatchNumber = returnThing.secondANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  secondAniDBWTMatchName = returnThing.secondAniDBWTMatchName
  //noinspection GroovyUnusedAssignment
  anidbSecondMatchDetails = returnThing.anidbSecondMatchDetails
  //noinspection GroovyUnusedAssignment
  thirdANIDBWTMatchNumber = returnThing.thirdANIDBWTMatchNumber
  //noinspection GroovyUnusedAssignment
  thirdAniDBWTMatchName = returnThing.thirdAniDBWTMatchName
  //noinspection GroovyUnusedAssignment
  anidbThirdMatchDetails = returnThing.anidbThirdMatchDetails
  fileBotANIDBJWDMatchNumber = returnThing.fileBotANIDBJWDMatchNumber
  fileBotANIDBJWDMatchDetails = returnThing.fileBotANIDBJWDMatchDetails
  if (!animeFoundInAniDB) {
    Logging.log.info "Since we can't find anything, we should skip.."
    performRename = false
    rfsIncomplete = false as Boolean
  }
  Logging.log.info '// ---------- deliberations on order, DB, filter ---------- //'
  // --- airdate Syntax --- //
  if (performRename) {
    sleep (2000) // Pause 2 seconds (minimum) between renames
    if (firstANIDBWTMatchNumber > 0.9800000000000000000) {
      Logging.log.info '------- 1st AniDB match 0.98+'
      Logging.log.info '--------- We are going to try 1st AniDB match'
      firstPassOptionsSet = true
      renameQuery = anidbFirstMatchDetails.dbid
      renameDB = 'AniDB'
      renameOrder = 'Absolute'
      if (useNonStrictOnAniDBMovies) {
        renameStrict = false
      } else {
        renameStrict = true
      }
      renameFilter = ''
      renameMapper = ''
      Logging.log.info "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    }
    if (firstANIDBWTMatchNumber < 0.9800000000000000000 && fileBotANIDBJWDMatchNumber > 0.9800000000000000000) {
      Logging.log.info '------- 1st AniDB match < 0.98 and Filebot 0.98+'
      Logging.log.info '--------- We are going to try Filebot match'
      firstPassOptionsSet = true
      renameQuery = fileBotANIDBJWDMatchDetails.dbid
      renameDB = 'AniDB'
      renameOrder = 'Absolute'
      if (useNonStrictOnAniDBMovies) {
        renameStrict = false
      } else {
        renameStrict = true
      }
      renameFilter = ''
      renameMapper = ''
      Logging.log.info "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    }
    if (!firstPassOptionsSet) {
      Logging.log.info '//-----------------------------//'
      Logging.log.info '//  STOP - Movie.1-1st.1      //'
      Logging.log.info '//-----------------------------//'
      performRename = false
//      firstPassOptionsSet = true
    }
  }
  // ---------- Start Renaming---------- //
  if (performRename) {
    Logging.log.info '// -------------------------------------- //'
    Logging.log.info '// ---------- 1st pass Renaming---------- //'
    Logging.log.info "group: ${group}, files: ${files}"
    Logging.log.info "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
    renameMovieWrapper(group, files, renameStrict, renameQuery, renameDB, renameOrder, renameFilter, renameMapper)
    Logging.log.info "rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
    Logging.log.info '// -------------------------------------- //'
  }

  // ---------- IMDB/TMDB Fallback---------- //
  Logging.log.fine "enableFileBotMovieLookups: ${enableFileBotMovieLookups}"
  Logging.log.fine "rfsLeftOver: ${rfsLeftOver}"
  Logging.log.fine "rfsIncomplete: ${rfsIncomplete}"
  Logging.log.fine "firstPassOptionsSet: ${firstPassOptionsSet}"
  Logging.log.fine "useNonStrictOnAniDBMovies: ${useNonStrictOnAniDBMovies}"
  Logging.log.fine "group.filebotMovieTitle: ${group.filebotMovieTitle}"
  if (enableFileBotMovieLookups && (rfsLeftOver || rfsIncomplete || !firstPassOptionsSet ) && useNonStrictOnAniDBMovies && group.filebotMovieTitle != null) {
    Logging.log.info '// ---------- Filebot FallBack Start---------- //'
    renameQuery = filebotMovieFallBack(group, animeListsXML)
    if (renameQuery != null) {
      renameDB = 'AniDB'
      renameOrder = 'Absolute'
      renameFilter = ''
      renameMapper = ''
      renameStrict = false
      Logging.log.info '// ---------------------------------------------- //'
      Logging.log.info '// ---------- Filebot FallBack Renaming---------- //'
      Logging.log.info "// ----------    Using AID: ${renameQuery} ------ //"
      Logging.log.info "group: ${group}, files: ${files}"
      Logging.log.info "renameStrict: ${renameStrict}, renameQuery: ${renameQuery}, renameDB: ${renameDB}, renameOrder: ${renameOrder}, renameFilter: ${renameFilter}, renameMapper: ${renameMapper}"
      renameMovieWrapper(group, files, renameStrict, renameQuery, renameDB, renameOrder, renameFilter, renameMapper)
      Logging.log.info "rfsLeftOver: ${rfsLeftOver}, rfsIncomplete: ${rfsIncomplete}"
      Logging.log.info '// -------------------------------------- //'
    }
  }

  // ---------- Stat Collection -----------//
  if ( rfsLeftOver ) {
    partialFiles += rfsLeftOver.getFiles { it.isFile() && it.isVideo() }
  }
  if ( rfsIncomplete ) {
    unsortedFiles += files.getFiles { it.isFile() && it.isVideo() }
  }
}








// ---------- END Renaming ---------- //
Logging.log.info '// ---------- END Renaming ---------- //'
Logging.log.info "Total Files: ${input.size()-1}"
Logging.log.info "   Processed files: ${destinationFiles.size()}"
renameLog.sort{it.value.metadata}.each { from, to ->
  Logging.log.info "       [$to.metadata];[${to.name}];[${from.name}];[${to.parent}]"
}
Logging.log.info '// ----------             ---------- //'
Logging.log.info "      Script Name Renamed files: ${destinationFilesScript.size()}"
destinationFilesScript.each { file ->
  Logging.log.info "       [${file.name}, ${file.parent}]"
}
Logging.log.info '// ----------             ---------- //'
Logging.log.info "      Filebot Name Renamed files: ${destinationFilesFilebot.size()}"
destinationFilesFilebot.each { file ->
  Logging.log.info "       [${file.name}, ${file.parent}]"
}
Logging.log.info '// ----------             ---------- //'
Logging.log.info "   1st Pass Rename Missed Files: ${renameMissedFiles1stPass.size()}"
Logging.log.info "   2nd Pass Rename Missed Files: ${renameMissedFiles2ndPass.size()}"
Logging.log.info "   Total Rename failure: ${unsortedFiles.size()}"
unsortedFiles.each { file ->
  Logging.log.info "       [${file.name}, ${file.parent}]"
}
Logging.log.info '// ----------             ---------- //'
Logging.log.info "Stats:"
Logging.log.info "   Tier Names included Filebot Detected Anime Name:[${statsTierFilebotNameIncluded}]"
Logging.log.info "   TVDB Filebot JWD Matched 1st TVDB JWD:[${statsTVDBFilebotMatchedScript}]"
Logging.log.info "   AniDB Filebot JWD Matched 1st AniDB JWD:[${statsANIDBFilebotMatchedScript}]"
Logging.log.info "   Tier 3 Filebot Detected Anime Name used:[${statsTier3FilebotNameAdded}]"
Logging.log.info "   Tier Filebot Detected Anime Name null:[${statsTierFilebotNameNull}]"
Logging.log.info "-----"
Logging.log.info "   Groups using Script Name:[${statsGroupsFromScript}]"
Logging.log.info "   Rename actions using Script JWD:[${statsRenamedUsingScript}]"
Logging.log.info "   Script Rename Failure:[${failedFilesScript.size()}]"
Logging.log.info "-----"
Logging.log.info "   Groups using Filebot Name:[${statsGroupsFromFilebot}]"
Logging.log.info "   TVDB JWD found only from Filebot:[${statsTVDBJWDFilebotOnly}]"
Logging.log.info "   AniDB JWD found only from Filebot:[${statsANIDBJWDFilebotOnly}]"
Logging.log.info "   Rename actions using Filebot JWD:[${statsRenamedUsingFilebot}]"
Logging.log.info "   Filebot Rename Failure:[${failedFilesFilebot.size()}]"

// abort and skip clean-up logic if we didn't process any files
if (destinationFiles.size() == 0) {
  Logging.log.info 'Finished without processing any files'
}