package com.vote.backend.exception;

import org.springframework.http.HttpStatus;

public class VoteException extends DomainException {
  public VoteException(String code, HttpStatus status, String message) {
    super(code, status, message);
  }
}
