//file:noinspection unused
//file:noinspection GrMethodMayBeStatic
package lib

//--- VERSION 1.0.1

import org.apache.commons.io.FilenameUtils

def executeGuessit(String fileName, String pathToGuessitExecutable){
  String sanitizedGuessitPath = FilenameUtils.separatorsToSystem(pathToGuessitExecutable)
  def guessitCommand = "sanitizedGuessitPath" + " -L EN -s -j \"${fileName}\""
  def proc = guessitCommand.execute()
  def outputStream = new StringBuffer()
  proc.waitForProcessOutput(outputStream, System.err)
  return outputStream.toString().trim()
}
