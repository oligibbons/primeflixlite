package com.example.primeflixlite.data.parser.xtream

/**
 * Interface for communicating with Xtream Codes based IPTV servers.
 */
interface XtreamParser {
    suspend fun getLiveStreams(input: XtreamInput): List<XtreamChannelInfo.LiveStream>
    suspend fun getVodStreams(input: XtreamInput): List<XtreamChannelInfo.VodStream>
    suspend fun getSeries(input: XtreamInput): List<XtreamChannelInfo.Series>
    suspend fun getSeriesEpisodes(input: XtreamInput, seriesId: Int): List<XtreamChannelInfo.Episode>

    // Helper to construct the XMLTV EPG URL
    fun createXmlUrl(basicUrl: String, username: String, password: String): String {
        return "$basicUrl/xmltv.php?username=$username&password=$password"
    }
}