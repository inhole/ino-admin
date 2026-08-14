package com.ino.admin.user;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserDirectoryUseCase userDirectory;

    public UserController(UserDirectoryUseCase userDirectory) {
        this.userDirectory = userDirectory;
    }

    @GetMapping
    UserDirectoryUseCase.UserPage findAll(
            @RequestParam(defaultValue = "") @Size(max = 320) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userDirectory.findUsers(query, page, size);
    }
}
