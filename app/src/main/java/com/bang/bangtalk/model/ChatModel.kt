package com.bang.bangtalk.model

import java.util.*
import kotlin.collections.HashMap

class ChatModel() {

    var users = HashMap<String, User>()
    var comments = HashMap<String, Comment>() //채팅방 대화내용
    var roomName = HashMap<String, String>() //채팅방 이름
    var recent : Long? = null
    var roomId : String? = null

    class Comment() {
        var uid: String? = null
        var message: String? = null
        var send_time : Date? = null
        var readUsers = HashMap<String, Any>()
        var invite = HashMap<String, Boolean>()
    }

    class User() {
        var status: Int? = null
        var invite_time: Long? = null
    }
}