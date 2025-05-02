package parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.utils.SourceRoot
import util.extractJavaDocInfo
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile

class JavaSourceParser : ClassParser {

    private val parser = JavaParser()
    private val combinedTypeSolver = CombinedTypeSolver() // Ensure this is properly initialized

    override fun parse(jar: JarFile): List<ParsedClass> {
        val parsedClasses = mutableListOf<ParsedClass>()

        jar.entries().asSequence().filter { it.name.endsWith(".java") }.forEach { entry ->
            parseEntry(jar, entry)?.let { parsedClasses.add(it) }
        }

        return parsedClasses
    }

    private fun parseEntry(jarFile: JarFile, entry: JarEntry): ParsedClass? {
        val inputStream: InputStream = jarFile.getInputStream(entry)
        val cu = parser.parse(inputStream).result.orElse(null) ?: return null
        inputStream.close()

        val pkgName = cu.packageDeclaration.map { it.nameAsString }.orElse("")

        var parsedClass: ParsedClass? = null

        // Handle enum
        cu.findFirst(EnumDeclaration::class.java).ifPresent { enumDecl ->
            parsedClass = ParsedClass(
                name = enumDecl.nameAsString,
                packageName = pkgName,
                fields = enumDecl.entries.map {
                    ParsedField(
                        name = it.nameAsString,
                        type = "string",
                        comment = it.comment.map { c -> extractJavaDocInfo(c.content).mainComment }.orElse(null)
                    )
                },
                methods = emptyList(),
                classComment = enumDecl.comment.map { extractJavaDocInfo(it.content).mainComment }.orElse(null),
                isEnum = true
            )
        }

        // Handle class or interface
        cu.findFirst(ClassOrInterfaceDeclaration::class.java).ifPresent { cls ->
            val methods = cls.methods.map { parseMethod(it, cls.nameAsString) }
            val fields = cls.fields.flatMap { parseField(it) }

            // Resolve extended types and their methods
            val extendedTypes = cls.extendedTypes.mapNotNull { type ->
                val resolvedType = type.resolveOrNull()
                resolvedType?.qualifiedName
            }

            /*val extendedMethods = extendedTypes.flatMap { qualifiedName ->
                val resolvedClass = combinedTypeSolver.tryToSolveType(qualifiedName).orElse(null)
                resolvedClass?.methods?.map { parseMethod(it.declaration, resolvedClass.name) } ?: emptyList()
            }*/

            parsedClass = ParsedClass(
                name = cls.nameAsString,
                packageName = pkgName,
                fields = fields,
                methods = methods, // Combine methods
                classComment = cls.comment.map { extractJavaDocInfo(it.content).mainComment }.orElse(null),
                //extendedTypes = extendedTypes, // Broken at the moment
                isEnum = false
            )
        }

        return parsedClass
    }

    private fun parseMethod(method: MethodDeclaration, className: String): ParsedMethod {
        return ParsedMethod(
            name = method.nameAsString,
            returnType = method.type.toString(),
            parameters = method.parameters.map { param ->
                // Check for nullability in the parameter type. Not sure why but it seems to include annotations in the type string
                val isNullableComment = param.type.toString()?.contains("@Nullable") ?: false

                ParsedParameter(
                    name = param.nameAsString,
                    type = param.type.toString().replace("@Nullable ", ""),
                    required = !isNullableComment,
                )
            },
            comment = method.comment.map { extractJavaDocInfo(it.content).mainComment }.orElse(null),
            isDeprecated = method.annotations.any { it.nameAsString == "Deprecated" },
            isAsync = method.nameAsString.contains("Async", ignoreCase = true) // Most Bukkit methods that are async have "Async" in their name
        )
    }

    private fun parseField(field: FieldDeclaration): List<ParsedField> {
        return field.variables.map { variable ->
            ParsedField(
                name = variable.nameAsString,
                type = field.elementType.asString(),
                comment = field.comment.map { extractJavaDocInfo(it.content).mainComment }.orElse(null)
            )
        }
    }

    private fun com.github.javaparser.ast.type.Type.resolveOrNull(): ResolvedReferenceTypeDeclaration? {
        return try {
            this.resolve().asReferenceType().typeDeclaration.orElse(null)
        } catch (e: Exception) {
            null
        }
    }
}