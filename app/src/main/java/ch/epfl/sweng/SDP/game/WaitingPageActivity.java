package ch.epfl.sweng.SDP.game;

import static java.lang.String.format;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import ch.epfl.sweng.SDP.BaseActivity;
import ch.epfl.sweng.SDP.R;
import ch.epfl.sweng.SDP.auth.Account;
import ch.epfl.sweng.SDP.firebase.Database;
import ch.epfl.sweng.SDP.game.drawing.DrawingOnline;
import ch.epfl.sweng.SDP.game.drawing.DrawingOnlineItems;
import ch.epfl.sweng.SDP.matchmaking.GameStates;
import ch.epfl.sweng.SDP.matchmaking.Matchmaker;
import ch.epfl.sweng.SDP.utils.LayoutUtils;
import ch.epfl.sweng.SDP.utils.network.NetworkStatusHandler;
import ch.epfl.sweng.SDP.utils.network.NetworkStateReceiver;
import ch.epfl.sweng.SDP.utils.network.NetworkStateReceiverListener;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Class representing the first phase of an online game: a waiting page in which players can vote
 * for the word to draw.
 */
public class WaitingPageActivity extends BaseActivity {

    private static final String TAG = "WaitingPageActivity";
    private static final String WORD_CHILDREN_DB_ID = "words";
    private static final String TOP_ROOM_NODE_ID = "realRooms";

    private NetworkStateReceiver networkStateReceiver;

    private static boolean enableSquareAnimation = true;

    private enum WordNumber {
        ONE, TWO
    }

    private String roomID = null;

    private int gameMode;

    private boolean isDrawingActivityLaunched = false;

    private boolean hasVoted = false;
    private boolean isWord1Voted = false;

    private DatabaseReference stateRef;
    private DatabaseReference timerRef;

    private DatabaseReference word1Ref;
    private int word1Votes = 0;

    private DatabaseReference word2Ref;
    private int word2Votes = 0;

    private String word1 = null;
    private String word2 = null;
    private String winningWord = null;

    @VisibleForTesting
    protected final ValueEventListener listenerTimer = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Integer value = dataSnapshot.getValue(Integer.class);

