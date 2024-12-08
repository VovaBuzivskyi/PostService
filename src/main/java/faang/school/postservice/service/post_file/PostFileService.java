package faang.school.postservice.service.post_file;

import com.amazonaws.services.s3.model.S3Object;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post_file.PostFileDto;
import faang.school.postservice.event.file.PostFilesUploadedEvent;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.mapper.post_file.PostFileMapper;
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
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostFileService {

    private final PostFileMapper postFileMapper;
    private final PostFilesUploadedEventPublisher postFilesUploadedEventPublisher;
    private final UserContext userContext;
    private final PostFileValidator postFileValidator;
    private final ResourceService resourceService;
    private final AmazonS3Service amazonS3Service;
    private final PostService postService;

    public void uploadFiles(List<MultipartFile> files, long postId) {
        long requesterId = userContext.getUserId();
        log.info("Received request from user with ID: {} to upload files to post with ID: {}", requesterId, postId);

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
            log.info("Request from user with ID: {} to upload files to post with ID: {} was processed", requesterId, postId);
        });
    }

    public List<PostFileDto> getPostFilesInfo(long postId) {
        Post post = postService.getPost(postId);
        List<Resource> postFiles = resourceService.findAllByPostId(post.getId());
        return postFileMapper.toDtoList(postFiles);
    }

    public void deletePostFile(long postId, long fileId) {
        long requesterId = userContext.getUserId();
        log.info("Received request from user with ID: {} to delete file with ID: {} from post with ID: {}", requesterId, fileId, postId);

        Post post = postService.getPost(postId);
        postFileValidator.validatePostBelongsToUser(post, requesterId);
        Resource postFile = resourceService.getResource(fileId);
        amazonS3Service.deleteFile(postFile.getKey());
        resourceService.deleteResource(fileId);

        log.info("File with ID: {} was deleted from post with ID: {}. Requester ID: {}", fileId, postId, requesterId);
    }

    public FileData downloadFile(long postId, long fileId) {
        long requesterId = userContext.getUserId();
        log.info("Received request from user with ID: {} to download file with ID: {} from post with ID: {}", requesterId, fileId, postId);

        Post post = postService.getPost(postId);
        postFileValidator.validatePostBelongsToUser(post, requesterId);
        Resource postFile = resourceService.getResource(fileId);
        S3Object file = amazonS3Service.getFileFromS3(postFile.getKey());

        String[] fileTypeInfo = postFile.getType().split("/");
        try (InputStream inputStream = file.getObjectContent()) {
            byte[] data = IOUtils.toByteArray(inputStream);
            log.info("File with ID: {} (key={}) was downloaded from AmazonS3", fileId, postFile.getKey());
            return FileData.builder()
                    .data(data)
                    .originalName(postFile.getName())
                    .type(fileTypeInfo[0])
                    .extension(fileTypeInfo[1])
                    .build();
        } catch (IOException e) {
            throw new FileProcessException("Error occurred while reading file from S3: %s".formatted(postFile.getKey()), e);
        }
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
