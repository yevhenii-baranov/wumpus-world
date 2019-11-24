package ua.nure.baranov.wumpus;

import jade.content.Predicate;

public class WumpusPerception implements Predicate {

    private String stench;
    private String breeze;
    private String glitter;
    private String bump;
    private String scream;
    private String state;

    public WumpusPerception() {}

    public WumpusPerception(String perceptionString) {

        this.toObject(perceptionString);
    }

    private void toObject(String perceptionString) {
        perceptionString = perceptionString.substring(1, perceptionString.length()-1);
        String[] result = perceptionString.split(",");
        if (result.length != 6)
            throw new RuntimeException("Size of the tuple does not correspond " +
                    "to the expected size: " + result.length);

/*        this.stench = (result[0].equals(WumpusConsts.SENSOR_STENCH) ? result[0] : WumpusConsts.SENSOR_DEFAULT);
        this.breeze = (result[1].equals(WumpusConsts.SENSOR_BREEZE) ? result[1] : WumpusConsts.SENSOR_DEFAULT);
        this.glitter = (result[2].equals(WumpusConsts.SENSOR_GLITTER) ? result[2] : WumpusConsts.SENSOR_DEFAULT);
        this.bump = (result[3].equals(WumpusConsts.SENSOR_BUMP) ? result[3] : WumpusConsts.SENSOR_DEFAULT);
        this.scream = (result[4].equals(WumpusConsts.SENSOR_SCREAM) ? result[4] : WumpusConsts.SENSOR_DEFAULT);

        this.state = (isValidState(result[5]) ? result[5] : "");*/
    }

    public void setState(String state) {
        if(isValidState(state)) {
            this.state = state;
        } else {
            throw new RuntimeException("Invalid state reached: " + state);
        }
    }

    public String getState() {
        if(isValidState(this.state)){
            return this.state;
        }

        return null;
    }

    private boolean isValidState(String state) {
        SpeleologistState stateEnum = SpeleologistState.fromString(state);

        return SpeleologistState.ALIVE.equals(stateEnum)
                || SpeleologistState.DEAD.equals(stateEnum)
                || SpeleologistState.GOAL.equals(stateEnum);
    }
}