package faang.school.postservice.service.file;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileDataDetectionServiceTest {

    @Mock
    private Tika tika;

    @InjectMocks
    private FileDataDetectionService fileDataDetectionService;

    @Test
    public void detectFileTypeAndExtensionTest() throws IOException {
        String detectedType = "image";
        String detectedExtension = "jpeg";
        String typeWithExtension = "%s/%s".formatted(detectedType, detectedExtension);
        byte[] fileBytes = new byte[] {41, 4, 2, 5};
        MultipartFile file = mock(MultipartFile.class);
        when(tika.detect(file.getInputStream())).thenReturn(typeWithExtension);
        when(file.getBytes()).thenReturn(fileBytes);

        FileData fileData = fileDataDetectionService.detect(file);

        assertNotNull(fileData);
        assertEquals(fileBytes, fileData.getData());
        assertEquals(file.getOriginalFilename(), fileData.getOriginalName());
        assertEquals(detectedType, fileData.getType());
        assertEquals(detectedExtension, fileData.getExtension());
    }

    @Test
    public void detectFileWithUnknownTypeTest() throws IOException {
        String detectedType = "unknown";
        byte[] fileBytes = new byte[] {41, 4, 2, 5};
        MultipartFile file = mock(MultipartFile.class);
        when(tika.detect(file.getInputStream())).thenReturn(detectedType);
        when(file.getBytes()).thenReturn(fileBytes);

        FileData fileData = fileDataDetectionService.detect(file);

        assertNotNull(fileData);
        assertEquals(fileBytes, fileData.getData());
        assertEquals(file.getOriginalFilename(), fileData.getOriginalName());
        assertEquals("another", fileData.getType());
        assertEquals("", fileData.getExtension());
    }

    @Test
    public void detectFileIOExceptionTest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(tika.detect(file.getInputStream())).thenThrow(new IOException());

        assertThrows(IOException.class, () -> fileDataDetectionService.detect(file));
    }
}