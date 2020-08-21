package rTree;

import common.GeoFence;
import org.locationtech.spatial4j.shape.Rectangle;

import java.util.*;

/**
 * Copied from https://github.com/simon04/rtree/blob/master/src/com/newbrightidea/util/RTree.java
 *
 * Implementation of an arbitrary-dimension RTree.
 * Based on R-Trees: A Dynamic Index Structure for Spatial Searching
 * (Antonn Guttmann, 1984)
 *
 * This class is not thread-safe.
 *
 * Copyright 2010 Russ Weeks rweeks@newbrightidea.com
 * Licensed under the GNU LGPL
 * License details here: http://www.gnu.org/licenses/lgpl-3.0.txt
 */
public class RTree
{
    private int internalNodeCounter;
    private HashMap<String, Node> leafNodeMap;
    private final int maxEntries;
    private final int minEntries;
    private final int numDims;
    private double averageXDims;
    private double averageYDims;
    private final double epsilon;

    private Node root;

    private volatile int size;

    /**
     * Creates a new RTree.
     * @param maxEntries maximum number of entries per node
     * @param minEntries minimum number of entries per node (except for the root node)
     * @param numDims the number of dimensions of the RTree.
     */
    public RTree (int maxEntries, int minEntries, int numDims, double epsilon) {
        assert( minEntries <= (maxEntries/2) );
        this.numDims = numDims;
        this.maxEntries = maxEntries;
        this.minEntries = minEntries;
        root = buildRoot(true);
        this.internalNodeCounter = 0;
        this.leafNodeMap = new HashMap<>();
        this.averageXDims = 0;
        this.averageYDims = 0;
        this.epsilon = epsilon;
    }

    private Node buildRoot(boolean asLeaf) {
        double[] initCoords = new double[numDims];
        double[] initDimensions = new double[numDims];

        for ( int i = 0; i < this.numDims; i++ ) {
            initCoords[i] = Math.sqrt(Double.MAX_VALUE);
            initDimensions[i] = -2.0f * Math.sqrt(Double.MAX_VALUE);
        }

        return new Node(initCoords, initDimensions, asLeaf);
    }

    /**
     * Builds a new RTree using default parameters:
     * maximum 50 entries per node
     * minimum 2 entries per node
     * 2 dimensions
     */
    public RTree() {
        this(50, 2, 2, 0.03);
    }

    /**
     * @return the maximum number of entries per node
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * @return the minimum number of entries per node for all nodes except the root.
     */
    public int getMinEntries() {
        return minEntries;
    }

    /**
     * @return the number of dimensions of the tree
     */
    public int getNumDims() {
        return numDims;
    }

    /**
     * Searches the RTree for objects overlapping with the given rectangle.
     * @param coords the corner of the rectangle that is the lower bound of
     * every dimension (eg. the top-left corner)
     * @param dimensions the dimensions of the rectangle.
     * @return a list of objects whose rectangles overlap with the given rectangle.
     */
    public List<Value> search(double[] coords, double[] dimensions) {
        assert(coords.length == numDims);
        assert(dimensions.length == numDims);
        LinkedList<Value> results = new LinkedList<>();
        search(coords, dimensions, root, results);
        return results;
    }

    private void search(double[] coords, double[] dimensions, Node n, LinkedList<Value> results) {
        if ( n.leaf ) {
            for ( Node e: n.children ) {
                if ( isOverlap(coords, dimensions, e.coords, e.dimensions) ) {
                    if (e.id != null) {
                        results.add(((Entry)e).entry);
                    } else {
                        // Adjusted because root is marked as leaf in the beginning but loses that status
                        e.leaf = false;

                        for ( Node c: e.children) {
                            if (isOverlap(coords, dimensions, c.coords, c.dimensions)) {
                                search(coords, dimensions, c, results);
                            }
                        }
                    }
                }
            }
        } else {
            for ( Node c : n.children ) {
                if ( isOverlap(coords, dimensions, c.coords, c.dimensions) ) {
                    search(coords, dimensions, c, results);
                }
            }
        }
    }

