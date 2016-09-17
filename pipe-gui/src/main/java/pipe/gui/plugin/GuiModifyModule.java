package pipe.gui.plugin;

import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * API for GUI modules
 */
public interface GuiModifyModule extends GuiModule {
    /**
     * Starts a module using an optional Petri net and the event listener
     * @param petriNet
     * @param changeSupport used to communicate back with controllers
     */
    void start(PetriNet petriNet, PropertyChangeSupport changeSupport);
}
