package tr.org.liderahenk.lider.messaging;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager.AutoReceiptMode;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.lider.core.api.configuration.IConfigurationService;
import tr.org.liderahenk.lider.core.api.messaging.enums.StatusCode;
import tr.org.liderahenk.lider.core.api.messaging.messages.IExecutePoliciesMessage;
import tr.org.liderahenk.lider.core.api.messaging.messages.ILiderMessage;
import tr.org.liderahenk.lider.core.api.messaging.messages.IRegistrationResponseMessage;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.IPolicyStatusSubscriber;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.IPolicySubscriber;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.IPresenceSubscriber;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.IRegistrationSubscriber;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.ITaskStatusSubscriber;
import tr.org.liderahenk.lider.core.api.messaging.subscribers.IUserSessionSubscriber;
import tr.org.liderahenk.lider.messaging.listeners.FileListener;
import tr.org.liderahenk.lider.messaging.listeners.OnlineRosterListener;
import tr.org.liderahenk.lider.messaging.listeners.PacketListener;
import tr.org.liderahenk.lider.messaging.listeners.XMPPConnectionListener;
import tr.org.liderahenk.lider.messaging.messages.GetPoliciesMessageImpl;
import tr.org.liderahenk.lider.messaging.messages.PolicyStatusMessageImpl;
import tr.org.liderahenk.lider.messaging.messages.RegistrationMessageImpl;
import tr.org.liderahenk.lider.messaging.messages.RegistrationResponseMessageImpl;
import tr.org.liderahenk.lider.messaging.messages.TaskStatusMessageImpl;
import tr.org.liderahenk.lider.messaging.messages.UserSessionMessageImpl;
import tr.org.liderahenk.lider.messaging.subscribers.DefaultRegistrationSubscriberImpl;

/**
 * This class works as an XMPP client which listens to incoming packets and
 * provides XMPP utility methods such as sending messages and reading roster.
 * 
 * @author <a href="mailto:bm.volkansahin@gmail.com">volkansahin</a>
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 * 
 */
public class XMPPClientImpl {

	private static Logger logger = LoggerFactory.getLogger(XMPPClientImpl.class);

	/**
	 * Connection and settings parameters are got from tr.org.liderahenk.cfg
	 */
	private String username;
	private String password;
	private String serviceName; // Service name / XMPP domain
	private String host; // Host name / Server name
	private Integer port; // Default 5222
	private int maxRetryConnectionCount;
	private int retryCount = 0;
	private int packetReplyTimeout; // milliseconds
	private int pingTimeout; // milliseconds

	/**
	 * Connection & packet listeners/filters
	 */
	private XMPPConnectionListener connectionListener;
	private OnlineRosterListener onlineRosterListener;
	private PacketListener packetListener;
	private FileListener fileListener;
	
	private TaskStatusListener taskStatusListener;
	private PolicyStatusListener policyStatusListener;
	private RegistrationListener registrationListener;
	private UserSessionListener userSessionListener;
	private PolicyListener policyListener;

	/**
	 * Packet subscribers
	 */
	private List<ITaskStatusSubscriber> taskStatusSubscribers;
	private List<IPolicyStatusSubscriber> policyStatusSubscribers;
	private List<IRegistrationSubscriber> registrationSubscribers;
	private List<IUserSessionSubscriber> userSessionSubscribers;
	private List<IPresenceSubscriber> presenceSubscribers;
	private IPolicySubscriber policySubscriber;

	private XMPPTCPConnection connection;
	private XMPPTCPConnectionConfiguration config;
	private MultiUserChatManager mucManager;

