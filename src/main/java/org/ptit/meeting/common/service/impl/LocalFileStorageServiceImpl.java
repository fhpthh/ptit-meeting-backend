package org.ptit.meeting.common.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.service.FileStorageService;
import org.ptit.meeting.config.storage.FileStorageProperties;
import org.ptit.meeting.exception.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

  private final FileStorageProperties properties;

  @Override
  public String storeFile(MultipartFile file, String moduleName) {
    validateFile(file, moduleName);

    FileStorageProperties.ModuleUploadRule rule = properties.getRule(moduleName);
    String baseDir = properties.getLocal() != null && properties.getLocal().getBaseDir() != null
        ? properties.getLocal().getBaseDir() : "uploads";
    String subDir = rule.getSubDir() != null ? rule.getSubDir() : "";

    Path uploadPath = subDir.isEmpty() ? Paths.get(baseDir) : Paths.get(baseDir, subDir);

    try {
      Files.createDirectories(uploadPath);
      String ext = getFileExtension(file.getOriginalFilename());
      String uniqueFilename =
          System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
      Path targetLocation = uploadPath.resolve(uniqueFilename);

      Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
      return subDir.isEmpty() ? uniqueFilename : (subDir + "/" + uniqueFilename);
    } catch (IOException ex) {
      log.error("(storeFile) Error storing file: ", ex);
      throw BusinessException.invalidInputFile("system.internal_error");
    }
  }

  private void validateFile(MultipartFile file, String moduleName) {
    if (file == null || file.isEmpty()) {
      throw BusinessException.invalidInputFile("error.timetable.file.empty");
    }
    FileStorageProperties.ModuleUploadRule rule = properties.getRule(moduleName);
    if (file.getSize() > rule.getMaxSizeBytes()) {
      throw BusinessException.invalidInputFile("attachment.file_too_large");
    }
    String ext = getFileExtension(file.getOriginalFilename());
    if (!rule.getAllowedExtensions().contains(ext.toLowerCase())) {
      throw BusinessException.invalidInputFile("error.timetable.file.invalid-format");
    }
  }

  @Override
  public Resource loadFileAsResource(String relativePath) {
    try {
      String baseDir = properties.getLocal() != null && properties.getLocal().getBaseDir() != null
          ? properties.getLocal().getBaseDir() : "uploads";
      Path filePath = Paths.get(baseDir).resolve(relativePath)
          .normalize();
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists()) {
        return resource;
      }
      throw BusinessException.notExisted("resource.not_found");
    } catch (Exception e) {
      throw BusinessException.notExisted("resource.not_found");
    }
  }

  @Override
  public void deleteFile(String relativePath) {
    try {
      String baseDir = properties.getLocal() != null && properties.getLocal().getBaseDir() != null
          ? properties.getLocal().getBaseDir() : "uploads";
      Path filePath = Paths.get(baseDir).resolve(relativePath)
          .normalize();
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      log.warn("(deleteFile) Could not delete file: {}", relativePath);
    }
  }

  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf("."));
  }
}