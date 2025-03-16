package faang.school.postservice.controller.ike;

import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.service.like.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/posts")
    public void addLikeToPost(@RequestBody @Valid LikeDto likeDto) {
        likeService.addLikeToPost(likeDto);
    }

    @PostMapping("/comments")
    public void addLikeToComment(@RequestBody @Valid LikeDto likeDto) {
        likeService.addLikeToComment(likeDto);
    }

    @DeleteMapping("/posts/{likeId}")
    public void removeLikeFromPost(@PathVariable Long likeId, @RequestBody @Valid LikeDto likeDto) {
        likeService.removeLikeFromPost(likeId, likeDto);
    }

    @DeleteMapping("/comments/{likeId}")
    public void removeLikeFromComment(@PathVariable Long likeId, @RequestBody @Valid LikeDto likeDto) {
        likeService.removeLikeFromComment(likeId, likeDto);
    }

    @GetMapping("/posts/{postId}")
    public List<UserDto> getLikedPostUsers(@PathVariable Long postId) {
        return likeService.getLikedPostUsers(postId);
    }

    @GetMapping("/comments/{commentId}")
    public List<UserDto> getLikedCommentUsers(@PathVariable Long commentId) {
        return likeService.getLikedCommentUsers(commentId);
    }
}