    /**
     * Deletes the entry associated with the given rectangle from the RTree
     * @param coords the corner of the rectangle that is the lower bound in
     * every dimension
     * @param dimensions the dimensions of the rectangle
     * @param entry the entry to delete
     * @return true iff the entry was deleted from the RTree.
     */
    public boolean delete(double[] coords, double[] dimensions, Value entry) {
        updatedAverageDims(false, dimensions[0], dimensions[1]);

        assert(coords.length == numDims);
        assert(dimensions.length == numDims);
        Node l = findLeaf(root, coords, dimensions, entry);

        // If value has been already deleted
        if (l == null) {
            return false;
        }

        assert(l.leaf);
        ListIterator<Node> li = l.children.listIterator();
        Entry toRemove = null;

        while (li.hasNext()) {
            @SuppressWarnings("unchecked")
            Entry e = (Entry)li.next();
            if ( e.entry.equals(entry) )
            {
                toRemove = e;
                break;
            }
        }

        if ( toRemove != null ) {
            condenseTree(toRemove);
            size--;
        }

        return (toRemove != null);
    }

    private Node findLeaf(Node n, double[] coords, double[] dimensions, Value entry) {
        if ( n.leaf ) {
            for (Node c: n.children)
            {
                if (((Entry)c).entry.equals(entry))
                {
                    return n;
                }
            }
            return null;
        } else {
            for ( Node c: n.children ) {
                if (isOverlap(c.coords, c.dimensions, coords, dimensions))
                {
                    Node result = findLeaf(c, coords, dimensions, entry);
                    if ( result != null )
                    {
                        return result;
                    }
                }
            }

            return null;
        }
    }

    private void condenseTree(Node n) {
        Set<Node> q = new HashSet<Node>();

        while ( n != root ) {
            if ( n.leaf && (n.children.size() < minEntries)) {
                q.addAll(n.children);
                n.parent.children.remove(n);
            } else if (!n.leaf && (n.children.size() < minEntries)) {
                // probably a more efficient way to do this...
                LinkedList<Node> toVisit = new LinkedList<Node>(n.children);
                while (!toVisit.isEmpty())
                {
                    Node c = toVisit.pop();
                    if ( c.leaf )
                    {
                        q.addAll(c.children);
                    }
                    else
                    {
                        toVisit.addAll(c.children);
                    }
                }
                n.parent.children.remove(n);
            } else {
                tighten(n);
            }

            n = n.parent;
        }

        for (Node ne: q) {
            @SuppressWarnings("unchecked")
            Entry e = (Entry)ne;
            insert(e.coords, e.dimensions, e.entry);
        }
    }

    /**
     * Inserts the given entry into the RTree, associated with the given
     * rectangle.
     * @param coords the corner of the rectangle that is the lower bound in
     * every dimension
     * @param dimensions the dimensions of the rectangle
     * @param entry the entry to insert
     */
    public void insert(double[] coords, double[] dimensions, Value entry) {
        size++;

        assert(coords.length == numDims);
        assert(dimensions.length == numDims);
        Entry e = new Entry(coords, dimensions, entry);
        Node l = chooseLeaf(root, e);
        l.children.add(e);
        e.parent = l;

        // Adjusted code
        updatedAverageDims(true, dimensions[0], dimensions[1]);

        if (e.id != null) {
            leafNodeMap.put(e.id, e);
        }

        if ( l.children.size() > maxEntries ) {
            Node[] splits = splitNode(l);

            if (splits[0].children.size() == maxEntries) {
                splits[0].full = true;
            }

            if (splits[1].children.size() == maxEntries) {
                splits[1].full = true;
            }

            adjustTree(splits[0], splits[1]);
        } else {
            // Adjusted code
            if (l.children.size() == maxEntries) {
                l.full = true;
            }

            adjustTree(l, null);
        }
    }

