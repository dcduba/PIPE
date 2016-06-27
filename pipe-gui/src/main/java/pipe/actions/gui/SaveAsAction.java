package pipe.actions.gui;

import pipe.controllers.application.PipeApplicationController;
import pipe.views.PipeApplicationView;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Performs a save as action
 */
public class SaveAsAction extends AbstractSaveAction {

    /**
     * Constructor
     * @param pipeApplicationController PIPE main application controller.
     * @param fileChooser file dialog for retrieving a path for a saved Petri net.
     */
    public SaveAsAction(PipeApplicationController pipeApplicationController, PipeApplicationView view) {
        super("Save as", "Save as...", KeyEvent.VK_S, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                pipeApplicationController, view);
    }

    /**
     * Performs a save as operation, opening the file dialog and saving the Petri net
     * to the specified location.
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        saveAsNet();
    }
}
