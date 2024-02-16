package dslabs.clientserver;

import dslabs.framework.Timer;
import lombok.Data;

@Data
final class ClientTimer implements Timer {
  static final int CLIENT_RETRY_MILLIS = 100;

  // Your code here...
  private final int sequenceNum;
  private final Request request;

  public ClientTimer(int sequenceNum, Request request) {
    this.sequenceNum = sequenceNum;
    this.request = request;
  }
}
