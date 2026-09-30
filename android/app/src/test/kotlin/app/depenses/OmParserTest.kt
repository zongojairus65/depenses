package app.depenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Les montants et numéros ci-dessous sont inventés ; seule la forme des messages est réelle. */
class OmParserTest {

    @Test
    fun retrait() {
        val r = OmParser.parse(
            "Vous avez retire 15000.00 FCFA aupres de l agent 70123456 ANSV ZONGO PASCAL BOUTIQUE TEGAWENDE, " +
                "Trans ID : CO260927.2307.45344437. Votre solde est de : 12345.00 FCFA. " +
                "Telechargez la nouvelle application Orange Money : https://onelink.to/trqvq2"
        )
        assertNotNull(r)
        assertEquals(TxType.RETRAIT, r!!.type)
        assertEquals(15000L, r.amount)
        assertEquals("CO260927.2307.45344437", r.transId)
        assertEquals("Ansv Zongo Pascal Boutique Tegawende", r.label)
    }

    @Test
    fun paiementMarchand() {
        val r = OmParser.parse(
            "Votre paiement de 5000.00 FCFA, Frais: 0.0 FCFA, Taxe: 0.0 FCFA a ACCEPTEUR GD LATOUCH NUMERIC " +
                "LATOUCH NUMERIC a ete effectue avec succes. Votre solde est de : 1000.00 FCFA. " +
                "Trans id: MP260928.2225.45345397."
        )
        assertNotNull(r)
        assertEquals(TxType.PAIEMENT, r!!.type)
        assertEquals(5000L, r.amount)
        assertEquals("MP260928.2225.45345397", r.transId)
        assertEquals("Gd Latouch Numeric", r.label)
    }

    @Test
    fun transfert() {
        val r = OmParser.parse(
            "Vous avez transfere 10000.00 FCFA, Frais: 0.0 FCFA, Taxe: 0.0 FCFA au numero 70123456,MADENNE ESTHER. " +
                "Votre solde est de 5000.00 FCFA. ID Trans: PP260928.1945.60787425. " +
                "Pour toute reclamation contactez par appel le 127 ou whatsapp 07000121. Orange Money BF."
        )
        assertNotNull(r)
        assertEquals(TxType.TRANSFERT, r!!.type)
        assertEquals(10000L, r.amount)
        assertEquals("PP260928.1945.60787425", r.transId)
        assertEquals("Madenne Esther", r.label)
    }

    @Test
    fun messageNonReconnuEstIgnore() {
        assertNull(OmParser.parse("Vous avez recu 5000.00 FCFA de 70123456. Votre solde est de 9000.00 FCFA."))
        assertNull(OmParser.parse("Bonjour, profitez de notre offre internet."))
    }

    @Test
    fun montants() {
        assertEquals(15000L, OmParser.parseAmount("15000.00"))
        assertEquals(15000L, OmParser.parseAmount("15 000"))
        assertEquals(1500L, OmParser.parseAmount("1.500"))
        assertEquals(1501L, OmParser.parseAmount("1,500.50"))
        assertNull(OmParser.parseAmount(""))
    }

    @Test
    fun expediteur() {
        assertTrue(OmParser.isOrangeMoneySender("OrangeMoney"))
        assertTrue(OmParser.isOrangeMoneySender("Orange Money"))
        assertFalse(OmParser.isOrangeMoneySender("+22670123456"))
        assertFalse(OmParser.isOrangeMoneySender(null))
    }
}
