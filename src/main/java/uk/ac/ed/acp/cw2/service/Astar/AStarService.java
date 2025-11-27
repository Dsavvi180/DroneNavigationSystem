package uk.ac.ed.acp.cw2.service.Astar;

import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.clients.MedSupplyDronesClient;
import uk.ac.ed.acp.cw2.dtos.*;
import uk.ac.ed.acp.cw2.service.CalculatePositioning;

import java.util.*;

@Service
public class AStarService {

  private static final double MOVE_DISTANCE = 0.00015;

  private static final double HEURISTIC_WEIGHT = 1.2;

  private static final double SPEED_UP_DISTANCE_THRESHOLD = 0.3;

  private MedSupplyDronesClient medSupplyDronesClient;

  public AStarService(MedSupplyDronesClient medSupplyDronesClient) {
    this.medSupplyDronesClient = medSupplyDronesClient;
  }

  public AStarResult findPathForLeg(
      PositionDto start, PositionDto goal, int maxMovesAvailable, DroneDto drone) {

    List<RestrictedRegionDto> restrictedRegions = medSupplyDronesClient.getRestrictedRegions();

    List<RegionDto> regionShapes =
        restrictedRegions.stream()
            .map(
                region ->
                    new RegionDto(
                        region.getName(),
                        region.getVertices().stream()
                            .map(loc -> new PositionDto(loc.getLng(), loc.getLat()))
                            .toList()))
            .toList();

    double distanceStartToGoal =
        CalculatePositioning.calculateDistance(new DistanceDto(start, goal));

    PriorityQueue<AStarNode> openSet =
        new PriorityQueue<>(Comparator.comparingDouble(AStarNode::getFCost));

    Map<PositionDto, AStarNode> bestForPosition = new HashMap<>();

    AStarNode startNode = new AStarNode();
    startNode.setPosition(start);
    startNode.setGCost(0.0);
    startNode.setHCost(heuristic(start, goal, drone));
    startNode.setFCost(startNode.getGCost() + HEURISTIC_WEIGHT * startNode.getHCost());
    startNode.setMovesUsed(0);
    startNode.setParent(null);

    openSet.add(startNode);
    bestForPosition.put(start, startNode);

    double costPerMove = drone.getCapability().getCostPerMove();
    int maxPossibleMoves = maxMovesAvailable;

    while (!openSet.isEmpty()) {
      AStarNode current = openSet.poll();

      AStarNode bestKnown = bestForPosition.get(current.getPosition());
      if (bestKnown != current && bestKnown != null && bestKnown.getGCost() <= current.getGCost()) {
        continue;
      }

      if (positionsEqual(current.getPosition(), goal)) {
        current.setPosition(goal);
        return new AStarResult(
            reconstructPath(current), current.getMovesUsed(), current.getGCost(), true);
      }

      int optimisticRemainingMoves = estimateMovesBetween(current.getPosition(), goal);
      if (current.getMovesUsed() + optimisticRemainingMoves > maxPossibleMoves) {
        continue;
      }

      double distanceToGoal =
          CalculatePositioning.calculateDistance(new DistanceDto(current.getPosition(), goal));
      boolean useMagnifiedStep =
          distanceStartToGoal > 0
              && (distanceToGoal > SPEED_UP_DISTANCE_THRESHOLD * distanceStartToGoal);
      useMagnifiedStep = false;

      for (PositionDto neighbourPos : getNeighbours(current.getPosition(), useMagnifiedStep)) {

        int tentativeMovesUsed = current.getMovesUsed() + 1;

        if (tentativeMovesUsed > maxPossibleMoves) {
          continue;
        }

        boolean isInRestrictedArea =
            regionShapes.stream()
                .anyMatch(
                    regionDto ->
                        CalculatePositioning.isInRegion(
                            new RegionCheckDto(neighbourPos, regionDto)));
        if (isInRestrictedArea) {
          continue;
        }

        double tentativeG = current.getGCost() + costPerMove;

        AStarNode existing = bestForPosition.get(neighbourPos);

        if (existing != null && existing.getGCost() <= tentativeG) {
          continue;
        }

        AStarNode neighbourNode = (existing != null) ? existing : new AStarNode();

        neighbourNode.setPosition(neighbourPos);
        neighbourNode.setGCost(tentativeG);
        neighbourNode.setHCost(heuristic(neighbourPos, goal, drone));
        neighbourNode.setFCost(
            neighbourNode.getGCost() + HEURISTIC_WEIGHT * neighbourNode.getHCost());
        neighbourNode.setMovesUsed(tentativeMovesUsed);
        neighbourNode.setParent(current);

        bestForPosition.put(neighbourPos, neighbourNode);
        openSet.add(neighbourNode);
      }
    }

    return new AStarResult(Collections.emptyList(), 0, Double.POSITIVE_INFINITY, false);
  }

  private double heuristic(PositionDto from, PositionDto to, DroneDto drone) {
    double distDegrees = euclideanDistance(from, to);
    double stepsNeeded = distDegrees / MOVE_DISTANCE;
    return stepsNeeded * drone.getCapability().getCostPerMove();
  }

  private double euclideanDistance(PositionDto p1, PositionDto p2) {
    DistanceDto distanceDto = new DistanceDto(p1, p2);
    return CalculatePositioning.calculateDistance(distanceDto);
  }

  private int estimateMovesBetween(PositionDto from, PositionDto to) {
    double distance = euclideanDistance(from, to);
    return (int) Math.ceil(distance / MOVE_DISTANCE);
  }

  private List<PositionDto> getNeighbours(PositionDto position, boolean magnified) {
    List<PositionDto> neighbours = new ArrayList<>();
    for (double angle = 0; angle < 360; angle += 22.5) {
      PositionDto neighbour =
          magnified
              ? CalculatePositioning.nextPositionMagnified(new NextPositionDto(position, angle))
              : CalculatePositioning.nextPosition(new NextPositionDto(position, angle));
      neighbours.add(neighbour);
    }
    return neighbours;
  }

  // Reconstructs path from goal position back to start
  private List<PositionDto> reconstructPath(AStarNode goalNode) {
    List<PositionDto> path = new ArrayList<>();
    AStarNode current = goalNode;
    while (current != null) {
      path.add(0, current.getPosition());
      current = current.getParent();
    }
    return path;
  }

  private boolean positionsEqual(PositionDto p1, PositionDto p2) {
    return CalculatePositioning.isCloseTo(new DistanceDto(p1, p2));
  }
}
