package com.lookaround.repo.overpass.mapper

import com.lookaround.core.model.NodeDTO
import com.lookaround.repo.overpass.entity.NodeEntity
import nice.fontaine.overpass.models.response.geometries.Node
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.Qualifier

@Mapper(componentModel = "jsr330")
interface NodeMapper {
    @Mappings(
        value = [Mapping(source = "tags", target = "name", qualifiedBy = [TagsToName::class])]
    )
    fun toDTO(node: Node): NodeDTO

    @Mappings(
        value = [Mapping(source = "tags", target = "name", qualifiedBy = [TagsToName::class])]
    )
    fun toEntity(node: Node): NodeEntity

    @Qualifier @Target(AnnotationTarget.FUNCTION) private annotation class TagsToName

    companion object {
        @TagsToName
        @JvmStatic
        fun tagsToName(tags: Map<String, String>): String =
            requireNotNull(tags["name"]) { "Node must have a name tag" }
    }
}
