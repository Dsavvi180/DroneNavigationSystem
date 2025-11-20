package uk.ac.ed.acp.cw2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.clients.MedSupplyDronesClient;
import uk.ac.ed.acp.cw2.dtos.DroneDto;

import java.util.List;
import java.util.Objects;

@Service
public class StaticQueries {
    private final MedSupplyDronesClient medSupplyDronesClient;

    public StaticQueries(MedSupplyDronesClient medSupplyDronesClient) {
        this.medSupplyDronesClient = medSupplyDronesClient;
    }

    public List<DroneDto> getDronesWithCooling(boolean state) {
        return medSupplyDronesClient.getAllDrones()
                .stream()
                .filter(drone -> drone.getCapability().getCooling() == state)
                .toList();
    }

    public DroneDto findDrone(String id) {
        return medSupplyDronesClient.getAllDrones()
                .stream()
                .filter(droneDto -> Objects.equals(droneDto.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

