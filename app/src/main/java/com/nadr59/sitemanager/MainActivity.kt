@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            // ═══ الثيم ═══
            val themeVm: ThemeViewModel = hiltViewModel()
            val currentTheme by themeVm.currentTheme.collectAsState()
            val isDarkMode by themeVm.isDarkMode.collectAsState()

            SiteManagerTheme(
                themePreference = currentTheme,
                darkTheme = isDarkMode
            ) {
                MainNavHost(initialSharedUrl = sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedUrl = extractSharedUrl(intent)
        if (sharedUrl.isNotBlank()) {
            setContent {
                val themeVm: ThemeViewModel = hiltViewModel()
                val currentTheme by themeVm.currentTheme.collectAsState()
                val isDarkMode by themeVm.isDarkMode.collectAsState()

                SiteManagerTheme(
                    themePreference = currentTheme,
                    darkTheme = isDarkMode
                ) {
                    MainNavHost(initialSharedUrl = sharedUrl)
                }
            }
        }
    }

    private fun extractSharedUrl(intent: Intent?): String {
        if (intent == null) return ""
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    extractUrlFromText(text)
                } else ""
            }
            Intent.ACTION_VIEW -> intent.dataString ?: ""
            else -> ""
        }
    }

    private fun extractUrlFromText(text: String): String {
        if (text.isBlank()) return ""
        if (text.startsWith("http://") || text.startsWith("https://")) return text.trim()
        val urlRegex = Regex("""https?://[^\s]+""")
        val match = urlRegex.find(text)
        if (match != null) return match.value.trim()
        if (text.contains(".") && !text.contains(" ")) return "https://${text.trim()}"
        return text.trim()
    }
}
