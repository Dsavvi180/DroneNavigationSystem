package uk.ac.ed.acp.cw2.clients;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.dtos.DroneDto;

import java.util.List;

// Registering this class as a Spring bean for easier instance lifecycle management
@Component
@Validated
public class MedSupplyDronesClient {
    private final WebClient medSupplyDronesClient;
    private final Validator validator; // Inject validator bean to validate deserialisation of response to dtos

    // Construct web client class and inject bean dependencies
    public MedSupplyDronesClient(WebClient.Builder medSupplyDronesClientBuilder, String getEndpointIlp, Validator validator) {
        this.validator = validator;
        this.medSupplyDronesClient = medSupplyDronesClientBuilder.baseUrl(getEndpointIlp).build();
    }

    public List<DroneDto> getAllDrones(){
        List<DroneDto> drones =  medSupplyDronesClient.get()
                .uri("/drones")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DroneDto>>() {}).block(); // tell thread to wait for the whole payload to arrive before continuing

        // Validate response from MedSupplyDronesClient API
        try {
            if (drones!=null){
                for (DroneDto drone : drones){
                    var errors = validator.validate(drone);
                    if (!errors.isEmpty()){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                    }
                }
                return drones;
            }
        } catch (Exception error){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }



