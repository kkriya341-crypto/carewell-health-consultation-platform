package com.example.health;

import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;

@Configuration
class ApiConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    /** Registers a small servlet endpoint so the project demonstrates the Servlet API directly. */
    @Bean ServletRegistrationBean<PlatformStatusServlet> platformStatusServlet() {
        return new ServletRegistrationBean<>(new PlatformStatusServlet(), "/servlet/status");
    }
    @Bean org.springframework.web.servlet.config.annotation.WebMvcConfigurer corsConfigurer() {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("http://localhost:5173").allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowCredentials(true);
            }
        };
    }
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<Map<String,String>> status(ResponseStatusException ex) { return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", Objects.toString(ex.getReason(),"Request failed"))); }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,String> other(Exception ex) { return Map.of("error", Objects.toString(ex.getMessage(),"Request failed")); }
}

class CurrentUser {
    static AppUser require(HttpServletRequest request, UserRepository users) {
        Object id=request.getSession(false)==null?null:request.getSession(false).getAttribute("userId");
        if (!(id instanceof Long userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in");
        return users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found"));
    }
    static AppUser requireRole(HttpServletRequest request, UserRepository users, Role... roles) {
        AppUser user=require(request,users);
        if (Arrays.stream(roles).noneMatch(role->role==user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to this action");
        return user;
    }
}

record UserView(Long id,String name,String email,String role,String specialty,String phone) {
    static UserView of(AppUser u) { return new UserView(u.id,u.name,u.email,u.role.name(),u.specialty,u.phone); }
}
