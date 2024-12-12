package faang.school.postservice.dto.album;

import faang.school.postservice.enums.VisibilityAlbums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AlbumCreateUpdateDto {

    @NotBlank(message = "Album title cannot be null or empty!")
    @Size(max = 256, message = "Title length cannot exceed 256 symbols!")
    private String title;

    @NotBlank(message = "Album description cannot be null or empty!")
    @Size(max = 4096, message = "Title length cannot exceed 4096 symbols!")
    private String description;

    @NotNull
    private VisibilityAlbums visibility;

    private List<Long> beholdersIds;
}
