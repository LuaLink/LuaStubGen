package parser

data class ParsedMethod(
    val name: String,
    val returnType: String,
    val visibility: Visibility,
    val parameters: List<ParsedParameter>,
    val comment: String?,
    val isDeprecated: Boolean = false,
    val isAsync: Boolean = false,
    val isConstructor: Boolean = false,
)

data class ParsedParameter(
    val name: String,
    val type: String,
    val required: Boolean = true,
    val comment: String? = null
)
