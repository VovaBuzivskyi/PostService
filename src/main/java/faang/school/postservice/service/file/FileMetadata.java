package faang.school.postservice.service.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@AllArgsConstructor
public class FileMetadata {

    private MultipartFile file;
    private String type;
    private String extension;
}
