package faang.school.postservice.mapper.post_file;

import faang.school.postservice.dto.post_file.PostFileDto;
import faang.school.postservice.model.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostFileMapper {

    @Mapping(source = "post.id", target = "postId")
    PostFileDto toDto(Resource resource);

    List<PostFileDto> toDtoList(List<Resource> resources);
}
