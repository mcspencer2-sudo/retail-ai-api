package com.retailai.controller;

import com.retailai.model.LoginRequest;
import com.retailai.model.SignupRequest;
import com.retailai.service.SaaSAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saas/auth")
@CrossOrigin(origins = "*")
public class SaaSAuthController {

    private final SaaSAuthService saasAuthService;

    public SaaSAuthController(SaaSAuthService saasAuthService) {
        this.saasAuthService = saasAuthService;
    }

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {
        try {
            return saasAuthService.signup(request);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return saasAuthService.login(request);
    }
}