package pipe.gui.plugin.concrete;

import pipe.gui.process.ProcessGenerator;
import pipe.gui.plugin.GuiModifyModule;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

import javax.swing.*;
import java.awt.FileDialog;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ProcessGeneratorModule implements GuiModifyModule {
    /**
     * Starts the Process Generator Module
     * @param petriNet current Petri net to use
     */
    @Override
    public void start(PetriNet petriNet, PropertyChangeSupport changeSupport) {
        JFrame frame = new JFrame("Process Generator");
        FileDialog selector = new FileDialog(frame, "Select petri net", FileDialog.LOAD);
        
        ProcessGenerator generator = new ProcessGenerator(petriNet, selector, changeSupport);

        frame.setContentPane(generator.getMainPanel());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     *
     * @return Process Generator
     */
    @Override
    public String getName() {
        return "Process Generator";
    }
}
