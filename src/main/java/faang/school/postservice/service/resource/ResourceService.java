package faang.school.postservice.service.resource;

import faang.school.postservice.model.Resource;
import faang.school.postservice.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public int getCountByPostId(long postId) {
        return resourceRepository.countByPostId(postId);
    }

    @Transactional
    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }
}
