package bTree;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import common.GeoFence;
import common.Location;
import org.locationtech.spatial4j.shape.Rectangle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generates a key based on the GeoFence using the Google S2 Geometry library
 */
public class Key {
    static double[] levelBoundaries = new double[]{
            7842000,
            3921000,
            1825000,
            840000,
            432000,
            210000,
            108000,
            54000,
            27000,
            14000,
            7000,
            3000,
            1699,
            850,
            425,
            212,
            106,
            53,
            27,
            13,
            7
    };

    /**
     * Generates a key based on a geofence
     * @param geoFence Shape to describe a field of interest
     * @return
     */
    public static String encodeGeoFence(GeoFence geoFence) {
        S2CellId fullCellId = createCellId(geoFence.center);
        double maxExtent = getMaxExtent(geoFence);
        int level = getLevel(maxExtent);

        return Long.toBinaryString(fullCellId.parent(level).id());
    }

    /**
     * Calls the Google S2 Geometry library functions to generate a key
     * @param location Point somewhere on earth
     * @return
     */
    public static S2CellId createCellId(Location location) {
        S2LatLng latLng = S2LatLng.fromRadians(location.location.getLat(), location.location.getLon());

        return S2CellId.fromLatLng(latLng);
    }

    /**
     *
     * @param location Point somewhere on earth
     * @return
     */
    public static List<String> potentialKeys(Location location) {
        List<String> potentialKeys = new ArrayList<String>();
        S2CellId cellId = createCellId(location);

        for (int i = 20; i > -1; i--) {
            StringBuffer initialKey = new StringBuffer(Long.toBinaryString(cellId.parent(i).id()));

            potentialKeys.add(initialKey.toString());

            List<S2CellId> neighbours = new ArrayList<>();
            cellId.getAllNeighbors(i, neighbours);

            for (S2CellId neighbour: neighbours) {
                List<S2CellId> neighboursNeighbours = new ArrayList<>();
                neighbour.getAllNeighbors(i, neighboursNeighbours);

                for (S2CellId neighbourNeighbour: neighboursNeighbours) {
                    String id = Long.toBinaryString((neighbourNeighbour.id()));

                    if (potentialKeys.indexOf(id) < 0) {
                        potentialKeys.add(id);
                    }
                }
            }
        }

        return new ArrayList<>(new LinkedHashSet<>(potentialKeys));
    }

    /**
     * Calculates the max extent of the GeoFence to determine the level
     * @param geoFence
     * @return
     */
    public static double getMaxExtent(GeoFence geoFence) {
        Rectangle boundingBox = geoFence.getMBR();
        double minLat = boundingBox.getMinY();
        double maxLat = boundingBox.getMaxY();
        double minLon = boundingBox.getMinX();
        double maxLon = boundingBox.getMaxX();
        Location lowerLeft = new Location(minLat, minLon);
        Location upperLeft = new Location(minLat, maxLon);
        Location lowerRight = new Location(maxLat, minLon);

        double maxDistHorizontal = lowerLeft.distInKmToLoc(lowerRight);
        double maxDistVertical = lowerLeft.distInKmToLoc(upperLeft);

        return Math.ceil(Math.max(maxDistHorizontal, maxDistVertical) * 1000);
    }

    /**
     * Determines level of cell based on the max extent of the GeoFence
     * When matching boundary is found we need to go 5 levels higher due to potential location of published messages
     * @param extent
     * @return
     */
    public static int getLevel(double extent) {
        for (int i = 20; i > 5; i--) {
            if (extent > levelBoundaries[i]) {
                continue;
            }

            return i - 5;
        }

        return 0;
    }
}
