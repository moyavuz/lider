/*
*
*    Copyright © 2015-2016 Tübitak ULAKBIM
*
*    This file is part of Lider Ahenk.
*
*    Lider Ahenk is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    Lider Ahenk is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with Lider Ahenk.  If not, see <http://www.gnu.org/licenses/>.
*/
package tr.org.liderahenk.lider.persistence.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;

import tr.org.liderahenk.lider.core.api.messaging.enums.StatusCode;
import tr.org.liderahenk.lider.core.api.persistence.entities.ICommandExecutionResult;
import tr.org.liderahenk.lider.core.api.persistence.enums.ContentType;

/**
 * Entity class for execution result.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Kağan Akkaya</a>
 * @see tr.org.liderahenk.lider.core.api.persistence.entities.
 *      ICommandExecutionResult
 *
 */
@JsonIgnoreProperties({ "commandExecution" })
@Entity
@Table(name = "C_COMMAND_EXECUTION_RESULT")
public class CommandExecutionResultImpl implements ICommandExecutionResult {

	private static final long serialVersionUID = -8995839892973401085L;

	@Id
	@GeneratedValue
	@Column(name = "COMMAND_EXECUTION_RESULT_ID", unique = true, nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "COMMAND_EXECUTION_ID", nullable = false)
	private CommandExecutionImpl commandExecution; // bidirectional

	@Column(name = "AGENT_ID")
	private Long agentId;

	@Column(name = "RESPONSE_CODE", nullable = false, length = 3)
	private Integer responseCode;

	@Lob
	@Column(name = "RESPONSE_MESSAGE")
	private String responseMessage;

	/**
	 * Response data can be anything (JSON data, alphanumeric value, file such
	 * as JPEG, doc etc.)
	 */
	@Lob
	@Column(name = "RESPONSE_DATA", length = 4 * 1024 * 1024 * 1024)
	private byte[] responseData;

	@Column(name = "CONTENT_TYPE", length = 3)
	private Integer contentType;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATE_DATE", nullable = false)
	private Date createDate;

	@Column(name = "MAIL_SUBJECT", length = 1000)
	private String mailSubject;

	@Column(name = "MAIL_CONTENT", columnDefinition = "TEXT", length = 65535)
	private String mailContent;

	public CommandExecutionResultImpl() {
	}

	public CommandExecutionResultImpl(Long id, CommandExecutionImpl commandExecution, Long agentId,
			StatusCode responseCode, String responseMessage, byte[] responseData, ContentType contentType,
			Date createDate, String mailSubject, String mailContent) {
		super();
		this.id = id;
		this.commandExecution = commandExecution;
		this.agentId = agentId;
		setResponseCode(responseCode);
		this.responseMessage = responseMessage;
		this.responseData = responseData;
		setContentType(contentType);
		this.createDate = createDate;
		this.mailSubject = mailSubject;
		this.mailContent = mailContent;
	}

	public CommandExecutionResultImpl(ICommandExecutionResult commandExecutionResult) {
		this.id = commandExecutionResult.getId();
		this.agentId = commandExecutionResult.getAgentId();
		setResponseCode(commandExecutionResult.getResponseCode());
		this.responseMessage = commandExecutionResult.getResponseMessage();
		this.responseData = commandExecutionResult.getResponseData();
		setContentType(commandExecutionResult.getContentType());
		this.createDate = commandExecutionResult.getCreateDate();
		if (commandExecutionResult.getCommandExecution() instanceof CommandExecutionImpl) {
			this.commandExecution = (CommandExecutionImpl) commandExecutionResult.getCommandExecution();
		}
		this.mailContent = commandExecutionResult.getMailContent();
		this.mailSubject = commandExecutionResult.getMailSubject();
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public CommandExecutionImpl getCommandExecution() {
		return commandExecution;
	}

	public void setCommandExecution(CommandExecutionImpl commandExecution) {
		this.commandExecution = commandExecution;
	}

	@Override
	public StatusCode getResponseCode() {
		return StatusCode.getType(responseCode);
	}

	public void setResponseCode(StatusCode responseCode) {
		if (responseCode == null) {
			this.responseCode = null;
		} else {
			this.responseCode = responseCode.getId();
		}
	}

	@Override
	public Long getAgentId() {
		return agentId;
	}

	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}

	@Override
	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	@Override
	public byte[] getResponseData() {
		return responseData;
	}

	public void setResponseData(byte[] responseData) {
		this.responseData = responseData;
	}

	@Override
	public ContentType getContentType() {
		return ContentType.getType(contentType);
	}

	public void setContentType(ContentType contentType) {
		if (contentType == null) {
			this.contentType = null;
		} else {
			this.contentType = contentType.getId();
		}
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Override
	public String getMailSubject() {
		return mailSubject;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	@Override
	public String getMailContent() {
		return mailContent;
	}

	public void setMailContent(String mailContent) {
		this.mailContent = mailContent;
	}

	@Override
	public String toJson() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return "CommandExecutionResultImpl [id=" + id + ", agentId=" + agentId + ", responseCode=" + responseCode
				+ ", responseMessage=" + responseMessage + ", contentType=" + contentType + ", createDate=" + createDate
				+ ", mailSubject=" + mailSubject + ", mailContent=" + mailContent + "]";
	}

}
