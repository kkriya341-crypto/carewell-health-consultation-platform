package com.example.health;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

/** Spring Data repositories provide CRUD access to the H2-backed domain entities. */
interface UserRepository extends JpaRepository<AppUser,Long> { Optional<AppUser> findByEmailIgnoreCase(String email); List<AppUser> findByRole(Role role); }
interface AvailabilityRepository extends JpaRepository<Availability,Long> { List<Availability> findByProfessionalIdAndBookedFalseOrderByStartsAt(Long id); List<Availability> findByProfessionalIdOrderByStartsAt(Long id); }
interface AppointmentRepository extends JpaRepository<Appointment,Long> { List<Appointment> findByPatientIdOrderByAvailabilityStartsAtDesc(Long id); List<Appointment> findByProfessionalIdOrderByAvailabilityStartsAtDesc(Long id); boolean existsByAvailability_IdAndStatusAndIdNot(Long availabilityId,AppointmentStatus status,Long appointmentId); }
interface RecordRepository extends JpaRepository<MedicalRecord,Long> { List<MedicalRecord> findByPatientIdOrderByUpdatedAtDesc(Long id); }
interface SettingRepository extends JpaRepository<AppSetting,String> {}
