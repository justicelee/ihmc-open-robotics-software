package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC hole-body inverse kinematics module.
       * It contains auxiliary information that allows to further customized the behavior of the solver.
       */
public class KinematicsToolboxConfigurationMessage extends Packet<KinematicsToolboxConfigurationMessage> implements Settable<KinematicsToolboxConfigurationMessage>, EpsilonComparable<KinematicsToolboxConfigurationMessage>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.euclid.tuple3D.Point3D privileged_root_joint_position_;
   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.euclid.tuple4D.Quaternion privileged_root_joint_orientation_;
   /**
            * This array is used to identify to which joint each angle in privileged_joint_angles belongs to.
            * See Joint.hashCode() for the computation of a joint hash code.
            */
   public us.ihmc.idl.IDLSequence.Integer  privileged_joint_hash_codes_;
   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.idl.IDLSequence.Float  privileged_joint_angles_;
   /**
            * The weight to use in the optimization for the privileged configuration.
            * When remaining close to the privileged configuration is important, raise this weight to a value higher than the
            * weight of the main objectives.
            * Any value less than zero will be ignored.
            */
   public double privileged_weight_ = -1.0;
   /**
            * The feedback proportional gain to use for the privileged configuration.
            * It is coupled to some extent to the privileged_weight
            */
   public double privileged_gain_ = -1.0;

   public KinematicsToolboxConfigurationMessage()
   {
      privileged_root_joint_position_ = new us.ihmc.euclid.tuple3D.Point3D();
      privileged_root_joint_orientation_ = new us.ihmc.euclid.tuple4D.Quaternion();
      privileged_joint_hash_codes_ = new us.ihmc.idl.IDLSequence.Integer (100, "type_2");

      privileged_joint_angles_ = new us.ihmc.idl.IDLSequence.Float (100, "type_5");

   }

   public KinematicsToolboxConfigurationMessage(KinematicsToolboxConfigurationMessage other)
   {
      this();
      set(other);
   }

   public void set(KinematicsToolboxConfigurationMessage other)
   {
      sequence_id_ = other.sequence_id_;

      geometry_msgs.msg.dds.PointPubSubType.staticCopy(other.privileged_root_joint_position_, privileged_root_joint_position_);
      geometry_msgs.msg.dds.QuaternionPubSubType.staticCopy(other.privileged_root_joint_orientation_, privileged_root_joint_orientation_);
      privileged_joint_hash_codes_.set(other.privileged_joint_hash_codes_);
      privileged_joint_angles_.set(other.privileged_joint_angles_);
      privileged_weight_ = other.privileged_weight_;

      privileged_gain_ = other.privileged_gain_;

   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }


   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.euclid.tuple3D.Point3D getPrivilegedRootJointPosition()
   {
      return privileged_root_joint_position_;
   }


   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.euclid.tuple4D.Quaternion getPrivilegedRootJointOrientation()
   {
      return privileged_root_joint_orientation_;
   }


   /**
            * This array is used to identify to which joint each angle in privileged_joint_angles belongs to.
            * See Joint.hashCode() for the computation of a joint hash code.
            */
   public us.ihmc.idl.IDLSequence.Integer  getPrivilegedJointHashCodes()
   {
      return privileged_joint_hash_codes_;
   }


   /**
            * When provided, the solver will attempt to find the solution that is the closest to the privileged configuration.
            */
   public us.ihmc.idl.IDLSequence.Float  getPrivilegedJointAngles()
   {
      return privileged_joint_angles_;
   }

   /**
            * The weight to use in the optimization for the privileged configuration.
            * When remaining close to the privileged configuration is important, raise this weight to a value higher than the
            * weight of the main objectives.
            * Any value less than zero will be ignored.
            */
   public void setPrivilegedWeight(double privileged_weight)
   {
      privileged_weight_ = privileged_weight;
   }
   /**
            * The weight to use in the optimization for the privileged configuration.
            * When remaining close to the privileged configuration is important, raise this weight to a value higher than the
            * weight of the main objectives.
            * Any value less than zero will be ignored.
            */
   public double getPrivilegedWeight()
   {
      return privileged_weight_;
   }

   /**
            * The feedback proportional gain to use for the privileged configuration.
            * It is coupled to some extent to the privileged_weight
            */
   public void setPrivilegedGain(double privileged_gain)
   {
      privileged_gain_ = privileged_gain;
   }
   /**
            * The feedback proportional gain to use for the privileged configuration.
            * It is coupled to some extent to the privileged_weight
            */
   public double getPrivilegedGain()
   {
      return privileged_gain_;
   }


   public static Supplier<KinematicsToolboxConfigurationMessagePubSubType> getPubSubType()
   {
      return KinematicsToolboxConfigurationMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return KinematicsToolboxConfigurationMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(KinematicsToolboxConfigurationMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!this.privileged_root_joint_position_.epsilonEquals(other.privileged_root_joint_position_, epsilon)) return false;
      if (!this.privileged_root_joint_orientation_.epsilonEquals(other.privileged_root_joint_orientation_, epsilon)) return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsIntegerSequence(this.privileged_joint_hash_codes_, other.privileged_joint_hash_codes_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsFloatSequence(this.privileged_joint_angles_, other.privileged_joint_angles_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.privileged_weight_, other.privileged_weight_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.privileged_gain_, other.privileged_gain_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof KinematicsToolboxConfigurationMessage)) return false;

      KinematicsToolboxConfigurationMessage otherMyClass = (KinematicsToolboxConfigurationMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if (!this.privileged_root_joint_position_.equals(otherMyClass.privileged_root_joint_position_)) return false;
      if (!this.privileged_root_joint_orientation_.equals(otherMyClass.privileged_root_joint_orientation_)) return false;
      if (!this.privileged_joint_hash_codes_.equals(otherMyClass.privileged_joint_hash_codes_)) return false;
      if (!this.privileged_joint_angles_.equals(otherMyClass.privileged_joint_angles_)) return false;
      if(this.privileged_weight_ != otherMyClass.privileged_weight_) return false;

      if(this.privileged_gain_ != otherMyClass.privileged_gain_) return false;


      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("KinematicsToolboxConfigurationMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("privileged_root_joint_position=");
      builder.append(this.privileged_root_joint_position_);      builder.append(", ");
      builder.append("privileged_root_joint_orientation=");
      builder.append(this.privileged_root_joint_orientation_);      builder.append(", ");
      builder.append("privileged_joint_hash_codes=");
      builder.append(this.privileged_joint_hash_codes_);      builder.append(", ");
      builder.append("privileged_joint_angles=");
      builder.append(this.privileged_joint_angles_);      builder.append(", ");
      builder.append("privileged_weight=");
      builder.append(this.privileged_weight_);      builder.append(", ");
      builder.append("privileged_gain=");
      builder.append(this.privileged_gain_);
      builder.append("}");
      return builder.toString();
   }
}
