package faang.school.postservice.dto.album;

import com.fasterxml.jackson.annotation.JsonFormat;
import faang.school.postservice.enums.VisibilityAlbums;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AlbumDto {

    private long id;
    private String title;
    private String description;
    private long authorId;
    private List<Long> postIds;

    @NotNull
    private VisibilityAlbums visibility;

    private List<Long> beholdersIds;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
