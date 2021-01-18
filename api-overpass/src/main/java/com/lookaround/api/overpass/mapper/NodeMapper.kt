package com.lookaround.api.overpass.mapper

import com.lookaround.core.overpass.model.NodeDTO
import nice.fontaine.overpass.models.response.geometries.Node
import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper

@Mapper(componentModel = "jsr330")
interface NodeMapper {
    fun toDTO(node: Node): NodeDTO
}
