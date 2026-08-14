package com.ino.admin.user;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userDirectory.findUsers(query, page, size);
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
}
