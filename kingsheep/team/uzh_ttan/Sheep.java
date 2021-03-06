package kingsheep.team.uzh_ttan;

import kingsheep.*;

public class Sheep extends UzhTtanCreature {
    private Type myType;

    public Sheep(Type type, Simulator parent, int playerID, int x, int y) {
        super(type, parent, playerID, x, y);
    }


    protected void think(Type map[][]) {
		/*
		TODO
		YOUR SHEEP CODE HERE
		
		BASE YOUR LOGIC ON THE INFORMATION FROM THE ARGUMENT map[][]
		
		YOUR CODE NEED TO BE DETERMINISTIC. 
		THAT MEANS, GIVEN A DETERMINISTIC OPPONENT AND MAP THE ACTIONS OF YOUR SHEEP HAVE TO BE REPRODUCIBLE
		
		SET THE MOVE VARIABLE TO ONE TOF THE 5 VALUES
        move = Move.UP;
        move = Move.DOWN;
        move = Move.LEFT;
        move = Move.RIGHT;
        move = Move.WAIT;
		*/

		this.myType = Type.SHEEP1;
        int[][] mapInt = convertMap(map);

        move = getMinMaxAction(mapInt, this.myType);
    }
}
