// NmraNetTest.java

package jmri.jmrix.can.nmranet;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for the jmri.jmrix.can.nmranet package.
 * @author      Bob Jacobsen  Copyright 2009
 * @version   $Revision: 1.1 $
 */
public class NmraNetTest extends TestCase {

    public void testDefinitions() {
    }
    
    // from here down is testing infrastructure

    public NmraNetTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        apps.tests.AllTest.initLogging();
        String[] testCaseName = {"-noloading", NmraNetTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite("jmri.jmrix.can.nmranet.NmraNetTest");
        // suite.addTest(jmri.jmrix.can.CanMessageTest.suite());
        return suite;
    }

    // The minimal setup for log4J
    protected void setUp() { apps.tests.Log4JFixture.setUp(); }
    protected void tearDown() { apps.tests.Log4JFixture.tearDown(); }
}
