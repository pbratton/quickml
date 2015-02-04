package quickml.supervised.alternative.optimizer;

import org.joda.time.DateTime;
import quickml.data.AttributesMap;
import quickml.data.Instance;

import java.io.Serializable;
import java.util.Map;

public class ClassifierInstance implements Instance<AttributesMap, Serializable>, Timestamped {

    private AttributesMap attributes;
    private Serializable label;
    private double weight;
    private DateTime timestamp;

    private ClassifierInstance() {

    }

    public ClassifierInstance(AttributesMap attributes, Serializable label) {
        this(attributes, label, 1.0);
    }

    public ClassifierInstance(AttributesMap attributes, Serializable label, double weight) {
        this.attributes = attributes;
        this.label = label;
        this.weight = weight;
        setTimeStamp();
    }

    @Override
    public AttributesMap getAttributes() {
        return attributes;
    }

    @Override
    public Serializable getLabel() {
        return label;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public DateTime getTimestamp() {
        if (timestamp == null) {
            setTimeStamp();
        }
        return timestamp;
    }

    private void setTimeStamp() {
        //Onespot Specific
        int year = attrVal("timeOfArrival-year");
        int month = attrVal("timeOfArrival-monthOfYear");
        int day = attrVal("timeOfArrival-dayOfMonth");
        int hour = attrVal("timeOfArrival-hourOfDay");
        int minute = attrVal("timeOfArrival-minuteOfHour");
        timestamp = new DateTime(year, month, day, hour, minute, 0, 0);
    }

    private int attrVal(String attrName) {
        return attributes.containsKey(attrName) ? ((Number) attributes.get(attrName)).intValue() : 1 ;
    }
}
