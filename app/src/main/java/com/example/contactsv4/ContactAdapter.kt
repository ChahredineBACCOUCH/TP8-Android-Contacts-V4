package com.example.contactsv4

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsv4.data.Contact

/**
 * Adapter de la RecyclerView. Il reçoit des préférences déjà converties ([reglages])
 * et identifie chaque contact par son id de base, jamais par sa position.
 */
class ContactAdapter(
    private val onClic: (Contact) -> Unit,
    private val onAppeler: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(Comparateur) {

    var reglages = PreferencesManager.Reglages(16f, 0xFF000000.toInt(), false)
        private set

    /** Applique de nouveaux réglages et redessine toutes les lignes. */
    fun appliquerReglages(nouveaux: PreferencesManager.Reglages) {
        if (nouveaux == reglages) return
        reglages = nouveaux
        notifyItemRangeChanged(0, itemCount)
    }

    class ContactViewHolder(vue: View) : RecyclerView.ViewHolder(vue) {
        val photo: ImageView = vue.findViewById(R.id.imgPhoto)
        val nom: TextView = vue.findViewById(R.id.txtNomComplet)
        val telephone: TextView = vue.findViewById(R.id.txtTelephone)
        val btnAppeler: ImageButton = vue.findViewById(R.id.btnAppelerLigne)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val vue = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(vue)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.nom.text = holder.itemView.context.getString(R.string.nom_complet, contact.prenom, contact.nom)
        holder.telephone.text = contact.telephone

        // Préférences : taille (nom, prénom, téléphone) et couleur (nom et prénom)
        holder.nom.setTextSize(TypedValue.COMPLEX_UNIT_SP, reglages.tailleSp + 2)
        holder.telephone.setTextSize(TypedValue.COMPLEX_UNIT_SP, reglages.tailleSp)
        holder.nom.setTextColor(reglages.couleur)

        PhotoUtils.afficher(holder.photo, contact.photoUri, tailleMax = 128)
        holder.itemView.setOnClickListener { onClic(contact) }

        // Bouton Appeler : visible seulement si la préférence est activée ;
        // son clic est consommé par le bouton et n'ouvre donc pas la fiche détaillée.
        holder.btnAppeler.visibility = if (reglages.appelDepuisListe) View.VISIBLE else View.GONE
        holder.btnAppeler.setOnClickListener { onAppeler(contact) }
    }

    private object Comparateur : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(ancien: Contact, nouveau: Contact) = ancien.id == nouveau.id
        override fun areContentsTheSame(ancien: Contact, nouveau: Contact) = ancien == nouveau
    }
}
