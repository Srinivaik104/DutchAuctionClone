/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hw3;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author srinivaik
 */
public class Participants extends Agent{
    private final int CLONES = 3;
    
    private boolean isClone = false;
    final double[] strategy1 = {0.5, 0.7, 0.9};
    final double[] strategy2 = {0.4, 0.6, 0.8};
    private boolean isInterested;
    private int price;
    private String museum;
    
// Agent initialization
    @Override 
    protected void setup() {
        museum = here().getName();
        
        // Clone
        for (int i=0; i<CLONES; i++)
            addBehaviour(new CloneBehaviour(museum + "_Participant " + (i+2))); 
        
        SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new register());
        sb.addSubBehaviour(new ParticipantBehaviour());
        sb.addSubBehaviour(new getBids());
        addBehaviour(sb);
    }
    
    private class CloneBehaviour extends OneShotBehaviour {
        String name;
        
        private CloneBehaviour(String name) {
            this.name = name;
        }
        
         @Override
        public void action() {
            if (!isClone) doClone(here(), name);
        }
    }
    
    @Override
    protected void afterClone() {
       isClone = true;
       System.out.println(getAID().getLocalName() + " got cloned!");      
    }
    
    private class register extends OneShotBehaviour {
        
         @Override
        public void action() {
            // register with DF
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd1 = new ServiceDescription();
           
            sd1.setType("buyer " + museum);    
            sd1.setName(getLocalName());
            dfd.addServices(sd1);
            try {
                DFService.register(myAgent, dfd);
            } catch (FIPAException ex) {}
        }
    }
    
    //Picking auction strategy

    private class ParticipantBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = blockingReceive(mt);
            int offer = Integer.parseInt(msg.getContent());
            int s1 = (int) (Math.random() * 3);
            int s2 = (int) (Math.random() * 3);
            double e;
            String m = here().getName();
            if (m.equals("HM")) e = strategy1[s1]; 
            else e = strategy2[s2];
            price = (int) (offer * e);
            isInterested = Math.random() < 0.5; // Random boolean
            StringBuilder sb = new StringBuilder("");
            sb.append("Agent ").append(getAID().getLocalName());
            sb.append(" evaluated item to ").append(price).append(" and ");
            if (!isInterested) {
                sb.append("not ");
            }
            sb.append("interested.");
            System.out.println(sb);
        }

    }
    
    // Participate in auction
    private class getBids extends SimpleBehaviour {
        boolean done = false;
        
        @Override
        public void action() {
            if (!isInterested) done = true;
            ACLMessage msg = receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.CFP:
                        int bid = Integer.parseInt(msg.getContent());
                        if (bid<=price) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.PROPOSE);
                            reply.setContent(Integer.toString(bid));
                            send(reply);
                        }
                        break;
                    case ACLMessage.ACCEPT_PROPOSAL:
                        done = true;
                        break;
                    case ACLMessage.INFORM:
                        done = true;
                        break;
                }
            }
        }

        @Override
        public boolean done() {
            return done;
        }      
    }
    
    @Override protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
}
