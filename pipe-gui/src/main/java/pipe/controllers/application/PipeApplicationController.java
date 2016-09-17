package pipe.controllers.application;

import pipe.actions.gui.PipeApplicationModel;
import pipe.controllers.*;
import pipe.gui.PetriNetTab;
import pipe.historyActions.AnimationHistoryImpl;
import uk.ac.imperial.pipe.animation.PetriNetAnimator;
import uk.ac.imperial.pipe.models.manager.PetriNetManager;
import uk.ac.imperial.pipe.models.manager.PetriNetManagerImpl;
import uk.ac.imperial.pipe.models.petrinet.*;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.pipe.models.petrinet.name.*;

import javax.swing.event.UndoableEditListener;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.awt.FileDialog;
import java.awt.Dialog;

/**
 * Pipes main application controller.
 * It houses the Petri net controllers of open tabs and is responsible for the creation of Petri nets
 */
public class PipeApplicationController {

    /**
     * Controllers for each tab
     */
    private final Map<PetriNetTab, PetriNetController> netControllers = new HashMap<>();

    /**
     * Main PIPE application model
     */
    private final PipeApplicationModel applicationModel;

    /**
     * Manages creation/deletion of Petri net models
     */
    private final PetriNetManager manager = new PetriNetManagerImpl();

    /**
     * The current tab displayed in the view
     */
    private PetriNetTab activeTab;

