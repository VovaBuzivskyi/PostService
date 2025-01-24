package faang.school.postservice.client;

import faang.school.postservice.dto.user.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service", url = "${user-service.host}:${user-service.port}${user-service.path}")
public interface UserServiceClient {

    @GetMapping("/users/{userId}")
    UserDto getUser(@PathVariable Long userId);

    @PostMapping("/users/by-ids")
    List<UserDto> getUsersByIds(@RequestBody List<Long> ids);

    @GetMapping("/subscriptions/{followeeId}/followers/{followerId}")
    Boolean checkFollowerOfFollowee(@PathVariable Long followeeId, @PathVariable Long followerId);

    @GetMapping("/subscriptions/{followeeId}")
    List<Long> getFollowersIds(@PathVariable Long followeeId);

    @GetMapping("/subscriptions/ids/{followerId}")
    List<Long> getFolloweesIds(@PathVariable Long followerId);
}
