package ua.nure.baranov.wumpus;

import java.util.Hashtable;
import java.util.Set;

class WorldPrediction {

    private Hashtable<Room, RoomPrediction> worldGrid;
    private boolean isWampusAlive;
    private int wampusRoomCount;
    private Room wampusCoords;

    WorldPrediction() {
        worldGrid = new Hashtable<>();
        isWampusAlive = true;
        wampusRoomCount = 0;
    }

    public Room getWampusCoords() {
        int xWampusCoord = 0;
        int yWampusCoord = 0;

        Set<Room> keys = worldGrid.keySet();
        for (Room roomPosition : keys) {
            RoomPrediction room = worldGrid.get(roomPosition);
            if (room.getWampus() == NavigatorAgent.ROOM_STATUS_POSSIBLE) {
                xWampusCoord += roomPosition.getX();
                yWampusCoord += roomPosition.getY();
            }
        }
        xWampusCoord /= wampusRoomCount;
        yWampusCoord /= wampusRoomCount;
        this.wampusCoords = new Room(xWampusCoord, yWampusCoord);
        return this.wampusCoords;
    }

    public Hashtable<Room, RoomPrediction> getWorldGrid() {
        return worldGrid;
    }


    public boolean isWampusAlive() {
        return isWampusAlive;
    }

    public void setWampusAlive(boolean wampusAlive) {
        isWampusAlive = wampusAlive;
    }

    public int getWampusRoomCount() {
        return wampusRoomCount;
    }

    public void setWampusRoomCount(int wampusRoomCount) {
        this.wampusRoomCount = wampusRoomCount;
    }
}
