package me.magnum.melonds.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.documentfile.provider.DocumentFile
import me.magnum.melonds.domain.model.Rom
import me.magnum.melonds.common.romprocessors.RomFileProcessorFactory
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

/**
 * Provider for ROM icons that supports caching. Both memory and disk caches are supported. If upon
 * request an icon is not found, it is generated and, if generated successfully, it's stored on both
 * caches.
 * The name of the file for the disk cache is the hash of the ROM's path.
 */
class RomIconProvider(private val context: Context, private val romFileProcessorFactory: RomFileProcessorFactory) {
    companion object {
        private const val ICON_CACHE_DIR = "rom_icons"
    }

    private val memoryIconCache = mutableMapOf<String, Bitmap>()

    fun getRomIcon(rom: Rom): Bitmap? {
        val romHash = rom.uri.hashCode().toString()
        return loadIconFromMemory(romHash, rom)
    }

    private fun loadIconFromMemory(hash: String, rom: Rom): Bitmap? {
        var bitmap = memoryIconCache[hash]
        if (bitmap != null)
            return bitmap

        bitmap = loadIconFromDisk(hash, rom)
        if (bitmap != null)
            memoryIconCache[hash] = bitmap

        return bitmap
    }

    private fun loadIconFromDisk(hash: String, rom: Rom): Bitmap? {
        val iconCacheDir = context.externalCacheDir?.let { File(it, ICON_CACHE_DIR) }
        if (iconCacheDir != null && iconCacheDir.isDirectory) {
            val iconFile = File(iconCacheDir, hash)
            if (iconFile.isFile) {
                return BitmapFactory.decodeFile(iconFile.absolutePath)
            }
        }

        val romDocument = DocumentFile.fromSingleUri(context, rom.uri) ?: return null
        val romProcessor = romFileProcessorFactory.getFileRomProcessorForDocument(romDocument) ?: return null
        val bitmap = romProcessor.getRomIcon(rom)
        if (bitmap != null && iconCacheDir != null) {
            saveRomIcon(hash, bitmap)
        }
        return bitmap
    }

    fun reloadIcon(hash: String,rom: Rom) {
        val iconCacheDir = context.externalCacheDir?.let { File(it, RomIconProvider.ICON_CACHE_DIR) }
        if (iconCacheDir != null && iconCacheDir.isDirectory) {
            val iconFile = File(iconCacheDir, hash)
            if (iconFile.isFile) {
                iconFile.delete()
            }
        }

        var bitmap = loadIconFromDisk(hash, rom)
        if (bitmap != null)
            memoryIconCache[hash] = bitmap
    }

    private fun saveRomIcon(romHash: String, icon: Bitmap) {
        val iconCacheDir = context.externalCacheDir?.let { File(it, ICON_CACHE_DIR) } ?: return
        if (iconCacheDir.isDirectory || iconCacheDir.mkdirs()) {
            val iconFile = File(iconCacheDir, romHash)
            try {
                FileOutputStream(iconFile).use {
                    icon.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (_: Exception) {
                // Ignore errors
            }
        }
    }
}