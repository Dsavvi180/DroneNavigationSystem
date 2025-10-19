package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {
    // Jackson coerces "" to 0.0 for primitive field double so we use Double
    private @NotNull Double lng;
    private @NotNull Double lat;
}
