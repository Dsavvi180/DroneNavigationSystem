package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestrictedRegionDto {
  @NotNull String name;
  @NotNull Integer id;
  @NotNull LimitsDto limits;
  @NotNull List<LocationDto> vertices;
}
