package uk.ac.ed.acp.cw2.service.Astar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ed.acp.cw2.dtos.PositionDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AStarResult {
  private List<PositionDto> path;

  private int movesUsed;

  private double totalCost;

  private boolean reachedGoal;
}
