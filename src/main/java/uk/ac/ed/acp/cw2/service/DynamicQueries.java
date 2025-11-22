package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.clients.MedSupplyDronesClient;
import uk.ac.ed.acp.cw2.dtos.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class DynamicQueries {
    private final MedSupplyDronesClient medSupplyDronesClient;

    public DynamicQueries(MedSupplyDronesClient medSupplyDronesClient) {
        this.medSupplyDronesClient = medSupplyDronesClient;
    }
 // Finds drones that match the capability passed in the payload to the queryAsPath/attribute-name/attribute-value endpoint
    public List<DroneDto> findDronesWithCapability(String capabilityName, String capabilityValue){
        ObjectMapper objectMapper = new ObjectMapper(); // converts Dto to map object
        return medSupplyDronesClient.getAllDrones()
                .stream()
                .filter(drone ->{
                    Object value = objectMapper.convertValue(drone.getCapability(), Map.class)
                            .get(capabilityName); // if the Map object i.e. the mapped drone dto doesn't have the capability as its key, null is returned
                    if (value instanceof Double){
                        try {
                            double capValue = Double.parseDouble(capabilityValue);
                            return Double.compare((Double) value, capValue)==0;
                         } catch (NumberFormatException error){
                            return false;
                        }
                    } else if (value instanceof Integer){
                        try {
                            Integer capValue = Integer.parseInt(capabilityValue);
                            return capValue.equals(value);
                        } catch (NumberFormatException error){
                            return false;
                        }
                    } else if(value instanceof Boolean){
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
                }).toList();
    }
    // Value1 <operation> Value2
    private boolean compareValue(String operator, String value1, String value2, boolean numeric){
        if (numeric){
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
            return switch(operator){
                case "=" -> boolValue1 == boolValue2;
                case "!=" -> boolValue1 != boolValue2;
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            };
        }
    }
    // finds a list of drones that all match the capabilities passed into the query endpoint
    public List<DroneDto> findDronesWithCapabilities(List<attributeQueryDto> attributeQueryDtos){
        List<DroneDto> drones =  medSupplyDronesClient.getAllDrones();
        List<DroneDto> matchedDrones = new ArrayList<>();
        for (DroneDto drone : drones){
            CapabilityDto droneCapability = drone.getCapability();
            int attributeMatchCount=0;
            for (attributeQueryDto attributeQuery : attributeQueryDtos){
                String attribute = attributeQuery.getAttribute();
                String attributeValue = attributeQuery.getValue();
                String attributeOperator = attributeQuery.getOperator();
                        switch (attribute){
                            case "cooling"      -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCooling()), attributeValue, false)){ attributeMatchCount+=1; }}
                            case "heating"      -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getHeating()), attributeValue, false)){ attributeMatchCount+=1; }}
                            case "capacity"     -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCapacity()), attributeValue, true)){ attributeMatchCount+=1; }}
                            case "maxMoves"     -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getMaxMoves()), attributeValue, true)){ attributeMatchCount+=1; }}
                            case "costPerMove"  -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostPerMove()), attributeValue, true)){ attributeMatchCount+=1; }}
                            case "costInitial"  -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostInitial()), attributeValue, true)){ attributeMatchCount+=1; }}
                            case "costFinal"    -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostFinal()), attributeValue, true)){ attributeMatchCount+=1; }}
                        }
            }
            if (attributeMatchCount==attributeQueryDtos.size()){
                matchedDrones.add(drone);
            }
        }
        return matchedDrones;
    }
    // New record type for encapsulating a drone with its corresponding address and time for preprocessing ease
    private record Triple< PositionDto, LocalDate, LocalTime>(PositionDto medRecAddress, LocalDate medRecDate, LocalTime medRecTime){ }

    public List<DroneDto> findAvailableDrones(List<MedDispatchRecDto> medDispatchRecDtos){
        // Drones that match requirements AND time availability AND distance capability
        List<DroneDto> matchedDrones = new ArrayList<>();
        List<ServicePointDronesDto> servicePointsDrones = medSupplyDronesClient.getDronesForServicePoints();
        Double cumulativeMaxCostOfMedRecords = medDispatchRecDtos.stream().mapToDouble(dto -> dto.getRequirements().getMaxCost()!=null?dto.getRequirements().getMaxCost():0.0).sum();
        for (DroneDto drone : medSupplyDronesClient.getAllDrones()){
            CapabilityDto droneCapability = drone.getCapability();
            List<Triple< PositionDto, LocalDate, LocalTime>> medRecords = new ArrayList<>();
            int satisfiedRequirements = 0;
            for (MedDispatchRecDto medDispatchRecDto : medDispatchRecDtos){
                // If drone satisfies specific MedDispatch requirements of this loop, add it to medRecords as a
                // Triple record including the corresponding address and time requirements for further checks:

                //Medical record requirements:
                RequirementsDto  medRequirements = medDispatchRecDto.getRequirements();
                Double medRecCapacity = medRequirements.getCapacity();
                Double medRecMaxCost = medRequirements.getMaxCost();
                boolean medRecCooling = medRequirements.isCooling();
                boolean medRecHeating = medRequirements.isHeating();

                // Medical record delivery address:
                PositionDto medRecAddress = medDispatchRecDto.getDelivery();

                //Medical record time, date & id:
                LocalDate medRecDate = medDispatchRecDto.getDate();
                LocalTime medRecTime = medDispatchRecDto.getTime();

                Triple< PositionDto, LocalDate, LocalTime> droneWithCapability = new Triple<>(medRecAddress, medRecDate, medRecTime);

                // Match drone to medical record requirement:
                boolean matchesCapacity = droneCapability.getCapacity() <= medRecCapacity;
                boolean matchesCooling = medRecCooling && droneCapability.getCooling();
                boolean matchesHeating = medRecHeating && droneCapability.getHeating();
                if (matchesCapacity && (matchesCooling || matchesHeating)){
                    medRecords.add(droneWithCapability);
                    satisfiedRequirements+=1;
                }
            }
            // Given that the drone satisfied the required capabilities of the Medical record we check if it is available and has enough moves within cost to make the trip before adding it to matchedDrones:
            if (satisfiedRequirements!=medDispatchRecDtos.size()){
                medRecords.clear();
            } else {
                Predicate<DroneAvailabilityDto> hasDrone = dto -> dto.getId().equals(drone.getId());
                ServicePointDronesDto servicePointWithDrone = servicePointsDrones.stream().filter(servicePoint -> servicePoint.getDrones().stream()
                                .anyMatch(hasDrone)).findFirst().orElse(null);
                if (servicePointWithDrone == null){
                    continue;
                }
                List<AvailabilityDto> droneAvailability = Objects.requireNonNull(servicePointWithDrone.getDrones().stream().filter(hasDrone).findFirst().orElse(null)).getAvailability();
                // Now check if drone is available at the specific time of the medical deliveries
                List<String> dayOfWeekAvailability =  droneAvailability.stream().map(AvailabilityDto::getDayOfWeek).toList();
                List<LocalDate> matchingDays = medRecords.stream().map(Triple::medRecDate).filter(time-> dayOfWeekAvailability.contains(time.getDayOfWeek().toString())).toList();
                boolean isAvailableDayOfWeek = matchingDays.size() == medDispatchRecDtos.size();
                boolean isAvailableTimeOfDay = medRecords.stream()
                        .allMatch(record ->  droneAvailability.stream()
                                .filter(time-> time.getDayOfWeek().equalsIgnoreCase(record.medRecDate().getDayOfWeek().toString()))
                                .anyMatch(time -> !record.medRecTime().isBefore(LocalTime.parse(time.getFrom())) && !record.medRecTime().isAfter(LocalTime.parse(time.getUntil())))); //loose bounds

                // Now check if drone can make the distance of the trip under cost constraint


            }

        }
        //               @NotNull Integer maxMoves;
//                @NotNull Double costPerMove;
//                @NotNull Double costInitial;
//                @NotNull Double costFinal;
        // find
        // Check if drone has enough moves to get to delivery point:
        // Calculate Euclidean distance between drone at service point to delivery address
    }
}
