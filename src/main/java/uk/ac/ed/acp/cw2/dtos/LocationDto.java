package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private @NotNull Double lng;
    private @NotNull Double lat;
    private Double alt;
}
