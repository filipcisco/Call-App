package com.kapp.call_app.contacts

import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.text.TextUtils
import android.util.SparseArray
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.kapp.call_app.R
import com.kapp.call_app.core.PERMISSION_READ_CONTACTS
import com.kapp.call_app.extensions.hasPermission
import com.kapp.call_app.extensions.queryCursor
import com.kapp.call_app.models.PhoneNumber
import com.kapp.call_app.models.SimpleContact
import com.kapp.call_app.models.SimpleContact.Companion.normalizeRegex
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class SimpleContactsHelper(val context: Context) {

    private val letterBackgroundColors = arrayListOf(
        0xCCD32F2F,
        0xCCC2185B,
        0xCC1976D2,
        0xCC0288D1,
        0xCC0097A7,
        0xCC00796B,
        0xCC388E3C,
        0xCC689F38,
        0xCCF57C00,
        0xCCE64A19
    )
    private val DARK_GREY = 0xFF333333.toInt()


    fun getAvailableContacts(favoritesOnly: Boolean, callback: (ArrayList<SimpleContact>) -> Unit) {
        ensureBackgroundThread {
            val names = getContactNames(favoritesOnly)
            var allContacts = getContactPhoneNumbers(favoritesOnly)
            allContacts.forEach {
                val contactId = it.rawId
                val contact = names.firstOrNull { it.rawId == contactId }
                val name = contact?.name
                if (name != null) {
                    it.name = name
                }

                val photoUri = contact?.photoUri
                if (photoUri != null) {
                    it.photoUri = photoUri
                }
            }

            allContacts = allContacts.filter { it.name.isNotEmpty() }.distinctBy {
                val startIndex = Math.max(0, it.phoneNumbers.first().normalizedNumber.length - 9)
                it.phoneNumbers.first().normalizedNumber.substring(startIndex)
            }.distinctBy { it.rawId }.toMutableList() as ArrayList<SimpleContact>

            // if there are duplicate contacts with the same name, while the first one has phone numbers 1234 and 4567, second one has only 4567,
            // use just the first contact
            val contactsToRemove = ArrayList<SimpleContact>()
            allContacts.groupBy { it.name }.forEach {
                val contacts = it.value.toMutableList() as ArrayList<SimpleContact>
                if (contacts.size > 1) {
                    contacts.sortByDescending { it.phoneNumbers.size }
                    if (contacts.any { it.phoneNumbers.size == 1 } && contacts.any { it.phoneNumbers.size > 1 }) {
                        val multipleNumbersContact = contacts.first()
                        contacts.subList(1, contacts.size).forEach { contact ->
                            if (contact.phoneNumbers.all { multipleNumbersContact.doesContainPhoneNumber(it.normalizedNumber) }) {
                                val contactToRemove = allContacts.firstOrNull { it.rawId == contact.rawId }
                                if (contactToRemove != null) {
                                    contactsToRemove.add(contactToRemove)
                                }
                            }
                        }
                    }
                }
            }

            contactsToRemove.forEach {
                allContacts.remove(it)
            }

            val birthdays = getContactEvents(true)
            var size = birthdays.size()
            for (i in 0 until size) {
                val key = birthdays.keyAt(i)
                allContacts.firstOrNull { it.rawId == key }?.birthdays = birthdays.valueAt(i)
            }

            val anniversaries = getContactEvents(false)
            size = anniversaries.size()
            for (i in 0 until size) {
                val key = anniversaries.keyAt(i)
                allContacts.firstOrNull { it.rawId == key }?.anniversaries = anniversaries.valueAt(i)
            }

            allContacts.sort()
            callback(allContacts)
        }
    }

    private fun getContactNames(favoritesOnly: Boolean): List<SimpleContact> {
        val contacts = ArrayList<SimpleContact>()
        val startNameWithSurname = true
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.StructuredName.PREFIX,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
            ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Organization.COMPANY,
            ContactsContract.CommonDataKinds.Organization.TITLE,
            ContactsContract.Data.MIMETYPE
        )

        var selection = "(${ContactsContract.Data.MIMETYPE} = ? OR ${ContactsContract.Data.MIMETYPE} = ?)"

        if (favoritesOnly) {
            selection += " AND ${ContactsContract.Data.STARRED} = 1"
        }

        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
        )

        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val rawId = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))
            val contactId = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
            val mimetype = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE))
            val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI)) ?: ""
            val isPerson = mimetype == ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            if (isPerson) {
                val prefix = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PREFIX)) ?: ""
                val firstName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)) ?: ""
                val middleName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)) ?: ""
                val familyName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)) ?: ""
                val suffix = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)) ?: ""
                if (firstName.isNotEmpty() || middleName.isNotEmpty() || familyName.isNotEmpty()) {
                    val names = if (startNameWithSurname) {
                        arrayOf(prefix, familyName, middleName, firstName, suffix).filter { it.isNotEmpty() }
                    } else {
                        arrayOf(prefix, firstName, middleName, familyName, suffix).filter { it.isNotEmpty() }
                    }

                    val fullName = TextUtils.join(" ", names)
                    val contact = SimpleContact(rawId, contactId, fullName, photoUri, ArrayList(), ArrayList(), ArrayList())
                    contacts.add(contact)
                }
            }

            val isOrganization = mimetype == ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            if (isOrganization) {
                val company = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)) ?: ""
                val jobTitle = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE)) ?: ""
                if (company.isNotEmpty() || jobTitle.isNotEmpty()) {
                    val fullName = "$company $jobTitle".trim()
                    val contact = SimpleContact(rawId, contactId, fullName, photoUri, ArrayList(), ArrayList(), ArrayList())
                    contacts.add(contact)
                }
            }
        }
        return contacts
    }

    private fun getContactPhoneNumbers(favoritesOnly: Boolean): ArrayList<SimpleContact> {
        val contacts = ArrayList<SimpleContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        val selection = if (favoritesOnly) "${ContactsContract.Data.STARRED} = 1" else null

        context.queryCursor(uri, projection, selection) { cursor ->
            val normalizedNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                ?: PhoneNumberUtils.normalizeNumber(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))) ?: return@queryCursor

            val rawId = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))
            val contactId = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
            val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)) ?: ""
            if (contacts.firstOrNull { it.rawId == rawId } == null) {
                val contact = SimpleContact(rawId, contactId, "", "", ArrayList(), ArrayList(), ArrayList())
                contacts.add(contact)
            }

            val phoneNumber = PhoneNumber(normalizedNumber, type, label, normalizedNumber)
            contacts.firstOrNull { it.rawId == rawId }?.phoneNumbers?.add(phoneNumber)
        }
        return contacts
    }

    fun getContactEvents(getBirthdays: Boolean): SparseArray<ArrayList<String>> {
        val eventDates = SparseArray<ArrayList<String>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.Event.START_DATE
        )

        val selection = "${ContactsContract.CommonDataKinds.Event.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val requiredType = if (getBirthdays) ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString() else ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY.toString()
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, requiredType)

        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))
            val startDate = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)) ?: return@queryCursor

            if (eventDates[id] == null) {
                eventDates.put(id, ArrayList())
            }

            eventDates[id]!!.add(startDate)
        }

        return eventDates
    }


    fun getNameFromPhoneNumber(number: String): String {
        if (!context.hasPermission(PERMISSION_READ_CONTACTS)) {
            return number
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME
        )

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }
        } catch (ignored: Exception) {
        }

        return number
    }

    fun getPhotoUriFromPhoneNumber(number: String): String {
        if (!context.hasPermission(PERMISSION_READ_CONTACTS)) {
            return ""
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)) ?: ""
                }
            }
        } catch (ignored: Exception) {
        }

        return ""
    }

    fun loadContactImage(path: String, imageView: ImageView, placeholderName: String, placeholderImage: Drawable? = null) {
        val placeholder = placeholderImage ?: BitmapDrawable(context.resources, getContactLetterIcon(placeholderName))

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .error(placeholder)
            .centerCrop()

        Glide.with(context)
            .load(path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(placeholder)
            .apply(options)
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
    }

    private fun getContrastColor(color: Int): Int {
        val y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000
        return if (y >= 149 && color != Color.BLACK) DARK_GREY else Color.WHITE
    }

    private fun getNameLetter(name: String) = Normalizer.normalize(name, Normalizer.Form.NFD).replace(normalizeRegex, "").toCharArray().getOrNull(0)?.toString()?.toUpperCase(
        Locale.getDefault()) ?: "A"


    fun getContactLetterIcon(name: String): Bitmap {
        val letter = getNameLetter(name)
        val size = context.resources.getDimension(R.dimen.normal_icon_size).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val view = TextView(context)
        view.layout(0, 0, size, size)

        val circlePaint = Paint().apply {
            color = letterBackgroundColors[abs(name.hashCode()) % letterBackgroundColors.size].toInt()
            isAntiAlias = true
        }

        val wantedTextSize = size / 2f
        val textPaint = Paint().apply {
            color = getContrastColor(circlePaint.color)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = wantedTextSize
            style = Paint.Style.FILL
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        val xPos = canvas.width / 2f
        val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, xPos, yPos, textPaint)
        view.draw(canvas)
        return bitmap
    }

    fun getColoredGroupIcon(title: String): Drawable {
        val icon = ResourcesCompat.getDrawable(context.resources,R.drawable.voip_call_participants, context.theme)
        val bgColor = letterBackgroundColors[abs(title.hashCode()) % letterBackgroundColors.size].toInt()
        (icon as LayerDrawable).findDrawableByLayerId(R.id.background).apply {
            colorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BlendModeColorFilter(bgColor, BlendMode.COLOR)
            } else {
                PorterDuffColorFilter(bgColor, PorterDuff.Mode.MULTIPLY)
            }
        }
        return icon
    }

    fun getContactLookupKey(contactId: String): String {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Data.LOOKUP_KEY)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId)

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
                val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY))
                return "$lookupKey/$id"
            }
        }

        return ""
    }

    private fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')


    fun deleteContactRawIDs(ids: ArrayList<Int>, callback: () -> Unit) {
        ensureBackgroundThread {
            val uri = ContactsContract.Data.CONTENT_URI
            ids.chunked(30).forEach { chunk ->
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} IN (${getQuestionMarks(chunk.size)})"
                val selectionArgs = chunk.map { it.toString() }.toTypedArray()
                context.contentResolver.delete(uri, selection, selectionArgs)
            }
            callback()
        }
    }

    fun getShortcutImage(path: String, placeholderName: String, callback: (image: Bitmap) -> Unit) {
        ensureBackgroundThread {
            val placeholder = BitmapDrawable(context.resources, getContactLetterIcon(placeholderName))
            try {
                val options = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(placeholder)
                    .centerCrop()

                val size = context.resources.getDimension(R.dimen.contact_avatar_size).toInt()
                val bitmap = Glide.with(context).asBitmap()
                    .load(path)
                    .placeholder(placeholder)
                    .apply(options)
                    .apply(RequestOptions.circleCropTransform())
                    .into(size, size)
                    .get()

                callback(bitmap)
            } catch (ignored: Exception) {
                callback(placeholder.bitmap)
            }
        }
    }

    fun exists(number: String, privateCursor: Cursor?, callback: (Boolean) -> Unit) {
        SimpleContactsHelper(context).getAvailableContacts(false) { contacts ->
            val contact = contacts.firstOrNull { it.doesHavePhoneNumber(number) }
            if (contact != null) {
                callback.invoke(true)
            } else {
                val privateContacts = MyContactsContentPovider.getSimpleContacts(context, privateCursor)
                val privateContact = privateContacts.firstOrNull { it.doesHavePhoneNumber(number) }
                callback.invoke(privateContact != null)
            }
        }
    }
}

private operator fun String.times(size: Int): String {
    val stringBuilder = StringBuilder()
    for (i in 1..size) {
        stringBuilder.append(this)
    }
    return stringBuilder.toString()
}
