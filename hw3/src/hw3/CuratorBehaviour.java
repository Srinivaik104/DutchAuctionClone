/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hw3;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author srinivaik
 */
public class CuratorBehaviour extends SequentialBehaviour{
    private final AuctionAgent agent;
    private final int price;
    private final String museum;
    private final StringBuilder sb = new StringBuilder("");
    private AID[] buyers;
    private final AID manager;
    
    CuratorBehaviour(AuctionAgent agent, int price, AID manager) {
        this.agent = agent;
        this.price = price;
        this.manager = manager;

        museum = agent.here().getName();
        
        this.addSubBehaviour(new registerParticipants(agent, 5000));
        this.addSubBehaviour(new startAuction());
        this.addSubBehaviour(new performAuction());
        }
    
    
    // Get the list of buyers from the DF
    private class registerParticipants extends WakerBehaviour {

        registerParticipants(Agent agent, long period) {
            super(agent, period);
        }

        @Override
        protected void onWake() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription tsd = new ServiceDescription();
            tsd.setType("buyer " + museum);
            template.addServices(tsd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    buyers = new AID[result.length];
                    for (int i = 0; i < result.length; i++) 
                        buyers[i] = result[i].getName();
                }
            } catch (FIPAException ex) {}
        }
        
    }
    
    // Start an auction
    private class startAuction extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            for (AID buyer : buyers) {msg.addReceiver(buyer);}
            msg.setContent(Integer.toString(price));
            agent.send(msg);
            sb.append("Agent ").append(agent.getAID().getLocalName())
                    .append(" started the auction for ").append(museum)
                    .append(System.getProperty("line.separator"));
        }
    }
        
    // Perform the auction
    private class performAuction extends ParallelBehaviour {
        
        private int bid;

        performAuction() {
            super(WHEN_ANY); // terminates when any child is done
            
            bid = price*2;
            String ls = System.getProperty("line.separator");
            
            // Increment the price and send offers to the buyers
            addSubBehaviour(new TickerBehaviour(myAgent, 3000) {

                @Override
                protected void onTick() {
                    bid *= 0.9;
                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                    for (AID buyer : buyers) {msg.addReceiver(buyer);}
                    msg.setContent(Integer.toString(bid));
                    agent.send(msg);
                    sb.append("Current bid: ").append(bid).append(ls);
                }
            });
            
            // Receive the offer from the buyer and end the auction
            addSubBehaviour(new SimpleBehaviour() {

                boolean done = false;

                @Override
                public void action() {
                    ACLMessage msg = agent.receive();
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            if (Integer.parseInt(msg.getContent()) == bid) {
                                AID winner = msg.getSender();
                                sb.append("Item sold for ").append(bid).append(" to ").append(winner)
                                        .append(ls);
                                
                                ACLMessage winMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                winMsg.addReceiver(winner);
                                winMsg.setContent("You won the auction");
                                agent.send(winMsg);
                                System.out.println(winMsg);
                    
                                ACLMessage endMsg = new ACLMessage(ACLMessage.INFORM);
                                for (AID buyer : buyers) {endMsg.addReceiver(buyer);}
                                endMsg.setContent("Auction ended");
                                agent.send(endMsg);
                                
                                ACLMessage winBid = new ACLMessage(ACLMessage.PROPOSE);
                                winBid.addReceiver(manager);
                                winBid.setContent(Integer.toString(bid));
                                agent.send(winBid);
                                System.out.println(winBid);
                                
                                System.out.println(sb);
                                done = true;
                            }
                        }
                    }
                }

                @Override
                public boolean done() {
                    return done;
                }
            });
        }
    }
    
}
