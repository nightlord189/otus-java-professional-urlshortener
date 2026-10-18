package org.aburavov.urlshortener.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.aburavov.urlshortener.common.BadRequestException;
import org.aburavov.urlshortener.common.ConflictException;
import org.aburavov.urlshortener.common.NotFoundException;
import org.aburavov.urlshortener.common.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String email, String rawPassword, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        validatePassword(rawPassword);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Page<User> listAll(int page, int size) {
        return userRepository.findAll(Paging.of(page, size, Sort.by("id")));
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    @Transactional
    public User block(long id) {
        User user = getById(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ConflictException("Cannot block an administrator");
        }
        user.setStatus(UserStatus.BLOCKED);
        return user;
    }

    @Transactional
    public User unblock(long id) {
        User user = getById(id);
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new ConflictException("User is not blocked");
        }
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static void validateEmail(String email) {
        if (email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Email is not valid");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + PASSWORD_MIN_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new BadRequestException("Password is too long");
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
