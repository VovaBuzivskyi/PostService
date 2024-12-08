package faang.school.postservice.service.amazonS3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.image.ImageCompressionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AmazonS3Service {

    private static final String IMAGE_TYPE = "image";

    private final AmazonS3 amazonS3Client;
    private final ImageCompressionService imageCompressionService;

    @Value("${amazonS3.bucket-name}")
    private String bucketName;

    @Async("fileUploadTaskExecutor")
    public CompletableFuture<Pair<String, FileData>> uploadFile(FileData fileMetadata, String folder) {
        try {
            byte[] fileData = fileMetadata.getType().equals(IMAGE_TYPE)
                    ? compressImage(fileMetadata)
                    : fileMetadata.getData();
            try (ByteArrayInputStream fileStream = new ByteArrayInputStream(fileData)) {
                PutObjectRequest request = createPutObjectRequest(fileStream, fileMetadata, folder);
                amazonS3Client.putObject(request);
                return CompletableFuture.completedFuture(Pair.of(request.getKey(), fileMetadata));
            }
        } catch (IOException e) {
            throw new FileProcessException("Error occurred while uploading file: " + fileMetadata.getOriginalName(), e);
        }
    }

    private PutObjectRequest createPutObjectRequest(InputStream fileStream, FileData fileData, String folder) {
        String fileName = folder + "/" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1, 10001)
                + "_" + fileData.getOriginalName();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("%s/%s".formatted(fileData.getType(), fileData.getExtension()));
        return new PutObjectRequest(bucketName, fileName, fileStream, metadata);
    }

    private byte[] compressImage(FileData fileData) throws IOException {
        byte[] imageData = imageCompressionService.compressImage(fileData.getData(), fileData.getExtension());
        fileData.setData(imageData);
        return imageData;
    }
}
