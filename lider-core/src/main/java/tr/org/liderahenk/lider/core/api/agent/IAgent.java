package tr.org.liderahenk.lider.core.api.agent;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * IAgent entity class is responsible for storing agent records.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Kağan Akkaya</a>
 *
 */
public interface IAgent extends Serializable {

	/**
	 * 
	 * @return
	 */
	Long getId();
	
	/**
	 * 
	 * @return
	 */
	Boolean getDeleted();

	/**
	 * 
	 * @return
	 */
	String getJid();

	/**
	 * 
	 * @return
	 */
	String getPassword();

	/**
	 * 
	 * @return
	 */
	String getHostname();

	/**
	 * 
	 * @return
	 */
	String getIpAddresses();

	/**
	 * 
	 * @return
	 */
	String getMacAddresses();

	/**
	 * 
	 * @return
	 */
	String getDn();

	/**
	 * 
	 * @return
	 */
	Date getCreationDate();

	/**
	 * 
	 * @return
	 */
	List<? extends IAgentProperty> getProperties();

	/**
	 * 
	 * @param property
	 */
	void addProperty(IAgentProperty property);

	/**
	 * 
	 * @return
	 */
	List<? extends IUserSession> getSessions();

	/**
	 * 
	 * @param userSession
	 */
	void addUserSession(IUserSession userSession);

}
