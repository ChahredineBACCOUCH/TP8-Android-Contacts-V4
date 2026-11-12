package com.example.contactsv4.data

/**
 * Modèle d'un contact.
 *
 * @property id vaut 0 tant que le contact n'est pas enregistré, puis prend la valeur _id attribuée par la base
 * @property photoUri chemin d'une copie de l'image dans le stockage interne (filesDir), ou null
 */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
