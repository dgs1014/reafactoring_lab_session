/*   This file is part of lanSimulation.
 *
 *   lanSimulation is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   lanSimulation is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with lanSimulation; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *   Copyright original Java version: 2004 Bart Du Bois, Serge Demeyer
 *   Copyright C++ version: 2006 Matthias Rieger, Bart Van Rompaey
 */
package lanSimulation;

import lanSimulation.internals.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;

/**
A <em>Network</em> represents the basic data stucture for simulating a Local Area Network (LAN).
The LAN network architecture is a token ring, implying that packahes will be passed from one node to another, until they reached their destination, or until they travelled the whole token ring.
 */
public class Network {
	private static final int INITIAL_BUFFER_CAPACITY = 30;
	
	/**
    Holds a pointer to myself.
    Used to verify whether I am properly initialized.
	 */
	private Network initPtr_;
	/**
    Holds a pointer to some "first" node in the token ring.
    Used to ensure that various printing operations return expected behaviour.
	 */
	public Node firstNode_;
	/**
    Maps the names of workstations on the actual workstations.
    Used to initiate the requests for the network.
	 */
	private Hashtable<String, Node> workstations_;

	/**
Construct a <em>Network</em> suitable for holding #size Workstations.
<p><strong>Postcondition:</strong>(result.isInitialized()) & (! result.consistentNetwork());</p>
	 */
	public Network(int size) {
		assert size > 0;
		initPtr_ = this;
		firstNode_ = null;
		workstations_ = new Hashtable<>(size, 1.0f);
		assert isInitialized();
		assert ! consistentNetwork();
	}

	/**
Return a <em>Network</em> that may serve as starting point for various experiments.
Currently, the network looks as follows.
    <pre>
    Workstation Filip [Workstation] -> Node -> Workstation Hans [Workstation]
    -> Printer Andy [Printer] -> ... 
    </pre>
<p><strong>Postcondition:</strong>result.isInitialized() & result.consistentNetwork();</p>
	 */
	public static Network DefaultExample () {
		Network network = new Network (2);

		Node wsFilip = new WorkStation ("Filip");
		Node n1 = new Node( "n1");
		Node wsHans = new WorkStation ("Hans");
		Node prAndy = new Printer ("Andy");

		wsFilip.setNextNode_(n1);
		n1.setNextNode_ (wsHans);
		wsHans.setNextNode_(prAndy);
		prAndy.setNextNode_(wsFilip);

		network.workstations_.put(wsFilip.name_, wsFilip);
		network.workstations_.put(wsHans.name_, wsHans);
		network.firstNode_ = wsFilip;

		assert network.isInitialized();
		assert network.consistentNetwork();
		return network;
	}

	/**
Answer whether #receiver is properly initialized.
	 */
	public boolean isInitialized () {
		return (initPtr_ == this);
	};

	/**
Answer whether #receiver contains a workstation with the given name.
<p><strong>Precondition:</strong>this.isInitialized();</p>
	 */
	public boolean hasWorkstation (String ws) {
		//return workstations_.containsKey(ws);
		Node n;

		assert isInitialized();
		n = workstations_.get(ws);
		return n != null && n instanceof WorkStation;
	};

	/**
Answer whether #receiver is a consistent token ring network.
A consistent token ring network
 - contains at least one workstation and one printer
 - is circular
 - all registered workstations are on the token ring
 - all workstations on the token ring are registered.
<p><strong>Precondition:</strong>this.isInitialized();</p>
	 */
	public boolean consistentNetwork () {
		assert isInitialized();
		Enumeration<Node> iter;
		Node currentNode;
		int printersFound = 0, workstationsFound = 0;
		Hashtable<String, Node> encountered = new Hashtable<>(workstations_.size() * 2, 1.0f);

		if (workstations_.isEmpty()) {
            return false;
        }
        if (firstNode_ == null) {
            return false;
        }
        
		//verify whether all registered workstations are indeed workstations
		iter = workstations_.elements();
		while (iter.hasMoreElements()) {
			currentNode = (Node) iter.nextElement();
			if (!(currentNode instanceof WorkStation)) {
				return false;
			};
		};
		//enumerate the token ring, verifying whether all workstations are registered
		//also count the number of printers and see whether the ring is circular
		currentNode = firstNode_;
        while (!encountered.containsKey(currentNode.name_)) {
            encountered.put(currentNode.name_, currentNode);
            if (currentNode instanceof WorkStation) {
                workstationsFound++;
            }
            if (currentNode instanceof Printer) {
                printersFound++;
            }
            currentNode = currentNode.getNextNode_();
        }

        if (currentNode != firstNode_) {
            return false;
        }
        if (printersFound == 0) {
            return false;
        }
        if (workstationsFound != workstations_.size()) {
            return false;
        }

        return true;
	}

