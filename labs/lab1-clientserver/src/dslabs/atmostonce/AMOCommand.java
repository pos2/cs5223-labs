package dslabs.atmostonce;

import dslabs.framework.Address;
import dslabs.framework.Command;
import lombok.Data;

@Data
public final class AMOCommand implements Command {
  // Your code here...
  private final int sequenceNum;
  private final Address addr;
  private final Command command;

  @Override
  public String toString() {
    return "AMOCommand{seqNum=" + sequenceNum + " addr=" + addr.toString() + " command=" + command().toString() + "}";
  }
}
