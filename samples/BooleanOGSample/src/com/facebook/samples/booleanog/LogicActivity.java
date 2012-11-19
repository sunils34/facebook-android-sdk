package com.facebook.samples.booleanog;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class LogicActivity extends FacebookActivity {

    private static final String TAG = "BooleanOpenGraphSample";

    private static final String SAVE_ACTIVE_TAB = TAG + ".SAVE_ACTIVE_TAB";
    private static final String SAVE_CONTENT_SELECTION = TAG + ".SAVE_CONTENT_SELECTION";
    private static final String SAVE_LEFT_OPERAND_SELECTION = TAG + ".SAVE_LEFT_OPERAND_SELECTION";
    private static final String SAVE_RIGHT_OPERAND_SELECTION = TAG + ".SAVE_RIGHT_OPERAND_SELECTION";
    private static final String SAVE_RESULT_TEXT = TAG + ".SAVE_RESULT_TEXT";
    private static final String SAVE_POST_RESULT_TEXT = TAG + ".SAVE_POST_RESULT_TEXT";
    private static final String SAVE_PENDING = TAG + ".SAVE_PENDING";
    private static final String PENDING_POST_PATH = "PENDING_POST_PATH";
    private static final String PENDING_POST_LEFT = "PENDING_POST_LEFT";
    private static final String PENDING_POST_RIGHT = "PENDING_POST_RIGHT";
    private static final String PENDING_POST_RESULT = "PENDING_POST_RESULT";

    private static final String AND_ACTION = "fb_sample_boolean_og:and";
    private static final String OR_ACTION = "fb_sample_boolean_og:or";
    private static final String POST_AND_ACTION_PATH = "me/" + AND_ACTION;
    private static final String POST_OR_ACTION_PATH = "me/" + OR_ACTION;
    private static final String TRUE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360019783304";
    private static final String FALSE_GRAPH_OBJECT_URL = "http://samples.ogp.me/369360256449947";
    private static final String INSTALLED = "installed";
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static volatile TruthValueGraphObject TRUE_GRAPH_OBJECT;
    private static volatile TruthValueGraphObject FALSE_GRAPH_OBJECT;
    private static volatile int TRUE_SPINNER_INDEX = -1;
    private static volatile int FALSE_SPINNER_INDEX = -1;

    // Main layout
    private Button logicButton;
    private Button friendsButton;
    private Button settingsButton;
    private Button contentButton;
    private String activeTab;

    // Logic group
    private ViewGroup logicGroup;
    private Spinner leftSpinner;
    private Spinner rightSpinner;
    private Button andButton;
    private Button orButton;
    private TextView resultText;
    private TextView postResultText;
    private Bundle pendingPost;

    // Friends group
    private ViewGroup friendsGroup;
    private FriendPickerFragment friendPickerFragment;
    private RequestAsyncTask pendingRequest;
    private SimpleCursorAdapter friendActivityAdapter;
    private ProgressBar friendActivityProgressBar;

    // Login group
    private ViewGroup settingsGroup;
    private LoginFragment loginFragment;

    // Content group
    private ViewGroup contentGroup;
    private ImageView contentImage;
    private Spinner contentSpinner;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Views
        logicButton = (Button) findViewById(R.id.logic_button);
        friendsButton = (Button) findViewById(R.id.friends_button);
        settingsButton = (Button) findViewById(R.id.settings_button);
        contentButton = (Button) findViewById(R.id.content_button);

        logicGroup = (ViewGroup) findViewById(R.id.logic_group);
        leftSpinner = (Spinner) findViewById(R.id.left_spinner);
        rightSpinner = (Spinner) findViewById(R.id.right_spinner);
        andButton = (Button) findViewById(R.id.and_button);
        orButton = (Button) findViewById(R.id.or_button);
        resultText = (TextView) findViewById(R.id.result_text);
        postResultText = (TextView) findViewById(R.id.post_result_text);

        friendsGroup = (ViewGroup) findViewById(R.id.friends_group);
        ListView friendActivityList = (ListView) findViewById(R.id.friend_activity_list);
        String[] mapColumnNames = {"date", "action"};
        int[] mapViewIds = {R.id.friend_action_date, R.id.friend_action_data};
        friendActivityAdapter = new SimpleCursorAdapter(this, R.layout.friend_activity_row, createEmptyCursor(),
                mapColumnNames, mapViewIds);
        friendActivityList.setAdapter(friendActivityAdapter);
        friendActivityProgressBar = (ProgressBar) findViewById(R.id.friend_activity_progress_bar);

        settingsGroup = (ViewGroup) findViewById(R.id.settings_group);

        contentGroup = (ViewGroup) findViewById(R.id.content_group);
        contentImage = (ImageView) findViewById(R.id.content_image);
        contentSpinner = (Spinner) findViewById(R.id.content_spinner);

        // Fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        friendPickerFragment = (FriendPickerFragment) fragmentManager.findFragmentById(R.id.friend_picker_fragment);
        if (friendPickerFragment == null) {
            Bundle args = new Bundle();
            args.putBoolean(FriendPickerFragment.SHOW_TITLE_BAR_BUNDLE_KEY, false);
            friendPickerFragment = new FriendPickerFragment(args);
            transaction.add(R.id.friend_picker_fragment, friendPickerFragment);
        }

        loginFragment = (LoginFragment) fragmentManager.findFragmentById(R.id.login_fragment);
        if (loginFragment == null) {
            loginFragment = new LoginFragment();
            transaction.add(R.id.login_fragment, loginFragment);
        }

        transaction.commit();

        // Spinners
        ArrayAdapter<CharSequence> truthAdapter = ArrayAdapter
                .createFromResource(this, R.array.truth_values, android.R.layout.simple_spinner_item);
        truthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leftSpinner.setAdapter(truthAdapter);
        rightSpinner.setAdapter(truthAdapter);
        contentSpinner.setAdapter(truthAdapter);
        leftSpinner.setSelection(0);
        rightSpinner.setSelection(0);

        // Navigation
        for (Button button : Arrays.asList(logicButton, friendsButton, settingsButton, contentButton)) {
            initializeNavigationButton(button);
        }

        // Logic
        initializeCalculationButton(andButton);
        initializeCalculationButton(orButton);

        // Friends
        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                LogicActivity.this.onError(error);
            }
        });
        friendPickerFragment.setUserId("me");
        friendPickerFragment.setMultiSelect(false);
        friendPickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged() {
                LogicActivity.this.onFriendSelectionChanged();
            }
        });
        friendPickerFragment.setExtraFields(Arrays.asList(INSTALLED));
        friendPickerFragment.setFilter(new PickerFragment.GraphObjectFilter<GraphUser>() {
            @Override
            public boolean includeItem(GraphUser graphObject) {
                Boolean installed = graphObject.cast(GraphUserWithInstalled.class).getInstalled();
                return (installed != null) && installed.booleanValue();
            }
        });

        // Content
        contentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                LogicActivity.this.onContentSelectionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                LogicActivity.this.onContentSelectionChanged();
            }
        });

        // Restore saved state
        Button startButton = logicButton;

        if (savedInstanceState != null) {
            leftSpinner.setSelection(savedInstanceState.getInt(SAVE_LEFT_OPERAND_SELECTION));
            rightSpinner.setSelection(savedInstanceState.getInt(SAVE_RIGHT_OPERAND_SELECTION));
            contentSpinner.setSelection(savedInstanceState.getInt(SAVE_CONTENT_SELECTION));
            resultText.setText(savedInstanceState.getString(SAVE_RESULT_TEXT));
            postResultText.setText(savedInstanceState.getString(SAVE_POST_RESULT_TEXT));
            activeTab = savedInstanceState.getString(SAVE_ACTIVE_TAB);
            pendingPost = (Bundle) savedInstanceState.getBundle(SAVE_PENDING);

            if (getString(R.string.navigate_friends).equals(activeTab)) {
                startButton = friendsButton;
            } else if (getString(R.string.navigate_content).equals(activeTab)) {
                startButton = contentButton;
            } else if (getString(R.string.navigate_settings).equals(activeTab)) {
                startButton = settingsButton;
            }
        }

        // Resolve deep-links, if any
        Boolean deepLinkContent = getDeepLinkContent(getIntent().getData());
        if (deepLinkContent != null) {
            startButton = contentButton;
            contentSpinner.setSelection(getSpinnerPosition(deepLinkContent));
        }

        onNavigateButtonClick(startButton);
    }

    // -----------------------------------------------------------------------------------
    // Activity lifecycle

    @Override
    protected void onStart() {
        super.onStart();
        loadIfSessionValid();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadIfSessionValid();
            }
        };

        IntentFilter openedFilter = new IntentFilter(Session.ACTION_ACTIVE_SESSION_OPENED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, openedFilter);
    }

    private void loadIfSessionValid() {
        Session session = Session.getActiveSession();
        if ((session != null) && session.isOpened()) {
            friendPickerFragment.loadData(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVE_LEFT_OPERAND_SELECTION, leftSpinner.getSelectedItemPosition());
        outState.putInt(SAVE_RIGHT_OPERAND_SELECTION, rightSpinner.getSelectedItemPosition());
        outState.putInt(SAVE_CONTENT_SELECTION, contentSpinner.getSelectedItemPosition());
        outState.putString(SAVE_RESULT_TEXT, resultText.getText().toString());
        outState.putString(SAVE_POST_RESULT_TEXT, postResultText.getText().toString());
        outState.putString(SAVE_ACTIVE_TAB, activeTab);
        outState.putBundle(SAVE_PENDING, pendingPost);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        friendPickerFragment.setOnErrorListener(null);
        friendPickerFragment.setOnSelectionChangedListener(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginFragment.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        if (exception != null) {
            pendingPost = null;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            sendPendingPost();
        }
    }

    // -----------------------------------------------------------------------------------
    // Navigation

    private void initializeNavigationButton(Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNavigateButtonClick((Button) view);
            }
        });
    }

    private void onNavigateButtonClick(Button source) {
        activeTab = source.getText().toString();

        logicGroup.setVisibility(getGroupVisibility(source, logicButton));
        friendsGroup.setVisibility(getGroupVisibility(source, friendsButton));
        settingsGroup.setVisibility(getGroupVisibility(source, settingsButton));
        contentGroup.setVisibility(getGroupVisibility(source, contentButton));
    }

    private int getGroupVisibility(Button source, Button groupButton) {
        if (source == groupButton) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    // -----------------------------------------------------------------------------------
    // Logic group

    private void initializeCalculationButton(Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOperationButtonClick(view);
            }
        });
    }

    private void onOperationButtonClick(View view) {
        if (view == andButton) {
            onAndButtonClick();
        } else if (view == orButton) {
            onOrButtonClick();
        } else {
            assert false;
        }
    }

    private void onAndButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand && rightOperand;

        resultText.setText(getLogicText(getString(R.string.and_operation), leftOperand, rightOperand, result));
        postAction(POST_AND_ACTION_PATH, leftOperand, rightOperand, result);
    }

    private void onOrButtonClick() {
        boolean leftOperand = getSpinnerBoolean(leftSpinner);
        boolean rightOperand = getSpinnerBoolean(rightSpinner);
        boolean result = leftOperand || rightOperand;

        resultText.setText(getLogicText(getString(R.string.or_operation), leftOperand, rightOperand, result));
        postAction(POST_OR_ACTION_PATH, leftOperand, rightOperand, result);
    }

    private String getLogicText(String op, boolean leftOperand, boolean rightOperand, boolean result) {
        String trueString = getString(R.string.true_value);
        String falseString = getString(R.string.false_value);
        String arg0String = leftOperand ? trueString : falseString;
        String arg1String = rightOperand ? trueString : falseString;
        String resultString = result ? trueString : falseString;

        return String.format("%s %s %s = %s", arg0String, op, arg1String, resultString);
    }

    private void postAction(final String actionPath, final boolean leftOperand, final boolean rightOperand,
            final boolean result) {
        Bundle post = new Bundle();
        post.putString(PENDING_POST_PATH, actionPath);
        post.putBoolean(PENDING_POST_LEFT, leftOperand);
        post.putBoolean(PENDING_POST_RIGHT, rightOperand);
        post.putBoolean(PENDING_POST_RESULT, result);
        pendingPost = post;

        sendPendingPost();
    }

    private void sendPendingPost() {
        if (pendingPost == null) {
            return;
        }

        Session session = Session.getActiveSession();
        if ((session == null) || !session.isOpened()) {
            postResultText.setText("Not logged in, no post generated.");
            pendingPost = null;
            return;
        }

        List<String> permissions = session.getPermissions();
        if (!permissions.containsAll(PERMISSIONS)) {
            Session.ReauthorizeRequest reauthRequest = new Session.ReauthorizeRequest(this, PERMISSIONS);
            session.reauthorizeForPublish(reauthRequest);
            return;
        }

        postResultText.setText("Posting action...");

        // For demo purposes, result is just a boolean, but operands are Open Graph objects
        String actionPath = pendingPost.getString(PENDING_POST_PATH);
        boolean leftOperand = pendingPost.getBoolean(PENDING_POST_LEFT);
        boolean rightOperand = pendingPost.getBoolean(PENDING_POST_RIGHT);
        boolean result = pendingPost.getBoolean(PENDING_POST_RESULT);

        LogicAction action = GraphObjectWrapper.createGraphObject(LogicAction.class);
        action.setResult(result);
        action.setTruthvalue(getTruthValueObject(leftOperand));
        action.setAnothertruthvalue(getTruthValueObject(rightOperand));

        Request.Callback callback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                onPostActionResponse(response);
            }
        };
        Request request = new Request(session, actionPath, null, HttpMethod.POST,
                callback);
        request.setGraphObject(action);
        RequestAsyncTask task = new RequestAsyncTask(request);

        task.execute();
    }

    private void onPostActionResponse(Response response) {
        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);

        String id = null;
        PostResponse.Body body = null;
        if (postResponse != null) {
            id = postResponse.getId();
            body = postResponse.getBody();
        }

        PostResponse.Error error = null;
        if (body != null) {
            error = body.getError();
        }

        String errorMessage = null;
        if (error != null) {
            errorMessage = error.getMessage();
        }

        if (errorMessage != null) {
            postResultText.setText(errorMessage);
        } else if (response.getError() != null) {
            postResultText.setText(response.getError().getLocalizedMessage());
        } else if (id != null) {
            postResultText.setText("Post id = " + id);
        } else {
            postResultText.setText("");
        }
    }

    private TruthValueGraphObject getTruthValueObject(boolean value) {
        if (value) {
            if (TRUE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObjectWrapper.createGraphObject(TruthValueGraphObject.class);
                object.setUrl(TRUE_GRAPH_OBJECT_URL);
                TRUE_GRAPH_OBJECT = object;
            }
            return TRUE_GRAPH_OBJECT;
        } else {
            if (FALSE_GRAPH_OBJECT == null) {
                TruthValueGraphObject object = GraphObjectWrapper.createGraphObject(TruthValueGraphObject.class);
                object.setUrl(FALSE_GRAPH_OBJECT_URL);
                FALSE_GRAPH_OBJECT = object;
            }
            return FALSE_GRAPH_OBJECT;
        }
    }

    // -----------------------------------------------------------------------------------
    // Friends group

    private void onFriendSelectionChanged() {
        GraphUser user = chooseOne(friendPickerFragment.getSelection());
        if (user != null) {
            onChooseFriend(user.getId());
        } else {
            friendActivityAdapter.changeCursor(createEmptyCursor());
        }
    }

    private void onChooseFriend(String friendId) {
        friendActivityProgressBar.setVisibility(View.VISIBLE);

        String andPath = String.format("%s/%s", friendId, AND_ACTION);
        String orPath = String.format("%s/%s", friendId, OR_ACTION);
        Request getAnds = new Request(Session.getActiveSession(), andPath, null, HttpMethod.GET);
        Request getOrs = new Request(Session.getActiveSession(), orPath, null, HttpMethod.GET);

        RequestBatch batch = new RequestBatch(getAnds, getOrs);

        if (pendingRequest != null) {
            pendingRequest.cancel(true);
        }

        pendingRequest = new RequestAsyncTask(batch) {
            @Override
            protected void onPostExecute(List<Response> result) {
                if (pendingRequest == this) {
                    pendingRequest = null;

                    LogicActivity.this.onPostExecute(result);
                }
            }
        };

        pendingRequest.execute();
    }

    private void onPostExecute(List<Response> result) {
        friendActivityProgressBar.setVisibility(View.GONE);

        ArrayList<ActionRow> publishedItems = createActionRows(result);
        updateCursor(publishedItems);
    }

    private ArrayList<ActionRow> createActionRows(List<Response> result) {
        ArrayList<ActionRow> publishedItems = new ArrayList<ActionRow>();

        for (Response response : result) {
            if (response.getError() != null) {
                continue;
            }

            GraphMultiResult list = response.getGraphObjectAs(GraphMultiResult.class);
            List<PublishedLogicAction> listData = list.getData().castToListOf(PublishedLogicAction.class);

            for (PublishedLogicAction action : listData) {
                publishedItems.add(createActionRow(action));
            }
        }

        Collections.sort(publishedItems);
        return publishedItems;
    }

    private void updateCursor(Iterable<ActionRow> publishedItems) {
        MatrixCursor cursor = createEmptyCursor();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int id = 0;
        for (ActionRow item : publishedItems) {
            Object[] row = new Object[3];
            row[0] = id++;
            row[1] = dateFormat.format(item.publishDate);
            row[2] = item.actionText;
            cursor.addRow(row);
        }

        friendActivityAdapter.changeCursor(cursor);
        friendActivityAdapter.notifyDataSetChanged();
    }

    private MatrixCursor createEmptyCursor() {
        String[] cursorColumns = {"_ID", "date", "action"};
        return new MatrixCursor(cursorColumns);
    }

    private ActionRow createActionRow(PublishedLogicAction action) {
        String actionText = getActionText(action);
        Date publishDate = action.getPublishTime();

        return new ActionRow(actionText, publishDate);
    }

    private String getActionText(PublishedLogicAction action) {
        LogicAction actionData = action.getData();
        if (actionData == null) {
            return "";
        }

        TruthValueGraphObject left = actionData.getTruthvalue();
        TruthValueGraphObject right = actionData.getAnothertruthvalue();
        Boolean actionResult = actionData.getResult();

        String verb = action.getType();
        if (AND_ACTION.equals(verb)) {
            verb = getString(R.string.and_operation);
        } else if (OR_ACTION.equals(verb)) {
            verb = getString(R.string.or_operation);
        }

        if ((left == null) || (right == null) || (actionResult == null) || (verb == null)) {
            return "";
        }

        return String.format("%s %s %s = %s", left.getTitle(), verb, right.getTitle(), actionResult.toString());
    }

    // -----------------------------------------------------------------------------------
    // Content group

    private Boolean getDeepLinkContent(Uri deepLinkUri) {
        if (deepLinkUri != null) {
            String deepLink = deepLinkUri.toString();

            if (deepLink.startsWith(TRUE_GRAPH_OBJECT_URL)) {
                return Boolean.TRUE;
            } else if (deepLink.startsWith(FALSE_GRAPH_OBJECT_URL)) {
                return Boolean.FALSE;
            }
        }

        return null;
    }

    private void onContentSelectionChanged() {
        Boolean spinnerBoolean = getSpinnerBoolean(contentSpinner);
        if (Boolean.TRUE.equals(spinnerBoolean)) {
            contentImage.setVisibility(View.VISIBLE);
            contentImage.setImageResource(R.drawable.true_content);
        } else if (Boolean.FALSE.equals(spinnerBoolean)) {
            contentImage.setVisibility(View.VISIBLE);
            contentImage.setImageResource(R.drawable.false_content);
        } else {
            contentImage.setImageResource(View.INVISIBLE);
        }
    }

    // -----------------------------------------------------------------------------------
    // Utility methods

    private int getSpinnerPosition(Boolean value) {
        initializeSpinnerIndexes();

        if (Boolean.TRUE.equals(value)) {
            return TRUE_SPINNER_INDEX;
        } else if (Boolean.FALSE.equals(value)) {
            return FALSE_SPINNER_INDEX;
        } else {
            return -1;
        }
    }

    private Boolean getSpinnerBoolean(Spinner spinner) {
        initializeSpinnerIndexes();

        int position = spinner.getSelectedItemPosition();
        if (position == TRUE_SPINNER_INDEX) {
            return Boolean.TRUE;
        } else if (position == FALSE_SPINNER_INDEX) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    private void initializeSpinnerIndexes() {
        if ((TRUE_SPINNER_INDEX < 0) || (FALSE_SPINNER_INDEX < 0)) {
            String[] truthArray = getResources().getStringArray(R.array.truth_values);
            List<String> truthList = Arrays.asList(truthArray);
            TRUE_SPINNER_INDEX = truthList.indexOf(getString(R.string.true_value));
            FALSE_SPINNER_INDEX = truthList.indexOf(getString(R.string.false_value));
        }
    }

    private void onError(Exception error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(error.getMessage()).setPositiveButton("OK", null);
        builder.show();
    }

    private <T> T chooseOne(List<T> ts) {
        for (T t : ts) {
            return t;
        }

        return null;
    }

    // -----------------------------------------------------------------------------------
    // Supporting types

    private interface GraphUserWithInstalled extends GraphUser {
        Boolean getInstalled();
    }

    private class ActionRow implements Comparable<ActionRow> {
        final String actionText;
        final Date publishDate;

        ActionRow(String actionText, Date publishDate) {
            this.actionText = actionText;
            this.publishDate = publishDate;
        }

        @Override
        public int compareTo(ActionRow other) {
            if (other == null) {
                return 1;
            } else {
                return publishDate.compareTo(other.publishDate);
            }
        }
    }

    /**
     * Used to create and consume TruthValue open graph objects.
     */
    private interface TruthValueGraphObject extends GraphObject {
        void setUrl(String url);

        String getTitle();
    }

    /**
     * Used to create and consume And an Or open graph actions
     */
    private interface LogicAction extends OpenGraphAction {
        Boolean getResult();

        void setResult(Boolean result);

        TruthValueGraphObject getTruthvalue();

        void setTruthvalue(TruthValueGraphObject truthvalue);

        TruthValueGraphObject getAnothertruthvalue();

        void setAnothertruthvalue(TruthValueGraphObject anothertruthvalue);
    }

    /**
     * Used to consume published And and Or open graph actions.
     */
    private interface PublishedLogicAction extends OpenGraphAction {
        LogicAction getData();

        String getType();
    }

    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        Body getBody();

        String getId();

        interface Body extends GraphObject {
            Error getError();
        }

        interface Error extends GraphObject {
            String getMessage();
        }
    }
}
