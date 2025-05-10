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

    args.filterNot { it.startsWith("--output-dir=") }.forEach { jarFilePath ->
        val file = File(jarFilePath)
        if (!file.exists()) {
            println("Warning: File $jarFilePath does not exist.")
            return@forEach
        }

        val parser: ClassParser = if (jarFilePath.endsWith("-sources.jar")) {
            println("Using JavaSourceParser for $jarFilePath")
            JavaSourceParser()
        } else {
            println("Using CompiledClassParser for $jarFilePath")
            JavaSourceParser() // Placeholder for compiled class parser
        }

        try {
            val parsedClasses = parser.parse(JarFile(file))

            parsedClasses.forEach { parsedClass ->
                val luaOutput = luaEmitter.emit(parsedClass)
                val outputFile = File(outputDir, "${parsedClass.name}.lua")
                outputFile.writeText(luaOutput)
                println("Generated Lua stubs for ${parsedClass.name} in ${outputFile.absolutePath}")
            }
            val importsOutput = luaEmitter.emitAvailableImports(parsedClasses)
            val importsFile = File(outputDir, "imports.lua")
            importsFile.writeText(importsOutput)
            println("Generated Lua imports in ${importsFile.absolutePath}")
        } catch (e: Exception) {
            println("Error processing $jarFilePath: ${e.message}")
        }
    }
}