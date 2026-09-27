package org.ptit.meeting.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

  private final Long userId;
  private final String email;
  private final String code;
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;
  private final boolean enabled;

  public CustomUserDetails(
      Long userId,
      String email,
      String code,
      String password,
      Collection<String> roles,
      boolean active
  ) {
    this(userId, email, code, password, roles, List.of(), active);
  }

  public CustomUserDetails(
      Long userId,
      String email,
      String code,
      String password,
      Collection<String> roles,
      Collection<String> permissions,
      boolean active
  ) {
    this.userId = userId;
    this.email = email;
    this.code = code;
    this.password = password != null ? password : "";

    List<SimpleGrantedAuthority> authList = new ArrayList<>();
    if (roles != null) {
      roles.stream()
          .filter(r -> r != null && !r.isBlank())
          .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
          .map(SimpleGrantedAuthority::new)
          .forEach(authList::add);
    }
    if (permissions != null) {
      permissions.stream()
          .filter(p -> p != null && !p.isBlank())
          .map(SimpleGrantedAuthority::new)
          .forEach(authList::add);
    }
    this.authorities = Collections.unmodifiableList(authList);
    this.enabled = active;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return enabled;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
