package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import faang.school.postservice.dto.post.PostDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class HashtagControllerIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    public static PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:13.6");

    @Container
    private static final RedisContainer REDIS_CONTAINER =
            new RedisContainer(DockerImageName.parse("redis/redis-stack:latest"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort())
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
                    .withStartupTimeout(Duration.ofMinutes(1));


    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);

        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    }

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

        Thread.sleep(1000);

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