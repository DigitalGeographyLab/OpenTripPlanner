
# OTP graph to CSV exporter

This procedure can be followed in order to export the internal OSM-based graph object of OTP to CSV files. 

### Prerequisites
- Git, a version control system
- Java Development Kit, preferably version 8 (AKA version 1.8)
- Maven, a build and dependency management system

### Install OTP
```
git clone https://github.com/HSLdevcom/OpenTripPlanner.git
cd OpenTripPlanner
mvn clean package -DskipTests
```

### Create & export graph
1) Download the latest OSM data for HSL area from https://api.digitransit.fi/routing-data/v2/hsl/hsl.pbf
2) Add the OSM data file (hsl.pbf) to ./graphs
3) Run `java -jar -Xmx8G target/otp-1.5.0-SNAPSHOT-shaded.jar --build ./graphs`
4) Run GraphCsvExporter module with args (adjust if needed): `--graph graphs/Graph.obj -d -v --edges_out graphs/edges.csv --nodes_out graphs/nodes.csv`
