package common;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.*;

public class GeoFence {
    public Shape shape;
    public Location center;

    /**
     * Generates a circular GeoFence with a given radius
     * @param lat
     * @param lng
     * @param radiusDegree
     */
    public GeoFence(double lat, double lng, double radiusDegree) {
        Location center = new Location(lat, lng);
        Circle circle = SpatialContext.GEO.getShapeFactory().circle(center.location, radiusDegree);
        this.center = center;
        this.shape = circle;
    }

    public Rectangle getMBR() {
        return this.shape.getBoundingBox();
    }
}
