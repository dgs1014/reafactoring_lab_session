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
package lanSimulation.tests;

import lanSimulation.internals.*;
import lanSimulation.*;
import junit.framework.*;
import junit.extensions.*;
import java.lang.AssertionError;
import java.io.*;

public class LANTests extends TestCase {

	public static Test suite() {
		TestSuite testSuite = new TestSuite(LANTests.class);
		return testSuite;
	}

	public void testBasicPacket() {
		Packet packet;

		packet = new Packet("c", "a");
		assertEquals("message_",
				packet.message_, "c");
		assertEquals("destination_",
				packet.destination_, "a");
		assertEquals("origin_",
				packet.origin_, "");
		packet.origin_ = "o";
		assertEquals("origin_ (after setting)",
				packet.origin_, "o");
	}

	private boolean compareFiles (String filename1, String filename2) {
		FileInputStream f1, f2;
		int b1 = 0, b2 = 0;

		try {
			f1 = new FileInputStream(filename1);
			try {
				f2 = new FileInputStream(filename2);
			} catch (FileNotFoundException f2exc) {
				try {f1.close();} catch (IOException exc) {};
				return false; // file 2 does not exist
			}
		} catch (FileNotFoundException f1exc) {
			return false; //file 1 does not exist
		};

		try {
			if (f1.available() != f2.available()) {return false;} // length of files is different
			while ((b1 != -1) & (b2 != -1)) {
				b1 = f1.read();
				b2 = f2.read();
				if (b1 != b2) {return false;} // discovered one diferring character
			};
			if ((b1 == -1) & (b2 == -1)){
				return true; // reached both end of files
			} else {
				return false; // one end of file not reached
			}
		} catch (IOException exc) {
			return false; // read error, assume one file corrupted
		} finally {
			try {f1.close();} catch (IOException exc) {};
			try {f2.close();} catch (IOException exc) {};
		}
	}

	private void helper_TestCompareFiles() {
		String fName1 = "testCompare1.txt", fName2 = "testCompare2.txt",
				fName3 = "testCompare3.txt", fName4 = "testCompare4.txt";
		FileWriter f1, f2, f3, f4;

		try {
			f1 = new FileWriter(fName1);
			try {
				f2 = new FileWriter(fName2);
				try {
					f3 = new FileWriter(fName3);
					try {
						f4 = new FileWriter(fName4);
					} catch (IOException f3exc) {
						try {f1.close();} catch (IOException exc) {};
						try {f2.close();} catch (IOException exc) {};
						try {f3.close();} catch (IOException exc) {};
						return;
					}
				} catch (IOException f3exc) {
					try {f1.close();} catch (IOException exc) {};
					try {f2.close();} catch (IOException exc) {};
					return;
				}
			} catch (IOException f2exc) {
				try {f1.close();} catch (IOException exc) {};
				return;
			}
		} catch (IOException f1exc) {
			return;
		};

		try {
			f1.write("aaa");
			f2.write("aaa");
			f3.write("aa");
			f4.write("aab");
		} catch (IOException exc) {
		} finally {
			try {f1.close();} catch (IOException exc) {};
			try {f2.close();} catch (IOException exc) {};
			try {f3.close();} catch (IOException exc) {};
			try {f4.close();} catch (IOException exc) {};
		}

        assertTrue("equals fName1 to fName2 ", compareFiles(fName1, fName2));
        assertFalse("not equals fName1 to fName3 (fName 3 is shorter)", compareFiles(fName1, fName3));
        assertFalse("not equals fName3 to fName1  (fName 3 is shorter)", compareFiles(fName3, fName1));
        assertFalse("not equals fName1 to fName4 (last character differs)", compareFiles(fName1, fName4));
        assertFalse("not equals fName1 to fName4 (last character differs)", compareFiles(fName1, fName4));
    }

    public void testBasicNode() {
        Node node;

    	node = new Node("n");
    	assertTrue("Instance of Node", node instanceof Node);
        assertEquals("name_",
            node.name_, "n");
        assertEquals("nextNode_",
            node.getNextNode_(), null);
        node.setNextNode_(node);
        assertEquals("nextNode_ (after setting)",
            node.getNextNode_(), node);
    }

    public void testDefaultNetworkToString() {
        Network network = Network.DefaultExample();

        assertTrue("isInitialized ", network.isInitialized());
        assertTrue("consistentNetwork ", network.consistentNetwork());
        assertEquals("DefaultNetwork.toString()",
            network.toString(),
            "Workstation Filip [Workstation] -> Node n1 [Node] -> Workstation Hans [Workstation] -> Printer Andy [Printer] ->  ... ");
    }

