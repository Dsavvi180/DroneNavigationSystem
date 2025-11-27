package uk.ac.ed.acp.cw2.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.w3c.dom.Node;
import uk.ac.ed.acp.cw2.dtos.*;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import java.util.*;

public class CalculatePositioning {
  public static final double MOVE_DISTANCE = 0.00015;
  private static final double ERROR_TOLERANCE = 1e-12;

  // Calculates Euclidean distance between two positions
  public static double calculateDistance(DistanceDto distanceDto) {
    PositionDto p1 = distanceDto.getPosition1();
    PositionDto p2 = distanceDto.getPosition2();
    double dx = p1.getLng() - p2.getLng();
    double dy = p1.getLat() - p2.getLat();
    return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
  }

  // Checks if the distance between two points is less than MOVE_DISTANCE=0.00015
  public static boolean isCloseTo(DistanceDto distanceDto) {
    return calculateDistance(distanceDto) < MOVE_DISTANCE;
  }

  // Uses trigonometry to calculate the next position from a given start point
  // after a move of distance 0.00015 with a specified angle.
  public static PositionDto nextPosition(NextPositionDto nextPositionDto) {
    double angle = Math.toRadians(nextPositionDto.getAngle());
    double lat = nextPositionDto.getStart().getLat();
    double lng = nextPositionDto.getStart().getLng();
    double dy = Math.sin(angle) * MOVE_DISTANCE; // Change in latitude from move
    double dx = Math.cos(angle) * MOVE_DISTANCE; // Change in longitude from move
    return new PositionDto(lng + dx, lat + dy);
  }

  public static PositionDto nextPositionMagnified(NextPositionDto nextPositionDto) {
    double angle = Math.toRadians(nextPositionDto.getAngle());
    double lat = nextPositionDto.getStart().getLat();
    double lng = nextPositionDto.getStart().getLng();
    double dy = Math.sin(angle) * 0.001; // Change in latitude from move
    double dx = Math.cos(angle) * 0.001; // Change in longitude from move
    return new PositionDto(lng + dx, lat + dy);
  }

  // Checks if a point is inside a polygon region defined by a list of vertices
  public static boolean isInRegion(RegionCheckDto regionCheckDto) {
    List<PositionDto> vertices = regionCheckDto.getRegion().getVertices();
    PositionDto checkPoint = regionCheckDto.getPosition();
    PositionDto firstPoint = vertices.getFirst();
    PositionDto lastPoint = vertices.getLast();
    // Equality check of first and last point with tolerance error level
    boolean areEqual = firstPoint.equals(lastPoint);
    // Raise 400 bad request if region is not closed. i.e. if the first point does not equal the
    // last.
    if (!areEqual) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    return checkPath2D(checkPoint, firstPoint, vertices);
  }

  // Helper function of isInRegion that constructs a polygon with Path2D library method and checks
  // if point is inside the polygon
  private static boolean checkPath2D(
      PositionDto checkPoint, PositionDto firstPoint, List<PositionDto> vertices) {
    Path2D polygon = new Path2D.Double();
    polygon.moveTo(firstPoint.getLng(), firstPoint.getLat());

    for (PositionDto point : vertices) {
      if (!point.equals(firstPoint)) {
        polygon.lineTo(point.getLng(), point.getLat());
      }
    }
    polygon.closePath();
    Point2D point = new Point2D.Double(checkPoint.getLng(), checkPoint.getLat());

    // returns true if the point is inside the shape or if the point lies on the boundary of the
    // shape
    // The boundary check is done by detecting overlap between a very small square around the point
    // and the polygon (intersects(Rectangle2D r) method)
    return polygon.contains(point)
        || polygon.intersects(
            checkPoint.getLng() - ERROR_TOLERANCE,
            checkPoint.getLat() - ERROR_TOLERANCE,
            ERROR_TOLERANCE * 2,
            ERROR_TOLERANCE * 2);
  }
}
