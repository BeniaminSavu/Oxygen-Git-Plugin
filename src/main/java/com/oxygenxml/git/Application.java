package com.oxygenxml.git;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.CommitPanel;
import com.oxygenxml.git.view.GitWindow;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.ToolbarPanel;
import com.oxygenxml.git.view.UnstagedChangesPanel;
import com.oxygenxml.git.view.WorkingCopySelectionPanel;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.StageController;

public class Application {

	private StagingPanel stagingPanel;

	public static void main(String[] args) {
		//new Application().start();
	}

	public void start() {

		/*try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}*/

		createAndShowFrame();
	}

	private void createAndShowFrame() {
		

		//stagingPanel.createGUI();

		this.stagingPanel = stagingPanel;
		GitWindow gitWindow = new GitWindow(stagingPanel);

		
		gitWindow.createGUI();
	}

	public JPanel getGitWindow() {
		return stagingPanel;
	}

}