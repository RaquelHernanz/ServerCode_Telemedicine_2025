package pojos;

import java.util.List;

public class Measurement {
    private int id;
    private Type type;
    private List<Integer> values;
    private String date;
    private Patient patient;

    public Measurement(Type type, List<Integer> values, String date, Patient patient){
        this.type = type;
        this.values = values;
        this.date = date;
        this.patient = patient;
    }
    public Measurement(int id, Type type, List<Integer> values, String date, Patient patient){
        this.id = id;
        this.type = type;
        this.values = values;
        this.date = date;
        this.patient = patient;
    }
    public enum Type{
        ECG, EDA
    }
}
