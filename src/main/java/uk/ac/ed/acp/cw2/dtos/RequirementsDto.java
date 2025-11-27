package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequirementsDto {
  @NotNull Double capacity;
  boolean cooling;
  boolean heating;
  Double maxCost;

  // A drone can be heating AND cooling but a medical record can only have heating OR cooling
  @AssertTrue
  public boolean isValidHeatingSettings() {
    return !(heating && cooling);
  }
}
