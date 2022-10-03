package com.kapp.call_app.models

data class CallLogDetail (
    var number: String? = null,
    var date: String? = null,
    var type: String? = null,
    var duration: Long? = null,
    var name: String? = null,
    var simName: String? = null,
    var description: String? = null,
    var id: String? = null
)