package util

fun mapJavaTypeToLua(javaType: String): String {
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
                "${mapJavaTypeToLua(baseType)}?"
            } else {
                "${mapJavaTypeToLua(innerType)}?"
            }
        }

        javaType.endsWith("?") -> {
            val baseType = javaType.removeSuffix("?")
            "${mapJavaTypeToLua(baseType)}?"
        }

        javaType in listOf(
            "java.lang.Integer",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Byte",
            "int",
            "float",
            "double",
            "long",
            "short",
            "byte"
        ) -> "number"

        javaType in listOf("java.lang.Boolean", "boolean") -> "boolean"
        javaType in listOf("java.lang.Character", "java.lang.String", "char", "String") -> "string"
        javaType == "void" -> "nil"
        else -> javaType // Default case for other types
    }
}