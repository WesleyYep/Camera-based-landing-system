package org.mavlink;

import java.io.IOException;

import org.mavlink.messages.MAV_AUTOPILOT;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_COMPONENT;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_STATE;
import org.mavlink.messages.ja4rtor.msg_command_int;
import org.mavlink.messages.ja4rtor.msg_command_long;
import org.mavlink.messages.ja4rtor.msg_heartbeat;
import org.mavlink.messages.ja4rtor.msg_manual_control;
import org.mavlink.messages.ja4rtor.msg_rc_channels_override;
import org.mavlink.messages.ja4rtor.msg_request_data_stream;

public class Sender {
	private SerialPortCommunicator spc;
	private int sequence = 0;
	
	public Sender(SerialPortCommunicator spc){
		this.spc = spc;
	}

	public boolean send(int streamId) {
		msg_request_data_stream ds = new msg_request_data_stream(255, 1);
		ds.sequence = sequence++;
		ds.req_message_rate = 10;
		ds.target_system = 1;
		ds.target_component = 1;
		ds.req_stream_id = Math.abs(streamId); //try 2, 6, 10, 11, 12, 01, 03 (10 is orientation AHRS)
		if (streamId > 0) {
			ds.start_stop = 1;
		} else {
			ds.start_stop = 0;
		}
        byte[] result;
		try {
			result = ds.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean command() {
	    msg_rc_channels_override msg = new msg_rc_channels_override(255, 1);
		msg.target_system = 1;
		msg.target_component = (byte) MAV_COMPONENT.MAV_COMP_ID_ALL;
		msg.chan1_raw = 2000;
		msg.chan2_raw = 2000;
		msg.chan3_raw = 2000;
		msg.chan4_raw = 2000;
		msg.chan5_raw = 2000;
		msg.chan6_raw = 2000;
		msg.chan7_raw = 2000;
		msg.chan8_raw = 2000;
//		msg_manual_control msg = new msg_manual_control(255, 1);
////		if (cmd.equals("x")) {
//			msg.x = 500;
//			msg.y = 500;
////		} else {
////			msg.x = 500;
////			msg.y = 500;
////		}
//		msg.z = 500;
//		msg.r = 500;
//		msg.buttons = 1;
//		msg.target = 1;
		byte[] result;
		try {
			result = msg.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean arm(boolean arm) {
//		msg_command_int armMessage = new msg_command_int(1,0);
//		
//		armMessage.target_system = 1;
//		armMessage.target_component = 0;
//		armMessage.command = 400;
//		if (arm) {
//			armMessage.param1 = 1;
//		} else {
//			armMessage.param1 = 0;
//		}
		msg_command_long msg = new msg_command_long(255,1);
		msg.target_system = 1;
		msg.target_component = (byte) MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;

		msg.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
		msg.param1 = arm?1:0;
		msg.param2 = 0;
		msg.param3 = 0;
		msg.param4 = 0;
		msg.param5 = 0;
		msg.param6 = 0;
		msg.param7 = 0;
		msg.confirmation = 0;
		
        byte[] result;
		try {
			result = msg.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean heartbeat() {
		msg_heartbeat hb = new msg_heartbeat(255, 1);
	    hb.sequence = sequence++;
	    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_PX4;
	    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED;
	    hb.custom_mode = 0; //custom mode
	    hb.mavlink_version = 3;
	    hb.system_status = MAV_STATE.MAV_STATE_ACTIVE;
	    byte[] result;
		try {
			result = hb.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
    }
	
//	public boolean send2() {
//	msg_heartbeat hb = new msg_heartbeat(1, 1);
//    hb.sequence = sequence++;
//    hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_PX4;
//    hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED;
//    hb.custom_mode = 0; //custom mode
//    hb.mavlink_version = 3;
//    hb.system_status = MAV_STATE.MAV_STATE_POWEROFF;
//    byte[] result;
//	try {
//		result = hb.encode();
//        spc.writeData(result);
//        return true;
//	} catch (IOException e) {
//		e.printStackTrace();
//	}
//	return false;
//}
	
//	public boolean send3() {
//		msg_param_request_list pr = new msg_param_request_list(1, 1);
//		pr.sequence = sequence++;
//		pr.target_system = 1;
//		pr.target_component = 1;
//        byte[] result;
//		try {
//			result = pr.encode();
//	        spc.writeData(result);
//	        return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
//	
//	public boolean send4() {
//		msg_param_value pr = new msg_param_value(1, 1);
//		pr.sequence = sequence++;
//		pr.param_index = 341;
//		//pr.param_id = 341;
//        byte[] result;
//		try {
//			result = pr.encode();
//	        spc.writeData(result);
//	        return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
}
