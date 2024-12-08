package faang.school.postservice.validator.post_file;

import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.exception.ForbiddenException;
import faang.school.postservice.model.Post;
import faang.school.postservice.properties.PostFileUploadProperties;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.file.FileDataDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostFileValidatorTest {

    @Mock
    private FileDataDetectionService fileDataDetectionService;

    @Mock
    private PostFileUploadProperties postFileUploadProperties;

    @InjectMocks
    private PostFileValidator postFileValidator;

    @Test
    void validateUploadFilesAmountValidFilesAmountTest() {
        List<MultipartFile> files = List.of(mock(MultipartFile.class), mock(MultipartFile.class));
        when(postFileUploadProperties.getMinimumAmount()).thenReturn(1);
        when(postFileUploadProperties.getMaximumAmount()).thenReturn(5);

        assertDoesNotThrow(() -> postFileValidator.validateUploadFilesAmount(files));
    }

    @Test
    void validateUploadFilesAmountTooManyFilesTest() {
        List<MultipartFile> files = List.of(
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class)
        );
        when(postFileUploadProperties.getMinimumAmount()).thenReturn(1);
        when(postFileUploadProperties.getMaximumAmount()).thenReturn(5);

        assertThrows(DataValidationException.class, () -> postFileValidator.validateUploadFilesAmount(files));
    }

    @Test
    void validateAlreadyUploadedFilesAmountValidTest() {
        List<MultipartFile> newFiles = List.of(mock(MultipartFile.class));
        int alreadyUploadedFiles = 4;
        when(postFileUploadProperties.getMaximumAmount()).thenReturn(5);

        assertDoesNotThrow(() -> postFileValidator.validateAlreadyUploadedFilesAmount(newFiles, alreadyUploadedFiles));
    }

    @Test
    void validateAlreadyUploadedFilesAmountExceedLimitTest() {
        List<MultipartFile> newFiles = List.of(mock(MultipartFile.class), mock(MultipartFile.class));
        int alreadyUploadedFiles = 4;
        when(postFileUploadProperties.getMaximumAmount()).thenReturn(5);

        assertThrows(DataValidationException.class, () -> postFileValidator.validateAlreadyUploadedFilesAmount(newFiles, alreadyUploadedFiles));
    }

    @Test
    void validatePostBelongsToUserUserIsAuthorTest() {
        long postId = 1L;
        Post post = new Post();
        post.setAuthorId(postId);

        assertDoesNotThrow(() -> postFileValidator.validatePostBelongsToUser(post, 1L));
    }

    @Test
    void validatePostBelongsToUserUserNotAuthorTest() {
        long postId = 1L;
        Post post = new Post();
        post.setAuthorId(postId);

        assertThrows(ForbiddenException.class, () -> postFileValidator.validatePostBelongsToUser(post, 2L));
    }

    @Test
    void validateFilesNotEmptyNonEmptyFilesTest() {
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);
        when(file.isEmpty()).thenReturn(false);

        assertDoesNotThrow(() -> postFileValidator.validateFilesNotEmpty(files));
    }

    @Test
    void validateFilesNotEmptyEmptyFileTest() {
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(DataValidationException.class, () -> postFileValidator.validateFilesNotEmpty(files));
    }

    @Test
    void validateAndExtractFileMetadatasInvalidFileExtensionTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        FileData fileData = new FileData(new byte[100], "test.invalid", "video", "invalid");
        List<MultipartFile> files = List.of(file);

        when(fileDataDetectionService.detect(file)).thenReturn(fileData);
        when(postFileUploadProperties.getInfoByFileType()).thenReturn(Map.of(
                "video", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("mp4", "avi")),
                "image", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("jpg", "png"))
        ));

        assertThrows(DataValidationException.class, () -> postFileValidator.validateAndExtractFileMetadatas(files));
    }

    @Test
    void validateAndExtractFileMetadatasFileExceedsMaxSizeTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        FileData fileData = new FileData(new byte[2000], "test.mp4", "video", "mp4");
        List<MultipartFile> files = List.of(file);

        when(fileDataDetectionService.detect(file)).thenReturn(fileData);
        when(postFileUploadProperties.getInfoByFileType()).thenReturn(Map.of(
                "video", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("mp4", "avi")),
                "image", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("jpg", "png"))
        ));

        assertThrows(DataValidationException.class, () -> postFileValidator.validateAndExtractFileMetadatas(files));
    }

    @Test
    void validateAndExtractFileMetadatasValidFilesTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        FileData fileData = new FileData(new byte[100], "test.jpg", "image", "jpg");
        List<MultipartFile> files = List.of(file);

        when(fileDataDetectionService.detect(file)).thenReturn(fileData);
        when(postFileUploadProperties.getInfoByFileType()).thenReturn(Map.of(
                "video", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("mp4", "avi")),
                "image", new PostFileUploadProperties.FileTypeInfo(1024, Set.of("jpg", "png"))
        ));

        List<FileData> result = postFileValidator.validateAndExtractFileMetadatas(files);

        assertEquals(1, result.size());
        assertEquals(fileData, result.get(0));
    }

    @Test
    void validateAndExtractFileMetadatasIOExceptionTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);
        when(fileDataDetectionService.detect(file)).thenThrow(new IOException("Error reading file"));

        assertThrows(FileProcessException.class,() -> postFileValidator.validateAndExtractFileMetadatas(files));
    }
}