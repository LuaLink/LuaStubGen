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

    // Initialize the emitter
    val luaEmitter = LuaEmitter()

    args.forEach { jarFilePath ->
        // Check if the file exists
        val file = File(jarFilePath)
        if (!file.exists()) {
            println("Warning: File $jarFilePath does not exist.")
            return@forEach
        }

        // Determine if the file is a source JAR or compiled JAR
        val parser : ClassParser = if (jarFilePath.endsWith("-sources.jar")) {
            println("Using JavaSourceParser for $jarFilePath")
            JavaSourceParser()
        } else {
            println("Using CompiledClassParser for $jarFilePath")
            JavaSourceParser()
        }

        try {
            // Parse the JAR file
            val parsedClasses = parser.parse(JarFile(file))

            // Generate Lua stubs for each parsed class
            parsedClasses.forEach { parsedClass ->
                val luaOutput = luaEmitter.emit(parsedClass)
                val outputFile = File("stubs/${parsedClass.name}.lua")
                outputFile.writeText(luaOutput)
                println("Generated Lua stubs for ${parsedClass.name} in ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("Error processing $jarFilePath: ${e.message}")
        }
    }
}