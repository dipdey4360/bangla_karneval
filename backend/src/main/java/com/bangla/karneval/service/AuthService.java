package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.AdminRegisterRequest;
import com.bangla.karneval.dto.request.ChangePasswordRequest;
import com.bangla.karneval.dto.request.LoginRequest;
import com.bangla.karneval.dto.response.AuthResponse;
import com.bangla.karneval.model.Admin;
import com.bangla.karneval.repository.AdminRepository;
import com.bangla.karneval.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private AdminRepository      adminRepository;
    @Autowired private JwtUtil              jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(admin.getEmail(), admin.getRole());
        return new AuthResponse(token, admin.getEmail(), admin.getName(), admin.getRole());
    }

    public AuthResponse register(AdminRegisterRequest request) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Admin with this email already exists");
        }
        Admin admin = new Admin();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole("ADMIN");
        adminRepository.save(admin);
        String token = jwtUtil.generateToken(admin.getEmail(), admin.getRole());
        return new AuthResponse(token, admin.getEmail(), admin.getName(), admin.getRole());
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New passwords do not match");
        }

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }

}
