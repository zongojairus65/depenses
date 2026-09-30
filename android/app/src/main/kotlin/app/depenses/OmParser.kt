package app.depenses

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer

enum class TxType { RETRAIT, PAIEMENT, TRANSFERT }

data class ParsedSms(
    val type: TxType,
    val amount: Long,
    val transId: String,
    val label: String
)

/**
 * Lecture des SMS Orange Money (Burkina Faso). Trois formats reconnus :
 *  - « Vous avez retire X FCFA aupres de l agent <numéro> <nom>, Trans ID : ... »
 *  - « Votre paiement de X FCFA, Frais: ... a <marchand> a ete effectue ... »
 *  - « Vous avez transfere X FCFA, Frais: ... au numero <numéro>,<nom>. ... »
 * Tout autre message (dépôt, offre, etc.) est ignoré.
 * Le solde n'est jamais lu ni conservé.
 */
object OmParser {
    private const val AMOUNT = "([0-9][0-9\\s.,]*?)\\s*FCFA"
    private val I = RegexOption.IGNORE_CASE
    private val IS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

    private val RE_RETRAIT = Regex("avez retire\\s+$AMOUNT", I)
    private val RE_TRANSFERT = Regex("avez transfere\\s+$AMOUNT", I)
    private val RE_PAIEMENT = Regex("votre paiement de\\s+$AMOUNT", I)

    private val RE_ID = Regex("(?:trans\\s*id|id\\s*trans)\\s*:?\\s*([A-Za-z]{2}\\d{6}\\.\\d{4}\\.\\d+)", I)

    private val LBL_RETRAIT = Regex("agent\\s+\\S+\\s+(.+?)\\s*,\\s*trans", IS)
    private val LBL_PAIEMENT = Regex("FCFA a\\s+(.+?)\\s+a ete effectue", IS)
    private val LBL_TRANSFERT = Regex("au numero\\s*[^,]*,\\s*(.+?)\\.\\s*votre solde", IS)

    fun isOrangeMoneySender(sender: String?): Boolean =
        sender != null && sender.lowercase().replace(Regex("[^a-z0-9]"), "").contains("orangemoney")

    fun parse(raw: String): ParsedSms? {
        val text = normalize(raw)

        val (type, match) = listOf(
            TxType.RETRAIT to RE_RETRAIT,
            TxType.TRANSFERT to RE_TRANSFERT,
            TxType.PAIEMENT to RE_PAIEMENT
        ).firstNotNullOfOrNull { (t, re) -> re.find(text)?.let { t to it } } ?: return null

        val amount = parseAmount(match.groupValues[1]) ?: return null
        if (amount <= 0L) return null

        val id = RE_ID.find(text)?.groupValues?.get(1)?.uppercase()
            ?: ("H" + Integer.toHexString(text.hashCode()))

        val rawLabel = when (type) {
            TxType.RETRAIT -> LBL_RETRAIT.find(text)?.groupValues?.get(1)
            TxType.PAIEMENT -> LBL_PAIEMENT.find(text)?.groupValues?.get(1)
            TxType.TRANSFERT -> LBL_TRANSFERT.find(text)?.groupValues?.get(1)
        }
        return ParsedSms(type, amount, id, cleanLabel(rawLabel ?: ""))
    }

    /**
     * « 15000.00 » -> 15000 ; « 15 000 » -> 15000 ; « 1.500 » -> 1500 ; « 1,500.50 » -> 1501.
     * Un séparateur suivi de 1 ou 2 chiffres est une décimale ; sinon c'est un séparateur de milliers.
     */
    fun parseAmount(raw: String): Long? {
        val s = raw.replace(Regex("[\\s\\u00A0\\u202F]"), "")
        if (s.isEmpty()) return null
        val idx = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        val decimals = if (idx >= 0) s.length - idx - 1 else 0
        val value = try {
            if (idx >= 0 && decimals in 1..2) {
                BigDecimal(s.substring(0, idx).replace(Regex("[.,]"), "") + "." + s.substring(idx + 1))
            } else {
                BigDecimal(s.replace(Regex("[.,]"), ""))
            }
        } catch (e: NumberFormatException) {
            return null
        }
        return value.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun cleanLabel(s: String): String {
        var words = s.replace(Regex("^ACCEPTEUR\\s+", I), "")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        // « GD LATOUCH NUMERIC LATOUCH NUMERIC » -> « GD LATOUCH NUMERIC »
        for (k in words.size / 2 downTo 1) {
            if (words.takeLast(k) == words.subList(words.size - 2 * k, words.size - k)) {
                words = words.dropLast(k)
                break
            }
        }
        return words.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }.take(60)
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
}
