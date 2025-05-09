package parser

data class ParsedClass(
    val name: String,
    val packageName: String,
    val methods: List<ParsedMethod>,
    val fields: List<ParsedField>,
    val classComment: String?,
    val extendedTypes : List<String> = emptyList(),
    val implementedTypes : List<String> = emptyList(),
    val constructors : List<ParsedMethod> = emptyList(),
    val isEnum: Boolean = false,
)
