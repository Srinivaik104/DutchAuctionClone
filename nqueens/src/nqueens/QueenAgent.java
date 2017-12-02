/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nqueens;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.io.IOException;

/**
 *
 * @author srinivaik
 */
public class QueenAgent extends Agent {
     private int id, n;
    private boolean board[][];
    private AID previousQueen;
    private AID nextQueen;
    private int solutions;


    @Override
    protected void setup() {
        Object[] args = getArguments();
        id = (int) args[0];
        n = (int) args[1];
        solutions = 0;
        
        previousQueen = new AID("Queen" + (id - 1), AID.ISLOCALNAME);
        nextQueen = new AID("Queen" + (id + 1), AID.ISLOCALNAME);
        addBehaviour(new QueenBehaviour());
    }

    private class QueenBehaviour extends SimpleBehaviour {
        boolean done = false;

        @Override
        public void action() {
            if (id == 0) {
                board = new boolean[n][n];
            }
            else {
                ACLMessage msg = blockingReceive();
                switch (msg.getPerformative()) {
                    case ACLMessage.INFORM: 
                        // Receive the board with previous queens positioned
                        try {board = (boolean[][]) msg.getContentObject();} 
                        catch (UnreadableException ex) {}
                        break;
                    case ACLMessage.CANCEL: // Quit
                        if (id != n - 1) { 
                            ACLMessage quitMsg = new ACLMessage(ACLMessage.CANCEL);
                            msg.addReceiver(nextQueen);
                            //System.out.println(quitMsg);
                            }
                        done = true;
                        return;
                }
            }    
        
            for (int i=0; i<n; i++) {
                if (isPositionSave(id,i)) {
                    board[id][i] = true;
                    if (id == n - 1) {
                        solutions+=1;
                        printSolution();
                    }
                    else {
                        // Send to the next queen
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(nextQueen);
                        try {msg.setContentObject(board);} 
                        catch (IOException ex) {}
                        send(msg);
                        blockingReceive(); // proposal to move
                        board[id][i] = false;
                    }
                }
            }
            
            // All positions are checked for this round
            
            if (id == 0) {
                System.out.println("All solutions are found");
                // Message others to quit 
                ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
                msg.addReceiver(nextQueen);
                done = true;
            }
            else {
                // Propose to move to next position
                ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(previousQueen);
                send(msg);
            }
        }

        @Override
        public boolean done() {
            return done;
        }
    }
    
    private boolean isPositionSave(int row, int col) {
        int i,j;
        
        // Checking up-left
        j = col - 1;
        for (i = row - 1; i >= 0; i--) {
            if (j >= 0)  {
                if (board[i][j]) return false;
            }
            j--;
        }
        
        // Checking upwards
        for (i = row; i >= 0; i--) {
            if (board[i][col]) return false;
        }
        
        // Checking up-right
        j = col + 1;
        for (i = row - 1; i >= 0; i--) {
            if (j < n)  {
                if (board[i][j]) return false;
            }
            j++;
        }
       
        return true;
    }
    
    private void printSolution() {
        System.out.println("Solution " + solutions + ":");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j]) System.out.print("* ");
                else System.out.print("□ ");      
            }
            System.out.println();
        }
        System.out.println();
    }
}
