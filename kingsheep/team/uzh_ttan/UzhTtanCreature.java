package kingsheep.team.uzh_ttan;

import kingsheep.Creature;
import kingsheep.Simulator;
import kingsheep.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.lang.Math;

/**
 * Author: Te Tan, University of Zurich, March 2018.
 */
public abstract class UzhTtanCreature extends Creature {

    public UzhTtanCreature(Type type, Simulator parent, int playerID, int x, int y) {
        super(type, parent, playerID, x, y);
    }

    public String getNickname() {
        //TODO change this to any nickname you like. This should not be your uzh_ttan. That way you can stay anonymous on the ranking list.
        return "Kingslayer Nelyo";
    }

    class State {
        private int map[][];
        private Type moveTarget;
        private Move howToGetHere;
        private double moveReward;
        private int depth;
        private double minMax = Double.NEGATIVE_INFINITY;
        /** opponent's sheep and wolf's position, -1 in case it does not exist */
        private int myX, myY, oppoSheepX, oppoSheepY, oppoWolfX, oppoWolfY;

        public State(int map[][], Move move, Type moveTarget) {
            this.map = new int[map.length][];
            for(int i = 0; i < map.length; i++)
                this.map[i] = map[i].clone();
            this.howToGetHere = move;
            this.moveReward = 0;
            this.moveTarget = moveTarget;
            int[] coordinates = locateOpponentCreature(this.map, moveTarget);
            this.myX = coordinates[0];
            this.myY = coordinates[1];
            this.oppoSheepX = coordinates[2];
            this.oppoSheepY = coordinates[3];
            this.oppoWolfX = coordinates[4];
            this.oppoWolfY = coordinates[5];
        }

        private State setDepth(int depth) {
            this.depth = depth;
            return this;
        }

        private void setMinMax(double minMax) {
            this.minMax = minMax;
        }

        private State setMoveReward(double reward) {
            this.moveReward = reward;
            return this;
        }

    }


    protected Move getMinMaxAction(int map[][], Type moveTarget) {
        State root = new State(map, null, moveTarget).setDepth(0);
        /** search depth of MinMax tree */
        int searchDepth = 4;

        ArrayList<State> successors = getSuccessorStates(root, true, moveTarget);
        if (successors.size() == 0) {
            return Move.WAIT;
        }

        Iterator<State> iter = successors.iterator();
        State optimalState = iter.next();
        optimalState.setMinMax(minMax(optimalState, false, searchDepth, moveTarget));

        while (iter.hasNext()) {
            State next = iter.next();
            double minMax = minMax(next, false, searchDepth, moveTarget);
//            if (moveTarget == Type.SHEEP1) System.out.println("minMax of sheep: " + minMax + ", locate: " + optimalState.myX + ", " + optimalState.myY);
            if (minMax > optimalState.minMax) {
                next.setMinMax(minMax);
                optimalState = next;
            }
        }

        /*if (moveTarget == Type.SHEEP1) {
            System.out.println("value from Grass: " + computeValueFromGrass(optimalState) + ". Move: " + optimalState.howToGetHere);
            System.out.println("Sheep move reward: " + optimalState.moveReward + ". Move: " + optimalState.howToGetHere);
        }
        if (moveTarget == Type.WOLF1) {
            System.out.println("Wolf move reward: " + optimalState.moveReward + ". Move: " + optimalState.howToGetHere);
        }*/

        return optimalState.howToGetHere;
    }


    protected Move getAStarAction(int[][] map, Type moveTarget) {

        return Move.WAIT;
    }

    protected double minMax(State state, boolean isMaxTurn, int searchDepth, Type moveTarget) {
        if (state.depth >= searchDepth) {
            return evaluation(state);
        } else if (isMaxTurn) {
            ArrayList<State> successors = getSuccessorStates(state, true, moveTarget);
            if (successors.size() == 0) return Double.NEGATIVE_INFINITY;

            Iterator<State> iter = successors.iterator();
            double value = minMax(iter.next(), false, searchDepth, moveTarget);
            while (iter.hasNext()) {
                double nextValue = minMax(iter.next(), false, searchDepth, moveTarget);
                value = nextValue > value? nextValue : value;
            }
            return value;
        } else {
            ArrayList<State> successors = getSuccessorStates(state, false, moveTarget);
            if (successors.size() == 0) return Double.POSITIVE_INFINITY;

            Iterator<State> iter = successors.iterator();
            double value = minMax(iter.next(), true, searchDepth, moveTarget);
            while (iter.hasNext()) {
                double nextValue = minMax(iter.next(), true, searchDepth, moveTarget);
                value = nextValue < value? nextValue : value;
            }
            return value;
        }
    }

