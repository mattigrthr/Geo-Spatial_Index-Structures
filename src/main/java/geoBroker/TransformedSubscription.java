package geoBroker;

import common.GeoFence;
import common.Location;
import common.Topic;

public class TransformedSubscription {
    public Id id;
    public de.hasenburg.geobroker.commons.model.message.Topic topic;
    public de.hasenburg.geobroker.commons.model.spatial.Geofence geoFence;

    public TransformedSubscription(String subscriptionId, GeoFence geoFence, Topic topic) {
        this.id = new Id(subscriptionId);
        this.topic = transformTopic(topic);
        this.geoFence = transformGeoFence(geoFence);
    }

    private de.hasenburg.geobroker.commons.model.message.Topic transformTopic(Topic topicTokens) {
        return new de.hasenburg.geobroker.commons.model.message.Topic(topicTokens.tokenString);
    }

    private de.hasenburg.geobroker.commons.model.spatial.Geofence transformGeoFence(GeoFence geoFence) {
        de.hasenburg.geobroker.commons.model.spatial.Location center = this.transformLocation(geoFence.center);

        return de.hasenburg.geobroker.commons.model.spatial.Geofence.circle(center,0.01);
    }

    private de.hasenburg.geobroker.commons.model.spatial.Location transformLocation(Location location) {
        double lat = location.location.getLat();
        double lng = location.location.getLon();

        return new de.hasenburg.geobroker.commons.model.spatial.Location(lat, lng);
    }
}
