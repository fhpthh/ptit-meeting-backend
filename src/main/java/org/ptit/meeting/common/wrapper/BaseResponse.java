package org.ptit.meeting.common.wrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.ptit.meeting.common.constant.StatusCode;
import org.springframework.data.domain.Page;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

  private String code;
  private String strCode;
  private String message;
  private T content;

  public BaseResponse() {
  }

  public BaseResponse(String code, String strCode, String message, T content) {
    this.code = code;
    this.strCode = strCode;
    this.message = message;
    this.content = content;
  }

  public static <T> BaseResponse<T> success(T data) {
    return new BaseResponse<>(
        String.valueOf(StatusCode.SUCCESSFUL.getCode()),
        "success",
        StatusCode.SUCCESSFUL.getMessage(),
        data
    );
  }

  public static <T> BaseResponse<T> success(String message) {
    return new BaseResponse<>(
        String.valueOf(StatusCode.SUCCESSFUL.getCode()),
        "success",
        message,
        null
    );
  }

  public static <T> BaseResponse<T> created(T data) {
    return success(data);
  }

  public static <E> BaseResponse<List<E>> ofPage(Page<E> page) {
    return success(page.getContent());
  }

  public static <T> BaseResponse<T> error(int code, String strCode, String message) {
    return new BaseResponse<>(String.valueOf(code), strCode, message, null);
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getStrCode() {
    return strCode;
  }

  public void setStrCode(String strCode) {
    this.strCode = strCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getContent() {
    return content;
  }

  public void setContent(T content) {
    this.content = content;
  }
}
