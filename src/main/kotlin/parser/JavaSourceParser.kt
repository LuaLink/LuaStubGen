package parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.SymbolResolver
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder
import util.extractMainComment
import util.extractParamComment
import util.extractReturnComment
import util.mapJavaTypeToLua
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.jvm.optionals.getOrNull

class JavaSourceParser(private val jarFiles: List<File>) : ClassParser {
    private val solver = CombinedTypeSolver()
    private val parserConfiguration = ParserConfiguration()
    private val parser: JavaParser = JavaParser(parserConfiguration)
    private val javaParserFacade: JavaParserFacade = JavaParserFacade.get(solver)
    val parsedClasses = mutableMapOf<String, ParsedClass>() // Map to store parsed classes by their fully qualified name

    init {
        // Add the reflection type solver to the combined type solver
        solver.add(ReflectionTypeSolver())

        // Process each source JAR
        // I don't like this at all however it seems to be the only way to let it parse from source JARS
        // I suppose we could use a custom type solver
        // Or move away from source jars
        jarFiles.forEach { jar ->
            val tempDir = Files.createTempDirectory("source_jar")
            JarFile(jar).use { jarFile ->
                jarFile.entries().asSequence()
                    .filter { it.name.endsWith(".java") }
                    .forEach { entry ->
                        val outputFile = tempDir.resolve(entry.name)
                        Files.createDirectories(outputFile.parent)
                        jarFile.getInputStream(entry).use { input ->
                            Files.copy(input, outputFile)
                        }
                    }
            }
            solver.add(JavaParserTypeSolver(tempDir.toFile()))
        }

        // Set the symbol resolver in the parser configuration
        val symbolResolver: SymbolResolver = JavaSymbolSolver(solver)
        parserConfiguration.setSymbolResolver(symbolResolver)
    }

    override fun parse(): List<ParsedClass> {

        val jars = jarFiles.map { JarFile(it) }

        jars.forEach { jar ->
            jar.entries().asSequence().filter { it.name.endsWith(".java") }.forEach { entry ->
                parseJarEntry(jar, entry)
            }
        }

        return parsedClasses.values.toList()
    }

    private fun parseJarEntry(jarFile: JarFile, entry: JarEntry): ParsedClass? {
        val inputStream: InputStream = jarFile.getInputStream(entry)
        val cu = parser.parse(inputStream).result.orElse(null) ?: return null
        inputStream.close()
        return parseEntry(cu)
    }

    private fun parseEntry(cu: CompilationUnit): ParsedClass? {
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
                        type = enumDecl.fullyQualifiedName.get().toString(),
                        visibility = Visibility.PUBLIC,
                        comment = extractMainComment(it.javadoc.getOrNull())
                    )
                },
                methods = emptyList(),
                classComment = extractMainComment(enumDecl.javadoc.getOrNull()),
                isEnum = true
            )
            parsedClasses[enumDecl.fullyQualifiedName.toString()] = parsedClass!!
        }

        // Handle class or interface
        cu.findFirst(ClassOrInterfaceDeclaration::class.java).ifPresent { cls ->
            val methods = cls.methods.map { parseMethod(it, cls.nameAsString) }
            val fields = cls.fields.flatMap { parseField(it) }

            // Gather all constructors and parse them

            parsedClass = ParsedClass(
                name = cls.nameAsString,
                packageName = pkgName,
                fields = fields,
                methods = methods, // Combine methods
                classComment = extractMainComment(cls.javadoc.getOrNull()),
                extendedTypes = cls.extendedTypes.map { getFullyQualifiedName(it) },
                implementedTypes = cls.implementedTypes.map { getFullyQualifiedName(it) },
                constructors = cls.constructors.map { parseConstructor(it, cls.nameAsString) },
                isEnum = false
            )
            parsedClasses[cls.fullyQualifiedName.toString()] = parsedClass!!
        }

        return parsedClass
    }

    private fun parseConstructor(constructor: ConstructorDeclaration, className: String): ParsedMethod {
        return ParsedMethod(
            name = constructor.nameAsString,
            returnType = className,
            parameters = constructor.parameters.map { param ->
                // Check for nullability in the parameter type. Not sure why but it seems to include annotations in the type string
                val isNullableComment = param.type.toString()?.contains("@Nullable") ?: false

                ParsedParameter(
                    name = param.nameAsString,
                    type = param.type.toString().replace("@Nullable ", ""),
                    required = !isNullableComment,
                )
            },
            visibility = Visibility.fromString(constructor.accessSpecifier.toString().uppercase()),
            comment = extractMainComment(constructor.javadoc.getOrNull()),
            isDeprecated = constructor.annotations.any { it.nameAsString == "Deprecated" },
            isAsync = false, // Constructors are not async
            isConstructor = true
        )
    }

    private fun parseMethod(method: MethodDeclaration, className: String): ParsedMethod {
        return ParsedMethod(
            name = method.nameAsString,
            returnType = getFullyQualifiedName(method.type),
            parameters = method.parameters.map { param ->
                // Check for nullability in the parameter type. Not sure why but it seems to include annotations in the type string
                val isNullableComment = param.type.toString()?.contains("@Nullable") ?: false

                ParsedParameter(
                    name = param.nameAsString,
                    type = getFullyQualifiedName(param.type),
                    required = !isNullableComment,
                    comment = extractParamComment(method.javadoc.getOrNull(), param.nameAsString)
                )
            },
            visibility = Visibility.fromString(method.accessSpecifier.toString().uppercase()),
            comment = method.javadoc.getOrNull()?.description?.toText()?.replace("\n", " ") ?: "",
            returnComment = extractReturnComment(method.javadoc.getOrNull()),
            isDeprecated = method.annotations.any { it.nameAsString == "Deprecated" },
            isAsync = method.nameAsString.contains(
                "Async",
                ignoreCase = true
            ) // Most Bukkit methods that are async have "Async" in their name
        )
    }

    private fun parseField(field: FieldDeclaration): List<ParsedField> {
        return field.variables.map { variable ->
            ParsedField(
                name = variable.nameAsString,
                type = getFullyQualifiedName(field.elementType),
                visibility = Visibility.fromString(field.accessSpecifier.toString().uppercase()),
                comment = extractMainComment(field.javadoc.getOrNull())
            )
        }
    }

    // Slightly based off of : https://github.com/freggy/blua/blob/2408c0e0fdbc29c2fd38ed9083ab437d31eb83e2/javadump/src/main/kotlin/dev/freggy/blua/javadump/Main.kt#L136-L167
    fun getFullyQualifiedName(
        type: Type,
    ): String {
        return try {
            val resolvedType = javaParserFacade.convertToUsage(type)
            if (resolvedType.isReferenceType) {
                mapJavaTypeToLua(resolvedType.asReferenceType().qualifiedName)
            } else {
                mapJavaTypeToLua(type.asString())
            }
        } catch (e: Exception) {
            when (e) {
                is UnsupportedOperationException -> {
                    mapJavaTypeToLua(type.asString())
                }

                is UnsolvedSymbolException -> {
                    "any" // Fallback to "any" for unresolved types
                }

                else -> throw e // Rethrow unexpected exceptions
            }
        }
    }
}