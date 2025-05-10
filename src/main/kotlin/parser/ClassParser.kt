package parser

import java.util.jar.JarFile

interface ClassParser {
    fun parse(): List<ParsedClass>
}