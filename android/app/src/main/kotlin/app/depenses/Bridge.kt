package app.depenses

import android.content.Context
import android.webkit.JavascriptInterface

/** Pont entre l'interface web (WebView) et le stockage natif des SMS détectés. */
class Bridge(private val ctx: Context) {
    @JavascriptInterface
    fun getPending(): String = PendingStore.toJson(ctx)

    @JavascriptInterface
    fun removePending(id: String) = PendingStore.remove(ctx, id)
}
