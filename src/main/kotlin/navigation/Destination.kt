package navigation

sealed class Destination constructor(val title: String) {

    abstract val url: String
    abstract fun updateUrl(url: String): Destination

    companion object {
        val values = arrayOf(Jfx(), Vlcj(), Jcv())
    }

    data class Jfx(override val url: String = "") : Destination("Jfx") {
        override fun updateUrl(url: String) = copy(url = url)
    }

    data class Vlcj(override val url: String = "") : Destination("Vlcj") {
        override fun updateUrl(url: String) = copy(url = url)
    }

    data class Jcv(override val url: String = "") : Destination("JavaCV") {
        override fun updateUrl(url: String) = copy(url = url)
    }
}