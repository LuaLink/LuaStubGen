package util

import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag

fun extractMainComment(doc: Javadoc?): String {
    return doc?.toComment()?.comment?.toString() ?: ""
}

fun extractParamComment(doc: Javadoc?, paramName: String): String? {
    return doc?.blockTags
        ?.filter { it.type == JavadocBlockTag.Type.PARAM && it.name.orElse("") == paramName }
        ?.map { it.content.toText().replace("\n", " ").trim() }
        ?.firstOrNull()
}

fun extractReturnComment(doc: Javadoc?): String? {
    return doc?.blockTags
        ?.filter { it.type == JavadocBlockTag.Type.RETURN }
        ?.joinToString(" ") { it.content.toText() }
        ?.replace("\n", " ")
        ?.trim()
        ?.ifBlank { null }
}
