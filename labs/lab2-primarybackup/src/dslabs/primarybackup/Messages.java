package dslabs.primarybackup;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Message;
import lombok.Data;

/* -------------------------------------------------------------------------
    ViewServer Messages
   -----------------------------------------------------------------------*/
@Data
class Ping implements Message {
    private final int viewNum;
}

@Data
class GetView implements Message {
}

@Data
class ViewReply implements Message {
    private final View view;
}

/* -------------------------------------------------------------------------
    Primary-Backup Messages
   -----------------------------------------------------------------------*/
@Data
class Request implements Message {
    // Your code here...
    private final AMOCommand command;

}

@Data
class Reply implements Message {
    // Your code here...
    private final AMOResult result;
    private final boolean isPrimary;
//    private final int rcvNum;
    private final boolean err;
}

// Your code here...
@Data
class BackupRequest implements Message {
    private final AMOCommand command;
    private final Address client;
    private final int currentNum;
    private final boolean past;
    private final boolean last;
    private final int viewNum;
}

@Data
class BackupReply implements Message {
    private final boolean done;
    private final int currentNum;
    private final int sequenceNum;
    private final Address client;
    private final boolean past;
    private final int rcv;
    private final boolean err;
}

@Data
class BackupWholeAppRequest implements Message {
    private final AMOApplication app;
    private final int viewNumber;
}

@Data
class BackupWholeAppReply implements Message {
    private final int viewNumber;
    private final boolean done;
}