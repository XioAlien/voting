package com.vote.backend.controller;

import com.vote.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ApiException extends DomainException {

  public ApiException(HttpStatus status, String message) {
    this(status.name(), status, message);
  }

  public ApiException(String code, HttpStatus status, String message) {
    super(code, status, message);
  }
}
