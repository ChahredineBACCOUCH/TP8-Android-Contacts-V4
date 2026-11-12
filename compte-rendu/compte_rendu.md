# Compte rendu — TP Android : gestion des contacts (Version 4)

**Nom et prénom :** …………………… **Groupe :** ……………

## 1. Schéma de la base et choix des types

Base `contacts.db`, table `contacts` : `_id INTEGER PRIMARY KEY AUTOINCREMENT`, `nom`, `prenom`, `telephone` en `TEXT NOT NULL`, `email` et `photo_uri` en `TEXT` (facultatifs), `latitude` et `longitude` en `REAL` acceptant `NULL`.

- Le **téléphone est en TEXT** : un `INTEGER` perdrait le 0 initial, le « + » de +216 et les espaces.
- Une position absente est `NULL` dans **les deux** colonnes GPS (jamais une seule).
- `_id` est le nom attendu par les outils Android (`BaseColumns._ID`).

## 2. Helper et DAO

- **`ContactContract`** regroupe le nom de table, les colonnes et le `CREATE TABLE` : aucun nom de colonne n'est écrit ailleurs.
- **`ContactDbHelper`** hérite de `SQLiteOpenHelper`. `onCreate` crée la table et insère 6 contacts de démonstration (dont 2 sans GPS) dans **une transaction**. `onUpgrade` supprime et recrée la table (pas de migration nécessaire en version 1). C'est un **singleton** construit avec `applicationContext` : une seule connexion pour toute l'application, et pas de fuite mémoire d'une activité.
- **`ContactDao`** est la seule classe contenant du SQL : `insert` (renvoie l'`_id` ou -1), `update` et `delete` (renvoient le nombre de lignes touchées), `getAll`, `getById`, `search`. Toutes les valeurs variables passent par des `?`. Chaque `Cursor` est fermé par `use { }` et lu avec `getColumnIndexOrThrow()`.

## 3. Threads

Aucun accès disque sur le thread principal : `ContactViewModel` exécute le DAO dans `withContext(Dispatchers.IO)` depuis `viewModelScope`, puis publie le résultat dans une `LiveData`. Le détail charge son contact et enregistre via `viewLifecycleOwner.lifecycleScope.launch`. Les valeurs de retour pilotent l'interface : `insert == -1` → « L'insertion a échoué », `update/delete == 0` → « Contact introuvable ».

## 4. Filtrage : SQL plutôt que mémoire

Le filtre est fait **en SQL** : `nom LIKE ? OR prenom LIKE ? OR (prenom || ' ' || nom) LIKE ? OR REPLACE(telephone,' ','') LIKE ?`, avec `ESCAPE` pour les caractères `%` et `_`. Avantages : la base reste la source unique de vérité (la liste affichée est toujours relue), le tri `COLLATE NOCASE` s'applique aussi aux résultats, et la solution reste efficace si le nombre de contacts augmente. `LIKE` est insensible à la casse pour les caractères ASCII.

## 5. Gestion des photos

L'image choisie dans la galerie est copiée dans `filesDir/photos` ; la photo prise par la caméra y est écrite directement via un `FileProvider`. C'est le **chemin de cette copie** qui est enregistré dans `photo_uri` : il reste valide après redémarrage, contrairement à une URI de galerie temporaire.

## 6. Préférences

`ParametresFragment` (`PreferenceFragmentCompat`) charge `res/xml/preferences.xml` : deux `ListPreference` (taille, couleur, entrées dans `arrays.xml`) et un `SwitchPreferenceCompat`, avec `useSimpleSummaryProvider`. Il est ouvert depuis l'icône ⚙ de la barre, par `replace()` + `addToBackStack()`. Les valeurs sont stockées dans les **SharedPreferences** (clé/valeur simple), pas dans SQLite.

`PreferencesManager` centralise la lecture et fournit des valeurs converties (`Float` en sp, couleur `Int` via `Color.parseColor`, `Boolean`). Dans `onResume`, la liste relit ces réglages et les passe à l'adapter (`appliquerReglages` → redessin des lignes) : le changement est **immédiat** au retour des Paramètres. Le bouton Appeler de chaque ligne utilise `ACTION_DIAL` ; son clic est consommé par le bouton et n'ouvre pas la fiche.

## 7. Tests réalisés

| Test | Résultat |
|---|---|
| Premier lancement : 6 contacts démo triés | ✅ |
| Création de « Leila O'Neil » (apostrophe) → apparaît triée | ✅ |
| Filtre « O'N » → aucune erreur SQL, contact trouvé | ✅ |
| Enregistrement sans nom / prénom / téléphone → erreurs, rien n'est écrit | ✅ |
| Modification du téléphone → liste actualisée | ✅ |
| Suppression avec confirmation → contact retiré | ✅ |
| Fermeture complète (`force-stop`) puis relance → création, modification, suppression conservées, démos non réinsérées | ✅ |
| Taille Grande + Bleu + Appel activé → appliqués dans la liste | ✅ |
| Taille Petite + Rouge → appliqués ; préférences conservées après relance | ✅ |
| Bouton Appeler de la ligne → composeur avec le bon numéro, sans ouvrir le détail | ✅ |
| Contact sans GPS → message explicite sur l'écran carte | ✅ |
| Database Inspector / carte avec marqueur | à faire dans Android Studio avec une clé Maps personnelle |

<p><img src="../captures/03_contact_cree_trie.png" width="140"> <img src="../captures/04_filtre_apostrophe.png" width="140"> <img src="../captures/07_confirmation_suppression.png" width="140"> <img src="../captures/09_liste_apres_relance.png" width="140"> <img src="../captures/10_ecran_parametres.png" width="140"></p>
<p><img src="../captures/12_liste_grande_bleue_bouton_appeler.png" width="140"> <img src="../captures/13_composeur_depuis_liste.png" width="140"> <img src="../captures/14_liste_petite_rouge.png" width="140"> <img src="../captures/16_detail_avec_preferences.png" width="140"> <img src="../captures/17_carte_sans_position.png" width="140"></p>

## 8. Questions de compréhension

1. **onCreate / onUpgrade** : `onCreate` est appelé à la première ouverture quand `contacts.db` n'existe pas ; `onUpgrade` quand `DATABASE_VERSION` est supérieure à la version enregistrée. Modifier `onCreate` sans changer la version n'a aucun effet sur une base existante : il faut désinstaller ou incrémenter la version.
2. **Arguments `?`** : la valeur est transmise séparément de la requête, elle ne peut pas modifier sa structure. Avec une concaténation, `O'Neil` donne `WHERE nom = 'O'Neil'` → erreur de syntaxe (et `' OR '1'='1` serait une injection).
3. **Valeurs de retour** : `insert` renvoie l'`_id` (ou -1), `update`/`delete` le nombre de lignes ; elles décident du message affiché et du retour à la liste.
4. **Thread principal** : un accès disque peut bloquer l'interface (gel, ANR). On l'évite avec `viewModelScope`/`lifecycleScope` et `Dispatchers.IO`.
5. **Helper unique avec applicationContext** : une seule connexion évite les conflits d'écriture ; le contexte d'application ne retient pas une activité détruite.
6. **SharedPreferences ou SQLite** : SharedPreferences pour quelques réglages clé/valeur ; SQLite pour des données structurées, nombreuses, interrogées et triées (les contacts).
7. **Application immédiate** : relecture dans `onResume` de la liste puis redessin de l'adapter.
8. **ACTION_DIAL / ACTION_CALL** : `ACTION_DIAL` ouvre le composeur, l'utilisateur valide l'appel, aucune permission ; `ACTION_CALL` appelle directement et exige la permission dangereuse `CALL_PHONE`.
9. **Désinstallation / effacement des données** : la base et les préférences (stockage privé de l'application) sont supprimées dans les deux cas.

## 9. Difficultés

- Ne pas réinsérer les données de démonstration : elles sont dans `onCreate` du helper, exécuté une seule fois.
- Charger le détail de façon asynchrone sans écraser une saisie en cours après rotation : un indicateur `champsRemplis` est sauvegardé.
- Échapper `%` et `_` dans `LIKE` pour qu'ils soient cherchés littéralement.
