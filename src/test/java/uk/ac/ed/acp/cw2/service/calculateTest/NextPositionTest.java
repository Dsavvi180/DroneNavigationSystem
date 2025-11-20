package uk.ac.ed.acp.cw2.service.calculateTest;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.dtos.DistanceDto;
import uk.ac.ed.acp.cw2.dtos.NextPositionDto;
import uk.ac.ed.acp.cw2.dtos.PositionDto;
import uk.ac.ed.acp.cw2.service.CalculatePositioning;

import static org.junit.jupiter.api.Assertions.*;

public class NextPositionTest {

    private static final double MOVE = 0.00015;

    @Test
    void testNextPosition() {

        PositionDto start = new PositionDto(-3.192473, 55.946233);

        // Test 1: Null start position should throw NullPointerException
        assertThrows(NullPointerException.class, () -> CalculatePositioning.nextPosition(new NextPositionDto(null, 0)));

        // Test 2: move in a full rotation (E, N, W, S), we should end up at start point
        PositionDto p2a = CalculatePositioning.nextPosition(new NextPositionDto(start, 0));
        PositionDto p2b = CalculatePositioning.nextPosition(new NextPositionDto(p2a, 90));
        PositionDto p2c = CalculatePositioning.nextPosition(new NextPositionDto(p2b, 180));
        PositionDto p2d = CalculatePositioning.nextPosition(new NextPositionDto(p2c, 270));
        double loopDist = CalculatePositioning.calculateDistance(new DistanceDto(start, p2d));
        assertTrue(loopDist < 1e-3); // checks if actual distance from old to new point adds up

        // Test 3: Random list of valid angles should always move ~0.00015 each time
        for (int angle : new int[]{0, 45, 90, 135, 180, 225, 270, 315, 360}) {
            PositionDto p = CalculatePositioning.nextPosition(new NextPositionDto(start, angle));
            double d = CalculatePositioning.calculateDistance(new DistanceDto(start, p));
            assertTrue(Math.abs(d - MOVE) < 1e-1); // checks if actual distance from old to new point adds up
        }
    }
}
