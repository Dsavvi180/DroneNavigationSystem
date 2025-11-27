package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.clients.MedSupplyDronesClient;
import uk.ac.ed.acp.cw2.dtos.*;
import uk.ac.ed.acp.cw2.dtos.deliveries.DeliveryPathDto;
import uk.ac.ed.acp.cw2.dtos.deliveries.DronePathDto;
import uk.ac.ed.acp.cw2.dtos.deliveries.OverallRouteDto;
import uk.ac.ed.acp.cw2.service.Astar.AStarResult;
import uk.ac.ed.acp.cw2.service.Astar.AStarService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DynamicQueries {
  private final MedSupplyDronesClient medSupplyDronesClient;
  private final StaticQueries staticQueries;
  private final AStarService aStarService;
  private AtomicInteger deliveryId = new AtomicInteger(0);

  public DynamicQueries(
      MedSupplyDronesClient medSupplyDronesClient,
      StaticQueries staticQueries,
      AStarService aStarService) {
    this.medSupplyDronesClient = medSupplyDronesClient;
    this.staticQueries = staticQueries;
    this.aStarService = aStarService;
  }

  // Finds drones that match the capability passed in the payload to the
  // queryAsPath/attribute-name/attribute-value endpoint
  public List<DroneDto> findDronesWithCapability(String capabilityName, String capabilityValue) {
    ObjectMapper objectMapper = new ObjectMapper(); // converts Dto to map object
    return medSupplyDronesClient.getAllDrones().stream()
        .filter(
            drone -> {
              Object value =
                  objectMapper
                      .convertValue(drone.getCapability(), Map.class)
                      .get(capabilityName); // if the Map object i.e. the mapped drone dto doesn't
              // have the capability as its key, null is returned
              if (value instanceof Double) {
                try {
                  double capValue = Double.parseDouble(capabilityValue);
                  return Double.compare((Double) value, capValue) == 0;
                } catch (NumberFormatException error) {
                  return false;
                }
              } else if (value instanceof Integer) {
                try {
                  Integer capValue = Integer.parseInt(capabilityValue);
                  return capValue.equals(value);
                } catch (NumberFormatException error) {
                  return false;
                }
              } else if (value instanceof Boolean) {
                Boolean capValue;
                if ("true".equalsIgnoreCase(capabilityValue)) {
                  capValue = Boolean.TRUE;
                } else if ("false".equalsIgnoreCase(capabilityValue)) {
                  capValue = Boolean.FALSE;
                } else {
                  return false;
                }
                return capValue.equals(value);
              } else {
                return false;
              }
            })
        .toList();
  }

  // Value1 <operation> Value2
  private boolean compareValue(String operator, String value1, String value2, boolean numeric) {
    if (numeric) {
      double dblValue1 = Double.parseDouble(value1);
      double dblValue2 = Double.parseDouble(value2);
      return switch (operator) {
        case "<" -> dblValue1 < dblValue2;
        case ">" -> dblValue1 > dblValue2;
        case "=" -> dblValue1 == dblValue2;
        case "!=" -> dblValue1 != dblValue2;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      };
    } else {
      boolean boolValue1 = Boolean.parseBoolean(value1);
      boolean boolValue2 = Boolean.parseBoolean(value2);
      return switch (operator) {
        case "=" -> boolValue1 == boolValue2;
        case "!=" -> boolValue1 != boolValue2;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      };
    }
  }

  // finds a list of drones that match all the capabilities passed into the query endpoint
  public List<String> findDronesWithCapabilities(List<attributeQueryDto> attributeQueryDtos) {
    List<DroneDto> drones = medSupplyDronesClient.getAllDrones();
    List<DroneDto> matchedDrones = new ArrayList<>();
    for (DroneDto drone : drones) {
      CapabilityDto droneCapability = drone.getCapability();
      int attributeMatchCount = 0;
      for (attributeQueryDto attributeQuery : attributeQueryDtos) {
        String attribute = attributeQuery.getAttribute();
        String attributeValue = attributeQuery.getValue();
        String attributeOperator = attributeQuery.getOperator();
        switch (attribute) {
          case "cooling" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getCooling()),
                attributeValue,
                false)) {
              attributeMatchCount += 1;
            }
          }
          case "heating" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getHeating()),
                attributeValue,
                false)) {
              attributeMatchCount += 1;
            }
          }
          case "capacity" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getCapacity()),
                attributeValue,
                true)) {
              attributeMatchCount += 1;
            }
          }
          case "maxMoves" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getMaxMoves()),
                attributeValue,
                true)) {
              attributeMatchCount += 1;
            }
          }
          case "costPerMove" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getCostPerMove()),
                attributeValue,
                true)) {
              attributeMatchCount += 1;
            }
          }
          case "costInitial" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getCostInitial()),
                attributeValue,
                true)) {
              attributeMatchCount += 1;
            }
          }
          case "costFinal" -> {
            if (compareValue(
                attributeOperator,
                String.valueOf(droneCapability.getCostFinal()),
                attributeValue,
                true)) {
              attributeMatchCount += 1;
            }
          }
          case "id" -> {
            if (compareValue(
                attributeOperator, String.valueOf(drone.getId()), attributeValue, true)) {
              attributeMatchCount += 1;
            }
          }
        }
      }
      if (attributeMatchCount == attributeQueryDtos.size()) {
        matchedDrones.add(drone);
      }
    }
    return matchedDrones.stream().map(drone -> drone.getId()).toList();
  }

  private record Triple<PositionDto, LocalDate, LocalTime>(
      PositionDto medRecAddress, LocalDate medRecDate, LocalTime medRecTime) {}

  public List<String> findAvailableDrones(
      List<MedDispatchRecDto> medDispatchRecDtos, boolean restrictCapacity) {
    // Drones that match requirements AND time availability AND distance capability
    List<DroneDto> matchedDrones = new ArrayList<>();

    List<ServicePointDronesDto> servicePointsDrones =
        medSupplyDronesClient.getDronesForServicePoints();
    List<ServicePointDto> servicePoints = medSupplyDronesClient.getServicePoints();
    double cumulativeMaxCostOfMedRecords =
        medDispatchRecDtos.stream()
            .mapToDouble(
                dto ->
                    dto.getRequirements().getMaxCost() != null
                        ? dto.getRequirements().getMaxCost()
                        : 0.0)
            .sum();
    double cumulativeCapacity =
        medDispatchRecDtos.stream()
            .mapToDouble(
                dto ->
                    dto.getRequirements().getCapacity() != null
                        ? dto.getRequirements().getCapacity()
                        : 0.0)
            .sum();
    for (DroneDto drone : medSupplyDronesClient.getAllDrones()) {
      CapabilityDto droneCapability = drone.getCapability();
      List<Triple<PositionDto, LocalDate, LocalTime>> medRecords = new ArrayList<>();
      if (restrictCapacity && droneCapability.getCapacity() < cumulativeCapacity) {
        continue;
      }

      int satisfiedRequirements = 0;
      for (MedDispatchRecDto medDispatchRecDto : medDispatchRecDtos) {
        // If drone satisfies specific MedDispatch requirements of this loop, add it to medRecords
        // as a
        // Triple record including the corresponding address and time requirements for later
        // processing

        // Medical record requirements:
        RequirementsDto medRequirements = medDispatchRecDto.getRequirements();
        Double medRecCapacity = medRequirements.getCapacity();
        Double medRecMaxCost = medRequirements.getMaxCost();
        boolean medRecCooling = medRequirements.isCooling();
        boolean medRecHeating = medRequirements.isHeating();

        // Medical record delivery address:
        PositionDto medRecAddress = medDispatchRecDto.getDelivery();

        // Medical record time, date and id:
        LocalDate medRecDate = medDispatchRecDto.getDate();
        LocalTime medRecTime = medDispatchRecDto.getTime();

        Triple<PositionDto, LocalDate, LocalTime> droneWithCapability =
            new Triple<>(medRecAddress, medRecDate, medRecTime);

        // check if drone matches requirements:
        boolean matchesCapacity = droneCapability.getCapacity() >= medRecCapacity;
        boolean heatingRequirementsMet = true;
        if (medRecCooling || medRecHeating) {
          if (!(medRecCooling && droneCapability.getCooling())
              && !(medRecHeating && droneCapability.getHeating())) {
            heatingRequirementsMet = false;
          }
        }
        if (matchesCapacity && heatingRequirementsMet) {
          medRecords.add(droneWithCapability);
          satisfiedRequirements += 1;
        }
      }
      // Given that the drone satisfied the required capabilities of the Medical record check if
      // it is available and has enough moves within cost to make the trip before adding it to
      // matchedDrones:
      if (satisfiedRequirements != medDispatchRecDtos.size()) {
        medRecords.clear();
        continue;
      } else {
        Predicate<DroneAvailabilityDto> hasDrone = dto -> dto.getId().equals(drone.getId());
        ServicePointDronesDto servicePointWithDrone =
            servicePointsDrones.stream()
                .filter(servicePoint -> servicePoint.getDrones().stream().anyMatch(hasDrone))
                .findFirst()
                .orElse(null);
        if (servicePointWithDrone == null) {
          continue;
        }
        List<AvailabilityDto> droneAvailability =
            Objects.requireNonNull(
                    servicePointWithDrone.getDrones().stream()
                        .filter(hasDrone)
                        .findFirst()
                        .orElse(null))
                .getAvailability();

        //  check if drone is available at the specific time of the dispatches
        List<String> dayOfWeekAvailability =
            droneAvailability.stream().map(AvailabilityDto::getDayOfWeek).toList();
        List<LocalDate> matchingDays =
            medRecords.stream()
                .map(Triple::medRecDate)
                .filter(time -> dayOfWeekAvailability.contains(time.getDayOfWeek().toString()))
                .toList();
        boolean isAvailableDayOfWeek = matchingDays.size() == medDispatchRecDtos.size();
        boolean isAvailableTimeOfDay =
            medRecords.stream()
                .allMatch(
                    record ->
                        droneAvailability.stream()
                            .filter(
                                time ->
                                    time.getDayOfWeek()
                                        .equalsIgnoreCase(
                                            record.medRecDate().getDayOfWeek().toString()))
                            .anyMatch(
                                time ->
                                    !record.medRecTime().isBefore(LocalTime.parse(time.getFrom()))
                                        && !record
                                            .medRecTime()
                                            .isAfter(
                                                LocalTime.parse(time.getUntil())))); // loose bounds

        // check if drone can make the distance of the trip under cost and distance constraints
        if (isAvailableTimeOfDay && isAvailableDayOfWeek) {
          int servicePointId = servicePointWithDrone.getServicePointId();
          ServicePointDto servicePoint =
              servicePoints.stream()
                  .filter(sp -> sp.getId() == servicePointId)
                  .findFirst()
                  .orElse(null);
          if (servicePoint == null) {
            continue;
          }
          LocationDto servicePointStart = servicePoint.getLocation();
          PositionDto servicePointPosition =
              new PositionDto(servicePointStart.getLng(), servicePointStart.getLat());
          // calculate distance from service point drone is at to delivery address:

          double totalMoves =
              medDispatchRecDtos.stream()
                  .mapToDouble(
                      record -> {
                        DistanceDto distanceDto =
                            new DistanceDto(servicePointPosition, record.getDelivery());
                        double distance = CalculatePositioning.calculateDistance(distanceDto);
                        double numMoves =
                            Math.ceil(2 * distance / CalculatePositioning.MOVE_DISTANCE);
                        return numMoves;
                      })
                  .sum();
          double totalCost =
              totalMoves * drone.getCapability().getCostPerMove()
                  + droneCapability.getCostInitial()
                  + droneCapability.getCostFinal();
          Map<Integer, Double> proportionality =
              medDispatchRecDtos.stream()
                  .collect(
                      Collectors.toMap(
                          MedDispatchRecDto::getId,
                          dto ->
                              (2
                                      * CalculatePositioning.calculateDistance(
                                          new DistanceDto(servicePointPosition, dto.getDelivery()))
                                      / CalculatePositioning.MOVE_DISTANCE)
                                  / totalMoves));

          boolean canDeliver =
              medDispatchRecDtos.stream()
                  .allMatch(
                      record ->
                          record.getRequirements().getMaxCost() == null
                              || record.getRequirements().getMaxCost()
                                  >= proportionality.get(record.getId()) * totalCost);
          boolean isWithinMaxMoves =
              medDispatchRecDtos.stream()
                  .allMatch(
                      record ->
                          Math.ceil(
                                  2
                                      * CalculatePositioning.calculateDistance(
                                          new DistanceDto(
                                              servicePointPosition, record.getDelivery()))
                                      / CalculatePositioning.MOVE_DISTANCE)
                              <= drone.getCapability().getMaxMoves());
          if (canDeliver && isWithinMaxMoves) {
            matchedDrones.add(drone);
          }
        }
      }
    }
    return matchedDrones.stream().map(drone -> drone.getId()).toList();
  }

  public List<String> findFallbackDrones(List<MedDispatchRecDto> medDispatchRecDtos) {
    Set<String> validDroneIds = new HashSet<>();
    List<DroneDto> allDrones = medSupplyDronesClient.getAllDrones();
    List<ServicePointDronesDto> spDrones = medSupplyDronesClient.getDronesForServicePoints();
    List<ServicePointDto> servicePoints = medSupplyDronesClient.getServicePoints();

    for (MedDispatchRecDto order : medDispatchRecDtos) {
      boolean droneFoundForThisOrder = false;

      for (DroneDto drone : allDrones) {
        // check capabilities
        if (drone.getCapability().getCapacity() < order.getRequirements().getCapacity()) continue;
        if (order.getRequirements().isCooling() && !drone.getCapability().getCooling()) continue;
        if (order.getRequirements().isHeating() && !drone.getCapability().getHeating()) continue;

        Predicate<DroneAvailabilityDto> hasDrone = dto -> dto.getId().equals(drone.getId());
        var servicePointWithDrone =
            spDrones.stream()
                .filter(sp -> sp.getDrones().stream().anyMatch(hasDrone))
                .findFirst()
                .orElse(null);

        if (servicePointWithDrone == null) continue;

        List<AvailabilityDto> schedule =
            servicePointWithDrone.getDrones().stream()
                .filter(hasDrone)
                .findFirst()
                .get()
                .getAvailability();

        String orderDay = order.getDate().getDayOfWeek().toString();
        LocalTime orderTime = order.getTime();

        boolean shiftMatch =
            schedule.stream()
                .anyMatch(
                    s ->
                        s.getDayOfWeek().equalsIgnoreCase(orderDay)
                            && !orderTime.isBefore(LocalTime.parse(s.getFrom()))
                            && !orderTime.isAfter(LocalTime.parse(s.getUntil())));

        if (!shiftMatch) continue;

        // Check cost and distance
        var spLocation =
            servicePoints.stream()
                .filter(sp -> sp.getId() == servicePointWithDrone.getServicePointId())
                .findFirst()
                .orElseThrow()
                .getLocation();

        PositionDto basePos = new PositionDto(spLocation.getLng(), spLocation.getLat());

        // Calculate Euclidean distance
        DistanceDto distDto = new DistanceDto(basePos, order.getDelivery());
        double dist = CalculatePositioning.calculateDistance(distDto);

        // Calculate total moves for trip
        int movesNeeded = (int) Math.ceil((dist * 2) / CalculatePositioning.MOVE_DISTANCE);

        if (movesNeeded > drone.getCapability().getMaxMoves()) continue;

        // Check Max Cost
        if (order.getRequirements().getMaxCost() != null) {
          double tripCost =
              (movesNeeded * drone.getCapability().getCostPerMove())
                  + drone.getCapability().getCostInitial()
                  + drone.getCapability().getCostFinal();
          if (tripCost > order.getRequirements().getMaxCost()) continue;
        }

        // Valid drone found for this order
        validDroneIds.add(drone.getId());
        droneFoundForThisOrder = true;
        break;
      }

      if (!droneFoundForThisOrder) {
        System.err.println("Warning: No drone found for order " + order.getId());
      }
    }

    return new ArrayList<>(validDroneIds);
  }

  public PositionDto getStartPoint(String droneId) {
    List<ServicePointDronesDto> servicePointsDrones =
        medSupplyDronesClient.getDronesForServicePoints();
    List<ServicePointDto> servicePoints = medSupplyDronesClient.getServicePoints();
    DroneDto drone = staticQueries.findDrone(droneId);
    Predicate<DroneAvailabilityDto> hasDrone = dto -> dto.getId().equals(drone.getId());
    ServicePointDronesDto servicePointWithDrone =
        servicePointsDrones.stream()
            .filter(servicePoint -> servicePoint.getDrones().stream().anyMatch(hasDrone))
            .findFirst()
            .orElse(null);

    int servicePointId = servicePointWithDrone.getServicePointId();
    ServicePointDto servicePoint =
        servicePoints.stream().filter(sp -> sp.getId() == servicePointId).findFirst().orElse(null);
    LocationDto servicePointStart = servicePoint.getLocation();
    return new PositionDto(servicePointStart.getLng(), servicePointStart.getLat());
  }

  public OverallRouteDto calcDeliveryPath(
      List<MedDispatchRecDto> medDispatchRecDtos, boolean restrictCapacity) {
    medDispatchRecDtos.sort(
        Comparator.comparing(MedDispatchRecDto::getDate).thenComparing(MedDispatchRecDto::getTime));
    List<String> availableDrones = findAvailableDrones(medDispatchRecDtos, restrictCapacity);
    if (availableDrones.isEmpty()) {
      availableDrones = findFallbackDrones(medDispatchRecDtos);
    }

    List<MedDispatchRecDto> remaining = new ArrayList<>(medDispatchRecDtos);
    List<PositionDto> completePath = new ArrayList<>();
    Map<String, List<DeliveryPathDto>> droneDeliveries = new HashMap<>();
    double totalCost = 0.0;
    int totalMoves = 0;

    while (!remaining.isEmpty()) {
      boolean deliveryAssigned = false;

      for (String droneId : availableDrones) {
        List<MedDispatchRecDto> dispatchBasket = new ArrayList<>();
        Iterator<MedDispatchRecDto> recordIterator = remaining.iterator();
        double basketCapacity = 0.0;

        int droneServicePointId = getServicePointIdForDrone(droneId);
        PositionDto startPoint = getStartPoint(droneId);
        double movesSoFar = 0.0;
        PositionDto currentPoint = startPoint;

        DroneDto drone = staticQueries.findDrone(droneId);
        int movesLimit = drone.getCapability().getMaxMoves();
        Double droneCapacity = drone.getCapability().getCapacity();

        while (recordIterator.hasNext()) {
          MedDispatchRecDto record = recordIterator.next();

          int closestServicePointId = findClosestServicePointId(record.getDelivery());
          if (closestServicePointId != droneServicePointId) {
            continue; // let the closest drone handle it
          }

          Double dispatchWeight = record.getRequirements().getCapacity();
          AStarResult toPointResult =
              aStarService.findPathForLeg(currentPoint, record.getDelivery(), movesLimit, drone);
          int movesToPoint = toPointResult.getMovesUsed();

          AStarResult toSpResult =
              aStarService.findPathForLeg(record.getDelivery(), startPoint, movesLimit, drone);
          int movesToSp = toSpResult.getMovesUsed();

          if ((basketCapacity + dispatchWeight <= droneCapacity)
              && movesSoFar + movesToPoint <= movesLimit) {
            currentPoint = record.getDelivery();
            movesSoFar += movesToPoint;
            basketCapacity += dispatchWeight;
            dispatchBasket.add(record);
            recordIterator.remove();
          } else {
            continue;
          }
        }

        if (!dispatchBasket.isEmpty()) {
          // execute A* here
          PositionDto currentPosition = startPoint;
          List<AStarResult> legs = new ArrayList<>();

          for (MedDispatchRecDto record : dispatchBasket) {
            PositionDto goal = record.getDelivery();
            int maxMoves = (int) Math.ceil(drone.getCapability().getMaxMoves());
            AStarResult leg = aStarService.findPathForLeg(currentPosition, goal, maxMoves, drone);
            if (leg.isReachedGoal()) {
              deliveryAssigned = true;
              legs.add(leg);
              currentPosition = record.getDelivery();
            } else {
              remaining.add(record);
              continue;
            }

            if (dispatchBasket.indexOf(record) == dispatchBasket.size() - 1) {
              leg = aStarService.findPathForLeg(record.getDelivery(), startPoint, maxMoves, drone);
              if (leg.isReachedGoal()) {
                legs.add(leg);
              } else {
                remaining.add(record);
              }
            }
          }

          // reconstruct path of the full delivery from individual legs
          List<PositionDto> path = new ArrayList<>();
          for (AStarResult result : legs) {
            List<PositionDto> legPath = result.getPath();
            path.addAll(legPath);
            totalCost += result.getTotalCost();
            totalMoves += result.getMovesUsed();
          }
          int thisDeliveryId = deliveryId.getAndIncrement();
          DeliveryPathDto deliveryPathDto = new DeliveryPathDto(thisDeliveryId, path);
          droneDeliveries.computeIfAbsent(droneId, k -> new ArrayList<>()).add(deliveryPathDto);

          completePath.addAll(path);
        }
      }

      if (!deliveryAssigned) {
        break;
      }
    }

    List<DronePathDto> dronePathDtoList =
        droneDeliveries.keySet().stream()
            .map(key -> new DronePathDto(Integer.parseInt(key), droneDeliveries.get(key)))
            .toList();

    return new OverallRouteDto(totalCost, totalMoves, dronePathDtoList);
  }

  private int getServicePointIdForDrone(String droneId) {
    List<ServicePointDronesDto> servicePointsDrones =
        medSupplyDronesClient.getDronesForServicePoints();
    DroneDto drone = staticQueries.findDrone(droneId);

    Predicate<DroneAvailabilityDto> hasDrone = dto -> dto.getId().equals(drone.getId());
    ServicePointDronesDto servicePointWithDrone =
        servicePointsDrones.stream()
            .filter(sp -> sp.getDrones().stream().anyMatch(hasDrone))
            .findFirst()
            .orElse(null);

    return servicePointWithDrone != null ? servicePointWithDrone.getServicePointId() : -1;
  }

  private int findClosestServicePointId(PositionDto deliveryPosition) {
    List<ServicePointDto> servicePoints = medSupplyDronesClient.getServicePoints();

    double bestDistance = Double.MAX_VALUE;
    int bestId = -1;

    for (ServicePointDto sp : servicePoints) {
      LocationDto loc = sp.getLocation();
      PositionDto spPos = new PositionDto(loc.getLng(), loc.getLat());
      double distance =
          CalculatePositioning.calculateDistance(new DistanceDto(spPos, deliveryPosition));
      if (distance < bestDistance) {
        bestDistance = distance;
        bestId = sp.getId();
      }
    }
    return bestId;
  }

  public JsonNode calcDeliveryPathAsGeoJson(List<MedDispatchRecDto> medDispatchRecDtos) {
    OverallRouteDto overallRoute = calcDeliveryPath(medDispatchRecDtos, true);
    GeoJsonConverter converter = new GeoJsonConverter();
    return converter.toGeoJson(overallRoute);
  }
}
