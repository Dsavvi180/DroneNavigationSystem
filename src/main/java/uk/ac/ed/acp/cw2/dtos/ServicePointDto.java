package uk.ac.ed.acp.cw2.dtos;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePointDto {
    @NotNull String name;
    @NotNull int id;
    @Valid LocationDto location;
}
