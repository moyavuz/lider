package tr.org.liderahenk.lider.messaging.listeners;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.lider.core.api.configuration.IConfigurationService;

/**
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
public class XMPPConnectionListener implements ConnectionListener, PingFailedListener, StanzaListener, StanzaFilter {

	private static Logger logger = LoggerFactory.getLogger(XMPPConnectionListener.class);

	private int pingTimeoutCount = 0;

	private IConfigurationService configurationService;

	public XMPPConnectionListener(IConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public void connectionClosed() {
		logger.info("XMPP connection was closed.");
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		logger.error("XMPP connection closed with an error", e.getMessage());
	}

	@Override
	public void reconnectingIn(int seconds) {
		logger.info("Reconnecting in {} seconds.", seconds);
	}

	@Override
	public void reconnectionFailed(Exception e) {
		logger.error("Failed to reconnect to the XMPP server.", e.getMessage());
	}

	@Override
	public void reconnectionSuccessful() {
		pingTimeoutCount = 0;
		logger.info("Successfully reconnected to the XMPP server.");
	}

	@Override
	public void connected(XMPPConnection connection) {
		logger.info("User: {} connected to XMPP Server {} via port {}",
				new Object[] { connection.getUser(), connection.getHost(), connection.getPort() });
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		logger.info("Connection successfully authenticated.");
		if (resumed) {
			logger.info("A previous XMPP session's stream was resumed");
		}
	}

	@Override
	public void pingFailed() {
		pingTimeoutCount++;
		logger.warn("XMPP ping failed: {}", pingTimeoutCount);
		if (pingTimeoutCount > configurationService.getXmppPingTimeout()) {
			logger.error(
					"Too many consecutive pings failed! This doesn't necessarily mean that the connection is lost.");
			pingTimeoutCount = 0;
		}
	}

	@Override
	public void processPacket(Stanza packet) throws NotConnectedException {
		try {
			if (packet instanceof IQ) {
				IQ iq = (IQ) packet;
				if (iq.getType().equals(IQ.Type.result)) {
					pingTimeoutCount = 0;
				}
				logger.debug("IQ packet received: {}.", iq.toXML());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean accept(Stanza stanza) {
		return stanza instanceof IQ;
	}

}
