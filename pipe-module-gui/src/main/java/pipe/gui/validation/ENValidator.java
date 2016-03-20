package pipe.gui.validation;

import org.apache.commons.collections.CollectionUtils;

//import javafx.animation.Transition;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.*;

import java.io.File;
import javax.swing.*;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Performs the exploration and steady state analysis of a Petri net.
 * Displays useful performance analysis metrics
 */
public class ENValidator {
    private static final String PLACE_INTRO = "## Places ##\n";
    private static final String TRANS_INTRO = "## Transitions ##\nTransitions in EN systems are immediate, have no priority and are single servers.\n";
    private static final String ARC_INTRO = "## Arcs ##\nArcs in EN systems can not have a weight (other than 1). Self-loops are not allowed, because in an EN system a transition is only enabled if all output places do not contain a token, while in P/T systems a transition is enabled if the resulting number of tokens in output places does not exceed the capacity.\n";
    
    private JPanel mainPanel;
    
    private JLabel validationIcon;
    
    private JCheckBox capacityCheck;
    
    private JCheckBox tokenCheck;
    
    private JCheckBox simplicityCheck;
    
    private JCheckBox selfloopCheck;
    
    private JCheckBox arcWeightCheck;
    
    private JLabel informationLabel;
    
    private JButton convertButton;
    
    private JButton refreshButton;
    
    private JTextArea resultPanel;
    
    private PetriNet net;
    
