package com.bang.bangtalk.model

class UserModel() : Comparable<UserModel>{
    var name: String? = null
    var email: String? = null
    var profileImageUrl: String? = null
    var uid: String? = null
    var pushToken : String? = null
    var comment : String? = null

    override fun compareTo(other: UserModel): Int {
        return this.name!!.compareTo(other.name!!)
    }
}