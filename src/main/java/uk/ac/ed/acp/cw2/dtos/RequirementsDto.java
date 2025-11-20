package uk.ac.ed.acp.cw2.dtos;


import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequirementsDto {
    @NotNull
    Double capacity;
    boolean cooling;
    boolean heating;
    Double maxCost;

    @AssertTrue
    public boolean isValidHeatingSettings(){
        return !(heating && cooling);
    }
}
