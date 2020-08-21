package geoBroker;

import common.GeoFence;
import common.Location;
import common.SubscriptionStorage;
import common.Topic;
import de.hasenburg.geobroker.server.main.Configuration;
import de.hasenburg.geobroker.server.storage.TopicAndGeofenceMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Data structure to manage subscriptions in a BTree
 */
public class GeoBrokerStorage implements SubscriptionStorage {
    public TopicAndGeofenceMapper geoBroker;
    HashMap<ImmutablePair<String, Integer>, de.hasenburg.geobroker.commons.model.spatial.Geofence> subscriptionGeoFences;
    HashMap<ImmutablePair<String, Integer>, de.hasenburg.geobroker.commons.model.message.Topic> subscriptionTopics;

    public GeoBrokerStorage() {
        this.geoBroker = new TopicAndGeofenceMapper(new Configuration(1, 1));
        this.subscriptionGeoFences = new HashMap<>();
        this.subscriptionTopics = new HashMap<>();
    }

    /**
     * Add a subscription to the data storage
     * @param subscriptionId Unique identifier for a new subscription
     * @param geoFence Area which the subscriber is interested in
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    public void addSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        Id id = new Id(subscriptionId);
        de.hasenburg.geobroker.commons.model.message.Topic topic = transformTopic(topicTokens);
        de.hasenburg.geobroker.commons.model.spatial.Geofence geoFenceBroker = transformGeoFence(geoFence);
        this.subscriptionGeoFences.put(id.id, geoFenceBroker);
        this.subscriptionTopics.put(id.id, topic);

        this.geoBroker.putSubscriptionId(id.id, topic, geoFenceBroker);
    }

    /**
     * Does the same as addSubscription but gets parameters already transformed so it does not influence the benchmark
     * @param subscriptionId
     * @param geoFence
     * @param topic
     */
    public void addTransformedSubscription(
            Id subscriptionId,
            de.hasenburg.geobroker.commons.model.spatial.Geofence geoFence,
            de.hasenburg.geobroker.commons.model.message.Topic topic
    ) {
        this.subscriptionGeoFences.put(subscriptionId.id, geoFence);
        this.subscriptionTopics.put(subscriptionId.id, topic);
        this.geoBroker.putSubscriptionId(subscriptionId.id, topic, geoFence);
    }

    /**
     * Delete a subscription from the data storage
     * @param subscriptionId Unique identifier of the subscription that is to be deleted
     * @param geoFence Area which the subscriber is interested in
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    public void deleteSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens) {
        Id id = new Id(subscriptionId);
        this.subscriptionGeoFences.remove(id.id);
        this.subscriptionTopics.remove(id.id);

        de.hasenburg.geobroker.commons.model.message.Topic topic = transformTopic(topicTokens);
        de.hasenburg.geobroker.commons.model.spatial.Geofence geoFenceBroker = transformGeoFence(geoFence);

        this.geoBroker.removeSubscriptionId(id.id, topic, geoFenceBroker);
    }

    /**
     * Does the same as deleteSubscription but gets parameters already transformed so it does not influence the benchmark
     * @param subscriptionId
     * @param geoFence
     * @param topic
     */
    public void deleteTransformedSubscription(
            Id subscriptionId,
            de.hasenburg.geobroker.commons.model.spatial.Geofence geoFence,
            de.hasenburg.geobroker.commons.model.message.Topic topic
    ) {
        this.subscriptionGeoFences.remove(subscriptionId.id);
        this.subscriptionTopics.remove(subscriptionId.id);
        this.geoBroker.removeSubscriptionId(subscriptionId.id, topic, geoFence);
    }

