package uk.ac.ed.acp.cw2.service.Astar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ed.acp.cw2.dtos.PositionDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
class AStarNode {

  private PositionDto position;

  private double gCost;

  private double hCost;

  private double fCost;

  private int movesUsed;

  private AStarNode parent;
}
