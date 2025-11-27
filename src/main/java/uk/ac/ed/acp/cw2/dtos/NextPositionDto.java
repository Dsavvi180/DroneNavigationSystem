package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextPositionDto {
  private @NotNull @Valid PositionDto start;
  private @NotNull @Min(value = 0) @Max(value = 360) double angle;
}
