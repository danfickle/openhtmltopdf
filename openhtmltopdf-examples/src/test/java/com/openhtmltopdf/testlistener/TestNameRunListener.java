package com.openhtmltopdf.testlistener;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

@org.junit.runner.notification.RunListener.ThreadSafe
public class TestNameRunListener extends RunListener {
    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);

        System.out.println();
        System.out.println(
            "##### " + description.getMethodName() + " of " +
            description.getClassName() + " #####");
    }
}
