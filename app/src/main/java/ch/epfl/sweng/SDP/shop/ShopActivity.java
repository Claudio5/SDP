package ch.epfl.sweng.SDP.shop;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ch.epfl.sweng.SDP.home.HomeActivity;
import ch.epfl.sweng.SDP.Activity;
import ch.epfl.sweng.SDP.R;

class BooleanWrapper {
    private boolean b;
    BooleanWrapper(boolean b) {
        this.b = b;
    }
    protected void setBoolean(boolean newb) {
        b = newb;
    }
    protected boolean getBoolean() {
        return b;
    }
}

class IntegerWrapper {
    private int i;
    IntegerWrapper(int i) {
        this.i = i;
    }
    protected void setInt(int newi) {
        i = newi;
    }
    protected int getInt() {
        return i;
    }
}

public class ShopActivity extends Activity {
    //to be replaced with whatever we use to store all these refs
    final FirebaseDatabase db = FirebaseDatabase.getInstance("https://gyrodraw.firebaseio.com/");
    final DatabaseReference dbRef = db.getReference();
    final DatabaseReference usersRef = dbRef.child("users");
    final DatabaseReference shopColorsRef = dbRef.child("items").child("colors");
    final DatabaseReference userColorsRef = usersRef.child(FirebaseAuth.getInstance()
            .getCurrentUser().getUid()).child("items").child("colors");

    private final BooleanWrapper b = new BooleanWrapper(false);
    private final IntegerWrapper stars = new IntegerWrapper(0);
    private final IntegerWrapper price = new IntegerWrapper(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shop_activity);
        getColorsFromDatabase();
        setReturn();
        setRefresh();
    }

    private void getColorsFromDatabase() {
        shopColorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    LinearLayout linearLayout = findViewById(R.id.linearLayout);
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Button b = initializeButton(snapshot.getKey());
                        addOnClickListenerToButton(b);
                        linearLayout.addView(b);
                    }
                }
                else {
                    setShopMessage("Currently no purchasable items in shop.");
                    resetShopMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }

    private Button initializeButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    private void addOnClickListenerToButton(final Button b) {
        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                purchaseItem(b.getText().toString());
            }
        };
        b.setOnClickListener(onClickListener);
    }

    private void setReturn() {
        Button ret = findViewById(R.id.returnFromShop);
        ret.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                gotoHome();
            }
        });
    }

    private void setRefresh() {
        Button refresh = findViewById(R.id.refreshShop);
        refresh.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                refreshShop();
            }
        });
    }

    private void purchaseItem(String s) {
        alreadyOwned(s, b);
    }

    private void alreadyOwned(final String item, final BooleanWrapper b) throws DatabaseException {
        userColorsRef.orderByKey().equalTo(item).addListenerForSingleValueEvent(
                new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    setShopMessage("Item already owned.");
                    resetShopMessage();
                }
                else {
                    stars.setInt(-1);
                    price.setInt(-1);
                    getStars(stars);
                    getPrice(price, item);
                    new CountDownTimer(2500, 500) {
                        public void onTick(long millisUntilFinished) {
                            if(stars.getInt() > -1 && price.getInt() > -1) {
                                this.cancel();
                                updateUserIf(item);
                            }
                        }
                        public void onFinish() {
                            if(stars.getInt() < 0 || price.getInt() < 0) {
                                setShopMessage("Unable to read from database in time.");
                                resetShopMessage();
                            }
                            else {
                                updateUserIf(item);
                            }
                        }
                    }.start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }

    private void updateUserIf(String item) {
        if(sufficientCurrency(stars.getInt(), price.getInt())) {
            updateUser(item, stars.getInt() - price.getInt());
        }
    }

    private void getStars(final IntegerWrapper i) throws DatabaseException {
        usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("stars")
                .addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    i.setInt((int) Math.max(Math.min((long) dataSnapshot.getValue(),
                            Integer.MAX_VALUE), Integer.MIN_VALUE));
                }
                else {
                    i.setInt(-1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }

    private void getPrice(final IntegerWrapper i, final String item) throws IllegalArgumentException,
            DatabaseException {
        shopColorsRef.child(item).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    i.setInt((int) Math.max(Math.min((long) dataSnapshot.getValue(),
                            Integer.MAX_VALUE), Integer.MIN_VALUE));
                }
                else {
                    i.setInt(-1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }

    private boolean sufficientCurrency(int stars, int price) {
        System.out.println(stars + " " + price);
        boolean sufficient = stars > price;
        if (!sufficient) {
            setShopMessage("Not enough stars to purchase item.");
            resetShopMessage();
        }
        return sufficient;
    }

    private void updateUser(String item, int newStars) {
        usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("stars")
                .setValue(newStars, new DatabaseReference.CompletionListener() {

            @Override
            public void onComplete(@Nullable DatabaseError databaseError,
                                   @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    throw databaseError.toException();
                }
            }
        });
        userColorsRef.child(item).setValue(true, new DatabaseReference.CompletionListener() {

            @Override
            public void onComplete(@Nullable DatabaseError databaseError,
                                   @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    throw databaseError.toException();
                }
            }
        });
    }

    private void setShopMessage(String message) {
        TextView t = findViewById(R.id.shopMessages);
        t.setText(message);
    }

    private void resetShopMessage() {
        new CountDownTimer(5000, 5000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                setShopMessage("");
            }
        }.start();
    }

    private void gotoHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void refreshShop() {
        startActivity(getIntent());
        finish();
    }
}