    /**
     *
     * @param oldGeoFence
     * @param updatedGeoFence
     * @param entry
     */
    public void update(GeoFence oldGeoFence, GeoFence updatedGeoFence, Value entry) {
        Rectangle oldMbr = oldGeoFence.getMBR();
        double[] oldCoords = { oldMbr.getMinX(), oldMbr.getMinY() };
        double[] oldDimensions = { oldMbr.getMaxX() - oldMbr.getMinX(), oldMbr.getMaxY() - oldMbr.getMinY() };

        Rectangle updatedMbr = updatedGeoFence.getMBR();
        double[] updatedCoords = { updatedMbr.getMinX(), updatedMbr.getMinY() };
        double[] updatedDimensions = { updatedMbr.getMaxX() - updatedMbr.getMinX(), updatedMbr.getMaxY() - updatedMbr.getMinY() };

        Node leaf = leafNodeMap.get(entry.id);

        if (
            (Math.abs(updatedDimensions[0] - oldDimensions[0]) <= (this.averageXDims * this.epsilon)) &&
            (Math.abs(updatedDimensions[1] - oldDimensions[1]) <= (this.averageYDims * this.epsilon))
        ) {
            enlargeNode(leaf, updatedCoords, updatedDimensions);
        } else {
            // Find suitable sibling
            if (leaf.parent != null) {
                for (Node sibling: leaf.parent.children) {
                    if (!sibling.full && isContained(sibling.coords, sibling.dimensions, updatedCoords, updatedDimensions)) {
                        leaf.parent = sibling.parent;
                        sibling.children.add(leaf);

                        return;
                    }
                }
            }

            // Find suitable sibling going higher
            if (leaf.parent != null && leaf.parent.parent != null) {
                for (Node parentSibling: leaf.parent.parent.children) {
                    for (Node sibling: parentSibling.children) {
                        if (!sibling.full && isContained(sibling.coords, sibling.dimensions, updatedCoords, updatedDimensions)) {
                            leaf.parent = sibling.parent;
                            sibling.children.add(leaf);

                            return;
                        }
                    }
                }
            }

            // Find suitable sibling going higher
            if (leaf.parent != null && leaf.parent.parent != null && leaf.parent.parent.parent != null) {
                for (Node parentParentSibling: leaf.parent.parent.parent.children) {
                    for (Node parentSibling: parentParentSibling.children) {
                        for (Node sibling: parentSibling.children) {
                            if (!sibling.full && isContained(sibling.coords, sibling.dimensions, updatedCoords, updatedDimensions)) {
                                leaf.parent = sibling.parent;
                                sibling.children.add(leaf);

                                return;
                            }
                        }
                    }
                }
            }

            delete(oldCoords, oldDimensions, ((Entry)leaf).entry);
            insert(updatedCoords, updatedDimensions, ((Entry)leaf).entry);
        }
    }

    private void enlargeNode(Node node, double[] updatedCoords, double[] updatedDimensions) {
        Node parent = node.parent;

        if (isContained(parent.coords, parent.dimensions, updatedCoords, updatedDimensions)) {
            node.coords[0] = updatedCoords[0];
            node.coords[1] = updatedCoords[1];
            node.dimensions[0] = updatedDimensions[0];
            node.dimensions[1] = updatedDimensions[1];
        }
    }

    private void adjustTree(Node n, Node nn) {
        if ( n == root ) {
            if ( nn != null ) {
                // build new root and add children.
                root = buildRoot(false);
                root.children.add(n);
                n.parent = root;
                root.children.add(nn);
                nn.parent = root;
            }

            tighten(root);
            return;
        }

        tighten(n);

        if ( nn != null ) {
            tighten(nn);

            if ( n.parent.children.size() > maxEntries ) {
                Node[] splits = splitNode(n.parent);
                adjustTree(splits[0], splits[1]);
            }
        } else if ( n.parent != null ) {
            adjustTree(n.parent, null);
        }
    }

    private Node[] splitNode(Node n) {
        @SuppressWarnings("unchecked")
        Node[] nn = new RTree.Node[] {n, new Node(n.coords, n.dimensions, n.leaf)};
        nn[1].parent = n.parent;
        if ( nn[1].parent != null )
        {
            nn[1].parent.children.add(nn[1]);
        }
        LinkedList<Node> cc = new LinkedList<Node>(n.children);
        n.children.clear();
        Node[] ss = pickSeeds(cc);
        nn[0].children.add(ss[0]);
        nn[1].children.add(ss[1]);

        while ( !cc.isEmpty() ) {
            if ( (nn[0].children.size() >= minEntries) &&
                    (nn[1].children.size() + cc.size() == minEntries) )
            {
                nn[1].children.addAll(cc);
                cc.clear();
                return nn;
            }
            else if ( (nn[1].children.size() >= minEntries) &&
                    (nn[1].children.size() + cc.size() == minEntries) )
            {
                nn[0].children.addAll(cc);
                cc.clear();
                return nn;
            }
            Node c = cc.pop();
            Node preferred;
            // Implementation of linear PickNext
            double e0 = getRequiredExpansion(nn[0].coords, nn[0].dimensions, c);
            double e1 = getRequiredExpansion(nn[1].coords, nn[1].dimensions, c);
            if ( e0 < e1 )
            {
                preferred = nn[0];
            }
            else if (e0 > e1 )
            {
                preferred = nn[1];
            }
            else
            {
                double a0 = getArea(nn[0].dimensions);
                double a1 = getArea(nn[1].dimensions);
                if ( a0 < a1 )
                {
                    preferred = nn[0];
                }
                else if (e0 > a1)
                {
                    preferred = nn[1];
                }
                else
                {
                    if ( nn[0].children.size() < nn[1].children.size() )
                    {
                        preferred = nn[0];
                    }
                    else if ( nn[0].children.size() > nn[1].children.size() )
                    {
                        preferred = nn[1];
                    }
                    else
                    {
                        preferred = nn[(int)Math.round(Math.random())];
                    }
                }
            }
            preferred.children.add(c);
        }
        tighten(nn[0]);
        tighten(nn[1]);

        return nn;
    }

