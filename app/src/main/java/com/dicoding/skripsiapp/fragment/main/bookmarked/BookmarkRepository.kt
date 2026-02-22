package com.dicoding.skripsiapp.fragment.main.bookmarked

import android.util.Log
import com.dicoding.skripsiapp.data.BookmarkItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BookmarkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val bookmarkCollection = firestore.collection("bookmarks")

    suspend fun addBookmark(bookmarkItem: BookmarkItem, userId: String) {
        bookmarkCollection.document(userId).collection("user_bookmarks")
            .document(bookmarkItem.id)
            .set(bookmarkItem)
            .await()
    }

    suspend fun getBookmarkById(bookmarkId: String): BookmarkItem? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return null  // Pastikan userId valid
        val document = firestore.collection("bookmarks")
            .document(userId)
            .collection("user_bookmarks")
            .document(bookmarkId)
            .get()
            .await()

        if (!document.exists()) {
            Log.e("Repository", "Document with ID $bookmarkId does not exist")
            return null
        }

        Log.d("Repository", "Fetched document data: ${document.data}")
        return document.toObject(BookmarkItem::class.java)
    }

    suspend fun removeBookmarkByUri(imageUri: String, userId: String) {
        val snapshot = firestore.collection("bookmarks").document(userId)
            .collection("user_bookmarks")
            .whereEqualTo("imageUri", imageUri) // Pastikan hanya menghapus yang sesuai
            .limit(1) // Tambahkan limit untuk memastikan hanya satu yang dihapus
            .get()
            .await()

        for (document in snapshot.documents) {
            document.reference.delete().await() // Tunggu hingga benar-benar dihapus
        }
    }

    suspend fun getBookmarks(userId: String): List<BookmarkItem> {
        val snapshot = bookmarkCollection.document(userId).collection("user_bookmarks")
            .get()
            .await()

        return snapshot.toObjects(BookmarkItem::class.java)
    }
}