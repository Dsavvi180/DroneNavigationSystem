package uk.ac.ed.acp.cw2.controller;


import uk.ac.ed.acp.cw2.dtos.*;
import uk.ac.ed.acp.cw2.service.CalculatePositioning;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.service.DynamicQueries;
import uk.ac.ed.acp.cw2.service.StaticQueries;

import java.net.URL;
import java.util.List;

/**
 * Controller class that handles various HTTP endpoints for the application.
 * Provides functionality for serving the index page, retrieving a static UUID,
 * and managing key-value pairs through POST requests.
 */
@RestController()
@RequestMapping("/api/v1")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    // Injects URL specified in application.yml
    @Value("${ilp.service.url}")
    public URL serviceUrl;

    // Inject StaticQueries & DynamicQueries Service beans
    private final StaticQueries staticQueries;
    private final DynamicQueries dynamicQueries;
    public ServiceController(StaticQueries staticQueries, DynamicQueries dynamicQueries) {
        this.staticQueries = staticQueries;
        this.dynamicQueries = dynamicQueries;
    }


    @GetMapping("/")
    public String index() {
        return "<html><body>" +
                "<h1>Welcome from ILP</h1>" +
                "<h4>ILP-REST-Service-URL:</h4> <a href=\"" + serviceUrl + "\" target=\"_blank\"> " + serviceUrl+ " </a>" +
                "</body></html>";
    }

    @GetMapping("/uid")
    public String uid() {
        return "s2507699";
    }

    // @Valid enforces cascading validation rules on Dtos
    @PostMapping(path="/distanceTo", consumes="application/json")
    public double distanceTo(@Valid @RequestBody DistanceDto dto) {
        return CalculatePositioning.calculateDistance(dto);
    }

    @PostMapping(path="/isCloseTo", consumes="application/json")
    public boolean isCloseTo(@Valid @RequestBody DistanceDto dto) {
        return CalculatePositioning.isCloseTo(dto);
    }

    @PostMapping(path="/nextPosition", consumes="application/json")
    public PositionDto nextPosition(@Valid @RequestBody NextPositionDto dto){
        return CalculatePositioning.nextPosition(dto);
    }

    @PostMapping(path="/isInRegion", consumes="application/json")
    public boolean isInRegion(@Valid @RequestBody RegionCheckDto dto){
        return CalculatePositioning.isInRegion(dto);
    }

    /* Static Queries */
    @GetMapping("/dronesWithCooling/{state}")
    public List<DroneDto> dronesWithCooling(@PathVariable boolean state){
        return staticQueries.getDronesWithCooling(state);
    }

    @GetMapping("/droneDetails/{id}")
    public DroneDto droneDetails(@PathVariable String id){
        return staticQueries.findDrone(id);
    }
    /* Dynamic Queries */
    @GetMapping("/queryAsPath/{capabilityName}/{capabilityValue}")
    public List<DroneDto> dronesWithCapability(@PathVariable String capabilityName, @PathVariable String capabilityValue){
        return dynamicQueries.findDronesWithCapability(capabilityName,capabilityValue);
    }

    @PostMapping("/query")
    public void dronesWithCapabilities(@RequestBody List<@Valid attributeQueryDto> attributeList){
        List<DroneDto> matchedDrones = dynamicQueries.findDronesWithCapabilities(attributeList);
    }
}
