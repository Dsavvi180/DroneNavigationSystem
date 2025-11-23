package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitsDto {
  @NotNull Integer lower;
  @NotNull Integer upper;
}
