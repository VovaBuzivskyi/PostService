package faang.school.postservice.mapper.post;

import faang.school.postservice.dto.post.PostCacheDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostRequestDto;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {
    @Mapping(source = "likes", target = "likesCount", qualifiedByName = "mapLikesCount")
    PostDto toDto(Post post);

    Post toEntity(PostDto postDto);

    Post toEntity(PostRequestDto postRequestDto);

    List<PostDto> toDto(List<Post> posts);

    void updatePostFromDto(PostDto postDto, @MappingTarget Post post);

    //postViewCount ignore!!
    //mapping Comment to commentDto ignore??
    @Mapping(source = "comments", target = "commentsCount", qualifiedByName = "mapCommentsCount")
    @Mapping(source = "likes", target = "likesCount", qualifiedByName = "mapLikesCount")
    PostCacheDto toPostCacheDto(Post post);

    default long mapLikesCount(List<Like> likes) {
        return likes == null ? 0 : likes.size();
    }

    default long mapCommentsCount(List<Comment> comments) {
        return comments == null ? 0 : comments.size();
    }
}