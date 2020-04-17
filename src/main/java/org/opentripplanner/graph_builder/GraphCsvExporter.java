package org.opentripplanner.graph_builder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.csvreader.CsvWriter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class GraphCsvExporter {

    private static final Logger LOG = LoggerFactory.getLogger(GraphCsvExporter.class);

    @Parameter(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose = false;

    @Parameter(names = {"-d", "--debug"}, description = "Debug mode")
    private boolean debug = false;

    @Parameter(names = {"-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    @Parameter(names = {"-g", "--graph"}, description = "path to the graph file", required = true)
    private String graphPath;

    @Parameter(names = {"-eo", "--edges_out"}, description = "output file for edges")
    private String outPathEdges;

    @Parameter(names = {"-no", "--nodes_out"}, description = "output file for nodes")
    private String outPathNodes;
    
    private JCommander jc;

    private Graph graph;

    private CsvWriter edgeWriter;

    private CsvWriter nodeWriter;

    public static void main(String[] args) {
        GraphCsvExporter graphCsvExporter = new GraphCsvExporter(args);
        graphCsvExporter.run();
    }

    private GraphCsvExporter(String[] args) {
        jc = new JCommander(this);
        try {
            jc.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }
    }

    private void run() {

        /* open input graph (same for all commands) */
        File graphFile = new File(graphPath);
        try {
            graph = Graph.load(graphFile);
        } catch (Exception e) {
            LOG.error("Exception while loading graph from " + graphFile);
            return;
        }

        /* open output stream (same for all commands) */
        if (outPathEdges != null && outPathNodes != null) {
            try {
                edgeWriter = new CsvWriter(outPathEdges, ';', Charset.forName("UTF8"));
                nodeWriter = new CsvWriter(outPathNodes, ';', Charset.forName("UTF8"));
            } catch (Exception e) {
                LOG.error("Exception while opening output file {}, exiting", outPathEdges);
                System.exit(1);
                return;
            }
        } else {
            LOG.error("No output files set for nodes or edges");
        }
        LOG.info("done loading graph.");

        printEdgeStats();
        exportEdgesToCsv();
        edgeWriter.close();

        printNodeStats();
        exportNodesToCsv();
        nodeWriter.close();
    }

    public void printEdgeStats() {

        LOG.info("Count edges by type...");
        Map<String, Integer> classCounts = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            String edgeClass = edge.getClass().getSimpleName();
            int count = classCounts.containsKey(edgeClass) ? classCounts.get(edgeClass) : 0;
            classCounts.put(edgeClass, count + 1);
        }
        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            LOG.info(entry.getKey() + " count: " + entry.getValue());
        }

        LOG.info("Count edges by permission...");
        Map<String, Integer> permissionCounts = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            if (edge instanceof StreetEdge || edge instanceof StreetWithElevationEdge) {
                StreetEdge streetEdge = (StreetEdge) edge;
                String permission = String.valueOf(streetEdge.getPermission());
                int count = permissionCounts.containsKey(permission) ? permissionCounts.get(permission) : 0;
                permissionCounts.put(permission, count + 1);
            }
        }
        for (Map.Entry<String, Integer> entry : permissionCounts.entrySet()) {
            LOG.info(entry.getKey() + " count: " + entry.getValue());
        }

    }

    public void exportEdgesToCsv() {
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
        LOG.info("Wrote {} edges to file: {}", writeCount, outPathEdges);
        if (writeCount < graph.getEdges().size()) {
            LOG.warn("Did not write all edges to file, missing: {} edges", graph.getEdges().size()-writeCount);
        }
    }

    public void printNodeStats() {
        LOG.info("Count nodes by type...");
        Map<String, Integer> classCounts = new HashMap<>();
        for (Vertex vertex : graph.getVertices()) {
            String vertexClass = vertex.getClass().getSimpleName();
            int count = classCounts.containsKey(vertexClass) ? classCounts.get(vertexClass) : 0;
            classCounts.put(vertexClass, count + 1);
        }
        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            LOG.info(entry.getKey() + " count: " + entry.getValue());
        }
    }

    public void exportNodesToCsv() {
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
        LOG.info("Wrote {} nodes to file: {}", writeCount, outPathNodes);
        if (writeCount < graph.getVertices().size()) {
            LOG.warn("Did not write all nodes to file, missing {} nodes", graph.getVertices().size()-writeCount);
        }
    }
}
