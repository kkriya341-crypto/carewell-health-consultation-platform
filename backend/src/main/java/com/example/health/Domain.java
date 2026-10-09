package com.example.health;

import jakarta.persistence.*;
import java.time.LocalDateTime;

enum Role { ADMIN, PROFESSIONAL, PATIENT }
enum AppointmentStatus { BOOKED, COMPLETED, CANCELLED }

@Entity @Table(name="app_users")
class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) String name;
    @Column(nullable=false, unique=true) String email;
    @Column(nullable=false) String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Role role;
    String specialty;
    String phone;
    AppUser() {}
    AppUser(String name, String email, String passwordHash, Role role, String specialty) { this.name=name; this.email=email; this.passwordHash=passwordHash; this.role=role; this.specialty=specialty; }
}

@Entity @Table(name="availability")
class Availability {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) AppUser professional;
    @Column(nullable=false) LocalDateTime startsAt;
    @Column(nullable=false) LocalDateTime endsAt;
    boolean booked=false;
    Availability() {}
    Availability(AppUser professional, LocalDateTime startsAt, LocalDateTime endsAt) { this.professional=professional; this.startsAt=startsAt; this.endsAt=endsAt; }
}

@Entity @Table(name="appointments")
class Appointment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) AppUser patient;
    @ManyToOne(optional=false) AppUser professional;
    @ManyToOne(optional=false) Availability availability;
    @Enumerated(EnumType.STRING) @Column(nullable=false) AppointmentStatus status=AppointmentStatus.BOOKED;
    @Column(length=2000) String reason;
    @Column(length=8000) String advice;
    @Column(length=4000) String summary;
    LocalDateTime createdAt=LocalDateTime.now();
    Appointment() {}
    Appointment(AppUser patient, AppUser professional, Availability availability, String reason) { this.patient=patient; this.professional=professional; this.availability=availability; this.reason=reason; }
}

@Entity @Table(name="medical_records")
class MedicalRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) AppUser patient;
    @ManyToOne AppUser createdBy;
    @Column(nullable=false) String title;
    @Column(length=5000) String details;
    LocalDateTime updatedAt=LocalDateTime.now();
    MedicalRecord() {}
    MedicalRecord(AppUser patient, AppUser createdBy, String title, String details) { this.patient=patient; this.createdBy=createdBy; this.title=title; this.details=details; }
}

@Entity @Table(name="app_settings")
class AppSetting {
    @Id String settingKey;
    @Column(length=2000) String settingValue;
    AppSetting() {}
    AppSetting(String key, String value) { settingKey=key; settingValue=value; }
}
