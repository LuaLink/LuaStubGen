package util

fun mapJavaTypeToLua(javaType: String): String {
    return when (javaType) {
        "int", "float", "double", "long", "short", "byte" -> "number"
        "boolean" -> "boolean"
        "char", "String" -> "string"
        "void" -> "nil"
        else -> javaType.substringAfterLast(".") // Simplify fully qualified names
    }
}
