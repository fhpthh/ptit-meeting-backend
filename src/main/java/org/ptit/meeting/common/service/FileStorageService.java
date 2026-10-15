package org.ptit.meeting.common.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

  String storeFile(MultipartFile file, String moduleName);

  Resource loadFileAsResource(String relativePath);

  void deleteFile(String relativePath);
}
