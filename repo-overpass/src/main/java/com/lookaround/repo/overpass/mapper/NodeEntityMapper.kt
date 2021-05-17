package com.lookaround.repo.overpass.mapper

import com.lookaround.core.model.NodeDTO
import com.lookaround.repo.overpass.entity.NodeEntity
import org.mapstruct.Mapper

@Mapper(componentModel = "jsr330")
interface NodeEntityMapper {
    fun toDTO(node: NodeEntity): NodeDTO
    fun toEntity(node: NodeDTO): NodeEntity
}
