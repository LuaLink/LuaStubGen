package util

fun mapJavaTypeToLua(javaType: String): String {
    println(javaType)
    return when {
        javaType.startsWith("List<") -> {
            val genericType = javaType.substringAfter("List<").substringBeforeLast(">")
            "table<${mapJavaTypeToLua(genericType)}>"
        }

        javaType.startsWith("Set<") -> {
            val genericType = javaType.substringAfter("Set<").substringBeforeLast(">")
            "table<${mapJavaTypeToLua(genericType)}>"
        }

        javaType.startsWith("Map<") -> {
            val keyValueTypes = javaType.substringAfter("Map<").substringBeforeLast(">").split(",")
            if (keyValueTypes.size == 2) {
                val keyType = mapJavaTypeToLua(keyValueTypes[0].trim())
                val valueType = mapJavaTypeToLua(keyValueTypes[1].trim())
                "table<$keyType, $valueType>"
            } else {
                "table"
            }
        }

        javaType.endsWith("[]") -> {
            val elementType = javaType.removeSuffix("[]")
            "table<${mapJavaTypeToLua(elementType)}>"
        }

        javaType.startsWith("Class<") -> {
            val innerType = javaType.substringAfter("Class<").substringBeforeLast(">")
            if (innerType.startsWith("? extends")) {
                val baseType = innerType.removePrefix("? extends ").trim()
                "optional<${mapJavaTypeToLua(baseType)}>"
            } else {
                "optional<${mapJavaTypeToLua(innerType)}>"
            }
        }

        javaType.endsWith("?") -> {
            val baseType = javaType.removeSuffix("?")
            "${mapJavaTypeToLua(baseType)}?"
        }

        javaType in listOf("int", "float", "double", "long", "short", "byte") -> "number"
        javaType == "boolean" -> "boolean"
        javaType in listOf("char", "String") -> "string"
        javaType == "void" -> "nil"
        else -> javaType.substringAfterLast(".") // Simplify fully qualified names
    }
}