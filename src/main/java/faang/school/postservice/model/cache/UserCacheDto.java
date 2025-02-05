package faang.school.postservice.model.cache;

import faang.school.postservice.dto.user.UserProfilePictureDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCacheDto implements Serializable {

    private long userId;
    private String username;
    private boolean active;
    private UserProfilePictureDto profilePicture;
}
