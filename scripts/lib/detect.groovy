package lib
//--- VERSION 1.0.0
Boolean detectAnimeMovie(File f) {
  return f.name =~ /(?i:Movie|Gekijouban)/ || ( f.isVideo() ? getMediaInfo(f, '{minutes}').toInteger() > 60 : false )
}

Boolean detectAirdateOrder(String filename) {
  // Should work for SnnEnnn, SnnEnnnVnn, nmXnnn, nnXnnnVnn
  // VOID - return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}\b|E\d{1,3}v[\d]{1,2}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
  return filename =~ /(?i)\b((S\d{1,2}|\d{1,2})(?>\.)?(E\d{1,3}[_]?v[\d]{1,2}\b|E\d{1,3}\b|x\d{1,3}\b|x\d{1,3}v[\d]{1,2}\b))/
}
