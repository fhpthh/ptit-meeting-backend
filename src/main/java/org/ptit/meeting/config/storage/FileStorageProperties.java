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
  private LocalConfig local = new LocalConfig();
  private Map<String, ModuleUploadRule> modules;

  public ModuleUploadRule getRule(String moduleName) {
    if (modules == null || !modules.containsKey(moduleName)) {
      throw new IllegalArgumentException("Module " + moduleName + " not found");
    }
    return modules.get(moduleName);
  }

  public LocalConfig getLocalConfig() {
    return local;
  }

  public void setLocalConfig(LocalConfig localConfig) {
    this.local = localConfig;
  }

  @Getter
  @Setter
  public static class LocalConfig {

    private String baseDir = "uploads";

    public String getBaseUrl() {
      return baseDir;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseDir = baseUrl;
    }
  }

  @Getter
  @Setter
  public static class ModuleUploadRule {

    private String subDir;
    private long maxSizeBytes;
    private List<String> allowedExtensions;

    public String getSubUrl() {
      return subDir;
    }

    public void setSubUrl(String subUrl) {
      this.subDir = subUrl;
    }
  }
}
