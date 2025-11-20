package uk.ac.ed.acp.cw2.dtos;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityDto {
    @NotNull Boolean cooling;
    @NotNull Boolean heating;
    @NotNull Double capacity;
    @NotNull Integer maxMoves;
    @NotNull Double costPerMove;
    @NotNull Double costInitial;
    @NotNull Double costFinal;
}
