package org.point85.domain.messaging;

import org.point85.domain.i18n.DomainLocalizer;

public class CollectorCommandMessage extends ApplicationMessage {
	public static final String CMD_RESTART = "RESTART";

	private String command;

	public CollectorCommandMessage(String senderHostName, String senderHostAddress) {
		super(senderHostName, senderHostAddress, MessageType.COMMAND);
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public void validate() throws Exception {
		super.validate();

		if (command == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("null.command"));
		}
	}
}
