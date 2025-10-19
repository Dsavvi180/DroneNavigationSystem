package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionCheckDto {
    private @NotNull @Valid PositionDto position;
    private @NotNull @Valid RegionDto region;
}
