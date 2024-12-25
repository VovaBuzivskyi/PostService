package faang.school.postservice.repository;

import faang.school.postservice.model.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long>{

    @Query(value = """
        SELECT
            h.hashtag,
            json_agg(
                json_build_object(
                    'id', p.id,
                    'content', p.content,
                    'authorId', p.author_id,
                    'projectId', p.project_id,
                    'published', p.published,
                    'publishedAt', p.published_at,
                    'scheduledAt', p.scheduled_at,
                    'deleted', p.deleted,
                    'createdAt', TO_CHAR(p.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS'),
                    'updatedAt', TO_CHAR(p.updated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS')
                )
            )
        FROM
            hashtag h
        JOIN
            post p ON h.post_id = p.id
        WHERE
            p.published = TRUE
            AND p.verified = TRUE
            AND p.deleted = FALSE
        GROUP BY
            h.hashtag
    """, nativeQuery = true)
    List<Object[]> findAllHashtagsWithPostIds();

    Set<Hashtag> findByPostId(long postId);

    void deleteAllByPostId(long postId);
}