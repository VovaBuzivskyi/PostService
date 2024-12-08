package faang.school.postservice.service.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class FileData {

    private byte[] data;
    private String originalName;
    private String type;
    private String extension;
}
