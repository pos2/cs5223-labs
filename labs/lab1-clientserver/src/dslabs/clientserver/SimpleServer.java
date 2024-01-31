package dslabs.clientserver;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Node;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Simple server that receives requests and returns responses.
 *
 * <p>See the documentation of {@link Node} for important implementation notes.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class SimpleServer extends Node {
  // Your code here...
  //  private int sequenceNum;
  private final AMOApplication app;

  //  private final ConcurrentHashMap<String, Result> addr_result = new ConcurrentHashMap<>();
  //  private final ConcurrentHashMap<String, Integer> addr_seq = new ConcurrentHashMap<>();
  /* -----------------------------------------------------------------------------------------------
   *  Construction and Initialization
   * ---------------------------------------------------------------------------------------------*/
  public SimpleServer(Address address, Application app) {
    super(address);
    //    this.app = app;
    this.app = new AMOApplication(app);
    //    this.sequenceNum = -1;
    // Your code here...
  }

  @Override
  public void init() {
    // No initialization necessary
  }

  /* -----------------------------------------------------------------------------------------------
   *  Message Handlers
   * ---------------------------------------------------------------------------------------------*/

  private void handleRequest(Request m, Address sender) {
    //    Result result = app.execute(m.command());
    AMOResult result = app.execute(m.command());
    //    send(new Reply(result, m.sequenceNum()), sender);
    send(new Reply(result), sender);
  }
  //  private void handleRequest_1(Request_1 m, Address sender) {
  //    // Your code here...
  //    Result result;
  //    String addr = sender.toString();
  ////    String key = String.format("%s#%d", sender.toString(), m.sequenceNum());
  //    if (addr_seq.containsKey(addr)) {
  //      int seq = addr_seq.get(addr);
  //      if (seq > m.sequenceNum()) {
  //        return;
  //      } else if (seq == m.sequenceNum()) {
  //        result = addr_result.get(addr);
  //      } else {
  //        result = app.execute(m.command());
  //        addr_seq.put(addr, m.sequenceNum());
  //        addr_result.put(addr, result);
  //      }
  //    } else {
  //      result = app.execute(m.command());
  //      addr_seq.put(addr, m.sequenceNum());
  //      addr_result.put(addr, result);
  //    }
  ////    if (addr_result.containsKey(key)) {
  ////      result = addr_result.get(key);
  ////    } else {
  ////      result = app.execute(m.command());
  ////      addr_result.put(key, result);
  ////    }
  //    Reply_1 reply = new Reply_1(result, m.sequenceNum());
  //    send(reply, sender);
  //  }
}
