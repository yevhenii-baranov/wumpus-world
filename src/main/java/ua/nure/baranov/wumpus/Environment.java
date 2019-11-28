package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class Environment extends Agent {

    private final int caveXDimension;
    private final int caveYDimension;

    private AID speleologistId;
    private AgentPosition spelPosition = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH);
    private boolean agentAlive = true;
    private boolean agentHasArrow = true;
    private boolean isGoldGrabbed = false;
    private boolean isWumpusAlive = true;

    private Room wumpus;
    private Room gold;
    private Set<Room> pits = new LinkedHashSet<>();
    private AID agentJustKillingWumpus;

    private Set<AID> bumpedAgents;
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

    public Environment(int caveXDimension, int caveYDimension, String config) {
        this(caveXDimension, caveYDimension);
        if (config.length() != 2 * caveXDimension * caveYDimension)
            throw new IllegalStateException("Wrong configuration length.");
        for (int i = 0; i < config.length(); i++) {
            char c = config.charAt(i);
            Room r = new Room(i / 2 % caveXDimension + 1, caveYDimension - i / 2 / caveXDimension);
            switch (c) {
                case 'S': spelPosition = new AgentPosition(r.getX(), r.getY(), AgentPosition.Orientation.FACING_NORTH); break;
                case 'W': wumpus = r; break;
                case 'G': gold = r; break;
                case 'P': pits.add(r); break;
            }
        }
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

    @Override
    protected void setup() {
        registerAgent();
    }

    private void registerAgent() {
        final DFAgentDescription description = new DFAgentDescription();
        description.setName(this.getAID());
        final ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("wumpus-environment");
        serviceDescription.setName("wumpus-world-environment");
        description.addServices(serviceDescription);

        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private static class MessagingBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage message = myAgent.receive();
            if (message != null) {
                ACLMessage response = new ACLMessage(ACLMessage.REQUEST);

                response.setContent(null);

                String replyMarker = String.format("nav req %d", System.currentTimeMillis());

                response.setConversationId(null);
                response.setReplyWith(replyMarker);
                response.addReceiver(message.getSender());
                this.myAgent.send(response);
            }
        }
    }

    public WumpusPercept getPerceptSeenBy(AID agent) {
        WumpusPercept result = new WumpusPercept();
        AgentPosition pos = spelPosition;
        List<Room> adjacentRooms = Arrays.asList(
                new Room(pos.getX()-1, pos.getY()), new Room(pos.getX()+1, pos.getY()),
                new Room(pos.getX(), pos.getY()-1), new Room(pos.getX(), pos.getY()+1)
        );
        for (Room r : adjacentRooms) {
            if (r.equals(wumpus))
                result.setStench();
            if (pits.contains(r))
                result.setBreeze();
        }
        if (pos.getRoom().equals(gold))
            result.setGlitter();
        if (bumpedAgents.contains(agent))
            result.setBump();
        if (agentJustKillingWumpus != null)
            result.setScream();
        return result;
    }

    public void execute(AID agent, WumpusAction action) {
        bumpedAgents.remove(agent);
        if (agent == agentJustKillingWumpus)
            agentJustKillingWumpus = null;
        AgentPosition pos = spelPosition;
        switch (action) {
            case FORWARD:
                AgentPosition newPos = moveForward(pos);
                spelPosition = newPos;
                if (newPos.equals(pos)) {
                    bumpedAgents.add(agent);
                } else if (pits.contains(newPos.getRoom()) || (newPos.getRoom().equals(wumpus) && isWumpusAlive)){
                    agentAlive = false;
                }
                break;
            case TURN_LEFT:
                spelPosition = turnLeft(pos);
                break;
            case TURN_RIGHT:
                spelPosition = turnRight(pos);
                break;
            case GRAB:
                if (!isGoldGrabbed && pos.getRoom().equals(gold))
                    isGoldGrabbed = true;
                break;
            case SHOOT:
                if (agentHasArrow && isAgentFacingWumpus(pos)) {
                    isWumpusAlive = false;
                    agentHasArrow = false;
                    agentJustKillingWumpus = agent;
                }
                break;
            case CLIMB:
                agentAlive = false;
        }
    }

    private boolean isAgentFacingWumpus(AgentPosition pos) {
        Room wumpus = this.wumpus;
        switch (pos.getOrientation()) {
            case FACING_NORTH:
                return pos.getX() == wumpus.getX() && pos.getY() < wumpus.getY();
            case FACING_SOUTH:
                return pos.getX() == wumpus.getX() && pos.getY() > wumpus.getY();
            case FACING_EAST:
                return pos.getY() == wumpus.getY() && pos.getX() < wumpus.getX();
            case FACING_WEST:
                return pos.getY() == wumpus.getY() && pos.getX() > wumpus.getX();
        }
        return false;
    }

    public AgentPosition moveForward(AgentPosition position) {
        int x = position.getX();
        int y = position.getY();
        switch (position.getOrientation()) {
            case FACING_NORTH: y++; break;
            case FACING_SOUTH: y--; break;
            case FACING_EAST: x++; break;
            case FACING_WEST: x--; break;
        }
        Room room = new Room(x, y);
        return allowedRooms.contains(room) ? new AgentPosition(x, y, position.getOrientation()) : position;
    }

    public AgentPosition turnLeft(AgentPosition position) {
        AgentPosition.Orientation orientation = null;
        switch (position.getOrientation()) {
            case FACING_NORTH: orientation = AgentPosition.Orientation.FACING_WEST; break;
            case FACING_SOUTH: orientation = AgentPosition.Orientation.FACING_EAST; break;
            case FACING_EAST: orientation = AgentPosition.Orientation.FACING_NORTH; break;
            case FACING_WEST: orientation = AgentPosition.Orientation.FACING_SOUTH; break;
        }
        return new AgentPosition(position.getX(), position.getY(), orientation);
    }

    public AgentPosition turnRight(AgentPosition position) {
        AgentPosition.Orientation orientation = null;
        switch (position.getOrientation()) {
            case FACING_NORTH: orientation = AgentPosition.Orientation.FACING_EAST; break;
            case FACING_SOUTH: orientation = AgentPosition.Orientation.FACING_WEST; break;
            case FACING_EAST: orientation = AgentPosition.Orientation.FACING_SOUTH; break;
            case FACING_WEST: orientation = AgentPosition.Orientation.FACING_NORTH; break;
        }
        return new AgentPosition(position.getX(), position.getY(), orientation);
    }
}
