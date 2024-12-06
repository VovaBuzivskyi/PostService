package faang.school.postservice.service.album;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.album.AlbumCreateUpdateDto;
import faang.school.postservice.dto.album.AlbumDto;
import faang.school.postservice.dto.album.AlbumFilterDto;
import faang.school.postservice.enums.VisibilityAlbums;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.FeignClientException;
import faang.school.postservice.exception.ForbiddenException;
import faang.school.postservice.exception.MessageError;
import faang.school.postservice.exception.UnauthorizedException;
import faang.school.postservice.filter.album.AlbumFilter;
import faang.school.postservice.mapper.AlbumMapper;
import faang.school.postservice.model.Album;
import faang.school.postservice.model.AlbumCreatedEvent;
import faang.school.postservice.model.Post;
import faang.school.postservice.redis.publisher.AlbumCreatedPublisher;
import faang.school.postservice.repository.AlbumRepository;
import faang.school.postservice.service.post.PostService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

    private static final String USER = "User";
    private static final String ALBUM = "Album";

    private final UserContext userContext;
    private final UserServiceClient userServiceClient;
    private final AlbumRepository albumRepository;
    private final AlbumMapper albumMapper;
    private final PostService postService;
    private final List<AlbumFilter> filters;
    private final AlbumCreatedPublisher albumCreatedPublisher;

    @Transactional
    public AlbumDto createAlbum(AlbumCreateUpdateDto createDto) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to create a new album with title '{}'", requesterUserId, createDto.getTitle());

        validateUserExistence(requesterUserId);
        validateAlbumTitle(createDto.getTitle(), requesterUserId);
        Album albumToSave = albumMapper.toEntity(createDto);
        albumToSave.setAuthorId(requesterUserId);
        if (albumToSave.getVisibility() == null) {
            albumToSave.setVisibility(VisibilityAlbums.ALL_USERS);
        }
        Album savedAlbum = albumRepository.save(albumToSave);

        AlbumCreatedEvent albumCreatedEvent = new AlbumCreatedEvent(requesterUserId, savedAlbum.getId(),
                                                                    savedAlbum.getTitle());
        albumCreatedPublisher.publish(albumCreatedEvent);

        log.info("User with ID {} successfully created album with ID {} titled '{}'", requesterUserId, savedAlbum.getId(), createDto.getTitle());
        return albumMapper.toDtoList(savedAlbum);
    }

    @Transactional
    public AlbumDto addPostToAlbum(long albumId, long postId) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is adding post with ID {} to album with ID {}", requesterUserId, postId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "add post to album with ID %d".formatted(albumId));
        Post post = postService.getPost(postId);
        album.addPost(post);
        Album savedAlbum = albumRepository.save(album);

        log.info("User with ID {} successfully added post with ID {} to album with ID {}", requesterUserId, postId, albumId);
        return albumMapper.toDtoList(savedAlbum);
    }

    @Transactional
    public void deletePostFromAlbum(long albumId, long postId) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to remove post with ID {} from album with ID {}", requesterUserId, postId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "delete post from album with ID %d".formatted(albumId));

        if (album.getPosts().stream().anyMatch(post -> post.getId() == postId)) {
            album.removePost(postId);
            albumRepository.save(album);
            log.info("User with ID {} successfully removed post with ID {} from album with ID {}", requesterUserId, postId, albumId);
        } else {
            log.warn("User with ID {} attempted to remove non-existent post with ID {} from album with ID {}", requesterUserId, postId, albumId);
        }
    }

    @Transactional
    public void addAlbumToFavorites(long albumId) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to add album with ID: {} to favorites", requesterUserId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "add album to favorites with ID %d".formatted(albumId));
        if (checkAlbumVisibility(requesterUserId, album)) {
            albumRepository.addAlbumToFavorites(albumId, requesterUserId);
        } else {
            throw new ForbiddenException(requesterUserId, "add album to favorites with ID %d".formatted(album.getId()));
        }

        log.info("User with ID {} added album with ID {} to favorites", requesterUserId, albumId);
    }

    @Transactional
    public void deleteAlbumFromFavorites(long albumId) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to remove album with ID: {} to favorites", requesterUserId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "delete album to favorites with ID %d".formatted(albumId));
        if (checkAlbumVisibility(requesterUserId, album)) {
            albumRepository.deleteAlbumFromFavorites(albumId, requesterUserId);
        } else {
            throw new ForbiddenException(requesterUserId, "delete album from favorites with ID %d".formatted(album.getId()));
        }

        log.info("User with ID {} removed album with ID {} from favorites", requesterUserId, albumId);
    }

    @Transactional
    public AlbumDto getAlbumById(long albumId) {
        long userId = userContext.getUserId();
        log.info("User with ID {} is fetching album with ID {}", userContext.getUserId(), albumId);
        Album album = getAlbum(albumId);
        if (!checkAlbumVisibility(userId, album)) {
            throw new ForbiddenException(userId, "get album with ID %d".formatted(album.getId()));
        }
        log.info("Album with ID {} successfully fetched", albumId);
        return albumMapper.toDtoList(album);
    }

    @Transactional
    public List<AlbumDto> getAllAlbums(AlbumFilterDto filterDto) {
        long userId = userContext.getUserId();
        log.info("User with ID {} is fetching all albums with applied filters", userContext.getUserId());

        Stream<Album> albums = StreamSupport.stream(albumRepository.findAll().spliterator(), false);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto).stream()
                .filter(album -> checkAlbumVisibility(userId, album))
                .toList();

        log.info("Found {} albums after applying filters", filteredAlbums.size());
        return albumMapper.toDtoList(filteredAlbums);
    }

    @Transactional
    public List<AlbumDto> getUserAlbums(long albumAuthorUserId, AlbumFilterDto filterDto) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is fetching albums of user with ID {} with applied filters", requesterUserId, albumAuthorUserId);

        Stream<Album> albums = albumRepository.findByAuthorId(albumAuthorUserId);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto);

        log.info("Found {} albums after applying filters for user's albums", filteredAlbums.size());
        return albumMapper.toDtoList(filteredAlbums);
    }

    @Transactional
    public List<AlbumDto> getUserFavoriteAlbums(long albumAuthorUserId, AlbumFilterDto filterDto) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is fetching favorite albums of user with ID {} with filters", requesterUserId, albumAuthorUserId);

        Stream<Album> albums = albumRepository.findFavoriteAlbumsByUserId(albumAuthorUserId);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto);

        log.info("Found {} albums after applying filters for user's favorite albums", filteredAlbums.size());
        return albumMapper.toDtoList(filteredAlbums);
    }

    @Transactional
    public AlbumDto updateAlbum(long albumId, AlbumCreateUpdateDto updateDto) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to update album with ID {}", requesterUserId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "update album with ID %d".formatted(albumId));
        validateAlbumTitle(updateDto.getTitle(), requesterUserId);
        albumMapper.update(updateDto, album);
        album = albumRepository.save(album);

        log.info("User with ID {} successfully updated album with ID {}", requesterUserId, albumId);
        return albumMapper.toDtoList(album);
    }

    @Transactional
    public void deleteAlbum(long albumId) {
        long requesterUserId = userContext.getUserId();
        log.info("User with ID {} is attempting to delete album with ID {}", requesterUserId, albumId);

        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, requesterUserId, "delete album with ID %d".formatted(albumId));
        albumRepository.delete(album);

        log.info("User with ID {} successfully deleted album with ID {}", requesterUserId, albumId);
    }

    private void validateUserExistence(long userId) {
        try {
            userServiceClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new UnauthorizedException(userId, e);
        } catch (FeignException e) {
            String errorMessage = "There was an attempt to get %s by ID: %d".formatted(USER, userId);
            throw new FeignClientException(MessageError.FEIGN_CLIENT_UNEXPECTED_EXCEPTION.getMessage(errorMessage), e);
        }
    }

    private void validateAlbumTitle(String title, long userId) {
        if (albumRepository.existsByTitleAndAuthorId(title, userId)) {
            throw new DataValidationException("User with ID %d already has an album titled '%s'.".formatted(userId, title));
        }
    }

    private void validateAlbumAuthor(Album album, long userId, String forbiddenMessage) {
        if (album.getAuthorId() != userId) {
            throw new ForbiddenException(userId, forbiddenMessage);
        }
    }

    private List<Album> filterAlbums(Stream<Album> albums, AlbumFilterDto filterDto) {
        return filters.stream()
                .filter(filter -> filter.isApplicable(filterDto))
                .reduce(
                        albums,
                        (albumStream, filter) -> filter.apply(albumStream, filterDto),
                        (s1, s2) -> s2
                ).toList();
    }

    private Album getAlbum(long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new EntityNotFoundException(ALBUM, albumId));
    }

    private boolean checkAlbumVisibility(Long userId, Album album) {
        return switch (album.getVisibility()) {
            case ALL_USERS -> true;
            case SUBSCRIBERS -> checkUserFollower(userId, album.getAuthorId());
            case SELECTED_USERS -> checkUserBeholder(userId, album.getAuthorId(), album.getBeholdersIds());
            case ONLY_AUTHOR ->  checkUserToAuthor(userId, album.getAuthorId());
        };
    }

    private boolean checkUserToAuthor(Long userId, Long authorId) {
        return authorId == userId;
    }

    private boolean checkUserFollower(Long userId, Long authorId) {
        if (checkUserToAuthor(userId, authorId)) {
            return true;
        }
        return userServiceClient.checkFollowerOfFollowee(authorId, userId);
    }

    private boolean checkUserBeholder(Long userId, Long authorId, List<Long> beholdersIds) {
        if (checkUserToAuthor(userId, authorId)) {
            return true;
        }
        return beholdersIds.stream()
                .anyMatch(beholderId -> Objects.equals(beholderId, userId));
    }
}