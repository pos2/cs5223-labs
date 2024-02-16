package dslabs.primarybackup;

import com.google.common.base.Objects;
import dslabs.framework.Address;
import dslabs.framework.Node;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static dslabs.primarybackup.PingCheckTimer.PING_CHECK_MILLIS;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class ViewServer extends Node {
    static final int STARTUP_VIEWNUM = 0;
    private static final int INITIAL_VIEWNUM = 1;

    // Your code here...
    private int view_num;
    private View current_view;
//    private View future_view;
    private Address PRIMARY, BACKUP, NEXT_PRI, NEXT_BACKUP;
    boolean needs_update = false;
    boolean primary_changed = false;
    private final HashMap<Address, Integer> ping_list = new HashMap<>();
    private final HashMap<Address, Integer> ack_list = new HashMap<>();
    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public ViewServer(Address address) {
        super(address);
    }

    @Override
    public void init() {
        set(new PingCheckTimer(), PING_CHECK_MILLIS);
        // Your code here...
        this.current_view = new View(STARTUP_VIEWNUM, null, null);
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePing(Ping m, Address sender) {
        // Your code here...
        if(PRIMARY == null) {
            // the first ping, make it primary
//            this.PRIMARY = sender;
//            this.view_num = INITIAL_VIEWNUM;
//            this.current_view = new View(this.view_num, this.PRIMARY, null);

            this.NEXT_PRI = sender;
            this.NEXT_BACKUP = null;
            needs_update = true;
            primary_changed = true;
            changeRightNowIfPossible();

//            this.ping_list.put(sender, 2);
        } else if ((BACKUP == null) && (!Objects.equal(sender, PRIMARY))) {
            // the second ping, make it backup

//            this.BACKUP = sender;
//            this.view_num += 1;
//            this.current_view = new View(this.view_num, this.PRIMARY, this.BACKUP);
            this.BACKUP = sender;
            this.NEXT_PRI = PRIMARY;
            this.NEXT_BACKUP = sender;
            needs_update = true;
            changeRightNowIfPossible();
        }
        if (ack_list.containsKey(sender) && (ack_list.get(sender) >= 0)) {
            if (m.viewNum() >= ack_list.get(sender)) {
                // if (ack_list.get(sender) > m.viewNum() {
//                send(new ViewReply(this.current_view), sender);
//                return;
                this.ping_list.put(sender, 2);
                this.ack_list.put(sender, m.viewNum());
            }
        } else {
            this.ping_list.put(sender, 2);
            this.ack_list.put(sender, m.viewNum());
        }
        if (Objects.equal(sender, NEXT_PRI)) {
            if (needs_update) {
                changeRightNowIfPossible();
            }
        }
//        if (sender == NEXT_PRI) {
//            System.out.println("if sender == NEXT_PRI executed");
//            if (needs_update) {
//                changeRightNowIfPossible();
//            }
//        }
        send(new ViewReply(this.current_view), sender);
    }

    private void handleGetView(GetView m, Address sender) {
        // Your code here...
        send(new ViewReply(this.current_view), sender);
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingCheckTimer(PingCheckTimer t) {
        // Your code here...
        for(Address addr: ping_list.keySet()) {
//            checkServerCounts(addr);
            int val = ping_list.get(addr);
            if (val > 0) {
                ping_list.put(addr, val-1);
            }
        }
        for(Address addr: ping_list.keySet()) {
//            System.out.printf("%s %d\n", addr.toString(), ping_list.get(addr));
            checkServerCounts(addr);
        }
        set(t, PING_CHECK_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // Your code here...

    private void changeRightNowIfPossible() {
//        if ((NEXT_PRI != null) && (ack_list.get(NEXT_PRI) == view_num)) {
//            change2NewView();
//        }
        if (!needs_update) {
            return;
        }
        if ((PRIMARY == null) || (ack_list.get(PRIMARY) == view_num)) {
            change2NewView();
        }
    }
    private void checkServerCounts(Address addr) {
        int val = ping_list.get(addr);
        if (val <= 0) {
//            ping_list.remove(addr);
//            ack_list.remove(addr);
//            ack_list.put(addr, -1);
            ping_list.put(addr, -1);
            if (Objects.equal(addr, PRIMARY)) {
                if (BACKUP != null && ping_list.get(BACKUP) > 0) {
                    this.NEXT_PRI = BACKUP;
                    this.NEXT_BACKUP = getIdleForBackup();
                    this.primary_changed = true;
                    this.needs_update = true;
                } else {
//                    return;
//                    ack_list.put(addr, -1);
                    this.NEXT_PRI = PRIMARY;
                    this.NEXT_BACKUP = null;
                    this.primary_changed = false;
                    this.needs_update = false;
                }
                changeRightNowIfPossible();
            } else if (Objects.equal(addr, BACKUP)) {
                ack_list.put(addr, -1);
                this.NEXT_PRI = PRIMARY;
                this.NEXT_BACKUP = getIdleForBackup();
                this.needs_update = true;
                changeRightNowIfPossible();
            } else {
                ack_list.put(addr, -1);
            }
        }
    }

    private void change2NewView() {
        if (this.primary_changed) {
            if (this.ack_list.containsKey(this.PRIMARY)) {
                this.ack_list.put(this.PRIMARY, -1);
            }
            this.primary_changed = false;
        }
        this.view_num += 1;
        this.PRIMARY = NEXT_PRI;
        this.BACKUP = NEXT_BACKUP;
        this.NEXT_PRI = null;
        this.NEXT_BACKUP = null;
        this.current_view = new View(this.view_num, this.PRIMARY, this.BACKUP);
        this.needs_update = false;
    }

    private Address getIdleForBackup() {
        for (Address addr : ping_list.keySet()) {
            if ((Objects.equal(addr, PRIMARY)) || (Objects.equal(addr, BACKUP))) {
                continue;
            }
            if (ping_list.get(addr) > 0) {
                return addr;
            }
        }
        return null;
    }
}
