/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nqueens;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;

/**
 *
 * @author srinivaik
 */
public class Main {

     public static final int QUEENS = 5;

    public static void main(String[] args)  throws StaleProxyException{
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        rt.createMainContainer(new ProfileImpl());
        
        AgentContainer c = rt.createAgentContainer(new ProfileImpl());
        for (int i = 0; i < QUEENS; i++)
            c.createNewAgent("Queen" + i, "nqueens.QueenAgent", new Object[]{i, QUEENS}).start();
    }
    
}
