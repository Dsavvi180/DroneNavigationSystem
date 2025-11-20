package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class attributeQueryDto {
    @NotNull
    @Pattern(regexp = "cooling|heating|capacity|maxMoves|costPerMove|costInitial|costFinal")
    private String attribute;
    @NotNull
    @Pattern(regexp = "=|!=|<|>")
    private String operator;
    @NotNull
    private String value;

    // Validate cooling/heating (boolean attributes) to operator and value inputs
    @AssertTrue(message = "")
    public boolean isOperatorValidForBooleanAttributes(){
        if (attribute == null || operator == null || value == null) {
            // @NotNull will handle null values but is triggered later than this assertion - prevent this assertion throwing null pointer exception
            return true;
        }
        if (attribute.equals("cooling") || attribute.equals("heating")){
            return (operator.equals("=") || operator.equals("!="))
                    && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
        }
        else {
            return true;
        }
    }
    // Validate numeric attributes to value and operator inputs
    @AssertTrue(message = "")
    public boolean isOperatorValidForNumericAttributes(){
        if (attribute == null || operator == null || value == null) {
            // @NotNull will handle null values but is triggered later than this assertion - prevent this assertion throwing null pointer exception
            return true;
        }
        if (!(attribute.equals("cooling") || attribute.equals("heating"))){
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException error){
                    return false;
                }
        }
        return true;
    }
}
