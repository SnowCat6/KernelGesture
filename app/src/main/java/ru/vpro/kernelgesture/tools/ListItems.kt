package ru.vpro.kernelgesture.tools

import android.os.Parcel
import android.os.Parcelable


data class HeaderString(val title : String)
    : Parcelable
{
    constructor(parcel: Parcel) : this(parcel.readString()) {
    }

    override fun toString() =  title
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HeaderString> {
        override fun createFromParcel(parcel: Parcel): HeaderString {
            return HeaderString(parcel)
        }

        override fun newArray(size: Int): Array<HeaderString?> {
            return arrayOfNulls(size)
        }
    }
}

data class TwoString(val title : String = "", val content : String = "")
    : Parcelable
{
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()) {
    }

    override fun toString() =  "$title=>$content"
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(content)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TwoString> {
        override fun createFromParcel(parcel: Parcel): TwoString {
            return TwoString(parcel)
        }

        override fun newArray(size: Int): Array<TwoString?> {
            return arrayOfNulls(size)
        }
    }
}
