package me.magnum.melonds.di

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import me.magnum.melonds.common.uridelegates.CompositeUriHandler
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.database.MelonDatabase
import me.magnum.melonds.utils.UriTypeHierarchyAdapter
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
                .registerTypeHierarchyAdapter(Uri::class.java, UriTypeHierarchyAdapter())
                .create()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MelonDatabase {
        return Room.databaseBuilder(context, MelonDatabase::class.java, "melon-database").build()
    }

    @Provides
    @Singleton
    fun provideUriHandler(@ApplicationContext context: Context): UriHandler {
        return CompositeUriHandler(context)
    }
}