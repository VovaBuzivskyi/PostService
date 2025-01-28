package faang.school.postservice.repository;

import faang.school.postservice.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorId(long authorId);

    List<Post> findByProjectId(long projectId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.projectId = :projectId")
    List<Post> findByProjectIdWithLikes(long projectId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.authorId = :authorId")
    List<Post> findByAuthorIdWithLikes(long authorId);

    @Query("SELECT p FROM Post p WHERE p.published = false AND p.deleted = false AND p.scheduledAt <= CURRENT_TIMESTAMP")
    List<Post> findReadyToPublish();

    @Query("SELECT p.authorId FROM Post p WHERE p.verified = false GROUP BY p.authorId HAVING COUNT(p) > :banCount")
    List<Long> findAuthorsIdsToBan(@Param("banCount") int banCount);

    @Query(value = """
            SELECT * FROM post
            WHERE author_id IN :authorsIds
            AND deleted = false
            AND published = true
            ORDER BY published_at DESC
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Post> findBatchNewestPostsForUserByFolloweesIds(@Param("authorsIds") List<Long> authorsIds,
                                                         @Param("batchSize") int batchSize);

    @Query(value = """
            SELECT * FROM post
            WHERE author_id IN :authorsIds
              AND deleted = false
              AND published = true
              AND published_at > (
                  SELECT published_at
                  FROM post
                  WHERE id = :particularPostId
              )
            ORDER BY published_at DESC
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Post> findBatchOrderedPostsAfterParticularPostIdInOrderByFolloweesIds(
            @Param("authorsIds") List<Long> authorsIds,
            @Param("particularPostId") long particularPostId,
            @Param("batchSize") int batchSize
    );

    @Query(nativeQuery = true, value = """
            SELECT id FROM post
            WHERE published = true
              AND deleted = false
              AND published_at >= NOW() - INTERVAL :publishedDaysAgo DAY
            ORDER BY published_at DESC
            LIMIT :limit OFFSET :offset
            """)
        List<Long> findAllPublishedNotDeletedPostsIdsPublishedNotLaterDaysAgo(
            @Param("publishedDaysAgo") long publishedDaysAgo,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
