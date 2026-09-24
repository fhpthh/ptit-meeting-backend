package org.ptit.meeting.layer.application.port.outbound;

public interface PasswordEncoderPort {

  String encode(CharSequence rawPassword);

  boolean matches(CharSequence rawPassword, String encodedPassword);
}