	private static final Pattern taskStatusPattern = Pattern.compile(".*\\\"type\\\"\\s*:\\s*\\\"TASK_STATUS\\\".*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern policyStatusPattern = Pattern.compile(".*\\\"type\\\"\\s*:\\s*\\\"POLICY_STATUS\\\".*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern registerPattern = Pattern.compile(".*\\\"type\\\"\\s*:\\s*\\\"(REGISTER|UNREGISTER)\\\".*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern userSessionPattern = Pattern.compile(".*\\\"type\\\"\\s*:\\s*\\\"LOG(IN|OUT)\\\".*",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern policyPattern = Pattern.compile(".*\\\"type\\\"\\s*:\\s*\\\"GET_POLICIES\\\".*",
			Pattern.CASE_INSENSITIVE);

	private IConfigurationService configurationService;
	private EventAdmin eventAdmin;

	public void init() {
		logger.info("XMPP service initialization is started");
		setParameters();
		createXmppTcpConfiguration();
		connect();
		login();
		setServerSettings();
		addListeners();
		logger.info("XMPP service initialized");
	}

	/**
	 * Sets XMPP client parameters.
	 */
	private void setParameters() {
		this.username = configurationService.getXmppUsername();
		this.password = configurationService.getXmppPassword();
		this.serviceName = configurationService.getXmppServiceName();
		this.host = configurationService.getXmppHost();
		this.port = configurationService.getXmppPort();
		this.maxRetryConnectionCount = configurationService.getXmppMaxRetryConnectionCount();
		this.packetReplyTimeout = configurationService.getXmppPacketReplayTimeout();
		this.pingTimeout = configurationService.getXmppPingTimeout();
		logger.debug(this.toString());
	}

	/**
	 * Configures XMPP connection parameters.
	 */
	private void createXmppTcpConfiguration() {
		config = XMPPTCPConnectionConfiguration.builder().setServiceName(serviceName).setHost(host).setPort(port)
				.setSecurityMode(configurationService.getXmppUseSsl() ? SecurityMode.required : SecurityMode.disabled)
				.setDebuggerEnabled(logger.isDebugEnabled())
				.build();
		logger.debug("XMPP configuration finished: {}", config.toString());
	}

	/**
	 * Connects to XMPP server
	 */
	private void connect() {
		connection = new XMPPTCPConnection(config);
		// Retry connection if it fails.
		while (!connection.isConnected() && retryCount < maxRetryConnectionCount) {
			retryCount++;
			try {
				try {
					connection.connect();
				} catch (SmackException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (XMPPException e) {
				logger.error("Cannot connect to XMPP server.");
			}
		}
		retryCount = 0;
		logger.debug("Successfully connected to XMPP server.");
	}

	/**
	 * Login to connected XMPP server via provided username-password.
	 * 
	 * @param username
	 * @param password
	 */
	private void login() {
		if (connection != null && connection.isConnected()) {
			try {
				connection.login(username, password);
				logger.debug("Successfully logged in to XMPP server: {}", username);
			} catch (XMPPException e) {
				logger.error(e.getMessage(), e);
			} catch (SmackException e) {
				logger.error(e.getMessage(), e);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Configure XMPP connection to use provided ping timeout and reply timeout.
	 */
	private void setServerSettings() {
		PingManager.getInstanceFor(connection).setPingInterval(pingTimeout);
		mucManager = MultiUserChatManager.getInstanceFor(connection);
		// Specifies when incoming message delivery receipt requests
		// should be automatically acknowledged with a receipt.
		DeliveryReceiptManager.getInstanceFor(connection).setAutoReceiptMode(AutoReceiptMode.always);
		SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);
		logger.debug("Successfully set server settings: {} - {}", new Object[] { pingTimeout, packetReplyTimeout });
	}

	/**
	 * Hook packet and connection listeners
	 */
	private void addListeners() {
		connectionListener = new XMPPConnectionListener(configurationService);
		onlineRosterListener = new OnlineRosterListener(connection);
		onlineRosterListener.setPresenceSubscribers(presenceSubscribers);
		packetListener = new PacketListener();
		taskStatusListener = new TaskStatusListener();
		policyStatusListener = new PolicyStatusListener();
		registrationListener = new RegistrationListener();
		policyListener = new PolicyListener();
		userSessionListener = new UserSessionListener();
		connection.addConnectionListener(connectionListener);
		fileListener = new FileListener(configurationService, eventAdmin);
		Socks5BytestreamManager bytestreamManager = Socks5BytestreamManager.getBytestreamManager(connection);
		bytestreamManager.addIncomingBytestreamListener(fileListener);
		PingManager.getInstanceFor(connection).registerPingFailedListener(connectionListener);
		connection.addAsyncStanzaListener(packetListener, packetListener);
		connection.addAsyncStanzaListener(registrationListener, registrationListener);
		connection.addAsyncStanzaListener(policyListener, policyListener);
		connection.addAsyncStanzaListener(userSessionListener, userSessionListener);
		connection.addAsyncStanzaListener(taskStatusListener, taskStatusListener);
		connection.addAsyncStanzaListener(policyStatusListener, policyStatusListener);
		connection.addAsyncStanzaListener(connectionListener, connectionListener);
		Roster.getInstanceFor(connection).addRosterListener(onlineRosterListener);
		logger.debug("Successfully added listeners for connection: {}", connection.toString());
	}

	/**
	 * Delete specific user
	 * 
	 * @param jid
	 * @param password
	 */
	public void deleteUser(String jid, String password) {
		XMPPTCPConnection tempConnection = null;
		try {
			tempConnection = new XMPPTCPConnection(this.host, this.port.toString());
			tempConnection.login(jid, password);

			AccountManager accountManager = AccountManager.getInstance(tempConnection);
			accountManager.deleteAccount();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		tempConnection.disconnect();
	}

	public void destroy() {
		this.disconnect();
	}

	/**
	 * Remove all connection & packet listeners and disconnect XMPP connection.
	 */
	public void disconnect() {
		if (null != connection && connection.isConnected()) {
			// Remove listeners
			Roster.getInstanceFor(connection).removeRosterListener(onlineRosterListener);
			connection.removeAsyncStanzaListener(packetListener);
			connection.removeAsyncStanzaListener(taskStatusListener);
			connection.removeAsyncStanzaListener(policyStatusListener);
			connection.removeAsyncStanzaListener(registrationListener);
			connection.removeAsyncStanzaListener(userSessionListener);
			connection.removeAsyncStanzaListener(policyListener);
			connection.removeAsyncStanzaListener(connectionListener);
			connection.removeConnectionListener(connectionListener);
			Socks5BytestreamManager bytestreamManager = Socks5BytestreamManager.getBytestreamManager(connection);
			bytestreamManager.removeIncomingBytestreamListener(fileListener);
			logger.debug("Listeners are removed.");
			PingManager.getInstanceFor(connection).setPingInterval(-1);
			logger.debug("Disabled ping manager");
			connection.disconnect();
			logger.info("Successfully closed XMPP connection.");
		}
	}

	/**
	 * Send provided message to provided JID. Message type is always NORMAL.
	 * 
	 * @param message
	 * @param jid
	 * @throws NotConnectedException
	 */
	public void sendMessage(String message, String jid) throws NotConnectedException {
		String jidFinal = getFullJid(jid);
		logger.debug("Sending message: {} to user: {}", new Object[] { message, jidFinal });
		Message msg = new Message(jidFinal, Message.Type.normal);
		msg.setBody(message);
		connection.sendStanza(msg);
		logger.debug("Successfully sent message to user: {}", jidFinal);
	}

	/**
	 * Convenience method for ILiderMessage instances.
	 * 
	 * @param obj
	 *            message to be sent
	 * @throws NotConnectedException
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	public void sendMessage(ILiderMessage message)
			throws NotConnectedException, JsonGenerationException, JsonMappingException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

		String msgStr = mapper.writeValueAsString(message);
		String jid = message.getRecipient();
		sendMessage(msgStr, getFullJid(jid));
	}

	/**
	 * Check if given recipient is whether online or not.
	 * 
	 * @param jid
	 * @return true iff the provided JID is not null or empty and it is online.
	 */
	public boolean isRecipientOnline(String jid) {
		boolean isOnline = false;
		if (jid != null && !jid.isEmpty()) {
			String jidFinal = getFullJid(jid);
			Presence presence = Roster.getInstanceFor(connection).getPresence(jidFinal);
			if (presence != null) {
				isOnline = presence.isAvailable();
			}
		}
		return isOnline;
	}

	/**
	 * Send invites to clients for joining multi user chat room
	 * 
	 * @param muc
	 * @param userList
	 * @param inviteMessage
	 */
	public void sendRoomInvite(MultiUserChat muc, ArrayList<String> userList, String inviteMessage) {

		if (muc != null && muc.getRoom() != null && !muc.getRoom().isEmpty()) {

			if (userList != null && !userList.isEmpty()) {
				for (String user : userList) {
					try {
						muc.invite(user, inviteMessage);
					} catch (NotConnectedException e) {
						e.printStackTrace();
					}
				}
				logger.info(userList.size() + " clients were invited to room(" + muc.getRoom() + ")");
			}
		} else {
			logger.info("There is no available room for invitation");
		}
	}

	/**
	 * Create new multi user chat jid ex: room1@conference.localhost
	 * 
	 * @param roomJid
	 * @param nickName
	 * @return
	 */
	public MultiUserChat createRoom(String roomJid, String nickName) {
		MultiUserChat muc = mucManager.getMultiUserChat(roomJid);
		try {
			muc.create(nickName);
			muc.sendConfigurationForm(new Form(DataForm.Type.submit));
		} catch (NoResponseException e) {
			e.printStackTrace();
		} catch (XMPPErrorException e) {
			e.printStackTrace();
		} catch (SmackException e) {
			e.printStackTrace();
		}

		return muc;
	}

	/**
	 * Send message to room
	 * 
	 * @param muc
	 * @param message
	 */
	public void sendMessageToRoom(MultiUserChat muc, String message) {
		try {
			if (muc != null && muc.getMembers() != null && message != null && !message.isEmpty()) {
				muc.sendMessage(message);
			}
		} catch (NotConnectedException e) {
			e.printStackTrace();
		} catch (NoResponseException e) {
			e.printStackTrace();
		} catch (XMPPErrorException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create new user with the provided password.
	 * 
	 * @param username
	 * @param password
	 * @return true if user created successfully, false otherwise
	 * @throws NotConnectedException
	 * @throws XMPPErrorException
	 * @throws NoResponseException
	 */
	public void createAccount(String username, String password)
			throws NoResponseException, XMPPErrorException, NotConnectedException {
		AccountManager.sensitiveOperationOverInsecureConnectionDefault(true);
		AccountManager accountManager = AccountManager.getInstance(connection);
		if (accountManager.supportsAccountCreation()) {
			accountManager.createAccount(username, password);
		}
	}

	/**
	 * Send file to provided JID via a SOCKS5 Bytestream session (XEP-0065).
	 * 
	 * @param file
	 * @param jid
	 * @throws SmackException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws XMPPException
	 */
	public void sendFile(byte[] file, String jid)
			throws XMPPException, IOException, InterruptedException, SmackException {
		String jidFinal = getFullJid(jid);
		jidFinal += "/receiver";
		Socks5BytestreamManager bytestreamManager = Socks5BytestreamManager.getBytestreamManager(connection);
		OutputStream outputStream = null;
		try {
			Socks5BytestreamSession session = bytestreamManager.establishSession(jidFinal);
			outputStream = session.getOutputStream();
			outputStream.write(file);
			outputStream.flush();
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Get full JID in format:<br/>
	 * "jid@$serviceName"
	 * 
	 * @param jid
	 * @return JID full name (jid + service name)
	 */
	public String getFullJid(String jid) {
		String jidFinal = jid;
		if (jid.indexOf("@") < 0) {
			jidFinal = jid + "@" + serviceName;
		}
		return jidFinal;
	}

	/**
	 * Listens to task status messages
	 *
	 */
	class TaskStatusListener implements StanzaListener, StanzaFilter {

		@Override
		public boolean accept(Stanza stanza) {
			if (stanza instanceof Message) {
				Message msg = (Message) stanza;
				// All messages from agents are type normal
				if (Message.Type.normal.equals(msg.getType()) && taskStatusPattern.matcher(msg.getBody()).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {
			try {
				if (packet instanceof Message) {

					Message msg = (Message) packet;
					logger.info("Task status update message received from => {}, body => {}", msg.getFrom(),
							msg.getBody());

					ObjectMapper mapper = new ObjectMapper();
					mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

					TaskStatusMessageImpl message = mapper.readValue(msg.getBody(), TaskStatusMessageImpl.class);
					message.setFrom(msg.getFrom());

					for (ITaskStatusSubscriber subscriber : taskStatusSubscribers) {
						try {
							subscriber.messageReceived(message);
						} catch (Exception e) {
							logger.error("Subscriber could not handle message: ", e);
						}
						logger.debug("Notified subscriber => {}", subscriber);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * Listens to task status messages
	 *
	 */
	class PolicyStatusListener implements StanzaListener, StanzaFilter {

		@Override
		public boolean accept(Stanza stanza) {
			if (stanza instanceof Message) {
				Message msg = (Message) stanza;
				logger.error("--->" + msg.getBody());
				// All messages from agents are type normal
				if (Message.Type.normal.equals(msg.getType()) && policyStatusPattern.matcher(msg.getBody()).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {
			try {
				if (packet instanceof Message) {

					Message msg = (Message) packet;
					logger.info("Policy status update message received from => {}, body => {}", msg.getFrom(),
							msg.getBody());

					ObjectMapper mapper = new ObjectMapper();
					mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

					PolicyStatusMessageImpl message = mapper.readValue(msg.getBody(), PolicyStatusMessageImpl.class);
					message.setFrom(msg.getFrom());

					for (IPolicyStatusSubscriber subscriber : policyStatusSubscribers) {
						try {
							subscriber.messageReceived(message);
						} catch (Exception e) {
							logger.error("Subscriber could not handle message: ", e);
						}
						logger.debug("Notified subscriber => {}", subscriber);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * RegistrationListener is responsible for listening to agent register
	 * messages. It triggers {@link IRegistrationSubscriber} instance upon
	 * incoming register messages. If there is no subscriber, it falls back to
	 * the default subscriber to handle registration.
	 * 
	 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
	 * @see tr.org.liderahenk.lider.DefaultRegistrationSubscriberImpl.
	 *      registration. DefaultRegistrationSubscriber
	 *
	 */
	class RegistrationListener implements StanzaListener, StanzaFilter {

		@Override
		public boolean accept(Stanza stanza) {
			if (stanza instanceof Message) {
				Message msg = (Message) stanza;
				// All messages from agents are type normal
				// Message body must contain this string => "type": "REGISTER"
				if (Message.Type.normal.equals(msg.getType()) && registerPattern.matcher(msg.getBody()).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {

			IRegistrationResponseMessage responseMessage = null;
			Message msg = null;

			try {
				if (packet instanceof Message) {

					msg = (Message) packet;
					logger.info("Register message received from => {}, body => {}", msg.getFrom(), msg.getBody());

					// Construct message
					ObjectMapper mapper = new ObjectMapper();
					mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

					RegistrationMessageImpl message = mapper.readValue(msg.getBody(), RegistrationMessageImpl.class);

					// Fall back to default register subscriber if reference
					// list is empty.
					if (registrationSubscribers == null || registrationSubscribers.isEmpty()) {
						responseMessage = triggerDefaultSubscriber(message);
					} else {
						// Try to find subscriber other than the default one.
						IRegistrationSubscriber subscriber = null;
						for (IRegistrationSubscriber temp : registrationSubscribers) {
							if (!(temp instanceof DefaultRegistrationSubscriberImpl)) {
								subscriber = temp;
								break;
							}
						}
						// Found another subscriber, notify it.
						if (subscriber != null) {
							responseMessage = subscriber.messageReceived(message);
							logger.debug("Notified subscriber => {}", subscriber);
						} else {
							// We cannot find another subscriber, trigger the
							// default.
							responseMessage = triggerDefaultSubscriber(message);
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				responseMessage = new RegistrationResponseMessageImpl(StatusCode.REGISTRATION_ERROR,
						"Unexpected error occurred while registring agent, see Lider logs for more info.", null,
						msg.getFrom(), new Date());
			}

			// Send registration info back to agent
			try {
				sendMessage(new ObjectMapper().writeValueAsString(responseMessage), msg.getFrom());
			} catch (JsonGenerationException e) {
				logger.error(e.getMessage(), e);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage(), e);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}

		private IRegistrationResponseMessage triggerDefaultSubscriber(RegistrationMessageImpl message)
				throws Exception {
			logger.info("Triggering default register subscriber.");
			IRegistrationSubscriber subscriber = new DefaultRegistrationSubscriberImpl();
			IRegistrationResponseMessage registrationInfo = subscriber.messageReceived(message);
			logger.debug("Notified subscriber => {}", subscriber);
			return registrationInfo;
		}

	}

	/**
	 * User session listener is responsible for logging user login and logout
	 * events.
	 * 
	 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
	 *
	 */
	class UserSessionListener implements StanzaListener, StanzaFilter {

		@Override
		public boolean accept(Stanza stanza) {
			if (stanza instanceof Message) {
				Message msg = (Message) stanza;
				// All messages from agents are type normal
				// Message body must contain one of these strings => "type":
				// "LOGIN" or "type": "LOGOUT"
				if (Message.Type.normal.equals(msg.getType()) && userSessionPattern.matcher(msg.getBody()).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {

			Message msg = null;

			try {
				if (packet instanceof Message) {

					msg = (Message) packet;
					logger.info("Register message received from => {}, body => {}", msg.getFrom(), msg.getBody());

					ObjectMapper mapper = new ObjectMapper();
					mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

					// Construct message
					UserSessionMessageImpl message = mapper.readValue(msg.getBody(), UserSessionMessageImpl.class);
					message.setFrom(msg.getFrom());

					if (userSessionSubscribers != null && !userSessionSubscribers.isEmpty()) {
						// Notify each subscriber
						for (IUserSessionSubscriber subscriber : userSessionSubscribers) {
							try {
								subscriber.messageReceived(message);
							} catch (Exception e) {
								logger.error("Subscriber could not handle message: ", e);
							}
							logger.debug("Notified subscriber => {}", subscriber);
						}
					}

				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}

	}

	/**
	 * Policy listener is responsible for sending (machine and user) policies to
	 * agent.
	 * 
	 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
	 *
	 */
	class PolicyListener implements StanzaListener, StanzaFilter {

		@Override
		public boolean accept(Stanza stanza) {
			if (stanza instanceof Message) {
				Message msg = (Message) stanza;
				// All messages from agents are type normal
				// Message body must contain one of these strings => "type":
				// "GET_POLICIES"
				if (Message.Type.normal.equals(msg.getType()) && policyPattern.matcher(msg.getBody()).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {

			IExecutePoliciesMessage responseMessage = null;
			Message msg = null;

			try {
				if (packet instanceof Message) {

					msg = (Message) packet;
					logger.info("Policy message received from => {}, body => {}", msg.getFrom(), msg.getBody());

					ObjectMapper mapper = new ObjectMapper();
					mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm"));

					// Construct message
					GetPoliciesMessageImpl message = mapper.readValue(msg.getBody(), GetPoliciesMessageImpl.class);
					message.setFrom(msg.getFrom());

					if (policySubscriber != null) {
						responseMessage = policySubscriber.messageReceived(message);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			// Send registration info back to agent
			try {
				sendMessage(new ObjectMapper().writeValueAsString(responseMessage), msg.getFrom());
			} catch (JsonGenerationException e) {
				logger.error(e.getMessage(), e);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage(), e);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * 
	 * @param configurationService
	 */
	public void setConfigurationService(IConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	/**
	 * 
	 * @param presenceSubscribers
	 */
	public void setPresenceSubscribers(List<IPresenceSubscriber> presenceSubscribers) {
		this.presenceSubscribers = presenceSubscribers;
		if (onlineRosterListener != null) onlineRosterListener.setPresenceSubscribers(presenceSubscribers) ;
	}

	/**
	 * 
	 * @param taskStatusSubscribers
	 */
	public void setTaskStatusSubscribers(List<ITaskStatusSubscriber> taskStatusSubscribers) {
		this.taskStatusSubscribers = taskStatusSubscribers;
	}

	/**
	 * 
	 * @param policyStatusSubscribers
	 */
	public void setPolicyStatusSubscribers(List<IPolicyStatusSubscriber> policyStatusSubscribers) {
		this.policyStatusSubscribers = policyStatusSubscribers;
	}

	/**
	 * 
	 * @param registrationSubscribers
	 */
	public void setRegistrationSubscribers(List<IRegistrationSubscriber> registrationSubscribers) {
		this.registrationSubscribers = registrationSubscribers;
	}

	/**
	 * 
	 * @param userSessionSubscribers
	 */
	public void setUserSessionSubscribers(List<IUserSessionSubscriber> userSessionSubscribers) {
		this.userSessionSubscribers = userSessionSubscribers;
	}

	/**
	 * 
	 * @param policySubscriber
	 */
	public void setPolicySubscriber(IPolicySubscriber policySubscriber) {
		this.policySubscriber = policySubscriber;
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getOnlineUsers() {
		return onlineRosterListener.getOnlineUsers();
	}

	/**
	 * 
	 * @return
	 */
	public XMPPTCPConnection getConnection() {
		return connection;
	}

	/**
	 * 
	 * @param eventAdmin
	 */
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
