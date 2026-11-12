# TP Android — Gestion des contacts (Version 4)

Persistance **SQLite** (API native, sans Room) · **Préférences utilisateur** · Fragments · Google Maps — Kotlin, interfaces XML.

## Architecture

| Composant | Responsabilité |
|---|---|
| `MainActivity` | héberge les fragments, barre d'application avec l'icône **Paramètres** |
| `ListeContactsFragment` | liste triée, filtre, bouton flottant **+**, bouton Appeler par ligne selon la préférence |
| `DetailContactFragment` | création (id = 0), consultation, modification, **suppression** ; Appeler, E-mail, Galerie, Caméra, Carte |
| `CarteContactFragment` | Google Maps avec marqueur (inchangé depuis la V3) |
| `ParametresFragment` | `PreferenceFragmentCompat` : `res/xml/preferences.xml` + `res/values/arrays.xml` |
| `ContactViewModel` | partagé ; appels au DAO dans `viewModelScope` + `Dispatchers.IO` |
| `data/ContactContract` | nom de table et colonnes en constantes |
| `data/ContactDbHelper` | `SQLiteOpenHelper` singleton (`applicationContext`), `onCreate` + 6 contacts démo (2 sans GPS), `onUpgrade` |
| `data/ContactDao` | **seule classe avec du SQL** : `insert`, `update`, `delete`, `getAll`, `getById`, `search` |
| `PreferencesManager` | lecture centralisée et conversion des préférences |

## Base `contacts.db`

```sql
CREATE TABLE contacts (
    _id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nom        TEXT NOT NULL,
    prenom     TEXT NOT NULL,
    telephone  TEXT NOT NULL,
    email      TEXT,
    photo_uri  TEXT,
    latitude   REAL,
    longitude  REAL
);
```

- Requêtes **paramétrées** (`?` + `selectionArgs`) : « O'Neil » ou un filtre contenant `'` ne cassent rien.
- `Cursor` fermés avec `use { }`, index par `getColumnIndexOrThrow()`.
- Tri `nom COLLATE NOCASE, prenom COLLATE NOCASE`.
- Filtrage **en SQL** (`LIKE` sur nom, prénom, « prénom nom », téléphone sans espaces).
- Photo : chemin d'une **copie** dans `filesDir` (une URI de galerie peut devenir illisible).

## Préférences (SharedPreferences)

| Clé | Composant | Valeurs | Défaut |
|---|---|---|---|
| `taille_police` | ListPreference | 14 / 16 / 20 sp | 16 |
| `couleur_preferee` | ListPreference | Noir, Bleu, Vert, Rouge, Violet | Noir |
| `appel_depuis_liste` | SwitchPreferenceCompat | oui / non | non |

Relues dans `onResume` de la liste → appliquées immédiatement au retour, sans redémarrer.

## Clé Google Maps

Non publiée : à placer dans `local.properties` (`MAPS_API_KEY=...`), voir [`local.properties.example`](local.properties.example).

## Captures

| Premier lancement | Création (O'Neil) | Confirmation suppression | Après relance |
|---|---|---|---|
| <img src="captures/01_premier_lancement_sqlite.png" width="170"> | <img src="captures/03_contact_cree_trie.png" width="170"> | <img src="captures/07_confirmation_suppression.png" width="170"> | <img src="captures/09_liste_apres_relance.png" width="170"> |
| **Paramètres** | **Grande + bleu + Appeler** | **Petite + rouge** | **Composeur depuis la liste** |
| <img src="captures/11_parametres_modifies.png" width="170"> | <img src="captures/12_liste_grande_bleue_bouton_appeler.png" width="170"> | <img src="captures/14_liste_petite_rouge.png" width="170"> | <img src="captures/13_composeur_depuis_liste.png" width="170"> |

Toutes les captures : [`captures/`](captures) · Compte rendu : [`compte-rendu/compte_rendu.pdf`](compte-rendu/compte_rendu.pdf)

## Lancer

Cloner, **File > Open** dans Android Studio, synchroniser Gradle, ▶ **Run**. Pour inspecter la base : **App Inspection > Database Inspector** → `contacts.db` → table `contacts`.
