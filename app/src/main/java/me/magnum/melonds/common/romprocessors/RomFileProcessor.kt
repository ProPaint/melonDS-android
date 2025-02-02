package me.magnum.melonds.common.romprocessors

import android.graphics.Bitmap
import android.net.Uri
import io.reactivex.Single
import me.magnum.melonds.domain.model.Rom
import me.magnum.melonds.domain.model.RomInfo

interface RomFileProcessor {
    fun getRomFromUri(uri: Uri): Rom?
    fun getRomIcon(rom: Rom): Bitmap?
    fun getRomInfo(rom: Rom): RomInfo?
    fun getRealRomUri(rom: Rom): Single<Uri>
}