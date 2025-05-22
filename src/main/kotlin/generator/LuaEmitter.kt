package generator

import parser.ParsedClass
import parser.Visibility
import util.mapJavaTypeToLua

class LuaEmitter {
    fun emit(parsedClass: ParsedClass): String {
        val sb = StringBuilder()
        sb.appendLine("---@meta")
        sb.appendLine("-- ${parsedClass.fqcn}")
        sb.appendLine(
            "---@class ${parsedClass.fqcn}${
                if (parsedClass.extendedTypes.isNotEmpty() || parsedClass.implementedTypes.isNotEmpty())
                    ": " + (parsedClass.extendedTypes + parsedClass.implementedTypes).joinToString(", ")
                else ""
            }"
        )

        // Process fields (variables)
        parsedClass.fields.forEach { field ->
            sb.appendLine(
                "---@field ${
                    field.visibility.toString().lowercase()
                } ${field.name} ${mapJavaTypeToLua(field.type)}"
            )
        }

        parsedClass.constructors.forEach { constructor ->

            val params = constructor.parameters.joinToString(", ") { param ->
                val paramName = param.name
                val luaType = mapJavaTypeToLua(param.type)
                "$paramName: $luaType${if (!param.required) "?" else ""}"
            }

            val returnType = mapJavaTypeToLua(constructor.returnType)
            sb.appendLine("---@overload fun($params): $returnType")
        }

        sb.appendLine("local ${parsedClass.name} = {}\n")

        val docInfo = parsedClass.classComment
        if (docInfo!!.isNotBlank()) {
            sb.insert(0, "--- ${docInfo}\n")
        }


        // Process methods
        parsedClass.methods.forEach { method ->
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

            when (method.visibility) {
                Visibility.PUBLIC -> sb.appendLine("---@public")
                Visibility.PROTECTED -> sb.appendLine("---@protected")
                Visibility.PRIVATE -> sb.appendLine("---@private")
            }

            sb.appendLine("---@return ${mapJavaTypeToLua(method.returnType)} ${method.returnComment ?: ""}")

            if (method.comment!!.isNotBlank()) {
                sb.appendLine("--- ${method.comment}")
            }


            val name = method.name
            sb.appendLine("function ${parsedClass.name}:$name(${method.parameters.joinToString(", ") { it.name }}) end\n")
        }

        return sb.toString()
    }

    fun emitAvailableImports(parsedClasses: List<ParsedClass>): String {
        val sb = StringBuilder()
        sb.appendLine("---@alias JavaClasses string|")
        parsedClasses.forEach { parsedClass ->
            val fullName = "${parsedClass.fqcn}"
            sb.appendLine("---| '\"$fullName\"' ")
        }
        // Add normal string to the alias as to not cause warnings when someone uses an import not in the list
        return sb.toString()
    }
}