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
import faang.school.postservice.exception.UnauthorizedException;
import faang.school.postservice.filter.album.AlbumDateFilter;
import faang.school.postservice.filter.album.AlbumDescriptionFilter;
import faang.school.postservice.filter.album.AlbumFilter;
import faang.school.postservice.filter.album.AlbumTitleFilter;
import faang.school.postservice.mapper.AlbumMapperImpl;
import faang.school.postservice.model.Album;
import faang.school.postservice.model.Post;
import faang.school.postservice.redis.publisher.AlbumCreatedPublisher;
import faang.school.postservice.repository.AlbumRepository;
import faang.school.postservice.service.post.PostService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private UserContext userContext;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private AlbumCreatedPublisher albumCreatedPublisher;

    @Spy
    private AlbumMapperImpl albumMapper;

    @Mock
    private PostService postService;

    private List<AlbumFilter> albumFilters;

    private AlbumService albumService;

    @BeforeEach
    void setUp() {
        albumFilters = List.of(
                new AlbumTitleFilter(),
                new AlbumDescriptionFilter(),
                new AlbumDateFilter()
        );
        albumService = new AlbumService(userContext, userServiceClient, albumRepository,
                albumMapper, postService, albumFilters, albumCreatedPublisher);
    }

    @Test
    void createAlbumNotExistingUserTest() {
        long userId = 1L;
        AlbumCreateUpdateDto createDto = new AlbumCreateUpdateDto();
        createDto.setTitle("Title");
        createDto.setDescription("Description");
        createDto.setVisibility(VisibilityAlbums.ALL_USERS);
        when(userContext.getUserId()).thenReturn(userId);
        when(userServiceClient.getUser(userId)).thenThrow(FeignException.NotFound.class);

        assertThrows(UnauthorizedException.class, () -> albumService.createAlbum(createDto));

        verify(userContext, times(1)).getUserId();
        verify(userServiceClient, times(1)).getUser(userId);
    }

    @Test
    void createAlbumFeignClientExceptionTest() {
        long userId = 1L;
        AlbumCreateUpdateDto createDto = new AlbumCreateUpdateDto();
        createDto.setTitle("Title");
        createDto.setDescription("Description");
        createDto.setVisibility(VisibilityAlbums.ALL_USERS);
        when(userContext.getUserId()).thenReturn(userId);
        when(userServiceClient.getUser(userId)).thenThrow(FeignException.class);

        assertThrows(FeignClientException.class, () -> albumService.createAlbum(createDto));

        verify(userContext, times(1)).getUserId();
        verify(userServiceClient, times(1)).getUser(userId);
    }

    @Test
    void createAlbumNotUniqueTitleTest() {
        long userId = 1L;
        String title = "Title";
        String description = "Description";
        AlbumCreateUpdateDto createDto = new AlbumCreateUpdateDto();
        createDto.setTitle(title);
        createDto.setDescription(description);
        createDto.setVisibility(VisibilityAlbums.ALL_USERS);

        when(userContext.getUserId()).thenReturn(userId);
        when(albumRepository.existsByTitleAndAuthorId(title, userId)).thenReturn(true);

        assertThrows(DataValidationException.class, () -> albumService.createAlbum(createDto));

        verify(userContext, times(1)).getUserId();
        verify(userServiceClient, times(1)).getUser(userId);
        verify(albumRepository, times(1)).existsByTitleAndAuthorId(title, userId);
    }

    @Test
    void createAlbumValidTest() {
        long userId = 1L;
        String title = "Title";
        String description = "Description";
        AlbumCreateUpdateDto createDto = new AlbumCreateUpdateDto();
        createDto.setTitle(title);
        createDto.setDescription(description);
        createDto.setVisibility(VisibilityAlbums.ALL_USERS);
        Album album = albumMapper.toEntity(createDto);
        album.setAuthorId(userId);

        when(userContext.getUserId()).thenReturn(userId);
        when(albumRepository.existsByTitleAndAuthorId(title, userId)).thenReturn(false);
        when(albumRepository.save(album)).thenReturn(album);

        AlbumDto responseDto = assertDoesNotThrow(() -> albumService.createAlbum(createDto));

        verify(userContext, times(1)).getUserId();
        verify(userServiceClient, times(1)).getUser(userId);
        verify(albumRepository, times(1)).existsByTitleAndAuthorId(title, userId);
        verify(albumRepository, times(1)).save(album);

        assertEquals(title, responseDto.getTitle());
        assertEquals(description, responseDto.getDescription());
    }

    @Test
    void addPostToNotExistingAlbumTest() {
        long albumId = 1L;
        long postId = 2L;
        when(albumRepository.findById(albumId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> albumService.addPostToAlbum(albumId, postId));

        verify(albumRepository, times(1)).findById(albumId);
    }

    @Test
    void addPostToAlbumUserInNotAuthorTest() {
        long albumId = 1L;
        long postId = 2L;
        long userId = 3L;
        long authorId = 4L;
        Album album = new Album();
        album.setAuthorId(authorId);

        when(userContext.getUserId()).thenReturn(userId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        assertThrows(ForbiddenException.class, () -> albumService.addPostToAlbum(albumId, postId));

        verify(albumRepository, times(1)).findById(albumId);
        verify(userContext, times(1 )).getUserId();
    }

    @Test
    void addPostToAlbumValidTest() {
        long albumId = 1L;
        long postId = 2L;
        long authorId = 3L;
        List<Long> expectedPostIds = List.of(postId);
        Album album = new Album();
        album.setAuthorId(authorId);
        album.setId(albumId);
        album.setPosts(new ArrayList<>());
        Post post = new Post();
        post.setId(postId);

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(postService.getPost(postId)).thenReturn(post);
        when(albumRepository.save(album)).thenReturn(album);

        AlbumDto responseDto = assertDoesNotThrow(() -> albumService.addPostToAlbum(albumId, postId));

        verify(albumRepository, times(1)).findById(albumId);
        verify(userContext, times(1 )).getUserId();
        verify(postService, times(1)).getPost(postId);
        verify(albumRepository, times(1)).save(album);

        assertEquals(albumId, responseDto.getId());
        assertEquals(expectedPostIds, responseDto.getPostIds());
    }

    @Test
    void deleteNotExistingPostFromAlbumTest() {
        long albumId = 1L;
        long postId = 2L;
        long authorId = 3L;
        Album album = new Album();
        album.setAuthorId(authorId);
        album.setPosts(new ArrayList<>());

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        assertDoesNotThrow(() -> albumService.deletePostFromAlbum(albumId, postId));

        verify(userContext, times(1)).getUserId();
        verify(albumRepository, times(1)).findById(albumId);
        verify(albumRepository, times(0)).save(album);
    }

    @Test
    void deleteExistingPostFromAlbumTest() {
        long albumId = 1L;
        long postId = 2L;
        long authorId = 3L;
        Post post = new Post();
        post.setId(postId);
        Album album = new Album();
        album.setAuthorId(authorId);
        album.setPosts(new ArrayList<>(List.of(post)));

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        assertDoesNotThrow(() -> albumService.deletePostFromAlbum(albumId, postId));

        verify(userContext, times(1)).getUserId();
        verify(albumRepository, times(1)).findById(albumId);
        verify(albumRepository, times(1)).save(album);

        assertEquals(0, album.getPosts().size());
    }

    @Test
    void addAlbumToFavorites() {
        long albumId = 1L;
        long authorId = 2L;
        Album album = new Album();
        album.setAuthorId(authorId);
        album.setVisibility(VisibilityAlbums.ALL_USERS);

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        assertDoesNotThrow(() -> albumService.addAlbumToFavorites(albumId));

        verify(userContext, times(1)).getUserId();
        verify(albumRepository, times(1)).findById(albumId);
        verify(albumRepository, times(1)).addAlbumToFavorites(albumId, authorId);
    }

    @Test
    void deleteAlbumFromFavorites() {
        long albumId = 1L;
        long authorId = 2L;
        Album album = new Album();
        album.setAuthorId(authorId);
        album.setVisibility(VisibilityAlbums.ALL_USERS);

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        assertDoesNotThrow(() -> albumService.deleteAlbumFromFavorites(albumId));

        verify(userContext, times(1)).getUserId();
        verify(albumRepository, times(1)).findById(albumId);
        verify(albumRepository, times(1)).deleteAlbumFromFavorites(albumId, authorId);
    }

    @Test
    void getAlbumByIdTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.ALL_USERS);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

        AlbumDto responseDto = albumService.getAlbumById(albumId);

        verify(albumRepository, times(1)).findById(albumId);
        assertEquals(albumId, responseDto.getId());
        assertEquals(title, responseDto.getTitle());
        assertEquals(description, responseDto.getDescription());
    }

    @Test
    void getAllAlbums() {
        String titlePattern = "Holiday";
        String descriptionPattern = "Vacation";
        LocalDateTime createdBefore = LocalDateTime.of(2024, 9, 10, 0, 0, 0);
        LocalDateTime createdAfter = LocalDateTime.of(2024, 8, 2, 0, 0, 0);
        Stream<Album> albums = Stream.of(
                createAlbum("Summer Holidays", "Amazing summer vacation photos!",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 9, 9)),
                createAlbum("Holiday Memories", "Fun moments from various holidays and vacations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 7, 20)),
                createAlbum("Winter Wonderland", "Beautiful winter scenery photos from our mountain trip.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2023, 12, 5)),
                createAlbum("Birthday Bash", "Celebrating John's birthday party with friends and family.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 2, 15)),
                createAlbum("Party Time", "Enjoy the holiday season with great parties and celebrations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 12, 25)),
                createAlbum("Vacation Spots", "Photos of our favorite family vacation spots.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 5, 14)),
                createAlbum("Family Holidays", "Precious moments from our family summer holidays.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 6, 18)),
                createAlbum("Reunited Again", "Another family reunion filled with fun and laughter.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 6, 25)),
                createAlbum("Summer Holidays", "More summer vacation pictures!",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 8, 30)),
                createAlbum("Adventurous Escapades", "Thrilling holiday adventures and exciting vacations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 8, 20))
        );
        AlbumFilterDto filterDto = AlbumFilterDto.builder()
                .titlePattern(titlePattern)
                .descriptionPattern(descriptionPattern)
                .createdBefore(createdBefore)
                .createdAfter(createdAfter)
                .build();
        when(albumRepository.findAll()).thenReturn(albums::iterator);

        List<AlbumDto> filteredAlbums = assertDoesNotThrow(() -> albumService.getAllAlbums(filterDto));

        assertEquals(2, filteredAlbums.size());
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getTitle().toLowerCase().contains(titlePattern.toLowerCase())));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getDescription().toLowerCase().contains(descriptionPattern.toLowerCase())));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getCreatedAt().isAfter(createdAfter)));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getCreatedAt().isBefore(createdBefore)));
        verify(albumRepository, times(1)).findAll();
    }

    @Test
    void getUserAlbums() {
        filterUserAlbums(
                albumService::getUserAlbums,
                (repository, albums) -> when(repository.findByAuthorId(anyLong())).thenReturn(albums)
        );
        verify(albumRepository, times(1)).findByAuthorId(anyLong());
    }

    @Test
    void getUserFavoriteAlbums() {
        filterUserAlbums(
                albumService::getUserFavoriteAlbums,
                (repository, albums) -> when(repository.findFavoriteAlbumsByUserId(anyLong())).thenReturn(albums)
        );
        verify(albumRepository, times(1)).findFavoriteAlbumsByUserId(anyLong());
    }

    @Test
    void getAlbumByIdVisibilityToSubscribersTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.SUBSCRIBERS);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(2L);
        when(userServiceClient.checkFollowerOfFollowee(1L, 2L)).thenReturn(true);

        AlbumDto responseDto = albumService.getAlbumById(albumId);

        verify(albumRepository, times(1)).findById(albumId);
        verify(userServiceClient, times(1)).checkFollowerOfFollowee(1L, 2L);
        assertEquals(albumId, responseDto.getId());
        assertEquals(title, responseDto.getTitle());
        assertEquals(description, responseDto.getDescription());
    }

    @Test
    void getAlbumByIdVisibilityToNotSubscribersTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.SUBSCRIBERS);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(2L);
        when(userServiceClient.checkFollowerOfFollowee(1L, 2L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> albumService.getAlbumById(albumId));
    }

    @Test
    void getAlbumByIdVisibilityToSelectedUsersTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.SELECTED_USERS);
        album.setBeholdersIds(List.of(1L, 2L, 3L));
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(2L);

        AlbumDto responseDto = albumService.getAlbumById(albumId);

        verify(albumRepository, times(1)).findById(albumId);
        assertEquals(albumId, responseDto.getId());
        assertEquals(title, responseDto.getTitle());
        assertEquals(description, responseDto.getDescription());
    }

    @Test
    void getAlbumByIdVisibilityToNotSelectedUsersTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.SELECTED_USERS);
        album.setBeholdersIds(List.of(1L, 2L, 3L));
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(4L);

        assertThrows(ForbiddenException.class, () -> albumService.getAlbumById(albumId));
    }

    @Test
    void getAlbumByIdVisibilityToOnlyAuthorTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.ONLY_AUTHOR);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(1L);

        AlbumDto responseDto = albumService.getAlbumById(albumId);

        verify(albumRepository, times(1)).findById(albumId);
        assertEquals(albumId, responseDto.getId());
        assertEquals(title, responseDto.getTitle());
        assertEquals(description, responseDto.getDescription());
    }

    @Test
    void getAlbumByIdVisibilityToNoAuthorTest() {
        long albumId = 1L;
        String title = "Title";
        String description = "Description";
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(1L);
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(VisibilityAlbums.ONLY_AUTHOR);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(2L);

        assertThrows(ForbiddenException.class, () -> albumService.getAlbumById(albumId));
    }

    @Test
    void updateAlbumValidTest() {
        long albumId = 1L;
        long authorId = 2L;
        String newTitle = "New Title";
        String newDescription = "New Description";
        Album album = new Album();
        album.setId(albumId);
        album.setTitle("First Title");
        album.setDescription("First Description");
        album.setAuthorId(authorId);
        AlbumCreateUpdateDto updateDto = AlbumCreateUpdateDto.builder()
                .title(newTitle)
                .description(newDescription)
                .build();

        when(userContext.getUserId()).thenReturn(authorId);
        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(albumRepository.existsByTitleAndAuthorId(newTitle, authorId)).thenReturn(false);
        when(albumRepository.save(album)).thenReturn(album);

        AlbumDto responseDto = assertDoesNotThrow(() -> albumService.updateAlbum(albumId, updateDto));

        verify(userContext, times(1)).getUserId();
        verify(albumRepository, times(1)).findById(albumId);
        verify(albumRepository, times(1)).existsByTitleAndAuthorId(newTitle, authorId);
        verify(albumRepository, times(1)).save(album);

        assertEquals(albumId, responseDto.getId());
        assertEquals(authorId, responseDto.getAuthorId());
        assertEquals(newTitle, responseDto.getTitle());
        assertEquals(newDescription, responseDto.getDescription());
    }

    @Test
    void deleteAlbumValidTest() {
        long albumId = 1L;
        long authorId = 2L;
        Album album = new Album();
        album.setId(albumId);
        album.setAuthorId(authorId);

        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(authorId);

        assertDoesNotThrow(() -> albumService.deleteAlbum(albumId));

        verify(albumRepository, times(1)).findById(albumId);
        verify(userContext, times(1)).getUserId();
    }

    private void filterUserAlbums(BiFunction<Long, AlbumFilterDto, List<AlbumDto>> method,
                                  BiConsumer<AlbumRepository, Stream<Album>> repositoryWhenAction) {
        long albumAuthorUserId = 1L;
        String titlePattern = "Holiday";
        String descriptionPattern = "Vacation";
        LocalDateTime createdBefore = LocalDateTime.of(2024, 9, 10, 0, 0, 0);
        LocalDateTime createdAfter = LocalDateTime.of(2024, 8, 2, 0, 0, 0);
        Stream<Album> albums = Stream.of(
                createAlbum("Summer Holidays", "Amazing summer vacation photos!",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 9, 9)),
                createAlbum("Holiday Memories", "Fun moments from various holidays and vacations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 7, 20)),
                createAlbum("Winter Wonderland", "Beautiful winter scenery photos from our mountain trip.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2023, 12, 5)),
                createAlbum("Birthday Bash", "Celebrating John's birthday party with friends and family.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 2, 15)),
                createAlbum("Party Time", "Enjoy the holiday season with great parties and celebrations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 12, 25)),
                createAlbum("Vacation Spots", "Photos of our favorite family vacation spots.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 5, 14)),
                createAlbum("Family Holidays", "Precious moments from our family summer holidays.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 6, 18)),
                createAlbum("Reunited Again", "Another family reunion filled with fun and laughter.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 6, 25)),
                createAlbum("Summer Holidays", "More summer vacation pictures!",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 8, 30)),
                createAlbum("Adventurous Escapades", "Thrilling holiday adventures and exciting vacations.",
                        VisibilityAlbums.ALL_USERS, LocalDate.of(2024, 8, 20))
        );
        AlbumFilterDto filterDto = AlbumFilterDto.builder()
                .titlePattern(titlePattern)
                .descriptionPattern(descriptionPattern)
                .createdBefore(createdBefore)
                .createdAfter(createdAfter)
                .build();
        repositoryWhenAction.accept(albumRepository, albums);

        List<AlbumDto> filteredAlbums = assertDoesNotThrow(() -> method.apply(albumAuthorUserId, filterDto));

        assertEquals(2, filteredAlbums.size());
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getTitle().toLowerCase().contains(titlePattern.toLowerCase())));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getDescription().toLowerCase().contains(descriptionPattern.toLowerCase())));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getCreatedAt().isAfter(createdAfter)));
        assertTrue(filteredAlbums.stream().allMatch(album -> album.getCreatedAt().isBefore(createdBefore)));
    }

    private Album createAlbum(String title, String description, VisibilityAlbums visibilityAlbums, LocalDate createdDate) {
        Album album = new Album();
        album.setTitle(title);
        album.setDescription(description);
        album.setVisibility(visibilityAlbums);
        album.setCreatedAt(createdDate.atStartOfDay());
        return album;
    }
}