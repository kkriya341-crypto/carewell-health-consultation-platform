package com.example.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api")
/** Publishes professional directories and manages appointment availability slots. */
class AvailabilityController {
    private final UserRepository users; private final AvailabilityRepository slots;
    AvailabilityController(UserRepository users,AvailabilityRepository slots){this.users=users;this.slots=slots;}
    record SlotInput(@NotNull LocalDateTime startsAt,@NotNull LocalDateTime endsAt) {}
    record ProfessionalView(Long id,String name,String specialty) { static ProfessionalView of(AppUser u){return new ProfessionalView(u.id,u.name,u.specialty);} }
    record SlotView(Long id,Long professionalId,String professionalName,LocalDateTime startsAt,LocalDateTime endsAt,boolean booked) {
        static SlotView of(Availability a){return new SlotView(a.id,a.professional.id,a.professional.name,a.startsAt,a.endsAt,a.booked);}
    }
    @GetMapping("/professionals") List<ProfessionalView> professionals(){return users.findByRole(Role.PROFESSIONAL).stream().map(ProfessionalView::of).toList();}
    @GetMapping("/professionals/{id}/availability") List<SlotView> publicSlots(@PathVariable Long id){return slots.findByProfessionalIdAndBookedFalseOrderByStartsAt(id).stream().filter(s->s.startsAt.isAfter(LocalDateTime.now())).map(SlotView::of).toList();}
    @GetMapping("/availability") List<SlotView> mySlots(HttpServletRequest request){AppUser me=CurrentUser.requireRole(request,users,Role.PROFESSIONAL);return slots.findByProfessionalIdOrderByStartsAt(me.id).stream().map(SlotView::of).toList();}
    @PostMapping("/availability") @ResponseStatus(HttpStatus.CREATED) SlotView add(@Valid @RequestBody SlotInput body,HttpServletRequest request){
        AppUser me=CurrentUser.requireRole(request,users,Role.PROFESSIONAL);
        if(!body.endsAt().isAfter(body.startsAt())||body.startsAt().isBefore(LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a future end time after the start time");
        boolean overlap=slots.findByProfessionalIdOrderByStartsAt(me.id).stream().anyMatch(s->body.startsAt().isBefore(s.endsAt)&&body.endsAt().isAfter(s.startsAt));
        if(overlap) throw new ResponseStatusException(HttpStatus.CONFLICT,"This time overlaps another availability slot");
        return SlotView.of(slots.save(new Availability(me,body.startsAt(),body.endsAt())));
    }
    @DeleteMapping("/availability/{id}") Map<String,String> delete(@PathVariable Long id,HttpServletRequest request){
        AppUser me=CurrentUser.requireRole(request,users,Role.PROFESSIONAL); Availability slot=slots.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Time slot not found"));
        if(!slot.professional.id.equals(me.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This slot belongs to another professional");
        if(slot.booked) throw new ResponseStatusException(HttpStatus.CONFLICT,"Booked appointments cannot be removed"); slots.delete(slot); return Map.of("message","Availability removed");
    }
}
