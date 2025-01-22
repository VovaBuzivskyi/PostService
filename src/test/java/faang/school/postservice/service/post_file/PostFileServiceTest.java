package faang.school.postservice.service.post_file;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.event.file.PostFilesUploadedEvent;
import faang.school.postservice.mapper.post_file.PostFileMapperImpl;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.publisher.redis.impl.PostFilesUploadedEventPublisher;
import faang.school.postservice.service.amazonS3.AmazonS3Service;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.service.resource.ResourceService;
import faang.school.postservice.validator.post_file.PostFileValidator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostFileServiceTest {

    @Spy
    private PostFileMapperImpl postFileMapper;

    @Mock
    private PostFilesUploadedEventPublisher postFilesUploadedEventPublisher;

    @Mock
    private UserContext userContext;

    @Mock
    private PostFileValidator postFileValidator;

    @Mock
    private ResourceService resourceService;

    @Mock
    private AmazonS3Service amazonS3Service;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostFileService postFileService;

    @Test
    void uploadFilesSuccessfullyTest() {
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(file);
        long postId = 1L;
        long requesterId = 1L;

        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(requesterId);

        List<FileData> fileDatas = Collections.singletonList(new FileData(new byte[]{1, 2, 3}, "file.jpg", "image", "jpg"));

        when(userContext.getUserId()).thenReturn(requesterId);
        when(postService.getPost(postId)).thenReturn(post);
        when(resourceService.getCountByPostId(postId)).thenReturn(0);
        when(postFileValidator.validateAndExtractFileMetadatas(files)).thenReturn(fileDatas);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getKey()).thenReturn("key");
        when(mockResource.getName()).thenReturn("file.jpg");

        when(resourceService.save(any(Resource.class))).thenReturn(mockResource);
        when(amazonS3Service.uploadFile(any(), any())).thenAnswer(invocation -> {
            FileData fileData = invocation.getArgument(0);
            return CompletableFuture.completedFuture(Pair.of("key", fileData));
        });

        postFileService.uploadFiles(files, postId);

        verify(postFileValidator, times(1)).validatePostBelongsToUser(post, requesterId);
        verify(postFileValidator, times(1)).validateUploadFilesAmount(files);
        verify(postFileValidator, times(1)).validateAlreadyUploadedFilesAmount(files, 0);
        verify(postFileValidator, times(1)).validateFilesNotEmpty(files);
        verify(postFileValidator, times(1)).validateAndExtractFileMetadatas(files);
        verify(amazonS3Service, times(1)).uploadFile(any(), any());
        verify(resourceService, times(1)).save(any(Resource.class));
        verify(postFilesUploadedEventPublisher, times(1)).publish(any(PostFilesUploadedEvent.class));
    }

    @Test
    void deletePostFileSuccessfulTest() {
        long postId = 1L;
        long fileId = 1L;
        long requesterId = 1L;

        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(requesterId);

        Resource resource = new Resource();
        resource.setId(fileId);

        when(userContext.getUserId()).thenReturn(requesterId);
        when(postService.getPost(postId)).thenReturn(post);
        when(resourceService.getResource(fileId)).thenReturn(resource);

        postFileService.deletePostFile(postId, fileId);

        verify(postFileValidator, times(1)).validatePostBelongsToUser(post, requesterId);
        verify(amazonS3Service, times(1)).deleteFile(resource.getKey());
        verify(resourceService, times(1)).deleteResource(fileId);
    }
}