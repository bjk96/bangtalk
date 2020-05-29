package com.bang.bangtalk.model

class NotificationModel() {

    var to : String? = null
    //var notification : Notification? = Notification()
    var data : Data? = Data()
    var chatRoom : String? = null

    data class Notification(
        var title : String? = null,
        var body : String? = null
    )

    data class Data(
        var title : String? = null,
        var body : String? = null,
        var sender : String? = null,
        var receiver : String? = null,
        var roomId : String? = null
    )

}