    /**
     * Constructor
     * @param applicationModel Main PIPE application model
     */
    public PipeApplicationController(PipeApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    /**
     *
     * @param listener to listen for change events in the petri net manager
     */
    public void registerToManager(PropertyChangeListener listener) {
        manager.addPropertyChangeListener(listener);
    }

    /**
     * Creates an empty Petri net with a default token
     */
    public void createEmptyPetriNet() {
        manager.createNewPetriNet();
    }


    /**
     * Register the tab to the Petri net
     * @param net Petri net
     * @param tab tab which houses the graphical petri net components
     * @param historyObserver listener for stepback/forward events in animation
     * @param undoListener listener for undo/redo events
     * @param zoomListener listener for zoom events
     */
    //TODO: THIS IS RATHER UGLY, too many params but better than what was here before
    public void registerTab(PetriNet net, PetriNetTab tab, Observer historyObserver, UndoableEditListener undoListener,
                            PropertyChangeListener zoomListener) {
        AnimationHistoryImpl animationHistory = new AnimationHistoryImpl();
        animationHistory.addObserver(historyObserver);
        GUIAnimator animator = new GUIAnimator(new PetriNetAnimator(net), animationHistory, this);

        CopyPasteManager copyPasteManager = new CopyPasteManager(undoListener, tab, net, this);

        ZoomController zoomController = new ZoomController(100);
        tab.addZoomListener(zoomController);
        PetriNetController petriNetController =
                new PetriNetController(net, undoListener, animator, copyPasteManager, zoomController, tab);
        netControllers.put(tab, petriNetController);
        tab.updatePreferredSize();

        PropertyChangeListener changeListener =
                new PetriNetChangeListener(applicationModel, tab, petriNetController);
        net.addPropertyChangeListener(changeListener);

        setActiveTab(tab);
        initialiseNet(net, changeListener);
    }

    /**
     *
     * @param tab the active tab - this is the tab that is currently being displayed in the view
     */
    public void setActiveTab(PetriNetTab tab) {
        this.activeTab = tab;
    }
    
    /**
     * This is a little hacky, I'm not sure how to make this better when it's so late
     * If a better implementation is clear please re-write
     * <p/>
     * This method invokes the change listener which will create the view objects on the
     * petri net tab
     *
     * @param propertyChangeListener
     */
    private void initialiseNet(PetriNet net, PropertyChangeListener propertyChangeListener) {
        for (Token token : net.getTokens()) {
            PropertyChangeEvent changeEvent =
                    new PropertyChangeEvent(net, PetriNet.NEW_TOKEN_CHANGE_MESSAGE, null, token);
            propertyChangeListener.propertyChange(changeEvent);
        }

        for (Place place : net.getPlaces()) {
            PropertyChangeEvent changeEvent =
                    new PropertyChangeEvent(net, PetriNet.NEW_PLACE_CHANGE_MESSAGE, null, place);
            propertyChangeListener.propertyChange(changeEvent);
        }

        for (Transition transition : net.getTransitions()) {
            PropertyChangeEvent changeEvent =
                    new PropertyChangeEvent(net, PetriNet.NEW_TRANSITION_CHANGE_MESSAGE, null, transition);
            propertyChangeListener.propertyChange(changeEvent);
        }

        for (Arc<? extends Connectable, ? extends Connectable> arc : net.getArcs()) {
            PropertyChangeEvent changeEvent = new PropertyChangeEvent(net, PetriNet.NEW_ARC_CHANGE_MESSAGE, null, arc);
            propertyChangeListener.propertyChange(changeEvent);
        }

        for (Annotation annotation : net.getAnnotations()) {
            PropertyChangeEvent changeEvent =
                    new PropertyChangeEvent(net, PetriNet.NEW_ANNOTATION_CHANGE_MESSAGE, null, annotation);
            propertyChangeListener.propertyChange(changeEvent);
        }

        for (RateParameter rateParameter : net.getRateParameters()) {
            PropertyChangeEvent changeEvent =
                    new PropertyChangeEvent(net, PetriNet.NEW_RATE_PARAMETER_CHANGE_MESSAGE, null, rateParameter);
            propertyChangeListener.propertyChange(changeEvent);
        }
    }

    /**
     * Loads and creates a Petri net located at the given file
     * @param file location of the XML file which contains a PNML representation of a Petri net
     * @throws UnparsableException
     */
    public void createNewTabFromFile(File file) throws UnparsableException {
        try {
            manager.createFromFile(file);
        } catch (FileNotFoundException | JAXBException e) {
            throw new UnparsableException("Could not initialise Petri net reader!", e);
        }
    }
    
    public void createNewTabFromPetrinet(PetriNet petriNet) {
    	manager.createFromPetrinet(petriNet);
    }

    /**
     * Save the currently displayed petri net to the specified file
     * @param outFile location to save the Petri net
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public void saveAsCurrentPetriNet(File outFile)
            throws ParserConfigurationException, TransformerException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        PetriNetController petriNetController = getActivePetriNetController();
        PetriNet petriNet = petriNetController.getPetriNet();

        try {
            manager.savePetriNet(petriNet, outFile);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException("Failed to write!", e);
        }
        petriNetController.save();
    }

    /**
     *
     * @return the active Petri net controller
     */
    public PetriNetController getActivePetriNetController() {
        return netControllers.get(activeTab);
    }

    /**
     * @return true if the current petri net has changed
     */
    public boolean hasCurrentPetriNetChanged() {
        PetriNetController activeController = getActivePetriNetController();
        return activeController != null && activeController.hasChanged();
    }

    public boolean anyNetsChanged() {
        return !getNetsChanged().isEmpty();
    }

    /**
     * @return the names of the petri nets that have changed
     */
    public Set<String> getNetsChanged() {
        Set<String> changed = new HashSet<>();
        for (PetriNetController controller : netControllers.values()) {
            if (controller.hasChanged()) {
                changed.add(controller.getPetriNet().getNameValue());
            }
        }
        return changed;
    }
    
    /**
     * Determines which tabs have not yet been saved, and issues a save for each tab that has changes.
     * A file dialog is shown if the tab has never been saved before, and otherwise it will directly save
     * to the last file that tab was saved to.
     * 
     * @return
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public boolean saveUnsavedNets()
    throws ParserConfigurationException, TransformerException, IllegalAccessException, NoSuchMethodException,
    InvocationTargetException {
        for (Map.Entry<PetriNetTab, PetriNetController> entry : netControllers.entrySet()) {
        	if(entry.getValue().hasChanged()) {
        		//Give visual feedback which net we are saving
        		setActiveTab(entry.getKey());
        		
        		if(!saveCurrentNet())
        			return false;
        	}
        }
        return true;
    }
    
    public boolean saveCurrentNet()
    throws ParserConfigurationException, TransformerException, IllegalAccessException, NoSuchMethodException,
    	    InvocationTargetException {
    	File file;
    	
		file = getFileToSaveTo();
		if(file == null)
			return false;
		
		saveAsCurrentPetriNet(file);
		return true;
    }
    
    /**
     * Gets file reference to which we should save. Might need to be moved to PetriNetController.
     * @return File reference, or null if action was canceled
     */
    private File getFileToSaveTo() {
        FileDialog fileDialog;
    	PetriNetName petriNetName = getActivePetriNetController().getPetriNet().getName();
    	String filename;
    	File file;
    	
    	if(petriNetName.getClass() == NormalPetriNetName.class) {
			//Net has not been saved yet
			fileDialog = new FileDialog((Dialog) null, "Save Petri Net", FileDialog.SAVE);
			fileDialog.setFile("*.xml");
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
			fileDialog.setVisible(true);
			filename = fileDialog.getFile();
			if(filename == null) {
				//Canceled save
				return null;
			}
			if(!filename.endsWith(".xml")) {
				filename += ".xml";
			}
			file = new File(fileDialog.getDirectory() + filename);
		} else {
			//Net has already been saved once
			file = ((PetriNetFileName) petriNetName).getFile();
		}
    	
    	return file;
    }
    
    /**
     * Removes the active tab from display if it exists.
     * Note active tab must be removed from netControllers before the petri net is removed
     * from the manager because the manager will fire a message which causes the active tab
     * to be swapped to the new open tab
     */
    public void removeActiveTab() {
        if (activeTab != null) {
            PetriNetController controller = netControllers.get(activeTab);
            netControllers.remove(activeTab);
            PetriNet petriNet = controller.getPetriNet();
            manager.remove(petriNet);
        }
    }

    /**
     *
     * @return the current active tab
     */
    public PetriNetTab getActiveTab() {
        return activeTab;
    }
}
