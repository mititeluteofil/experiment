package com.example.demo.controller;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.register(request));
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return UserResponse.from(userService.findByEmail(principal.getName()));
    }
}
