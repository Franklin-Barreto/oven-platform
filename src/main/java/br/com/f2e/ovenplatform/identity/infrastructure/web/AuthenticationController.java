package br.com.f2e.ovenplatform.identity.infrastructure.web;

import br.com.f2e.ovenplatform.identity.application.AuthService;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

  private final AuthService authService;

  public AuthenticationController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(
        new LoginResponse(authService.login(request.email(), request.password())));
  }
}
