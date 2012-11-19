/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import junit.framework.AssertionFailedError;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    private static final String TAG = FacebookActivityTestCase.class.getSimpleName();

    private static String applicationId;
    private static String applicationSecret;

    public final static String SECOND_TEST_USER_TAG = "Second";
    public final static String THIRD_TEST_USER_TAG = "Third";

    private TestBlocker testBlocker;

    protected synchronized TestBlocker getTestBlocker() {
        if (testBlocker == null) {
            testBlocker = TestBlocker.createTestBlocker();
        }
        return testBlocker;
    }

    public FacebookActivityTestCase(Class<T> activityClass) {
        super("", activityClass);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithSharedUser() {
        return getTestSessionWithSharedUser(null);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithSharedUser(String sessionUniqueUserTag) {
        return getTestSessionWithSharedUserAndPermissions(sessionUniqueUserTag, (String[]) null);
    }

    protected TestSession getTestSessionWithSharedUserAndPermissions(String sessionUniqueUserTag,
            String... permissions) {
        List<String> permissionsList = (permissions != null) ? Arrays.asList(permissions) : null;
        return TestSession.createSessionWithSharedUser(getActivity(), permissionsList, sessionUniqueUserTag);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithPrivateUser(TestBlocker testBlocker) {
        return TestSession.createSessionWithPrivateUser(getActivity(), null);
    }

    protected TestSession openTestSessionWithSharedUser(final TestBlocker blocker) {
        return openTestSessionWithSharedUser(blocker, null);
    }

    protected TestSession openTestSessionWithSharedUser(final TestBlocker blocker, String sessionUniqueUserTag) {
        TestSession session = getTestSessionWithSharedUser();
        openSession(getActivity(), session, blocker);
        return session;
    }

    protected TestSession openTestSessionWithSharedUser() {
        return openTestSessionWithSharedUser((String) null);
    }

    protected TestSession openTestSessionWithSharedUser(String sessionUniqueUserTag) {
        return openTestSessionWithSharedUserAndPermissions(sessionUniqueUserTag, (String[]) null);
    }

    protected TestSession openTestSessionWithSharedUserAndPermissions(String sessionUniqueUserTag,
            String... permissions) {
        final TestBlocker blocker = getTestBlocker();
        TestSession session = getTestSessionWithSharedUserAndPermissions(sessionUniqueUserTag, permissions);
        openSession(getActivity(), session, blocker);
        return session;
    }

    // Turns exceptions from the TestBlocker into JUnit assertions
    protected void waitAndAssertSuccess(TestBlocker testBlocker, int numSignals) {
        try {
            testBlocker.waitForSignalsAndAssertSuccess(numSignals);
        } catch (AssertionFailedError e) {
            throw e;
        } catch (Exception e) {
            fail("Got exception: " + e.getMessage());
        }
    }

    protected void waitAndAssertSuccess(int numSignals) {
        waitAndAssertSuccess(getTestBlocker(), numSignals);
    }

    protected void waitAndAssertSuccessOrRethrow(int numSignals) throws Exception {
        getTestBlocker().waitForSignalsAndAssertSuccess(numSignals);
    }

    protected void runAndBlockOnUiThread(final int expectedSignals, final Runnable runnable) throws Throwable {
        final TestBlocker blocker = getTestBlocker();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                blocker.signal();
            }
        });
        // We wait for the operation to complete; wait for as many other signals as we expect.
        blocker.waitForSignals(1 + expectedSignals);
        // Wait for the UI thread to become idle so any UI updates the runnable triggered have a chance
        // to finish before we return.
        getInstrumentation().waitForIdleSync();
    }

    protected synchronized void readApplicationIdAndSecret() {
        synchronized (FacebookTestCase.class) {
            if (applicationId != null && applicationSecret != null) {
                return;
            }

            AssetManager assets = getInstrumentation().getContext().getResources().getAssets();
            InputStream stream = null;
            final String errorMessage = "could not read applicationId and applicationSecret from config.json; ensure "
                    + "you have run 'configure_unit_tests.sh'. Error: ";
            try {
                stream = assets.open("config.json");
                String string = Utility.readStreamToString(stream);

                JSONTokener tokener = new JSONTokener(string);
                Object obj = tokener.nextValue();
                if (!(obj instanceof JSONObject)) {
                    fail(errorMessage + "could not deserialize a JSONObject");
                }
                JSONObject jsonObject = (JSONObject) obj;

                applicationId = jsonObject.optString("applicationId");
                applicationSecret = jsonObject.optString("applicationSecret");

                if (Utility.isNullOrEmpty(applicationId) || Utility.isNullOrEmpty(applicationSecret)) {
                    fail(errorMessage + "one or both config values are missing");
                }

                String machineUniqueUserTag = jsonObject.optString("machineUniqueUserTag");

                TestSession.setTestApplicationId(applicationId);
                TestSession.setTestApplicationSecret(applicationSecret);
                TestSession.setMachineUniqueUserTag(machineUniqueUserTag);
            } catch (IOException e) {
                fail(errorMessage + e.toString());
            } catch (JSONException e) {
                fail(errorMessage + e.toString());
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        fail(errorMessage + e.toString());
                    }
                }
            }
        }
    }

    protected void openSession(Activity activity, TestSession session) {
        final TestBlocker blocker = getTestBlocker();
        openSession(activity, session, blocker);
    }

    protected void openSession(Activity activity, TestSession session, final TestBlocker blocker) {
        Session.OpenRequest openRequest = new Session.OpenRequest(activity).
                setCallback(new Session.StatusCallback() {
                    boolean signaled = false;

                    @Override
                    public void call(Session session, SessionState state, Exception exception) {
                        if (exception != null) {
                            Log.w(TAG,
                                    "openSession: received an error opening session: " + exception.toString());
                        }
                        assertTrue(exception == null);
                        // Only signal once, or we might screw up the count on the blocker.
                        if (!signaled) {
                            blocker.signal();
                            signaled = true;
                        }
                    }
                });

        session.openForRead(openRequest);
        waitAndAssertSuccess(blocker, 1);
    }

    protected void setUp() throws Exception {
        super.setUp();

        // Make sure we have read application ID and secret.
        readApplicationIdAndSecret();

        // These are useful for debugging unit test failures.
        Settings.addLoggingBehavior(LoggingBehaviors.REQUESTS);
        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_ACCESS_TOKENS);

        // We want the UI thread to be in StrictMode to catch any violations.
        turnOnStrictModeForUiThread();
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        if (testBlocker != null) {
            testBlocker.quit();
        }
    }

    interface GraphObjectPostResult extends GraphObject {
        String getId();
    }

    protected GraphObject getAndAssert(Session session, String id) {
        Request request = new Request(session, id);
        Response response = request.executeAndWait();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObject result = response.getGraphObject();
        assertNotNull(result);

        return result;
    }

    protected GraphObject postGetAndAssert(Session session, String path, GraphObject graphObject) {
        Request request = Request.newPostRequest(session, path, graphObject, null);
        Response response = request.executeAndWait();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObjectPostResult result = response.getGraphObjectAs(GraphObjectPostResult.class);
        assertNotNull(result);
        assertNotNull(result.getId());

        return getAndAssert(session, result.getId());
    }

    protected void setBatchApplicationIdForTestApp() {
        String appId = TestSession.getTestApplicationId();
        Request.setDefaultBatchApplicationId(appId);
    }

    protected <U extends GraphObject> U batchCreateAndGet(Session session, String graphPath, GraphObject graphObject,
            String fields, Class<U> resultClass) {
        Request create = Request.newPostRequest(session, graphPath, graphObject, new ExpectSuccessCallback());
        create.setBatchEntryName("create");
        Request get = Request.newGraphPathRequest(session, "{result=create:$.id}", new ExpectSuccessCallback());
        if (fields != null) {
            Bundle parameters = new Bundle();
            parameters.putString("fields", fields);
            get.setParameters(parameters);
        }

        return batchPostAndGet(create, get, resultClass);
    }

    protected <U extends GraphObject> U batchUpdateAndGet(Session session, String graphPath, GraphObject graphObject,
            String fields, Class<U> resultClass) {
        Request update = Request.newPostRequest(session, graphPath, graphObject, new ExpectSuccessCallback());
        Request get = Request.newGraphPathRequest(session, graphPath, new ExpectSuccessCallback());
        if (fields != null) {
            Bundle parameters = new Bundle();
            parameters.putString("fields", fields);
            get.setParameters(parameters);
        }

        return batchPostAndGet(update, get, resultClass);
    }

    protected <U extends GraphObject> U batchPostAndGet(Request post, Request get, Class<U> resultClass) {
        List<Response> responses = Request.executeBatchAndWait(post, get);
        assertEquals(2, responses.size());

        U resultGraphObject = responses.get(1).getGraphObjectAs(resultClass);
        assertNotNull(resultGraphObject);
        return resultGraphObject;
    }

    protected GraphObject createStatusUpdate() {
        GraphObject statusUpdate = GraphObjectWrapper.createGraphObject();
        String message = String.format(
                "Check out my awesome new status update posted at: %s. Some chars for you: +\"[]:,", new Date());
        statusUpdate.setProperty("message", message);
        return statusUpdate;
    }

    protected Bitmap createTestBitmap(int size) {
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        image.eraseColor(Color.BLUE);
        return image;
    }

    protected void issueFriendRequest(TestSession session, String targetUserId) {
        String graphPath = "me/friends/" + targetUserId;
        Request request = Request.newPostRequest(session, graphPath, null, null);
        Response response = request.executeAndWait();
        // We will get a 400 error if the users are already friends.
        FacebookException error = response.getError();
        assertTrue(error == null ||
                (error instanceof FacebookServiceErrorException && ((FacebookServiceErrorException) error)
                        .getHttpResponseCode() == 400));
    }

    protected void makeTestUsersFriends(TestSession session1, TestSession session2) {
        issueFriendRequest(session1, session2.getTestUserId());
        issueFriendRequest(session2, session1.getTestUserId());
    }

    protected void assertNoErrors(List<Response> responses) {
        for (int i = 0; i < responses.size(); ++i) {
            Response response = responses.get(i);
            assertNotNull(response);
            assertNull(response.getError());
        }
    }

    protected void runOnBlockerThread(final Runnable runnable, boolean waitForCompletion) {
        Runnable runnableToPost = runnable;
        final ConditionVariable condition = waitForCompletion ? new ConditionVariable(!waitForCompletion) : null;

        if (waitForCompletion) {
            runnableToPost = new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    condition.open();
                }
            };
        }

        TestBlocker blocker = getTestBlocker();
        Handler handler = blocker.getHandler();
        handler.post(runnableToPost);

        if (waitForCompletion) {
            boolean success = condition.block(10000);
            assertTrue(success);
        }
    }

    protected void closeBlockerAndAssertSuccess() {
        TestBlocker blocker = getTestBlocker();
        testBlocker = null;

        blocker.quit();

        boolean joined = false;
        while (!joined) {
            try {
                blocker.join();
                joined = true;
            } catch (InterruptedException e) {
            }
        }

        try {
            blocker.assertSuccess();
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    protected TestRequestAsyncTask createAsyncTaskOnUiThread(final Request... requests) throws Throwable {
        final ArrayList<TestRequestAsyncTask> result = new ArrayList<TestRequestAsyncTask>();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.add(new TestRequestAsyncTask(requests));
            }
        });
        return result.isEmpty() ? null : result.get(0);
    }


    /*
     * Classes and helpers related to asynchronous requests.
     */

    // A subclass of RequestAsyncTask that knows how to interact with TestBlocker to ensure that tests can wait
    // on and assert success of async tasks.
    protected class TestRequestAsyncTask extends RequestAsyncTask {
        private final TestBlocker blocker = FacebookActivityTestCase.this.getTestBlocker();

        public TestRequestAsyncTask(Request... requests) {
            super(requests);
        }

        public TestRequestAsyncTask(List<Request> requests) {
            super(requests);
        }

        public TestRequestAsyncTask(RequestBatch requests) {
            super(requests);
        }

        public TestRequestAsyncTask(HttpURLConnection connection, Request... requests) {
            super(connection, requests);
        }

        public TestRequestAsyncTask(HttpURLConnection connection, List<Request> requests) {
            super(connection, requests);
        }

        public TestRequestAsyncTask(HttpURLConnection connection, RequestBatch requests) {
            super(connection, requests);
        }

        public final TestBlocker getBlocker() {
            return blocker;
        }

        public final Exception getThrowable() {
            return getException();
        }

        protected void onPostExecute(List<Response> result) {
            try {
                super.onPostExecute(result);

                if (getException() != null) {
                    blocker.setException(getException());
                }
            } finally {
                Log.d("TestRequestAsyncTask", "signaling blocker");
                blocker.signal();
            }
        }

        // In order to be able to block and accumulate exceptions, we want to ensure the async task is really
        // being started on the blocker's thread, rather than the test's thread. Use this instead of calling
        // execute directly in unit tests.
        public void executeOnBlockerThread() {
            ensureAsyncTaskLoaded();

            Runnable runnable = new Runnable() {
                public void run() {
                    execute();
                }
            };
            Handler handler = new Handler(blocker.getLooper());
            handler.post(runnable);
        }

        private void ensureAsyncTaskLoaded() {
            // Work around this issue on earlier frameworks: http://stackoverflow.com/a/7818839/782044
            try {
                runAndBlockOnUiThread(0, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Class.forName("android.os.AsyncTask");
                        } catch (ClassNotFoundException e) {
                        }
                    }
                });
            } catch (Throwable throwable) {
            }
        }
    }

    // Provides an implementation of Request.Callback that will assert either success (no error) or failure (error)
    // of a request, and allow derived classes to perform additional asserts.
    protected class TestCallback implements Request.Callback {
        private final TestBlocker blocker;
        private final boolean expectSuccess;

        public TestCallback(TestBlocker blocker, boolean expectSuccess) {
            this.blocker = blocker;
            this.expectSuccess = expectSuccess;
        }

        public TestCallback(boolean expectSuccess) {
            this(FacebookActivityTestCase.this.getTestBlocker(), expectSuccess);
        }

        @Override
        public void onCompleted(Response response) {
            try {
                // We expect to be called on the right thread.
                if (Thread.currentThread() != blocker) {
                    throw new FacebookException("Invalid thread " + Thread.currentThread().getId()
                            + "; expected to be called on thread " + blocker.getId());
                }

                // We expect either success or failure.
                if (expectSuccess && response.getError() != null) {
                    throw response.getError();
                } else if (!expectSuccess && response.getError() == null) {
                    throw new FacebookException("Expected failure case, received no error");
                }

                // Some tests may want more fine-grained control and assert additional conditions.
                performAsserts(response);
            } catch (Exception e) {
                blocker.setException(e);
            } finally {
                // Tell anyone waiting on us that this callback was called.
                blocker.signal();
            }
        }

        protected void performAsserts(Response response) {
        }
    }

    // A callback that will assert if the request resulted in an error.
    protected class ExpectSuccessCallback extends TestCallback {
        public ExpectSuccessCallback() {
            super(true);
        }
    }

    // A callback that will assert if the request did NOT result in an error.
    protected class ExpectFailureCallback extends TestCallback {
        public ExpectFailureCallback() {
            super(false);
        }
    }

    private AtomicBoolean strictModeOnForUiThread = new AtomicBoolean();
    protected void turnOnStrictModeForUiThread() {
        // We only ever need to do this once. If the boolean is true, we know that the next runnable
        // posted to the UI thread will have strict mode on.
        if (strictModeOnForUiThread.get() == false) {
            try {
                runTestOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Double-check whether we really need to still do this on the UI thread.
                        if (strictModeOnForUiThread.compareAndSet(false, true)) {
                            turnOnStrictModeForThisThread();
                        }
                    }
                });
            } catch (Throwable throwable) {
            }
        }
    }

    protected void turnOnStrictModeForThisThread() {
        // We use reflection, because Instrumentation will complain about any references to StrictMode in API versions < 9
        // when attempting to run the unit tests. No particular effort has been made to make this efficient, since we
        // expect to call it just once.
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, loader);
            Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, loader);
            Class<?> threadPolicyBuilderClass = Class.forName("android.os.StrictMode$ThreadPolicy$Builder", true,
                    loader);

            Object threadPolicyBuilder = threadPolicyBuilderClass.getConstructor().newInstance();
            threadPolicyBuilder = threadPolicyBuilderClass.getMethod("detectAll").invoke(threadPolicyBuilder);
            threadPolicyBuilder = threadPolicyBuilderClass.getMethod("penaltyDeath").invoke(threadPolicyBuilder);

            Object threadPolicy = threadPolicyBuilderClass.getMethod("build").invoke(threadPolicyBuilder);
            strictModeClass.getMethod("setThreadPolicy", threadPolicyClass).invoke(strictModeClass, threadPolicy);
        } catch (Exception ex) {
        }
    }
}
