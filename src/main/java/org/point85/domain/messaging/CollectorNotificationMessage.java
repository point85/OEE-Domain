package org.point85.domain.messaging;

public class CollectorNotificationMessage extends ApplicationMessage {

	private NotificationSeverity severity;
	private String text;

	public CollectorNotificationMessage(String senderHostName,  String senderHostAddress) {
		super(senderHostName, senderHostAddress, MessageType.NOTIFICATION);
	}

	public NotificationSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(NotificationSeverity severity) {
		this.severity = severity;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public void validate() throws Exception {
		super.validate();
		
		if (text == null) {
			throw new Exception("The notification text cannot be null");
		}
	}
}
