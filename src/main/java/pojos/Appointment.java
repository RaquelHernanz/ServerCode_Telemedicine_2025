package pojos;

public class Appointment {
    private int id;
    private String date;
    private String type;
    private String state;
    private Doctor doctor;
    private Patient patient;

    public Appointment(String date, String type, String state, Doctor doctor, Patient patient){
        this.date = date;
        this.type = type;
        this.state = state;
        this.doctor = doctor;
        this.patient = patient;
    }
    public Appointment(int id, String date, String type, String state, Doctor doctor, Patient patient){
        this.id = id;
        this.date = date;
        this.type = type;
        this.state = state;
        this.doctor = doctor;
        this.patient = patient;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getDate(){
        return date;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getState(){
        return state;
    }

    public void setState(String state){
        this.state = state;
    }

    public Doctor getDoctor(){
        return doctor;
    }

    public void setDoctor(Doctor doctor){
        this.doctor = doctor;
    }

    public Patient getPatient(){
        return patient;
    }

    public void setPatient(Patient patient){
        this.patient = patient;
    }

    @Override
    public String toString(){
        return "Appointment{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", doctor=" + doctor +
                ", patient=" + patient +
                '}';
    }
}
