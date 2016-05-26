package tr.org.liderahenk.lider.taskmanager.notifiers;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.lider.core.api.messaging.IMessageFactory;
import tr.org.liderahenk.lider.core.api.messaging.IMessagingService;
import tr.org.liderahenk.lider.core.api.messaging.messages.ITaskStatusMessage;
import tr.org.liderahenk.lider.core.api.messaging.notifications.ITaskStatusNotification;
import tr.org.liderahenk.lider.core.api.persistence.entities.ICommandExecutionResult;

/**
 * Lider Console notifier implementation for {@link EventHandler}. This class
 * listens to task status messages and notifies related Lider Console users if
 * there is any.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
public class LiderConsoleNotifier implements EventHandler {

	private Logger logger = LoggerFactory.getLogger(LiderConsoleNotifier.class);

	private IMessageFactory messageFactory;
	private IMessagingService messagingService;

	@Override
	public void handleEvent(Event event) {
		logger.debug("Started handling task status.");

		ITaskStatusMessage message = (ITaskStatusMessage) event.getProperty("message");
		ICommandExecutionResult result = (ICommandExecutionResult) event.getProperty("result");

		String recipient = result.getCommandExecution().getCommand().getCommandOwnerUid();
		logger.info("Sending task status message to Lider Console. Task: {} Status: {} JID: {}",
				new Object[] { message.getTaskId(), message.getResponseCode(), recipient });

		try {
			ITaskStatusNotification notification = messageFactory.createTaskStatusNotification(recipient, result);
			messagingService.sendNotification(notification);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("Handled task status.");
	}

	/**
	 * 
	 * @param messageFactory
	 */
	public void setMessageFactory(IMessageFactory messageFactory) {
		this.messageFactory = messageFactory;
	}

	/**
	 * 
	 * @param messagingService
	 */
	public void setMessagingService(IMessagingService messagingService) {
		this.messagingService = messagingService;
	}

}
