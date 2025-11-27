package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.ed.acp.cw2.dtos.PositionDto;
import uk.ac.ed.acp.cw2.dtos.deliveries.DeliveryPathDto;
import uk.ac.ed.acp.cw2.dtos.deliveries.DronePathDto;
import uk.ac.ed.acp.cw2.dtos.deliveries.OverallRouteDto;

public class GeoJsonConverter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode toGeoJson(OverallRouteDto overall) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("type", "FeatureCollection");

    ArrayNode features = objectMapper.createArrayNode();

    for (DronePathDto dronePath : overall.getDronePaths()) {
      int droneId = dronePath.getDroneId();

      for (DeliveryPathDto delivery : dronePath.getDeliveries()) {
        int deliveryId = delivery.getDeliveryId();

        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");

        ObjectNode props = objectMapper.createObjectNode();
        props.put("featureType", "flightPath");
        props.put("droneId", droneId);
        props.put("deliveryId", deliveryId);
        props.put("totalCost", overall.getTotalCost());
        props.put("totalMoves", overall.getTotalMoves());
        feature.set("properties", props);

        ObjectNode geom = objectMapper.createObjectNode();
        geom.put("type", "LineString");

        ArrayNode coords = objectMapper.createArrayNode();
        for (PositionDto pos : delivery.getFlightPath()) {
          ArrayNode coord = objectMapper.createArrayNode();
          coord.add(pos.getLng());
          coord.add(pos.getLat());
          coords.add(coord);
        }
        geom.set("coordinates", coords);

        feature.set("geometry", geom);

        features.add(feature);

        if (!delivery.getFlightPath().isEmpty()) {
          PositionDto start = delivery.getFlightPath().get(0);
          PositionDto end = delivery.getFlightPath().get(delivery.getFlightPath().size() - 1);

          features.add(createPointFeature("start", droneId, deliveryId, start));
          features.add(createPointFeature("end", droneId, deliveryId, end));
        }
      }
    }

    root.set("features", features);
    return root;
  }

  private ObjectNode createPointFeature(String type, int droneId, int deliveryId, PositionDto pos) {
    ObjectNode feature = objectMapper.createObjectNode();
    feature.put("type", "Feature");

    ObjectNode props = objectMapper.createObjectNode();
    props.put("featureType", type);
    props.put("droneId", droneId);
    props.put("deliveryId", deliveryId);
    feature.set("properties", props);

    ObjectNode geom = objectMapper.createObjectNode();
    geom.put("type", "Point");
    ArrayNode coord = objectMapper.createArrayNode();
    coord.add(pos.getLng());
    coord.add(pos.getLat());
    geom.set("coordinates", coord);

    feature.set("geometry", geom);
    return feature;
  }
}
