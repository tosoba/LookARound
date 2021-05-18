package com.lookaround.repo.overpass.dao

import androidx.room.*
import com.lookaround.repo.overpass.entity.NodeEntity
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.entity.SearchAroundResultEntity
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
}