    protected double evaluation(State state) {
        if (isSheepCatched(state)) return 100;

        double value = state.moveReward;
        /** sheep agent */
        if (this.type == Type.SHEEP1 || this.type == Type.SHEEP2) {
            /** initialize value as a function which evaluate the threat from opponent wolf */
            value += computeThreatFromWolf(state, (double) (state.map.length+state.map[0].length));
//            System.out.println("Threat from Wolf: " + value + ", move: " + state.howToGetHere);
            value += computeValueFromGrass(state);
        } else { /** Wolf agent */
//            value = -Math.sqrt(Math.pow(Math.abs(state.oppoSheepX - state.myX),2) + Math.pow(Math.abs(state.oppoSheepY - state.myY),2));
            value = - (Math.abs(state.oppoSheepX - state.myX) + Math.abs(state.oppoSheepY - state.myY));
//            value += computeValueFromGrass(state);
            value += computeValueForOpponentSheep(state);
        }
        return value;
    }

    protected double computeThreatFromWolf(State state, double mapLength) {
        /** my sheep's distance to opponent wolf, normalized within 0 to 1 */
        double distToWolf = (Math.abs(state.myX-state.oppoWolfX)+Math.abs(state.myY-state.oppoWolfY))/mapLength;
        /** if opponent wolf is far away, just ignore it */
        if (distToWolf >= 0.2) return 0;

        /** else do non-linear transformation with log function */
        double threatFromWolf = 0.5 * mapLength * Math.log(distToWolf);
        /** penalty of getting close to edge of the map */
        double distanceToEdge = 0.6 * (double) Math.min(state.myX, Math.abs(state.myX-state.map[0].length)) + Math.min(state.myY, Math.abs(state.myY-state.map.length));
        return threatFromWolf + distanceToEdge;
    }

    protected double computeValueFromGrass(State state) {
        double value = 0;
        for (int i = 0; i < state.map.length; ++i)
        {
            for (int j = 0; j < state.map[0].length; ++j)
            {
                if (state.map[i][j] == 1) { // GRASS
                    int distance = Math.abs(j-state.myX) + Math.abs(i-state.myY);
                    value -= distance;
                } else if (state.map[i][j] == 3) { // RHUBARB
                    int distance = Math.abs(j-state.myX) + Math.abs(i-state.myY);
                    value -= 5*distance;
                }
            }
        }
        return value;
    }

    protected double computeValueOfGrassInGroup(State state) {
        Double[] values = {0.0, 0.0, 0.0, 0.0};
        for (int i = 0; i < state.map.length; ++i)
        {
            for (int j = 0; j < state.map[0].length; ++j)
            {
                if (state.map[i][j] == 1 || state.map[i][j] == 3) { // GRASS
                    int weight = 1;
                    if (state.map[i][j] == 3) weight = 5;
                    int distance = weight * Math.abs(j-state.myX) + Math.abs(i-state.myY);

                    if ((j-state.myX) < 0 && (i-state.myX) < 0) values[0] -= (double) distance;
                    else if ((j-state.myX) >= 0 && (i-state.myX) < 0) values[1] -= (double) distance;
                    else if ((j-state.myX) < 0 && (i-state.myX) >= 0) values[2] -= (double) distance;
                    else values[3] -= (double) distance;
                }
            }
        }
        return Collections.min(Arrays.asList(values));
    }

    protected double computeValueForOpponentSheep(State state) {
        double value = 0;
        for (int i = 0; i < state.map.length; ++i)
        {
            for (int j = 0; j < state.map[0].length; ++j)
            {
                if (state.map[i][j] == 1) { // GRASS
                    int distance = Math.abs(j-state.oppoSheepX) + Math.abs(i-state.oppoSheepY);
                    value += distance;
                } else if (state.map[i][j] == 3) { // RHUBARB
                    int distance = Math.abs(j-state.oppoSheepX) + Math.abs(i-state.oppoSheepY);
                    value += 5*distance;
                }
            }
        }
        return value;
    }

