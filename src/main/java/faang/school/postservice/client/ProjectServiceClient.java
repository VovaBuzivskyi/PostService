package faang.school.postservice.client;

import faang.school.postservice.dto.project.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "project-service", url = "${project-service.host}:${project-service.port}${project-service.path}")
public interface ProjectServiceClient {

    @GetMapping("/projects/{projectId}")
    ProjectDto getProject(@PathVariable long projectId);

    @PostMapping("/projects/filter")
    List<ProjectDto> getProjectsByIds(@RequestBody List<Long> ids);
}
