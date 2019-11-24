package ua.nure.baranov.wumpus;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static jade.lang.acl.MessageTemplate.*;

public class Speleologist extends Agent {

    private AID navigator;
    private AID environment;
    private WumpusAction lastAction;
    private static final String SPELEOLOGIST_NAVIGATOR_CONVERSATION_ID = "speleologist-navigator";
    private MessageTemplate expectedMessageTemplate;

    @Override
    protected void setup() {
        addBehaviour(new RequestBehavior());


    }

    private class RequestBehavior extends Behaviour {
        private int step = 0;


        @Override
        public void action() {
            switch (step) {
                case 0: {
                    ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);

                    aclMessage.setConversationId(SPELEOLOGIST_NAVIGATOR_CONVERSATION_ID);

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

    private class ProposalBehavior extends CyclicBehaviour {

        @Override
        public void action() {

        }
    }
}
