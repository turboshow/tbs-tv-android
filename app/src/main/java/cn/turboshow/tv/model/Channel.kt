package cn.turboshow.tv.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Channel(
    val title: String,
    val addr: String
) : Parcelable {
    override fun toString(): String {
        return title
    }
}