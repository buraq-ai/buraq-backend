package com.buraqai.backend.service;

import com.buraqai.backend.dto.LoginRequestDTO;
import com.buraqai.backend.dto.LoginResponseDTO;
import com.buraqai.backend.model.RefreshToken;
import com.buraqai.backend.model.User;
import com.buraqai.backend.repository.RefreshTokenRepository;
import com.buraqai.backend.repository.UserRepository;
import com.buraqai.backend.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if user is active
        if (!user.getActive()) {
            throw new RuntimeException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Save refresh token to database
        refreshTokenRepository.deleteByEmail(user.getEmail());
        RefreshToken refreshTokenEntity = new RefreshToken(
                refreshToken,
                user.getEmail(),
                LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(refreshTokenEntity);

        // Return response
        return new LoginResponseDTO(accessToken, refreshToken, user.getRole());
    }

    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshTokenRepository.delete(token);
    }
}