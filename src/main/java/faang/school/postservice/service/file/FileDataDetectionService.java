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
public class FileDataDetectionService {

    private static final Pattern BEFORE_AND_AFTER_SLASH_PATTERN = Pattern.compile("([^/]+)/([^/]+)");

    private final Tika tika;

    public FileData detect(MultipartFile file) throws IOException {
        byte[] fileData = file.getBytes();
        String typeWithExtension = tika.detect(file.getInputStream());
        Matcher matcher = BEFORE_AND_AFTER_SLASH_PATTERN.matcher(typeWithExtension);
        if (matcher.find()) {
            String type = matcher.group(1);
            String extension = matcher.group(2);

            return new FileData(fileData, file.getOriginalFilename(), type, extension);
        }
        return new FileData(fileData, file.getOriginalFilename(),"another", "");
    }
}
