package ch.epfl.sweng.SDP.matchmaking;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.junit.Before;
import org.junit.Test;

import ch.epfl.sweng.SDP.auth.Account;
import ch.epfl.sweng.SDP.auth.ConstantsWrapper;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MatchmakerTest {

    private static final String USER_ID = "123456789";
    private static final String FAKE_USERNAME = "IAmNotFake";
    private static final String FAKE_ROOM = "Testroom";
    private static final String FAKE_LEAGUE = "league1";
    private ConstantsWrapper mockConstantsWrapper;
    private Account mockAccount;

    /**
     * Setup up all the mocks.
     */
    @Before
    public void init() {
        mockConstantsWrapper = mock(ConstantsWrapper.class);
        DatabaseReference mockReference = mock(DatabaseReference.class);
        Task mockTask = mock(Task.class);
        mockAccount = mock(Account.class);
        when(mockReference.child(isA(String.class))).thenReturn(mockReference);
        when(mockReference.removeValue()).thenReturn(mockTask);
        when(mockConstantsWrapper.getReference(isA(String.class))).thenReturn(mockReference);
        doNothing().when(mockReference)
                .addListenerForSingleValueEvent(isA(ValueEventListener.class));
    }

    @Test
    public void testJoinRoom() {
        when(mockConstantsWrapper.getFirebaseUserId()).thenReturn(USER_ID);
        when(mockAccount.getUserId()).thenReturn(USER_ID);
        when(mockAccount.getUsername()).thenReturn(FAKE_USERNAME);
        when(mockAccount.getCurrentLeague()).thenReturn(FAKE_LEAGUE);
        Task<String> task = Matchmaker.getInstance(mockAccount).joinRoom(0);
        assertNotNull(task);
    }

    @Test
    public void testLeaveRoom() {
        when(mockConstantsWrapper.getFirebaseUserId()).thenReturn(USER_ID);
        when(mockAccount.getUserId()).thenReturn(USER_ID);
        when(mockAccount.getUsername()).thenReturn(FAKE_USERNAME);
        when(mockAccount.getCurrentLeague()).thenReturn(FAKE_LEAGUE);
        Matchmaker.getInstance(mockAccount).leaveRoom(FAKE_ROOM);
    }
}