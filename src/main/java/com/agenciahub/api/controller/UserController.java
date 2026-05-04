package com.agenciahub.api.controller;

import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UpdateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users (Owner only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users (owner only)")
    public List<UserResponse> list() {
        return userService.listAll();
    }

    @GetMapping("/sellers")
    @Operation(summary = "List active sellers — used to populate assignment dropdowns")
    public List<UserResponse> sellers() {
        return userService.listSellers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public UserResponse get(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user (owner or seller)")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user (name, password, active, commission)")
    public UserResponse patch(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
