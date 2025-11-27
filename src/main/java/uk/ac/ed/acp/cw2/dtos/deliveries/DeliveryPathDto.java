package uk.ac.ed.acp.cw2.dtos.deliveries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import uk.ac.ed.acp.cw2.dtos.PositionDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPathDto {

  @NotNull Integer deliveryId;

  @NotNull List<PositionDto> flightPath;
}
