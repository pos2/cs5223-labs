package dslabs.clientserver;

import static dslabs.clientserver.ClientTimer.CLIENT_RETRY_MILLIS;

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

/**
 * Simple client that sends requests to a single server and returns responses.
 *
 * <p>See the documentation of {@link Client} and {@link Node} for important implementation notes.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class SimpleClient extends Node implements Client {
  private final Address serverAddress;

  // Your code here...
  private int sequenceNum = 0;
  private AMOResult reply;
  //  private Reply_1 reply_1;
  private final HashMap<Integer, Integer> req_rcv = new HashMap<>();

  /* -----------------------------------------------------------------------------------------------
   *  Construction and Initialization
   * ---------------------------------------------------------------------------------------------*/
  public SimpleClient(Address address, Address serverAddress) {
    super(address);
    this.serverAddress = serverAddress;
  }

  @Override
  public synchronized void init() {
    // No initialization necessary
  }

  /* -----------------------------------------------------------------------------------------------
   *  Client Methods
   * ---------------------------------------------------------------------------------------------*/
  //  @Override
  //  public synchronized void sendCommand_1(Command command) {
  //    // Your code here...
  //    sequenceNum += 1;
  //    req_rcv.put(sequenceNum, 0);
  //    Request_1 request_1 = new Request_1(command, sequenceNum);
  ////    Request request = new Request((AMOCommand) command);
  //    send(request_1, serverAddress);
  //    set(new ClientTimer(sequenceNum, request_1), CLIENT_RETRY_MILLIS);
  //  }

  @Override
  public synchronized void sendCommand(Command command) {
    // Your code here...
    sequenceNum += 1;
    req_rcv.put(sequenceNum, 0);
    //    Request request = new Request(command, sequenceNum);
    Request request = new Request(new AMOCommand(sequenceNum, address(), command));
    send(request, serverAddress);
    set(new ClientTimer(sequenceNum, request), CLIENT_RETRY_MILLIS);
  }

  @Override
  public synchronized boolean hasResult() {
    // Your code here...
    return req_rcv.get(sequenceNum) == 1;
    //    return ((this.reply != null) && ( this.sequenceNum == this.reply.sequenceNum()));
  }

  @Override
  public synchronized Result getResult() throws InterruptedException {
    // Your code here...
    //    while ((this.reply == null) || (reply.sequenceNum() != this.sequenceNum)) {
    //      wait();
    //    }
    while (req_rcv.get(sequenceNum) != 1) {
      wait();
    }
    return reply.result();
  }

  /* -----------------------------------------------------------------------------------------------
   *  Message Handlers
   * ---------------------------------------------------------------------------------------------*/

  private synchronized void handleReply(Reply m, Address sender) {
    // Your code here...
    if ((sender == serverAddress) && (m != null)) {
      if (req_rcv.get(sequenceNum) == 1) {
        return;
      }
      if (m.result().sequenceNum() == sequenceNum) {
        reply = m.result();
        req_rcv.put(sequenceNum, 1);
        notify();
      }
    }
  }

  //  private synchronized void handleReply_1(Reply_1 m, Address sender) {
  //    // Your code here...
  //    if ((sender == serverAddress) && (m != null) && (m.sequenceNum() == sequenceNum)) {
  //      if (req_rcv.get(sequenceNum) == 1) {
  //        return;
  //      }
  //      reply_1 = m;
  //      req_rcv.put(sequenceNum, 1);
  //      notify();
  //    }
  //  }

  /* -----------------------------------------------------------------------------------------------
   *  Timer Handlers
   * ---------------------------------------------------------------------------------------------*/
  private synchronized void onClientTimer(ClientTimer t) {
    // Your code here...
    //    if ((this.reply == null) || (this.reply.sequenceNum() != this.sequenceNum) ||
    // (req_rcv.get(t.sequenceNum()) != 1)) {
    //      send(t.request(), this.serverAddress);
    //      set(t, CLIENT_RETRY_MILLIS);
    //    }
    if (req_rcv.get(t.sequenceNum()) == 0) {
      send(t.request(), serverAddress);
      set(t, CLIENT_RETRY_MILLIS);
    }
  }
}
