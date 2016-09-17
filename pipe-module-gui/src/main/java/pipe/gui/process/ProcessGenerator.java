package pipe.gui.process;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.rendersnake.*;
import static org.rendersnake.HtmlAttributesFactory.*;

import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.*;
import uk.ac.imperial.pipe.animation.PetriNetAnimator;
import uk.ac.imperial.pipe.exceptions.PetriNetComponentNotFoundException;
import uk.ac.imperial.pipe.exceptions.PetriNetComponentException;
import uk.ac.imperial.pipe.naming.ComponentNamer;
import uk.ac.imperial.pipe.models.petrinet.name.*;
import uk.ac.imperial.utils.Pair;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;
import uk.ac.imperial.pipe.visitor.ClonePetriNet;

import pipe.gui.widget.StateSpaceLoader;
import pipe.gui.widget.StateSpaceLoaderException;
import pipe.reachability.algorithm.*;

import java.io.File;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.Math;

import pipe.gui.ModuleBridge;

/**
 * Performs the exploration and steady state analysis of a Petri net.
 * Displays useful performance analysis metrics
 */
public class ProcessGenerator {
    private JPanel mainPanel;
    
    private JScrollPane infoScrollPane;
    
    private JTextPane infoTextPane;
    
    private JTextField fireSequenceTextField;
    
    private JButton generateButton;
        
    private JCheckBox condenseCheckBox;
    
    private JProgressBar progressBar;
    
    private final PetriNet net;
    
    private PetriNet animatedNet;
    
    private final PropertyChangeSupport changeSupport;
    
    private PetriNetAnimator animator;
    
    private String error;
    
    private final int PROCESS_NET_LEFT_PADDING = 100;
    private final int PROCESS_NET_TOP_PADDING = 200;
    private final int PROCESS_NET_COLUMN_DISTANCE = 75;
    private final int PROCESS_NET_ROW_DISTANCE = 100;
    private final int PROCESS_NET_ANNOTATION_MIN_WIDTH = 600;
    
    private final String NOT_ENABLED = "TRANSITION_NOT_ENABLED";
    private final String NOT_EXIST = "TRANSITION_DOES_NOT_EXIST";
    
    private final String PROTOCOL_REMOVE = "transition-remove";
    private final String PROTOCOL_SELECT = "transition-select";
    private final String PROTOCOL_ADD = "transition-add";
    
    /**
     * We can only generate guaranteed correct process nets for nets with places that have capacity 1
     * @return
     */
    private boolean isValidNet() {
    	StateSpaceLoader stateSpaceLoader = new StateSpaceLoader(net, null);
    	Map<Transition,Pair<Set<String>,Set<String>>> transitionMap = new HashMap<>();
    	StateSpaceLoader.Results stateSpace = null;
    	
    	for(Place place : this.net.getPlaces()) {
    		if(place.getCapacity() > 1 || !place.hasCapacityRestriction()) {
    	    	this.error = "Process net generator only works on EN systems";
    			return false;
    		}
    	}
    	
    	try {
	        StateSpaceExplorer.StateSpaceExplorerResults results =
	                stateSpaceLoader.calculateResults(new StateSpaceLoader.ExplorerCreator() {
	                                                      @Override
	                                                      public ExplorerUtilities create(PetriNet petriNet) {
	                                                          return new BoundedExplorerUtilities(petriNet, 1000);
	                                                      }
	                                                  }, new StateSpaceLoader.VanishingExplorerCreator() {
	                                                      @Override
	                                                      public VanishingExplorer create(ExplorerUtilities utils) {
	                                                          return new SimpleVanishingExplorer();
	                                                      }
	                                                  }, 1
	                );
	        stateSpace = stateSpaceLoader.loadStateSpace();
        } catch (InvalidRateException | TimelessTrapException | IOException | InterruptedException | ExecutionException | StateSpaceLoaderException e) {
            this.error = "Unable to generate state space for contact-detection";
        }
        
        for(Transition transition : net.getTransitions()) {
        	Set<String> inboundPlaces = new HashSet<>();
        	Set<String> outboundPlaces = new HashSet<>();
        	
        	for(InboundArc arc : net.inboundArcs(transition)) {
        		inboundPlaces.add(arc.getSource().getId());
        	}
        	for(OutboundArc arc : net.outboundArcs(transition)) {
        		outboundPlaces.add(arc.getTarget().getId());
        	}
        	
        	transitionMap.put(transition, new Pair<>(inboundPlaces, outboundPlaces));
        }
        	
        String tokenName = null;
        	
    	for(ClassifiedState state : stateSpace.stateMappings.values()) {
    		Map<String, Map<String, Integer>> tokenMap = state.asMap();
    		Set<String> configuration = new HashSet<>();
    		
    		for(Map.Entry<String, Map<String, Integer>> entry : tokenMap.entrySet()) {
    			if(tokenName == null) {
    				for(String token : entry.getValue().keySet()) {
    					tokenName = token;
    					break;
    				}
    			}
    			
    			if(entry.getValue().get(tokenName) == 1) {
    				configuration.add(entry.getKey());
    			}
    		}
    		
    		for(Map.Entry<Transition, Pair<Set<String>,Set<String>>> entry : transitionMap.entrySet()) {
    			if(configuration.containsAll(entry.getValue().getLeft())) {
    				for(String outboundPlace : entry.getValue().getRight()) {
    					if(configuration.contains(outboundPlace)) {
    						this.error = "Transition " + entry.getKey().getId() + " has contact in configuration (" +
    								StringUtils.join(configuration, ", ") + ")";
    						return false;
    					}
    				}
    			}
    		}
    	}
        
    	return true;
    }
    
