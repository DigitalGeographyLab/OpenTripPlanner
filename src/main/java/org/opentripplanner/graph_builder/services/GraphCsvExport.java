package org.opentripplanner.graph_builder.services;

import com.csvreader.CsvWriter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GraphCsvExport {

    public GraphCsvExport() {
    }

    private static Logger LOG = LoggerFactory.getLogger(GraphCsvExport.class);

    public static void exportEdgesToCsv(Graph graph, String filePath) {
        CsvWriter edgeWriter = new CsvWriter(filePath, ';', StandardCharsets.UTF_8);

        LOG.info("Writing edges to file...");
        // Write column names as the first row
        String[] record = {"id_otp", "name_otp", "node_orig_id", "node_dest_id", "length", "edge_class", "street_class", "is_stairs", "is_no_thru_traffic", "permission", "allows_walking", "allows_biking", "traversable_walking", "traversable_biking", "bike_safety_factor", "geometry"};
        try {
            edgeWriter.writeRecord(record);
        } catch (IOException ioe) {
            LOG.error("Exception while creating CSV");
        }
        // Write edges of the graph one by one
        int writeCount = 0;
        for (Edge edge : graph.getEdges()) {
            String id = String.valueOf(edge.getId());
            String name = edge.getName();
            String nodeOrigId = String.valueOf(edge.getFromVertex().getIndex());
            String nodeDestId = String.valueOf(edge.getToVertex().getIndex());
            String length = String.valueOf(edge.getDistance());
            String edgeClass = edge.getClass().getSimpleName();
            String streetClass = "";
            String isStairs = "False";
            String isNoThruTraffic = "False";
            String permission = "";
            String allowsWalking = "True";
            String allowsBiking = "True";
            String traversableWalking = "True";
            String traversableBiking = "True";
            String bikeSafetyFactor = "";
            if (edge instanceof StreetEdge) {
                StreetEdge streetEdge = (StreetEdge) edge;
                streetClass = String.valueOf(streetEdge.getStreetClass());
                isStairs = streetEdge.isStairs() ? "True" : "False";
                isNoThruTraffic = streetEdge.isNoThruTraffic() ? "True" : "False";
                StreetTraversalPermission traversalPermission = streetEdge.getPermission();
                permission = String.valueOf(traversalPermission);
                allowsWalking = traversalPermission.allows(TraverseMode.WALK) ? "True" : "False";
                allowsBiking = traversalPermission.allows(TraverseMode.BICYCLE) ? "True" : "False";
                traversableWalking = streetEdge.canTraverseIncludingBarrier(TraverseMode.WALK) ? "True" : "False";
                traversableBiking = streetEdge.canTraverseIncludingBarrier(TraverseMode.BICYCLE) ? "True" : "False";
                bikeSafetyFactor = String.valueOf(streetEdge.getBicycleSafetyFactor());
            }
            String geometry = String.valueOf(edge.getGeometry());
            // Prepare the record (CSV row) to write
            record = new String[]{id, name, nodeOrigId, nodeDestId, length, edgeClass, streetClass, isStairs, isNoThruTraffic, permission, allowsWalking, allowsBiking, traversableWalking, traversableBiking, bikeSafetyFactor, geometry};
            try {
                edgeWriter.writeRecord(record);
                writeCount += 1;
            } catch (IOException ioe) {
                LOG.error("Exception while writing CSV, exiting");
                System.exit(1);
            }
        }
        LOG.info("Wrote {} edges to file: {}", writeCount, filePath);
        if (writeCount < graph.getEdges().size()) {
            LOG.warn("Did not write all edges to file, missing: {} edges", graph.getEdges().size()-writeCount);
        }
        edgeWriter.close();
    }

    public static void exportNodesToCsv(Graph graph, String filePath) {

        CsvWriter nodeWriter = new CsvWriter(filePath, ';', StandardCharsets.UTF_8);

        LOG.info("Writing nodes to file...");
        // Write column names as the first row
        String[] record = {"id_otp", "name_otp", "vertex_class", "traversable_walking", "traversable_biking", "label", "traffic_light", "free_flowing", "x", "y", "geometry"};
        try {
            nodeWriter.writeRecord(record);
        } catch (IOException ioe) {
            LOG.error("Exception while creating CSV");
        }
        // Write nodes (vertices) of the graph one by one
        int writeCount = 0;
        for (Vertex vertex : graph.getVertices()) {
            // basic attributes
            String id = String.valueOf(vertex.getIndex());
            String vertexClass = vertex.getClass().getSimpleName();
            String trafficLight = "False";
            String freeFlowing = "";
            if (vertex instanceof OsmVertex) {
                OsmVertex osmVertex = (OsmVertex) vertex;
                trafficLight = osmVertex.trafficLight ? "True" : "False";
                freeFlowing = osmVertex.freeFlowing ? "True" : "False";
            }
            // permissions
            String traversableWalking = "True";
            String traversableBiking = "True";
            if (vertex instanceof BarrierVertex) {
                BarrierVertex barrierVertex = (BarrierVertex) vertex;
                traversableWalking = barrierVertex.getBarrierPermissions().allows(TraverseMode.WALK) ? "True" : "False";
                traversableBiking = barrierVertex.getBarrierPermissions().allows(TraverseMode.BICYCLE) ? "True" : "False";
            }
            // geometry
            String coordX = String.valueOf(vertex.getCoordinate().x);
            String coordY = String.valueOf(vertex.getCoordinate().y);
            String geometry = "POINT ("+ coordX + " "+ coordY +")";
            record = new String[]{id, vertex.getName(), vertexClass, traversableWalking, traversableBiking, vertex.getLabel(), trafficLight, freeFlowing, coordX, coordY, geometry};
            try {
                nodeWriter.writeRecord(record);
                writeCount += 1;
            } catch (IOException iow) {
                LOG.error("Exception while writing CSV, exiting");
                System.exit(1);
            }

        }
        LOG.info("Wrote {} nodes to file: {}", writeCount, filePath);
        if (writeCount < graph.getVertices().size()) {
            LOG.warn("Did not write all nodes to file, missing {} nodes", graph.getVertices().size()-writeCount);
        }
        nodeWriter.close();
    }

}
