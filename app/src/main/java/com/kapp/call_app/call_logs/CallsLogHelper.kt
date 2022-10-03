package com.kapp.call_app.call_logs

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CallLog

import com.kapp.call_app.models.CallLogDetail




class CallsLogHelper(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun fetch(limit: Int = 100, offset: Int=0): List<CallLogDetail>? {
        val callLogDetailLongSparseArray: ArrayList<CallLogDetail> = ArrayList()
        // Create a new cursor and go to the first position
        val cursor: Cursor? = createCursor(limit, offset)
        if (cursor != null) {
            cursor.moveToFirst()
            // Get the column indexes
            val idxNumber: Int = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val idxType: Int = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val idxID: Int = cursor.getColumnIndex(CallLog.Calls._ID)
            val idxDate: Int = cursor.getColumnIndex(CallLog.Calls.DATE)
            val idxName: Int = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val idxDuration: Int = cursor.getColumnIndex(CallLog.Calls.DURATION)

            // Map the columns to the fields of the contact
            while (!cursor.isAfterLast) {
                val callLogDetail = CallLogDetail()
                // Get data using cursor.getString(index) and map it to callLogDetail object
                ColumnMapper.mapCallLogType(cursor, callLogDetail, idxType)
                ColumnMapper.mapCallLogNumber(cursor, callLogDetail, idxNumber)
                ColumnMapper.mapCallLogId(cursor, callLogDetail, idxID)
                ColumnMapper.mapCallLogDate(cursor, callLogDetail, idxDate)
                ColumnMapper.mapCallLogDuration(cursor, callLogDetail, idxDuration)
                ColumnMapper.mapCallLogName(cursor, callLogDetail, idxName)


                // Add the contact to the collection
                callLogDetailLongSparseArray.add(callLogDetail)
                cursor.moveToNext()
            }
            // Close the cursor
            cursor.close()
            return callLogDetailLongSparseArray
        }
        else{
            return null
        }
    }

    fun fetchWithType(limit: Int = 100, offset: Int=0, type: Int): List<CallLogDetail>? {
        val callLogDetailLongSparseArray: ArrayList<CallLogDetail> = ArrayList()
        // Create a new cursor and go to the first position
        val cursor: Cursor? = createCursorWithType(limit, offset, type)
        if (cursor != null) {
            cursor.moveToFirst()
            // Get the column indexes
            val idxNumber: Int = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val idxType: Int = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val idxID: Int = cursor.getColumnIndex(CallLog.Calls._ID)
            val idxDate: Int = cursor.getColumnIndex(CallLog.Calls.DATE)
            val idxName: Int = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val idxDuration: Int = cursor.getColumnIndex(CallLog.Calls.DURATION)

            // Map the columns to the fields of the contact
            while (!cursor.isAfterLast) {
                val callLogDetail = CallLogDetail()
                // Get data using cursor.getString(index) and map it to callLogDetail object
                ColumnMapper.mapCallLogType(cursor, callLogDetail, idxType)
                ColumnMapper.mapCallLogNumber(cursor, callLogDetail, idxNumber)
                ColumnMapper.mapCallLogId(cursor, callLogDetail, idxID)
                ColumnMapper.mapCallLogDate(cursor, callLogDetail, idxDate)
                ColumnMapper.mapCallLogDuration(cursor, callLogDetail, idxDuration)
                ColumnMapper.mapCallLogName(cursor, callLogDetail, idxName)


                // Add the contact to the collection
                callLogDetailLongSparseArray.add(callLogDetail)
                cursor.moveToNext()
            }
            // Close the cursor
            cursor.close()
            return callLogDetailLongSparseArray
        }
        else{
            return null
        }
    }

    private fun createCursor(limit: Int, offset: Int): Cursor? {
        val sortOrder = CallLog.Calls.DATE + " DESC limit " + limit + " offset " + offset
        return resolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls._ID
            ),
            null,
            null,
            sortOrder
        )
    }

    private fun createCursorWithType(limit: Int, offset: Int, type: Int ): Cursor?{
        val sortOrder = CallLog.Calls.DATE + " DESC limit " + limit + " offset " + offset

        return resolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls._ID
            ),
            CallLog.Calls.TYPE + "='$type'",
            null,
            sortOrder
        )
    }
}