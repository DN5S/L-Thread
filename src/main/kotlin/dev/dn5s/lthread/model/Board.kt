package dev.dn5s.lthread.model

data class Board(
    val name: String,
    val displayName: String,
    val description: String = ""
) {
    companion object {
        const val THREADS_PER_PAGE = 15
        const val PREVIEW_REPLIES = 3

        val DEFAULT_BOARDS = listOf(
            Board("general", "/g/", "General Discussion"),
            Board("tech", "/t/", "Technology & Programming"),
            Board("anime", "/a/", "Anime & Manga"),
            Board("gaming", "/v/", "Video Games"),
            Board("random", "/r/", "Random"),
            Board("request", "/req/", "Feature Requests")
        )
    }
}