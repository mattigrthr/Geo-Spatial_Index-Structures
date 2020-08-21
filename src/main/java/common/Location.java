package common;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

import static org.locationtech.spatial4j.distance.DistanceUtils.DEG_TO_KM;

public class Location {
    public Point location;

    public Location(double lat, double lng) {
        this.location = SpatialContext.GEO.getShapeFactory().pointLatLon(lat, lng);
    }

    /**
     * Returns the distance in km to another point
     * @param otherLocation
     * @return
     */
    public double distInKmToLoc(Location otherLocation) {
        double distanceInRadians = SpatialContext.GEO.getDistCalc().distance(this.location, otherLocation.location);

        return distanceInRadians * DEG_TO_KM;
    }

    @Override
    public String toString() {
        return "Lat: " + this.location.getY() + ", Lng: " + this.location.getX();
    }
}
