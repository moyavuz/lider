package tr.org.liderahenk.lider.core.api.messaging.enums;

/**
 * Types used when sending messages <b>from Lider to agents</b>.<br/>
 * <br/>
 * 
 * <b>EXECUTE_TASK</b>: Commands agent to execute a provided machine task.<br/>
 * <b>EXECUTE_SCRIPT</b>: Commands agent to execute a provided script and return
 * its result as response.<br/>
 * <b>REQUEST_FILE</b>: Commands agent to send a desired file back to Lider.
 * <br/>
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 * @see tr.org.liderahenk.lider.impl.messaging.XMPPClientImpl
 * 
 */
public enum LiderMessageType {
	EXECUTE_TASK, EXECUTE_SCRIPT, REQUEST_FILE
}