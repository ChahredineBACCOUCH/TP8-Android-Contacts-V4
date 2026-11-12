package com.example.contactsv4

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.contactsv4.data.Contact
import kotlinx.coroutines.launch
import java.io.File

/**
 * Création (id = 0), consultation, modification et suppression d'un contact ;
 * actions Appeler, E-mail, Galerie, Caméra et Voir sur la carte.
 */
class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private val viewModel: ContactViewModel by activityViewModels()

    private val idContact: Long by lazy { requireArguments().getLong(ARG_ID) }
    private val modeCreation get() = idContact == 0L

    /** Contact chargé depuis la base (null en mode création). */
    private var contact: Contact? = null

    private lateinit var imgPhoto: ImageView
    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etTelephone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText

    private var photoCourante: String? = null
    private var fichierCamera: String? = null

    /** Les champs ont-ils déjà été remplis ? (une saisie en cours n'est jamais écrasée) */
    private var champsRemplis = false

    private val choisirPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) {
            toast(R.string.msg_selection_annulee)
            return@registerForActivityResult
        }
        // Copie dans filesDir : une URI de galerie temporaire peut devenir illisible après redémarrage
        val copie = PhotoUtils.copierDepuisGalerie(requireContext(), uri)
        if (copie == null) {
            toast(R.string.msg_photo_illisible)
        } else {
            photoCourante = copie
            PhotoUtils.afficher(imgPhoto, photoCourante)
        }
    }

    private val prendrePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { reussi ->
        val chemin = fichierCamera
        fichierCamera = null
        if (reussi && chemin != null && File(chemin).length() > 0) {
            photoCourante = chemin
            PhotoUtils.afficher(imgPhoto, photoCourante)
        } else {
            chemin?.let { File(it).delete() }
            toast(R.string.msg_photo_annulee)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            photoCourante = savedInstanceState.getString(CLE_PHOTO)
            fichierCamera = savedInstanceState.getString(CLE_FICHIER_CAMERA)
            champsRemplis = savedInstanceState.getBoolean(CLE_CHAMPS_REMPLIS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgPhoto = view.findViewById(R.id.imgPhoto)
        etNom = view.findViewById(R.id.etNom)
        etPrenom = view.findViewById(R.id.etPrenom)
        etTelephone = view.findViewById(R.id.etTelephone)
        etEmail = view.findViewById(R.id.etEmail)
        etLatitude = view.findViewById(R.id.etLatitude)
        etLongitude = view.findViewById(R.id.etLongitude)
        PhotoUtils.afficher(imgPhoto, photoCourante)
        appliquerPreferences()

        val btnSupprimer = view.findViewById<Button>(R.id.btnSupprimer)
        btnSupprimer.visibility = if (modeCreation) View.GONE else View.VISIBLE
        btnSupprimer.setOnClickListener { confirmerSuppression() }
        view.findViewById<Button>(R.id.btnGalerie).setOnClickListener { ouvrirGalerie() }
        view.findViewById<Button>(R.id.btnCamera).setOnClickListener { ouvrirCamera() }
        view.findViewById<Button>(R.id.btnAppeler).setOnClickListener { appeler() }
        view.findViewById<Button>(R.id.btnEmail).setOnClickListener { envoyerEmail() }
        view.findViewById<Button>(R.id.btnCarte).setOnClickListener { voirSurLaCarte() }
        view.findViewById<Button>(R.id.btnEnregistrer).setOnClickListener { enregistrer() }
        view.findViewById<Button>(R.id.btnAnnuler).setOnClickListener { parentFragmentManager.popBackStack() }

        if (modeCreation) {
            champsRemplis = true
            return
        }
        // Read : la fiche est chargée depuis la base, hors du thread principal
        viewLifecycleOwner.lifecycleScope.launch {
            val charge = viewModel.getById(idContact)
            if (charge == null) {
                toast(R.string.msg_contact_introuvable)
                parentFragmentManager.popBackStack()
                return@launch
            }
            contact = charge
            if (!champsRemplis) remplirChamps(charge)
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar
            ?.setTitle(if (modeCreation) R.string.titre_creation else R.string.titre_detail)
    }

    private fun remplirChamps(c: Contact) {
        etNom.setText(c.nom)
        etPrenom.setText(c.prenom)
        etTelephone.setText(c.telephone)
        etEmail.setText(c.email.orEmpty())
        etLatitude.setText(c.latitude?.toString().orEmpty())
        etLongitude.setText(c.longitude?.toString().orEmpty())
        photoCourante = c.photoUri
        PhotoUtils.afficher(imgPhoto, photoCourante)
        champsRemplis = true
    }

    /** Taille de police (nom, prénom, téléphone) et couleur (nom, prénom) choisies dans les Paramètres. */
    private fun appliquerPreferences() {
        val reglages = PreferencesManager(requireContext()).lire()
        listOf(etNom, etPrenom, etTelephone).forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, reglages.tailleSp) }
        listOf(etNom, etPrenom).forEach { it.setTextColor(reglages.couleur) }
    }

    // ---------- Photo ----------

    private fun ouvrirGalerie() {
        try {
            choisirPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_aucune_galerie)
        }
    }

    private fun ouvrirCamera() {
        val contexte = requireContext()
        if (!contexte.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(R.string.msg_camera_indisponible)
            return
        }
        val fichier = PhotoUtils.nouveauFichier(contexte)
        fichierCamera = fichier.absolutePath
        try {
            prendrePhoto.launch(PhotoUtils.uriPartageable(contexte, fichier))
        } catch (e: ActivityNotFoundException) {
            fichierCamera = null
            toast(R.string.msg_camera_indisponible)
        } catch (e: SecurityException) {
            fichierCamera = null
            toast(R.string.msg_camera_refusee)
        }
    }

    // ---------- Intents implicites ----------

    private fun appeler() {
        val numero = etTelephone.text.toString().trim()
        if (numero.isEmpty()) {
            etTelephone.error = getString(R.string.erreur_obligatoire)
            return
        }
        lancer(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(numero))), R.string.msg_aucune_app_telephone)
    }

    private fun envoyerEmail() {
        val adresse = etEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(adresse).matches()) {
            etEmail.error = getString(R.string.erreur_email)
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(adresse))
        }
        lancer(intent, R.string.msg_aucune_app_email)
    }

    private fun lancer(intent: Intent, messageErreur: Int) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(messageErreur)
        }
    }

    // ---------- Coordonnées GPS ----------

    private sealed class Position {
        data object Absente : Position()
        data class Valide(val latitude: Double, val longitude: Double) : Position()
        data object Invalide : Position()
    }

    /** Deux champs vides = aucune position ; sinon numériques, latitude ∈ [−90 ; 90], longitude ∈ [−180 ; 180]. */
    private fun lirePosition(): Position {
        val texteLat = etLatitude.text.toString().trim().replace(',', '.')
        val texteLon = etLongitude.text.toString().trim().replace(',', '.')
        if (texteLat.isEmpty() && texteLon.isEmpty()) return Position.Absente

        var valide = true
        val lat = texteLat.toDoubleOrNull()
        val lon = texteLon.toDoubleOrNull()
        when {
            texteLat.isEmpty() -> { etLatitude.error = getString(R.string.erreur_coordonnee_manquante); valide = false }
            lat == null -> { etLatitude.error = getString(R.string.erreur_non_numerique); valide = false }
            lat !in -90.0..90.0 -> { etLatitude.error = getString(R.string.erreur_latitude); valide = false }
        }
        when {
            texteLon.isEmpty() -> { etLongitude.error = getString(R.string.erreur_coordonnee_manquante); valide = false }
            lon == null -> { etLongitude.error = getString(R.string.erreur_non_numerique); valide = false }
            lon !in -180.0..180.0 -> { etLongitude.error = getString(R.string.erreur_longitude); valide = false }
        }
        return if (valide && lat != null && lon != null) Position.Valide(lat, lon) else Position.Invalide
    }

    private fun voirSurLaCarte() {
        val position = lirePosition()
        if (position is Position.Invalide) {
            toast(R.string.msg_coordonnees_invalides)
            return
        }
        val nom = getString(R.string.nom_complet, etPrenom.text.toString().trim(), etNom.text.toString().trim())
        val fragment = when (position) {
            is Position.Valide -> CarteContactFragment.nouvelleInstance(nom, position.latitude, position.longitude)
            else -> CarteContactFragment.nouvelleInstance(nom, null, null)
        }
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
            addToBackStack("carte")
        }
    }

    // ---------- Create / Update / Delete ----------

    private fun enregistrer() {
        val nom = etNom.text.toString().trim()
        val prenom = etPrenom.text.toString().trim()
        val telephone = etTelephone.text.toString().trim()
        val email = etEmail.text.toString().trim()

        // Validation : rien n'est écrit en base si une contrainte n'est pas respectée
        var valide = true
        if (nom.isEmpty()) { etNom.error = getString(R.string.erreur_obligatoire); valide = false }
        if (prenom.isEmpty()) { etPrenom.error = getString(R.string.erreur_obligatoire); valide = false }
        if (telephone.isEmpty()) { etTelephone.error = getString(R.string.erreur_obligatoire); valide = false }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.erreur_email); valide = false
        }
        val position = lirePosition()
        if (position is Position.Invalide) valide = false
        if (!valide) {
            toast(R.string.msg_champs_invalides)
            return
        }
        if (!modeCreation && contact == null) return // chargement encore en cours

        val aEnregistrer = Contact(
            id = idContact,
            nom = nom, prenom = prenom, telephone = telephone,
            email = email.ifEmpty { null },
            photoUri = photoCourante,
            latitude = (position as? Position.Valide)?.latitude,
            longitude = (position as? Position.Valide)?.longitude
        )
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.enregistrer(aEnregistrer)) {
                toast(if (modeCreation) R.string.msg_cree else R.string.msg_enregistre)
                parentFragmentManager.popBackStack()
            } else {
                toast(if (modeCreation) R.string.msg_insertion_echouee else R.string.msg_contact_introuvable)
            }
        }
    }

    private fun confirmerSuppression() {
        val c = contact ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.titre_suppression)
            .setMessage(getString(R.string.msg_confirmer_suppression, getString(R.string.nom_complet, c.prenom, c.nom)))
            .setPositiveButton(R.string.btn_supprimer) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (viewModel.supprimer(c.id)) {
                        toast(R.string.msg_supprime)
                        parentFragmentManager.popBackStack()
                    } else {
                        toast(R.string.msg_contact_introuvable)
                    }
                }
            }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }

    private fun toast(message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CLE_PHOTO, photoCourante)
        outState.putString(CLE_FICHIER_CAMERA, fichierCamera)
        outState.putBoolean(CLE_CHAMPS_REMPLIS, champsRemplis)
    }

    companion object {
        private const val ARG_ID = "id_contact"
        private const val CLE_PHOTO = "photo"
        private const val CLE_FICHIER_CAMERA = "fichier_camera"
        private const val CLE_CHAMPS_REMPLIS = "champs_remplis"

        /** @param id identifiant _id du contact, ou 0 pour créer un nouveau contact */
        fun nouvelleInstance(id: Long) = DetailContactFragment().apply {
            arguments = bundleOf(ARG_ID to id)
        }
    }
}
