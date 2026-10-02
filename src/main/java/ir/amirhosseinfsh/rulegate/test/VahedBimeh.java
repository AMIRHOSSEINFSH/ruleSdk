package ir.amirhosseinfsh.rulegate.test;

import java.time.*;
import java.util.Date;
import java.util.Set;

public class VahedBimeh {
        private String naam;
        private Long id;
        private YESNO yn;
        private LocalDateTime dateTime;
    private LocalDate birthday = LocalDate.now();
    private LocalDateTime dateOfBirth = LocalDateTime.now();
    private LocalTime timeOfBirth = LocalTime.now();
    private Date dateOfDeath = new Date();
    private Instant instant = Instant.now();
    private ZonedDateTime zonedDateTime = ZonedDateTime.now();
        private Set<Address> addressList;

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalTime getTimeOfBirth() {
        return timeOfBirth;
    }

    public void setTimeOfBirth(LocalTime timeOfBirth) {
        this.timeOfBirth = timeOfBirth;
    }

    public Date getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(Date dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

    public YESNO getYn() {
        return yn;
    }

    public void setYn(YESNO yn) {
        this.yn = yn;
    }

        public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getNaam() {
            return naam;
        }

        public void setNaam(String naam) {
            this.naam = naam;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

    public Set<Address> getAddressList() {
        return addressList;
    }

    public void setAddressList(Set<Address> addressList) {
        this.addressList = addressList;
    }
}