package pipe.actions.gui;

import org.apache.commons.io.FilenameUtils;
import pipe.controllers.application.PipeApplicationController;
import pipe.utilities.gui.GuiUtils;
import pipe.views.PipeApplicationView;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract class for implementing save and save as actions
 */
public abstract class AbstractSaveAction extends GuiAction {
    /**
     * The controller for the entire application
     */
    protected final PipeApplicationController pipeApplicationController;

    /**
     * The file dialog that will pop up when saving a Petri net. It is used to define a file save location.
     */
    private final PipeApplicationView view;
    
    /**
     * For testing only!
     * 
     * This variable only exists so the code in this class can be tested without mocking the class itself
     */
    private FileDialog testingFileDialog;

    /**
     * Constructor for a save action
     * @param name graphical name representation
     * @param tooltip string that will appear as a tool tip when hovering over the action on a toolbar
     * @param key key shortcut
     * @param modifiers key shortcut modifiers
     * @param pipeApplicationController controller for the entire application
     * @param fileDialog file dialog used to select files on save
     */
    public AbstractSaveAction(String name, String tooltip, int key, int modifiers,
                              PipeApplicationController pipeApplicationController, PipeApplicationView view) {
        super(name, tooltip, key, modifiers);
        this.pipeApplicationController = pipeApplicationController;
        this.view = view;
        this.testingFileDialog = null;
    }

    
    protected final File saveAsFile(FileDialog fileDialog, String extension) {
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        File file = null;
        if (files.length != 0) {
            file = files[0];
            file = ensureExtension(file, extension);
        }
        
        return file;
    }
    
    
    /**
     * Performs a save as operation. A save as chooses a file to save the Petri net to
     */
    protected final void saveAsNet() {
    	FileDialog fileDialog = getFileDialog("Save Petri net", "xml");
    	File file = saveAsFile(fileDialog, "xml");
    	saveNet(file);
    }

    /**
     * Performs a save operation. This action saves the Petri net to the specified file without
     * popping up the file dialog
     *
     * @param file file path to save petri net to
     */
    protected final void saveNet(File file) {
        try {
            pipeApplicationController.saveAsCurrentPetriNet(file);
        } catch (ParserConfigurationException | TransformerException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            GuiUtils.displayErrorMessage(null, e.getMessage());
        }
    }
    
    protected final FileDialog buildFileDialog(String title) {
    	if(this.testingFileDialog != null) {
    		return this.testingFileDialog;
    	} else {
    		return new FileDialog(view, title, FileDialog.SAVE);
    	} 	
    }
    
    protected final FileDialog getFileDialog(String title, final String extension) {
        FileDialog fileDialog = buildFileDialog(title);
        fileDialog.setFile("*." + extension);
        fileDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("." + extension);
            }
        });

        return fileDialog;
    }
    
    /*
     * For unit tests only! The Deprecated tag is just so it shows up marked in IDE's.
     */
    @Deprecated
    public void setFileDialog(FileDialog fileDialog) {
    	this.testingFileDialog = fileDialog;
    }

    /**
     * Append required extension onto file if it is missing. This happens if a user types
     * the name of the file they would like to save the Petri net to but does not add
     * the file extension to it.
     *
     * @param file file to add the xml extension to if it is missing
     * @return file with extension
     */
    private File ensureExtension(File file, String wantedExtension) {
        String ext = FilenameUtils.getExtension(file.getAbsolutePath());
        if (ext.isEmpty() || !ext.toLowerCase().endsWith(wantedExtension)) {
            return new File(file.getAbsolutePath() + "." + wantedExtension);
        }
        return file;
    }
}
