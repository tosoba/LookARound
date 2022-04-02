package com.lookaround.repo.photon.dao

import androidx.room.*
import com.lookaround.repo.photon.entity.AutocompleteSearchEntity
import com.lookaround.repo.photon.entity.AutocompleteSearchResultEntity
import com.lookaround.repo.photon.entity.PointEntity
import java.util.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AutocompleteSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: AutocompleteSearchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(nodes: List<PointEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<AutocompleteSearchResultEntity>)

    @Query(
        """SELECT p.* FROM point p 
        INNER JOIN autocomplete_search_result acsr ON acsr.point_id = p.id 
        INNER JOIN autocomplete_search acs ON acs.id = acsr.autocomplete_search_id
        WHERE `query` = :query 
        AND (priority_lat = :priorityLat OR (priority_lat IS NULL AND :priorityLat IS NULL)) 
        AND (priority_lon = :priorityLon OR (priority_lon IS NULL AND :priorityLon IS NULL))"""
    )
    fun selectPoints(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?
    ): Flow<List<PointEntity>>

    @Query(
        """DELETE FROM autocomplete_search 
        WHERE `query` = :query 
        AND (priority_lat = :priorityLat OR (priority_lat IS NULL AND :priorityLat IS NULL)) 
        AND (priority_lon = :priorityLon OR (priority_lon IS NULL AND :priorityLon IS NULL))"""
    )
    suspend fun deleteSearch(query: String, priorityLat: Double?, priorityLon: Double?)

    @Query("DELETE FROM autocomplete_search") suspend fun deleteAllSearches()

    @Query(
        """DELETE FROM point 
            WHERE id IN
            (SELECT p.id FROM point p
            LEFT JOIN autocomplete_search_result acsr ON p.id = acsr.point_id 
            WHERE acsr.point_id IS NULL)"""
    )
    suspend fun deleteUnassociatedPoints()

    @Query("DELETE FROM point") suspend fun deleteAllPoints()

    @Query("DELETE FROM autocomplete_search WHERE id = :id") suspend fun deleteById(id: Long)

    @Transaction
    suspend fun delete(query: String, priorityLat: Double?, priorityLon: Double?) {
        deleteSearch(query, priorityLat, priorityLon)
        deleteUnassociatedPoints()
    }

    @Transaction
    suspend fun deleteAll() {
        deleteAllSearches()
        deleteAllPoints()
    }

    @Transaction
    suspend fun insert(search: AutocompleteSearchEntity, points: List<PointEntity>) {
        val searchId = insertSearch(search)
        val pointIds = insertPoints(points)
        insertResults(pointIds.map { nodeId -> AutocompleteSearchResultEntity(searchId, nodeId) })
    }

    @Query("SELECT * FROM autocomplete_search ORDER BY last_searched_at DESC LIMIT :limit")
    fun selectSearches(limit: Int): Flow<List<AutocompleteSearchEntity>>

    @Query(
        """SELECT * FROM autocomplete_search 
            WHERE LOWER(`query`) LIKE '%' || :query || '%' 
            ORDER BY last_searched_at DESC LIMIT :limit"""
    )
    fun selectSearches(limit: Int, query: String): Flow<List<AutocompleteSearchEntity>>

    @Query("SELECT COUNT(*) FROM autocomplete_search") fun selectSearchesCountFlow(): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM autocomplete_search
        WHERE LOWER(`query`) LIKE '%' || :query || '%'"""
    )
    suspend fun selectSearchesCount(query: String): Int

    @Query("SELECT COUNT(*) FROM autocomplete_search") suspend fun selectSearchesCount(): Int

    @Query(
        """SELECT p.* FROM point p 
        INNER JOIN autocomplete_search_result acsr ON acsr.point_id = p.id 
        WHERE acsr.autocomplete_search_id = :autocompleteSearchId"""
    )
    suspend fun selectSearchResults(autocompleteSearchId: Long): List<PointEntity>

    @Query("UPDATE autocomplete_search SET last_searched_at = :date WHERE id = :searchId")
    suspend fun updateAutocompleteSearchLastSearchedAt(searchId: Long, date: Date)

    @Query(
        """UPDATE autocomplete_search SET last_searched_at = :date 
        WHERE `query` = :query 
        AND (priority_lat = :priorityLat OR (priority_lat IS NULL AND :priorityLat IS NULL))  
        AND (priority_lon = :priorityLon OR (priority_lon IS NULL AND :priorityLon IS NULL))"""
    )
    suspend fun updateAutocompleteSearchLastSearchedAt(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?,
        date: Date
    )

    @Query("SELECT * FROM autocomplete_search WHERE `query` = :query")
    suspend fun selectAutocompleteSearches(query: String): List<AutocompleteSearchEntity>
}
