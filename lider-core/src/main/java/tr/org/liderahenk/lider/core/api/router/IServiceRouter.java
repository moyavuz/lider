package tr.org.liderahenk.lider.core.api.router;

import java.util.List;

import tr.org.liderahenk.lider.core.api.plugin.ICommand;
import tr.org.liderahenk.lider.core.api.rest.IRestRequest;
import tr.org.liderahenk.lider.core.api.rest.IRestResponse;
import tr.org.liderahenk.lider.core.api.taskmanager.TaskSubmissionFailedException;
import tr.org.liderahenk.lider.core.model.ldap.LdapEntry;

/**
 * Routes {@link IRestRequest} to matching {@link ICommand} in
 * {@link IServiceRegistry}
 *
 * @author <a href="mailto:birkan.duman@gmail.com">Birkan Duman</a>
 */
public interface IServiceRouter {

	/**
	 * delegates {@link IRestRequest} to matching {@link ICommand} and executes
	 * it, then returns immediately to the rest client or after creating a task
	 * from the request and getting a Task id
	 * 
	 * @param request
	 *            REST request
	 * @param entries
	 * @return
	 * @throws InvalidRequestException
	 *             if no matching {@link ICommand} to handle
	 *             {@link IRestRequest}
	 * @throws TaskSubmissionFailedException
	 *             if sth goes wrong during task creation wrt
	 *             {@link IRestRequest}
	 */
	IRestResponse delegateRequest(IRestRequest request, List<LdapEntry> entries)
			throws InvalidRequestException, TaskSubmissionFailedException;

}