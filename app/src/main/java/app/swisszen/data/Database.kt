package app.swisszen.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "journal")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val text: String,
)

@Entity(tableName = "bell_events")
data class BellEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rangAt: Long,
    val answered: Boolean = false,
    val note: String? = null,
)

/** One finished (or ended early) session of any tool; feeds Home stats, streak and "last used" hints. */
@Entity(tableName = "sessions")
data class SessionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val startedAt: Long,
    val durationSec: Int,
    val detail: String? = null,
    val longestHoldSec: Int? = null,
) {
    companion object {
        const val GUIDED = "guided"
        const val WIM_HOF = "wimhof"
        const val PRACTICE = "practice"
        const val MEDITATION = "meditation"
    }
}

@Dao
interface JournalDao {
    @Insert suspend fun insert(entry: JournalEntry): Long
    @Query("SELECT * FROM journal ORDER BY createdAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<JournalEntry>>
    @Query("SELECT * FROM journal WHERE createdAt BETWEEN :from AND :to ORDER BY RANDOM() LIMIT 1")
    suspend fun randomBetween(from: Long, to: Long): JournalEntry?
    @Query("SELECT createdAt FROM journal ORDER BY createdAt DESC")
    fun allTimes(): Flow<List<Long>>
}

@Dao
interface BellDao {
    @Insert suspend fun insert(event: BellEvent): Long
    @Query("SELECT * FROM bell_events ORDER BY rangAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<BellEvent>>
    @Query("UPDATE bell_events SET answered = 1 WHERE id = :id")
    suspend fun markAnswered(id: Long)
    @Query("UPDATE bell_events SET note = :note, answered = 1 WHERE id = :id")
    suspend fun setNote(id: Long, note: String)
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(log: SessionLog): Long
    @Query("SELECT * FROM sessions WHERE startedAt >= :since ORDER BY startedAt DESC")
    fun since(since: Long): Flow<List<SessionLog>>
    @Query("SELECT * FROM sessions WHERE kind = :kind ORDER BY startedAt DESC LIMIT 1")
    fun lastOf(kind: String): Flow<SessionLog?>
    @Query("SELECT * FROM sessions WHERE kind IN (:kinds) ORDER BY startedAt DESC LIMIT 1")
    fun lastOfAny(kinds: List<String>): Flow<SessionLog?>
    @Query("SELECT startedAt FROM sessions ORDER BY startedAt DESC")
    fun allTimes(): Flow<List<Long>>
}

@Database(entities = [JournalEntry::class, BellEvent::class, SessionLog::class], version = 1, exportSchema = true)
abstract class ZenDatabase : RoomDatabase() {
    abstract fun journal(): JournalDao
    abstract fun bell(): BellDao
    abstract fun sessions(): SessionDao

    companion object {
        fun create(context: Context): ZenDatabase =
            Room.databaseBuilder(context, ZenDatabase::class.java, "swisszen.db").build()
    }
}