            if (value != null) {
                ((TextView) findViewById(R.id.waitingTime))
                        .setText(String.valueOf(value));
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    @VisibleForTesting
    protected final ValueEventListener listenerState = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Integer state = dataSnapshot.getValue(Integer.class);
            if (state != null) {
                GameStates stateEnum = GameStates.convertValueIntoState(state);
                switch (stateEnum) {
                    case HOMESTATE:
                        findViewById(R.id.waitingTime).setVisibility(View.GONE);
                        findViewById(R.id.leaveButton).setVisibility(View.VISIBLE);
                        break;
                    case CHOOSE_WORDS_TIMER_START:
                        findViewById(R.id.waitingTime).setVisibility(View.VISIBLE);
                        findViewById(R.id.leaveButton).setVisibility(View.GONE);

                        timerRef = Database.getReference(TOP_ROOM_NODE_ID + "."
                                + roomID + ".timer.observableTime");
                        timerRef.addValueEventListener(listenerTimer);
                        break;
                    case START_DRAWING_ACTIVITY:
                        if (timerRef != null) {
                            timerRef.removeEventListener(listenerTimer);
                        }
                        launchDrawingActivity(gameMode);
                        break;
                    default:
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    @VisibleForTesting
    protected final ValueEventListener listenerWord1 = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            try {
                Long value = dataSnapshot.getValue(Long.class);
                if (value != null) {
                    word1Votes = value.intValue();
                    winningWord = getWinningWord(word1Votes, word2Votes,
                            new String[]{word1, word2});
                }
            } catch (Exception e) {
                Log.e(TAG, "Value is not ready");
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    @VisibleForTesting
    protected final ValueEventListener listenerWord2 = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            try {
                Long value = dataSnapshot.getValue(Long.class);
                if (value != null) {
                    word2Votes = value.intValue();
                    winningWord = getWinningWord(word1Votes, word2Votes,
                            new String[]{word1, word2});
                }
            } catch (Exception e) {
                Log.e(TAG, "Value is not ready");
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };

    @VisibleForTesting
    protected final ValueEventListener listenerCountUsers = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            long usersCount = dataSnapshot.getChildrenCount();
            ((TextView) findViewById(R.id.playersCounterText)).setText(
                    format("%s/5", String.valueOf(usersCount)));

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            throw databaseError.toException();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_waiting_page);

        NetworkStateReceiverListener networkStateReceiverListener =
                new NetworkStatusHandler(this);
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(networkStateReceiverListener);
        registerReceiver(networkStateReceiver,
                new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        Intent intent = getIntent();
        roomID = intent.getStringExtra("roomID");
        word1 = intent.getStringExtra("word1");
        word2 = intent.getStringExtra("word2");
        gameMode = intent.getIntExtra("mode", 0);

        if (enableSquareAnimation) {
            Glide.with(this).load(R.drawable.waiting_animation_square)
                    .into((ImageView) findViewById(R.id.waitingAnimationSquare));
            Glide.with(this).load(R.drawable.background_animation)
                    .into((ImageView) findViewById(R.id.waitingBackgroundAnimation));
        }

        DatabaseReference wordsVotesRef = Database.getReference(
                TOP_ROOM_NODE_ID + "." + roomID + "." + WORD_CHILDREN_DB_ID);
        word1Ref = wordsVotesRef.child(word1);
        word2Ref = wordsVotesRef.child(word2);

        stateRef = Database.getReference(TOP_ROOM_NODE_ID + "." + roomID + ".state");
        stateRef.addValueEventListener(listenerState);

        DatabaseReference userRef = Database.getReference(TOP_ROOM_NODE_ID + "." + roomID + ".users." + Account.getInstance(this).getUserId());
        userRef.onDisconnect().removeValue();

        DatabaseReference usersCountRef = Database.getReference(TOP_ROOM_NODE_ID + "." +
                roomID + ".users");
        usersCountRef.addValueEventListener(listenerCountUsers);

        initRadioButton((Button) findViewById(R.id.buttonWord1), word1, word1Ref,
                WordNumber.ONE);
        initRadioButton((Button) findViewById(R.id.buttonWord2), word2, word2Ref,
                WordNumber.TWO);

        Typeface typeMuro = Typeface.createFromAsset(getAssets(), "fonts/Muro.otf");

        setTypeFace(typeMuro, findViewById(R.id.playersReadyText),
                findViewById(R.id.playersCounterText), findViewById(R.id.buttonWord1),
                findViewById(R.id.buttonWord2), findViewById(R.id.voteText),
                findViewById(R.id.waitingTime), findViewById(R.id.leaveButton));

        LayoutUtils.setFadingExitListener(findViewById(R.id.leaveButton), this);

        findViewById(R.id.waitingTime).setVisibility(View.GONE);
    }

    private void launchDrawingActivity(int gameMode) {
        Intent intent = new Intent(getApplicationContext(),
                gameMode == 0 ? DrawingOnline.class : DrawingOnlineItems.class);

        isDrawingActivityLaunched = true;

        intent.putExtra("RoomID", roomID);
        intent.putExtra("WinningWord", winningWord);
        startActivity(intent);
    }

    private void initRadioButton(Button button, String childString,
                                 DatabaseReference dbRef, WordNumber wordNumber) {
        dbRef.addValueEventListener(
                wordNumber == WordNumber.ONE ? listenerWord1 : listenerWord2);

        // Display the word on the button
        button.setText(childString);
    }

    /**
     * Gets the words that received the larger amount of votes.
     *
     * @param word1Votes Votes for the word 1
     * @param word2Votes Votes for the word 2
     * @param words      Array containing the words
     * @return Returns the winning word.
     */
    public static String getWinningWord(int word1Votes, int word2Votes, String[] words) {
        String winningWord = words[1];
        if (word1Votes >= word2Votes) {
            winningWord = words[0];
        }
        return winningWord;
    }

    /**
     * Callback function called when a radio button is pressed. Updates the votes in the database.
     *
     * @param view View corresponding to the button clicked
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.buttonWord1:
                if (checked) {
                    // Vote for word1
                    hasVoted = true;
                    isWord1Voted = true;
                    voteForWord(WordNumber.ONE);
                    disableButtons();
                }
                break;
            case R.id.buttonWord2:
                if (checked) {
                    // Vote for word2
                    hasVoted = true;
                    isWord1Voted = false;
                    voteForWord(WordNumber.TWO);
                    disableButtons();
                }
                break;
            default:
        }
    }

    // Vote for the specified word and update the database
    private void voteForWord(WordNumber wordNumber) {
        switch (wordNumber) {
            case ONE:
                word1Ref.setValue(++word1Votes);
                ((ImageView) findViewById(R.id.imageWord1))
                        .setImageResource(R.drawable.word_image_picked);
                break;
            case TWO:
                word2Ref.setValue(++word2Votes);
                ((ImageView) findViewById(R.id.imageWord2))
                        .setImageResource(R.drawable.word_image_picked);
                break;
            default:
        }
        animateWord1();
        animateWord2();
    }

    private void animateWord1() {
        final Animation pickWord1 = AnimationUtils.loadAnimation(this, R.anim.pick_word_1);
        pickWord1.setFillAfter(true);
        findViewById(R.id.imageWord1).startAnimation(pickWord1);
    }

    private void animateWord2() {
        final Animation pickWord2 = AnimationUtils.loadAnimation(this, R.anim.pick_word_2);
        pickWord2.setFillAfter(true);
        findViewById(R.id.imageWord2).startAnimation(pickWord2);
    }

    private void disableButtons() {
        Button b1 = findViewById(R.id.buttonWord1);
        b1.setEnabled(false);
        Button b2 = findViewById(R.id.buttonWord2);
        b2.setEnabled(false);
    }

    private void removeVote(final DatabaseReference wordRef) {
        wordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer value = dataSnapshot.getValue(Integer.class);
                if (value != null) {
                    wordRef.setValue(--value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }

    /**
     * Getter of the number of votes for word 1.
     *
     * @return the number of votes for word 1
     */
    @VisibleForTesting
    public int getWord1Votes() {
        return word1Votes;
    }

    @VisibleForTesting
    public void setWord1Votes(int votes) {
        word1Votes = votes;
    }

    /**
     * Getter of the number of votes for word 2.
     *
     * @return the number of votes for word 2
     */
    @VisibleForTesting
    public int getWord2Votes() {
        return word2Votes;
    }

    @VisibleForTesting
    public void setWord2Votes(int votes) {
        word2Votes = votes;
    }

    private void removeAllListeners() {
        try {
            timerRef.removeEventListener(listenerTimer);
        } catch (NullPointerException e) {
            Log.e(TAG, "Timer listener not initialized");
        }
        stateRef.removeEventListener(listenerState);
        word1Ref.removeEventListener(listenerWord1);
        word2Ref.removeEventListener(listenerWord2);
    }

    @VisibleForTesting
    public static void disableAnimations() {
        enableSquareAnimation = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkStateReceiver);

        // Does not leave the room if the activity is stopped because
        // drawing activity is launched.
        if (!isDrawingActivityLaunched && NetworkStateReceiver.isOnline(this)) {
            Matchmaker.getInstance(Account.getInstance(this)).leaveRoom(roomID);
            if (hasVoted) {
                String wordVoted = isWord1Voted ? word1 : word2;
                DatabaseReference wordRef = Database.getReference(TOP_ROOM_NODE_ID + "."
                        + roomID + ".words." + wordVoted);
                removeVote(wordRef);
            }
        }

        removeAllListeners();
        finish();
    }

    /**
     * Method that calls onDataChange on the UI thread.
     *
     * @param dataSnapshot Snapshot of the database (mock snapshot in this case).
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void callOnDataChange(final DataSnapshot dataSnapshot) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listenerTimer.onDataChange(dataSnapshot);
            }
        });
    }

}
