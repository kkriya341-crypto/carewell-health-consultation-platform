package com.example.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/admin")
/** Provides admin-only account, settings, appointment analytics, and JDBC reporting endpoints. */
class AdminController {
    private final UserRepository users; private final AppointmentRepository appointments; private final AvailabilityRepository slots; private final RecordRepository records; private final SettingRepository settings; private final PasswordEncoder encoder; private final JdbcTemplate jdbc;
    AdminController(UserRepository users,AppointmentRepository appointments,AvailabilityRepository slots,RecordRepository records,SettingRepository settings,PasswordEncoder encoder,JdbcTemplate jdbc){this.users=users;this.appointments=appointments;this.slots=slots;this.records=records;this.settings=settings;this.encoder=encoder;this.jdbc=jdbc;}
    record UserInput(@NotBlank @Size(max=120) String name,@Email @NotBlank String email,Role role,String specialty,String phone,@Size(max=100) String password) {}
    @GetMapping("/users") List<UserView> listUsers(HttpServletRequest request){CurrentUser.requireRole(request,users,Role.ADMIN);return users.findAll().stream().map(UserView::of).toList();}
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) UserView createUser(@Valid @RequestBody UserInput input,HttpServletRequest request){
        CurrentUser.requireRole(request,users,Role.ADMIN); String email=input.email().trim().toLowerCase();
        if(users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"That email is already in use");
        if(input.role()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a role");
        String raw= input.password()==null||input.password().isBlank()?"Welcome123!":input.password(); if(raw.length()<8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passwords must contain at least 8 characters");
        AppUser u=new AppUser(input.name().trim(),email,encoder.encode(raw),input.role(),input.specialty());u.phone=input.phone();return UserView.of(users.save(u));
    }
    @PutMapping("/users/{id}") UserView updateUser(@PathVariable Long id,@Valid @RequestBody UserInput input,HttpServletRequest request){
        CurrentUser.requireRole(request,users,Role.ADMIN); AppUser u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        users.findByEmailIgnoreCase(input.email().trim()).filter(other->!other.id.equals(id)).ifPresent(other->{throw new ResponseStatusException(HttpStatus.CONFLICT,"That email is already in use");});
        u.name=input.name().trim();u.email=input.email().trim().toLowerCase();if(input.role()!=null)u.role=input.role();u.specialty=input.specialty();u.phone=input.phone();if(input.password()!=null&&!input.password().isBlank()){if(input.password().length()<8)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passwords must contain at least 8 characters");u.passwordHash=encoder.encode(input.password());}
        return UserView.of(users.save(u));
    }
    @DeleteMapping("/users/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteUser(@PathVariable Long id,HttpServletRequest request){
        AppUser admin=CurrentUser.requireRole(request,users,Role.ADMIN);if(admin.id.equals(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot delete your own account");
        AppUser target=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        boolean linked=appointments.findAll().stream().anyMatch(a->a.patient.id.equals(id)||a.professional.id.equals(id))||records.findAll().stream().anyMatch(r->r.patient.id.equals(id));
        if(linked)throw new ResponseStatusException(HttpStatus.CONFLICT,"This account has health history and cannot be deleted");slots.deleteAll(slots.findByProfessionalIdOrderByStartsAt(id));users.delete(target);
    }
    @GetMapping("/settings") Map<String,String> getSettings(HttpServletRequest request){CurrentUser.requireRole(request,users,Role.ADMIN);Map<String,String> map=new TreeMap<>();settings.findAll().forEach(s->map.put(s.settingKey,s.settingValue));return map;}
    @PutMapping("/settings") Map<String,String> updateSettings(@RequestBody Map<String,String> body,HttpServletRequest request){
        CurrentUser.requireRole(request,users,Role.ADMIN);for(String key:List.of("clinicName","appointmentDurationMinutes","bookingWindowDays")){String value=body.get(key);if(value!=null&&!value.isBlank())settings.save(new AppSetting(key,value.trim()));}
        return getSettings(request);
    }
    @GetMapping("/analytics") Map<String,Object> analytics(HttpServletRequest request){
        CurrentUser.requireRole(request,users,Role.ADMIN);
        // These parameterized SQL queries demonstrate direct JDBC integration for admin reporting.
        Long totalUsers=jdbc.queryForObject("SELECT COUNT(*) FROM app_users",Long.class);
        Long patients=jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE role = ?",Long.class,Role.PATIENT.name());
        Long professionals=jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE role = ?",Long.class,Role.PROFESSIONAL.name());
        Long totalAppointments=jdbc.queryForObject("SELECT COUNT(*) FROM appointments",Long.class);
        Long upcoming=jdbc.queryForObject("SELECT COUNT(*) FROM appointments a JOIN availability v ON v.id = a.availability_id WHERE a.status = ? AND v.starts_at > ?",Long.class,AppointmentStatus.BOOKED.name(),java.time.LocalDateTime.now());
        Long completed=jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE status = ?",Long.class,AppointmentStatus.COMPLETED.name());
        return Map.of("users",totalUsers,"patients",patients,"professionals",professionals,"appointments",totalAppointments,"upcoming",upcoming,"completed",completed);
    }
}
