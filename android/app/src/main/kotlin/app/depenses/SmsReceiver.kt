package app.depenses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Reçoit chaque SMS entrant, garde ceux d'Orange Money qui sont des sorties d'argent, et notifie. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Un SMS long arrive en plusieurs morceaux : on les recolle par expéditeur.
        val bySender = LinkedHashMap<String, StringBuilder>()
        for (m in parts) {
            val sender = m.originatingAddress ?: continue
            bySender.getOrPut(sender) { StringBuilder() }.append(m.messageBody ?: "")
        }

        for ((sender, body) in bySender) {
            if (!OmParser.isOrangeMoneySender(sender)) continue
            val parsed = OmParser.parse(body.toString()) ?: continue
            val item = PendingItem(parsed.transId, parsed.type, parsed.amount, parsed.label, System.currentTimeMillis())
            if (PendingStore.add(context, item)) Notifier.show(context, item)
        }
    }
}