    /**
     * Sets up the UI
     */
    private void setUp() {
    	ImageIcon icon = new ImageIcon(getImageURL("refresh.png"));
    	refreshButton.setIcon(icon);
    	
    	//Select correct checkboxes upon opening form
       	validateNet();
    	
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertNet();
                
               	validateNet();
            }
        });
        
        refreshButton.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
      			validateNet();
        	}
        });
    }
    
    private URL getImageURL(String name) {
        return this.getClass().getResource("/" + "images" + File.separator + name);
    }
    
    private void convertNet() {
    	Collection<Place> places = net.getPlaces();
    	Collection<Transition> transitions = net.getTransitions();
    	Collection<InboundArc> inboundArcs = net.getInboundArcs();
    	Collection<OutboundArc> outboundArcs = net.getOutboundArcs();
    	
    	for(Place place : places) {
    		if(place.getNumberOfTokensStored() > 1) {
    			//Working on a copy, so the HashMap does not change while changing the actual tokens
        		for(Map.Entry<String,Integer> entry : new HashMap<String,Integer>(place.getTokenCounts()).entrySet()) {
      				place.removeAllTokens(entry.getKey());
        		}
        		place.setTokenCount("Default", 1);
    		}
    		place.setCapacity(1);
    	}
    	
    	for(Transition transition : transitions) {
    		transition.setPriority(1);
    		transition.setInfiniteServer(false);
    		transition.setTimed(false);
    	}
    	
    	for(InboundArc inboundArc : inboundArcs) {
    		for(Map.Entry<String,String> entry : inboundArc.getTokenWeights().entrySet()) {
    			inboundArc.setWeight(entry.getKey(), "");
    		}
			inboundArc.setWeight("Default", "1");
    	}
    	for(OutboundArc outboundArc : outboundArcs) {
    		for(Map.Entry<String,String> entry : outboundArc.getTokenWeights().entrySet()) {
    			outboundArc.setWeight(entry.getKey(), "");
    		}
    		outboundArc.setWeight("Default", "1");
    	}
    }
    
    private void validateNet() {
    	Collection<Place> places = net.getPlaces();
    	Collection<Transition> transitions = net.getTransitions();
    	Collection<InboundArc> inboundArcs = net.getInboundArcs();
    	Collection<OutboundArc> outboundArcs = net.getOutboundArcs();
    	ImageIcon icon;
    	
    	boolean hasENCapacity = true;
    	boolean hasENTokens = true;
    	boolean isSimpleNet = true;
    	boolean hasSelfloops = false;
    	boolean hasArcWeights = false;
    	
        String validationResult = "";
        
    	/* An EN system consists of only simple places and transitions, where places
    	 * have a capacity of 1, and at most 1 token. A P/T system with these properties
    	 * only behaves as an EN system if it contains no selfloops.
    	 */
        validationResult += PLACE_INTRO;
    	for(Place place : places) {
    		if(!place.hasCapacityRestriction() || place.getCapacity() > 1) {
    			validationResult += "- Place " + place.getName() + " has " + (place.hasCapacityRestriction() ? ("a capacity of " + place.getCapacity()) : "an infinite capacity") + " and should have capacity 1.\n";
    			hasENCapacity = false;
    		}
    		if(place.getNumberOfTokensStored() > 1) {
    			validationResult += "- Place " + place.getName() + " has " + place.getNumberOfTokensStored() + " tokens and should have at most 1 token.\n";
    			hasENTokens = false;
    		}
    	}
    	if(hasENCapacity && hasENTokens) {
    		validationResult += "- None\n";
    	}
    	
    	validationResult += "\n" + TRANS_INTRO;
    	for(Transition transition : transitions) {
    		if(transition.isTimed() || transition.getPriority() != 1 || transition.getRate().getRateType() != RateType.NORMAL_RATE || transition.isInfiniteServer()) {
    			validationResult += "- Transition " + transition.getName() + " is not a \"simple\" transition.\n";
    			isSimpleNet = false;
    		}
    	}
    	if(isSimpleNet) {
    		validationResult += "- None\n";
    	}
    	
    	validationResult += "\n" + ARC_INTRO;
    	for(InboundArc inboundArc : inboundArcs) {
    		Place start = inboundArc.getSource();
    		Transition middle = inboundArc.getTarget();
    		
    		for(OutboundArc outboundArc : outboundArcs) {
    			if(outboundArc.getSource() == middle && outboundArc.getTarget() == start) {
    				validationResult += "- Place " + start.getName() + " loops to itself via transition " + middle.getName() + ".\n";
    				hasSelfloops = true;
    			}
    		}
    	}
    	for(InboundArc inboundArc : inboundArcs) {
    		Place start = inboundArc.getSource();
    		Transition end = inboundArc.getTarget();
    		for(Map.Entry<String,String> entry : inboundArc.getTokenWeights().entrySet()) {
    			if(entry.getKey().equals("Default") && !entry.getValue().equals("1")) {
    				validationResult += "- The arc between " + start.getName() + " and " + end.getName() + " has weight \"" + entry.getValue() + "\" while a weight of \"1\" is expected.\n";
    				hasArcWeights = true;
    			} else if(!entry.getKey().equals("Default") && !entry.getValue().equals("") && !entry.getValue().equals("0")) {
    				validationResult += "- The arc between " + start.getName() + " and " + end.getName() + " has a weight for tokens of type " + entry.getKey() + " while non is expected.\n";
    				hasArcWeights = true;
    			}
    		}
    	}
    	for(OutboundArc outboundArc : outboundArcs) {
    		Transition start = outboundArc.getSource();
    		Place end = outboundArc.getTarget();
    		for(Map.Entry<String,String> entry : outboundArc.getTokenWeights().entrySet()) {
    			if(entry.getKey().equals("Default") && !entry.getValue().equals("1")) {
    				validationResult += "- The arc between " + start.getName() + " and " + end.getName() + " has weight \"" + entry.getValue() + "\" while a weight of \"1\" is expected.\n";
    				hasArcWeights = true;
    			} else if(!entry.getKey().equals("Default") && !entry.getValue().equals("") && !entry.getValue().equals("0")) {
    				validationResult += "- The arc between " + start.getName() + " and " + end.getName() + " has a weight for tokens of type " + entry.getKey() + " while non is expected.\n";
    				hasArcWeights = true;
    			}
    		}
    	}
    	if(!hasSelfloops && !hasArcWeights) {
    		validationResult += "- None\n";
    	}
    	
    	//The checkboxes give easy confirmation which conditions are not met
    	capacityCheck.setSelected(hasENCapacity);
    	tokenCheck.setSelected(hasENTokens);
    	simplicityCheck.setSelected(isSimpleNet);
    	selfloopCheck.setSelected(!hasSelfloops);
    	arcWeightCheck.setSelected(!hasArcWeights);

    	//We cannot convert if it contains selfloops, because we do not know how the user wants to solve this
    	if(hasSelfloops) {
    		convertButton.setEnabled(false);
    		convertButton.setToolTipText("Please remove all selfloops before converting to an EN system");
    	} else if(hasENCapacity && hasENTokens && isSimpleNet && !hasSelfloops && !hasArcWeights) {
    		convertButton.setEnabled(false);
    		convertButton.setToolTipText("System is already an EN system");
    	} else {
    		convertButton.setEnabled(true);
    		convertButton.setToolTipText("Click to resolve all problems highlighted above");
    	}
    	
    	//Easy confirmation if system is actually EN system via large icon with checkmark/cross
    	validationIcon.setText(null);
    	if(hasENCapacity && hasENTokens && isSimpleNet && !hasSelfloops && !hasArcWeights) {
    		icon = new ImageIcon(getImageURL("envalid.png"));
    	} else {
    		icon = new ImageIcon(getImageURL("eninvalid.png"));
    	}
    	validationIcon.setIcon(icon);
    	
        resultPanel.setText(validationResult);
    }

    public ENValidator(PetriNet petriNet, FileDialog fileDialog) {
        this.net = petriNet;
        setUp();
    }

    /**
     * Main method for running this externally without PIPE
     *
     * @param args          command line arguments
     */
    public static void main(String[] args) {
        //Makes no sense to run alone
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
