package com.kapp.call_app.call_logs

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
import com.kapp.call_app.models.CallLogDetail
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object ColumnMapper {
    fun mapCallLogType(cursor: Cursor, callLogDetail: CallLogDetail, idxType: Int) {
        callLogDetail.type = cursor.getString(idxType)
    }

    fun mapCallLogNumber(cursor: Cursor, callLogDetail: CallLogDetail, idxNumber: Int) {
        callLogDetail.number = cursor.getString(idxNumber)
    }

    fun mapCallLogId(cursor: Cursor, callLogDetail: CallLogDetail, idxID: Int) {
        callLogDetail.id = cursor.getString(idxID)
    }
    fun mapCallLogDate(cursor: Cursor, callLogDetail: CallLogDetail, idxDate: Int){
        callLogDetail.date = cursor.getString(idxDate)
        //val timeLong = cursor.getString(idxDate).toLong()
        //val date = Date(timeLong)
        //callLogDetail.date = DateFormat.getDateTimeInstance().format(date)
    }
    fun mapCallLogDuration(cursor: Cursor, callLogDetail: CallLogDetail, idxDuration: Int){
        callLogDetail.duration = cursor.getString(idxDuration).toLong()
    }
    fun mapCallLogName(cursor: Cursor, callLogDetail: CallLogDetail, idxName: Int){
        callLogDetail.name = cursor.getString(idxName)
    }
}