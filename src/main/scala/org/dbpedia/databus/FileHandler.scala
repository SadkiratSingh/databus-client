package org.dbpedia.databus

import java.io.{BufferedInputStream, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.net.URL

import better.files.File
import org.apache.commons.io.{FileUtils, IOUtils}

object FileHandler {

  val src_dir: String="./downloaded_files/"

  def convertFile(inputFile:File, dest_dir:String, outputFormat:String, outputCompression:String): Unit = {
    val bufferedInputStream = new BufferedInputStream(new FileInputStream(inputFile.toJava))

    if (outputCompression=="same" && outputFormat=="same"){
      val compressionInputFile = Converter.getCompressionType(bufferedInputStream)
      val formatInputFile = Converter.getFormatType(inputFile) //NOCH OHNE FUNKTION
      val outputStream = new FileOutputStream(getOutputFile(inputFile, formatInputFile, compressionInputFile, dest_dir).toJava)
      copyStream(new FileInputStream(inputFile.toJava), outputStream)
    }
    else if (outputCompression!="same" && outputFormat=="same"){
      val decompressedInStream = Converter.decompress(bufferedInputStream)
      val format = Converter.getFormatType(inputFile) // NOCH OHNE FUNKTION
      val compressedFile = getOutputFile(inputFile, format, outputCompression, dest_dir)
      val compressedOutStream = Converter.compress(outputCompression, compressedFile)

      //file is written here
      copyStream(decompressedInStream, compressedOutStream)
    }
    //  With FILEFORMAT CONVERSION
    else if (outputCompression=="same" && outputFormat!="same"){
      val compressionInputFile= Converter.getCompressionType(bufferedInputStream)
      val targetFile = getOutputFile(inputFile, outputFormat, compressionInputFile, dest_dir)
//      val decompressedInStream = Converter.decompress(bufferedInputStream)

      val typeConvertedFile = Converter.convertFormat(inputFile, outputFormat)

      val compressedOutStream = Converter.compress(compressionInputFile, targetFile)

      //file is written here
      copyStream(new FileInputStream(typeConvertedFile.toJava), compressedOutStream)
      typeConvertedFile.delete()
    }
    else{
      val targetFile = getOutputFile(inputFile, outputFormat, outputCompression, dest_dir)
//      val decompressedInStream = Converter.decompress(bufferedInputStream)

      val typeConvertedFile = Converter.convertFormat(inputFile, outputFormat)

      val compressedOutStream = Converter.compress(outputCompression, targetFile)

      //file is written here
      copyStream(new FileInputStream(typeConvertedFile.toJava), compressedOutStream)
      typeConvertedFile.delete()
    }
  }

  def getOutputFile(inputFile: File, outputFormat:String, outputCompression:String, dest_dir: String): File ={


    val nameWithoutExtension = inputFile.nameWithoutExtension
    val name = inputFile.name
    var filepath_new = ""
    val dataIdFile = inputFile.parent / "dataid.ttl"

    if(dataIdFile.exists) {
      var dir_structure: List[String] = QueryHandler.executeDataIdQuery(dataIdFile)
      filepath_new = dest_dir.concat("/")
      dir_structure.foreach(dir => filepath_new = filepath_new.concat(dir).concat("/"))
      filepath_new = filepath_new.concat(nameWithoutExtension)
    }
    else{
      // changeExtensionTo() funktioniert nicht bei noch nicht existierendem File, deswegen ausweichen über Stringmanipulation
      filepath_new = inputFile.pathAsString.replaceAll(File(src_dir).pathAsString,File(dest_dir).pathAsString.concat("/NoDataID"))
      filepath_new = filepath_new.replaceAll(name, nameWithoutExtension)
    }

    if (outputCompression.isEmpty){
      filepath_new = filepath_new.concat(".").concat(outputFormat)
    }
    else{
      filepath_new = filepath_new.concat(".").concat(outputFormat).concat(".").concat(outputCompression)
    }

    var outputFile = File(filepath_new)
    //create necessary parent directories to write the outputfile there, later
    outputFile.parent.createDirectoryIfNotExists(createParents = true)

    println(s"Converted File: ${outputFile.pathAsString}\n")

    return outputFile
  }

  def copyStream(in: InputStream, out: OutputStream): Unit ={
    try {
      IOUtils.copy(in, out)
    }
    finally if (out != null) {
      out.close()
    }
  }

  def readQueryFile(file:File) : String = {

    var queryString:String = ""
    for (line <- file.lineIterator) {
      queryString = queryString.concat(line).concat("\n")
    }
    return queryString
  }

  def downloadFile(url: String): Unit = {
    println(url)
    //filepath from url without http://
    var filepath = src_dir.concat(url.split("http://|https://").map(_.trim).last)
    var file = File(filepath)

    file.parent.createDirectoryIfNotExists(createParents = true)

    /*new URL(url) #> file.toJava !!*/
    FileUtils.copyURLToFile(new URL(url),file.toJava)


    var dataIdFile = file.parent / "dataid.ttl"
    //if no dataid.ttl File in directory of downloaded file, then download the belongig dataid.ttl
    if (!dataIdFile.exists()){
      println("Download Dataid.ttl")
      QueryHandler.getDataIdFile(url ,dataIdFile)
    }
  }

}