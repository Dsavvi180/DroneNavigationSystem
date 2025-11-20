package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.clients.MedSupplyDronesClient;
import uk.ac.ed.acp.cw2.dtos.CapabilityDto;
import uk.ac.ed.acp.cw2.dtos.DroneDto;
import uk.ac.ed.acp.cw2.dtos.attributeQueryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DynamicQueries {
    private final MedSupplyDronesClient medSupplyDronesClient;

    public DynamicQueries(MedSupplyDronesClient medSupplyDronesClient) {
        this.medSupplyDronesClient = medSupplyDronesClient;
    }

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
                    } else{
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
    public List<DroneDto> findDronesWithCapabilities(List<attributeQueryDto> attributeQueryDtos){
        List<DroneDto> drones =  medSupplyDronesClient.getAllDrones();
        List<DroneDto> matchedDrones = new ArrayList<>();
        for (attributeQueryDto attributeQuery : attributeQueryDtos){
            String attribute = attributeQuery.getAttribute();
            String attributeValue = attributeQuery.getValue();
            String attributeOperator = attributeQuery.getOperator();
            for (DroneDto drone : drones){
                CapabilityDto droneCapability = drone.getCapability();
                    switch (attribute){
                        case "cooling"      -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCooling()), attributeValue, false)){ matchedDrones.add(drone); }}
                        case "heating"      -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getHeating()), attributeValue, false)){ matchedDrones.add(drone); }}
                        case "capacity"     -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCapacity()), attributeValue, true)){ matchedDrones.add(drone); }}
                        case "maxMoves"     -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getMaxMoves()), attributeValue, true)){ matchedDrones.add(drone); }}
                        case "costPerMove"  -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostPerMove()), attributeValue, true)){ matchedDrones.add(drone); }}
                        case "costInitial"  -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostInitial()), attributeValue, true)){ matchedDrones.add(drone); }}
                        case "costFinal"    -> {if(compareValue(attributeOperator, String.valueOf(droneCapability.getCostFinal()), attributeValue, true)){ matchedDrones.add(drone); }}
                    }
            }
        }
        return matchedDrones;
    }
}