    public void testWorkstationPrintsDocument() {
        Network network = Network.DefaultExample();
	StringWriter report = new StringWriter(500);

        assertTrue("PrintSuccess ",
            network.requestWorkstationPrintsDocument("Filip", "Hello World", "Andy", report));
        assertFalse("PrintFailure (UnkownPrinter) ",
            network.requestWorkstationPrintsDocument("Filip", "Hello World", "UnknownPrinter", report));
        assertFalse("PrintFailure (print on Workstation) ",
            network.requestWorkstationPrintsDocument("Filip", "Hello World", "Hans", report));
        assertFalse("PrintFailure (print on Node) ",
            network.requestWorkstationPrintsDocument("Filip", "Hello World", "n1", report));
        assertTrue("PrintSuccess Postscript",
	    network.requestWorkstationPrintsDocument("Filip", "!PS Hello World in postscript", "Andy", report));
        assertFalse("PrintFailure Postscript",
	    network.requestWorkstationPrintsDocument("Filip", "!PS Hello World in postscript", "Hans", report));
    }

    public void testBroadcast() {
        Network network = Network.DefaultExample();
	StringWriter report = new StringWriter(500);

        assertTrue("Broadcast ",
		   network.requestBroadcast(report));
    }

    /**
Test whether output routines work as expected.
This is done by comparing generating output on a file "useOutput.txt" and
comparing it to a file "expectedOutput.txt". On a first run this test might
break because the file "expectedOutput.txt" does not exist. Then just run the
test, verify manually whether "useOutput.txt" conforms to the expected output
and if it does rename "useOutput.txt" in "expectedOutput.txt". From then on
the tests should work as expected.
*/
    public void testOutput () {
        Network network = Network.DefaultExample();
	String generateOutputFName = "useOutput.txt", expectedOutputFName = "expectedOutput.txt";
	FileWriter generateOutput;
	StringBuffer buf = new StringBuffer(500);
	StringWriter report = new StringWriter(500);

	try {
	    generateOutput = new FileWriter(generateOutputFName);
	} catch (IOException f2exc) {
	    assertTrue("Could not create '" + generateOutputFName + "'", false);
	    return;
	};

	try {
	    buf.append("---------------------------------ASCII------------------------------------------\n");
	    network.printOn(network.firstNode_, buf);
	    buf.append("\n\n---------------------------------HTML------------------------------------------\n");
	    network.printHTMLOn(network.firstNode_, buf);
	    buf.append("\n\n---------------------------------XML------------------------------------------\n");
	    network.printXMLOn(network.firstNode_, buf);
	    generateOutput.write(buf.toString());
	    report.write("\n\n---------------------------------SCENARIO: Print Success --------------------------\n");
	    network.requestWorkstationPrintsDocument("Filip", "Hello World", "Andy", report);
	    report.write("\n\n---------------------------------SCENARIO: PrintFailure (UnkownPrinter) ------------\n");
	    network.requestWorkstationPrintsDocument("Filip", "Hello World", "UnknownPrinter", report);
	    report.write("\n\n---------------------------------SCENARIO: PrintFailure (print on Workstation) -----\n");
	    network.requestWorkstationPrintsDocument("Filip", "Hello World", "Hans", report);
	    report.write("\n\n---------------------------------SCENARIO: PrintFailure (print on Node) -----\n");
	    network.requestWorkstationPrintsDocument("Filip", "Hello World", "n1", report);
	    report.write("\n\n---------------------------------SCENARIO: Print Success Postscript-----------------\n");
	    network.requestWorkstationPrintsDocument("Filip", "!PS Hello World in postscript", "Andy", report);
	    report.write("\n\n---------------------------------SCENARIO: Print Failure Postscript-----------------\n");
	    network.requestWorkstationPrintsDocument("Filip", "!PS Hello World in postscript", "Hans", report);
	    report.write("\n\n---------------------------------SCENARIO: Broadcast Success -----------------\n");
	    network.requestBroadcast(report);
	    generateOutput.write(report.toString());
	} catch (IOException exc) {
	} finally {
	    try {generateOutput.close();} catch (IOException exc) {};
	};
        assertTrue("Generated output is not as expected ", compareFiles(generateOutputFName, expectedOutputFName));
    }

  /*  static public class PreconditionViolationTestCase extends ExceptionTestCase {
            public PreconditionViolationTestCase(String name, Class exception) {
                super(name, exception);
            }*/

	public void test() {
	    Network network = Network.DefaultExample();
	    StringWriter report = new StringWriter(100);
	    network.requestWorkstationPrintsDocument("Filip",
					      "does not matter", "does not matter", report);
	}

  
    
/*    public void testPreconditionViolation() {
        PreconditionViolationTestCase test= new PreconditionViolationTestCase("test", AssertionError.class);
        TestResult result= test.;
        assertEquals(1, result.runCount());
        assertEquals(1, result.errorCount());
    }*/

}