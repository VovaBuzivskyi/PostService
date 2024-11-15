package faang.school.postservice.service.album;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.album.AlbumCreateUpdateDto;
import faang.school.postservice.dto.album.AlbumDto;
import faang.school.postservice.dto.album.AlbumFilterDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.FeignClientException;
import faang.school.postservice.exception.MessageError;
import faang.school.postservice.filter.album.AlbumFilter;
import faang.school.postservice.mapper.AlbumMapper;
import faang.school.postservice.model.Album;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.AlbumRepository;
import faang.school.postservice.service.post.PostService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Transactional
    public AlbumDto createAlbum(AlbumCreateUpdateDto createDto) {
        log.info("Start creating album with title: {}", createDto.getTitle());

        long userId = userContext.getUserId();
        validateUserId(userId);
        validateAlbumTitle(createDto.getTitle(), userId);
        Album albumToSave = albumMapper.toEntity(createDto);
        albumToSave.setAuthorId(userId);
        Album savedAlbum = albumRepository.save(albumToSave);

        log.info("Album created successfully with ID: {}", savedAlbum.getId());
        return albumMapper.toDto(savedAlbum);
    }

    @Transactional
    public AlbumDto addPostToAlbum(long albumId, long postId) {
        log.info("Start adding post with ID: {} to album with ID: {}", postId, albumId);

        long userId = userContext.getUserId();
        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, userId);
        Post post = postService.getPost(postId);
        album.addPost(post);

        log.info("Post added successfully to album with ID: {}", albumId);
        return albumMapper.toDto(albumRepository.save(album));
    }

    @Transactional
    public void deletePostFromAlbum(long albumId, long postId) {
        log.info("Start removing post with ID: {} from album with ID: {}", postId, albumId);

        long userId = userContext.getUserId();
        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, userId);

        if (album.getPosts().stream().anyMatch(post -> post.getId() == postId)) {
            album.removePost(postId);
            albumRepository.save(album);
            log.info("Post with ID: {} removed from album with ID: {}", postId, albumId);
        } else {
            log.warn("Post with ID: {} not found in album with ID: {}", postId, albumId);
        }
    }

    @Transactional
    public void addAlbumToFavorites(long albumId) {
        log.info("Start adding album with ID: {} to favorites", albumId);
        Album album = getAlbum(albumId);
        long userId = userContext.getUserId();
        validateAlbumAuthor(album, userId);
        albumRepository.addAlbumToFavorites(albumId, userId);
        albumRepository.save(album);
        log.info("Album with ID: {} added to favorites", albumId);
    }

    @Transactional
    public void deleteAlbumFromFavorites(long albumId) {
        log.info("Start removing album with ID: {} from favorites", albumId);
        Album album = getAlbum(albumId);
        long userId = userContext.getUserId();
        validateAlbumAuthor(album, userId);
        albumRepository.deleteAlbumFromFavorites(albumId, userId);
        albumRepository.save(album);
        log.info("Album with ID: {} removed from favorites", albumId);
    }

    @Transactional
    public AlbumDto getAlbumById(long albumId) {
        log.info("Start fetching album with ID: {}", albumId);
        Album album = getAlbum(albumId);
        log.info("Album with ID: {} found", albumId);
        return albumMapper.toDto(album);
    }

    @Transactional
    public List<AlbumDto> getAllAlbums(AlbumFilterDto filterDto) {
        log.info("Start fetching all albums with filters");

        Stream<Album> albums = StreamSupport.stream(albumRepository.findAll().spliterator(), false);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto);

        log.info("Found {} albums after applying filters for all albums", filteredAlbums.size());
        return albumMapper.toDto(filteredAlbums);
    }

    @Transactional
    public List<AlbumDto> getUserAlbums(AlbumFilterDto filterDto) {
        log.info("Start fetching user's albums with filters");

        long userId = userContext.getUserId();
        Stream<Album> albums = albumRepository.findByAuthorId(userId);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto);

        log.info("Found {} albums after applying filters for user's albums", filteredAlbums.size());
        return albumMapper.toDto(filteredAlbums);
    }

    @Transactional
    public List<AlbumDto> getUserFavoriteAlbums(AlbumFilterDto filterDto) {
        log.info("Start fetching user's favorite albums with filters");

        long userId = userContext.getUserId();
        Stream<Album> albums = albumRepository.findFavoriteAlbumsByUserId(userId);
        List<Album> filteredAlbums = filterAlbums(albums, filterDto);

        log.info("Found {} albums after applying filters for user's favorite albums", filteredAlbums.size());
        return albumMapper.toDto(filteredAlbums);
    }

    @Transactional
    public AlbumDto updateAlbum(long albumId, AlbumCreateUpdateDto updateDto) {
        log.info("Start updating album with ID {}", albumId);

        long userId = userContext.getUserId();
        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, userId);
        validateAlbumTitle(updateDto.getTitle(), userId);
        albumMapper.update(updateDto, album);
        album = albumRepository.save(album);

        log.info("Album updated successfully with ID: {}", albumId);
        return albumMapper.toDto(album);
    }

    @Transactional
    public void deleteAlbum(long albumId) {
        log.info("Start deleting album with ID {}", albumId);

        long userId = userContext.getUserId();
        Album album = getAlbum(albumId);
        validateAlbumAuthor(album, userId);
        albumRepository.delete(album);

        log.info("Album deleted successfully with ID: {}", albumId);
    }

    private void validateUserId(long userId) {
        try {
            userServiceClient.getUser(userId);
        } catch(FeignException.NotFound e) {
            throw new EntityNotFoundException(USER, userId);
        } catch (Exception e) {
            throw new FeignClientException(
                    MessageError.FEIGN_CLIENT_UNEXPECTED_EXCEPTION
                            .getMessage("There was an attempt to get %s by ID: %d".formatted(USER, userId)),
                    e
            );
        }
    }

    private void validateAlbumTitle(String title, long userId) {
        if (albumRepository.existsByTitleAndAuthorId(title, userId)) {
            throw new DataValidationException("User with ID %d already has an album titled '%s'.".formatted(userId, title));
        }
    }

    private void validateAlbumAuthor(Album album, long userId) {
        if (album.getAuthorId() != userId) {
            throw new DataValidationException("User with ID %d is not the author of the album with ID %d.".formatted(userId, album.getId()));
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
}