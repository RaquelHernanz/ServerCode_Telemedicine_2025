package pojos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Symptoms {
    private int id;
    private String description;
    private LocalDateTime date_hour;
    private Patient patient;

    public Symptoms(String description,LocalDateTime data_hour, Patient patient){
        this.description = description;
        this.date_hour = data_hour;
        this.patient = patient;
    }

    public Symptoms(int id, String description, LocalDateTime date_hour, Patient patient){
        this.id = id;
        this.description = description;
        this.date_hour = date_hour;
        this.patient = patient;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public Patient getPatient(){
        return patient;
    }

    public void setPatient(Patient patient){
        this.patient = patient;
    }

    public LocalDateTime getDate_hour() {
        return date_hour;
    }

    public void setDate_hour(LocalDateTime date_hour) {
        this.date_hour = date_hour;
    }

    @Override
    public String toString() {
        return "Symptoms{" +
                "date_hour=" + date_hour +
                ", id=" + id +
                ", description='" + description + '\'' +
                ", patient=" + patient +
                '}';
    }
}
