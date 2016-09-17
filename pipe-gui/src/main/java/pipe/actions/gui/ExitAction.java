package pipe.actions.gui;

import pipe.controllers.application.PipeApplicationController;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

public class ExitAction extends GuiAction {

    private final PipeApplicationController pipeApplicationController;

    private Frame application;

    public ExitAction(Frame application, PipeApplicationController pipeApplicationController) {
        super("Exit", "Close the program", KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        this.application = application;
        this.pipeApplicationController = pipeApplicationController;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tryToExit();
    }

    public void tryToExit() {
        boolean safeToExit = !pipeApplicationController.anyNetsChanged();
        tryToExit(safeToExit);
    }

    /**
     * Tries to exit. If it is not safe to immediately exit then a warning comes up
     * asking the user if they wish to exit. The result is then published and the relevant action
     * is performed (e.g. exit or cancel).
     *
     * @param safeExit boolean determines if it safe to quit immediately
     */
    private void tryToExit(boolean safeExit) {
        if (safeExit) {
            application.dispose();
            System.exit(0);
        } else {
        	Object[] options = {"Save and exit", "Don't save and exit", "Cancel"};
        	int result = JOptionPane.showOptionDialog(application,
        			"Would you like to save your unsaved work before exiting?",
        			"Save before exit?", 
        			JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, null);
        	switch (result) {
        	case 0:
        		try {
        			if(pipeApplicationController.saveUnsavedNets()) {
        				tryToExit(true);
        			}
        		} catch(ParserConfigurationException | TransformerException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    JOptionPane.showMessageDialog(null, "Fatal error while saving.", "Fatal error",
                            JOptionPane.ERROR_MESSAGE);
        		}
        		break;
        	case 1:
        		tryToExit(true);
        		break;
        	case 2:
        	default:
        		break;
        	}
        }
    }

    public String changedNamesMessage(Iterable<String> changedNames) {
        StringBuilder buffer = new StringBuilder("The following Petri nets have changed. Do you still want to exit?");
        for (String name : changedNames) {
            buffer.append("\n");
            buffer.append(name);
        }
        return buffer.toString();
    }
}
