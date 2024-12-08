package faang.school.postservice.event.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostFilesUploadedEvent {

    private long userId;
    private Map<String, String> keyToFileName;
}
