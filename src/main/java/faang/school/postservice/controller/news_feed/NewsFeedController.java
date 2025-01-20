package faang.school.postservice.controller.news_feed;

import faang.school.postservice.config.context.UserContext;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class NewsFeedController {

    private final NewsFeedService newsFeedService;
    private final UserContext userContext;

    @GetMapping
    public NewsFeedDto getNewsFeedBatch(@RequestParam(value = "pageNumber", defaultValue = "0")
                                        @Min(value = 0, message = "Value should be positive") Long pageNumber) {
        return newsFeedService.getNewsFeedBatch(pageNumber, userContext.getUserId());
    }
}
