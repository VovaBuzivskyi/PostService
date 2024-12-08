package faang.school.postservice.service.post_file;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.event.file.PostFilesUploadedEvent;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.publisher.PostFilesUploadedEventPublisher;
import faang.school.postservice.service.amazonS3.AmazonS3Service;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.service.resource.ResourceService;
import faang.school.postservice.validator.post_file.PostFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostFileService {

    private final PostFilesUploadedEventPublisher postFilesUploadedEventPublisher;
    private final UserContext userContext;
    private final PostFileValidator postFileValidator;
    private final ResourceService resourceService;
    private final AmazonS3Service amazonS3Service;
    private final PostService postService;

    public void uploadFiles(List<MultipartFile> files, long postId) {
        long requesterId = userContext.getUserId();
        log.info("Received request from user with ID {} to upload files to post with ID {}", requesterId, postId);

        Post post = postService.getPost(postId);
        int alreadyUploadedFilesAmount = resourceService.getCountByPostId(postId);
        postFileValidator.validatePostBelongsToUser(post, requesterId);
        postFileValidator.validateUploadFilesAmount(files);
        postFileValidator.validateAlreadyUploadedFilesAmount(files, alreadyUploadedFilesAmount);
        postFileValidator.validateFilesNotEmpty(files);
        List<FileData> fileDatas = postFileValidator.validateAndExtractFileMetadatas(files);

        ConcurrentLinkedQueue<Resource> futureResources = new ConcurrentLinkedQueue<>();
        String folder = "post/%s".formatted(postId);
        CompletableFuture<Void> allUploads = CompletableFuture.allOf(
                fileDatas.stream()
                        .map(fileData -> amazonS3Service.uploadFile(fileData, folder)
                                .thenAccept(uploadFileInfo -> {
                                    Resource resource = createResource(uploadFileInfo.getRight(), uploadFileInfo.getLeft(), post);
                                    futureResources.add(resourceService.save(resource));
                                })
                        ).toArray(CompletableFuture[]::new));

        allUploads.thenRun(() -> {
            PostFilesUploadedEvent event = new PostFilesUploadedEvent();
            event.setUserId(requesterId);
            Map<String, String> keyToFileName = futureResources.stream()
                    .collect(Collectors.toMap(
                            Resource::getKey,
                            Resource::getName
                    ));
            event.setKeyToFileName(keyToFileName);
            postFilesUploadedEventPublisher.publish(event);
        });
    }

    private Resource createResource(FileData fileData, String key, Post post) {
        return Resource.builder()
                .key(key)
                .name(fileData.getOriginalName())
                .type("%s/%s".formatted(fileData.getType(), fileData.getExtension()))
                .size(fileData.getData().length)
                .post(post)
                .build();
    }
}
