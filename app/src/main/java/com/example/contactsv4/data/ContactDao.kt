package com.example.contactsv4.data

import android.database.Cursor
import com.example.contactsv4.data.ContactContract.COL_EMAIL
import com.example.contactsv4.data.ContactContract.COL_ID
import com.example.contactsv4.data.ContactContract.COL_LATITUDE
import com.example.contactsv4.data.ContactContract.COL_LONGITUDE
import com.example.contactsv4.data.ContactContract.COL_NOM
import com.example.contactsv4.data.ContactContract.COL_PHOTO_URI
import com.example.contactsv4.data.ContactContract.COL_PRENOM
import com.example.contactsv4.data.ContactContract.COL_TELEPHONE
import com.example.contactsv4.data.ContactContract.ORDRE_TRI
import com.example.contactsv4.data.ContactContract.TABLE

/**
 * Seule classe contenant du SQL. Toutes les valeurs variables passent par des arguments « ? »
 * (selectionArgs) : aucune saisie utilisateur n'est concaténée dans une requête.
 * Ces méthodes sont bloquantes : elles doivent être appelées hors du thread principal.
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l'_id du nouveau contact, ou -1 en cas d'échec */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(TABLE, null, ContactDbHelper.versValeurs(contact))

    /** @return le nombre de lignes modifiées (0 si le contact n'existe plus) */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            TABLE, ContactDbHelper.versValeurs(contact), "$COL_ID = ?", arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    /** Tous les contacts, triés par nom puis prénom (insensible à la casse). */
    fun getAll(): List<Contact> =
        helper.readableDatabase.query(TABLE, null, null, null, null, null, ORDRE_TRI).use { lireTous(it) }

    fun getById(id: Long): Contact? =
        helper.readableDatabase.query(TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null)
            .use { if (it.moveToFirst()) lireContact(it) else null }

    /**
     * Recherche insensible à la casse (LIKE) sur le nom, le prénom et le téléphone.
     * Une apostrophe (« O'Neil ») ne pose aucun problème car le texte est passé en argument.
     * Les caractères spéciaux de LIKE (% et _) sont échappés.
     */
    fun search(texte: String): List<Contact> {
        val recherche = texte.trim()
        if (recherche.isEmpty()) return getAll()
        val motif = "%" + recherche.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        val motifTelephone = "%" + recherche.replace(" ", "").replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_") + "%"
        val selection = "$COL_NOM LIKE ? ESCAPE '\\' OR $COL_PRENOM LIKE ? ESCAPE '\\' OR " +
            "($COL_PRENOM || ' ' || $COL_NOM) LIKE ? ESCAPE '\\' OR " +
            "REPLACE($COL_TELEPHONE, ' ', '') LIKE ? ESCAPE '\\'"
        return helper.readableDatabase.query(
            TABLE, null, selection, arrayOf(motif, motif, motif, motifTelephone), null, null, ORDRE_TRI
        ).use { lireTous(it) }
    }

    private fun lireTous(cursor: Cursor): List<Contact> {
        val liste = mutableListOf<Contact>()
        while (cursor.moveToNext()) liste.add(lireContact(cursor))
        return liste
    }

    /** Lecture d'une ligne : les index sont obtenus par getColumnIndexOrThrow(). */
    private fun lireContact(c: Cursor): Contact {
        val iLat = c.getColumnIndexOrThrow(COL_LATITUDE)
        val iLon = c.getColumnIndexOrThrow(COL_LONGITUDE)
        val iEmail = c.getColumnIndexOrThrow(COL_EMAIL)
        val iPhoto = c.getColumnIndexOrThrow(COL_PHOTO_URI)
        return Contact(
            id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
            nom = c.getString(c.getColumnIndexOrThrow(COL_NOM)),
            prenom = c.getString(c.getColumnIndexOrThrow(COL_PRENOM)),
            telephone = c.getString(c.getColumnIndexOrThrow(COL_TELEPHONE)),
            email = if (c.isNull(iEmail)) null else c.getString(iEmail),
            photoUri = if (c.isNull(iPhoto)) null else c.getString(iPhoto),
            latitude = if (c.isNull(iLat)) null else c.getDouble(iLat),
            longitude = if (c.isNull(iLon)) null else c.getDouble(iLon)
        )
    }
}
