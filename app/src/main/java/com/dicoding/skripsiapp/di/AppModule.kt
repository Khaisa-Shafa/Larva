package com.dicoding.skripsiapp.di

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import com.dicoding.skripsiapp.database.AppDatabase
import com.dicoding.skripsiapp.database.FavoriteNewsDao
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkRepository
import com.dicoding.skripsiapp.util.Constants.ONBOARDING_SP
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestoreDatabase() = Firebase.firestore

    @Provides
    fun provideIntroductionSP(
        application: Application
    ) = application.getSharedPreferences(ONBOARDING_SP, MODE_PRIVATE)


    @Provides
    @Singleton
    fun provideStorage() = FirebaseStorage.getInstance().reference

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase { // Perbaiki dengan @ApplicationContext
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavoriteNewsDao(database: AppDatabase): FavoriteNewsDao {
        return database.favoriteNewsDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(firestore: FirebaseFirestore): BookmarkRepository {
        return BookmarkRepository(firestore)
    }
}