package com.example.homefitness.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.homefitness.models.WgerExercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExerciseCache(context: Context) : SQLiteOpenHelper(context, "home_fitness_cache.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercise_cache (cache_key TEXT PRIMARY KEY, payload TEXT NOT NULL, saved_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS exercise_cache")
        onCreate(db)
    }
    fun save(key: String, exercises: List<WgerExercise>) {
        val values = android.content.ContentValues().apply {
            put("cache_key", key); put("payload", Gson().toJson(exercises)); put("saved_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("exercise_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun read(key: String): List<WgerExercise> {
        readableDatabase.rawQuery("SELECT payload FROM exercise_cache WHERE cache_key = ?", arrayOf(key)).use { cursor ->
            if (!cursor.moveToFirst()) return emptyList()
            val type = object : TypeToken<List<WgerExercise>>() {}.type
            return runCatching { Gson().fromJson<List<WgerExercise>>(cursor.getString(0), type) }.getOrDefault(emptyList())
        }
    }
}
