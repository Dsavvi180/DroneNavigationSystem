// package uk.ac.ed.acp.cw2.dtos;
//
// import jakarta.validation.constraints.Negative;
// import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.Positive;
//
// import lombok.*;
//
// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// public class PositionDto {
//    // Jackson coerces "" to 0.0 for primitive field double so we use Double
//    private @NotNull Double lng;
//    private @NotNull Double lat;
// }

package uk.ac.ed.acp.cw2.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString // Keep ToString for debugging
public class PositionDto {
  private @NotNull Double lng;
  private @NotNull Double lat;

  private static final double PRECISION = 1000000.0;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PositionDto that = (PositionDto) o;

    return Math.round(this.lng * PRECISION) == Math.round(that.lng * PRECISION)
        && Math.round(this.lat * PRECISION) == Math.round(that.lat * PRECISION);
  }

  @Override
  public int hashCode() {

    return Objects.hash(Math.round(this.lng * PRECISION), Math.round(this.lat * PRECISION));
  }
}
