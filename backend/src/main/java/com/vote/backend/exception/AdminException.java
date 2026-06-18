package com.vote.backend.exception;

import org.springframework.http.HttpStatus;

public class AdminException extends DomainException {
  public AdminException(String code, HttpStatus status, String message) {
    super(code, status, message);
  }
}
