package com.panha.markmekh.service;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.entity.RefreshToken;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.RefreshTokenRepository;
import com.panha.markmekh.repository.UserRepository;
import com.panha.markmekh.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final com.panha.markmekh.repository.EmployeeRepository employeeRepository;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       com.panha.markmekh.repository.EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public void register(com.panha.markmekh.dto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new BadRequestException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email already registered");

        com.panha.markmekh.entity.User user = com.panha.markmekh.entity.User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(com.panha.markmekh.entity.Role.EMPLOYEE)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        userRepository.save(user);

        // ⚡ Defensive: check for existing employee record
        employeeRepository.findByUser_Id(user.getId()).ifPresent(e -> {
            throw new BadRequestException("Employee profile already exists for this user");
        });

        long count = employeeRepository.count() + 1;
        String code = String.format("EMP%04d", count);

        com.panha.markmekh.entity.Employee emp = com.panha.markmekh.entity.Employee.builder()
                .user(user)
                .employeeCode(code)
                .fullNameKh(req.getFullNameKh())
                .fullNameEn(req.getFullNameEn())
                .phoneNumber(req.getPhoneNumber())
                .employmentType("FULL_TIME")
                .employmentStatus("ACTIVE")
                .startDate(java.time.LocalDate.now())
                .annualLeaveBalance(18)
                .build();
        employeeRepository.save(emp);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!user.isEnabled()) throw new BadRequestException("Account is disabled");
        if (!user.isAccountNonLocked()) throw new BadRequestException("Account is locked");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (Exception ex) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) user.setAccountNonLocked(false);
            userRepository.save(user);
            throw new BadRequestException("Invalid username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .user(user)
                .expirationTime(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(rt);
        return token;
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (rt.isRevoked()) throw new BadRequestException("Refresh token revoked");
        if (rt.getExpirationTime().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Refresh token expired");

        User user = rt.getUser();
        String newAccess = jwtService.generateToken(user);
        String newRefresh = createRefreshToken(user);

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser_Id(userId);
    }

    /**
     * Verify username/password without issuing a JWT.
     * Used when an employee scans a QR session and confirms their identity.
     * Throws BadRequestException if invalid.
     */
    public User verifyCredentials(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!user.isEnabled()) throw new BadRequestException("Account is disabled");
        if (!user.isAccountNonLocked()) throw new BadRequestException("Account is locked");

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new BadRequestException("Invalid username or password");

        return user;
    }
}