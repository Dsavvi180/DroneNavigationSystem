package uk.ac.ed.acp.cw2.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ed.acp.cw2.service.Calculate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvc starts the web layer of the application, and loads specific controllers for testing and MockMvc
@WebMvcTest(ServiceController.class)
public class ServiceControllerTest {

    // Injects MockMvc bean pre-configured by @WebMvc. MockMvc is a class for simulating HTTP requests.
    @Autowired
    MockMvc mvc;

    // Test configuration defined for @WebMvc configuring the test ServiceController
    @TestConfiguration
    static class ValueConfig{
        // Bean to be passed into serviceUrl value for test context as opposed to URL defined in application.yaml
        @Bean
        URL serviceUrl() throws MalformedURLException {
            return URI.create("https://localhost:8080").toURL(); // test-only value
        }
    }
    // Valid get request to /uid, checks for correct string output
    @Test void uid_mock() throws Exception {
        mvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2507699"));
    }

    // Valid JSON posted to /distanceTo endpoint, checks for correct string output.
    @Test
    void distanceTo_mock() throws Exception {
        String body = """
                {"position1":{"lng":-3.192473,"lat":55.946233},
                   "position2":{"lng":-3.184319,"lat":55.942617}}
                """;
        // static methods being mocked so we use mockStatic static method of Mockito class.
        try (MockedStatic<Calculate> mockedDistanceTo = mockStatic(Calculate.class)) {
            mockedDistanceTo.when(() -> Calculate.calculateDistance((any()))).thenReturn(0.123456);
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0.123456"));
            mockedDistanceTo.verify(() -> Calculate.calculateDistance(any()));
        }
    }

    // Invalid payload should raise 400 Exception from @Valid constraint enforcement.
    @Test
    void distanceTo_invalid() throws Exception {
        // invalid: lat must be a float
        String bad = """
            {"position1":{"lng":-3.192473,"lat": ""},
             "position2":{"lng":-3.184319,"lat":55.942617}}
        """;

        mvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest());
    }

    // Valid JSON posted to /isCloseTo, checks for correct string output.
    @Test
    void isCloseTo_mock() throws Exception {
        String body = """
                {"position1":{"lng":-3.192473,"lat":55.946233},
                 "position2":{"lng":-3.192323,"lat":55.946233}}
                """;
        try (MockedStatic<Calculate> mockedIsCloseTo = mockStatic(Calculate.class)) {
            mockedIsCloseTo.when(()->Calculate.isCloseTo(any())).thenReturn(true);
            mvc.perform(post("/api/v1/isCloseTo").
                    contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
            mockedIsCloseTo.verify(()->Calculate.isCloseTo(any()));
        }
    }

    // Mock nextPosition method returns PositionDto and check controller endpoint serialises it to return JSON.
    @Test
    void nextPosition_mock() throws Exception {
        String body = """
                 {"start":{"lng":-3.192473,"lat":55.946233},"angle":90}
                """;
        try (MockedStatic<Calculate> mockedNextPosition = mockStatic(Calculate.class)) {
            uk.ac.ed.acp.cw2.dtos.PositionDto fake =
                    new uk.ac.ed.acp.cw2.dtos.PositionDto(-3.192473, 55.946383);
            mockedNextPosition.when(()->Calculate.nextPosition(any())).thenReturn(fake);
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.lng").value(-3.192473))
                    .andExpect(jsonPath("$.lat").value(55.946383));
            mockedNextPosition.verify(()->Calculate.nextPosition(any()));
        }
    }

    // Valid JSON posted to /isInRegion, checks for correct string output.
    @Test
    void isInRegion_mock() throws Exception {
        String body = """
                {"position":{"lng":-3.189,"lat":55.9445},
                           "region":{"name":"central",
                                     "vertices":[
                                        {"lng":-3.192473,"lat":55.946233},
                                        {"lng":-3.192473,"lat":55.942617},
                                        {"lng":-3.184319,"lat":55.942617},
                                        {"lng":-3.184319,"lat":55.946233},
                                        {"lng":-3.192473,"lat":55.946233}
                                     ]}}
                """;
        try (MockedStatic<Calculate> mockedIsInRegion = mockStatic(Calculate.class)) {
            mockedIsInRegion.when(()->Calculate.isInRegion(any())).thenReturn(true);
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
            mockedIsInRegion.verify(()->Calculate.isInRegion(any()));
        }
    }


}
