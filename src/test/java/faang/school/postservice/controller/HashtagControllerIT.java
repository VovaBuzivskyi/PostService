package faang.school.postservice.controller;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class HashtagControllerIT extends BaseContextTest {

    @Test
    public void testGetPostsByHashtag() throws Exception {

        PostDto firstPostDto = new PostDto();
        firstPostDto.setId(6L);
        firstPostDto.setContent("noise #forest");
        firstPostDto.setAuthorId(3L);
        PostDto secondPostDto = new PostDto();
        secondPostDto.setId(7L);
        secondPostDto.setContent("green #forest");
        secondPostDto.setAuthorId(3L);
        List<PostDto> expectedPostsDto = List.of(firstPostDto, secondPostDto);

        MvcResult result = mockMvc.perform(
                        get("/hashtag/forest")
                                .header("x-user-id", 1)
                ).andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<PostDto> actualPostsDto = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, PostDto.class)
        );
        assertEquals(expectedPostsDto.get(0).getId(), actualPostsDto.get(0).getId());
        assertEquals(expectedPostsDto.get(0).getContent(), actualPostsDto.get(0).getContent());
        assertEquals(expectedPostsDto.get(0).getAuthorId(), actualPostsDto.get(0).getAuthorId());
        assertEquals(expectedPostsDto.get(1).getId(), actualPostsDto.get(1).getId());
        assertEquals(expectedPostsDto.get(1).getContent(), actualPostsDto.get(1).getContent());
        assertEquals(expectedPostsDto.get(1).getAuthorId(), actualPostsDto.get(1).getAuthorId());
    }
}