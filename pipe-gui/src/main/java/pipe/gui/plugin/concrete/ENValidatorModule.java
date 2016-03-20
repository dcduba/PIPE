package pipe.gui.plugin.concrete;

import pipe.gui.validation.ENValidator;
import pipe.gui.plugin.GuiModule;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

import javax.swing.*;
import java.awt.FileDialog;

public class ENValidatorModule implements GuiModule {
    /**
     * Starts the GSPN analysis module
     * @param petriNet current Petri net to use
     */
    @Override
    public void start(PetriNet petriNet) {
        JFrame frame = new JFrame("EN Validator and Converter");
        FileDialog selector = new FileDialog(frame, "Select petri net", FileDialog.LOAD);
        
        ENValidator validator = new ENValidator(petriNet, selector);

        frame.setContentPane(validator.getMainPanel());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     *
     * @return EN Validator and Converter
     */
    @Override
    public String getName() {
        return "EN Validator and Converter";
    }
}
