package com.example.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/appointments")
/** Enforces role-aware booking, consultation, and appointment status workflows. */
class AppointmentController {
    private final UserRepository users; private final AppointmentRepository appointments; private final AvailabilityRepository slots;
    AppointmentController(UserRepository users,AppointmentRepository appointments,AvailabilityRepository slots){this.users=users;this.appointments=appointments;this.slots=slots;}
    record Booking(@NotNull Long availabilityId,@Size(max=2000) String reason) {}
    record ConsultationInput(@Size(max=8000) String advice,@Size(max=4000) String summary) {}
    record StatusInput(AppointmentStatus status) {}
    record AppointmentView(Long id,UserView patient,UserView professional,AvailabilityController.SlotView slot,String status,String reason,String advice,String summary) {
        static AppointmentView of(Appointment a){return new AppointmentView(a.id,UserView.of(a.patient),UserView.of(a.professional),AvailabilityController.SlotView.of(a.availability),a.status.name(),a.reason,a.advice,a.summary);}
    }
    @GetMapping List<AppointmentView> list(HttpServletRequest request){
        AppUser me=CurrentUser.require(request,users); List<Appointment> list;
        if(me.role==Role.ADMIN) list=appointments.findAll(); else if(me.role==Role.PROFESSIONAL) list=appointments.findByProfessionalIdOrderByAvailabilityStartsAtDesc(me.id); else list=appointments.findByPatientIdOrderByAvailabilityStartsAtDesc(me.id);
        return list.stream().map(AppointmentView::of).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional AppointmentView book(@Valid @RequestBody Booking body,HttpServletRequest request){
        AppUser patient=CurrentUser.requireRole(request,users,Role.PATIENT); Availability slot=slots.findById(body.availabilityId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Time slot not found"));
        if(slot.booked||!slot.startsAt.isAfter(java.time.LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This time is no longer available");
        slot.booked=true; return AppointmentView.of(appointments.save(new Appointment(patient,slot.professional,slot,body.reason())));
    }
    @PatchMapping("/{id}/consultation") AppointmentView consultation(@PathVariable Long id,@Valid @RequestBody ConsultationInput body,HttpServletRequest request){
        AppUser me=CurrentUser.requireRole(request,users,Role.PROFESSIONAL); Appointment a=appointments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found"));
        if(!a.professional.id.equals(me.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This appointment belongs to another professional");
        a.advice=body.advice(); a.summary=body.summary(); a.status=AppointmentStatus.COMPLETED; return AppointmentView.of(appointments.save(a));
    }
    @PatchMapping("/{id}/status") AppointmentView status(@PathVariable Long id,@RequestBody StatusInput body,HttpServletRequest request){
        AppUser me=CurrentUser.requireRole(request,users,Role.ADMIN,Role.PROFESSIONAL); Appointment a=appointments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment not found"));
        if(me.role==Role.PROFESSIONAL&&!a.professional.id.equals(me.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This appointment belongs to another professional");
        if(body.status()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an appointment status");
        if(body.status()==AppointmentStatus.BOOKED&&appointments.existsByAvailability_IdAndStatusAndIdNot(a.availability.id,AppointmentStatus.BOOKED,a.id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Another appointment already uses this time");
        a.status=body.status(); if(body.status()==AppointmentStatus.CANCELLED) { a.availability.booked=appointments.existsByAvailability_IdAndStatusAndIdNot(a.availability.id,AppointmentStatus.BOOKED,a.id); slots.save(a.availability); }
        if(body.status()==AppointmentStatus.BOOKED) a.availability.booked=true;
        return AppointmentView.of(appointments.save(a));
    }
}
