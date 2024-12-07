package faang.school.postservice.service.amazonS3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.service.file.FileMetadata;
import faang.school.postservice.service.image.ImageCompressionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AmazonS3Service {

    private static final String IMAGE_TYPE = "image";

    private final AmazonS3 amazonS3Client;
    private final ImageCompressionService imageCompressionService;

    @Value("${amazonS3.bucket-name}")
    private String bucketName;

    @Async("fileUploadTaskExecutor")
    public CompletableFuture<Pair<String, FileMetadata>> uploadFile(FileMetadata fileMetadata, String folder) {
        try (InputStream fileStream = fileMetadata.getType().equals(IMAGE_TYPE)
                ? imageCompressionService.compressImage(fileMetadata.getFile().getInputStream(), fileMetadata.getExtension())
                : fileMetadata.getFile().getInputStream()
        ) {
            PutObjectRequest request = createPutObjectRequest(fileStream, fileMetadata, folder);
            amazonS3Client.putObject(request);
            return CompletableFuture.completedFuture(Pair.of(request.getKey(), fileMetadata));
        } catch (IOException e) {
            throw new FileProcessException(
                    "Error occurred while uploading file: " + fileMetadata.getFile().getOriginalFilename(), e);
        }
    }

    private PutObjectRequest createPutObjectRequest(InputStream fileStream, FileMetadata fileMetadata, String folder) {
        String fileName = folder + "/" + System.currentTimeMillis() + "_" + fileMetadata.getFile().getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(fileMetadata.getType());

        return new PutObjectRequest(bucketName, fileName, fileStream, metadata);
    }
}
