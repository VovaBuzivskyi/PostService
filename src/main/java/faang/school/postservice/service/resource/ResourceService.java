package faang.school.postservice.service.resource;

import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.model.Resource;
import faang.school.postservice.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE = "Resource";

    private final ResourceRepository resourceRepository;

    public int getCountByPostId(long postId) {
        return resourceRepository.countByPostId(postId);
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    public List<Resource> findAllByPostId(long postId) {
        return resourceRepository.findAllByPostId(postId);
    }

    public Resource getResource(long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException(RESOURCE, resourceId));
    }

    public void deleteResource(long resourceId) {
        resourceRepository.deleteById(resourceId);
    }
}
