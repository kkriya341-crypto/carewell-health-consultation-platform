package com.example.health;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;

@Configuration
/** Creates sample accounts, appointment slots, and clinic defaults for an empty local database. */
class SeedData {
    @Bean CommandLineRunner seed(UserRepository users, AvailabilityRepository availability, SettingRepository settings, PasswordEncoder encoder) {
        return args -> {
            if (users.count()==0) {
                AppUser admin=users.save(new AppUser("Alex Morgan","admin@carewell.test",encoder.encode("Admin123!"),Role.ADMIN,null));
                AppUser doctor=users.save(new AppUser("Dr. Maya Patel","doctor@carewell.test",encoder.encode("Doctor123!"),Role.PROFESSIONAL,"Family Medicine")); doctor.phone="+1 (555) 010-2030"; users.save(doctor);
                users.save(new AppUser("Jordan Lee","patient@carewell.test",encoder.encode("Patient123!"),Role.PATIENT,null));
                LocalDate monday=LocalDate.now().plusDays(1); while(monday.getDayOfWeek()!=DayOfWeek.MONDAY) monday=monday.plusDays(1);
                for(int d=0;d<5;d++) for(int h=9;h<16;h++) if(h!=12) availability.save(new Availability(doctor,monday.plusDays(d).atTime(h,0),monday.plusDays(d).atTime(h+1,0)));
            }
            if(settings.count()==0) { settings.save(new AppSetting("clinicName","Carewell Health")); settings.save(new AppSetting("appointmentDurationMinutes","60")); settings.save(new AppSetting("bookingWindowDays","60")); }
        };
    }
}
