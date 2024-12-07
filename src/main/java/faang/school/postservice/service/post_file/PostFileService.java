package faang.school.postservice.service.post_file;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.event.file.FilesUploadedEvent;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.service.amazonS3.AmazonS3Service;
import faang.school.postservice.service.file.FileMetadata;
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
        List<FileMetadata> fileMetadatas = postFileValidator.validateAndExtractFileMetadatas(files);

        ConcurrentLinkedQueue<Resource> futureResources = new ConcurrentLinkedQueue<>();
        String folder = "post/%s".formatted(postId);
        CompletableFuture<Void> allUploads = CompletableFuture.allOf(
                fileMetadatas.stream()
                        .map(fileMetadata -> amazonS3Service.uploadFile(fileMetadata, folder)
                                .thenAccept(uploadFileInfo -> {
                                    Resource resource = createResource(uploadFileInfo.getRight(), uploadFileInfo.getLeft());
                                    futureResources.add(resourceService.save(resource));
                                })
                        ).toArray(CompletableFuture[]::new)
        );
        allUploads.thenRun(() -> {
            FilesUploadedEvent event = new FilesUploadedEvent();
            event.setUserId(requesterId);
            Map<String, String> keyToFileName = futureResources.stream()
                    .collect(Collectors.toMap(Resource::getName, Resource::getKey));
            event.setKeyToFileName(keyToFileName);
        });
    }

    private Resource createResource(FileMetadata fileMetadata, String key) {
        return Resource.builder()
                .key(key)
                .name(fileMetadata.getFile().getOriginalFilename())
                .type("%s/%s".formatted(fileMetadata.getType(), fileMetadata.getExtension()))
                .size(fileMetadata.getFile().getSize())
                .build();
    }
}
