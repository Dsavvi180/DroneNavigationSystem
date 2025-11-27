package uk.ac.ed.acp.cw2.clients;

import jakarta.validation.Validator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.acp.cw2.dtos.DroneDto;
import uk.ac.ed.acp.cw2.dtos.RestrictedRegionDto;
import uk.ac.ed.acp.cw2.dtos.ServicePointDronesDto;
import uk.ac.ed.acp.cw2.dtos.ServicePointDto;

import java.util.List;

// Registering this class as a Spring bean for easier instance lifecycle management
@Component
@Validated
public class MedSupplyDronesClient {
  private final WebClient medSupplyDronesClient;
  private final Validator
      validator; // Inject validator bean to validate deserialisation of response to dtos

  // Construct web client class and inject bean dependencies
  public MedSupplyDronesClient(
      WebClient.Builder medSupplyDronesClientBuilder, String getEndpointIlp, Validator validator) {
    this.validator = validator;
    this.medSupplyDronesClient = medSupplyDronesClientBuilder.baseUrl(getEndpointIlp).build();
  }

  // Generic function to validate a range of Dtos - generic type T
  private <T> List<T> validateResponse(List<T> listDtos) {
    // Validate response from MedSupplyDronesClient API
    if (listDtos != null) {
      for (T dto : listDtos) {
        var errors = validator.validate(dto);
        if (!errors.isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
      }
      return listDtos;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }

  public List<DroneDto> getAllDrones() {
    return validateResponse(
        medSupplyDronesClient
            .get()
            .uri("/drones")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<DroneDto>>() {})
            .block());
  }

  public List<ServicePointDronesDto> getDronesForServicePoints() {
    return validateResponse(
        medSupplyDronesClient
            .get()
            .uri("/drones-for-service-points")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<ServicePointDronesDto>>() {})
            .block());
  }

  public List<ServicePointDto> getServicePoints() {
    return validateResponse(
        medSupplyDronesClient
            .get()
            .uri("/service-points")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<ServicePointDto>>() {})
            .block());
  }

  public List<RestrictedRegionDto> getRestrictedRegions() {
    return validateResponse(
        medSupplyDronesClient
            .get()
            .uri("/restricted-areas")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<RestrictedRegionDto>>() {})
            .block());
  }
}
