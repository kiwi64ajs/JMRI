package jmri.server.json.roster;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import jmri.util.JUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Randall Wood Copyright 2018
 */
public class JsonRosterTest {

    @Before
    public void setUp() {
        JUnitUtil.setUp();
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    @Test
    public void testConstructor() throws Exception {
        try {
            Constructor<JsonRoster> constructor;
            constructor = JsonRoster.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            Assert.fail("Instance of JsonRoster created");
        } catch (InvocationTargetException ex) {
            // because the constructor throws UnsupportedOperationException, and
            // that is thrown by newInstance() into an InvocationTargetException
            // we pass an InvocationTargetException that is caused by an
            // UnsupportedOperationException and fail everything else by
            // rethrowing the unexepected exception to get a stack trace
            if (!ex.getCause().getClass().equals(UnsupportedOperationException.class)) {
                throw ex;
            }
        }
    }

}
