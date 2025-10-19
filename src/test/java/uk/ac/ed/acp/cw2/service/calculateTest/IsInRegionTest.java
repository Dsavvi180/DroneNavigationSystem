package uk.ac.ed.acp.cw2.service.calculateTest;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import uk.ac.ed.acp.cw2.dtos.PositionDto;
import uk.ac.ed.acp.cw2.dtos.RegionCheckDto;
import uk.ac.ed.acp.cw2.dtos.RegionDto;
import uk.ac.ed.acp.cw2.service.Calculate;

public class IsInRegionTest {

    // Helper method to make a RegionDto
    private RegionDto region(String name, List<PositionDto> vertices) {
        return new RegionDto(name, vertices);
    }

    // CLOSED rectangle for central Edinburgh area
    // Coordinates from ILP instructions sheet
    private List<PositionDto> centralRectClosed() {
        return new ArrayList<>(Arrays.asList(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.942617),
                new PositionDto(-3.184319, 55.942617),
                new PositionDto(-3.184319, 55.946233),
                new PositionDto(-3.192473, 55.946233) // closed
        ));
    }

    // Same rectangle but NOT closed
    private List<PositionDto> centralRectOpen() {
        return new ArrayList<>(Arrays.asList(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.942617),
                new PositionDto(-3.184319, 55.942617),
                new PositionDto(-3.184319, 55.946233)
        ));
    }

    @Test
    void testIsInRegion() {

        RegionDto region = region("central", centralRectClosed());

        // Test 1: Inside point should return true
        PositionDto inside = new PositionDto(-3.189000, 55.944500);
        boolean insideResult = Calculate.isInRegion(new RegionCheckDto(inside, region));
        assertTrue(insideResult);

        // Test 2: Outside point should return false
        PositionDto outside = new PositionDto(-3.180000, 55.944000);
        boolean outsideResult = Calculate.isInRegion(new RegionCheckDto(outside, region));
        assertFalse(outsideResult);

        // Test 3: Point exactly on an edge should return True (check includes boundary)
        PositionDto onEdge = new PositionDto(-3.192473, 55.944000);
        boolean edgeResult = Calculate.isInRegion(new RegionCheckDto(onEdge, region));
        assertTrue(edgeResult);

        // Test 4: Point exactly on a vertex should return True
        PositionDto onVertex = new PositionDto(-3.192473, 55.946233);
        boolean vertexResult = Calculate.isInRegion(new RegionCheckDto(onVertex, region));
        assertTrue(vertexResult);
    }

    @Test
    void testIsInRegion_notClosedPolygon() {

        // Test 5: Region not closed should raise 400 Bad Request
        RegionDto region = region("openRect", centralRectOpen());
        PositionDto point = new PositionDto(-3.189000, 55.944500);

        assertThrows(ResponseStatusException.class,
                () -> Calculate.isInRegion(new RegionCheckDto(point, region)));
    }

    @Test
    void testIsInRegion_concavePolygon() {

        // Test 6: Concave polygon, arrow shape
        List<PositionDto> concave = new ArrayList<>(Arrays.asList(
                new PositionDto(-3.200000, 55.950000),
                new PositionDto(-3.180000, 55.950000),
                new PositionDto(-3.180000, 55.940000),
                new PositionDto(-3.190000, 55.945000), // inward dent
                new PositionDto(-3.200000, 55.940000),
                new PositionDto(-3.200000, 55.950000) // close
        ));
        RegionDto region = region("concave", concave);

        // Inside
        PositionDto inside = new PositionDto(-3.1905, 55.9460);
        assertTrue(Calculate.isInRegion(new RegionCheckDto(inside, region)));

        // Point inside indentation but outside actual area
        PositionDto notch = new PositionDto(-3.190000, 55.944800);
        assertFalse(Calculate.isInRegion(new RegionCheckDto(notch, region)));
    }


    @Test
    void testIsInRegion_clockwiseVsCounterClockwise() {

        // Test 7: Same rectangle but reversed order (should behave the same)
        List<PositionDto> ccw = centralRectClosed();
        List<PositionDto> cw = new ArrayList<>();
        for (int i = ccw.size() - 1; i >= 0; i--) cw.add(ccw.get(i)); // reverse order

        RegionDto regionCCW = region("rectCCW", ccw);
        RegionDto regionCW = region("rectCW", cw);

        PositionDto inside = new PositionDto(-3.189500, 55.944800);
        assertTrue(Calculate.isInRegion(new RegionCheckDto(inside, regionCCW)));
        assertTrue(Calculate.isInRegion(new RegionCheckDto(inside, regionCW)));
    }

    @Test
    void testIsInRegion_nullHandling() {

        // Test 8: Null arguments should throw NullPointerException
        assertThrows(NullPointerException.class, () -> Calculate.isInRegion(null));

        PositionDto p = new PositionDto(-3.189000, 55.944500);
        RegionDto region = region("central", centralRectClosed());

        assertThrows(NullPointerException.class, () -> Calculate.isInRegion(new RegionCheckDto(p, null)));
        assertThrows(NullPointerException.class, () -> Calculate.isInRegion(new RegionCheckDto(null, region)));

        RegionDto badRegion = new RegionDto("bad", null);
        assertThrows(NullPointerException.class, () -> Calculate.isInRegion(new RegionCheckDto(p, badRegion)));
    }
}
