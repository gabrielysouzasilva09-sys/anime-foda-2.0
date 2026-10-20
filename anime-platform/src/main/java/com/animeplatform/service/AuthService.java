package com.animeplatform.service;

import com.animeplatform.dto.AuthResponse;
import com.animeplatform.dto.LoginRequest;
import com.animeplatform.dto.RegisterRequest;
import com.animeplatform.model.User;
import com.animeplatform.repository.UserRepository;
import com.animeplatform.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registo e autenticação de utilizadores.
 *
 * As passwords são sempre guardadas com hash BCrypt.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Cria um novo utilizador e devolve o token JWT. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("O username '" + username + "' já está em uso");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("O email '" + email + "' já está em uso");
        }

        User user = new User(username, email, passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user.getUsername()), user);
    }

    /** Valida as credenciais e devolve o token JWT. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        return new AuthResponse(jwtService.generateToken(user.getUsername()), user);
    }

    /** Devolve o utilizador autenticado pelo seu username. */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Utilizador não encontrado"));
    }
}
