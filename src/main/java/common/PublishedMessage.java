package common;

/**
 * Representation of one csv entry
 */
public class PublishedMessage {
    public Location location;
    public Topic topic;

    public PublishedMessage(double lat, double lng) {
        this.location = new Location(lat, lng);
        this.topic = Topic.generateRandomTopic();
    }

    @Override
    public String toString() {
        return "[Location: { " + this.location + " }, Topic: " + this.topic.tokenString + "]";
    }
}
