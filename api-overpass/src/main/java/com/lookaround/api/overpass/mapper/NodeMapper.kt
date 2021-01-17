package com.lookaround.api.overpass.mapper

import com.lookaround.core.overpass.model.NodeDTO
import nice.fontaine.overpass.models.response.geometries.Node
import org.mapstruct.Mapper

@Mapper
internal interface NodeMapper {
    fun toDTO(node: Node): NodeDTO
}
