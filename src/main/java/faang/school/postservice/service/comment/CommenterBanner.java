package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.publisher.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommenterBanner {

    private final CommentService commentService;
    private final RedisMessagePublisher redisMessagePublisher;

    @Scheduled(cron = "${cron.check-comments}")
    public void banCommenter() {
        List<CommentDto> comments = commentService.getAllCommentsNoVerified();
        Map<Long, Long> commentGroupingByAuthor = comments.stream()
                .collect(Collectors.groupingBy(
                        CommentDto::getAuthorId,
                        Collectors.counting()
                ));
        List<Long> authorsToBanned = commentGroupingByAuthor.entrySet().stream()
                .filter(entry -> entry.getValue() > 5)
                .map(Map.Entry::getKey)
                .toList();
        for(Long author : authorsToBanned) {
            log.info("Author {} is banned", author);
            redisMessagePublisher.publish(author.toString());
        }
    }
}
