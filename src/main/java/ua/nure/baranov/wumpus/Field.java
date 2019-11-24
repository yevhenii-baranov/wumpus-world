package ua.nure.baranov.wumpus;

public class Field {
    private int x;
    private int y;
    private boolean pit;
    private boolean wumpus;
    private boolean wumpusAlive;
    private boolean gold;

    public Field(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setWumpus(boolean wumpus) {
        this.wumpus = wumpus;
        this.wumpusAlive = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean hasPit() {
        return pit;
    }

    public boolean hasWumpus() {
        return wumpus;
    }

    public boolean isWumpusAlive() {
        return wumpusAlive;
    }

    public boolean hasGold() {
        return gold;
    }

    public void setGold(boolean gold) {
        this.gold = gold;
    }

    public void setWumpusAlive(boolean wumpusAlive) {
        this.wumpusAlive = wumpusAlive;
    }
}
