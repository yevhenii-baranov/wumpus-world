package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static jade.lang.acl.MessageTemplate.MatchInReplyTo;

public class Speleologist extends Agent {

    private AID navigator;
    private AID environment;
    private WumpusAction lastAction;
    private static final String SPELEOLOGIST_NAVIGATOR_CONVERSATION_ID = "speleologist-navigator";
    private static final String SPELEOLOGIST_ENVIRONMENT_CONVERSATION_ID = "speleologist-environment";
    private MessageTemplate expectedMessageTemplate;

    @Override
    protected void setup() {
        registerAgent();
        String environmentType = "wumpus-environment";
        String navigatorType = "wumpus-navigator";

        this.environment = findService(environmentType);
        this.navigator = findService(navigatorType);

        addBehaviour(new SpeleologistBehaviour());
    }

    private AID findService(String serviceType) {
        final DFAgentDescription templateDescription = new DFAgentDescription();
        final ServiceDescription serviceDescription = new ServiceDescription();

        serviceDescription.setType(serviceType);
        templateDescription.addServices(serviceDescription);

        try {
            final DFAgentDescription[] result = DFService.search(this, templateDescription);
            if (result.length != 1) {
                throw new RuntimeException("Size of found agents does not equal to 1!");
            }
            return result[0].getName();

        } catch (FIPAException e) {
            throw new RuntimeException();
        }
    }

    private void registerAgent() {
        final DFAgentDescription description = new DFAgentDescription();
        description.setName(this.getAID());
        final ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("book-selling");
        serviceDescription.setName("JADE-quickstart-book-seller");
        description.addServices(serviceDescription);
        //todo update for speleologist
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class SpeleologistBehaviour extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0: {
                    ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);

                    aclMessage.setConversationId(SPELEOLOGIST_ENVIRONMENT_CONVERSATION_ID);

                    String replyMarker = String.format("env req %d", System.currentTimeMillis());
                    expectedMessageTemplate = MatchInReplyTo(replyMarker);

                    aclMessage.setReplyWith(replyMarker);
                    aclMessage.addReceiver(environment);
                    this.myAgent.send(aclMessage);
                    step = 1;
                    break;
                }
                case 1: {
                    ACLMessage response = myAgent.receive(expectedMessageTemplate);
                    if (response != null) {
                        WumpusPercept perception = new WumpusPercept();
                        // TODO: 11/24/2019  
                        ACLMessage navigatorRequest = new ACLMessage(ACLMessage.REQUEST);

                        navigatorRequest.setContent(fromPerception(perception));

                        String replyMarker = String.format("nav req %d", System.currentTimeMillis());
                        expectedMessageTemplate = MatchInReplyTo(replyMarker);

                        navigatorRequest.setConversationId(SPELEOLOGIST_NAVIGATOR_CONVERSATION_ID);
                        navigatorRequest.setReplyWith(replyMarker);
                        navigatorRequest.addReceiver(navigator);
                        this.myAgent.send(navigatorRequest);
                        step = 2;
                    } else {
                        block();
                    }
                    break;

                }
                case 2: {
                    ACLMessage response = myAgent.receive(expectedMessageTemplate);
                    if (response != null) {
                        lastAction = null;
                        // TODO: 11/24/2019

                        ACLMessage callForProposal = new ACLMessage(ACLMessage.CFP);
                        callForProposal.addReceiver(environment);
                        callForProposal.setContent(lastAction.name());

                        String replyMarker = String.format("env cfp %d", System.currentTimeMillis());
                        expectedMessageTemplate = MatchInReplyTo(replyMarker);

                        callForProposal.setConversationId(SPELEOLOGIST_ENVIRONMENT_CONVERSATION_ID);
                        callForProposal.setReplyWith(replyMarker);
                        this.myAgent.send(callForProposal);
                        step = 3;
                    } else {
                        block();
                    }
                    break;
                }
                case 3: {
                    ACLMessage response = myAgent.receive(expectedMessageTemplate);
                    if (response != null) {
                        if (response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && "OK".equals(response.getContent())) {
                            if (lastAction == WumpusAction.CLIMB) {
                                step = 4;
                            } else {
                                step = 0;
                            }
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case 4: {
                    // delete everybody
                    break;
                }
                default: {
                    System.out.println("WTF?!");
                }
            }
        }

        private String fromPerception(WumpusPercept perception) {
            return null;
        }

        @Override
        public boolean done() {
            return step == 4;
        }
    }
}
