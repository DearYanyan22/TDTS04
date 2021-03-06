/*
 * ---1. Constructor---
 * ---2. recvUpdate()---'
 * 3. printDistanceTable()
 *
 * ---4. updateLinkCost()---
 *
 * 5. Poisoned Reverse implementation
 */


import javax.swing.*;
import java.util.Arrays;
import java.awt.Font;     

class RouteEntry {
    int firsthop;
    int distance;
}

public class RouterNode {
    private int myID;
    private RouterSimulator sim;
    private GuiTextArea myGUI;
    private int[] linkcosts = new int[RouterSimulator.NUM_NODES];
    private int[][] bestDistanceto_ofNeighbour = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES]; //Allocating (sizeof dist(int) * NUM_NODES * NUM_NODES) space to this table, instead of (s_o dist(int) * NN * number of neighbours * s_o what neighbour(int)).
    private RouteEntry[] bestRouteto = new RouteEntry[RouterSimulator.NUM_NODES];
    private final boolean[] isNeighbour = new boolean[RouterSimulator.NUM_NODES]; //Assuming no severed connections or restructuring

    //--------------------------------------------------
    public RouterNode(int ID, RouterSimulator sim, int[] linkcosts) {
	myID = ID;
	this.sim = sim;
	myGUI = new GuiTextArea("  Output window for Router #"+ ID + "  ");

	System.arraycopy(linkcosts, 0, this.linkcosts, 0, RouterSimulator.NUM_NODES);

	//Build the bestRouteto-array with actual RouteEntry:s.
	//Set initial best routes (bestRouteto) and determine neighbours (isNeighbour).
	//"Everything in java defaults to 0". IS A LIE.
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    bestRouteto[i] = new RouteEntry() {{ firsthop = 0; distance = 0; }};
	    bestRouteto[i].firsthop = i;
	    if ((bestRouteto[i].distance = linkcosts[i]) != RouterSimulator.INFINITY && i != myID)
		isNeighbour[i] = true;
	    else 	
		isNeighbour[i] = false; //We are not neighbours with ourselves
	}
	//Set initial values of bestDistanceto_ofNeighbour[][] to infinity, and 0 on the diagonal.
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    Arrays.fill(bestDistanceto_ofNeighbour[i], RouterSimulator.INFINITY);
	    bestDistanceto_ofNeighbour[i][i] = 0;
	}

	printDistanceTable();
	informNeighbours();
    }




 
    //--------------------------------------------------
    public void recvUpdate(RouterPacket pkt) {
	boolean bRtchanged = false;
	int src = pkt.sourceid;
	RouteEntry minroute = new RouteEntry() {{ firsthop = 0; distance = 0; }};

	//Check packet contents. Update route and source's distance to i as necessary.
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    //D_src(i) the same - go to next i.
	    if (pkt.mincost[i] == bestDistanceto_ofNeighbour[i][src]) {
		continue;
	    }
	    //D_src(i) got better
	    else if (pkt.mincost[i] < bestDistanceto_ofNeighbour[i][src]) {
		//Update local neighbour-table entry.
		bestDistanceto_ofNeighbour[i][src] = pkt.mincost[i];

		//Can we improve our routing?
		if (linkcosts[src] + pkt.mincost[i] < bestRouteto[i].distance) {
		    bestRouteto[i].distance = linkcosts[src] + pkt.mincost[i];
		    bestRouteto[i].firsthop = src;
		    bRtchanged = true;
		}
	    }
	    //D_src(i) got worse - However, are we routing through src to i?
	    else {
		//Update local neighbour-table entry.
		bestDistanceto_ofNeighbour[i][src] = pkt.mincost[i];

		if (bestRouteto[i].firsthop == src) {
		    minroute.distance = RouterSimulator.INFINITY;
		    //We have no clues which route to i could possibly be optimal. We must check them all!
		    //Find the best route/distance to i based on all saved tables of nodes j.
		    for (int j = 0 ; j < RouterSimulator.NUM_NODES ; ++j) {
			if (!isNeighbour[j]) {
			    continue;
			}
		   
			if (linkcosts[j] + bestDistanceto_ofNeighbour[i][j] < minroute.distance) {
			    minroute.distance = linkcosts[j] + bestDistanceto_ofNeighbour[i][j];
			    minroute.firsthop = j;
			}
		    }
		    //New best route in minroute.
		    bestRouteto[i].distance = minroute.distance;
		    bestRouteto[i].firsthop = minroute.firsthop;
		    bRtchanged = true; 
		}
	    }

	}

	printDistanceTable();

	if (bRtchanged) {
	    informNeighbours();
	}
    }
    

    //--------------------------------------------------
    private void sendUpdate(RouterPacket pkt) {
	sim.toLayer2(pkt);
    }
  
    
    //--------------------------------------------------
    public void printDistanceTable() {
	myGUI.println("Current table for " + myID +
		      " at time " + sim.getClocktime());
	myGUI.print("   \\to");
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i)
	    myGUI.print("   " + i);
	myGUI.println();

	myGUI.print("from\\__");
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i)
	    myGUI.print("____");
	myGUI.println();

	String extraspaces = "";

	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    if (!isNeighbour[i])
		continue;
	   
	    myGUI.print(i + "    |");
	   
	    for (int j = 0 ; j < RouterSimulator.NUM_NODES ; ++j) {
		if(bestDistanceto_ofNeighbour[j][i]/100 != 0) {
		    extraspaces = "";
		}
		else if(bestDistanceto_ofNeighbour[j][i]/10 != 0) {
		    extraspaces = " ";
		}
		else {
		    extraspaces = "  ";
		}
		myGUI.print(" " + extraspaces + bestDistanceto_ofNeighbour[j][i]);
	    }
	    myGUI.println();
	}

	myGUI.println("Local routing table:");

	myGUI.print("      to");
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    if (i!=myID)
		myGUI.print("   " + i);
	}
	myGUI.println();

	myGUI.print("distance");
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    if (i==myID)
		continue;

	    if(bestRouteto[i].distance/100 != 0) {
		extraspaces = "";
	    }
	    else if(bestRouteto[i].distance/10 != 0) {
		extraspaces = " ";
	    }
	    else {
		extraspaces = "  ";
	    }
	    myGUI.print(" " + extraspaces + bestRouteto[i].distance);
	}

	myGUI.println();
    
	myGUI.print("firsthop");
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    if (i!=myID)
		myGUI.print("   " + bestRouteto[i].firsthop);
	}
	myGUI.println();
	myGUI.println();
    }


    //--------------------------------------------------
    public void updateLinkCost(int dest, int newcost) {
	boolean bRtchanged = false;
	RouteEntry minroute = new RouteEntry() {{ firsthop = 0; distance = 0; }};
	int oldcost = linkcosts[dest];

	//Update linkcosts entry.
	linkcosts[dest] = newcost;

	//Update routes as necessary

	//Link cost c(me, dest) got better
	if (newcost < oldcost) { 
	    //Can we improve our route to i?
	    for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
		if (newcost + bestDistanceto_ofNeighbour[i][dest] < bestRouteto[i].distance) {
		    bestRouteto[i].distance = newcost + bestDistanceto_ofNeighbour[i][dest];
		    bestRouteto[i].firsthop = dest;
		    bRtchanged = true;
		}	
	    }	    
	}
	//Link cost c(me, dest) got worse
	else {
	    //Are we routing through dest to i?
	    for(int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
		if (bestRouteto[i].firsthop == dest) {
		    minroute.distance = RouterSimulator.INFINITY;
		    //We have no clues which route to i could possibly be optimal. We must check them all!
		    //Find the best route/distance to i based on all saved tables of nodes j.
		    for (int j = 0 ; j < RouterSimulator.NUM_NODES ; ++j) {
			if (!isNeighbour[j]) {
			    continue;
			}
			
			if (linkcosts[j] + bestDistanceto_ofNeighbour[i][j] < minroute.distance) {	      
			    minroute.distance = linkcosts[j] + bestDistanceto_ofNeighbour[i][j];
			    minroute.firsthop = j;
			}
		    } 
		    //New best route in minroute.
		    bestRouteto[i].distance = minroute.distance;
		    bestRouteto[i].firsthop = minroute.firsthop;
		    bRtchanged = true; 
		}
	    }	    
	}

	printDistanceTable();

	if (bRtchanged) {
	    informNeighbours();
	}	
    }
 

    //--------------------------------------------------  
    private void informNeighbours() {
        int[] distancecosts = new int[RouterSimulator.NUM_NODES];
    
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i)
	    distancecosts[i] = bestRouteto[i].distance;	
    
	RouterPacket updatepkt;

	//Prepare a packet for every neighbour i
	for (int i = 0 ; i < RouterSimulator.NUM_NODES ; ++i) {
	    if(isNeighbour[i]) {
		updatepkt = new RouterPacket(myID, 0, distancecosts);
		updatepkt.destid = i;
		
		//If we route to node j through i, poison the reverse
		for (int j = 0 ; j < RouterSimulator.NUM_NODES ; ++j ) {
		    if (bestRouteto[j].firsthop == i)
			updatepkt.mincost[j] = RouterSimulator.INFINITY;
		}
		sendUpdate(updatepkt);
	    }
	}
    }
    

}
