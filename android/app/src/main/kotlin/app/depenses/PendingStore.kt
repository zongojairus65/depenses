package app.depenses

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Dépense détectée dans un SMS, en attente que l'utilisateur choisisse une catégorie. */
data class PendingItem(
    val id: String,
    val type: TxType,
    val amount: Long,
    val label: String,
    val ts: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("type", type.name)
        .put("amount", amount)
        .put("label", label)
        .put("ts", ts)
}

/**
 * Stockage local (SharedPreferences privées à l'application).
 * On ne garde ni le solde ni le texte du SMS : seulement type, montant, libellé, heure et identifiant.
 */
object PendingStore {
    private const val PREFS = "pending_store"
    private const val K_ITEMS = "items"
    private const val K_SEEN = "seen"
    private const val MAX_SEEN = 300

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readArray(c: Context, key: String): JSONArray =
        try {
            JSONArray(prefs(c).getString(key, "[]"))
        } catch (e: Exception) {
            JSONArray()
        }

    /** Renvoie false si ce message (même identifiant de transaction) a déjà été vu. */
    @Synchronized
    fun add(c: Context, item: PendingItem): Boolean {
        val seen = readArray(c, K_SEEN)
        for (i in 0 until seen.length()) {
            if (seen.optString(i) == item.id) return false
        }
        seen.put(item.id)
        val trimmedSeen = JSONArray()
        for (i in maxOf(0, seen.length() - MAX_SEEN) until seen.length()) trimmedSeen.put(seen.get(i))

        val items = readArray(c, K_ITEMS)
        items.put(item.toJson())

        prefs(c).edit()
            .putString(K_SEEN, trimmedSeen.toString())
            .putString(K_ITEMS, items.toString())
            .commit()
        return true
    }

    @Synchronized
    fun remove(c: Context, id: String) {
        val items = readArray(c, K_ITEMS)
        val kept = JSONArray()
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            if (o.optString("id") != id) kept.put(o)
        }
        prefs(c).edit().putString(K_ITEMS, kept.toString()).commit()
    }

    /** Les plus récents d'abord. */
    @Synchronized
    fun toJson(c: Context): String {
        val items = readArray(c, K_ITEMS)
        val out = JSONArray()
        for (i in items.length() - 1 downTo 0) out.put(items.get(i))
        return out.toString()
    }
}
