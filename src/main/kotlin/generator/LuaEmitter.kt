package generator

import parser.ParsedClass
import util.extractJavaDocInfo
import util.mapJavaTypeToLua

class LuaEmitter {
    fun emit(parsedClass: ParsedClass): String {
        val sb = StringBuilder()
        sb.appendLine("-- ${parsedClass.packageName}.${parsedClass.name}")
        sb.appendLine(
            "---@class ${parsedClass.name}${
                if (parsedClass.extendedTypes.isNotEmpty() || parsedClass.implementedTypes.isNotEmpty())
                    ": " + (parsedClass.extendedTypes + parsedClass.implementedTypes).joinToString(", ")
                else ""
            }"
        )

        // Process fields (variables)
        parsedClass.fields.forEach { field ->
            sb.appendLine("---@field ${field.name} ${mapJavaTypeToLua(field.type)}")
        }

        parsedClass.constructors.forEach { constructor ->
            val doc = extractJavaDocInfo(constructor.comment)

            val params = constructor.parameters.joinToString(", ") { param ->
                val paramName = param.name
                val luaType = mapJavaTypeToLua(param.type)
                "$paramName: $luaType${if (!param.required) "?" else ""}"
            }

            val returnType = mapJavaTypeToLua(constructor.returnType)
            sb.appendLine("---@overload fun($params): $returnType ${doc.returnComment ?: ""}")
        }

        sb.appendLine("local ${parsedClass.name} = {}\n")

        val docInfo = extractJavaDocInfo(parsedClass.classComment)
        if (docInfo.mainComment.isNotBlank()) {
            sb.insert(0, "--- ${docInfo.mainComment}\n")
        }


        // Process methods
        parsedClass.methods.forEach { method ->
            val doc = extractJavaDocInfo(method.comment)

            if (method.isDeprecated) {
                sb.appendLine("---@deprecated")
            }
            if (method.isAsync) {
                sb.appendLine("---@async")
            }
            method.parameters.forEach { param ->
                val paramName = param.name
                val luaType = mapJavaTypeToLua(param.type)
                sb.appendLine("---@param $paramName${if (!param.required) "?" else ""} $luaType ${param.comment ?: ""}")
            }

            sb.appendLine("---@return ${mapJavaTypeToLua(method.returnType)} ${doc.returnComment ?: ""}")

            if (doc.mainComment.isNotBlank()) {
                sb.appendLine("--- ${doc.mainComment}")
            }


            val name = method.name
            sb.appendLine("function ${parsedClass.name}:$name(${method.parameters.joinToString(", ") { it.name }}) end\n")
        }

        return sb.toString()
    }
}