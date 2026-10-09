package com.example.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
/** Handles login, patient registration, session lookup, and patient profile updates. */
class AuthController {
    private final UserRepository users; private final PasswordEncoder encoder;
    AuthController(UserRepository users, PasswordEncoder encoder) { this.users=users; this.encoder=encoder; }
    record Login(@Email @NotBlank String email,@NotBlank String password) {}
    record Registration(@NotBlank @Size(max=120) String name,@Email @NotBlank String email,@NotBlank @Size(min=8,max=100) String password,@Size(max=40) String phone) {}
    record ProfileInput(@NotBlank @Size(max=120) String name,@Email @NotBlank String email,@Size(max=40) String phone) {}
    @PostMapping("/login") UserView login(@Valid @RequestBody Login body,HttpServletRequest request) {
        AppUser user=users.findByEmailIgnoreCase(body.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"));
        if(!encoder.matches(body.password(),user.passwordHash)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        request.getSession(true).setAttribute("userId",user.id); return UserView.of(user);
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) UserView register(@Valid @RequestBody Registration body,HttpServletRequest request) {
        String email=body.email().trim().toLowerCase();
        if(users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
        AppUser user=new AppUser(body.name().trim(),email,encoder.encode(body.password()),Role.PATIENT,null); user.phone=body.phone(); users.save(user);
        request.getSession(true).setAttribute("userId",user.id); return UserView.of(user);
    }
    @PutMapping("/profile") UserView updateProfile(@Valid @RequestBody ProfileInput body,HttpServletRequest request) {
        AppUser user=CurrentUser.requireRole(request,users,Role.PATIENT); String email=body.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email).filter(other->!other.id.equals(user.id)).ifPresent(other->{throw new ResponseStatusException(HttpStatus.CONFLICT,"That email is already in use");});
        user.name=body.name().trim();user.email=email;user.phone=body.phone();return UserView.of(users.save(user));
    }
    @GetMapping("/me") UserView me(HttpServletRequest request) { return UserView.of(CurrentUser.require(request,users)); }
    @PostMapping("/logout") Map<String,String> logout(HttpServletRequest request) { if(request.getSession(false)!=null) request.getSession(false).invalidate(); return Map.of("message","Signed out"); }
}