    /**
     * Update a subscription in the data storage
     * @param subscriptionId Unique identifier of the subscription that is to be updated
     * @param updatedGeoFence To be passed if subscription geofence changes
     * @param updatedTokens To be passed if tokens change
     */
    public void updateSubscription(String subscriptionId, GeoFence updatedGeoFence, Topic updatedTokens) {
        Id id = new Id(subscriptionId);
        de.hasenburg.geobroker.commons.model.spatial.Geofence oldGeoFence = this.subscriptionGeoFences.get(id.id);
        de.hasenburg.geobroker.commons.model.message.Topic oldTopic = this.subscriptionTopics.get(id.id);

        this.geoBroker.removeSubscriptionId(id.id, oldTopic, oldGeoFence);

        /**
         * Add updated values
         */
        de.hasenburg.geobroker.commons.model.message.Topic updatedTopicBroker = transformTopic(updatedTokens);
        de.hasenburg.geobroker.commons.model.spatial.Geofence updatedGeoFenceBroker = transformGeoFence(updatedGeoFence);

        this.subscriptionGeoFences.put(id.id, updatedGeoFenceBroker);
        this.subscriptionTopics.put(id.id, updatedTopicBroker);
        this.geoBroker.putSubscriptionId(id.id, updatedTopicBroker, updatedGeoFenceBroker);
    }

    /**
     * Does the same as updateSubscription but gets parameters already transformed so it does not influence the benchmark
     * @param subscriptionId
     * @param updatedGeoFence
     * @param updatedTopic
     */
    public void updateTransformedSubscription(
            Id subscriptionId,
            de.hasenburg.geobroker.commons.model.spatial.Geofence updatedGeoFence,
            de.hasenburg.geobroker.commons.model.message.Topic updatedTopic
    ) {
        de.hasenburg.geobroker.commons.model.spatial.Geofence oldGeoFence = this.subscriptionGeoFences.get(subscriptionId.id);
        de.hasenburg.geobroker.commons.model.message.Topic oldTopic = this.subscriptionTopics.get(subscriptionId.id);

        this.geoBroker.removeSubscriptionId(subscriptionId.id, oldTopic, oldGeoFence);
        this.subscriptionGeoFences.put(subscriptionId.id, updatedGeoFence);
        this.subscriptionTopics.put(subscriptionId.id, updatedTopic);
        this.geoBroker.putSubscriptionId(subscriptionId.id, updatedTopic, updatedGeoFence);
    }

    /**
     * Get ids of subscriptions that match a published message
     * @param publisherLocation Location of the publisher when publishing the message
     * @param messageTokens Tokens the published message consists of
     * @return
     */
    public List<String> getMatchingSubscriptions(Location publisherLocation, Topic messageTokens) {
        de.hasenburg.geobroker.commons.model.message.Topic topic = transformTopic(messageTokens);
        de.hasenburg.geobroker.commons.model.spatial.Location transformedLocation = this.transformLocation(publisherLocation);

        List<String> results = new ArrayList<>();

        List<ImmutablePair<String, Integer>> resultsGeo = this.geoBroker.getPotentialSubscriptionIds(topic, transformedLocation);

        resultsGeo.removeIf(id -> !this.subscriptionGeoFences.get(id).contains(transformedLocation));

        Iterator result = resultsGeo.iterator();

        while (result.hasNext()) {
            ImmutablePair<String, Integer>  idComponents = (ImmutablePair<String, Integer>) result.next();

            results.add(idComponents.left + "_" + idComponents.right);
        }

        return results;
    }

    /**
     * Does the same as getMatchingSubscriptions but objects are already transformed for the GeoBroker
     * @param location
     * @param topic
     * @return
     */
    public List<String> getTransformedMatchingSubscriptions(
        de.hasenburg.geobroker.commons.model.spatial.Location location,
        de.hasenburg.geobroker.commons.model.message.Topic topic
    ) {
        List<String> results = new ArrayList<>();

        List<ImmutablePair<String, Integer>> resultsGeo = this.geoBroker.getPotentialSubscriptionIds(topic, location);

        resultsGeo.removeIf(id -> !this.subscriptionGeoFences.get(id).contains(location));

        Iterator result = resultsGeo.iterator();

        while (result.hasNext()) {
            ImmutablePair<String, Integer>  idComponents = (ImmutablePair<String, Integer>) result.next();

            results.add(idComponents.left + "_" + idComponents.right);
        }

        return results;
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
