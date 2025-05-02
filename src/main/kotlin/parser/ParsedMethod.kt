package parser

data class ParsedMethod(
    val name: String,
    val returnType: String,
    val parameters: List<ParsedParameter>,
    val comment: String?,
    val isDeprecated: Boolean = false,
    val isAsync: Boolean = false
)

data class ParsedParameter(
    val name: String,
    val type: String,
    val required: Boolean = true,
    val comment: String? = null
)
