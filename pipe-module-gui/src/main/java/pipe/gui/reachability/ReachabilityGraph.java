package pipe.gui.reachability;

import net.sourceforge.jpowergraph.Edge;
import net.sourceforge.jpowergraph.Node;
import net.sourceforge.jpowergraph.defaults.DefaultGraph;
import net.sourceforge.jpowergraph.layout.Layouter;
import net.sourceforge.jpowergraph.layout.spring.SpringLayoutStrategy;
import net.sourceforge.jpowergraph.lens.*;
import net.sourceforge.jpowergraph.manipulator.dragging.DraggingManipulator;
import net.sourceforge.jpowergraph.manipulator.popup.PopupManipulator;
import net.sourceforge.jpowergraph.swing.SwingJGraphPane;
import net.sourceforge.jpowergraph.swing.SwingJGraphScrollPane;
import net.sourceforge.jpowergraph.swing.manipulator.SwingPopupDisplayer;
import net.sourceforge.jpowergraph.swtswinginteraction.color.JPowerGraphColor;
import pipe.gui.widget.GenerateResultsForm;
import pipe.gui.widget.StateSpaceLoader;
import pipe.gui.widget.StateSpaceLoaderException;
import pipe.reachability.algorithm.*;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;
import uk.ac.imperial.pipe.models.petrinet.Place;
import uk.ac.imperial.utils.Pair;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI class used to display and run the results of reachability and coverability classes
 */
public class ReachabilityGraph {

    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(ReachabilityGraph.class.getName());

    /**
     * Maximum number of states to graphically display
     */
    private static final int MAX_STATES_TO_DISPLAY = 100;


    private JPanel panel1;

    /**
     * Contains the graph based results of state space exploration
     */
    private JPanel resultsPanel;

    /**
     * Check box to determine if we include vanishing states in the exploration
     */
    private JCheckBox includeVanishingStatesCheckBox;

    /**
     * For saving state space results
     */
    private JButton saveButton;

    private JLabel textResultsLabel;
    private JPanel textResultsPanel;


    private JRadioButton reachabilityButton;

    private JRadioButton coverabilityButton;

    private JTextField maxStatesField;

    private JPanel stateLoadingPanel;

    private JPanel generatePanel;

    private DefaultGraph graph = new DefaultGraph();

    private StateSpaceLoader stateSpaceLoader;
    
    /**
     * Asks user to select a petrinet. "use current Petri net" can be used to use current petrinet
     *
     * @param loadDialog the dialog to be shown
     * @param petriNet   current petri net
     */
    public ReachabilityGraph(FileDialog loadDialog, PetriNet petriNet) {
    	stateSpaceLoader = new StateSpaceLoader(petriNet, loadDialog);
        setUp();
    }
    
    /**
     * Calculates the maximum capacity of all places in this petriNet
     * 
     * @return int 0 if infinite, otherwise maximum capacity
     */
    private int getMaxCapacity() {
    	int maxCapacity = 1;
    	PetriNet petriNet = stateSpaceLoader.getPetriNet();
    	
    	for(Place place : petriNet.getPlaces()) {
    		if(!place.hasCapacityRestriction()) {
    			//There is nothing larger than infinity
    			return 0;
    		} else if(place.getCapacity() > maxCapacity) {
    			maxCapacity = place.getCapacity();
    		}
    	}
    	return maxCapacity;
    }
    
    /**
     * 
     * @return return true if all places have infinite capacity, and false if at least 1 has a limited capacity
     */
    private boolean hasOnlyInfiniteCapacity() {
    	PetriNet petriNet = stateSpaceLoader.getPetriNet();
    	
    	for(Place place : petriNet.getPlaces()) {
    		if(place.hasCapacityRestriction()) {
    			return false;
    		}
    	}
    	return true;
    }

    /**
     * Set up action listeners
     */
    private void setUp() {
        JPanel pane = setupGraph();
        resultsPanel.add(pane);
        stateLoadingPanel.add(stateSpaceLoader.getMainPanel(), 0);

        ActionListener disableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reachabilityButton.setEnabled(false);
                coverabilityButton.setEnabled(false);
                includeVanishingStatesCheckBox.setEnabled(false);
            }
        };

        ActionListener enableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reachabilityButton.setEnabled(true);
                coverabilityButton.setEnabled(true);
                includeVanishingStatesCheckBox.setEnabled(true);
            }
        };
