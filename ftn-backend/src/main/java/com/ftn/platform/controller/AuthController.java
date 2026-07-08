package com.ftn.platform.controller;

import com.ftn.platform.dto.LoginRequestDTO;
import com.ftn.platform.dto.LoginResponseDTO;
import com.ftn.platform.entity.User;
import com.ftn.platform.exception.UnauthorizedException;
import com.ftn.platform.repository.UserRepository;
import com.ftn.platform.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = tokenService.createToken(user.getUsername(), user.getRole());
        
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenService.removeToken(token);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Not authenticated");
        }
        String token = authHeader.substring(7);
        if (!tokenService.validateToken(token)) {
            throw new UnauthorizedException("Session expired or invalid");
        }
        TokenService.SessionInfo sessionInfo = tokenService.getSession(token);
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .username(sessionInfo.getUsername())
                .role(sessionInfo.getRole())
                .build();
        return ResponseEntity.ok(response);
    }
}
