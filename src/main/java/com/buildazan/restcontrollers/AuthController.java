package com.buildazan.restcontrollers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildazan.config.JwtService;
import com.buildazan.config.UserDetailsImpl;
import com.buildazan.entities.User;
import com.buildazan.enums.MemberShipLevel;
import com.buildazan.enums.SubscriptionStatus;
import com.buildazan.service.UserService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Not Authorized",
                    "redirectTo", "/login"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (!userDetails.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Email not verified",
                    "redirectTo", "/verify-email"));
        } else if (userDetails.getSubscriptionStatus().equals(SubscriptionStatus.NONE) ||
                userDetails.getSubscriptionStatus().equals(SubscriptionStatus.UNPAID) ||
                userDetails.getMemberShipLevel().equals(MemberShipLevel.NONE)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                    "error", "Payment not cleared",
                    "redirectTo", "/pay"));
        }
        return ResponseEntity.ok(Map.of("isAuthorized", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> caredentials,
            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(caredentials.get("username"),
                            caredentials.get("password")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtService.generateJwtToken(userDetailsImpl);
            jwtService.setTokenCookies(response, jwt);
            return ResponseEntity.ok().build();
        } catch (AuthenticationException e) {
            System.out.println(e);
            if (e instanceof BadCredentialsException || e instanceof UsernameNotFoundException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occured during authentication"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> details) {
        try {
            User savedUser = userService.userInitializer(details);
            return ResponseEntity.ok(savedUser);
        } catch (DuplicateKeyException e) {
            System.out.println(e);
            String errorMessage;
            if (e.getMessage().contains("username")) {
                errorMessage = "The username is already taken, please choose a different username.";
            } else if (e.getMessage().contains("email")) {
                errorMessage = "An account with this email already exists. Please use a different email.";
            } else if (e.getMessage().contains("storeName")) {
                errorMessage = "A store with this name already exists. Please choose a different store name.";
            } else if (e.getMessage().contains("subDomain")) {
                errorMessage = "The subdomain is already in use. Please choose a different subdomain.";
            } else if (e.getMessage().contains("customDomain")) {
                errorMessage = "The custom domain is already in use. Please choose a different custom domain.";
            } else {
                errorMessage = "A unique constraint violation occurred.";
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", errorMessage,
                    "details", e.getMessage()));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "An unexpected error occurred during user registration.",
                    "details", e.getMessage()));
        }
    }
}