package uk.ac.ed.acp.cw2.dtos.deliveries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DronePathDto {

  @NotNull Integer droneId;

  @NotNull List<DeliveryPathDto> deliveries;
}
