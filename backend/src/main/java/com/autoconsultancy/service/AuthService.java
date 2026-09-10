package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.LoginRequest;
import com.autoconsultancy.dto.request.RegisterRequest;
import com.autoconsultancy.dto.response.AuthResponse;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.entity.Role;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.CustomerRepository;
import com.autoconsultancy.repository.UserRepository;
import com.autoconsultancy.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException | DisabledException e) {
            throw new UnauthorizedException("Invalid email or password");
        } catch (Exception e) {
            throw new UnauthorizedException("Authentication failed");
        }
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
                
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .profileComplete(false)
                .build();
        customerRepository.save(customer);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
