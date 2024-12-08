package faang.school.postservice.service.amazonS3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.image.ImageCompressionService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmazonS3ServiceTest {

    @InjectMocks
    private AmazonS3Service amazonS3Service;

    @Mock
    private AmazonS3 amazonS3Client;

    @Mock
    private ImageCompressionService imageCompressionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(amazonS3Service, "bucketName", "test");
    }

    @Test
    void uploadFileImageCompressionAppliedTest() throws IOException {
        byte[] originFileBytes = new byte[]{1, 2, 3};
        FileData fileData = new FileData(originFileBytes, "test.jpg", "image", "jpg");
        byte[] compressedData = new byte[]{4, 5, 6};
        String folder = "test-folder";
        when(imageCompressionService.compressImage(fileData.getData(), fileData.getExtension())).thenReturn(compressedData);

        CompletableFuture<Pair<String, FileData>> result = amazonS3Service.uploadFile(fileData, folder);

        verify(imageCompressionService, Mockito.times(1)).compressImage(originFileBytes, fileData.getExtension());
        verify(amazonS3Client, Mockito.times(1)).putObject(any(PutObjectRequest.class));
        assertNotNull(result);
        assertEquals(fileData, result.join().getRight());
    }

    @Test
    void uploadFileWithoutCompressionTest() throws IOException {
        byte[] originFileBytes = new byte[]{1, 2, 3};
        FileData fileData = new FileData(originFileBytes, "test.jpg", "image", "jpg");
        String folder = "test-folder";
        when(imageCompressionService.compressImage(fileData.getData(), fileData.getExtension())).thenReturn(originFileBytes);

        CompletableFuture<Pair<String, FileData>> result = amazonS3Service.uploadFile(fileData, folder);

        verify(imageCompressionService, times(1)).compressImage(originFileBytes, "jpg");
        verify(amazonS3Client, Mockito.times(1)).putObject(any(PutObjectRequest.class));
        assertNotNull(result);
        assertEquals(fileData, result.join().getRight());
    }

    @Test
    void uploadFileIOExceptionTest() throws IOException {
        FileData fileData = new FileData(new byte[]{1, 2, 3}, "test.jpg", "image", "jpg");
        String folder = "test-folder";
        when(imageCompressionService.compressImage(fileData.getData(), fileData.getExtension())).thenThrow(new IOException("Compression error"));

        FileProcessException exception = assertThrows(FileProcessException.class, () -> {
            amazonS3Service.uploadFile(fileData, folder);
        });

        assertTrue(exception.getMessage().contains("Error occurred while uploading file"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void uploadFileSdkClientExceptionTest() throws IOException {
        FileData fileData = new FileData(new byte[]{1, 2, 3}, "test.jpg", "image", "jpg");
        String folder = "test-folder";
        when(imageCompressionService.compressImage(fileData.getData(), fileData.getExtension())).thenReturn(fileData.getData());
        doThrow(new SdkClientException("S3 Client error")).when(amazonS3Client).putObject(any(PutObjectRequest.class));

        FileProcessException exception = assertThrows(FileProcessException.class, () -> {
            amazonS3Service.uploadFile(fileData, folder);
        });

        assertTrue(exception.getMessage().contains("Error occurred while uploading file"));
        assertInstanceOf(SdkClientException.class, exception.getCause());
    }

    @Test
    void deleteFileSuccessfulTest() {
        String fileKey = "test/file.jpg";
        amazonS3Service.deleteFile(fileKey);
        verify(amazonS3Client, Mockito.times(1)).deleteObject(Mockito.anyString(), Mockito.eq(fileKey));
    }

    @Test
    void deleteFileThrowsExceptionTest() {
        String fileKey = "test/file.jpg";
        doThrow(new SdkClientException("S3 error")).when(amazonS3Client).deleteObject(Mockito.anyString(), Mockito.eq(fileKey));

        FileProcessException exception = assertThrows(FileProcessException.class, () -> amazonS3Service.deleteFile(fileKey));

        verify(amazonS3Client, Mockito.times(1)).deleteObject(Mockito.anyString(), Mockito.eq(fileKey));
        assertInstanceOf(SdkClientException.class, exception.getCause());
    }

    @Test
    void getFileFromS3SuccessfulTest() {
        String fileKey = "test/file.jpg";
        S3Object mockFile = new S3Object();

        when(amazonS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockFile);

        S3Object result = amazonS3Service.getFileFromS3(fileKey);

        assertNotNull(result);
        assertEquals(mockFile, result);
        verify(amazonS3Client, Mockito.times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void getFileFromS3ThrowsExceptionTest() {
        String fileKey = "test/file.jpg";

        doThrow(new SdkClientException("S3 error")).when(amazonS3Client).getObject(any(GetObjectRequest.class));

        FileProcessException exception = assertThrows(FileProcessException.class, () -> amazonS3Service.getFileFromS3(fileKey));

        verify(amazonS3Client, Mockito.times(1)).getObject(any(GetObjectRequest.class));
        assertInstanceOf(SdkClientException.class, exception.getCause());
    }
}