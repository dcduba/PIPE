package pipe.gui.plugin;

import uk.ac.imperial.pipe.models.petrinet.PetriNet;

/**
 * API for GUI modules
 */
public interface GuiAnalysisModule extends GuiModule {

    /**
     * Start a module using optionally the current Petri net
     * @param petriNet
     */
    void start(PetriNet petriNet);
}
