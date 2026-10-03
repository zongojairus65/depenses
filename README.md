# Dépenses

Suivi de dépenses en FCFA, avec sessions (budgets séparés) et, sur Android, une notification à chaque SMS Orange Money qui correspond à une sortie d'argent (retrait, paiement, transfert). Tu ouvres la notification, tu choisis la catégorie, tu enregistres.

## Contenu du dépôt

- `web/index.html` : l'application (un seul fichier, sans dépendance). Elle fonctionne seule dans un navigateur.
- `android/` : l'application Android. Elle embarque `web/` dans une WebView et ajoute la lecture des SMS et les notifications.
- `.github/workflows/android.yml` : construit l'APK sur GitHub, sans Android Studio.

## Comment ça marche

1. Un SMS arrive de l'expéditeur « OrangeMoney ».
2. `SmsReceiver` le lit et `OmParser` reconnaît trois formats : retrait chez un agent, paiement marchand, transfert. Tout autre message est ignoré.
3. Le montant, le type, le libellé (agent, marchand ou bénéficiaire), l'heure et l'identifiant de transaction sont gardés sur le téléphone. Le solde et le texte du SMS ne sont pas conservés.
4. Une notification s'affiche. En l'ouvrant, la section « À saisir » apparaît en haut de l'application : catégorie, note, Enregistrer ou Ignorer.
5. L'identifiant de transaction évite les doublons si le même SMS est signalé deux fois.

## Installer l'application sur le téléphone

### Option A : GitHub construit l'APK (aucun outil à installer)

1. Crée un dépôt sur GitHub, puis dans ce dossier :
   ```
   git init
   git add .
   git commit -m "Premier commit"
   git branch -M main
   git remote add origin https://github.com/<ton-compte>/depenses.git
   git push -u origin main
   ```
2. Sur GitHub : onglet **Actions** → **Build APK**. Le build démarre au push (ou clique sur **Run workflow**).
3. Une fois le build terminé (coche verte), ouvre-le et télécharge l'artefact `depenses-debug-apk` (un zip qui contient `app-debug.apk`).
4. Copie l'APK sur le téléphone et installe-le. Android demandera d'autoriser l'installation depuis cette source.

### Option B : Android Studio

Ouvre le dossier `android/`, laisse la synchronisation Gradle se faire, puis **Run** sur le téléphone branché ou **Build → Build APK(s)**.

## Autorisations : les trois pièges

1. **Paramètres restreints (Android 13 et plus).** Une application installée hors Play Store peut se voir refuser l'autorisation SMS (option grisée ou refus automatique). Va dans Paramètres → Applications → Dépenses → menu ⋮ en haut à droite → **Autoriser les paramètres restreints**. Ensuite Autorisations → SMS → **Autoriser**. Le libellé varie selon le téléphone.
2. **Économie de batterie.** Certaines marques (Xiaomi, Tecno, Infinix, Itel, Samsung…) bloquent les applications en arrière-plan. Désactive l'optimisation de batterie pour Dépenses et autorise le démarrage automatique si le téléphone le propose. Sinon les notifications peuvent arriver en retard, ou pas du tout.
3. **Google Play Protect.** Il peut afficher un avertissement à l'installation, parce que l'application n'est pas sur le Play Store et demande la permission SMS. Le code est dans ce dépôt : tu peux le relire.

## Mises à jour de l'application

Le dépôt contient une clé de signature fixe (`android/app/debug.keystore.b64`) pour que chaque nouvel APK puisse s'installer **par-dessus** l'ancien sans perdre les données. Conséquences :
- **Garde le dépôt privé.** Quiconque possède cette clé peut signer une fausse mise à jour de ton application.
- Un APK construit avant l'ajout de cette clé est signé autrement : le **premier** passage à la version signée avec la clé fixe oblige à désinstaller l'ancienne (les données sont alors perdues). Les mises à jour suivantes se font par-dessus.

## Données et confidentialité

- Tout reste sur le téléphone. L'application n'a **pas** la permission INTERNET : elle ne peut rien envoyer en ligne.
- Elle demande `RECEIVE_SMS` (SMS entrants) et non `READ_SMS` : elle ne lit pas ta boîte de réception.
- `allowBackup` est désactivé : les données ne partent pas dans la sauvegarde Google.
- Conséquence : **désinstaller l'application ou vider ses données efface tout**, et il n'existe pas encore d'export ni d'import. Ne désinstalle pas sans avoir noté ce dont tu as besoin (le bouton « Copier le mois en texte » sert de sauvegarde manuelle).
- La version installée sur le téléphone et la version web dans un navigateur ne partagent pas leurs données.

## Limites connues

- **Non testé sur un vrai téléphone ni compilé** par moi. Le parseur a été vérifié sur les trois formats de tes captures avec des montants inventés. Le build Gradle, lui, n'a jamais tourné : si l'étape « Build APK » échoue sur GitHub, copie l'erreur et corrige-la ou demande de l'aide.
- Seuls les trois formats vus sont reconnus. Un message de dépôt (« Vous avez recu ... ») est ignoré. Si Orange change ses textes, adapte `OmParser.kt` et ajoute un test dans `OmParserTest.kt`.
- Si aucune notification n'apparaît alors que les autorisations sont bonnes, vérifie l'expéditeur réel du SMS : `isOrangeMoneySender` cherche « orangemoney » dans son nom.
- Un retrait n'est pas une dépense en soi : si tu enregistres le retrait **et** ce que tu paies ensuite avec ce cash, tu comptes deux fois. Utilise « Ignorer » ou une catégorie dédiée selon ta façon de compter.
- Pas de saisie directe depuis la notification : il faut ouvrir l'application.

## Tests

Le parseur a des tests unitaires : `cd android && gradle testDebugUnitTest` (ou clic droit sur `OmParserTest` dans Android Studio). Le workflow GitHub les lance avant de construire l'APK.
