package dslabs.kvstore;

import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class KVStore implements Application {

  public interface KVStoreCommand extends Command {}

  public interface SingleKeyCommand extends KVStoreCommand {
    String key();
  }

  @Data
  public static final class Get implements SingleKeyCommand {
    @NonNull private final String key;

    @Override
    public boolean readOnly() {
      return true;
    }
  }

  @Data
  public static final class Put implements SingleKeyCommand {
    @NonNull private final String key, value;
  }

  @Data
  public static final class Append implements SingleKeyCommand {
    @NonNull private final String key, value;
  }

  public interface KVStoreResult extends Result {}

  @Data
  public static final class GetResult implements KVStoreResult {
    @NonNull private final String value;
  }

  @Data
  public static final class KeyNotFound implements KVStoreResult {}

  @Data
  public static final class PutOk implements KVStoreResult {}

  @Data
  public static final class AppendResult implements KVStoreResult {
    @NonNull private final String value;
  }

  // Your code here...

  //  private final HashMap<String,String> data = new HashMap<>();
  private final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

  @Override
  public synchronized KVStoreResult execute(Command command) {
    if (command instanceof Get) {
      Get g = (Get) command;
      if (data.containsKey(g.key)) {
        return new GetResult(data.get(g.key));
      }
      return new KeyNotFound();
    }

    if (command instanceof Put) {
      Put p = (Put) command;
      data.put(p.key, p.value);
      return new PutOk();
    }

    if (command instanceof Append) {
      Append a = (Append) command;
      if ((data.containsKey(a.key)) && (data.get(a.key) != null)) {
        data.put(a.key, data.get(a.key) + a.value);
        return new AppendResult(data.get(a.key));
      }
      data.put(a.key, a.value);
      return new AppendResult(data.get(a.key));
    }
    throw new IllegalArgumentException();
  }
}
