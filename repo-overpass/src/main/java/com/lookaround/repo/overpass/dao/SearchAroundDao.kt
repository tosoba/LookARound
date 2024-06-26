package com.lookaround.repo.overpass.dao

import androidx.room.*
import com.lookaround.repo.overpass.entity.NodeEntity
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.entity.SearchAroundResultEntity
import java.util.*
import kotlinx.coroutines.flow.Flow
import nice.fontaine.overpass.models.query.settings.Filter

@Dao
interface SearchAroundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchAroundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<SearchAroundResultEntity>)

    @Query(
        """SELECT n.* FROM node n 
        INNER JOIN search_around_result sar ON sar.node_id = n.id 
        INNER JOIN search_around sa ON sa.id = sar.search_around_id 
        WHERE sa.lat = :lat AND sa.lng = :lng AND sa.radius_in_meters = :radiusInMeters 
        AND sa.`key` = :key AND sa.value = :value AND sa.filter = :filter"""
    )
    fun selectNodes(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter
    ): Flow<List<NodeEntity>>

    @Query(
        """DELETE FROM search_around 
        WHERE lat = :lat AND lng = :lng AND radius_in_meters = :radiusInMeters 
        AND `key` = :key AND value = :value AND filter = :filter"""
    )
    suspend fun deleteSearch(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter
    )

    @Query("DELETE FROM search_around") suspend fun deleteAllSearches()

    @Query(
        """DELETE FROM node 
            WHERE id IN
            (SELECT n.id FROM node n 
            LEFT JOIN search_around_result sar ON n.id=sar.node_id 
            WHERE sar.node_id IS NULL)"""
    )
    suspend fun deleteUnassociatedNodes()

    @Query("DELETE FROM node") suspend fun deleteAllNodes()

    @Query("DELETE FROM search_around WHERE id = :id") suspend fun deleteById(id: Long)

    @Transaction
    suspend fun delete(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter
    ) {
        deleteSearch(lat, lng, radiusInMeters, key, value, filter)
        deleteUnassociatedNodes()
    }

    @Transaction
    suspend fun deleteAll() {
        deleteAllSearches()
        deleteAllNodes()
    }

    @Transaction
    suspend fun insert(search: SearchAroundEntity, nodes: List<NodeEntity>) {
        val searchId = insertSearch(search)
        val nodeIds = insertNodes(nodes)
        insertResults(nodeIds.map { nodeId -> SearchAroundResultEntity(searchId, nodeId) })
    }

    @Query("SELECT * FROM search_around ORDER BY last_searched_at DESC LIMIT :limit")
    fun selectSearches(limit: Int): Flow<List<SearchAroundEntity>>

    @Query(
        """SELECT * FROM search_around 
        WHERE LOWER(value) LIKE '%' || :query || '%' 
        ORDER BY last_searched_at DESC LIMIT :limit"""
    )
    fun selectSearches(limit: Int, query: String): Flow<List<SearchAroundEntity>>

    @Query("SELECT COUNT(*) FROM search_around") fun selectSearchesCountFlow(): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM search_around
        WHERE LOWER(value) LIKE '%' || :query || '%'"""
    )
    suspend fun selectSearchesCount(query: String): Int

    @Query("SELECT COUNT(*) FROM search_around") suspend fun selectSearchesCount(): Int

    @Query(
        """SELECT n.* FROM node n 
        INNER JOIN search_around_result sar ON sar.node_id = n.id 
        WHERE sar.search_around_id = :searchAroundId"""
    )
    suspend fun selectSearchResults(searchAroundId: Long): List<NodeEntity>

    @Query("UPDATE search_around SET last_searched_at = :date WHERE id = :searchId")
    suspend fun updateSearchAroundLastSearchedAt(searchId: Long, date: Date)

    @Query(
        """UPDATE search_around SET last_searched_at = :date 
        WHERE lat = :lat AND lng = :lng AND radius_in_meters = :radiusInMeters  
        AND `key` = :key AND value = :value AND filter = :filter"""
    )
    suspend fun updateSearchAroundLastSearchedAt(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter,
        date: Date
    )

    @Query(
        """SELECT * FROM search_around
            WHERE radius_in_meters = :radiusInMeters AND `key` = :key AND value = :value AND filter = :filter"""
    )
    suspend fun selectSearchesAround(
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter,
    ): List<SearchAroundEntity>
}
