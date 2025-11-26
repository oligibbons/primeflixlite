package com.example.primeflixlite

/**
 * Annotation to exclude a field from logic processing (not necessarily Room persistence).
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class Exclude

/**
 * Marker annotation for entities that can be "Liked" or Favorited.
 */
@Target(AnnotationTarget.CLASS)
annotation class Likable

/**
 * Extension function to check if a string starts with any of the provided prefixes.
 */
fun String.startsWithAny(vararg prefixes: String, ignoreCase: Boolean = false): Boolean {
    return prefixes.any { this.startsWith(it, ignoreCase) }
}