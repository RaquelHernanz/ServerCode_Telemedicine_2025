package pojos;

public class Symptoms {
    private int id;
    private String description;
    private String date_hour;
    private Patient patient;

    public Symptoms(String description, String date_hour, Patient patient){
        this.description = description;
        this.date_hour = date_hour;
        this.patient = patient;
    }

    public Symptoms(int id, String description, String date_hour, Patient patient){
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

    public String getDate_hour(){
        return date_hour;
    }

    public void setDate_hour(String date_hour){
        this.date_hour = date_hour;
    }

    public Patient getPatient(){
        return patient;
    }

    public void setPatient(Patient patient){
        this.patient = patient;
    }

    @Override
    public String toString(){
        return "Symptoms{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", date_hour='" + date_hour + '\'' +
                ", patient=" + patient +
                '}';
    }
}
