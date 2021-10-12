package com.example.appmusic

import java.io.Serializable

class MySong (
    var id: String,
    var title: String?,
    var artist: String?,
    var displayName: String?,
    var data: String?,
    var duration: Long
):Serializable{
    override fun toString(): String {
        return "[id:$id,  title:$title,  artist:$artist,  displayName:$displayName,  duration:$duration, data:$data]"
    }

}