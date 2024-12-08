package faang.school.postservice.validator.post_file;

import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.FileProcessException;
import faang.school.postservice.exception.ForbiddenException;
import faang.school.postservice.model.Post;
import faang.school.postservice.properties.PostFileUploadProperties;
import faang.school.postservice.properties.PostFileUploadProperties.FileTypeInfo;
import faang.school.postservice.service.file.FileData;
import faang.school.postservice.service.file.FileDataDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PostFileValidator {

    private final FileDataDetectionService fileDataDetectionService;
    private final PostFileUploadProperties postFileUploadProperties;

    public void validateUploadFilesAmount(List<MultipartFile> files) {
        int minFileUploadAmount = postFileUploadProperties.getMinimumAmount();
        int maxFileUploadAmount = postFileUploadProperties.getMaximumAmount();
        if (files.size() < minFileUploadAmount || files.size() > maxFileUploadAmount) {
            throw new DataValidationException("Unable to upload to post more than %d files! You are trying to upload %d files."
                    .formatted(maxFileUploadAmount, files.size()));
        }
    }

    public void validateAlreadyUploadedFilesAmount(List<MultipartFile> files, int alreadyUploadedFilesAmount) {
        int maxFileUploadAmount = postFileUploadProperties.getMaximumAmount();
        if (alreadyUploadedFilesAmount + files.size() > maxFileUploadAmount) {
            throw new DataValidationException("Unable to upload to post more than %d files! You are trying to upload %d files, already uploaded: %d."
                    .formatted(maxFileUploadAmount, files.size(), alreadyUploadedFilesAmount));
        }
    }

    public void validatePostBelongsToUser(Post post, long userId) {
        if (post.getAuthorId() != userId) {
            throw new ForbiddenException(userId, "upload files to post with ID %d".formatted(post.getId()));
        }
    }

    public void validateFilesNotEmpty(List<MultipartFile> files) {
        for (MultipartFile file: files) {
            if (file.isEmpty()) {
                throw new DataValidationException("The file named '%s' is empty!".formatted(file.getName()));
            }
        }
    }

    public List<FileData> validateAndExtractFileMetadatas(List<MultipartFile> files) {
        List<FileData> fileDatas = new ArrayList<>();
        for (MultipartFile file: files) {
            try {
                FileData fileData = fileDataDetectionService.detect(file);
                validateFileExtension(fileData);
                validateFileSize(fileData);
                fileDatas.add(fileData);
            } catch (IOException e) {
                throw new FileProcessException(
                        "The file named '%s' is unable to be processed.".formatted(file.getOriginalFilename()), e);
            }
        }
        return fileDatas;
    }

    private void validateFileExtension(FileData fileData) {
        Map<String, FileTypeInfo> infoByFileType = postFileUploadProperties.getInfoByFileType();
        FileTypeInfo fileTypeInfo = infoByFileType.getOrDefault(fileData.getType(), infoByFileType.get("another"));
        Set<String> allowedExtensions = fileTypeInfo.getAllowedExtensions();
        System.out.println(fileData.getData().length);
        if (!allowedExtensions.contains(fileData.getExtension())) {
            throw new DataValidationException(
                    String.format("The file named '%s' is not allowed to be uploaded due to extension. Allowed extensions: %s.",
                            fileData.getOriginalName(), allowedExtensions)
            );
        }
    }

    private void validateFileSize(FileData fileData) {
        Map<String, FileTypeInfo> infoByFileType = postFileUploadProperties.getInfoByFileType();
        FileTypeInfo fileTypeInfo = infoByFileType.getOrDefault(fileData.getType(), infoByFileType.get("another"));
        int fileTypeMaxSize = fileTypeInfo.getMaxSize();

        if (fileData.getData().length > fileTypeMaxSize) {
            String sizeExceedMessage = String.format(
                    "The file named '%s' with type '%s' exceeds the maximum allowed size. " +
                            "File size: %d bytes, allowed: %d bytes.",
                    fileData.getOriginalName(),
                    fileData.getType(),
                    fileData.getData().length,
                    fileTypeMaxSize
            );
            throw new DataValidationException(sizeExceedMessage);
        }
    }
}
