package com.lookaround.repo.overpass.mapper

import com.lookaround.core.model.NodeDTO
import nice.fontaine.overpass.models.response.geometries.Node
import org.mapstruct.Mapper

@Mapper(componentModel = "jsr330")
interface NodeMapper {
    fun toDTO(node: Node): NodeDTO
}
