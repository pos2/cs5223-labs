package dslabs.clientserver;

import dslabs.framework.Timer;
import lombok.Data;

@Data
final class ClientTimer implements Timer {
  static final int CLIENT_RETRY_MILLIS = 100;

  // Your code here...
  private final int sequenceNum;
  private final Request request;
  private final Request_1 request_1;

  public ClientTimer(int sequenceNum, Request request) {
    this.sequenceNum = sequenceNum;
    this.request = request;
    this.request_1 = null;
  }

  public ClientTimer(int sequenceNum, Request_1 request_1) {
    this.sequenceNum = sequenceNum;
    this.request_1 = request_1;
    this.request = null;
  }
}
