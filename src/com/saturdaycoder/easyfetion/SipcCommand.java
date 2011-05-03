package com.saturdaycoder.easyfetion;

public class SipcCommand extends SipcMessage {
	public enum CommandType 
	{
		NONE,
		REGISTER,
		SUBSCRIPTION,
		SERVICE,
		MESSAGE,
		INCOMING,
		OPTION,
		INVITATION,
		ACKNOWLEDGE,
		// others
		UNKNOWN		
	};
	
	private String commandTypeStrings[] = {
		"",
		"R",
		"SUB",
		"S",
		"M",
		"IN",
		"O",
		"I",
		"A"
	};
	
	CommandType getCommandType() {
		String strs[] = this.cmdline.split(" ");
		if (strs.length <= 1) return CommandType.UNKNOWN;
		String cmdtype = strs[0];
		for (int i = 0; i < this.commandTypeStrings.length; ++i) {	
			if (this.commandTypeStrings[i].equals(cmdtype)) {
				return CommandType.values()[i];
			}
		}
		return CommandType.UNKNOWN;
	}
	
	String getCommandArg() {
		String strs[] = this.cmdline.split(" ");
		if (strs.length <= 1)
			return "";
		return strs[1];
	}
}
