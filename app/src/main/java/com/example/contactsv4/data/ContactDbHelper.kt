package com.example.contactsv4.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Création et mise à jour de la base contacts.db.
 *
 * onCreate n'est appelé que lorsque le fichier contacts.db n'existe pas encore :
 * les contacts de démonstration ne sont donc insérés qu'au premier lancement.
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ContactContract.SQL_CREATE)
        insererContactsDemo(db)
    }

    /**
     * Appelé quand DATABASE_VERSION augmente. Pour ce TP (version 1), il n'existe
     * aucune migration : on recrée la table. Une vraie migration utiliserait ALTER TABLE.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ContactContract.SQL_DROP)
        onCreate(db)
    }

    /** Au moins 5 contacts de démonstration, dont 2 sans coordonnées GPS. Insertion dans une transaction. */
    private fun insererContactsDemo(db: SQLiteDatabase) {
        val demo = listOf(
            Contact(nom = "Ben Ali", prenom = "Ahmed", telephone = "+216 22 345 678", email = "ahmed.benali@example.com", latitude = 36.8065, longitude = 10.1815),
            Contact(nom = "Trabelsi", prenom = "Sarra", telephone = "+216 98 765 432", email = "sarra.trabelsi@example.com", latitude = 34.7406, longitude = 10.7603),
            Contact(nom = "Gharbi", prenom = "Mohamed", telephone = "+216 55 123 456", email = "mohamed.gharbi@example.com", latitude = 35.8256, longitude = 10.6084),
            Contact(nom = "Jaziri", prenom = "Ines", telephone = "+216 29 876 543", email = "ines.jaziri@example.com"),
            Contact(nom = "Mansouri", prenom = "Youssef", telephone = "+216 50 111 222", email = "youssef.mansouri@example.com", latitude = 37.2744, longitude = 9.8739),
            Contact(nom = "Chaabane", prenom = "Amira", telephone = "+216 97 333 444", email = "amira.chaabane@example.com")
        )
        db.beginTransaction()
        try {
            demo.forEach { db.insert(ContactContract.TABLE, null, versValeurs(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: ContactDbHelper? = null

        /**
         * Instance unique (singleton) construite avec applicationContext :
         * une seule connexion partagée, et aucune fuite d'Activity.
         */
        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }

        /** Conversion Contact → ContentValues (l'id n'est jamais écrit : il est attribué par la base). */
        fun versValeurs(c: Contact) = ContentValues().apply {
            put(ContactContract.COL_NOM, c.nom)
            put(ContactContract.COL_PRENOM, c.prenom)
            put(ContactContract.COL_TELEPHONE, c.telephone)
            put(ContactContract.COL_EMAIL, c.email)
            put(ContactContract.COL_PHOTO_URI, c.photoUri)
            if (c.latitude != null && c.longitude != null) {
                put(ContactContract.COL_LATITUDE, c.latitude)
                put(ContactContract.COL_LONGITUDE, c.longitude)
            } else {
                // Position absente : NULL dans les deux colonnes GPS
                putNull(ContactContract.COL_LATITUDE)
                putNull(ContactContract.COL_LONGITUDE)
            }
        }
    }
}
