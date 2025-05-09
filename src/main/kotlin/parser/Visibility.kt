package parser

enum class Visibility {
    PUBLIC, PROTECTED, PRIVATE;

    companion object {
        fun fromString(visibility: String): Visibility {
            return when (visibility) {
                "PUBLIC" -> PUBLIC
                "PROTECTED" -> PROTECTED
                "PRIVATE" -> PRIVATE
                else -> PUBLIC
            }
        }
    }
}