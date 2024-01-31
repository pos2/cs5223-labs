package dslabs.clientserver;

import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Command;
import dslabs.framework.Message;
import dslabs.framework.Result;
import lombok.Data;

// @Data
// class Request implements Message {
//  private final Command command;
//  private final int sequenceNum;
// }
//
// @Data
// class Reply implements Message {
//  private final Result result;
//  private final int sequenceNum;
// }

@Data
class Request_1 implements Message {
  private final Command command;
  private final int sequenceNum;
}

@Data
class Reply_1 implements Message {
  private final Result result;
  private final int sequenceNum;
}

@Data
class Request implements Message {
  private final AMOCommand command;
}

@Data
class Reply implements Message {
  private final AMOResult result;
}
