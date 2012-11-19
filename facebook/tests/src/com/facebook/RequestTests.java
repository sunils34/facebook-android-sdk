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

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateRequest() {
        Request request = new Request();
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePostRequest() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();
        Request request = Request.newPostRequest(null, "me/statuses", graphObject, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.POST, request.getHttpMethod());
        assertEquals("me/statuses", request.getGraphPath());
        assertEquals(graphObject, request.getGraphObject());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateMeRequest() {
        Request request = Request.newMeRequest(null, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("me", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateMyFriendsRequest() {
        Request request = Request.newMyFriendsRequest(null, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("me/friends", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateUploadPhotoRequest() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        Request request = Request.newUploadPhotoRequest(null, image, null);
        assertTrue(request != null);

        Bundle parameters = request.getParameters();
        assertTrue(parameters != null);

        assertTrue(parameters.containsKey("picture"));
        assertEquals(image, parameters.getParcelable("picture"));
        assertEquals("me/photos", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePlacesSearchRequest() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        Request request = Request.newPlacesSearchRequest(null, location, 1000, 50, null, null);

        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("search", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePlacesSearchRequestRequiresLocationOrSearchText() {
        try {
            Request request = Request.newPlacesSearchRequest(null, null, 1000, 50, null, null);

            assertTrue(request != null);
            assertEquals(HttpMethod.GET, request.getHttpMethod());
            assertEquals("search", request.getGraphPath());

            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePlacesSearchRequestRequiresLocation() {
        try {
            Request.newPlacesSearchRequest(null, null, 1000, 50, null, null);
            fail("expected NullPointerException");
        } catch (FacebookException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSetHttpMethodToNilGivesDefault() {
        Request request = new Request();
        assertEquals(HttpMethod.GET, request.getHttpMethod());

        request.setHttpMethod(null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithNullRequestsThrows() {
        try {
            Request.executeBatchAndWait((Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithZeroRequestsThrows() {
        try {
            Request.executeBatchAndWait(new Request[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithNullRequestThrows() {
        try {
            Request.executeBatchAndWait(new Request[]{null});
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithNullRequestsThrows() {
        try {
            Request.toHttpConnection((Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithZeroRequestsThrows() {
        try {
            Request.toHttpConnection(new Request[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithNullRequestThrows() {
        try {
            Request.toHttpConnection(new Request[]{null});
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSingleGetToHttpRequest() throws Exception {
        Request requestMe = new Request(null, "TourEiffel");
        HttpURLConnection connection = Request.toHttpConnection(requestMe);

        assertTrue(connection != null);

        assertEquals("GET", connection.getRequestMethod());
        assertEquals("/TourEiffel", connection.getURL().getPath());

        assertTrue(connection.getRequestProperty("User-Agent").startsWith("FBAndroidSDK"));

        Uri uri = Uri.parse(connection.getURL().toString());
        assertEquals("android", uri.getQueryParameter("sdk"));
        assertEquals("json", uri.getQueryParameter("format"));
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGet() {
        Request request = new Request(null, "TourEiffel");
        Response response = request.executeAndWait();

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertTrue(response.getGraphObject() != null);

        GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
        assertEquals("Paris", graphPlace.getLocation().getCity());
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetUsingHttpURLConnection() throws IOException {
        Request request = new Request(null, "TourEiffel");
        HttpURLConnection connection = Request.toHttpConnection(request);

        List<Response> responses = Request.executeConnectionAndWait(connection, Arrays.asList(new Request[]{request}));
        assertNotNull(responses);
        assertEquals(1, responses.size());

        Response response = responses.get(0);

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertTrue(response.getGraphObject() != null);

        GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
        assertEquals("Paris", graphPlace.getLocation().getCity());

        // Make sure calling code can still access HTTP headers and call disconnect themselves.
        int code = connection.getResponseCode();
        assertEquals(200, code);
        assertTrue(connection.getHeaderFields().keySet().contains("Content-Length"));
        connection.disconnect();
    }

    @MediumTest
    @LargeTest
    public void testFacebookErrorResponseCreatesError() {
        Request request = new Request(null, "somestringthatshouldneverbeavalidfobjectid");
        Response response = request.executeAndWait();

        assertTrue(response != null);

        FacebookException exception = response.getError();
        assertTrue(exception != null);

        assertTrue(exception instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException = (FacebookServiceErrorException) exception;
        assertTrue(serviceException.getFacebookErrorType() != null);
        assertTrue(serviceException.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
        assertTrue(serviceException.getResponseBody() != null);
    }

    @LargeTest
    public void testFacebookSuccessResponseWithErrorCodeCreatesError() {
        TestSession session = openTestSessionWithSharedUser();

        Request request = Request.newRestRequest(session, "auth.extendSSOAccessToken", null, null);
        assertNotNull(request);

        // Because TestSession access tokens were not created via SSO, we expect to get an error from the service,
        // but with a 200 (success) code.
        Response response = request.executeAndWait();

        assertTrue(response != null);

        FacebookException exception = response.getError();
        assertTrue(exception != null);

        assertTrue(exception instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException = (FacebookServiceErrorException) exception;
        assertTrue(serviceException.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
        assertTrue(serviceException.getResponseBody() != null);
    }

    @MediumTest
    @LargeTest
    public void testRequestWithUnopenedSessionFails() {
        TestSession session = getTestSessionWithSharedUser(null);
        Request request = new Request(session, "me");
        Response response = request.executeAndWait();

        FacebookException exception = response.getError();
        assertNotNull(exception);
    }

    @MediumTest
    @LargeTest
    public void testExecuteRequestMe() {
        TestSession session = openTestSessionWithSharedUser();
        Request request = Request.newMeRequest(session, null);
        Response response = request.executeAndWait();

        validateMeResponse(session, response);
    }

    static void validateMeResponse(TestSession session, Response response) {
        FacebookException exception = response.getError();
        assertNull(exception);

        GraphUser me = response.getGraphObjectAs(GraphUser.class);
        assertNotNull(me);
        assertEquals(session.getTestUserId(), me.getId());
    }

    @MediumTest
    @LargeTest
    public void testExecuteMyFriendsRequest() {
        TestSession session = openTestSessionWithSharedUser();

        Request request = Request.newMyFriendsRequest(session, null);
        Response response = request.executeAndWait();

        validateMyFriendsResponse(session, response);
    }

    static void validateMyFriendsResponse(TestSession session, Response response) {
        assertNotNull(response);

        assertNull(response.getError());

        GraphMultiResult graphResult = response.getGraphObjectAs(GraphMultiResult.class);
        assertNotNull(graphResult);

        List<GraphObject> results = graphResult.getData();
        assertNotNull(results);
    }

    @MediumTest
    @LargeTest
    public void testExecutePlaceRequest() {
        TestSession session = openTestSessionWithSharedUser();

        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        Request request = Request.newPlacesSearchRequest(session, location, 5, 5, null, null);
        Response response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        GraphMultiResult graphResult = response.getGraphObjectAs(GraphMultiResult.class);
        assertNotNull(graphResult);

        List<GraphObject> results = graphResult.getData();
        assertNotNull(results);
    }

    @LargeTest
    public void testExecuteUploadPhoto() {
        TestSession session = openTestSessionWithSharedUser();
        Bitmap image = createTestBitmap(128);

        Request request = Request.newUploadPhotoRequest(session, image, null);
        Response response = request.executeAndWait();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObject result = response.getGraphObject();
        assertNotNull(result);
    }

    @LargeTest
    public void testExecuteUploadPhotoViaFile() throws IOException {
        File outputFile = null;
        FileOutputStream outStream = null;

        try {
            TestSession session = openTestSessionWithSharedUser();
            Bitmap image = createTestBitmap(128);

            File outputDir = getActivity().getCacheDir(); // context being the Activity pointer
            outputFile = File.createTempFile("prefix", "extension", outputDir);

            outStream = new FileOutputStream(outputFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
            outStream = null;

            Request request = Request.newUploadPhotoRequest(session, outputFile, null);
            Response response = request.executeAndWait();
            assertNotNull(response);

            Exception exception = response.getError();
            assertNull(exception);

            GraphObject result = response.getGraphObject();
            assertNotNull(result);
        } finally {
            if (outStream != null) {
                outStream.close();;
            }
            if (outputFile != null) {
                outputFile.delete();
            }
        }
    }

    @LargeTest
    public void testPostStatusUpdate() {
        TestSession session = openTestSessionWithSharedUser();

        GraphObject statusUpdate = createStatusUpdate();

        GraphObject retrievedStatusUpdate = postGetAndAssert(session, "me/feed", statusUpdate);

        assertEquals(statusUpdate.getProperty("message"), retrievedStatusUpdate.getProperty("message"));
    }

    @LargeTest
    public void testRestMethodGetUser() {
        TestSession session = openTestSessionWithSharedUser();
        String testUserId = session.getTestUserId();

        Bundle parameters = new Bundle();
        parameters.putString("uids", testUserId);
        parameters.putString("fields", "uid,name");

        Request request = Request.newRestRequest(session, "users.getInfo", parameters, null);
        Response response = request.executeAndWait();
        assertNotNull(response);

        GraphObjectList<GraphObject> graphObjects = response.getGraphObjectList();
        assertNotNull(graphObjects);
        assertEquals(1, graphObjects.size());

        GraphObject user = graphObjects.get(0);
        assertNotNull(user);
        assertEquals(testUserId, user.getProperty("uid").toString());
    }

    @MediumTest
    @LargeTest
    public void testCallbackIsCalled() {
        Request request = new Request(null, "4");

        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();
        request.setCallback(new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                calledBack.add(true);
            }
        });

        Response response = request.executeAndWait();
        assertNotNull(response);
        assertTrue(calledBack.size() == 1);
    }

    @MediumTest
    @LargeTest
    public void testBatchTimeoutIsApplied() {
        Request request = new Request(null, "me");
        RequestBatch batch = new RequestBatch(request);

        // We assume 1 ms is short enough to fail
        batch.setTimeout(1);

        List<Response> responses = Request.executeBatchAndWait(batch);
        assertNotNull(responses);
        assertTrue(responses.size() == 1);
        Response response = responses.get(0);
        assertNotNull(response);
        assertNotNull(response.getError());
    }

    @MediumTest
    @LargeTest
    public void testBatchTimeoutCantBeNegative() {
        try {
            RequestBatch batch = new RequestBatch();
            batch.setTimeout(-1);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    @MediumTest
    @LargeTest
    public void testCantSetBothGraphPathAndRestMethod() {
        Request request = new Request();
        request.setGraphPath("me");
        request.setRestMethod("amethod");
        request.setCallback(new ExpectFailureCallback());
        request.executeAndWait();
    }
}
