package org.ptit.meeting.config.storage;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.storage")
public class FileStorageProperties {

  private String type = "LOCAL";
  private LocalConfig localConfig = new LocalConfig();
  private Map<String, ModuleUploadRule> modules;

  public ModuleUploadRule getRule(String moduleName) {
    if (modules == null || !modules.containsKey(moduleName)) {
      throw new IllegalArgumentException("Module " + moduleName + " not found");
    }
    return modules.get(moduleName);
  }

  @Getter
  @Setter
  public static class LocalConfig {

    private String baseUrl = "uploads";
  }

  @Getter
  @Setter
  public static class ModuleUploadRule {

    private String subUrl;
    private long maxSizeBytes;
    private List<String> allowedExtensions;

  }
}
