package com.example.primeflixlite.util

import java.util.regex.Pattern

object TitleNormalizer {

    // Regex to remove common "scene" tags, years, resolutions, and language markers
    private val TAGS_REGEX = Pattern.compile(
        "([.\\[(]?(19|20)\\d{2}[.\\])]?)|" + // Years like 2023, (2023), [2023]
                "(S\\d{1,2}E\\d{1,2})|" + // S01E01
                "(1080p|720p|480p|4k|uhd|hdr|h264|h265|hevc|x264|x265)|" + // Resolutions/Codecs
                "(multi|vostfr|vf|fr|en|eng|ita|spa|ger|ru)|" + // Languages
                "(\\[.*?\\])|" + // Content in brackets
                "(\\(.*?\\))", // Content in parentheses
        Pattern.CASE_INSENSITIVE
    )

    // Specific Regex just to capture the Language for the "Quality" label
    private val LANG_REGEX = Pattern.compile(
        "(multi|vostfr|vf|fr|en|eng|ita|spa|ger|ru)",
        Pattern.CASE_INSENSITIVE
    )

    private val SPACER_REGEX = Pattern.compile("[._-]")

    data class ContentInfo(
        val rawTitle: String,
        val normalizedTitle: String,
        val quality: String,
        val year: String?
    )

    fun parse(rawTitle: String): ContentInfo {
        var cleanTitle = rawTitle

        // 1. Extract Year
        val yearMatcher = Pattern.compile("([.\\[(]?(19|20)\\d{2}[.\\])]?)").matcher(rawTitle)
        var year: String? = null
        if (yearMatcher.find()) {
            val y = yearMatcher.group(1) ?: ""
            year = y.replace(Regex("[^0-9]"), "")
        }

        // 2. Identify Resolution
        val res = when {
            rawTitle.contains("4k", true) || rawTitle.contains("uhd", true) -> "4K"
            rawTitle.contains("1080", true) -> "1080p"
            rawTitle.contains("720", true) -> "720p"
            else -> "SD"
        }

        // 3. Identify Language (New Step)
        val langMatcher = LANG_REGEX.matcher(rawTitle)
        val lang = if (langMatcher.find()) langMatcher.group(1)?.uppercase() else null

        // Combined Quality Label (e.g., "1080p EN")
        val finalQuality = if (lang != null) "$res $lang" else res

        // 4. Remove prefixes (e.g. "US: ")
        if (cleanTitle.contains(":")) {
            val parts = cleanTitle.split(":")
            if (parts.size > 1 && parts[0].length < 5) {
                cleanTitle = parts.subList(1, parts.size).joinToString(":")
            }
        }

        // 5. Regex Replace tags (Strips the EN/FR/1080p from the Title string)
        cleanTitle = TAGS_REGEX.matcher(cleanTitle).replaceAll(" ")

        // 6. Cleanup
        cleanTitle = SPACER_REGEX.matcher(cleanTitle).replaceAll(" ")
        cleanTitle = cleanTitle.trim().replace(Regex("\\s+"), " ")

        return ContentInfo(
            rawTitle = rawTitle,
            normalizedTitle = cleanTitle,
            quality = finalQuality, // This is what appears on the button
            year = year
        )
    }

    fun generateGroupKey(normalizedTitle: String): String {
        return normalizedTitle.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}