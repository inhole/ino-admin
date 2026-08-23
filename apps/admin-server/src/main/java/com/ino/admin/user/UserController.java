package com.ino.admin.user;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import com.ino.admin.identity.api.UserManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserDirectoryUseCase userDirectory;
    private final UserManagementUseCase userManagement;

    public UserController(UserDirectoryUseCase userDirectory, UserManagementUseCase userManagement) {
        this.userDirectory = userDirectory;
        this.userManagement = userManagement;
    }

    @GetMapping
    UserDirectoryUseCase.UserPage findAll(
            @RequestParam(defaultValue = "") @Size(max = 320) String query,
            @RequestParam(defaultValue = "") @Size(max = 50) String role,
            @RequestParam(defaultValue = "")
            @Pattern(regexp = "^$|ACTIVE|LOCKED|DISABLED") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "createdAt|displayName|email|role|status") String sort,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "asc|desc") String direction
    ) {
        return userDirectory.findUsers(new UserQuery(
                query,
                role,
                status,
                page,
                size,
                UserSort.from(sort),
                SortDirection.from(direction)));
    }

    @GetMapping("/{userId}")
    UserDirectoryUseCase.UserSummary findOne(@PathVariable UUID userId) {
        return userDirectory.findUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserManagementUseCase.CreatedUser create(@Valid @RequestBody CreateUserRequest request) {
        return userManagement.create(new UserManagementUseCase.CreateUser(
                request.email(), request.password(), request.displayName(), request.role()));
    }

    record CreateUserRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank String role
    ) {}

    @PatchMapping("/{userId}/status")
    UserManagementUseCase.UpdatedUser changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return userManagement.changeStatus(UUID.fromString(jwt.getSubject()), userId, request.status());
    }

    record ChangeStatusRequest(@NotBlank String status) {}

    @PatchMapping("/{userId}")
    UserManagementUseCase.UpdatedProfile updateProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userManagement.updateProfile(UUID.fromString(jwt.getSubject()), userId,
                new UserManagementUseCase.UpdateProfile(request.displayName(), request.role()));
    }

    record UpdateProfileRequest(@NotBlank @Size(max = 100) String displayName, @NotBlank String role) {}
}
