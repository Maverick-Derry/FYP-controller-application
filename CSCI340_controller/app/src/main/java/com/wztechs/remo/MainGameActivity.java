package com.wztechs.remo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.csci340_controller.R;
import com.wztechs.remo.service.ActionType;
import com.wztechs.remo.service.connection.ConnectTask;
import com.wztechs.remo.service.connection.ConnectionListener;
import com.wztechs.remo.service.connection.Connector;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

// Second Activity - Memory Game
public class MainGameActivity extends AppCompatActivity implements ConnectionListener, ActionType {

    private GridLayout grid;
    private int lastFlippedCardIndex = -1;
    private boolean allowCardFlips = false;
    Timer timer = new Timer();

    // handler for updating UI
    private Handler handler = null;

    // List of available card resources (drawable ID, card number, number available)
    ArrayList<CardResource> cardResources = new ArrayList<>(4);

    // List of cards in the current game
    ArrayList<Card> cardsOnBoard = new ArrayList<>(8);
    int numberOfCards = 0; // Number of cards


    // data field copied from controller activity
    private final static int SHOW_DIALOG = 1;
    private final static int HIDE_LOADING = 2;

    private String errorMessage;
    private Connector conn;
    private AlertDialog authDialog;
    private Handler handler2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        // Get the IDs of the face-up card drawable resources
        setCardResources();

        setGameBoard(); // set the memory game game board

        allowCardFlips = true; // Allow player actions to flip cards

        // set up the handler for updating UI
        handler = new Handler();

