package me.magnum.melonds.impl

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.LayoutConfiguration
import me.magnum.melonds.domain.model.UILayout
import me.magnum.melonds.domain.repositories.LayoutsRepository
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.util.*

class InternalLayoutsRepository(private val context: Context, private val gson: Gson, private val defaultLayoutProvider: DefaultLayoutProvider) : LayoutsRepository {
    companion object {
        private const val DATA_FILE = "layouts.json"
        private val layoutListType: Type = object : TypeToken<List<LayoutConfiguration>>(){}.type
    }

    private var areLayoutsLoaded = false
    private val layouts = mutableListOf<LayoutConfiguration>()

    private val layoutsChangedSubject = PublishSubject.create<Unit>()
    private val mGlobalLayoutPlaceholder by lazy {
        LayoutConfiguration(
                null,
                context.getString(R.string.use_global_layout),
                LayoutConfiguration.LayoutType.DEFAULT,
                UILayout(emptyList()),
                UILayout(emptyList())
        )
    }

    override fun getLayouts(): Observable<List<LayoutConfiguration>> {
        return getCachedLayoutsOrLoad()
                .toObservable()
                .concatWith(layoutsChangedSubject.map { layouts })
    }

    override fun getLayout(id: UUID): Maybe<LayoutConfiguration> {
        return getCachedLayoutsOrLoad().flatMapMaybe { layouts ->
            val layout = layouts.firstOrNull { it.id == id }
            if (layout != null) {
                Maybe.just(layout)
            } else {
                Maybe.empty()
            }
        }
    }

    override fun getGlobalLayoutPlaceholder(): LayoutConfiguration {
        return mGlobalLayoutPlaceholder
    }

    override fun observeLayout(id: UUID): Observable<LayoutConfiguration> {
        return getCachedLayoutsOrLoad()
                .toObservable()
                .map { it.find { layout -> layout.id == id } != null }
                .concatWith(layoutsChangedSubject.map { layouts.find { it.id == id } != null })
                .map {
                    layouts.first { it.id == id }
                }
    }

    override fun saveLayout(layout: LayoutConfiguration) {
        if (layout.id == null) {
            val newLayout = layout.copy(
                    id = UUID.randomUUID()
            )
            layouts.add(newLayout)
        } else {
            val index = layouts.indexOfFirst { it.id == layout.id }
            if (index >= 0) {
                layouts[index] = layout
            } else {
                layouts.add(layout)
            }
        }
        layoutsChangedSubject.onNext(Unit)
        saveLayouts()
    }

    private fun getCachedLayoutsOrLoad(): Single<List<LayoutConfiguration>> {
        return if (areLayoutsLoaded) {
            Single.just(layouts)
        } else {
            loadLayouts().map {
                layouts.clear()
                layouts.add(buildDefaultLayout())
                layouts.addAll(it)
                areLayoutsLoaded = true
                layouts
            }
        }
    }

    private fun loadLayouts(): Single<List<LayoutConfiguration>> {
        return Single.create { emitter ->
            val dataFile = File(context.filesDir, DATA_FILE)
            if (!dataFile.isFile) {
                emitter.onSuccess(emptyList())
                return@create
            }

            try {
                val roms = gson.fromJson<List<LayoutConfiguration>>(FileReader(dataFile), layoutListType)
                emitter.onSuccess(roms ?: emptyList())
            } catch (_: Exception) {
                emitter.onSuccess(emptyList())
            }
        }
    }

    private fun saveLayouts() {
        val dataFile = File(context.filesDir, DATA_FILE)

        try {
            // Remove default layouts. They shouldn't be saved
            val customLayouts = layouts.filter { it.type != LayoutConfiguration.LayoutType.DEFAULT }
            val layoutsJson = gson.toJson(customLayouts)

            OutputStreamWriter(FileOutputStream(dataFile)).use {
                it.write(layoutsJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildDefaultLayout(): LayoutConfiguration {
        return defaultLayoutProvider.defaultLayout.copy(
                id = LayoutConfiguration.DEFAULT_ID,
                name = context.getString(R.string.default_layout_name),
                type = LayoutConfiguration.LayoutType.DEFAULT
        )
    }
}