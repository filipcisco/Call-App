package com.kapp.call_app.models

import android.os.Build
import android.telephony.PhoneNumberUtils
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList

data class SimpleContact(
    val rawId: Int, val contactId: Int, var name: String, var photoUri: String, var phoneNumbers: ArrayList<PhoneNumber>,
    var birthdays: ArrayList<String>, var anniversaries: ArrayList<String>
) : Comparable<SimpleContact> {

    companion object {
        var sorting = -1
        const val SORT_DESCENDING = 1024
        const val SORT_BY_FULL_NAME = 65536
        val normalizeRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    }

    override fun compareTo(other: SimpleContact): Int {
        if (sorting == -1) {
            return compareByFullName(other)
        }

        var result = when {
            sorting and SORT_BY_FULL_NAME != 0 -> compareByFullName(other)
            else -> rawId.compareTo(other.rawId)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    private fun compareByFullName(other: SimpleContact): Int {
        val firstString = Normalizer.normalize(name, Normalizer.Form.NFD).replace(normalizeRegex, "")
        val secondString = Normalizer.normalize(other.name, Normalizer.Form.NFD).replace(normalizeRegex, "")

        return if (firstString.firstOrNull()?.isLetter() == true && secondString.firstOrNull()?.isLetter() == false) {
            -1
        } else if (firstString.firstOrNull()?.isLetter() == false && secondString.firstOrNull()?.isLetter() == true) {
            1
        } else {
            if (firstString.isEmpty() && secondString.isNotEmpty()) {
                1
            } else if (firstString.isNotEmpty() && secondString.isEmpty()) {
                -1
            } else {
                firstString.compareTo(secondString, true)
            }
        }
    }

    fun doesContainPhoneNumber(text: String): Boolean {
        return if (text.isNotEmpty()) {
            val normalizedText = PhoneNumberUtils.normalizeNumber(text)
            if (normalizedText.isEmpty()) {
                phoneNumbers.map { it.normalizedNumber }.any { phoneNumber ->
                    phoneNumber.contains(text)
                }
            } else {
                phoneNumbers.map { it.normalizedNumber }.any { phoneNumber ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PhoneNumberUtils.areSamePhoneNumber(PhoneNumberUtils.normalizeNumber(phoneNumber), normalizedText, PhoneNumberUtils.formatNumber(phoneNumber,
                            Locale.getDefault().country
                        )) ||
                                phoneNumber.contains(text) ||
                                PhoneNumberUtils.normalizeNumber(phoneNumber).contains(normalizedText) ||
                                phoneNumber.contains(normalizedText)
                    } else {
                        PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(phoneNumber), normalizedText) ||
                                phoneNumber == text ||
                                PhoneNumberUtils.normalizeNumber(phoneNumber) == normalizedText ||
                                phoneNumber == normalizedText
                    }
                }
            }
        } else {
            false
        }
    }

    fun doesHavePhoneNumber(text: String): Boolean {
        return if (text.isNotEmpty()) {
            val normalizedText = PhoneNumberUtils.normalizeNumber(text)
            if (normalizedText.isEmpty()) {
                phoneNumbers.map { it.normalizedNumber }.any { phoneNumber ->
                    phoneNumber == text
                }
            } else {
                phoneNumbers.map { it.normalizedNumber }.any { phoneNumber ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PhoneNumberUtils.areSamePhoneNumber(PhoneNumberUtils.normalizeNumber(phoneNumber), normalizedText, PhoneNumberUtils.formatNumber(phoneNumber,
                            Locale.getDefault().country
                        )) ||
                                phoneNumber.contains(text) ||
                                PhoneNumberUtils.normalizeNumber(phoneNumber).contains(normalizedText) ||
                                phoneNumber.contains(normalizedText)
                    } else {
                        PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(phoneNumber), normalizedText) ||
                                phoneNumber == text ||
                                PhoneNumberUtils.normalizeNumber(phoneNumber) == normalizedText ||
                                phoneNumber == normalizedText
                    }
                }
            }
        } else {
            false
        }
    }
}