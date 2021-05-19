package com.lookaround.repo.overpass.mapper

import com.lookaround.core.model.NodeDTO
import com.lookaround.repo.overpass.entity.NodeEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "jsr330")
interface NodeEntityMapper {
    fun toDTO(node: NodeEntity): NodeDTO
    @Mapping(target = "id", ignore = true) fun toEntity(node: NodeDTO): NodeEntity
}
