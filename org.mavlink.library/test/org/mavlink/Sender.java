package org.mavlink;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.mavlink.messages.MAV_AUTOPILOT;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_STATE;
import org.mavlink.messages.ja4rtor.msg_heartbeat;
import org.mavlink.messages.ja4rtor.msg_mission_item;
import org.mavlink.messages.ja4rtor.msg_param_request_list;
import org.mavlink.messages.ja4rtor.msg_param_value;
import org.mavlink.messages.ja4rtor.msg_request_data_stream;

import jssc.SerialPortList;

public class Sender {
	private SerialPortCommunicator spc;
	private int sequence = 0;
	
	public Sender(SerialPortCommunicator spc){
		this.spc = spc;
	}

	public boolean send() {
		msg_heartbeat hb = new msg_heartbeat(1, 1);
        hb.sequence = sequence++;
        hb.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_PX4;
        hb.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED;
        hb.custom_mode = 0; //custom mode
        hb.mavlink_version = 3;
        hb.system_status = MAV_STATE.MAV_STATE_POWEROFF;
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
	
	public boolean send2() {
		msg_request_data_stream ds = new msg_request_data_stream(1, 1);
		ds.sequence = sequence++;
		ds.req_message_rate = 10;
		ds.target_system = 1;
		ds.target_component = 1;
		ds.req_stream_id = 10; //try 2, 6, 10, 11, 12, 01, 03 (10 is orientation AHRS)
		ds.start_stop = 1;
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
	
	public boolean send3() {
		msg_param_request_list pr = new msg_param_request_list(1, 1);
		pr.sequence = sequence++;
		pr.target_system = 1;
		pr.target_component = 1;
        byte[] result;
		try {
			result = pr.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean send4() {
		msg_param_value pr = new msg_param_value(1, 1);
		pr.sequence = sequence++;
		pr.param_index = 341;
		//pr.param_id = 341;
        byte[] result;
		try {
			result = pr.encode();
	        spc.writeData(result);
	        return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
