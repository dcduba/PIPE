package pipe.actions.gui;

import pipe.views.PipeApplicationView;
import pipe.controllers.application.PipeApplicationController;
import pipe.utilities.gui.GuiUtils;

import java.io.File;
import java.io.IOException;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Exports the Petri net as a PNG
 */
public class ExportPNGAction extends AbstractSaveAction {
    /**
     * Main PIPE application controller
     */
    private final PipeApplicationController applicationController;

    /**
     * Constructor
     * Sets short cut to ctrl G
     */
    public ExportPNGAction(PipeApplicationController applicationController, PipeApplicationView view) {
        super("PNG", "Export the net to PNG format", KeyEvent.VK_G, InputEvent.META_DOWN_MASK, applicationController, view);
        this.applicationController = applicationController;
    }

    /**
     * Saves the Petri net as a PNG when selected.
     *
     * Currently this feature has not been implemented.
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        BufferedImage bfi = applicationController.getActivePetriNetController().getPetriNetTab().exportAsImage();
        FileDialog fileDialog = getFileDialog("Save net as PNG", "png");
        File file = saveAsFile(fileDialog, "png");
        if(file != null) {
        	try {
        		ImageIO.write(bfi, "png", file);
        	} catch( IOException error ) {
        		GuiUtils.displayErrorMessage(null, error.getMessage());
        	}
        }
        //else cancelled
    }
}
