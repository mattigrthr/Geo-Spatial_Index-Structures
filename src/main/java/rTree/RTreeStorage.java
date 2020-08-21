package rTree;

import common.GeoFence;
import common.Location;
import common.SubscriptionStorage;
import common.Topic;
import org.locationtech.spatial4j.shape.Rectangle;

import java.util.*;
import java.util.stream.Collectors;

public class RTreeStorage implements SubscriptionStorage {
    public RTree rTree;
    HashMap<String, GeoFence> subscriptionGeoFences;

    public RTreeStorage() {
        this.rTree = new RTree();
        this.subscriptionGeoFences = new HashMap<>();
    }

    /**
     *
     * @param subscriptionId Unique identifier for a new subscription
     * @param geoFence Points that are necessary to describe the subscription geofence;
     *               If only one point is given, the geofence is set to a circle with
     *               a radius of 1 km
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    public void addSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        this.subscriptionGeoFences.put(subscriptionId, geoFence);

        Value value = new Value(subscriptionId, geoFence.shape, topicTokens);
        Rectangle mbr = geoFence.getMBR();
        double[] coords = { mbr.getMinX(), mbr.getMinY() };
        double[] dimensions = { mbr.getMaxX() - mbr.getMinX(), mbr.getMaxY() - mbr.getMinY() };

        rTree.insert(coords, dimensions, value);
    }

    /**
     *
     * @param subscriptionId Unique identifier of the subscription that is to be deleted
     * @param geoFence
     * @param topicTokens
     */
    public void deleteSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        this.subscriptionGeoFences.remove(subscriptionId);

        Value value = new Value(subscriptionId, geoFence.shape, topicTokens);
        Rectangle mbr = geoFence.getMBR();
        double[] coords = { mbr.getMinX(), mbr.getMinY() };
        double[] dimensions = { mbr.getMaxX() - mbr.getMinX(), mbr.getMaxY() - mbr.getMinY() };

        rTree.delete(coords, dimensions, value);
    }

    /**
     *
     * @param subscriptionId Unique identifier of the subscription that is to be updated
     * @param updatedGeoFence To be passed if subscription geofence changes
     * @param updatedTokens To be passed if tokens change
     */
    public void updateSubscription(String subscriptionId, GeoFence updatedGeoFence, Topic updatedTokens) {
        GeoFence oldGeoFence = this.subscriptionGeoFences.get(subscriptionId);

        this.subscriptionGeoFences.put(subscriptionId, updatedGeoFence);

        Value value = new Value(subscriptionId, updatedGeoFence.shape, updatedTokens);

        rTree.update(oldGeoFence, updatedGeoFence, value);
    }

    public List<String> getMatchingSubscriptions(Location publisherLocation, Topic messageTokens) {
        List<String> results = new ArrayList<String>();
        Rectangle mbr = publisherLocation.location.getBoundingBox();
        double[] coords = { mbr.getMinX(), mbr.getMinY() };
        double[] dimensions = { mbr.getMaxX() - mbr.getMinX(), mbr.getMaxY() - mbr.getMinY() };
        List<Value> rTreeResults = rTree.search(coords, dimensions);

        for (Value value: rTreeResults) {
            if (this.validateValue(value, publisherLocation, messageTokens)) {
                results.add(value.id);
            }
        }

        return results.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Check if a value matches with the location and tokens of a published message
     * @param value One subscription in a node
     * @param publisherLocation Location of the publisher when publishing the message
     * @param messageTokens Tokens that the message consists of
     * @return
     */
    private boolean validateValue(Value value, Location publisherLocation, Topic messageTokens) {
        if (Topic.validateTopic(value.topicTokens.tokens, messageTokens.tokens)) {
            return value.geoFence.relate(publisherLocation.location).intersects();
        }

        return false;
    }

    @Override
    public String toString() {
        return "Size of RTree: " + this.rTree.getSize();
    }
}
