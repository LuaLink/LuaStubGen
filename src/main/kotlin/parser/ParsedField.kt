package parser

data class ParsedField(
    val name: String,
    val type: String,
    val comment: String?
)