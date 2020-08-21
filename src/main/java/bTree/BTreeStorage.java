package bTree;

import common.GeoFence;
import common.Location;
import common.SubscriptionStorage;
import common.Topic;

import java.util.*;

/**
 * Data structure to manage subscriptions in a BTree
 */
public class BTreeStorage implements SubscriptionStorage {
    public BTree bTree;
    HashMap<String, GeoFence> subscriptionGeoFences;

    public BTreeStorage() {
        this.bTree = new BTree();
        this.subscriptionGeoFences = new HashMap<>();
    }

    /**
     * Add a subscription to the data storage
     * @param subscriptionId Unique identifier for a new subscription
     * @param geoFence Area which the subscriber is interested in
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    public void addSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        this.subscriptionGeoFences.put(subscriptionId, geoFence);

        String key = Key.encodeGeoFence(geoFence);
        Value value = new Value(subscriptionId, geoFence.shape, topicTokens);
        HashMap<String, Value> existingValues = (HashMap<String, Value>) this.bTree.get(key);

        if (existingValues == null) {
            existingValues = new HashMap<>();
            existingValues.put(subscriptionId, value);
            this.bTree.put(key, existingValues);
        } else {
            existingValues.put(subscriptionId, value);
            this.bTree.put(key, existingValues);
        }
    }

    /**
     * Delete a subscription from the data storage
     * @param subscriptionId Unique identifier of the subscription that is to be deleted
     * @param geoFence Area which the subscriber is interested in
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    public void deleteSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        this.subscriptionGeoFences.remove(subscriptionId);
        String key = Key.encodeGeoFence(geoFence);
        HashMap<String, Value> existingValues = (HashMap<String, Value>) this.bTree.get(key);

        if (existingValues.size() == 1) {
            this.bTree.put(key, null);
        } else {
            existingValues.remove(subscriptionId);
            this.bTree.put(key, existingValues);
        }
    }

    /**
     * Update a subscription in the data storage
     * @param subscriptionId Unique identifier of the subscription that is to be updated
     * @param updatedGeoFence To be passed if subscription geofence changes
     * @param updatedTokens To be passed if tokens change
     */
    public void updateSubscription(String subscriptionId, GeoFence updatedGeoFence, Topic updatedTokens) {
        GeoFence oldGeoFence = this.subscriptionGeoFences.get(subscriptionId);
        this.subscriptionGeoFences.put(subscriptionId, updatedGeoFence);
        String oldKey = Key.encodeGeoFence(oldGeoFence);
        String newKey = Key.encodeGeoFence(updatedGeoFence);

        if (oldKey.equals(newKey)) {
            HashMap<String, Value> values = (HashMap<String, Value>) this.bTree.get(oldKey);
            Value newValue = new Value(subscriptionId, updatedGeoFence.shape, updatedTokens);

            values.put(subscriptionId, newValue);
            this.bTree.put(newKey, values);
        } else {
            this.deleteSubscription(subscriptionId, oldGeoFence, null);
            this.addSubscription(subscriptionId, updatedGeoFence, updatedTokens);
        }
    }

    /**
     * Get ids of subscriptions that match a published message
     * @param publisherLocation Location of the publisher when publishing the message
     * @param messageTokens Tokens the published message consists of
     * @return
     */
    public List<String> getMatchingSubscriptions(Location publisherLocation, Topic messageTokens) {
        List<String> results = new ArrayList<String>();
        List<String> potentialKeys = Key.potentialKeys(publisherLocation);

        for (String key: potentialKeys) {
            HashMap<String, Value> subResults = (HashMap<String, Value>) this.bTree.get(key);

            if (subResults != null) {
                for (Value value: subResults.values()) {
                    if (this.validateValue(value, publisherLocation, messageTokens)) {
                        results.add(value.id);
                    }
                }
            }
        }

        return results;
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
        return this.bTree.toString();
    }
}
