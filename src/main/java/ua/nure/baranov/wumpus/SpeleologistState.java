package ua.nure.baranov.wumpus;

public enum SpeleologistState {
    ALIVE,
    DEAD,
    GOAL;

    public static SpeleologistState fromString(String state) {
        for (SpeleologistState enumState: SpeleologistState.values()) {
            if (state.equals(enumState.name())) {
                return enumState;
            }
        }
        return null;
    }
}
