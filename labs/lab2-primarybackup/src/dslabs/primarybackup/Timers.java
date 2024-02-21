package dslabs.primarybackup;

import dslabs.atmostonce.AMOApplication;
import dslabs.framework.Address;
import dslabs.framework.Timer;
import lombok.Data;

@Data
final class PingCheckTimer implements Timer {
    static final int PING_CHECK_MILLIS = 100;
}

@Data
final class PingTimer implements Timer {
    static final int PING_MILLIS = 25;
}

@Data
final class ClientTimer implements Timer {
    static final int CLIENT_RETRY_MILLIS = 100;

    // Your code here...
    private final ClientTimerType tp;
    private final int seqNumber;
    private final Request req;
}

// Your code here...
@Data
final class BackupTimer implements Timer {
    static final int BACKUP_RETRY_MILLIS = 25;
    private final BackupRequest br;
//    private final int currentNum;
}

@Data
final class BackupWholeAppTimer implements Timer {
    static final int BACK_WHOLE_RETRY_MILLIS = 25;
    private final BackupWholeAppRequest bkw;
    private final Address to;
}

enum ClientTimerType {
    ViewServer,
    Server
}