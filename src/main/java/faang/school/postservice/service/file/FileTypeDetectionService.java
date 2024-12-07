package faang.school.postservice.service.file;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileTypeDetectionService {

    private static final Pattern BEFORE_AND_AFTER_SLASH_PATTERN = Pattern.compile("([^/]+)/([^/]+)");

    private final Tika tika;

    public FileMetadata detect(MultipartFile file) throws IOException {
        String typeWithExtension = tika.detect(file.getInputStream());
        Matcher matcher = BEFORE_AND_AFTER_SLASH_PATTERN.matcher(typeWithExtension);

        if (matcher.find()) {
            String type = matcher.group(1);
            String extension = matcher.group(2);
            return new FileMetadata(file, type, extension);
        }

        return new FileMetadata(file, "another", "");
    }
}
