package com.oxygenxml.git.view.event;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.LoginDialog;
import com.oxygenxml.git.view.PullWithConflictsDialog;
import com.oxygenxml.git.view.StatusMessages;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * 
 * Executes the push and pull commands and update the observer state after
 * initiating those commands. Prompts the user to enter new credentials if
 * necessary.
 * 
 * @author Beniamin Savu
 *
 */
public class PushPullController implements Subject<PushPullEvent> {

	private static Logger logger = Logger.getLogger(PushPullController.class);

	/**
	 * After a pull or push this will chage it's state
	 */
	private Observer<PushPullEvent> observer;

	/**
	 * The Git API
	 */
	private GitAccess gitAccess;

	boolean commandExecuted = true;

	public PushPullController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	/**
	 * Opens a login dialog to update the credentials
	 * 
	 * @param loginMessage
	 * 
	 * @return the new credentials
	 */
	public UserCredentials loadNewCredentials(String loginMessage) {
		return new LoginDialog(gitAccess.getHostName(), loginMessage).getUserCredentials();
	}

	/**
	 * Creates a new Thread to do the action depending on the given command(Push
	 * or Pull) so that the application will not freeze.
	 * 
	 * 
	 * @param command
	 *          - The command to execute
	 */
	public void execute(final Command command) {
		final UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
		String message = "";
		if (command == Command.PUSH) {
			message = StatusMessages.PUSH_IN_PROGRESS;
		} else {
			message = StatusMessages.PULL_IN_PROGRESS;
		}
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED, message);
		notifyObservers(pushPullEvent);
		new Thread(new Runnable() {

			public void run() {
				String message = "";
				try {
					if (command == Command.PUSH) {
						message = push(userCredentials);
					} else {
						message = pull(userCredentials);
					}
					commandExecuted = true;
				} catch (GitAPIException e) {
					if (e.getMessage().contains("not authorized")) {
						String loginMessage = "";
						if ("".equals(userCredentials.getUsername())) {
							loginMessage = "Invalid credentials";
						} else {
							loginMessage = "Invalid credentials for " + userCredentials.getUsername();
						}
						// JOptionPane.showMessageDialog((Component)
						// PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
						// "Invalid credentials for " + userCredentials.getUsername());
						UserCredentials loadNewCredentials = loadNewCredentials(loginMessage);
						if (loadNewCredentials != null) {
							commandExecuted = false;
							execute(command);
							return;
						}
					}
					if (e.getMessage().contains("not permitted")) {
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"You have no rights to push in this repository " + userCredentials.getUsername());
					}
					e.printStackTrace();
				} catch (RevisionSyntaxException e) {
					e.printStackTrace();
				} catch (AmbiguousObjectException e) {
					e.printStackTrace();
				} catch (IncorrectObjectTypeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (commandExecuted) {
						PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.FINISHED, message);
						notifyObservers(pushPullEvent);
					}
				}

			}

			/**
			 * Pull the changes and inform the user with messages depending on the
			 * result status
			 * 
			 * @param userCredentials
			 *          - credentials used to push the changes
			 * @throws WrongRepositoryStateException
			 * @throws InvalidConfigurationException
			 * @throws DetachedHeadException
			 * @throws InvalidRemoteException
			 * @throws CanceledException
			 * @throws RefNotFoundException
			 * @throws RefNotAdvertisedException
			 * @throws NoHeadException
			 * @throws TransportException
			 * @throws GitAPIException
			 * @throws AmbiguousObjectException
			 * @throws IncorrectObjectTypeException
			 * @throws IOException
			 */
			private String pull(final UserCredentials userCredentials)
					throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
					InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException,
					TransportException, GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
				PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
				String message = "";
				if (PullStatus.OK == response.getStatus()) {
					// JOptionPane.showMessageDialog((Component)
					// PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
					// "Pull successful");
					message = StatusMessages.PULL_SUCCESSFUL;
				} else if (PullStatus.UNCOMITED_FILES == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Cannot pull with uncommited changes");

				} else if (PullStatus.CONFLICTS == response.getStatus()) {
					// prompts a dialog showing the files in conflict
					new PullWithConflictsDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
							"Information", true, response.getConflictingFiles());

				} else if (PullStatus.UP_TO_DATE == response.getStatus()) {
					// ((StandalonePluginWorkspace)
					// PluginWorkspaceProvider.getPluginWorkspace())
					// .showInformationMessage("Repository is already up to date");
					message = StatusMessages.PULL_UP_TO_DATE;
				} else if (PullStatus.REPOSITORY_HAS_CONFLICTS == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage(StatusMessages.PULL_WITH_CONFLICTS);
				}
				return message;
			}

			/**
			 * Pushes the changes and inform the user user depending on the result
			 * status
			 * 
			 * @param userCredentials
			 *          - credentials used to pull the changes
			 * @throws InvalidRemoteException
			 * @throws TransportException
			 * @throws GitAPIException
			 * @throws IOException
			 */
			private String push(final UserCredentials userCredentials)
					throws InvalidRemoteException, TransportException, GitAPIException, IOException {
				PushResponse response = gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
				String message = "";
				if (Status.OK == response.getStatus()) {
					// ((StandalonePluginWorkspace)
					// PluginWorkspaceProvider.getPluginWorkspace())
					// .showInformationMessage("Push successful");
					message = StatusMessages.PUSH_SUCCESSFUL;
				} else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Push failed, please get your repository up to date(PULL)");
				} else if (Status.UP_TO_DATE == response.getStatus()) {
					// ((StandalonePluginWorkspace)
					// PluginWorkspaceProvider.getPluginWorkspace())
					// .showInformationMessage("There was nothing to push");.
					message = StatusMessages.PUSH_UP_TO_DATE;
				} else if (Status.REJECTED_OTHER_REASON == response.getStatus()) {
					// message = response.getMessage();
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage(response.getMessage());
				}
				return message;
			}
		}).start();
	}

	/**
	 * Notifies the observer to update it's state with the given Event fired from
	 * a Push or Pull action
	 * 
	 * @param pushPullEvent
	 *          - the Event fired
	 */
	private void notifyObservers(PushPullEvent pushPullEvent) {
		observer.stateChanged(pushPullEvent);
	}

	public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	public void removeObserver(Observer<PushPullEvent> obj) {
		observer = null;
	}

}
