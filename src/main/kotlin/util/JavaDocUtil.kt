package util

data class JavaDocInfo(
    val mainComment: String,
    val paramComments: Map<String, String>,
    val returnComment: String?
)

fun extractJavaDocInfo(comment: String?): JavaDocInfo {
    if (comment.isNullOrBlank()) {
        return JavaDocInfo("", emptyMap(), null)
    }

    val cleanedComment = comment.trim()
        .replace(Regex("\\s*\\*\\s*"), " ") // Remove leading asterisks
        .replace(Regex("<p>\\s*"), "\n") // Replace HTML paragraph tags with line breaks
        .replace(Regex("</p>\\s*"), "\n")
        .replace(Regex("<br ?/?>"), "\n") // Replace <br> with newlines
        .replace(Regex("<[^>]+>"), "") // Remove any remaining HTML tags
        .replace(Regex("\\{@link\\s+([^}]+)}"), "$1") // Convert {@link ClassName} to ClassName
        .replace(Regex("\\{@code\\s+([^}]+)}"), "$1") // Convert {@code something} to something
        .replace(Regex("\\{@[^}]+}"), "") // Remove any other inline tags
        .replace(Regex("\\s+"), " ") // Normalize whitespace
        .trim()

    // Extract @param comments
    val paramRegex = Regex("@param\\s+(\\w+)\\s+([^@]*?)(?=@|$)")
    val paramComments = paramRegex.findAll(cleanedComment).associate {
        it.groupValues[1] to it.groupValues[2].trim()
    }

    // Extract @return comment
    val returnRegex = Regex("@return\\s+([^@]*?)(?=@|$)")
    val returnComment = returnRegex.find(cleanedComment)?.groupValues?.get(1)?.trim()

    // Extract the main comment (everything before the first @tag)
    val mainComment = cleanedComment.split(Regex("@\\w+")).first().trim()

    return JavaDocInfo(mainComment, paramComments, returnComment)
}
