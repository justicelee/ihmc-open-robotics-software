package ihmc_msgs;

public interface HighLevelStatePacketMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/HighLevelStatePacketMessage";
  static final java.lang.String _DEFINITION = "## HighLevelStatePacketMessage\n# This message is used to switch the control scheme between force and position control.\n# WARNING: When in position control, the IHMC balance algorithms will be disabled and\n# it is up to the user to ensure stability.\n\n# Options for highLevelState\n# WALKING = 0 - whole body force control employing IHMC walking, balance, and manipulation algorithms\n# JOINT_POSITION_CONTROL = 1 - joint control. NOTE: controller will not attempt to keep the robot balanced\nuint8 high_level_state\n\n\n";
  byte getHighLevelState();
  void setHighLevelState(byte value);
}
