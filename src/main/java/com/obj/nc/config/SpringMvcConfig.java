package com.obj.nc.config;

import com.obj.nc.security.model.AuthenticationError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.obj.nc.exceptions.PayloadValidationException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@RestControllerAdvice
public class SpringMvcConfig {

    @ExceptionHandler({PayloadValidationException.class})
    ResponseEntity<String> handleMethodArgumentNotValidException(PayloadValidationException e) {
        return new ResponseEntity<>("Request not valid becase of invalid payload: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler({MethodArgumentNotValidException.class})
    ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return new ResponseEntity<>("Request arguments not valid: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler({HttpMessageNotReadableException.class})
    ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return new ResponseEntity<>("Request arguments not valid: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<String> handleAuthenticationException(AuthenticationException e) {
        return new ResponseEntity<>(AuthenticationError.builder().timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .message(e.getMessage()).build().toString(), HttpStatus.UNAUTHORIZED
        );
    }
    
    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler({RuntimeException.class})
    ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return new ResponseEntity<>("Unexpected error ocured: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @Bean
    @Primary
    //there is another objectMapper with different setting used for message serialization. this is used for REST Controllers
    public ObjectMapper objectMapper() {       
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
