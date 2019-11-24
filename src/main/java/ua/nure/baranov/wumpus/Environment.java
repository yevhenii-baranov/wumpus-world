package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;

import java.util.List;

public class Environment extends Agent {

    private Field[][] gameField;

    private AID speleologistId;
    private Coordinates speleologistCoordinates;
    private Direction speleologistDirection;

    private List<Coordinates> pitsCoordinates;

    private List<Coordinates> goldCoordinates;
    private List<Coordinates> wumpusCoordinates;

    private long currentTimeTact;

}
