package uk.ac.ed.acp.cw2.service.calculateTest;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.dtos.DistanceDto;
import uk.ac.ed.acp.cw2.dtos.PositionDto;
import uk.ac.ed.acp.cw2.service.CalculatePositioning;

import static org.junit.jupiter.api.Assertions.*;

public class IsCloseToTest {

    @Test
    void testIsCloseTo() {

        // Test 1: Identical points (distance = 0)
        PositionDto p1 = new PositionDto(-3.192473, 55.946233);
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(p1, p1)));

        // Test 2: Just below threshold
        double threshold = 0.00015;
        PositionDto a2 = new PositionDto(-3.192473, 55.946233);
        PositionDto b2 = new PositionDto(-3.192473, 55.946233);
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(a2, b2)));

        // Test 3: Exactly at threshold
        PositionDto a3 = new PositionDto(-3.192473, 55.946233);
        PositionDto b3 = new PositionDto(-3.192473, 55.946233 + threshold);
        double d = CalculatePositioning.calculateDistance(new DistanceDto(a3,b3));
        assertTrue(Math.abs(d - 0.00015) < 1e-12);


        // Test 4: Just above threshold
        PositionDto a4 = new PositionDto(-3.192473, 55.946233);
        PositionDto b4 = new PositionDto(-3.192473, 55.946233 + (threshold + 1e-12));
        assertFalse(CalculatePositioning.isCloseTo(new DistanceDto(a4, b4)));

        // Test 5: Diagonal move just under threshold
        double step5 = (threshold - 1e-12) / Math.sqrt(2.0);
        PositionDto a5 = new PositionDto(-3.192473, 55.946233);
        PositionDto b5 = new PositionDto(-3.192473 + step5, 55.946233 + step5);
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(a5, b5)));

        // Test 6: Diagonal move exactly at threshold
        double step6 = threshold / Math.sqrt(2.0);
        PositionDto a6 = new PositionDto(-3.192473, 55.946233);
        PositionDto b6 = new PositionDto(-3.192473 + step6, 55.946233 + step6);
        d = CalculatePositioning.calculateDistance(new DistanceDto(a6, b6));
        assertTrue(Math.abs(d - 0.00015) < 1e-12);

        // Test 7: Commutativity test
        PositionDto p7a = new PositionDto(-3.19, 55.94);
        PositionDto p7b = new PositionDto(-3.18995, 55.93990); // ~0.00011 apart
        boolean ab = CalculatePositioning.isCloseTo(new DistanceDto(p7a, p7b));
        boolean ba = CalculatePositioning.isCloseTo(new DistanceDto(p7b, p7a));
        assertEquals(ab, ba);

        // Test 8: Far apart points
        PositionDto a8 = new PositionDto(-3.20, 55.94);
        PositionDto b8 = new PositionDto(-3.10, 55.84);
        assertFalse(CalculatePositioning.isCloseTo(new DistanceDto(a8, b8)));

        // Test 9: Precision tests
        double baseLng9 = -3.192473;
        double ulp9 = Math.ulp(baseLng9); // approx. 4.44e-16
        PositionDto a9 = new PositionDto(baseLng9, 55.946233);
        PositionDto b9 = new PositionDto(baseLng9 + ulp9, 55.946233);
        assertTrue(ulp9 < threshold);
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(a9, b9)));

        // Test 10: Longitude-only under threshold
        PositionDto a10 = new PositionDto(-3.192473, 55.946233);
        PositionDto b10 = new PositionDto(-3.192473 + (threshold - 1e-12), 55.946233);
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(a10, b10)));

        // Test 11: Latitude-only exactly threshold
        PositionDto a11 = new PositionDto(-3.192473, 55.946233);
        PositionDto b11 = new PositionDto(-3.192473, 55.946233 + threshold);
        d = CalculatePositioning.calculateDistance(new DistanceDto(a11, b11));
        assertTrue(Math.abs(d-0.00015)<1e-12);

        // Test 12: NaN value
        PositionDto a12 = new PositionDto(Double.NaN, 55.946233);
        PositionDto b12 = new PositionDto(-3.192473, 55.946233);
        assertFalse(CalculatePositioning.isCloseTo(new DistanceDto(a12, b12)));

        // Test 13: Infinite value
        PositionDto a13 = new PositionDto(Double.POSITIVE_INFINITY, 55.946233);
        PositionDto b13 = new PositionDto(-3.192473, 55.946233);
        assertFalse(CalculatePositioning.isCloseTo(new DistanceDto(a13, b13)));

        // Test 14: Missing latitude
        PositionDto a14 = new PositionDto(-3.19, 0.0); // simulate missing lat
        PositionDto b14 = new PositionDto(-3.19, 55.94);
        assertFalse(CalculatePositioning.isCloseTo(new DistanceDto(a14, b14)));

        // Test 15: Null DistanceDto
        assertThrows(NullPointerException.class, () -> CalculatePositioning.isCloseTo(null));

        // Test 16: Null positions inside DistanceDto
        assertThrows(NullPointerException.class,
                () -> CalculatePositioning.isCloseTo(new DistanceDto(null, new PositionDto(-3.19, 55.94))));
        assertThrows(NullPointerException.class,
                () -> CalculatePositioning.isCloseTo(new DistanceDto(new PositionDto(-3.19, 55.94), null)));

        // Test 17: Realistic Edinburgh close coordinate test
        PositionDto a17 = new PositionDto(-3.188, 55.943);
        PositionDto b17 = new PositionDto(-3.18805, 55.94302); // ~0.000054 apart
        assertTrue(CalculatePositioning.isCloseTo(new DistanceDto(a17, b17)));
    }
}