    /**
     * Sets up the UI
     */
    private void setUp() {
    	final PropertyChangeListener childListener = new PropertyChangeListener() {
    		@Override
    	    public void propertyChange(PropertyChangeEvent e) {
    			String innerProperty = e.getPropertyName();
    			System.out.println("place: " + e.getPropertyName());
    			if(innerProperty.equals(PlaceablePetriNetComponent.X_CHANGE_MESSAGE) ||
  				   innerProperty.equals(PlaceablePetriNetComponent.Y_CHANGE_MESSAGE) ||
  				   innerProperty.equals(PlaceablePetriNetComponent.WIDTH_CHANGE_MESSAGE) ||
  				   innerProperty.equals(PlaceablePetriNetComponent.HEIGHT_CHANGE_MESSAGE)) {
    				//We do not need to change anything
    				return;
    			}
    			setUpDialog();
    		}
		};
    	
    	setUpDialog();
    	progressBar.setVisible(false);
    	
    	this.net.addPropertyChangeListener(new PropertyChangeListener() {
    		@Override
    	    public void propertyChange(PropertyChangeEvent e) {
    			String property = e.getPropertyName();
    			if(property.equals(PetriNet.NEW_PLACE_CHANGE_MESSAGE)) {
    				((Place) e.getNewValue()).addPropertyChangeListener(childListener);
    			} else if(property.equals(PetriNet.NEW_TRANSITION_CHANGE_MESSAGE)) {
    				((Transition) e.getNewValue()).addPropertyChangeListener(childListener);
    			}
    			System.out.println("main: " + property);
    			
    			setUpDialog();
    	    }
    	});
    	
    	for(Place place : this.net.getPlaces()) {
    		place.addPropertyChangeListener(childListener);
    	}
    	for(Transition transition : this.net.getTransitions()) {
    		transition.addPropertyChangeListener(childListener);
    	}
    	
    	
    	//Called live during editing
    	fireSequenceTextField.getDocument().addDocumentListener(new DocumentListener() {
    		  public void changedUpdate(DocumentEvent e) {
    		    updateState();
    		  }
    		  public void removeUpdate(DocumentEvent e) {
      		    updateState();
    		  }
    		  public void insertUpdate(DocumentEvent e) {
      		    updateState();
    		  }
    		});

    	//Called after losing focus
        fireSequenceTextField.addFocusListener(new FocusListener() {
        	@Override
        	public void focusGained(FocusEvent e) {
        		//Do nothing
        	}
        	
            @Override
            public void focusLost(FocusEvent e) {
                updateState();
            }
        });
        
        generateButton.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		generateFormattedPetriNet(getFireSequence(), condenseCheckBox.isSelected());
        	}
        });
        
        infoTextPane.addHyperlinkListener(new HyperlinkListener() {
        	public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                	List<String> fireSequence = getFireSequence();
                	String[] action = e.getDescription().split("::");
                	
                	if(action[0].equals(PROTOCOL_REMOVE)) {
                		fireSequence.remove(Integer.parseInt(action[1]));
                		setFireSequence(fireSequence);
                	} else if(action[0].equals(PROTOCOL_ADD)) {
                		addToFireSequence(action[1]);
                	} else if(action[0].equals(PROTOCOL_SELECT)) {
                		String inputString = fireSequenceTextField.getText();
                		int start;
                		int entryIndex = Integer.parseInt(action[1]);
                		if(entryIndex == 0) {
                			//There can be whitespace before the entry
                			start = inputString.indexOf(fireSequence.get(entryIndex));
                		} else {
                			int commastart = StringUtils.ordinalIndexOf(inputString, ",", entryIndex);
                			start = inputString.indexOf(fireSequence.get(entryIndex), commastart);
                		}
                		int end = start + fireSequence.get(entryIndex).length();
                		
                		fireSequenceTextField.requestFocusInWindow();
                		fireSequenceTextField.setCaretPosition(start);
                		fireSequenceTextField.moveCaretPosition(end);
                	} else {
                		System.out.println(action[0] + " is unknown!");
                	}
                	
                }
            }
        });
    }
    
    private void setUpAnimator() {
        this.animatedNet = ClonePetriNet.clone(this.net);
        if(this.animatedNet == null) {
        	JOptionPane.showMessageDialog(getMainPanel(), "Could not clone current Petri net: Petri net might be inconsistent.", "Error", JOptionPane.OK_OPTION);
        	getMainPanel().getRootPane().setVisible(false);
        }
    	this.animator = new PetriNetAnimator(this.animatedNet);
    }
    
    private void setUpDialog() {
    	if(isValidNet()) {
    		this.error = null;
    		
			//We can use a fire sequence on this net
   			fireSequenceTextField.setEnabled(true);
			
			//The net used for animating might be different
			setUpAnimator();
			
			//Since the net was changed, this information might be out of date
			updateState();
		} else {
			fireSequenceTextField.setEnabled(false);
	    	generateButton.setEnabled(false);
	    	generateButton.setToolTipText(this.error);
	    	updateInfo();
		}
    }
    
    private void addToFireSequence(String transition) {
    	List<String> sequence = getFireSequence();
    	sequence.add(transition);
    	fireSequenceTextField.setText(StringUtils.join(sequence, ", "));
    }
    
    private void updateState() {
    	ProcessResult result = recalculateAnimations(getFireSequence());
    	
    	if (result.errors.size() == 0) {
			generateButton.setEnabled(true);
			generateButton.setToolTipText("Generate a process net with given fire sequence");
			this.error = null;
		} else {
			generateButton.setEnabled(false);
			generateButton.setToolTipText("Given fire sequence is invalid");
			this.error = "Given fire sequence is invalid.";
    	}
    	
   		updateInfo(result);
    }

    /**
     * Processes input string into names of transitions
     * @return tokenized string with names of fired transitions
     */
    private List<String> getFireSequence() {
    	String unparsedFireSequence = fireSequenceTextField.getText();
    	String[] partlyParsedSequence = unparsedFireSequence.split("[\\s\\u0085\\p{Z}]*,[\\s\\u0085\\p{Z}]*");
    	List<String> parsedSequence = new ArrayList<>();
    	
    	for( String transition : partlyParsedSequence ) {
    		String parsedTransition = transition.trim();
    		if(!parsedTransition.equals("")) {
    			parsedSequence.add(parsedTransition);
    		}
    	}
    	return parsedSequence;
    }
    
    private void setFireSequence(List<String> fireSequence) {
    	fireSequenceTextField.setText(StringUtils.join(fireSequence, ", "));
    }
    
    /**
     * Simulates the current fire sequence, detects impossible situations and calculates transitions that can be fired
     * @return
     */
    private ProcessResult recalculateAnimations(List<String> fireSequence) {
    	animator.saveState();
    	Set<String> availableTransitions = new HashSet<>();
    	ArrayList<Pair<Integer,String>> errors = new ArrayList<>();
    	int lastElement = fireSequence.size() - 1;

    	for(int i = 0; i < fireSequence.size(); i++) {
    		String firedTransition = fireSequence.get(i);
    		if(!firedTransition.equals("")) {
    			try {
    				Transition transition = animatedNet.getComponent(firedTransition, Transition.class);
    				
    				if(!animator.getEnabledTransitions().contains(transition)) {
    					errors.add(new Pair<>(i, NOT_ENABLED));
    				} else {
    					animator.fireTransition(transition);
    				}
    			} catch(PetriNetComponentNotFoundException e) {
    				if(!(fireSequenceTextField.hasFocus() && i == lastElement)) {
    					errors.add(new Pair<>(i, NOT_EXIST));
    				}
    			}
    		}
    	}
		for(Transition transition : animator.getEnabledTransitions()) {
			availableTransitions.add(transition.getId());
		}
    	
    	animator.reset();
    	return new ProcessResult(availableTransitions, errors);
    }
    
    private void updateInfo(ProcessResult result) {
        HtmlCanvas html = new HtmlCanvas();
        List<String> fireSequence = getFireSequence();
    	
    	try {
    		html.html().head()._head();
    		html.body();
        
    		if(result.errors.size() > 0) {
    			html.b().content("The following issues currently exist");
    			html.ul();
    			for(Pair<Integer,String> error : result.errors) {
    				html.li().write(fireSequence.get(error.getLeft()) + ": ");
    				if(error.getRight().equals(NOT_ENABLED)) {
    					html.write("Transition " + (error.getLeft()+1) + " is not enabled. ");
    				} else if(error.getRight().equals(NOT_EXIST)) {
    					html.write("No transition with such a name exists. ");
    				} else {
    					html.write("UKNOWN ERROR");
    				}
    				html.write("(");
    				html.a(href(PROTOCOL_REMOVE + "::" + error.getLeft())).content("Remove").write(" | ");
    				html.a(href(PROTOCOL_SELECT + "::" + error.getLeft())).content("Select");
    				html.write(")");
    				html._li();
    			}
    			html._ul();
    		}
    		html.b().content("The following transitions can be fired");
    		html.ul();
    		for(String availableTransition : result.availableTransitions) {
    			html.li().write(availableTransition);
    			html.write(" (");
    			html.a(href(PROTOCOL_ADD + "::" + availableTransition)).content("Add");
    			html.write(")");
    			html._li();
    		}
    		html._ul();
    		
    		if(this.error != null) {
    			html.b().content("Cannot generate process net");
    			html.ul();
    			html.li().write(this.error);
    			html._li();
    			html._ul();
    		}
    		
    		html._body()._html();
            infoTextPane.setText(html.toHtml());
    	} catch(IOException e) {
    		infoTextPane.setText("<html><head></head><body>Error</body></html>");
    	}
    }
    
    private void updateInfo() {
        HtmlCanvas html = new HtmlCanvas();
    	
    	try {
	    	html.html().head()._head().body();
			if(this.error != null) {
				html.b().content("Cannot generate process net");
				html.ul();
				html.li().write(this.error);
				html._li();
				html._ul();
			}
			html._body()._html();
			infoTextPane.setText(html.toHtml());
    	} catch(IOException e) {
	    	infoTextPane.setText("<html><head></head><body>Error</body></html>");
	    }
    }
    
    private void generateFormattedPetriNet(final List<String> fireSequence, final boolean condensed) {
    	final PetriNet processNet = new PetriNet();
    	final PetriNet localAnimatedNet = ClonePetriNet.clone(this.net);
    	
    	//Set up the basics of this net
    	PetriNetName name = new NormalPetriNetName(localAnimatedNet.getNameValue() + " process (" + StringUtils.join(fireSequence, ", ") + ")");
    	processNet.setName(name);
    	
    	Thread thread = new Thread() {
    		@Override
    		public void run() {
        		generateButton.setEnabled(false);
        		progressBar.setVisible(true);
        		progressBar.setValue(0);
    			
    	    	Map<Place, Place> processPlaceMap = new HashMap<>();
    	    	List<List<PlaceablePetriNetComponent>> columns = new ArrayList<>();
    	    	Map<Place, PlaceNamer> placeNamers = new HashMap<>();
    	    	Map<Transition, TransitionNamer> transitionNamers = new HashMap<>();
    	    	
    	    	Comparator<PlaceablePetriNetComponent> comparator = new Comparator<PlaceablePetriNetComponent>() {
    	    		@Override
    			    public int compare(PlaceablePetriNetComponent component1, PlaceablePetriNetComponent component2) {
    			        if(component2.getX() - component1.getX() != 0) {
    			        	//Left-most component should be after right-most element
    			        	return component2.getX() - component1.getX();
    			        } if(component1.getY() - component2.getY() != 0) {
    			        	//Top-most component must be before components below it
    			        	return component1.getY() - component2.getY();
    			        } else {
    			        	return component1.getId().compareTo(component2.getId());
    			        }
    			    }
    			};
    	    	
    			progressBar.setValue(1);
    	    	
    	    	for(Token token : localAnimatedNet.getTokens()) {
    	    		processNet.addToken(token);
    	    	}
    	    	
    	    	for(int i = 0; i < (fireSequence.size()+1)*2; i++) {
    	    		columns.add(new ArrayList<PlaceablePetriNetComponent>());
    	    	}
    	    	
    	    	try {
    	    		progressBar.setValue(10);
    		    	//Starting configuration and initialisation
    		    	for(Place place : localAnimatedNet.getPlaces()) {
    		    		Place processPlace = null;
    					placeNamers.put(place, new PlaceNamer(processNet, localAnimatedNet, place.getId()));
    		
    		    		if(place.getNumberOfTokensStored() > 0) {
    		    			processPlace = new DiscretePlace(placeNamers.get(place).getName());
    		    			processPlace.setTokenCounts(place.getTokenCounts());
    		    			processPlace.setX(PROCESS_NET_LEFT_PADDING);
    		    			//Y coordinate is further refined later
    		    			processPlace.setY(PROCESS_NET_TOP_PADDING);
    		    			columns.get(0).add(processPlace);
    		    			processNet.add(processPlace);
    		    		}
    					processPlaceMap.put(place, processPlace);
    		    	}
    		    	for(Transition transition : localAnimatedNet.getTransitions()) {
    		    		transitionNamers.put(transition, new TransitionNamer(processNet, localAnimatedNet, transition.getId()));
    		    	}
    		    	
    				progressBar.setValue(30);
    		    	//Process transitions in fire sequence
    		    	for(int i = 0; i < fireSequence.size(); i++) {
    		    		String transitionName = fireSequence.get(i);
    		    		if(transitionName.equals("")) {
    		    			continue;
    		    		}
    		    		
    		    		Transition transition = localAnimatedNet.getComponent(transitionName, Transition.class);
    		    		Transition processTransition = new DiscreteTransition(transitionNamers.get(transition).getName());
    		    		Set<Place> inboundPlaces = new HashSet<Place>();
    		    		Set<Place> outboundPlaces = new HashSet<Place>();
    		    		int transitionColumn = 0;
    		    		
    		    		processNet.add(processTransition);
    		    		
    		    		for(InboundArc inboundArc : localAnimatedNet.inboundArcs(transition)) {
    		    			Place inboundPlace = inboundArc.getSource();
    		    			Place processPlace = processPlaceMap.get(inboundPlace);
    		    			if(processPlace == null) {
    		    				JOptionPane.showMessageDialog(getMainPanel(), "Transition " + transitionName + " fired, but it's inbound place " + inboundPlace.getId() + " was not yet in the process net.", "Error", JOptionPane.OK_OPTION);
    		    				return;
    		    			}
    		    			processPlaceMap.put(inboundPlace, null);
    		    			int placeColumn = (processPlace.getX() - PROCESS_NET_LEFT_PADDING) / PROCESS_NET_COLUMN_DISTANCE;
    		    			
    		    			if(transitionColumn <= placeColumn) {
    		    				transitionColumn = placeColumn + 1;
    		    			}
    		    			
    		    			InboundArc processInboundArc = new InboundNormalArc(processPlace, processTransition, inboundArc.getTokenWeights());
    		    			
    		    			inboundPlaces.add(processPlace);
    		    			
    		    			processNet.add(processPlace);
    		    			processNet.add(processInboundArc);
    		    		}
    		    		
    		    		if(!condensed) {
    		    			//If not in condensed format, each transition is in it's own column
    		    			transitionColumn = 1 + i*2;
    		    		}
    		    		processTransition.setX(PROCESS_NET_LEFT_PADDING + transitionColumn * PROCESS_NET_COLUMN_DISTANCE);
    		    		
    		    		for(OutboundArc outboundArc : localAnimatedNet.outboundArcs(transition)) {
    		    			Place outboundPlace = outboundArc.getTarget();
    		    			if(processPlaceMap.get(outboundPlace) != null) {
    		    				JOptionPane.showMessageDialog(getMainPanel(), "Transition " + transitionName + " fired, but it's outbound place " + outboundPlace.getId() + " was already in the process net.", "Error", JOptionPane.OK_OPTION);
    		    				return;    				
    		    			}
    		    			Place processPlace = new DiscretePlace(placeNamers.get(outboundPlace).getName());
    		    			processPlace.setX(PROCESS_NET_LEFT_PADDING + (transitionColumn + 1) * PROCESS_NET_COLUMN_DISTANCE);
    		    			processPlaceMap.put(outboundPlace, processPlace);
    		    			OutboundArc processOutboundArc = new OutboundNormalArc(processTransition, processPlace, outboundArc.getTokenWeights());
    		    			
    		    			outboundPlaces.add(processPlace);
    		    			
    		    			processNet.add(processPlace);
    		    			processNet.add(processOutboundArc);
    		    		}
    		    		
    		    		columns.get(transitionColumn).add(processTransition);
    		    		columns.get(transitionColumn + 1).addAll(outboundPlaces);
    		    	}

    				progressBar.setValue(50);    	
    		    	//Initial height
    		    	for(int i = 0; i < columns.size(); i++) {
    		    		List<PlaceablePetriNetComponent> column = columns.get(i);
    		    		for(PlaceablePetriNetComponent component : column) {
    		    			int y = component.getY();
    		    			Collection<Arc> arcs = new HashSet<Arc>();
    		    			if((i % 2) == 0 ) {
    		    				arcs.addAll(processNet.inboundArcs((Place) component));
    		    			} else {
    		    				arcs.addAll(processNet.inboundArcs((Transition) component));
    		    			}
    		    			
    		    			for(Arc arc : arcs) {
    		    				y += arc.getSource().getY();
    		    			}
    		    			if(arcs.size() > 0) {
    		    				y = y / arcs.size();
    		    			}
    		    			if(y < PROCESS_NET_TOP_PADDING) {
    		    				y = PROCESS_NET_TOP_PADDING;
    		    			}
    		    			
    		    			component.setY(y);
    		    		}
    		    		
    		    		Collections.sort(column, comparator);
    		    		
    		    		ensureRowDistance(column);
    		    	}
    		    	
    				progressBar.setValue(60);
    		    	//Fine-tuning of height based on transitions
    		    	for(int i = 1; i < columns.size(); i += 2) {
    		    		List<PlaceablePetriNetComponent> column = columns.get(i);
    		    		for(PlaceablePetriNetComponent component : column) {
    		    			List<PlaceablePetriNetComponent> places = new ArrayList<>();
    		    			Collection<Arc> arcs = new HashSet<Arc>(processNet.inboundArcs((Transition) component));
    		    			for(Arc arc : arcs) {
    		    				places.add(arc.getSource());
    		    			}
    		    			Collections.sort(places, comparator);
    		    			
    		    			int y = 0;
    		    			int num = 0;
    		    			y += ensureRowDistance(places);
    		    			num += places.size();
    		    			
    		    			places.clear();
    		    			arcs = new HashSet<Arc>(processNet.outboundArcs((Transition) component));
    		    			for(Arc arc : arcs) {
    		    				places.add(arc.getTarget());
    		    			}
    		    			Collections.sort(places, comparator);
    		    			
    		    			y += ensureRowDistance(places);
    		    			num += places.size();
    		    			
    		    			if(num > 0) {
    		    				component.setY(y / num);
    		    			}
    		    		}
    		    	}
    		    	
    		    	for(List<PlaceablePetriNetComponent> column : columns) {
    		    		ensureRowDistance(column);
    		    	}
    		    	
    				progressBar.setValue(80);
    		    	//Finishing touches
    		    	Annotation annotation = new AnnotationImpl(100, 100, "Process net for " + StringUtils.join(fireSequence, ", "), Math.max(PROCESS_NET_ANNOTATION_MIN_WIDTH, columns.size()*PROCESS_NET_ROW_DISTANCE), 30, false);
    		    	processNet.add(annotation);
    		    	
            		progressBar.setValue(100);
            		
            		progressBar.setVisible(false);
            		generateButton.setEnabled(true);
    	    	} catch(PetriNetComponentNotFoundException e) {
    	    		JOptionPane.showMessageDialog(getMainPanel(), "Could not find a Petri net component, even though we established somewhere else that it should exist.", "Error", JOptionPane.OK_OPTION);
    	    		return;
    	    	} catch(PetriNetComponentException e) {
    	    		JOptionPane.showMessageDialog(getMainPanel(), "Could not add a component to process net, even though we made sure they were unique...", "Error", JOptionPane.OK_OPTION);
    	    		return;
    	    	}
    	    	
       			changeSupport.firePropertyChange(ModuleBridge.MODULE_ADD_PETRINET_MESSAGE, null, processNet);
    		}
    	};
    	thread.start();
    }
    
    public ProcessGenerator(PetriNet petriNet, FileDialog fileDialog, PropertyChangeSupport changeSupport) {
        this.net = petriNet;
        this.changeSupport = changeSupport;

        setUp();
    }
    
    private int ensureRowDistance(Collection<PlaceablePetriNetComponent> components) {
    	int y = 0;
		int lastY = PROCESS_NET_TOP_PADDING - PROCESS_NET_ROW_DISTANCE;
		for(PlaceablePetriNetComponent component : components) {
			if(component.getY() - lastY < PROCESS_NET_ROW_DISTANCE) {
				component.setY(lastY + PROCESS_NET_ROW_DISTANCE);
			}
			lastY = component.getY();
			y += lastY;
		}
		
		return y;
    }

    /**
     * Main method for running this externally without PIPE
     *
     * @param args          command line arguments
     */
    public static void main(String[] args) {
    	//Does not make sense to run this as a stand-alone program
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
    
    private final class ProcessResult {
    	public final Set<String> availableTransitions;
    	public final ArrayList<Pair<Integer,String>> errors;
    	
    	ProcessResult(Set<String> availableTransitions, ArrayList<Pair<Integer,String>> errors) {
    		this.availableTransitions = availableTransitions;
    		this.errors = errors;
    	}
    }
    
    private class PlaceNamer extends ComponentNamer {
    	PlaceNamer(PetriNet petriNet, PetriNet originalPetriNet, String placeName) {
    		super(petriNet, placeName + " _", PetriNet.NEW_PLACE_CHANGE_MESSAGE, PetriNet.DELETE_PLACE_CHANGE_MESSAGE);
    		for(Place place : originalPetriNet.getPlaces()) {
                names.add(place.getId());
    		}
    	}
    }
    
    private class TransitionNamer extends ComponentNamer {
    	TransitionNamer(PetriNet petriNet, PetriNet originalPetriNet, String transitionName) {
    		super(petriNet, transitionName + " _", PetriNet.NEW_TRANSITION_CHANGE_MESSAGE, PetriNet.DELETE_TRANSITION_CHANGE_MESSAGE);
    		for(Transition transition : originalPetriNet.getTransitions()) {
                names.add(transition.getId());
    		}
    	}
    }
}
