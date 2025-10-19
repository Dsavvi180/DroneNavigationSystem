package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Data generates getter and setter methods, overrides equality and hashcode methods
// Allows direct value equality checks when comparing different Dtos of the same type

// @NoArgsConstructor required for Jackson in @RequestBody to serialise JSON into Dtos
// @AllArgsConstructor for manual Dto instantiation
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceDto {
    private @NotNull @Valid uk.ac.ed.acp.cw2.dtos.PositionDto position1;
    private @NotNull @Valid uk.ac.ed.acp.cw2.dtos.PositionDto position2;
}
