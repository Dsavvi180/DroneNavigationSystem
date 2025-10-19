package uk.ac.ed.acp.cw2.service.calculateTest;

import org.junit.jupiter.api.Test;
import uk.ac.ed.acp.cw2.dtos.DistanceDto;
import uk.ac.ed.acp.cw2.dtos.PositionDto;
import uk.ac.ed.acp.cw2.service.Calculate;

import static org.junit.jupiter.api.Assertions.*;

public class CalculateDistanceTest {

    @Test
    void testCalculateDistance(){
        /// Testing for variety of edge cases:

        /// Missing variables and null tests
        // Test 1: DistanceDto is null
        assertThrows(NullPointerException.class, () -> Calculate.calculateDistance(null));

        // Test 2: Position 1 is null
        DistanceDto p1 = new DistanceDto(null, new PositionDto(-3.19, 55.94));
        assertThrows(NullPointerException.class, () -> Calculate.calculateDistance(p1));

        // Test 3: Position 2 is null
        DistanceDto p2 = new DistanceDto(new PositionDto(-3.19, 55.94), null);
        assertThrows(NullPointerException.class, () -> Calculate.calculateDistance(p2));

        // Test 4: Position 1 and 2 are the same object in memory (Distance = 0.0)
        PositionDto p4 = new PositionDto(-3.192473, 55.946233);
        DistanceDto dto4 = new DistanceDto(p4, p4);
        assertEquals(0.0, Calculate.calculateDistance(dto4), 0.0);

        /// Geometric tests:
        // Test 5: Position 1 and 2 have value equality
        DistanceDto dto5 = new DistanceDto(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.946233)
        );
        assertEquals(0.0, Calculate.calculateDistance(dto5), 0.0);

        // Test 6: Commutativity of input points
        PositionDto p6_a = new PositionDto(-3.19, 55.94);
        PositionDto p6_b = new PositionDto(-3.18, 55.95);
        double d6_a = Calculate.calculateDistance(new DistanceDto(p6_a, p6_b));
        double d6_b = Calculate.calculateDistance(new DistanceDto(p6_b, p6_a));
        assertEquals(d6_a, d6_b, 0.0);

        // Test 7: Distance must be non-negative
        double d7 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.19, 55.94), new PositionDto(-3.18, 55.95)));
        assertTrue(d7 >= 0.0);

        // Test 8: Testing triangle inequality holds
        PositionDto a = new PositionDto(-3.192, 55.946);
        PositionDto b = new PositionDto(-3.190, 55.947);
        PositionDto c = new PositionDto(-3.188, 55.948);
        double ab = Calculate.calculateDistance(new DistanceDto(a, b));
        double bc = Calculate.calculateDistance(new DistanceDto(b, c));
        double ac = Calculate.calculateDistance(new DistanceDto(a, c));
        assertTrue(ac <= ab + bc); //+1e-12

        /// Axis invariance checks:
        // Test 9: Only longitude differs
        double d9 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192323, 55.946233)
        ));
        assertEquals(0.00015, d9, 1e-12); // delta = 1e-12

        // Test 10: Only latitude differs
        double d10 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.946083)
        ));
        assertEquals(0.00015, d10, 1e-12);

        // Test 11: Checking when distance is just below close threshold
        double d11 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.946233 + 0.000149999)
        ));
        assertTrue(d11 < 0.00015);

        // Test 12: Just above threshold
        double d12 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.192473, 55.946233),
                new PositionDto(-3.192473, 55.946233 + 0.000150001)
        ));
        assertTrue(d12 > 0.00015);

        ///  Validation checking: testing robustness of validation logic
        // Test 13: Longitude non-negative (Outside Edinburgh)
        double d13 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(3.19, 55.94), new PositionDto(-3.19, 55.94)
        ));
        assertEquals(6.38, d13);

        // Test 14: latitude non-positive:
        double d14 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.19, -55.94), new PositionDto(-3.19, 55.94)
        ));
        assertTrue(d14 > 100.0);

        // Test 15: Zeros for coordinates
        double d15 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(0.0, 55.94), new PositionDto(-3.19, 55.94)
        ));
        assertEquals(3.19, d15);

        // Test 16: Huge magnitudes
        double d16 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3000.0, 5600.0), new PositionDto(-3000.1, 5600.0)
        ));
        assertEquals(0.1, d16, 1e-12);

        /// Floating-point checks
        // Test 17: NaN checks
        double d17 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(Double.NaN, 55.946233),
                new PositionDto(-3.192473, 55.946233)
        ));
        assertTrue(Double.isNaN(d17));

        // Test 18: Infinity checks
        double d18 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(Double.POSITIVE_INFINITY, 55.946233),
                new PositionDto(-3.192473, 55.946233)
        ));
        assertTrue(Double.isInfinite(d18));

        // Test 20: Precision checks
        double d20 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(-3.1924730000000004, 55.9462330000000007),
                new PositionDto(-3.1924730000000000, 55.9462330000000000)
        ));
        assertEquals(0.0, d20, 1e-12);

        // Test 21: Value overflow check
        double big = 1e308;
        double d21 = Calculate.calculateDistance(new DistanceDto(
                new PositionDto(big, big), new PositionDto(-big, -big)
        ));
        assertTrue(Double.isInfinite(d21)); // dx^2 + dy^2 is too large

    }
}
