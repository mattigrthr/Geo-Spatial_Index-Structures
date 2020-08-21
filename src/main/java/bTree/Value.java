package bTree;

import common.Topic;
import org.locationtech.spatial4j.shape.Shape;

/**
 * A value in a BTree node
 */
public class Value {
    String id;
    Shape geoFence;
    Topic topicTokens;

    public Value(String id, Shape geoFence, Topic topicTokens) {
        this.id = id;
        this.geoFence = geoFence;
        this.topicTokens = topicTokens;
    }

    @Override
    public String toString() {
        return "Id: " + this.id + ", GeoFence: " + geoFence + ", Topic: " + topicTokens;
    }
}
