package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Environment extends Agent {

    private final int caveXDimension;
    private final int caveYDimension;


    private AID speleologistId;
    private Coordinates speleologistCoordinates;
    private Direction speleologistDirection;

    private Room wumpus;
    private Room gold;
    private Set<Room> pits = new LinkedHashSet<>();

    private Set<Room> allowedRooms;

    private long currentTimeTact;

    public Environment(int caveXDimension, int caveYDimension) {
        if (caveXDimension < 1)
            throw new IllegalArgumentException("Cave must have x dimension >= 1");
        if (caveYDimension < 1)
            throw new IllegalArgumentException("Case must have y dimension >= 1");
        this.caveXDimension = caveXDimension;
        this.caveYDimension = caveYDimension;
        allowedRooms = getAllRooms();
    }

    private Set<Room> getAllRooms() {
        Set<Room> rooms = new HashSet<>();
        for (int i = 0; i < caveXDimension; i++) {
            for (int j = 0; j < caveYDimension; j++) {
                Room room = new Room(i, j);
                rooms.add(room);
            }
        }
        return rooms;
    }
}
