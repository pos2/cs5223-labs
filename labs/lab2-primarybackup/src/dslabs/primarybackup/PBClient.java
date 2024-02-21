package dslabs.primarybackup;

import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Client;
import dslabs.framework.Command;
import dslabs.framework.Node;
import dslabs.framework.Result;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import static dslabs.primarybackup.ClientTimer.CLIENT_RETRY_MILLIS;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class PBClient extends Node implements Client {
    private final Address viewServer;

    // Your code here...
    private Address server;
    private int sequenceNum = 0;
    private int viewNum = 0;
    private final HashMap<Integer, Integer> req_rcv = new HashMap<>();
    private AMOResult reply;
    private boolean needNewView = false;
    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PBClient(Address address, Address viewServer) {
        super(address);
        this.viewServer = viewServer;
    }

    @Override
    public synchronized void init() {
        // Your code here...
        askForNewView();
    }

    /* -------------------------------------------------------------------------
        Client Methods
       -----------------------------------------------------------------------*/
    @Override
    public synchronized void sendCommand(Command command) {
        // Your code here...
        sequenceNum += 1;
        req_rcv.put(sequenceNum, 0);
        Request request = new Request(new AMOCommand(sequenceNum, address(), command));
        send(request, server);
        set(new ClientTimer(ClientTimerType.Server, sequenceNum, request), CLIENT_RETRY_MILLIS);
    }

    @Override
    public synchronized boolean hasResult() {
        // Your code here...
        return req_rcv.get(sequenceNum) == 1;
    }

    @Override
    public synchronized Result getResult() throws InterruptedException {
        // Your code here...
        while (req_rcv.get(sequenceNum) != 1) {
            wait();
        }
        return reply.result();
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private synchronized void handleReply(Reply m, Address sender) {
        // Your code here...

        if ((sender != server) || (!m.isPrimary())) {
//            this.server = sender;
            this.needNewView = true;
            askForNewView();
            return;
        }
        if (m.err()) {
            return;
        }
        if (req_rcv.get(sequenceNum) == 1) {
            return;
        }
        if (m.result().sequenceNum() == sequenceNum) {
            reply = m.result();
            req_rcv.put(sequenceNum, 1);
            notify();
        }
    }

    private synchronized void handleViewReply(ViewReply m, Address sender) {
        // Your code here...
        View v = m.view();
        if (v.viewNum() > this.viewNum) {
            this.viewNum = v.viewNum();
            this.server = v.primary();
            this.needNewView = false;
        } else if (v.viewNum() == this.viewNum) {
            this.needNewView = false;
        }
    }

    // Your code here...
    private void askForNewView() {
        this.needNewView = true;
        send(new GetView(), viewServer);
        set(new ClientTimer(ClientTimerType.ViewServer, 0, null), CLIENT_RETRY_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private synchronized void onClientTimer(ClientTimer t) {
        // Your code here...
        switch (t.tp()) {
            case Server:
                // server may be dead, need new view
                if (req_rcv.get(t.seqNumber()) == 0) {
                    send(new GetView(), viewServer);
//                    System.out.printf("%d %s->%s\n", t.seqNumber(), t.req().command(), server.toString());
                    send(t.req(), server);
                    set(t, CLIENT_RETRY_MILLIS);
                }
                break;
            case ViewServer:
                // keep trying
                if (this.needNewView) {
                    send(new GetView(), viewServer);
                    set(t, CLIENT_RETRY_MILLIS);
                }
                break;
        }
    }
}
