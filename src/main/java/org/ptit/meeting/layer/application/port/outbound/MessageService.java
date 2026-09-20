package org.ptit.meeting.layer.application.port.outbound;

public interface MessageService {

  String getMessage(String code, Object... args);

  String getMessage(String code, String language, Object... args);
}
