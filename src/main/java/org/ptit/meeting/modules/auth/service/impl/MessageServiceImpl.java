
package org.ptit.meeting.modules.auth.service.impl;

import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.service.MessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

  private final MessageSource messageSource;

  @Override
  public String getMessage(String code) {
    return getMessage(code, null, null);
  }

  @Override
  public String getMessage(String code, String defaultMessage) {
    return getMessage(code, null, defaultMessage);
  }

  @Override
  public String getMessage(String code, Map<String, Object> params) {
    return getMessage(code, params, null);
  }

  @Override
  public String getMessage(String code, Map<String, Object> params, String defaultMessage) {
    if (code == null || code.isBlank()) {
      return defaultMessage != null ? defaultMessage : "";
    }

    Locale locale = LocaleContextHolder.getLocale();
    try {
      String template = messageSource.getMessage(code, null, defaultMessage != null ? defaultMessage : code, locale);
      if (params == null || params.isEmpty()) {
        return template;
      }
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        String placeholder = "{" + entry.getKey() + "}";
        String value = entry.getValue() != null ? entry.getValue().toString() : "";
        template = template.replace(placeholder, value);
      }
      return template;
    } catch (Exception e) {
      log.warn("(getMessage) Error resolving message for code: {}", code);
      return defaultMessage != null ? defaultMessage : code;
    }
  }
}
