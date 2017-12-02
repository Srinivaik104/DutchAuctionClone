/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hw3;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.*;
import jade.core.*;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.*;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import java.util.HashMap;

/**
 *
 * @author srinivaik
 */
public class AuctionAgent extends Agent {
 private final int PRICE = 12000;
    private boolean isClone = false;
    private final String museums[] = {"HM", "Galileo"};
    private HashMap locations = new HashMap<>();
    private AID manager;
    private Location home;
    
    
    // Agent initialization
    @Override 
    protected void setup() {
        manager = getAID();
        home = here();
        // Register language and ontology
	getContentManager().registerLanguage(new SLCodec());
	getContentManager().registerOntology(MobilityOntology.getInstance());
        
        addBehaviour(new ManagerBehaviour());
    }
 
    private class ManagerBehaviour extends OneShotBehaviour {
        
        @Override
        public void action() {
            
            // Creating new containers
            jade.core.Runtime rt = jade.core.Runtime.instance();
            Profile p1 = new ProfileImpl();
            p1.setParameter(Profile.CONTAINER_NAME, museums[0]);
            AgentContainer c1 = rt.createAgentContainer(p1);
            Profile p2 = new ProfileImpl();
            p2.setParameter(Profile.CONTAINER_NAME, museums[1]);
            AgentContainer c2 = rt.createAgentContainer(p2);
            
            try {
                c1.createNewAgent(museums[0] + "_Participant1", "hw3.Participants", 
                        new Object[0]).start();
                c2.createNewAgent(museums[1] + "_Participant1", "hw3.Participants", 
                        new Object[0]).start();
            } catch (StaleProxyException ex) {}
            
            // Request available locations with AMS
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.setLanguage(new SLCodec().getName());
            request.setOntology(MobilityOntology.getInstance().getName());
            try {
                Action action = new Action(getAMS(), 
                        new QueryPlatformLocationsAction());
                getContentManager().fillContent(request, action);
                request.addReceiver(action.getActor());
                send(request);
            
                //Receive response from AMS
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchSender(getAMS()),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage resp = blockingReceive(mt);
                ContentElement ce;
                ce = getContentManager().extractContent(resp);
                Result result = (Result) ce;
                jade.util.leap.Iterator it = result.getItems().iterator();
                while (it.hasNext()) {
                    Location loc = (Location)it.next();
                    locations.put(loc.getName(), loc);
		}
                
                // Clone into containers
                for (int i=0; i<2; i++) {
                    String s = museums[i];
                    addBehaviour(new CloneBehaviour((Location) locations.get(s),
                            "Curator" + (i+1))); 
                }
                
                addBehaviour(new determineWinningBid());
                
            } catch (Codec.CodecException | OntologyException ex) {}       
        }
    }
    
    //Clone to the provided location
    private class CloneBehaviour extends OneShotBehaviour {
        
        String name;
        Location loc;
        
        private CloneBehaviour(Location loc, String name) {
            this.name = name;
            this.loc = loc;
        }
        
         @Override
        public void action() {
            if (!isClone) doClone(loc, name);
        }
    }
    
    
    @Override
    protected void afterClone() {
       isClone = true;
       System.out.println(getLocalName() + " got cloned!");
       SequentialBehaviour sb = new SequentialBehaviour();
       sb.addSubBehaviour(new CuratorBehaviour(this, PRICE, manager));
       sb.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                doMove(home);
            }
        });
       addBehaviour(sb);
    }
    
    private class determineWinningBid extends ParallelBehaviour {
        
        int bid = 0;
        
        determineWinningBid (){
            super(WHEN_ALL);
            for (int i = 0; i<2; i++) {
                addSubBehaviour(new SimpleBehaviour() {
                    boolean done = false;
                    
                    @Override
                    public void action() {
                        if (isClone) done = true;
                        MessageTemplate mt = MessageTemplate
                                .MatchPerformative(ACLMessage.PROPOSE);
                        ACLMessage msg = receive(mt);
                        if (msg!=null) {
                            int prop = Integer.parseInt(msg.getContent());
                            if (prop>bid) bid=prop;
                            done = true;
                        }
                    }

                    @Override
                    public boolean done() {
                        return done;
                    }
                });
            }
        }
        
        @Override
        public int onEnd() {
            if(!isClone) System.out.println("Auction Ended! ");
            return 0;
        }    
    }
    
    @Override
    protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
}
