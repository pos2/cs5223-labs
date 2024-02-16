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
    private Address PRIMARY, BACKUP, NEXT_PRI, NEXT_BACKUP;
    boolean needs_update = false;
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
            makeNewView(sender, null, true);
        } else if ((BACKUP == null) && (!Objects.equal(sender, PRIMARY))) {
            // the second ping, make it backup
            makeNewView(PRIMARY, sender, true);
        }
        // ignore the old pings
        if (!(ack_list.containsKey(sender) &&
                (m.viewNum() < ack_list.get(sender)))) {
            this.ping_list.put(sender, 2);
            this.ack_list.put(sender, m.viewNum());
        }
        if (Objects.equal(sender, NEXT_PRI)) {
            if (needs_update) {
                changeRightNowIfPossible();
            }
        }
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
            int val = ping_list.get(addr);
            if (val > 0) {
                ping_list.put(addr, val-1);
            }
        }
        for(Address addr: ping_list.keySet()) {
            checkServerCounts(addr);
        }
        set(t, PING_CHECK_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // Your code here...

    private void changeRightNowIfPossible() {
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
            if (Objects.equal(addr, PRIMARY)) {
                // there is a backup to replace primary
                if (BACKUP != null && ping_list.get(BACKUP) > 0) {
                    makeNewView(BACKUP, getIdleForBackup(), true);
                } else {
                    // no backup, keep spinning
                    makeNewView(PRIMARY, null, false);
                }
            } else if (Objects.equal(addr, BACKUP)) {
                // new backup or nothing
                makeNewView(PRIMARY, getIdleForBackup(), true);
            }
        }
    }

    private void change2NewView() {
        this.view_num += 1;
        this.PRIMARY = NEXT_PRI;
        this.BACKUP = NEXT_BACKUP;
        this.NEXT_PRI = null;
        this.NEXT_BACKUP = null;
        this.current_view = new View(this.view_num, this.PRIMARY, this.BACKUP);
        this.needs_update = false;
    }

    private void makeNewView(Address next_pri, Address next_backup, boolean needs_update) {
        this.NEXT_PRI = next_pri;
        this.NEXT_BACKUP = next_backup;
        this.needs_update = needs_update;
        changeRightNowIfPossible();
    }

    private Address getIdleForBackup() {
        for (Address addr : ping_list.keySet()) {
            if ((Objects.equal(addr, PRIMARY)) || (Objects.equal(addr, BACKUP))) {
                continue;
            }
            // make sure an alive idle
            if (ping_list.get(addr) > 0) {
                return addr;
            }
        }
        return null;
    }
}
