package parser

import java.util.jar.JarFile

interface ClassParser {
    fun parse(jar: JarFile): List<ParsedClass>
}