package com.vokal.afergulator;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.*;
import com.google.android.gms.games.multiplayer.realtime.*;
import com.vokal.afergulator.fragments.InvitationDialog;
import com.vokal.afergulator.tools.Log;
import com.vokal.afergulator.tools.Play;

public class MainActivity extends Play.BaseGameActivity implements ActionBar.OnNavigationListener {

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS   = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM     = 10002;

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean audioEnabled = true;

    private GameView   mGameView;
    private RomAdapter romAdapter;
    private ProgressBar mSpinner;

    boolean mMultiplayer = false;

    private PlayHandler    mPlayHandler   = new PlayHandler();
    private InviteHandler  mInviteHandler = new InviteHandler();
    private RoomHandler    mRoomHandler   = new RoomHandler();
    private MessageHandler mMessenger     = new MessageHandler();


    // flag indicating whether we're dismissing the waiting room because the game is starting
    boolean mWaitRoomDismissedFromCode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.main);

        getActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.frameLayout).setOnClickListener(toggleActionBar);

        romAdapter = new RomAdapter();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(romAdapter, this);

        mGameView = (GameView) findViewById(R.id.gameView);
        mSpinner = (ProgressBar) findViewById(R.id.progress_spinner);

        initGameServices();

    }

    @Override
    protected void onResume() {
        super.onResume();

        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        mGameView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_nes, menu);
        updateAudio(menu.findItem(R.id.menu_nes_audio));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int icon = isSignedIn() ? R.drawable.common_signin_btn_icon_dark : R.drawable.common_signin_btn_icon_light;
        menu.findItem(R.id.menu_nes_google).setIcon(icon);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nes_google:
                mPlayHandler.googleButtonClicked();
                return true;
            case R.id.menu_nes_load:
                Engine.loadState();
                return true;
            case R.id.menu_nes_save:
                Engine.saveState();
                return true;
            case R.id.menu_nes_audio:
                audioEnabled = !audioEnabled;
                Engine.enableAudio(audioEnabled);
                updateAudio(item);
                return true;
            case R.id.menu_nes_shutdown:
                Toast.makeText(this, "power down not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAudio(MenuItem menuItem) {
        if (audioEnabled) {
            menuItem.setIcon(R.drawable.ic_action_device_access_volume_on);
            menuItem.setTitle("Mute");
        } else {
            menuItem.setIcon(R.drawable.ic_action_device_access_volume_muted);
            menuItem.setTitle("Muted");
        }
    }

    private View.OnClickListener toggleActionBar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getActionBar().isShowing()) {
                getActionBar().hide();
                mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                mGameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                getActionBar().show();
            }
        }
    };

    private class RomAdapter extends ArrayAdapter<String> {

        public static final String SELECT_ROM = "Select ROM...";

        public RomAdapter() {
            super(MainActivity.this, R.layout.rom_spinner_item);
            try {
                String[] roms = getRoms();
                add(SELECT_ROM);
                addAll(roms);
            } catch (IOException e) {
                e.printStackTrace();
                add("NO ROMS FOUND!");
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getDropDownView(position, convertView, parent);

            if (position == getActionBar().getSelectedNavigationIndex()) {
                v.setTextColor(Integer.MAX_VALUE);
            } else {
                v.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            }

            v.setText(displayRomName(v.getText().toString()));

            return v;
        }

        private String[] getRoms() throws IOException {
            return getAssets().list("roms");
        }
    }

    private String displayRomName(String rom) {
        if (rom.endsWith(".nes")) {
            return rom.substring(0, rom.length() - 4);
        }
        return rom;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition == 0)
            return false;

        Engine.pauseEmulator();

        String rom = romAdapter.getItem(itemPosition);

        InputStream is = null;
        try {
            is = getAssets().open("roms/" + rom);
            if (mGameView.loadGame(is, rom)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public void onSignInSucceeded() {
        Log.i(this, "Game Sign-in Succeeded");
        supportInvalidateOptionsMenu();

        getGamesClient().registerInvitationListener(mInviteHandler);

        // if we received an invite via notification, accept it; otherwise, go to main screen
        if (getInvitationId() != null) {
            mInviteHandler.acceptInviteToRoom(getInvitationId());
        }

        startActivity(getGamesClient().getSettingsIntent());
    }

    @Override
    public void onSignInFailed() {
        Log.w(this, "Game Sign-in Failed!");
        supportInvalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                mRoomHandler.handleSelectPlayersResult(responseCode, data);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                mInviteHandler.handleInvitationInboxResult(responseCode, data);
                break;
            case RC_WAITING_ROOM:
                // ignore result if we dismissed the waiting room from code:
                if (mWaitRoomDismissedFromCode)
                    break;

                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // player wants to start playing
                    Log.d(TAG, "Starting game because user requested via waiting room UI.");

                    // start the game!
                    mMessenger.broadcastStart();
                    mMessenger.startGame(true);
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player actively indicated that they want to leave the room
                    mRoomHandler.leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    /* Dialog was cancelled (user pressed back key, for
                     * instance). In our game, this means leaving the room too. In more
                     * elaborate games,this could mean something else (like minimizing the
                     * waiting room UI but continue in the handshake process). */
                    mRoomHandler.leaveRoom();
                }

                break;
        }
    }

    private class PlayHandler extends DialogFragment {

        private final String[] START_CHOICES = {"See Invitations", "Invite Player"};

        public PlayHandler() {}

        void googleButtonClicked() {
            if (!isSignedIn()) {
                beginUserInitiatedSignIn();
            } else {
                show(getSupportFragmentManager(), PlayHandler.class.getCanonicalName());
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
            bldr.setTitle("Multiplayer:");
            if (!mMultiplayer) {
                bldr.setItems(START_CHOICES, startListener);
            } else {
                bldr.setMessage("Quit Multiplayer?");
                bldr.setPositiveButton("QUIT", quitListener);
                bldr.setNegativeButton("KEEP PLAYING", null);
            }
            return bldr.create();
        }

        private DialogInterface.OnClickListener startListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mInviteHandler.seeInvitations();
                        return;
                    case 1:
                        mInviteHandler.invitePlayers();
                        return;
                    default:
                        dismiss();
                }
            }
        };


        private DialogInterface.OnClickListener quitListener  = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRoomHandler.leaveRoom();
            }
        };

    }

    private class InviteHandler implements OnInvitationReceivedListener,
            InvitationDialog.Callbacks, View.OnClickListener {

        // If non-null, this is the id of the invitation we received via the invitation listener
        String mIncomingInvitationId = null;
        private InvitationDialog mInviteDialog;

        @Override
        public void onClick(View v) {
            invitePlayers();
        }

        public void invitePlayers() {
            // show list of invitable players
            Intent intent = getGamesClient().getSelectPlayersIntent(1, 1);
            startActivityForResult(intent, RC_SELECT_PLAYERS);
        }

        public void seeInvitations() {
            // show list of pending invitations
            Intent intent = getGamesClient().getInvitationInboxIntent();
            startActivityForResult(intent, RC_INVITATION_INBOX);
        }

        // Handle the result of the invitation inbox UI, where the player can pick an invitation
        // to accept. We react by accepting the selected invitation, if any.
        private void handleInvitationInboxResult(int response, Intent data) {
            if (response != Activity.RESULT_OK) {
                Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
                mSpinner.setVisibility(View.GONE);
                return;
            }

            Log.d(TAG, "Invitation inbox UI succeeded.");
            Invitation inv = data.getExtras().getParcelable(GamesClient.EXTRA_INVITATION);

            // accept invitation
            acceptInviteToRoom(inv.getInvitationId());
        }

        @Override
        public void onInvitationReceived(Invitation invitation) {
            mIncomingInvitationId = invitation.getInvitationId();
            mInviteDialog = InvitationDialog.newInstance(invitation).setCallbacks(this);
            mInviteDialog.show(getSupportFragmentManager());
        }

        @Override
        public void onInviteAccepted(Invitation aInvite) {
            acceptInviteToRoom(aInvite.getInvitationId());
        }

        @Override
        public void onInviteDeclined(Invitation aInvite) {
            mSpinner.setVisibility(View.GONE);
        }

        void acceptInviteToRoom(String invId) {
            // accept the invitation
            Log.d(TAG, "Accepting invitation: " + invId);
            RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(mRoomHandler);
            roomConfigBuilder.setInvitationIdToAccept(invId)
                    .setMessageReceivedListener(mMessenger)
                    .setRoomStatusUpdateListener(mRoomHandler);
            mSpinner.setVisibility(View.VISIBLE);
            keepScreenOn();
            getGamesClient().joinRoom(roomConfigBuilder.build());
        }

        void startQuickGame() {
            // quick-start a game with 1 randomly selected opponent
            final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
            Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                                                                          MAX_OPPONENTS, 0);
            RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(mRoomHandler);
            rtmConfigBuilder.setMessageReceivedListener(mMessenger);
            rtmConfigBuilder.setRoomStatusUpdateListener(mRoomHandler);
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            mSpinner.setVisibility(View.VISIBLE);
            keepScreenOn();
            getGamesClient().createRoom(rtmConfigBuilder.build());
        }
    }

    private class RoomHandler implements RoomUpdateListener, RoomStatusUpdateListener {

        String                 my_id        = null;
        String                 room_id      = null;
        ArrayList<Participant> otherPlayers = null;

        // Handle the result of the "Select players UI" we launched when the user clicked the
        // "Invite friends" button. We react by creating a room with those players.
        private void handleSelectPlayersResult(int response, Intent data) {
            if (response != Activity.RESULT_OK) {
                Log.w(TAG, "*** select players UI cancelled, " + response);
                mSpinner.setVisibility(View.GONE);
                return;
            }

            Log.d(TAG, "Select players UI succeeded.");

            // get the invitee list
            final ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
            Log.d(TAG, "Invitee count: " + invitees.size());

            // get the automatch criteria
            Bundle autoMatchCriteria = null;
            int minAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
            if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
            }

            // create the room
            Log.d(TAG, "Creating room...");
            RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
            rtmConfigBuilder.addPlayersToInvite(invitees);
            rtmConfigBuilder.setMessageReceivedListener(mMessenger);
            rtmConfigBuilder.setRoomStatusUpdateListener(this);
            if (autoMatchCriteria != null) {
                rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            }
            mSpinner.setVisibility(View.VISIBLE);
            keepScreenOn();
            getGamesClient().createRoom(rtmConfigBuilder.build());
            Log.d(TAG, "Room created, waiting for it to be ready...");
        }

        // Show the waiting room UI to track the progress of other players as they enter the
        // room and get connected.
        void showWaitingRoom(Room room) {
            mWaitRoomDismissedFromCode = false;

            // minimum number of players required for our game
            final int MIN_PLAYERS = 2;
            Intent i = getGamesClient().getRealTimeWaitingRoomIntent(room, MIN_PLAYERS);

            // show waiting room UI
            startActivityForResult(i, RC_WAITING_ROOM);
        }

        // Forcibly dismiss the waiting room UI (this is useful, for example, if we realize the
        // game needs to start because someone else is starting to play).
        void dismissWaitingRoom() {
            mWaitRoomDismissedFromCode = true;
            finishActivity(RC_WAITING_ROOM);
        }

        @Override
        public void onRoomCreated(int statusCode, Room room) {
            Log.d(this, "onRoomCreated(%d, %s)", statusCode, room);
            if (statusCode != GamesClient.STATUS_OK) {
                Log.e(this, "*** Error: onRoomCreated, status " + statusCode);
                showGameError("Could create game room!");
                return;
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        @Override
        public void onJoinedRoom(int statusCode, Room room) {
            Log.d(this, "onJoinedRoom(%d, %s)", statusCode, room);
            if (statusCode != GamesClient.STATUS_OK) {
                Log.e(this, "*** Error: onRoomConnected, status " + statusCode);
                showGameError("Couldn't join game room.");
                return;
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Leave the room.
        void leaveRoom() {
            Log.d(TAG, "Leaving room.");
            stopKeepingScreenOn();
            if (room_id != null) {
                getGamesClient().leaveRoom(this, room_id);
                room_id = null;
            }
            mSpinner.setVisibility(View.GONE);
        }

        @Override
        public void onLeftRoom(int statusCode, String roomId) {
            // Called when we've successfully left the room (this happens a result of voluntarily leaving
            // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).

            // we have left the room; return to main screen.
            Log.d(this, "onLeftRoom, code " + statusCode);
            mSpinner.setVisibility(View.GONE);
        }

        @Override
        public void onRoomConnected(int statusCode, Room room) {
            Log.d(this, "onRoomConnected(%d, %s)", statusCode, room);
            if (statusCode != GamesClient.STATUS_OK) {
                Log.e(this, "*** Error: onRoomConnected, status " + statusCode);
                showGameError("Couldn't connect to game room.");
                return;
            }
            updateRoom(room);
        }

        // Called when we are connected to the room. We're not ready to play yet!
        // (maybe not everybody is connected yet).
        @Override
        public void onConnectedToRoom(Room room) {
            Log.d(TAG, "onConnectedToRoom.");

            // get room ID, participants and my ID:
            updateRoom(room);

            // print out the list of participants (for debug purposes)
            Log.d(TAG, "Room ID: " + room_id);
            Log.d(TAG, "My ID " + my_id);
            Log.d(TAG, "<< CONNECTED TO ROOM>>");
        }

        @Override
        public void onDisconnectedFromRoom(Room room) {
            room_id = null;
            otherPlayers.clear();
            showGameError("Disconnected from room!");
        }

        @Override
        public void onRoomConnecting(Room room) {
            updateRoom(room);
        }

        @Override
        public void onRoomAutoMatching(Room room) {
            updateRoom(room);
        }

        @Override
        public void onPeerInvitedToRoom(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onPeerDeclined(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onPeerJoined(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onPeerLeft(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onPeersConnected(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onPeersDisconnected(Room room, List<String> strings) {
            updateRoom(room);
        }

        @Override
        public void onP2PConnected(String s) {
            Log.i(this, "P2P Connected: %s", s);
        }

        @Override
        public void onP2PDisconnected(String s) {
            Log.i(this, "P2P Disconnected: %s", s);
        }

        void updateRoom(Room aRoom) {
            room_id = aRoom.getRoomId();
            otherPlayers = aRoom.getParticipants();
            for (Participant p : otherPlayers) {
                if (my_id != null && my_id.equals(p.getParticipantId())) {
                    otherPlayers.remove(p);
                    break;
                }
            }

            my_id = aRoom.getParticipantId(getGamesClient().getCurrentPlayerId());
        }

        // Show error message about game being cancelled and return to main screen.
        void showGameError(String msg) {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        }

    }

    private class MessageHandler implements RealTimeMessageReceivedListener, Engine.KeyListener {

        // Message buffer for sending messages
        byte[] mMsgBuf = {0x00, 0x00, 0x00, 0x00};

        // Start the gameplay phase of the game.
        void startGame(boolean multiplayer) {
            mMultiplayer = multiplayer;
            mSpinner.setVisibility(View.GONE);

            if (mMultiplayer) {
                Engine.setKeyListener(this);
            } else {
                Engine.setKeyListener(null);
            }

            Log.i(this, "start game: multiplayer = %b", mMultiplayer);

            // LOAD ROM

        }

        private void broadcastStart() {
            mMsgBuf[0] = 'S';
            mMsgBuf[1] = (byte) 0;
            sendBufferToAll();
        }

        @Override
        public void onRealTimeMessageReceived(RealTimeMessage rtm) {
            byte[] buf = rtm.getMessageData();
            String msg = String.format("%s-%d", (char) buf[0], (int) buf[1]);
            Log.d(TAG, "received: %s", msg);

            if (mRoomHandler.my_id != null && mRoomHandler.my_id.equals(rtm.getSenderParticipantId())) {
                Log.w(this, "received my own message! %s", msg);
                return;
            }

            byte cmd = buf[0];
            byte key = buf[1];

            switch (cmd) {
                case 'D':
                    Engine.buttonDown(key, 1);
                    Log.i(this, "player 2 button down: " + key);
                    break;

                case 'U':
                    Engine.buttonUp(key, 1);
                    Log.i(this, "player 2 button up: " + key);
                    break;

                case 'S':
                    mRoomHandler.dismissWaitingRoom();
                    startGame(true);
                    break;
            }
        }

        @Override
        public void onKeyEvent(int key, int down) {
            mMsgBuf[0] = (byte) (down == 1 ? 'D' : 'U');
            mMsgBuf[1] = (byte) key;
            sendBufferToAll();
        }

        private void sendBufferToAll() {
            for (Participant p : mRoomHandler.otherPlayers) {
                if (p.getStatus() != Participant.STATUS_JOINED) continue;
                getGamesClient().sendReliableRealTimeMessage(null, mMsgBuf, mRoomHandler.room_id, p.getParticipantId());
            }
        }

    }
}
