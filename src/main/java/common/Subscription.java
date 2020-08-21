package common;

/**
 * Representation of one subscription
 */
public class Subscription {
    public String id;
    public GeoFence geoFence;
    public Topic topic;

    public Subscription (String id, double lat, double lng) {
        this.id = id;
        this.geoFence = new GeoFence(lat, lng, 0.01);
        this.topic = Topic.generateRandomTopic();
    }

    @Override
    public String toString() {
        return "Id: " + this.id + "\nShape: " + this.geoFence + "\nTopic: " + this.topic;
    }
}
