package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Navigator extends Agent {

    private static final Logger LOGGER = LogManager.getLogger(Navigator.class);

    public static final String START = "start";
    public static final String WUMPUS = "wumpus";
    public static final String PIT = "pit";
    public static final String BREEZE = "breeze";
    public static final String STENCH = "stench";
    public static final String SCREAM = "scream";
    public static final String GOLD = "gold";
    public static final String BUMP = "bump";


    private static int ROOM_EXIST = 1;
    private static int ROOM_STENCH = 2;
    private static int ROOM_BREEZE = 3;
    private static int ROOM_PIT = 4;
    private static int ROOM_WAMPUS = 5;
    private static int ROOM_OK = 6;
    private static int ROOM_GOLD = 7;

    public static int ROOM_STATUS_TRUE = 1;
    public static int ROOM_STATUS_FALSE = 2;
    public static int ROOM_STATUS_POSSIBLE = 3;
    public static int ROOM_STATUS_NO_GOLD_WAY = 4;
    public static int ROOM_STATUS_NO_STATUS = -1;


    private static final String SERVICE_DESCRIPTION = "navigator";
    private final String nickname = "navigator";
    private AID id = new AID(nickname, AID.ISLOCALNAME);
    private Hashtable<AID, Room> agentsCoords;
    private Hashtable<AID, List<Room>> agentsWayStory;

    private boolean moveRoom = false;
    private int agentX;
    private int agentY;

    WorldPrediction world;

    @Override
    protected void setup() {
        world = new WorldPrediction();
        agentsWayStory = new Hashtable<>();
        agentsCoords = new Hashtable<>();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-navigator");
        sd.setName(SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new LocationRequestsServer());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator-agent " + getAID().getName() + " terminating.");
    }

    private class LocationRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                AID agent = msg.getSender();
                if (agentsWayStory.get(agent) == null) {
                    LinkedList<int[]> agentWay = new LinkedList<>();
                    agentsWayStory.put(agent, agentWay);
                }
                Room agentPosition = agentsCoords.get(agent);
                if (agentPosition == null) {
                    agentPosition = new Room();
                    agentsCoords.put(agent, agentPosition);
                }
                String location = msg.getContent();
                location = location.substring(1, location.length() - 1);
                String[] roomInfo = location.split(", ");
                String[] actions = getActions(agent, agentPosition, roomInfo);
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Arrays.toString(actions));
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private String[] getActions(AID agent, Room agentCoordinates, String[] roomInfo) {
        LOGGER.debug("Previous agent position: " + agentCoordinates.getX() + " | " + agentCoordinates.getY());
        int[] actions;
        RoomPrediction checkingRoom = world.getWorldGrid().get(agentCoordinates);
        if (checkingRoom == null) {
            checkingRoom = new RoomPrediction();
            world.getWorldGrid().put(agentCoordinates, checkingRoom);
        }

        if (!Arrays.asList(roomInfo).contains(BUMP)) {
            List<Room> agentStory = agentsWayStory.get(agent);
            agentStory.add(agentCoordinates);
            agentCoordinates.setX(agentX);
            agentCoordinates.setY(agentY);
            if (world.getWorldGrid().get(agentCoordinates).getExist() != NavigatorAgent.ROOM_STATUS_TRUE) {
                world.getWorldGrid().get(agentCoordinates).setExist(NavigatorAgent.ROOM_STATUS_TRUE);
                System.out.println("MARKED THE EXISTENCE");
            }
            moveRoom = false;
        } else {
            Room helpPosition = new Room(agentX, agentY);
            world.getWorldGrid().get(helpPosition).setExist(NavigatorAgent.ROOM_STATUS_FALSE);
        }
        checkingRoom = world.getWorldGrid().get(agentCoordinates);
        if (checkingRoom == null) {
            checkingRoom = new RoomPrediction();
            world.getWorldGrid().put(agentCoordinates, checkingRoom);
        }

        if (checkingRoom.getOk() != NavigatorAgent.ROOM_STATUS_TRUE) {
            checkingRoom.setOk(NavigatorAgent.ROOM_STATUS_TRUE);
        }
        for (String event : roomInfo) {
            checkingRoom.(event);
        }
        updateNeighbors(agentCoordinates);
        if (world.isWampusAlive() && world.getWampusRoomCount() > 2) {
            Room wampusPosition = world.getWampusCoords();
            actions = getNextRoomAction(agentCoordinates, wampusPosition, WumpusAction.SHOOT);
        } else {
            Room[] nextOkRooms = getOkNeighbors(agent, agentCoordinates);
            int bestCandidate = -1;
            int candidateStatus = -1;
            for (int i = 0; i < nextOkRooms.length; ++i) {
                Room candidateRoom = nextOkRooms[i];
                System.out.println("CANDIDATE CHECKING: " + candidateRoom.getX() + " " + candidateRoom.getY());
                System.out.println("AGENT CHECKING: " + agentCoordinates.getX() + " " + agentCoordinates.getY());
                if (candidateRoom.getX() > agentCoordinates.getX()) {
                    bestCandidate = i;
                    System.out.println("1");
                    break;
                } else if (candidateRoom.getY() > agentCoordinates.getY()) {
                    if (candidateStatus < 3) {
                        System.out.println("2");
                        candidateStatus = 3;
                    } else continue;
                } else if (candidateRoom.getX() < agentCoordinates.getX()) { // влево
                    if (candidateStatus < 2) {
                        System.out.println("3");
                        candidateStatus = 2;
                    } else continue;
                } else { // вниз
                    if (candidateStatus < 1) {
                        System.out.println("4");
                        candidateStatus = 1;
                    } else continue;
                }
                bestCandidate = i;
            }
            System.out.println("OK ROOMS COUNT IS: " + nextOkRooms.length);
            System.out.println("ADVICE POSITION IS: " + nextOkRooms[bestCandidate].getX() + " | " + nextOkRooms[bestCandidate].getY());
            actions = getNextRoomAction(agentCoordinates, nextOkRooms[bestCandidate], SpeleologistAgent.MOVE);
            System.out.println("ADVICE ACTIONS IS: " + Arrays.toString(actions));
        }

        String[] languageActions = new String[actions.length];
        for (int i = 0; i < actions.length; ++i) {
            languageActions[i] = SpeleologistAgent.actionCodes.get(actions[i]);
        }
        return languageActions;
    }

    private int[] getNextRoomAction(Room agentPosition, Room nextOkRoom, WumpusAction action) {
        agentX = agentPosition.getX();
        agentY = agentPosition.getY();
        int look;
        if (agentPosition.getY() < nextOkRoom.getY()) {
            agentY += 1;
            look = SpeleologistAgent.LOOK_UP;
        } else if (agentPosition.getY() > nextOkRoom.getY()) {
            agentY -= 1;
            look = SpeleologistAgent.LOOK_DOWN;
        } else if (agentPosition.getX() < nextOkRoom.getX()) {
            agentX += 1;
            look = SpeleologistAgent.LOOK_RIGHT;
        } else {
            agentX -= 1;
            look = SpeleologistAgent.LOOK_LEFT;
        }
        moveRoom = true;

        return new int[]{look, action};
    }

    private Room[] getOkNeighbors(AID agent, Room position) {
        Room[] okNeighbors = getNeighborsPosition(position);
        ArrayList<Room> okPositions = new ArrayList<>();
        for (Room position : okNeighbors) {
            this.world.getWorldGrid().putIfAbsent(position, new RoomPrediction()); // если комнаты
            // не существует - добавляем новую комнату на карте
            if (Boolean.TRUE.equals(this.world.getWorldGrid().get(position).getOk())
                    && !Boolean.TRUE.equals(this.world.getWorldGrid().get(position).getNoWay()
                    && !Boolean.FALSE.equals(this.world.getWorldGrid().get(position).getExist())
            ) || this.world.getWorldGrid().get(position).getOk() == null) {
                okPositions.add(position);
            }
        }
        if (okPositions.size() == 0) {
            List<Room> rooms = agentsWayStory.get(agent);
            int x = rooms.get(rooms.size() - 1).getX();
            int y = rooms.get(rooms.size() - 1).getY();
            okPositions.add(new Room(x, y));
            this.world.getWorldGrid().get(position).setNoWay(ROOM_STATUS_TRUE);
        }
        return okPositions.toArray(new Room[0]);
    }

    private RoomPrediction[] getNeighborsImaginaryRoom(Room agentPosition) {
        Room rightNeighbor = new Room(agentPosition.getX() + 1, agentPosition.getY());
        Room upNeighbor = new Room(agentPosition.getX(), agentPosition.getY() + 1);
        Room leftNeighbor = new Room(agentPosition.getX() - 1, agentPosition.getY());
        Room bottomNeighbor = new Room(agentPosition.getX(), agentPosition.getY() - 1);
        RoomPrediction rightRoom = world.getWorldGrid().get(rightNeighbor);
        if (rightRoom == null) {
            rightRoom = new RoomPrediction();
            world.getWorldGrid().put(rightNeighbor, rightRoom);
        }
        RoomPrediction upRoom = world.getWorldGrid().get(upNeighbor);
        if (upRoom == null) {
            upRoom = new RoomPrediction();
            world.getWorldGrid().put(rightNeighbor, upRoom);
        }
        RoomPrediction leftRoom = world.getWorldGrid().get(leftNeighbor);
        if (leftRoom == null) {
            leftRoom = new RoomPrediction();
            world.getWorldGrid().put(rightNeighbor, leftRoom);
        }
        RoomPrediction bottomRoom = world.getWorldGrid().get(bottomNeighbor);
        if (bottomRoom == null) {
            bottomRoom = new RoomPrediction();
            world.getWorldGrid().put(rightNeighbor, bottomRoom);
        }
        return new RoomPrediction[]{rightRoom, upRoom, leftRoom, bottomRoom};
    }

    private Room[] getNeighborsPosition(Room position) {
        Room rightNeighbor = new Room(position.getX() + 1, position.getY());
        Room upNeighbor = new Room(position.getX(), position.getY() + 1);
        Room leftNeighbor = new Room(position.getX() - 1, position.getY());
        Room bottomNeighbor = new Room(position.getX(), position.getY() - 1);

        return new Room[]{rightNeighbor, upNeighbor, leftNeighbor, bottomNeighbor};
    }

    private void updateNeighbors(Room position) {
        RoomPrediction currentRoom = world.getWorldGrid().get(position);
        RoomPrediction[] roomList = getNeighborsImaginaryRoom(position);

        if (currentRoom.getStench() == NavigatorAgent.ROOM_STATUS_TRUE) {
            world.setWampusRoomCount(world.getWampusRoomCount() + 1);
            for (RoomPrediction room : roomList) {
                if (room.getWampus() == NavigatorAgent.ROOM_STATUS_NO_STATUS) {
                    room.setOk(NavigatorAgent.ROOM_STATUS_POSSIBLE);
                    room.setWampus(NavigatorAgent.ROOM_STATUS_POSSIBLE);
                }
            }
        }
        if (currentRoom.getBreeze() == NavigatorAgent.ROOM_STATUS_TRUE) {
            for (RoomPrediction room : roomList) {
                if (room.getPit() == NavigatorAgent.ROOM_STATUS_NO_STATUS) {
                    room.setOk(NavigatorAgent.ROOM_STATUS_POSSIBLE);
                    room.setPit(NavigatorAgent.ROOM_STATUS_POSSIBLE);
                }
            }
        }
        if (currentRoom.getBreeze() == false && currentRoom.getStench() == NavigatorAgent.ROOM_STATUS_FALSE) {
            for (RoomPrediction room : roomList) {
                room.setOk(NavigatorAgent.ROOM_STATUS_TRUE);
                room.setWampus(NavigatorAgent.ROOM_STATUS_FALSE);
                room.setPit(NavigatorAgent.ROOM_STATUS_FALSE);
            }
        }
    }

}

