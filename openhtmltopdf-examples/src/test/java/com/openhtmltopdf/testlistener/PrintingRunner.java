package com.openhtmltopdf.testlistener;

import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class PrintingRunner extends BlockJUnit4ClassRunner {
    public PrintingRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    public void run(RunNotifier notifier) {
        RunListener listener = new TestNameRunListener();
        notifier.addListener(listener);
        super.run(notifier);
        notifier.removeListener(listener);
    }
}
