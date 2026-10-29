package com.eec.bookhub.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<?> bad(RuntimeException e){ return ResponseEntity.badRequest().body(Map.of("ok",false,"message",e.getMessage())); }
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<?> unauthorized(SecurityException e){ return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok",false,"message",e.getMessage())); }
}
