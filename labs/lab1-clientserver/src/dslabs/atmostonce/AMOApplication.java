package dslabs.atmostonce;

import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public final class AMOApplication<T extends Application> implements Application {
  @Getter @NonNull private final T application;

  // Your code here...
  private final HashMap<String, AMOResult> addr_result = new HashMap<>();
  private final HashMap<String, Integer> addr_seq = new HashMap<>();

  @Override
  public AMOResult execute(Command command) {
    if (!(command instanceof AMOCommand)) {
      throw new IllegalArgumentException();
    }

    AMOCommand amoCommand = (AMOCommand) command;
    // Your code here...
    String addr = amoCommand.addr().toString();
    int seq = amoCommand.sequenceNum();
    if (alreadyExecuted(amoCommand)) {
      return addr_result.get(addr);
    }
    Result result = application.execute(amoCommand.command());
    AMOResult amoResult = new AMOResult(seq, result);
    addr_seq.put(addr, seq);
    addr_result.put(addr, amoResult);
    return amoResult;
  }

  public Result executeReadOnly(Command command) {
    if (!command.readOnly()) {
      throw new IllegalArgumentException();
    }

    if (command instanceof AMOCommand) {
      return execute(command);
    }

    return application.execute(command);
  }

  public boolean alreadyExecuted(AMOCommand amoCommand) {
    // Your code here...
    int seq = amoCommand.sequenceNum();
    String addr = amoCommand.addr().toString();
    return addr_seq.containsKey(addr) && (addr_seq.get(addr) >= seq);
  }
}
