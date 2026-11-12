package com.example.contactsv4

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

/**
 * Lecture centralisée des préférences utilisateur (SharedPreferences).
 * Les valeurs sont renvoyées déjà converties : taille en sp (Float), couleur (Int), booléen.
 */
class PreferencesManager(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /** Taille de police en sp : 14, 16 (défaut) ou 20. */
    val tailleSp: Float
        get() = prefs.getString(CLE_TAILLE, TAILLE_DEFAUT)?.toFloatOrNull() ?: TAILLE_DEFAUT.toFloat()

    /** Couleur préférée du texte (nom et prénom) ; noir par défaut. */
    val couleur: Int
        get() = try {
            Color.parseColor(prefs.getString(CLE_COULEUR, COULEUR_DEFAUT))
        } catch (e: IllegalArgumentException) {
            Color.parseColor(COULEUR_DEFAUT)
        }

    /** Afficher un bouton Appeler sur chaque ligne de la liste ; non par défaut. */
    val appelDepuisListe: Boolean
        get() = prefs.getBoolean(CLE_APPEL, false)

    /** Instantané des trois réglages, transmis tel quel à l'adapter. */
    fun lire() = Reglages(tailleSp, couleur, appelDepuisListe)

    data class Reglages(val tailleSp: Float, val couleur: Int, val appelDepuisListe: Boolean)

    companion object {
        const val CLE_TAILLE = "taille_police"
        const val CLE_COULEUR = "couleur_preferee"
        const val CLE_APPEL = "appel_depuis_liste"
        private const val TAILLE_DEFAUT = "16"
        private const val COULEUR_DEFAUT = "#000000"
    }
}
