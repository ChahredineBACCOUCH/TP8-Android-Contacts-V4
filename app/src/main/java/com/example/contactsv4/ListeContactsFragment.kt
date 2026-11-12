package com.example.contactsv4

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsv4.data.Contact
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** Liste des contacts (SQLite), filtrage au fil de la saisie, bouton « + », appel depuis la liste. */
class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val viewModel: ContactViewModel by activityViewModels()

    private lateinit var adapter: ContactAdapter
    private lateinit var txtAucunResultat: TextView
    private lateinit var preferences: PreferencesManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        txtAucunResultat = view.findViewById(R.id.txtAucunResultat)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerContacts)
        adapter = ContactAdapter(
            onClic = { contact -> ouvrirDetail(contact.id) },
            onAppeler = { contact -> appeler(contact) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Filtrage au fur et à mesure de la saisie (requête SQL LIKE paramétrée)
        val etRecherche = view.findViewById<EditText>(R.id.etRecherche)
        etRecherche.doAfterTextChanged { texte -> viewModel.filtrer(texte?.toString().orEmpty()) }

        // Create : le bouton flottant ouvre le détail en mode création (id = 0)
        view.findViewById<FloatingActionButton>(R.id.fabAjouter).setOnClickListener { ouvrirDetail(0L) }

        viewModel.contacts.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            txtAucunResultat.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
            txtAucunResultat.setText(
                if (viewModel.filtre.isBlank()) R.string.msg_liste_vide else R.string.msg_aucun_resultat
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.titre_liste)
        // Au retour des Paramètres : relecture et application immédiate des préférences
        adapter.appliquerReglages(preferences.lire())
    }

    private fun ouvrirDetail(id: Long) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, DetailContactFragment.nouvelleInstance(id))
            addToBackStack("detail")
        }
    }

    /** ACTION_DIAL : ouvre le composeur (aucune permission requise, contrairement à ACTION_CALL). */
    private fun appeler(contact: Contact) {
        val numero = contact.telephone.trim()
        if (numero.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_numero_vide, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(numero))))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.msg_aucune_app_telephone, Toast.LENGTH_SHORT).show()
        }
    }
}
