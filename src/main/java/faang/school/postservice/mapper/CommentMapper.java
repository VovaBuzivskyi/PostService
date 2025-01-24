package faang.school.postservice.mapper;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.event.comment.CacheCommentEvent;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
    CommentDto toDto(Comment comment);

    Comment toEntity(CommentDto commentDto);

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "likes", target = "likesCount", qualifiedByName = "mapLikesCount")
    CacheCommentEvent toCacheCommentEvent(Comment comment);

    @Named(value = "mapLikesCount")
    default long mapLikesCount(List<Like> likes) {
        return likes == null ? 0 : likes.size();
    }
}
