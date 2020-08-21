package common;

import java.util.List;

public interface SubscriptionStorage {
    /**
     * Method to add a subscription to the data structure
     *
     * @param subscriptionId Unique identifier for a new subscription
     * @param geoFence Points that are necessary to describe the subscription geofence;
     *               If only one point is given, the geofence is set to a circle with
     *               a radius of 1 km
     * @param topicTokens Tokens that the subscriber wants to be notified about (conjunctive)
     */
    void addSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens);

    /**
     * Method to remove a subscription from the data structure
     *
     * @param subscriptionId Unique identifier of the subscription that is to be deleted
     */
    void deleteSubscription(String subscriptionId, GeoFence geoFence, Topic topicTokens);

    /**
     * Method to update a subscription in the data structure
     *
     * @param subscriptionId Unique identifier of the subscription that is to be updated
     * @param updatedGeoFence To be passed if subscription geofence changes
     * @param updatedTokens To be passed if tokens change
     */
    void updateSubscription(String subscriptionId, GeoFence updatedGeoFence, Topic updatedTokens);

    /**
     *
     * @param publisherLocation Location of the publisher when publishing the message
     * @param messageTokens Tokens the published message consists of
     * @return
     */
    List<String> getMatchingSubscriptions(Location publisherLocation, Topic messageTokens);
}
