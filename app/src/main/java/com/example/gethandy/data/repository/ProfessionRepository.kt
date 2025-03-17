package com.example.gethandy.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gethandy.TAG
import com.example.gethandy.data.local.dao.ProfessionDao
import com.example.gethandy.data.model.Profession
import com.example.gethandy.utils.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfessionRepository(
    private val professionDao: ProfessionDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getAllProfessions(): LiveData<List<Profession>> {
        return professionDao.getAllProfessions()
    }

    suspend fun refreshProfessions(): NetworkResult<List<Profession>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("professions")
                    .orderBy("name")
                    .get()
                    .await()

                val professions = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Profession(name)
                }

                professionDao.insertAllProfessions(professions)

                NetworkResult.Success(professions)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing professions")
                NetworkResult.Error(e.message ?: "Error fetching professions")
            }
        }
    }

}