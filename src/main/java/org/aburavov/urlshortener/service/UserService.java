package org.aburavov.urlshortener.service;

import org.aburavov.urlshortener.model.User;
import org.aburavov.urlshortener.model.UserRole;
import org.aburavov.urlshortener.model.UserStatus;
import org.aburavov.urlshortener.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String email, String password, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        email = email.trim().toLowerCase();
        if (email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is not valid");
        }
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 8-64 characters");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(password), role));
        log.info("created user {} with role {}", user.getEmail(), role);
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public User getById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Page<User> findAll(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size between 1 and 100");
        }
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
    }

    @Transactional
    public User block(long id) {
        User user = getById(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin cannot be blocked");
        }
        user.setStatus(UserStatus.BLOCKED);
        return user;
    }

    @Transactional
    public User unblock(long id) {
        User user = getById(id);
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not blocked");
        }
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