    // Implementation of LinearPickSeeds
    private RTree.Node[] pickSeeds(LinkedList<Node> nn) {
        RTree.Node[] bestPair = null;
        double bestSep = 0.0f;

        for ( int i = 0; i < numDims; i++ )
        {
            double dimLb = Double.MAX_VALUE, dimMinUb = Double.MAX_VALUE;
            double dimUb = -1.0f * Double.MAX_VALUE, dimMaxLb = -1.0f * Double.MAX_VALUE;
            Node nMaxLb = null, nMinUb = null;
            for ( Node n: nn )
            {
                if ( n.coords[i] < dimLb )
                {
                    dimLb = n.coords[i];
                }
                if ( n.dimensions[i] + n.coords[i] > dimUb )
                {
                    dimUb = n.dimensions[i] + n.coords[i];
                }
                if ( n.coords[i] > dimMaxLb )
                {
                    dimMaxLb = n.coords[i];
                    nMaxLb = n;
                }
                if ( n.dimensions[i] + n.coords[i] < dimMinUb )
                {
                    dimMinUb = n.dimensions[i] + n.coords[i];
                    nMinUb = n;
                }
            }
            double sep = Math.abs((dimMinUb - dimMaxLb) / (dimUb - dimLb));
            if ( sep >= bestSep )
            {
                bestPair = new RTree.Node[] { nMaxLb, nMinUb };
                bestSep = sep;
            }
        }
        nn.remove(bestPair[0]);
        nn.remove(bestPair[1]);
        return bestPair;
    }

    private void tighten(Node n) {
        double[] minCoords = new double[n.coords.length];
        double[] maxDimensions = new double[n.dimensions.length];

        for (int i = 0; i < minCoords.length; i++ ) {
            minCoords[i] = Double.MAX_VALUE;
            maxDimensions[i] = 0.0f;

            for (Node c: n.children) {
                // we may have bulk-added a bunch of children to a node (eg. in splitNode)
                // so here we just enforce the child->parent relationship.
                c.parent = n;
                if (c.coords[i] < minCoords[i]) {
                    minCoords[i] = c.coords[i];
                }

                if ((c.coords[i] + c.dimensions[i]) > maxDimensions[i]) {
                    maxDimensions[i] = (c.coords[i] + c.dimensions[i]);
                }
            }
        }
        System.arraycopy(minCoords, 0, n.coords, 0, minCoords.length);
        System.arraycopy(maxDimensions, 0, n.dimensions, 0, maxDimensions.length);
    }

    private RTree.Node chooseLeaf(RTree.Node n, RTree.Entry e) {
        if ( n.leaf )
        {
            return n;
        }
        double minInc = Double.MAX_VALUE;
        Node next = null;
        for ( RTree.Node c: n.children )
        {
            double inc = getRequiredExpansion( c.coords, c.dimensions, e );
            if ( inc < minInc )
            {
                minInc = inc;
                next = c;
            }
            else if ( inc == minInc )
            {
                double curArea = 1.0f;
                double thisArea = 1.0f;
                for ( int i = 0; i < c.dimensions.length; i++ )
                {
                    curArea *= next.dimensions[i];
                    thisArea *= c.dimensions[i];
                }
                if ( thisArea < curArea )
                {
                    next = c;
                }
            }
        }
        return chooseLeaf(next, e);
    }

