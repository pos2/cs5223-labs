package dslabs.primarybackup;

import com.google.common.base.Objects;
import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Node;
import dslabs.framework.Result;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static dslabs.primarybackup.PingTimer.PING_MILLIS;
import static dslabs.primarybackup.BackupTimer.BACKUP_RETRY_MILLIS;
import static dslabs.primarybackup.BackupWholeAppTimer.BACK_WHOLE_RETRY_MILLIS;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class PBServer extends Node {
    private final Address viewServer;

    // Your code here...
    private Role role;
    private AMOApplication app;
    private final int STARTUP_VIEWNUM = 0;
    private int view_num;
    private View current_view = null;
    private int backupRcv;
    private Address newBackup;
    private final List<AMOCommand> cmd_list = new ArrayList<>();
//    private final HashMap<Integer, Integer> cmd_ack = new HashMap<>();
    private boolean able2Execute;
    private boolean viewChange;
    private AMOCommand lastCmd;
    /* -----------------------------------------------------------------------------------------------
     *  Construction and Initialization
     * ---------------------------------------------------------------------------------------------*/
    PBServer(Address address, Address viewServer, Application app) {
        super(address);
        this.viewServer = viewServer;

        // Your code here...
        this.app = new AMOApplication(app);
    }

    @Override
    public void init() {
        // Your code here...
        role = Role.IDLE;
        able2Execute = false;
        backupRcv = 0;
        newBackup = null;
        send(new Ping(STARTUP_VIEWNUM), viewServer);
        set(new PingTimer(), PING_MILLIS);
    }

    /* -----------------------------------------------------------------------------------------------
     *  Message Handlers
     * ---------------------------------------------------------------------------------------------*/
    private void handleRequest(Request m, Address sender) {
        // Your code here...
        switch (role) {
            case PRIMARY -> {
                if (!able2Execute) {
                    Reply rpl = new Reply(null, true, true);
                    send(rpl, sender);
                } else {
                    cmd_list.add(m.command());
//                    cmd_ack.put(cmd_list.size(), 0);
                    if (current_view.backup() != null) {
                        // transfer to backup, don't reply to client directly
                        BackupRequest br = new BackupRequest(m.command(), sender, cmd_list.size(), false, true, 0);
                        send(br, current_view.backup());
                        set(new BackupTimer(br), BACKUP_RETRY_MILLIS);
                        able2Execute = false;
                        System.out.printf("%s -%d> %s %s line76\n", address(), cmd_list.size(), current_view.backup(), m.command());
                    } else {
                        // no backup, execute and reply
                        AMOResult result = this.app.execute(m.command());
                        Reply rpl = new Reply(result, true, false);
                        send(rpl, sender);
                        System.out.printf("%s -%d> %s %s line82\n", address(), cmd_list.size(), sender, m.command());
                    }
                }
            }
            case BACKUP, IDLE -> {
                Reply rpl = new Reply(null, false, true);
                send(rpl, sender);
            }
        }
    }

//    private void handleRequest(Request m, Address sender) {
//        switch (role) {
//            case PRIMARY -> {
//                if (!able2Execute) {
//                    Reply rpl = new Reply(null, true, true);
//                    send(rpl, sender);
//                } else {
//                    if (current_view.backup() == null) {
//                        AMOResult result = this.app.execute(m.command());
//                        Reply rpl = new Reply(result, true, false);
//                        send(rpl, sender);
//                    } else {
//                        //                        cmd_list.add(m.command());
//                        lastCmd = m.command();
//                        BackupRequest br = new BackupRequest(m.command(), sender, cmd_list.size(), false, true, view_num);
//                        send(br, current_view.backup());
//                        set(new BackupTimer(br), BACKUP_RETRY_MILLIS);
//                    }
//                }
//            }
//            case BACKUP, IDLE -> {
//                Reply rpl = new Reply(null, false, true);
//                send(rpl, sender);
//            }
//        }
//    }

    private void handleViewReply(ViewReply m, Address sender) {
        // Your code here...
        View v = m.view();
        if (!(v.viewNum() > view_num)) {
            // don't need to change
            return;
        }
        viewChange = true;
        System.out.printf("%s : %s\n", address(), v);
        switch (role) {
            case PRIMARY -> {
                // stay primary
                if (Objects.equal(address(), v.primary())) {
                    role = Role.PRIMARY;
                    if (v.backup() == null) {
                        // reset states
                        resetStates();
                        able2Execute = true;
                    } else if (!Objects.equal(v.backup(), current_view.backup())) {
                        // backup changed, need to transfer
                        newBackup = v.backup();
                        setupBackup();
                    }
                } else if (Objects.equal(address(), v.backup())){
//                    if (!able2Execute) {
//                        return;
//                    }
                    role = Role.BACKUP;
                    resetStates();
                } else {
                    role = Role.IDLE;
                    resetStates();
                }
            }
            case BACKUP -> {
                // promoted to be primary
                if (Objects.equal(address(), v.primary())) {
                    if (!able2Execute) {
                        return;
                    }
                    role = Role.PRIMARY;
                    if (v.backup() != null) {
                        // found backup, need to transfer
                        newBackup = v.backup();
                        setupBackup();
                    } else {
                        // reset states
                        resetStates();
                        able2Execute = true;
                    }
                } else if (Objects.equal(address(), v.backup())){
                    role = Role.BACKUP;
//                    cmd_list.clear();
                    resetStates();
                } else {
                    role = Role.IDLE;
                    resetStates();
                }
            }
            case IDLE -> {
                // the first ping, became primary
                if (Objects.equal(address(), v.primary())) {
                    role = Role.PRIMARY;
                    if (v.backup() != null) {
                        // found backup, need to transfer
                        newBackup = v.backup();
                        setupBackup();
                    } else {
                        // reset states
                        resetStates();
                        able2Execute = true;
                    }
                } else if (Objects.equal(address(), v.backup())) {
                    role = Role.BACKUP;
//                    cmd_list.clear();
                    resetStates();
                } else {
                    role = Role.IDLE;
                    resetStates();
                }
            }
        }
        viewChange = false;
        current_view = v;
        view_num = v.viewNum();
        send(new Ping(view_num), viewServer);
    }

//    private void handleViewReply(ViewReply m, Address sender) {
//        View v = m.view();
//        if (v.viewNum() <= view_num) {
//            return;
//        }
//        switch (role) {
//            case PRIMARY -> {
//                if (Objects.equal(address(), v.primary())) {
//                    role = Role.PRIMARY;
//                    Address bk = v.backup();
//                    if (bk != null) {
//                        if (!Objects.equal(bk, current_view.backup())) {
//                            able2Execute = false;
//                            view_num = v.viewNum();
//                            current_view = v;
//                            backupWholeApp(current_view.backup());
//                        }
//                    } else {
//                        able2Execute = true;
//                        view_num = v.viewNum();
//                        current_view = v;
//                    }
//                } else if (Objects.equal(address(), v.backup())){
//                    role = Role.BACKUP;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                } else {
//                    role = Role.IDLE;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                }
//            }
//            case BACKUP -> {
//                if (Objects.equal(address(), v.primary())) {
//                    role = Role.PRIMARY;
//                    Address bk = v.backup();
//                    if (bk == null) {
//                        able2Execute = true;
//                        view_num = v.viewNum();
//                        current_view = v;
//                    } else {
//                        able2Execute = false;
//                        view_num = v.viewNum();
//                        current_view = v;
//                        backupWholeApp(current_view.backup());
//                    }
//                } else if (Objects.equal(address(), v.backup())) {
//                    role = Role.BACKUP;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                } else {
//                    role = Role.IDLE;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                }
//            }
//            case IDLE -> {
//                if (Objects.equal(address(), v.primary())) {
//                    role = Role.PRIMARY;
//                    Address bk = v.backup();
//                    if (bk == null) {
//                        able2Execute = true;
//                        view_num = v.viewNum();
//                        current_view = v;
//                    } else {
//                        view_num = v.viewNum();
//                        current_view = v;
//                        backupWholeApp(current_view.backup());
//                    }
//                } else if (Objects.equal(address(), v.backup())) {
//                    role = Role.BACKUP;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                } else {
//                    role = Role.IDLE;
//                    able2Execute = false;
//                    view_num = v.viewNum();
//                    current_view = v;
//                }
//            }
//        }
//        send(new Ping(view_num), viewServer);
//    }

//     Your code here...

    private synchronized void handleBackupRequest(BackupRequest r, Address sender) {
        if ((role != Role.BACKUP) || (!Objects.equal(sender, current_view.primary()))) {
            BackupReply br = new BackupReply(false, -1, 0, null, false, 0, true);
            send(br, sender);
            System.out.printf("%s from %s to %s %s\n", r, sender, address(), current_view);
            System.out.printf("%s <%d- %s %s %s %s line179\n", address(), -1, sender, r.command(), current_view, role);
        } else {
            if (r.currentNum() > cmd_list.size() + 1) {
                BackupReply br = new BackupReply(false, -1, 0, null, false, 0, true);
                send(br, sender);
                System.out.printf("%s <%d- %s %s line185\n", address(), -1, sender, r.command());
                return;
            }
            if (r.currentNum() <= cmd_list.size()) {
                // retry by primary, don't need to execute
                BackupReply br = new BackupReply(true, r.currentNum(), r.command().sequenceNum(), r.client(), r.past(), cmd_list.size(), false);
                send(br, sender);
                System.out.printf("%s <%d- %s %s line192\n", address(), r.currentNum(), sender, r.command());
            } else {
                // new for backup
                cmd_list.add(r.command());
                app.execute(r.command());
                BackupReply br = new BackupReply(true, r.currentNum(), r.command().sequenceNum(), r.client(), r.past(), cmd_list.size(), false);
                send(br, sender);
                System.out.printf("%s <%d- %s %s line196\n",address(), r.currentNum(), sender, r.command());
            }
            if (r.last()) {
                able2Execute = true;
            }
        }
    }

    private synchronized void handleBackupReply(BackupReply r, Address sender) {
        if ((role == Role.PRIMARY) &&
                (Objects.equal(sender, newBackup)) &&
                (r.done())) {
            if (r.currentNum()-1 == backupRcv) {
                if (r.rcv() > backupRcv) {
                    backupRcv = r.rcv();
                } else {
                    backupRcv += 1;
                }
//                backupRcv += 1;
                if (r.past()) {
                    // no need to reply to client
                    // if there is commands never been transferred, continue
                    if (backupRcv < cmd_list.size()) {
                        if (viewChange) {
                            resetStates();
                            return;
                        }
                        backupNext();
                    } else {
                        able2Execute = true;
                    }
                } else {
                    // reply to client
                    //                cmd_ack.put(r.currentNum(), 1);
                    AMOResult result = app.execute(cmd_list.get(r.currentNum()-1));
                    Reply rpl = new Reply(result, true, false);
                    // TODO here client can be null, what's going on
                    send(rpl, r.client());
                    System.out.printf("%s <%d- %s %s line244\n",address(), r.currentNum(), sender, cmd_list.get(r.currentNum()-1));
                    able2Execute = true;
                }
            }

        } else {
            resetStates();
        }
    }

//    private synchronized void handleBackupRequest(BackupRequest br, Address sender) {
//        if ((br.viewNum() > view_num) ||
//                (!Objects.equal(sender, current_view.primary())) ||
//                (role != Role.BACKUP)) {
//            BackupReply reply = new BackupReply(false, view_num,0,br.client(), false, 0, true);
//            send(reply, sender);
//            send(new Ping(view_num), viewServer);
//        } else {
//            app.execute(br.command());
//            BackupReply reply = new BackupReply(true, view_num, 0, br.client(), false, 0, false);
//            send(reply, sender);
//        }
//    }
//
//    private synchronized void handleBackupReply(BackupReply br, Address sender) {
//        if (br.currentNum() < view_num) {
//
//        } else if (br.currentNum() == view_num){
//            if (br.done() && lastCmd != null) {
//                AMOResult result = app.execute(lastCmd);
//                Reply reply = new Reply(result, true, false);
//                send(reply, br.client());
//            }
//        } else {
//            send(new Ping(view_num), viewServer);
//        }
//    }

    private synchronized void handleBackupWholeAppRequest(BackupWholeAppRequest bkw, Address sender) {
        if (view_num > bkw.viewNumber()) {
            BackupWholeAppReply reply = new BackupWholeAppReply(view_num, false);
            send(reply, sender);
            return;
        }
        if ((role != Role.BACKUP) ||
                !(Objects.equal(sender,current_view.primary())) ||
                (view_num != bkw.viewNumber())) {
            send(new Ping(view_num), viewServer);
            BackupWholeAppReply reply = new BackupWholeAppReply(view_num, false);
            send(reply, sender);
        } else {
            this.app = bkw.app();
            BackupWholeAppReply reply = new BackupWholeAppReply(view_num, true);
            send(reply, sender);
        }
    }

    private synchronized void handleBackupWholeAppReply(BackupWholeAppReply bkw, Address sender) {
        if (role != Role.PRIMARY) {
            send(new Ping(view_num), viewServer);
            return;
        }
        if ((bkw.viewNumber() == view_num) && (Objects.equal(sender, current_view.backup()))) {
            if (bkw.done()) {
                able2Execute = true;
            }
        } else if ((bkw.viewNumber() < view_num) && (Objects.equal(sender, current_view.backup()))) {
            BackupWholeAppRequest req = new BackupWholeAppRequest(this.app, view_num);
            send(req, sender);
            set(new BackupWholeAppTimer(req, sender), BACK_WHOLE_RETRY_MILLIS);
        } else {
            send(new Ping(view_num), viewServer);
        }
    }

    /* -----------------------------------------------------------------------------------------------
     *  Timer Handlers
     * ---------------------------------------------------------------------------------------------*/
    private void onPingTimer(PingTimer t) {
        // Your code here...
        send(new Ping(view_num), viewServer);
        set(t, PING_MILLIS);
    }

    private void onBackupTimer(BackupTimer t) {
        if (role != Role.PRIMARY) {
            return;
        }
        if (backupRcv < t.br().currentNum()) {
            if (!viewChange) {
                send(t.br(), newBackup);
                set(t, BACKUP_RETRY_MILLIS);
            }
        }
    }

//    private void onBackupTimer(BackupTimer t) {
//        if (role != Role.PRIMARY) {
//            return;
//        }
//        if (current_view.backup() == null) {
//            if (lastCmd != null) {
//                AMOResult result = app.execute(lastCmd);
//                lastCmd = null;
//                send(new Reply(result, true, false), t.br().client());
//                able2Execute = true;
//            }
//        } else {
//            send(t.br(), current_view.backup());
//            set(new BackupTimer(t.br()), BACKUP_RETRY_MILLIS);
//        }
//    }

    private void onBackupWholeAppTimer(BackupWholeAppTimer t) {
        if ((role != Role.PRIMARY) ||
                (current_view.backup() == null) ||
                (Objects.equal(t.to(), current_view.backup()))) {
            return;
        }
        if (!able2Execute) {
            send(t.bkw(), t.to());
            set(t, BACK_WHOLE_RETRY_MILLIS);
        }
    }

    // Your code here...

    /* -----------------------------------------------------------------------------------------------
     *  Utils
     * ---------------------------------------------------------------------------------------------*/
    // Your code here...

    private synchronized void resetStates() {
        able2Execute = false;
        backupRcv = 0;
    }


    private void setupBackup() {
        resetStates();
        backupNext();
    }

//    private synchronized void backupWholeApp(Address to) {
//        if ((role == Role.PRIMARY) && (to == null)) {
//            able2Execute = true;
//            return;
//        }
//        if (role != Role.PRIMARY){
//            return;
//        }
//        BackupWholeAppRequest bkw = new BackupWholeAppRequest(this.app, view_num);
//        send(bkw, to);
//        set(new BackupWholeAppTimer(bkw, to), BACK_WHOLE_RETRY_MILLIS);
//    }

    private synchronized void backupNext() {
        if (backupRcv == cmd_list.size()) {
            able2Execute = true;
            return;
        }
        int num = backupRcv + 1;
        AMOCommand cmd = cmd_list.get(num-1);
        BackupRequest br;
        if (num == cmd_list.size()) {
            br = new BackupRequest(cmd, null, num, true, true, 0);
        } else {
            br = new BackupRequest(cmd, null, num, true, false, 0);
        }
        send(br, newBackup);
        set(new BackupTimer(br), BACKUP_RETRY_MILLIS);
        System.out.printf("%s -%d> %s %s line295\n",address(), num, newBackup, br.command());
    }

//    @Override
//    protected void send(Message msg, Address to) {
//        System.out.printf("from %s to %s\n", address().toString(), to.toString());
//        super.send(msg, to);
//    }

}

enum Role {
    PRIMARY,
    BACKUP,
    IDLE
}