	/**
The #receiver is requested to broadcast a message to all nodes.
Therefore #receiver sends a special broadcast packet across the token ring network,
which should be treated by all nodes.
<p><strong>Precondition:</strong> consistentNetwork();</p>
@param report Stream that will hold a report about what happened when handling the request.
@return Anwer #true when the broadcast operation was succesful and #false otherwise
	 */
	public boolean requestBroadcast(Writer report) {
		assert consistentNetwork();

		try {
			report.write("Broadcast Request\n");
		} catch (IOException ignored) {
        }

		Node currentNode = firstNode_;
		Packet packet = new Packet("BROADCAST", firstNode_.name_, firstNode_.name_);
		do {
			try {
				report.write("\tNode '");
				report.write(currentNode.name_);
				report.write("' accepts broadcase packet.\n");
				report.flush();
			} catch (IOException ignored) {
	        }
			currentNode.extractedWriterNode(report, this);
			currentNode = currentNode.getNextNode_();
		} while (! atDestination( packet, currentNode));

		try {
			report.write(">>> Broadcast travelled whole token ring.\n\n");
		} catch (IOException ignored) {
        }
		return true;
	}    

	/**
The #receiver is requested by #workstation to print #document on #printer.
Therefore #receiver sends a packet across the token ring network, until either
(1) #printer is reached or (2) the packet travelled complete token ring.
<p><strong>Precondition:</strong> consistentNetwork() & hasWorkstation(workstation);</p>
@param workstation Name of the workstation requesting the service.
@param document Contents that should be printed on the printer.
@param printer Name of the printer that should receive the document.
@param report Stream that will hold a report about what happened when handling the request.
@return Anwer #true when the print operation was succesful and #false otherwise
	 */
	public boolean requestWorkstationPrintsDocument(String workstation, String document,
			String printer, Writer report) {
		assert consistentNetwork() & hasWorkstation(workstation);

		try {
			report.write("'");
			report.write(workstation);
			report.write("' requests printing of '");
			report.write(document);
			report.write("' on '");
			report.write(printer);
			report.write("' ...\n");
		} catch (IOException ignored) {
        }

		boolean result = false;
		Node startNode, currentNode;
		Packet packet = new Packet(document, workstation, printer);

		startNode = (Node) workstations_.get(workstation);

		startNode.extractedWriterNode(report, this);
		currentNode = startNode.getNextNode_();
		while ((! atDestination( packet, currentNode))
				& (! packet.origin_.equals(currentNode.name_))) {
			currentNode.extractedWriterNode(report, this);
			currentNode = currentNode.getNextNode_();
		};

		if (atDestination( packet, currentNode)) {
			result = currentNode.printDocument(this, packet, report);
		} else {
			try {
				report.write(">>> Destinition not found, print job cancelled.\n\n");
				report.flush();
			} catch (IOException ignored) {
	        }
			result = false;
		}

		return result;
	}

	public void extractedAccounting(Writer report, String author, String title) throws IOException {
		report.write("\tAccounting -- author = '");
		report.write(author);
		report.write("' -- title = '");
		report.write(title);
		report.write("'\n");
	}

	/**
Return a printable representation of #receiver.
 <p><strong>Precondition:</strong> isInitialized();</p>
	 */
	public String toString () {
		assert isInitialized();
		StringBuffer buf = new StringBuffer(INITIAL_BUFFER_CAPACITY  * workstations_.size());
		printOn(firstNode_, buf);
		return buf.toString();
	}

	private boolean atDestination(Packet packet,Node currentNode) {
		return packet.destination_.equals(currentNode.name_);
	}

	/**
	Write an XML representation of #receiver on the given #buf.
	<p><strong>Precondition:</strong> isInitialized();</p>
	 * @param node TODO
	 * @param buf 
	 */
	public void printXMLOn (Node node, StringBuffer buf) {
		assert isInitialized();
	
		Node currentNode = node;
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<network>");
		do {
			buf.append("\n\t");
			currentNode.toXML(buf);
			currentNode = currentNode.getNextNode_();
		} while (currentNode != node);
		buf.append("\n</network>");
	}

	/**
	Write a HTML representation of #receiver on the given #buf.
	<p><strong>Precondition:</strong> isInitialized();</p>
	 * @param node TODO
	 * @param buf 
	 */
	public void printHTMLOn (Node node, StringBuffer buf) {
		assert isInitialized();
	
		buf.append("<HTML>\n<HEAD>\n<TITLE>LAN Simulation</TITLE>\n</HEAD>\n<BODY>\n<H1>LAN SIMULATION</H1>");
		Node currentNode = node;
		buf.append("\n\n<UL>");
		do {
			buf.append("\n\t<LI> ");
			currentNode.toText(buf);
			buf.append(" </LI>");
			currentNode = currentNode.getNextNode_();
		} while (currentNode != node);
		buf.append("\n\t<LI>...</LI>\n</UL>\n\n</BODY>\n</HTML>\n");
	}

	/**
	Write a printable representation of #receiver on the given #buf.
	<p><strong>Precondition:</strong> isInitialized();</p>
	 * @param node TODO
	 * @param buf 
	 */
	public void printOn (Node node, StringBuffer buf) {
		assert isInitialized();
		Node currentNode = node;
		do {
			currentNode.toText(buf);
			buf.append(" -> ");
			currentNode = currentNode.getNextNode_();
		} while (currentNode != node);
		buf.append(" ... ");
	}
}
