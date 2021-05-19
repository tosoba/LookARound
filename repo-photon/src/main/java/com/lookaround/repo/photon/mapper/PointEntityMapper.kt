package com.lookaround.repo.photon.mapper

import com.lookaround.core.model.PointDTO
import com.lookaround.repo.photon.entity.PointEntity
import org.mapstruct.Mapper

@Mapper(componentModel = "jsr330")
interface PointEntityMapper {
    fun toDTO(point: PointEntity): PointDTO
    fun toEntity(point: PointDTO): PointEntity
}
