
package org.ptit.meeting.modules.auth.service;

import java.util.Map;

public interface MessageService {

  String getMessage(String code);

  String getMessage(String code, String defaultMessage);

  String getMessage(String code, Map<String, Object> params);

  String getMessage(String code, Map<String, Object> params, String defaultMessage);
}
