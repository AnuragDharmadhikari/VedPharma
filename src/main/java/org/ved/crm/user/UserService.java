package org.ved.crm.user;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        return userMapper.toDto(user);
    }

    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    // Gets the currently logged in user from JWT context
    public UserDto getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));

        if (!passwordEncoder.matches(
                request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deactivateUser(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        user.setActive(false);
        userRepository.save(user);
    }
}
