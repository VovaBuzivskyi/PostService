package faang.school.postservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlbumCreatedEvent {

    private Long userId;

    private Long albumId;

    private String titleAlbum;
}
