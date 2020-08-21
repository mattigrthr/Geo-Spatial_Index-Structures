package common;

/**
 * Representation of one csv entry
 */
public class Message {
    public String thingId;
    public String tupleNr;
    public int startTimeOffset;
    public double lat;
    public double lng;

    public Message(String thingId, String tupleNr, String startTimeOffset, String lat, String lng) {
        this.thingId = thingId;
        this.tupleNr = tupleNr;
        this.startTimeOffset = Integer.parseInt(startTimeOffset);
        this.lat = Double.parseDouble(lat);
        this.lng = Double.parseDouble(lng);
    }

    @Override
    public String toString() {
        return "ThingId: " + this.thingId + "\nTupleNr: " + this.tupleNr + "\nStartTimeOffset: " + this.startTimeOffset + "\nLatitude: " + this.lat + "\nLongitude: " + this.lng;
    }
}
