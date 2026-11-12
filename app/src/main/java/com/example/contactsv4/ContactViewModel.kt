package com.example.contactsv4

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactsv4.data.Contact
import com.example.contactsv4.data.ContactDao
import com.example.contactsv4.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel partagé (portée : MainActivity). Il lance les appels au DAO dans viewModelScope,
 * sur Dispatchers.IO (jamais sur le thread principal), et republie la liste après chaque modification.
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))

    private val _contacts = MutableLiveData<List<Contact>>()

    /** Liste affichée : tous les contacts ou le résultat du filtre courant. */
    val contacts: LiveData<List<Contact>> = _contacts

    /** Filtre courant (conservé pendant la navigation et la rotation). */
    var filtre: String = ""
        private set

    init {
        rafraichir()
    }

    /** Recharge la liste depuis la base en appliquant le filtre (recherche SQL LIKE). */
    fun rafraichir() {
        val texte = filtre
        viewModelScope.launch {
            val resultat = withContext(Dispatchers.IO) { dao.search(texte) }
            if (texte == filtre) _contacts.value = resultat // ignore une réponse devenue obsolète
        }
    }

    fun filtrer(texte: String) {
        if (texte == filtre && _contacts.value != null) return
        filtre = texte
        rafraichir()
    }

    suspend fun getById(id: Long): Contact? = withContext(Dispatchers.IO) { dao.getById(id) }

    /** Insère (id = 0) ou met à jour un contact. @return vrai si l'opération a réussi */
    suspend fun enregistrer(contact: Contact): Boolean {
        val ok = withContext(Dispatchers.IO) {
            if (contact.id == 0L) dao.insert(contact) != -1L else dao.update(contact) > 0
        }
        if (ok) rafraichir()
        return ok
    }

    /** @return vrai si une ligne a été supprimée */
    suspend fun supprimer(id: Long): Boolean {
        val ok = withContext(Dispatchers.IO) { dao.delete(id) > 0 }
        if (ok) rafraichir()
        return ok
    }
}
