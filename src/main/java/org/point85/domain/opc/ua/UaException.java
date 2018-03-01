package org.point85.domain.opc.ua;

public class UaException extends Exception {

	private static final long serialVersionUID = -8926034384486797525L;

	// TODO:  provide helpful hint
	private String help;

	public UaException(String message) {
		super(message);
	}

	public UaException(String message, String help) {
		super(message);
		this.setHelp(help);
	}

	public UaException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

}
