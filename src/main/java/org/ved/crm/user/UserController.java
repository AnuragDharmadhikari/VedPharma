package org.ved.crm.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ved.crm.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully",
                        userService.getAllUsers()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        return ResponseEntity.ok(
                ApiResponse.success("Current user retrieved successfully",
                        userService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully",
                        userService.getUserById(id)));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByRole(
            @PathVariable Role role) {
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully",
                        userService.getUsersByRole(role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully",
                        userService.updateUser(id, request)));
    }

    @PatchMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully"));
    }
}