//
        stateSpaceLoader.addPetriNetRadioListener(enableListener);
        stateSpaceLoader.addBinariesListener(disableListener);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveBinaryFiles();
            }
        });
        GenerateResultsForm resultsForm = new GenerateResultsForm(new GenerateResultsForm.GoAction() {
            @Override
            public void go(int threads) {
                calculateResults(threads);
            }
        });
        generatePanel.add(resultsForm.getPanel());
    }

    /**
     * Sets up the graph and returns the JPanel to add to
     * the resultsPanel
     */
    private JPanel setupGraph() {
        SwingJGraphPane pane = new SwingJGraphPane(graph);
        LensSet lensSet = new LensSet();
        lensSet.addLens(new RotateLens());
        lensSet.addLens(new TranslateLens());
        lensSet.addLens(new ZoomLens());
        CursorLens draggingLens = new CursorLens();
        lensSet.addLens(draggingLens);
        lensSet.addLens(new TooltipLens());
        lensSet.addLens(new LegendLens());
        lensSet.addLens(new NodeSizeLens());
        pane.setLens(lensSet);

        pane.addManipulator(new DraggingManipulator(draggingLens, -1));
        pane.addManipulator(new PopupManipulator(pane, (TooltipLens) lensSet.getFirstLensOfType(TooltipLens.class)));


        pane.setNodePainter(TangibleStateNode.class, TangibleStateNode.getShapeNodePainter());
        pane.setNodePainter(VanishingStateNode.class, VanishingStateNode.getShapeNodePainter());
        pane.setNodePainter(TangibleStartStateNode.class, TangibleStartStateNode.getShapeNodePainter());
        pane.setNodePainter(VanishingStartStateNode.class, VanishingStartStateNode.getShapeNodePainter());


        pane.setEdgePainter(DirectedTextEdge.class,
                new PIPELineWithTextEdgePainter(JPowerGraphColor.BLACK, JPowerGraphColor.GRAY, false));
        pane.setEdgePainter(SpacerEdge.class,
        		new PIPESpacerEdgePainter(JPowerGraphColor.BLACK, JPowerGraphColor.GRAY, false));

        pane.setAntialias(true);

        pane.setPopupDisplayer(new SwingPopupDisplayer(new PIPESwingToolTipListener(),
                new PIPESwingContextMenuListener(graph, new LensSet(), new Integer[]{}, new Integer[]{})));

        return new SwingJGraphScrollPane(pane, lensSet);
    }

    /**
     * Calculates the steady state exploration of a Petri net and stores its results
     * in a temporary file.
     * 
     * These results are then read in and turned into a graphical representation using mxGraph
     * which is displayed to the user
     * @param threads number of threads to use to explore the state space
     */
    private void calculateResults(int threads) {
    	if(coverabilityButton.isSelected() && !hasOnlyInfiniteCapacity()) {
    		JOptionPane.showMessageDialog(getMainPanel(),
    			    "The coverability graph algorithm only works for Petri nets where every place has infinite capacity. Please complement all places that have a capacity and set their capacity to infinite, then try again.",
    			    "All places must have infinite capacity",
    			    JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	
        try {
            StateSpaceExplorer.StateSpaceExplorerResults results =
                    stateSpaceLoader.calculateResults(new StateSpaceLoader.ExplorerCreator() {
                                                          @Override
                                                          public ExplorerUtilities create(PetriNet petriNet) {
                                                              return getExplorerUtilities(petriNet);
                                                          }
                                                      }, new StateSpaceLoader.VanishingExplorerCreator() {
                                                          @Override
                                                          public VanishingExplorer create(ExplorerUtilities utils) {
                                                              return getVanishingExplorer(utils);
                                                          }
                                                      }, threads
                    );
            updateTextResults(results.numberOfStates, results.processedTransitions);
            if (results.numberOfStates <= MAX_STATES_TO_DISPLAY) {
                StateSpaceLoader.Results stateSpace = stateSpaceLoader.loadStateSpace();
                updateGraph(stateSpace.records, stateSpace.stateMappings);
            }

        } catch (InvalidRateException | TimelessTrapException | IOException | InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, e.toString());
            JOptionPane.showMessageDialog(panel1, e.toString(), "State space explorer error", JOptionPane.ERROR_MESSAGE);
        } catch (StateSpaceLoaderException e) {
            JOptionPane.showMessageDialog(panel1, e.getMessage(), "GSPN Analysis Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Copies the temporary files to a permanent loaction
     */
    private void saveBinaryFiles() {
        stateSpaceLoader.saveBinaryFiles();
    }

    /**
     * Creates the explorer utilities based upon whether the coverability or reachability graph
     * is being generate
     *
     * @param petriNet  petrinet
     * @return explorer utilities for generating state space
     */
    private ExplorerUtilities getExplorerUtilities(PetriNet petriNet) {
        if (coverabilityButton.isSelected()) {
            return new CoverabilityExplorerUtilities(new UnboundedExplorerUtilities(petriNet));
        }

        return new BoundedExplorerUtilities(petriNet, Integer.valueOf(maxStatesField.getText()));

    }

    /**
     * Vanishing explorer is either a {@link pipe.reachability.algorithm.SimpleVanishingExplorer} if
     * vanishing states are to be included in the graph, else it is {@link pipe.reachability.algorithm.OnTheFlyVanishingExplorer}
     *
     * @param explorerUtilities  previously generated explorer utilities
     */
    private VanishingExplorer getVanishingExplorer(ExplorerUtilities explorerUtilities) {
        if (includeVanishingStatesCheckBox.isSelected()) {
            return new SimpleVanishingExplorer();
        }
        return new OnTheFlyVanishingExplorer(explorerUtilities);
    }

    /**
     * Updates the text results with the number of states and transitions
     *
     * @param states      number of states
     * @param transitions number of transitions
     */
    private void updateTextResults(int states, int transitions) {
        StringBuilder results = new StringBuilder();
        results.append("Results: ").append(states).append(" states and ").append(transitions).append(" transitions");
        textResultsLabel.setText(results.toString());
    }

    /**
     * Updates the mxGraph to display the records
     *
     * @param records  state transitions from a processed Petri net
     * @param stateMap state map
     */
    private void updateGraph(Iterable<Record> records, Map<Integer, ClassifiedState> stateMap) {
        graph.clear();
        Map<Integer, Node> nodes = getNodes(stateMap);
        Collection<Edge> edges = getEdges(records, nodes);
        graph.addElements(nodes.values(), edges);
        layoutGraph();
    }

    /**
     * @param stateMap state map
     * @return All nodes to be added to the graph
     */
    private Map<Integer, Node> getNodes(Map<Integer, ClassifiedState> stateMap) {
    	int maxCapacity = getMaxCapacity();
    	
        Map<Integer, Node> nodes = new HashMap<>(stateMap.size());
        for (Map.Entry<Integer, ClassifiedState> entry : stateMap.entrySet()) {
            ClassifiedState state = entry.getValue();
            int id = entry.getKey();
            
            if(maxCapacity == 1) {
            	nodes.put(id, createSimpleNode(state, id));
            } else {
            	nodes.put(id, createRegularNode(state, id));
            }
        }
        return nodes;
    }

    /**
     * All edges to be added to the graph
     *
     * @param records  records of states, and all states that can be reached from each state
     * @param nodes    map of ids to the corresponding state nodes
     * @return         all directional edges between state nodes A and B, where B can be reached from A
     */
    private Collection<Edge> getEdges(Iterable<Record> records, Map<Integer, Node> nodes) {
        Collection<Edge> edges = new ArrayList<>();
        Map<Node,Set<Node>> connections = new HashMap<>();
        
        for (Record record : records) {
            int state = record.state;
            for (Map.Entry<Integer, Pair<Double, Collection<String>>> entry : record.successors.entrySet()) {
                int succ = entry.getKey();
                ArrayList<String> transitionNames = (ArrayList<String>) entry.getValue().getRight();
                Collections.sort(transitionNames);
                double rate = entry.getValue().getLeft();
                
                Node startNode = nodes.get(state);
                Node endNode = nodes.get(succ);
                edges.add(new DirectedTextEdge(startNode, endNode,
                        String.format("%s (%.2f)", StringUtils.join(transitionNames, ", "), rate)));
                
                //Keep track of all single connections
                addToSet(startNode, endNode, connections);
                addToSet(endNode, startNode, connections);
            }
        }
        
        return edges;
    }
    
    private void addToSet(Node key, Node val, Map<Node,Set<Node>> collection) {
        Set<Node> result;
    	if(!collection.containsKey(key)) {
        	result = new HashSet<>(1);
        	result.add(val);
        	collection.put(key, result);
        } else {
        	result = collection.get(key);
        	result.add(val);
        }
    }

    private boolean inSet(Node key, Node val, Map<Node,Set<Node>> collection) {
    	return collection.containsKey(key) && collection.get(key).contains(val);
    }
    
    /**
     * Performs laying out of items on the graph
     */
    private void layoutGraph() {
        Layouter layouter = new Layouter(new SpringLayoutStrategy(graph));
        layouter.start();
    }

    /**
     * Creates a node that displays which places contain a token. Only to be used if maxCapacity == 1
     * 
     * @param state classified state to be turned into a graph node
     * @param id    state integer id
     * @return Tangible or Vanishing state node with simple notation
     */
    private Node createSimpleNode(ClassifiedState state, int id) {
    	String label = "";
    	String toolTip = "";
    	
    	List<String> places = new ArrayList<String>();
    	for(String place : state.getPlaces()) {
    		places.add(place);
    	}
    	Map<String, Map<String, Integer>> tokenMap = state.asMap();
    	Collections.sort(places);
   		int numberOfTokens = tokenMap.get(places.get(0)).size();

   		if(numberOfTokens > 1) {
   			//Well, that was a waste of time...
   			return createRegularNode(state, id);
   		}
   		
   		String token = "";
   		for(String onlyToken : tokenMap.get(places.get(0)).keySet()) {
   			token = onlyToken;
   		}
   		
    	List<String> preparedLabel = new ArrayList<String>();
    	List<String> preparedToolTip = new ArrayList<String>();
    	
    	for(String place : places) {
    		int tokenCount = tokenMap.get(place).get(token); 
			if( tokenCount == 1 ) {
				preparedLabel.add(place);
			}
			preparedToolTip.add("<b>" + place + ":</b> " + tokenCount);
    	}
    	
    	label = StringUtils.join(preparedLabel, ", ");
    	toolTip = StringUtils.join(preparedToolTip, "<br>");
    	return createNode(state, label, toolTip, id);
    }
    
    /**
     * Creates a node that displays the number of tokens per type, per place for this state in a tuple
     * 
     * @param state classified state to be turned into a graph node
     * @param id    state integer id
     * @return Tangible or Vanishing state node with tuple notation
     */
    private Node createRegularNode(ClassifiedState state, int id) {
    	String label = "";
    	String toolTip = "";
    	
    	List<String> places = new ArrayList<String>();
    	for(String place : state.getPlaces()) {
    		places.add(place);
    	}
    	List<String> tokens = new ArrayList<String>();
    	Map<String, Map<String, Integer>> tokenMap = state.asMap();
    	Collections.sort(places);
   		int numberOfTokens = tokenMap.get(places.get(0)).size();
    	for(String token : tokenMap.get(places.get(0)).keySet()) {
    		tokens.add(token);
    	}
    	Collections.sort(tokens);
    	List<String> preparedStrings = new ArrayList<String>();
    	List<String> preparedToolTipStrings = new ArrayList<String>();
    	
    	label += "[" + Integer.toString(id) + "] ";
    	for(String place : places) {
    		List<String> tokenCountForPlace = new ArrayList<String>();
    		List<String> toolTipTokenCountForPlace = new ArrayList<String>(); 
    		String preparedString;
    		
    		for(String token : tokens) {
    			int tokenCount = tokenMap.get(place).get(token);
    			if(tokenCount == Integer.MAX_VALUE) {
    				tokenCountForPlace.add("ω");
    				toolTipTokenCountForPlace.add("ω " + token);
    			} else {
    				tokenCountForPlace.add(Integer.toString(tokenCount));
    				toolTipTokenCountForPlace.add(Integer.toString(tokenCount) + " " + token);
    			}
    		}
    		preparedString = StringUtils.join(tokenCountForPlace, ",");
    		
    		if(numberOfTokens > 1) {
    			preparedStrings.add("(" + preparedString + ")");
    		} else {
    			preparedStrings.add(preparedString);
    		}
    		
    		preparedToolTipStrings.add("<b>" + place + ":</b> " + StringUtils.join(toolTipTokenCountForPlace, ", "));
    	}
    	label += "(";
    	label += StringUtils.join(preparedStrings, ",");    	
    	label += ")";
    	
    	toolTip = StringUtils.join(preparedToolTipStrings, "<br>");
    	
    	return createNode(state, label, toolTip, id);
    }
    
    /**
     * @param state classified state to be turned into a graph node
     * @param String label to be used on the node
     * @param String tooltip to be used when hovering over node with mouse
     * @param id    state integer id
     * @return Tangible or Vanishing state node
     */
    private Node createNode(ClassifiedState state, String label, String toolTip, int id) {
        if (state.isTangible() && id == 0) {
        	return new TangibleStartStateNode(label, toolTip, id);
        } else if(state.isTangible()) {
            return new TangibleStateNode(label, toolTip, id);
        } else if(id == 0) {
        	return new VanishingStartStateNode(label, toolTip, id);
        } else {
        	return new VanishingStateNode(label, toolTip, id);
        }
    }

    /**
     * Constructor deactivates use current petri net radio button since none is supplied.
     *
     * @param loadDialog the dialog to be shown
     */
    public ReachabilityGraph(FileDialog loadDialog) {
        stateSpaceLoader = new StateSpaceLoader(loadDialog);
        setUp();
    }

    /**
     * Main method for running this externally without PIPE
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ReachabilityGraph");
        FileDialog selector = new FileDialog(frame, "Select petri net", FileDialog.LOAD);
        frame.setContentPane(new ReachabilityGraph(selector).panel1);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @return main panel of the GUI
     */
    public Container getMainPanel() {
        return panel1;
    }
}
