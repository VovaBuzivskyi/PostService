package faang.school.postservice.service.resource;

import faang.school.postservice.model.Resource;
import faang.school.postservice.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public int getCountByPostId(long postId) {
        return resourceRepository.countByPostId(postId);
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }
}