    /**
     * Returns the increase in area necessary for the given rectangle to cover the given entry.
     */
    private double getRequiredExpansion( double[] coords, double[] dimensions, Node e ) {
        double area = getArea(dimensions);
        double[] deltas = new double[dimensions.length];
        for ( int i = 0; i < deltas.length; i++ )
        {
            if ( coords[i] + dimensions[i] < e.coords[i] + e.dimensions[i] )
            {
                deltas[i] = e.coords[i] + e.dimensions[i] - coords[i] - dimensions[i];
            }
            else if ( coords[i] + dimensions[i] > e.coords[i] + e.dimensions[i] )
            {
                deltas[i] = coords[i] - e.coords[i];
            }
        }
        double expanded = 1.0f;
        for ( int i = 0; i < dimensions.length; i++ )
        {
            area *= dimensions[i] + deltas[i];
        }
        return (expanded - area);
    }

    private double getArea(double[] dimensions) {
        double area = 1.0f;
        for ( int i = 0; i < dimensions.length; i++ )
        {
            area *= dimensions[i];
        }
        return area;
    }

    private boolean isOverlap( double[] scoords, double[] sdimensions, double[] coords, double[] dimensions ) {
        for ( int i = 0; i < scoords.length; i++ )
        {
            boolean overlapInThisDimension = false;
            if ( scoords[i] == coords[i] )
            {
                overlapInThisDimension = true;
            }
            else if (scoords[i] < coords[i])
            {
                if (scoords[i] + sdimensions[i] >= coords[i])
                {
                    overlapInThisDimension = true;
                }
            }
            else if (scoords[i] > coords[i])
            {
                if ( coords[i] + dimensions[i] >= scoords[i] )
                {
                    overlapInThisDimension = true;
                }
            }
            if ( !overlapInThisDimension )
            {
                return false;
            }
        }
        return true;
    }

    private boolean isContained(double[] outCoords, double[] outDims, double[] inCoords, double[] inDims) {
        if (inCoords[0] < outCoords[0]) {
            return false;
        } else if (inCoords[1] < outCoords[1]) {
            return false;
        } else if ((inCoords[0] + inDims[0]) > (outCoords[0] + outDims[0])) {
            return false;
        } else if ((inCoords[1] + inDims[1]) > (outCoords[1] + outDims[1])) {
            return false;
        } else {
            return true;
        }
    }

    private class Node {
        final double[] coords;
        final double[] dimensions;
        final LinkedList<Node> children;
        boolean leaf;
        String id;
        boolean full;

        Node parent;

        private Node(double[] coords, double[] dimensions, boolean leaf) {
            this.coords = new double[coords.length];
            this.dimensions = new double[dimensions.length];
            System.arraycopy(coords, 0, this.coords, 0, coords.length);
            System.arraycopy(dimensions, 0, this.dimensions, 0, dimensions.length);
            this.leaf = leaf;

            // Adjusted code
            if (leaf) {
                this.id = null;
                this.full = false;
            } else {
                this.id = "internalNode" + internalNodeCounter;
                internalNodeCounter += 1;
            }

            children = new LinkedList<>();
        }
    }

    private class Entry extends Node {
        final Value entry;

        public Entry(double[] coords, double[] dimensions, Value entry)
        {
            // an entry isn't actually a leaf (its parent is a leaf)
            // but all the algorithms should stop at the first leaf they encounter,
            // so this little hack shouldn't be a problem.
            super(coords, dimensions, true);

            this.id = entry.id;
            this.entry = entry;
        }
    }

    private void updatedAverageDims (boolean adding, double x, double y) {
        double numberOfLeafs = this.leafNodeMap.size();

        if (adding) {
            this.averageXDims = (this.averageXDims * numberOfLeafs + x) / (numberOfLeafs + 1);
            this.averageYDims = (this.averageYDims * numberOfLeafs + y) / (numberOfLeafs + 1);
        } else {
            this.averageXDims = (this.averageXDims * numberOfLeafs - x) / (numberOfLeafs - 1);
            this.averageYDims = (this.averageYDims * numberOfLeafs - y) / (numberOfLeafs - 1);
        }
    }


    public void clear() {
        root = buildRoot(true);
        // let the GC take care of the rest.
    }

    public int getSize() {
        return this.size;
    }
}