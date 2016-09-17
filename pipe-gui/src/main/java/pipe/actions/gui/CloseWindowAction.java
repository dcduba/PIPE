package pipe.actions.gui;

import pipe.controllers.application.PipeApplicationController;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * Action that closes the currently displayed tab
 */
public class CloseWindowAction extends GuiAction {

    /**
     * Application controller
     */
    private final PipeApplicationController applicationController;

    /**
     * Constructor
     * @param applicationController PIPE main application controller
     */
    public CloseWindowAction(PipeApplicationController applicationController) {
        super("Close", "Close the current tab", KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
        this.applicationController = applicationController;
    }

    /**
     * On performing this action the currently displayed tab will be closed.
     *
     * If there have been modifications to the Petri net in the current tab then a confirm
     * dialog is shown asking the user if they really wish to close.
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!applicationController.hasCurrentPetriNetChanged()) {
            applicationController.removeActiveTab();
        } else {
        	Object[] options = {"Save and close tab", "Don't save and close tab", "Cancel"};
        	int result = JOptionPane.showOptionDialog((Frame) null,
        			"Would you like to save your unsaved work before closing this tab?",
        			"Save before closing?", 
        			JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, null);
        	switch (result) {
        	case 0:
        		try {
        			if(applicationController.saveCurrentNet()) {
        				applicationController.removeActiveTab();
        			}
        		} catch(ParserConfigurationException | TransformerException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ee) {
                    JOptionPane.showMessageDialog(null, "Fatal error while saving.", "Fatal error",
                            JOptionPane.ERROR_MESSAGE);
        		}
        		break;
        	case 1:
        		applicationController.removeActiveTab();
        		break;
        	case 2:
        	default:
        		break;
        	}
        }
    }
}
