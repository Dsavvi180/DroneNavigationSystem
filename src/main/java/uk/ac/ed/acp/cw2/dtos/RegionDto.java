package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionDto {
    private @NotBlank String name;
    private @NotEmpty @Size(min=4) List<@Valid PositionDto> vertices;
}
