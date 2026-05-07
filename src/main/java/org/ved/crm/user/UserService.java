package org.ved.crm.user;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
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

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<UserDto> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public UserDto getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        return userMapper.toDto(user);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public UserDto getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
        return userMapper.toDto(user);
    }

    @PreAuthorize("hasRole('OWNER') or @userSecurity.isOwner(#id, authentication)")
    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));

        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot update a deactivated user: " + user.getFullName());
        }

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        userRepository.save(user);
        return userMapper.toDto(userRepository.findById(id).orElseThrow());
    }

    @PreAuthorize("hasRole('OWNER') or @userSecurity.isOwner(#id, authentication)")
    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));

        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot change password for a deactivated user: " + user.getFullName());
        }

        if (!passwordEncoder.matches(
                request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword()));
    }

    @Audited(action = "USER_DEACTIVATED", entityType = "User")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public void deactivateUser(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        user.setActive(false);
        userRepository.save(user);
    }
}
