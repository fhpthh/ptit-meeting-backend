package org.ptit.meeting.layer.application.service.base;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.layer.application.port.outbound.MessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

  private final MessageSource messageSource;

  @Override
  public String getMessage(String code, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return resolveMessage(code, locale, args);
  }

  @Override
  public String getMessage(String code, String language, Object... args) {
    Locale locale = (language != null && !language.isBlank())
        ? Locale.forLanguageTag(language)
        : LocaleContextHolder.getLocale();
    return resolveMessage(code, locale, args);
  }

  private String resolveMessage(String code, Locale locale, Object... args) {
    if (code == null || code.isBlank()) {
      return "";
    }
    return messageSource.getMessage(code, args, code, locale);
  }
}