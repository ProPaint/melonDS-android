package me.magnum.melonds.parcelables

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import me.magnum.melonds.domain.model.RomConfig
import me.magnum.melonds.domain.model.RuntimeConsoleType
import me.magnum.melonds.domain.model.RuntimeMicSource
import java.util.*

class RomConfigParcelable : Parcelable {
    val romConfig: RomConfig

    constructor(romConfig: RomConfig) {
        this.romConfig = romConfig
    }

    private constructor(parcel: Parcel) {
        romConfig = RomConfig()
        romConfig.runtimeConsoleType = RuntimeConsoleType.values()[parcel.readInt()]
        romConfig.runtimeMicSource = RuntimeMicSource.values()[parcel.readInt()]
        romConfig.layoutId = parcel.readString()?.let { UUID.fromString(it) }
        romConfig.setLoadGbaCart(parcel.readInt() == 1)
        romConfig.gbaCartPath = parcel.readString()?.let { Uri.parse(it) }
        romConfig.gbaSavePath = parcel.readString()?.let { Uri.parse(it) }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(romConfig.runtimeConsoleType.ordinal)
        dest.writeInt(romConfig.runtimeMicSource.ordinal)
        dest.writeString(romConfig.layoutId?.toString())
        dest.writeInt(if (romConfig.loadGbaCart()) 1 else 0)
        dest.writeString(romConfig.gbaCartPath?.toString())
        dest.writeString(romConfig.gbaSavePath?.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RomConfigParcelable> {
        override fun createFromParcel(parcel: Parcel): RomConfigParcelable {
            return RomConfigParcelable(parcel)
        }

        override fun newArray(size: Int): Array<RomConfigParcelable?> {
            return arrayOfNulls(size)
        }
    }
}