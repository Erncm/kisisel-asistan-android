package com.example.kisisel_asistan

import android.content.Context
import android.provider.ContactsContract

object KisiRehberi {
    fun telefonNumarasiBul(context: Context, isim: String): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$isim%"),
            null
        ) ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                val numaraIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val hamNumara = it.getString(numaraIndex) ?: return null
                return numarayiNormallestir(hamNumara)
            }
        }
        return null
    }

    private fun numarayiNormallestir(numara: String): String {
        var temiz = numara.filter { it.isDigit() }
        temiz = when {
            temiz.startsWith("0") && temiz.length == 11 -> "90" + temiz.substring(1)
            temiz.length == 10 -> "90$temiz"
            else -> temiz
        }
        return temiz
    }
}
