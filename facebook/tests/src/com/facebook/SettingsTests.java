package com.facebook;

import android.os.ConditionVariable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.concurrent.Executor;

public final class SettingsTests extends AndroidTestCase {

    @SmallTest @MediumTest @LargeTest
    public void testGetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        Settings.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                condition.open();
            }
        });

        boolean success = condition.block(5000);
        assertTrue(success);
    }

    @SmallTest @MediumTest @LargeTest
    public void testSetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() { }
        };

        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                assertEquals(runnable, command);
                command.run();

                condition.open();
            }
        };

        Executor original = Settings.getExecutor();
        try {
            Settings.setExecutor(executor);
            Settings.getExecutor().execute(runnable);

            boolean success = condition.block(5000);
            assertTrue(success);
        } finally {
            Settings.setExecutor(original);
        }
    }
}
