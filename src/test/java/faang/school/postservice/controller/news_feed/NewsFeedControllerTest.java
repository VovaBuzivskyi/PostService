package faang.school.postservice.controller.news_feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.news_feed.NewsFeedResponseDto;
import faang.school.postservice.service.feed.FeedHeater;
import faang.school.postservice.service.feed.NewsFeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsFeedController.class)
class NewsFeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsFeedService newsFeedService;

    @MockBean
    private FeedHeater feedHeater;

    @MockBean
    private UserContext userContext;

    @Test
    void getNewsFeedBatchWithoutLastViewedPostIdTest() throws Exception {
        long userId = 1L;

        NewsFeedResponseDto responseDto = NewsFeedResponseDto.builder()
                .posts(new LinkedHashSet<>())
                .postsAuthors(new ArrayList<>())
                .build();

        when(userContext.getUserId()).thenReturn(userId);
        when(newsFeedService.getNewsFeedBatch(null, userId)).thenReturn(responseDto);

        mockMvc.perform(get("/feeds")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(responseDto)));

        verify(userContext).getUserId();
        verify(newsFeedService).getNewsFeedBatch(null, userId);
    }

    @Test
    void getNewsFeedBatchWithLastViewedPostIdTest() throws Exception {
        long userId = 1L;
        long lastViewedPostId = 2L;

        NewsFeedResponseDto responseDto = NewsFeedResponseDto.builder()
                .posts(new LinkedHashSet<>())
                .postsAuthors(new ArrayList<>())
                .build();

        when(userContext.getUserId()).thenReturn(userId);
        when(newsFeedService.getNewsFeedBatch(lastViewedPostId, userId)).thenReturn(responseDto);

        mockMvc.perform(get("/feeds")
                        .param("lastViewedPostId", String.valueOf(lastViewedPostId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(responseDto)));

        verify(userContext).getUserId();
        verify(newsFeedService).getNewsFeedBatch(lastViewedPostId, userId);
    }

    @Test
    void heatCacheTest() throws Exception {
        mockMvc.perform(post("/feeds/cache/heat")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache heating started successfully."));

        verify(feedHeater).startHeatFeedCache();
    }
}