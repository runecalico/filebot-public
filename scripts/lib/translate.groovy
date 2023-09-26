import net.filebot.Logging
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Translates a text from one language to another using the Google Translate API.
 *
 * @param langFrom     The source language code (e.g., "en" for English).
 * @param langTo       The target language code (e.g., "es" for Spanish).
 * @param text         The text to be translated.
 * @param translateUrl The URL of the Google Translate API endpoint.
 * @return A String containing the translated text.
 * @throws IOException If an I/O exception occurs while making the HTTP request or reading the response.
 */
String googleTranslate(String langFrom, String langTo, String text, String translateUrl) throws IOException {
  String urlStr =  translateUrl +
      "?q=" + URLEncoder.encode(text, "UTF-8") +
      "&target=" + langTo +
      "&source=" + langFrom;
  URL url = new URL(urlStr);
  StringBuilder response = new StringBuilder();
  HttpURLConnection myHttpConnection = (HttpURLConnection) url.openConnection();
  myHttpConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
  BufferedReader incomingData = new BufferedReader(new InputStreamReader(myHttpConnection.getInputStream()));
  String inputLine;
  while ((inputLine = incomingData.readLine()) != null) {
    response.append(inputLine);
  }
  incomingData.close();
  return response.toString();
}

/**
 * Cleans and sanitizes a filename obtained from Google Translate to remove unusual characters.
 *
 * @param filename The filename to be cleaned.
 * @return A cleaned and sanitized version of the filename.
 */
String cleanGoogleTranslateText(String filename) {
  String urlDecoded = URLDecoder.decode(filename, "UTF-8")
  // Define the regular expression pattern to match the unusual characters
  Pattern pattern = Pattern.compile("[^A-Za-z0-9_\\-\\.\\(\\)\\[\\]\\s\\?\\!]+");

  // Replace the unusual characters with an empty string
  Matcher matcher = pattern.matcher(urlDecoded);
  String cleanFilename = matcher.replaceAll("");

  return cleanFilename;
}

/**
 * Checks and renames files in a list if they contain CJK (Chinese, Japanese, Korean) characters in their filenames.
 *
 * @param myList The list of files to check and potentially rename.
 */
void checkRenameCJK(List myList, File outputFolder) {
  myList.each { File f ->
    String renameFormat = ""
    String filename = f.name
    String fileType = f.isDirectory() ? "directory" : "file"
    Logging.log.info "Checking ${fileType} ${filename}"
    if ( detectCJKCharacters(filename)) {
      String translatedText = ""
      Logging.log.info "...CJK Language detected"
      Logging.log.info ".....Send to Google Translate"
      Boolean notTranslated = true
      Integer maxRetries = 3
      Integer retryCount = 0
      while (notTranslated && retryCount < maxRetries) {
        try {
          translatedText = cleanGoogleTranslateText(StringUtils.stripAccents(googleTranslate("", "en", filename, translateUrl)).replaceAll(/(?<=\S)-(?!Global)(?=\S)/, ' - '))
        } catch (Exception e) {
          Logging.log.warning("Google Translate Failed")
          Logging.log.warning ".....${e.message}"
        }
        if (translatedText.length() > 0) {
          retryCount++
          sleep(5000) // 5 seconds
        } else {
          notTranslated = false
        }
      }
      Logging.log.info ".......Translated text: ${translatedText}"
      Logging.log.finest "\t getName: ${f.getName()}" // Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
      Logging.log.finest "\t parentFolder: ${f.dir}"
      Logging.log.finest "\t getNameCount of outputDir: [${f.dir.toPath().getNameCount()}] - ${f.dir.toPath()}:"
      // [3] - W:\2-AniAdd\AniAddSortedSeries\Ani ni Tsukeru Kusuri wa Nai! 4 [anidb-15600]
      def myNameCount = f.dir.toPath().getNameCount()
      Logging.log.finest "\t getNameCount of outputFolder: [${outputFolder.toPath().getNameCount()}] - ${outputFolder}"
      // [2] - W:\2-AniAdd\AniAddSortedSeries
      def myArgNameCount = outputFolder.toPath().getNameCount()
      if ( myArgNameCount == myNameCount ) {
        Logging.log.finest "\t $f.name is in same folder as outputFolder"
        renameFormat = "{ \'${translatedText}\' }"
      } else {
        def myRelativePath = FilenameUtils.separatorsToUnix(f.toPath().subpath(myArgNameCount, myNameCount).toString())
        log.finest "\t Relative path of directory to outputFolder using subpath: ${myRelativePath}"
        // winter\Youkai Watch Jam Youkai Gakuen Y - N to no Souguu [anidb-15268]
        renameFormat = "{ \'${myRelativePath}/${translatedText}\' }"
      }
      Logging.log.finest "\t \t InputFiles: ${f}"
      Logging.log.info   "\t renameFormat: ${renameFormat}"
      Logging.log.info   "\t rename(file: ${f}, format: renameFormat, db: file)"
      rename(file:f, format: renameFormat, db: 'file') // Why is it adding .mkv to the end of the file? Only windows 11?
    }
  }
}