    protected ArrayList<State> getSuccessorStates(State state, boolean isMaxTurn, Type moveTarget) {
        ArrayList<State> successors = new ArrayList<>();

        /** if is MAX's turn, move my own agent */
        if (isMaxTurn) {
            // discount factor for computing accumulated rewards
            double rewardDiscount = 0.9;

            State up = moveOneStep(state, Move.UP, moveTarget, true);
            if (up != null) {
                up.setDepth(state.depth+1);
                successors.add(up.setMoveReward(up.moveReward * Math.pow(rewardDiscount, up.depth)));
            }
            State down = moveOneStep(state, Move.DOWN, moveTarget, true);
            if (down != null) {
                down.setDepth(state.depth+1);
                successors.add(down.setMoveReward(down.moveReward * Math.pow(rewardDiscount, down.depth)));
            }
            State left = moveOneStep(state, Move.LEFT, moveTarget, true);
            if (left != null) {
                left.setDepth(state.depth+1);
                successors.add(left.setMoveReward(left.moveReward * Math.pow(rewardDiscount, left.depth)));
            }
            State right = moveOneStep(state, Move.RIGHT, moveTarget, true);
            if (right != null) {
                right.setDepth(state.depth+1);
                successors.add(right.setMoveReward(right.moveReward * Math.pow(rewardDiscount, right.depth)));
            }
            /** wait is proven to be a nice strategy when catching opponent sheep */
            State wait = moveOneStep(state, Move.WAIT, moveTarget, true);
            if (wait != null) {
                if (moveTarget == Type.WOLF1 || (moveTarget == Type.SHEEP1 && noGrassExist(state)))
                successors.add(wait.setDepth(state.depth + 1));
            }
        } else {
            /** if is MIN's turn, move the opponent sheep and wolf */
            State upS = moveOneStep(state.map, Move.UP, moveTarget, Type.SHEEP2);
            if (upS != null) successors.add(upS.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State downS = moveOneStep(state.map, Move.DOWN, moveTarget, Type.SHEEP2);
            if (downS != null) successors.add(downS.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State leftS = moveOneStep(state.map, Move.LEFT, moveTarget, Type.SHEEP2);
            if (leftS != null) successors.add(leftS.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State rightS = moveOneStep(state.map, Move.RIGHT, moveTarget, Type.SHEEP2);
            if (rightS != null) successors.add(rightS.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State upW = moveOneStep(state.map, Move.UP, moveTarget, Type.WOLF2);
            if (upW != null) successors.add(upW.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State downW = moveOneStep(state.map, Move.DOWN, moveTarget, Type.WOLF2);
            if (downW != null) successors.add(downW.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State leftW = moveOneStep(state.map, Move.LEFT, moveTarget, Type.WOLF2);
            if (leftW != null) successors.add(leftW.setDepth(state.depth+1).setMoveReward(state.moveReward));
            State rightW = moveOneStep(state.map, Move.RIGHT, moveTarget, Type.WOLF2);
            if (rightW != null) successors.add(rightW.setDepth(state.depth+1).setMoveReward(state.moveReward));
        }

        return successors;
    }

    /** move one step function for MAX player */
    protected State moveOneStep(State state, Move move, Type moveTarget, boolean myMove) {
        int[][] map = state.map;
        if (move == Move.WAIT) return new State(map, move, moveTarget);

        int di = 0, dj = 0;
        if (move == Move.DOWN) {
            di = 1;
        } else if (move == Move.UP) {
            di = -1;
        } else if (move == Move.LEFT) {
            dj = -1;
        } else if (move == Move.RIGHT) {
            dj = 1;
        }

        try {
            State newState = new State(map, move, moveTarget);
            newState.setMoveReward(state.moveReward);

            int nextType = map[newState.myY + di][newState.myX + dj];
            if (isPlaceholder(nextType) && (! (moveTarget == Type.WOLF1 && nextType == 5))) {
                return null;
            } else {
                if (moveTarget == Type.SHEEP1) {
                    if (nextType == 1) {
                        newState.setMoveReward(newState.moveReward + 1);  // immediate reward for SHEEP1
                    } else if (nextType == 3) {
                        newState.setMoveReward(newState.moveReward + 5);
                    } else {
                        newState.setMoveReward(newState.moveReward - 0.5);
                    }
                    newState.map[newState.myY + di][newState.myX + dj] = 4; // SHEEP1
                } else {
                    if (nextType == 1) {
                        newState.setMoveReward(newState.moveReward - 1);  // immediate reward for SHEEP1
                    } else if (nextType == 3) {
                        newState.setMoveReward(newState.moveReward - 5);
                    } else if (nextType == 5) {
                        newState.setMoveReward(newState.moveReward + 100);
                    }
                    newState.map[newState.myY + di][newState.myX + dj] = 6; // WOLF1
                }
                newState.map[newState.myY][newState.myX] = 0; // EMPTY
                newState.myY += di;
                newState.myX += dj;
                return newState;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /** move one step function for MIN player */
    protected State moveOneStep(int map[][], Move move, Type moveTarget, Type type) {
        if (move == Move.WAIT) return new State(map, move, moveTarget);

        int di = 0, dj = 0;
        if (move == Move.DOWN) {
            di = 1;
        } else if (move == Move.UP) {
            di = -1;
        } else if (move == Move.LEFT) {
            dj = -1;
        } else if (move == Move.RIGHT) {
            dj = 1;
        }
        if (di == 0 && dj == 0) {
            return null;
        }

        int oppoX, oppoY;
        State state = new State(map, move, moveTarget);
        if (type == Type.SHEEP2){
            if (state.oppoSheepX == -1 || state.oppoSheepY == -1) {
                return null;
            } else {
                oppoX = state.oppoSheepX;
                oppoY = state.oppoSheepY;
            }
        } else {
            if (state.oppoWolfX == -1 || state.oppoWolfY == -1) {
                return null;
            } else {
                oppoX = state.oppoWolfX;
                oppoY = state.oppoWolfY;
            }
        }

        try {
            int nextType = map[oppoY + di][oppoX + dj];
            if (isPlaceholder(nextType)) {
                return null;
            } else {
                state.map[oppoY + di][oppoX + dj] = type == Type.SHEEP2 ? 5 : 7;
                state.map[oppoY][oppoX] = 0;
                if (type == Type.SHEEP2) {
                    state.oppoSheepX = oppoX + dj;
                    state.oppoSheepY = oppoY + di;
                } else {
                    state.oppoWolfX = oppoX + dj;
                    state.oppoWolfY = oppoY + di;
                }
                return state;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    protected boolean isPlaceholder(int type) {
        return type == 2 || type == 4 || type == 6 || type == 5 || type == 7;
    }

    protected int[] locateOpponentCreature(int map[][], Type type) {
        int[] coordinates = {-1, -1, -1, -1, -1, -1};

        for (int i = 0; i < map.length; ++i)
        {
            for (int j = 0; j < map[0].length; ++j)
            {
                if (map[i][j] == type.ordinal()) {
                    coordinates[0] = j;
                    coordinates[1] = i;
                    continue;
                }
                if (map[i][j] == 5) {   // if the element is SHEEP2
                    coordinates[2] = j;
                    coordinates[3] = i;
                    continue;
                }
                if (map[i][j] == 7) {   // if the element is WOLF2
                    coordinates[4] = j;
                    coordinates[5] = i;
                }
            }
        }
        return coordinates;
    }

    protected int[][] convertMap(Type[][] map) {
        int[][] mapInt = new int[map.length][map[0].length];
        for (int i = 0; i < map.length; ++i)
        {
            for (int j = 0; j < map[0].length; ++j)
            {
                mapInt[i][j] = map[i][j].ordinal();
            }
        }
        return mapInt;
    }

    protected boolean isSheepCatched(State state) {
        boolean isCatched = true;
        for (int i = 0; i < state.map.length; ++i)
        {
            for (int j = 0; j < state.map[0].length; ++j)
            {
                if (state.map[i][j] == 5) {
                    isCatched = false;
                }
            }
        }
        return isCatched;
    }

    protected boolean noGrassExist(State state) {
        boolean noGrass = true;
        for (int i = 0; i < state.map.length; ++i)
        {
            for (int j = 0; j < state.map[0].length; ++j)
            {
                if (state.map[i][j] == 1 || state.map[i][j] == 3) {
                    noGrass = false;
                }
            }
        }
        return noGrass;
    }

}
