package com.vote.backend.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends DomainException {
  public AuthException(String code, HttpStatus status, String message) {
    super(code, status, message);
  }
}
