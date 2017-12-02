/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hw3;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

/**
 *
 * @author srinivaik
 */
public class Main {
     public static void main(String[] args)  throws StaleProxyException{
        
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        rt.createMainContainer(new ProfileImpl()).createNewAgent("rma", "jade.tools.rma.rma", new Object[]{}).start();
        
        Profile p = new ProfileImpl();
        p.setParameter(Profile.CONTAINER_NAME, "OriginalContainer");
        AgentContainer c = rt.createAgentContainer(p);
        c.createNewAgent("Manager", "hw3.AuctionAgent", new Object[]{}).start();
    }
}
