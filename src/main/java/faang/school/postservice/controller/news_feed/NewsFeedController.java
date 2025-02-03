package faang.school.postservice.controller.news_feed;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.news_feed.NewsFeedResponseDto;
import faang.school.postservice.service.feed.FeedHeater;
import faang.school.postservice.service.feed.NewsFeedService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class NewsFeedController {

    private final NewsFeedService newsFeedService;
    private final FeedHeater feedHeater;
    private final UserContext userContext;

    @GetMapping
    public NewsFeedResponseDto getNewsFeedBatch(@RequestParam(value = "lastViewedPostId", required = false)
                                                @Min(value = 0, message = "Value should be positive")
                                                Long lastViewedPostId) {
        return newsFeedService.getNewsFeedBatch(lastViewedPostId, userContext.getUserId());
    }

    @PostMapping("/cache/heat")
    public String heatCache() {
        feedHeater.startHeatFeedCache();
        return "Cache heating started successfully.";
    }
}
