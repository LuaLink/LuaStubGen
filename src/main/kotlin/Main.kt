package main.kotlin

import generator.LuaEmitter
import parser.ClassParser
import parser.JavaSourceParser
import java.io.File
import java.util.jar.JarFile


fun main(args: Array<String>) {
    // Ensure at least one JAR file is provided
    if (args.isEmpty()) {
        println("Error: Please provide at least one JAR file as argument.")
        return
    }

    // Parse the --output-dir flag
    val outputDirFlag = args.find { it.startsWith("--output-dir=") }
    val outputDirPath = outputDirFlag?.substringAfter("=") ?: "./stubs"
    val outputDir = File(outputDirPath)

    // Ensure the output directory exists
    if (!outputDir.exists() && !outputDir.mkdirs()) {
        println("Error: Could not create output directory at $outputDirPath")
        return
    }

    // Initialize the emitter
    val luaEmitter = LuaEmitter()

    var parser: ClassParser = JavaSourceParser(args.filter { !it.startsWith("--output-dir=") }.map { File(it) })


    try {
        val parsedClasses = parser.parse()

        parsedClasses.forEach { parsedClass ->
            val luaOutput = luaEmitter.emit(parsedClass)
            val outputFile = File(outputDir, "${parsedClass.packageName}.${parsedClass.name}.lua")
            outputFile.writeText(luaOutput)
            println("Generated Lua stubs for ${parsedClass.name} in ${outputFile.absolutePath}")
        }
        val importsOutput = luaEmitter.emitAvailableImports(parsedClasses)
        val importsFile = File(outputDir, "imports.lua")
        importsFile.writeText(importsOutput)
        println("Generated Lua imports in ${importsFile.absolutePath}")
    } catch (e: Exception) {
        println("Error processing: ${e.message}")
    }
}