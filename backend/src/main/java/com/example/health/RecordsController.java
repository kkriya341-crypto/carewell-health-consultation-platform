package com.example.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@RestController @RequestMapping("/api/records")
/** Restricts patient health record access to the patient and their care professionals. */
class RecordsController {
    private final UserRepository users; private final RecordRepository records; private final AppointmentRepository appointments;
    RecordsController(UserRepository users,RecordRepository records,AppointmentRepository appointments){this.users=users;this.records=records;this.appointments=appointments;}
    record RecordInput(@NotBlank @Size(max=180) String title,@Size(max=5000) String details) {}
    record RecordView(Long id,Long patientId,String patientName,String title,String details,LocalDateTime updatedAt,String author) {
        static RecordView of(MedicalRecord r){return new RecordView(r.id,r.patient.id,r.patient.name,r.title,r.details,r.updatedAt,r.createdBy==null?"Patient":r.createdBy.name);}
    }
    @GetMapping List<RecordView> list(@RequestParam(required=false) Long patientId,HttpServletRequest request){
        AppUser me=CurrentUser.require(request,users); AppUser patient=resolvePatient(me,patientId);
        return records.findByPatientIdOrderByUpdatedAtDesc(patient.id).stream().map(RecordView::of).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) RecordView create(@RequestParam(required=false) Long patientId,@Valid @RequestBody RecordInput body,HttpServletRequest request){
        AppUser me=CurrentUser.require(request,users); AppUser patient=resolvePatient(me,patientId);
        return RecordView.of(records.save(new MedicalRecord(patient,me,body.title().trim(),body.details())));
    }
    private AppUser resolvePatient(AppUser me,Long patientId){
        if(me.role==Role.PATIENT){if(patientId!=null&&!patientId.equals(me.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only access your own records"); return me;}
        if(me.role!=Role.PROFESSIONAL) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to patient records");
        if(patientId==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select a patient");
        boolean consulted=appointments.findByProfessionalIdOrderByAvailabilityStartsAtDesc(me.id).stream().anyMatch(a->a.patient.id.equals(patientId)&&a.status!=AppointmentStatus.CANCELLED);
        if(!consulted) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Patient records are available after a consultation is booked");
        return users.findById(patientId).filter(u->u.role==Role.PATIENT).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient not found"));
    }
}
