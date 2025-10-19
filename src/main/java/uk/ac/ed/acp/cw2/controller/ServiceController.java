package uk.ac.ed.acp.cw2.controller;

import uk.ac.ed.acp.cw2.service.Calculate;

import uk.ac.ed.acp.cw2.dtos.RegionCheckDto;
import uk.ac.ed.acp.cw2.dtos.DistanceDto;
import uk.ac.ed.acp.cw2.dtos.NextPositionDto;
import uk.ac.ed.acp.cw2.dtos.PositionDto;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URL;

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
        return Calculate.calculateDistance(dto);
    }

    @PostMapping(path="/isCloseTo", consumes="application/json")
    public boolean isCloseTo(@Valid @RequestBody DistanceDto dto) {
        return Calculate.isCloseTo(dto);
    }

    @PostMapping(path="/nextPosition", consumes="application/json")
    public PositionDto nextPosition(@Valid @RequestBody NextPositionDto dto){
        return Calculate.nextPosition(dto);
    }

    @PostMapping(path="/isInRegion", consumes="application/json")
    public boolean isInRegion(@Valid @RequestBody RegionCheckDto dto){
        return Calculate.isInRegion(dto);
    }
}