        // set up data field related to sending number
        errorMessage = "";
        handler2 = constructMessageHandler();
        authDialog = constructAuthDialog();
        Intent intent = getIntent();
        String ip = intent.getStringExtra("ip");
        new ConnectTask(( conn = new Connector(ip, this))).execute(); // makes new connection
    }

    private void setGameBoard() {
        // Get the screen size available
        ViewGroup view = findViewById(R.id.main_container);

        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    initializeGrid(view.getWidth(), view.getHeight()); // Set card sizes based on available screen size
                }
            });
        }
    }

    /* Adds the appropriate number of card/image resources to cardResources list */
    private void setCardResources() {
        // Add all the face-up cards
        cardResources.add(new CardResource(0, R.drawable.card_13));
        cardResources.add(new CardResource(1, R.drawable.card_15));
        cardResources.add(new CardResource(2, R.drawable.card_9));
        cardResources.add(new CardResource(3, R.drawable.card_12));

    }

    /* Initializes card objects and their locations, adds card images face-down to the grid */
    private void initializeGrid(int viewWidth, int viewHeight) {

        int cardSide;
        int side;
        int padding;

        if (viewWidth < viewHeight) side = viewWidth;
        else side = viewHeight;

        padding = (int) Math.floor(side * 0.02);
        grid = findViewById(R.id.main_grid);

        // (2 columns 4 rows)
        cardSide = (int) Math.floor(side * 0.24);
        numberOfCards = 8;
        grid.setColumnCount(2);

        for (int i = 0; i < numberOfCards; i++) {
            int random = (int) (Math.random() * cardResources.size());
            CardResource cardResource = cardResources.get(random); // Get a random resource from the pool
            // Initialize new card; it is added to the grid through initialization
            Card card = new Card(cardResource.drawableId, cardResource.cardNumber, i, cardSide, padding);
            cardResource.numberAvailable--;
            if (cardResource.numberAvailable > 0) {
                cardResource.firstCardIndex = i;
            } else {    // Set the indices of the cards' matches
                int index = cardResource.firstCardIndex;
                cardsOnBoard.get(index).matchIndex = i;
                card.matchIndex = index;
                cardResources.remove(random);
            }
            cardsOnBoard.add(card);
        }
    }

    /* Data associated with a card resource: card number, drawable ID, number available, index of its match */
    private class CardResource {
        private int cardNumber;
        private int drawableId;
        private int numberAvailable;
        private int firstCardIndex;

        private CardResource(int cardNumber, int drawableId) {
            this.cardNumber = cardNumber;
            this.drawableId = drawableId;
            numberAvailable = 2;
        }
    }

    /* Data associated with each card on the grid */
    private class Card {
        private ImageButton button;
        private boolean matched;
        private int drawableId;
        private int cardNumber;
        private int layoutIndex;
        private int matchIndex;

        /* Sets Card fields and renders card image in GUI */
        private Card(int drawableId, int cardNumber, int layoutIndex, int sideLength, int paddingLength) {
            this.drawableId = drawableId;
            this.cardNumber = cardNumber;
            this.layoutIndex = layoutIndex;
            matched = false;

            button = new ImageButton(getApplicationContext());
            button.setImageResource(R.drawable.card_back);
            button.setAdjustViewBounds(true);
            button.setBackground(null);
            button.setPadding(paddingLength, paddingLength, paddingLength, paddingLength);
            button.setLayoutParams(new LinearLayout.LayoutParams(sideLength, sideLength));
            button.setOnClickListener(view -> flipCard(this));
            grid.addView(button);
        }
    }

    /* Actions to take when a card is flipped */
    private synchronized void flipCard(Card card) {
        if (allowCardFlips && !card.matched && lastFlippedCardIndex != card.layoutIndex) {
            allowCardFlips = false;
            card.button.setImageResource(card.drawableId); // Set card image to face-up in GUI
            if (lastFlippedCardIndex == -1) { // First card in pair flipped
                firstInPairFlipped(card);
            } else if (lastFlippedCardIndex == card.matchIndex) { // Matching pair found
                matchingCardFlipped(card);
            } else { // Non-matching pair
                notMatchingCardFlipped(card);
            }
        }
    }

    /* First card in pair flipped */
    private void firstInPairFlipped(Card card) {
        // Set timer for slight delay before allowing player to flip another (prevent simultaneous flips)
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                allowCardFlips = true;
            }
        }, 10);
        lastFlippedCardIndex = card.layoutIndex;
    }

    /* Second card flipped and a match was found */
    private void matchingCardFlipped(Card card) {
        timer.schedule(new TimerTask() { // First timer - delay removing card so player can see match, then display "matched" images
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                Card otherCard = cardsOnBoard.get(lastFlippedCardIndex);
                card.matched = true;
                otherCard.matched = true;
                card.button.setImageResource(R.drawable.card_match);
                otherCard.button.setImageResource(R.drawable.card_match);
                lastFlippedCardIndex = -1;
                timer.schedule(new TimerTask() { // Second timer - remove "matched" images and set empty slots
                    @Override
                    public void run() {
                        card.button.setImageResource(R.drawable.empty_card_slot);
                        otherCard.button.setImageResource(R.drawable.empty_card_slot);
                    }
                }, 200);

                //send number of the corresponding card to PC
                conn.sendAction(KEY_INSERT, String.valueOf(card.cardNumber));

                // use handler to update UI
                handler.postDelayed(updUI, 250);
            }
        }, 400);
    }

    /* Second card flipped and does not match first card */
    private void notMatchingCardFlipped(Card card) {
        // Set timer for delay to allow player to memorize card images
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Card otherCard = cardsOnBoard.get(lastFlippedCardIndex);
                card.button.setImageResource(R.drawable.card_back);
                otherCard.button.setImageResource(R.drawable.card_back);
                lastFlippedCardIndex = -1;
                allowCardFlips = true;
            }
        }, 500);
    }

    // runnable that update UI
    final Runnable updUI = new Runnable() {
        @Override
        public void run() {
            cardsOnBoard.clear();
            grid.removeViews(0,8);
            setCardResources();
            setGameBoard();
            allowCardFlips = true;
        }
    };

    // section related to connection
    @Override
    public void onStop() {
        super.onStop();
        conn.stop();
        conn = null;
        authDialog = null;
        handler2 = null;
    }

    @Override
    public void onConnMessageReceived(String message) {
        //removed
    }

    @Override
    public void onConnError(String type, Exception e) {
        errorMessage = type;
    }

    @Override
    public void onConnSuccess() {
        //hide progress bar
        Message message = handler2.obtainMessage(HIDE_LOADING);
        message.sendToTarget();

        //register event
    }

    @Override
    public void onConnClose() {
        backToMain();
    }

    @Override
    public void onConnStart() {
        //no implementation needed
    }

    @Override
    public void onConnAuthentication() {
        Message message = handler2.obtainMessage(SHOW_DIALOG, "");
        message.sendToTarget();
    }

    @Override
    public void onConnAuthenticationSuccess() {
        authDialog = null;
    }

    @Override
    public void onConnAuthenticationFailed() {
        Message message = handler2.obtainMessage(SHOW_DIALOG, "Invalid password, please retry!");
        message.sendToTarget();
    }

    //ui thread message handler
    private Handler constructMessageHandler(){
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch(message.what){
                    case SHOW_DIALOG:
                        authDialog.setMessage((String)message.obj);
                        authDialog.show();
                        break;
                    case HIDE_LOADING:
                        // crashed, comment this first
                        // findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        break;
                }
            }
        };
    }

    //authentication dialog
    private AlertDialog constructAuthDialog()
    {
        //construct container
        final LinearLayout dialogContainer = new LinearLayout(this);
        dialogContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50, 0, 50, 0);
        final EditText passwordField = new EditText(this);
        passwordField.setLayoutParams(lp);
        passwordField.setGravity(android.view.Gravity.TOP | android.view.Gravity.LEFT);
        passwordField.setLines(1);
        passwordField.setMaxLines(1);
        dialogContainer.addView(passwordField);

        return new AlertDialog.Builder(this)
                .setTitle("Password Required")
                .setView(dialogContainer)
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(conn.setEncryptionKey(passwordField.getText().toString())){
                            conn.testConnection();
                        }else{
                            Message message = handler2.obtainMessage(SHOW_DIALOG, "Invalid password, please retry!");
                            message.sendToTarget();
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        backToMain();
                    }
                }).create();
    }

    private void backToMain(){
        //set status to canceled and put error message
        if(!errorMessage.isEmpty() )
            setResult(RESULT_CANCELED,  new Intent().putExtra("error",errorMessage));

        finish();
    }

}