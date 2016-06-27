package pipe.gui;

import pipe.constants.GUIConstants;
import pipe.controllers.SelectionManager;
import pipe.controllers.ZoomController;
import pipe.views.AbstractPetriNetViewComponent;
import pipe.views.PetriNetViewComponent;
import uk.ac.imperial.pipe.exceptions.PetriNetComponentException;
import uk.ac.imperial.pipe.models.petrinet.*;
import uk.ac.imperial.pipe.visitor.component.PetriNetComponentVisitor;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main canvas that the {@link pipe.views.PetriNetViewComponent}s appear on
 * It is a tab in the main application
 */
public class PetriNetTab extends JLayeredPane implements Observer, Printable {

    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(PetriNetTab.class.getName());

    /**
     * Map of components in the tab with id -> component
     */
    private final Map<String, PetriNetViewComponent> petriNetComponents = new HashMap<>();

    /**
     * Grid displayed on petri net tab
     */
    private final Grid grid = new Grid();

    /**
     * Legacy file for the saving of the underlying Petri net
     */
    @Deprecated
    public File appFile;
    
    /**
     * Constructor
     *
     * Sets no layout manager to acheive an (x,y) layout
     */
    public PetriNetTab() {
        setLayout(null);
        setOpaque(true);
        setDoubleBuffered(true);
        setAutoscrolls(true);
        setBackground(GUIConstants.ELEMENT_FILL_COLOUR);

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    /**
     *
     * Register the zoom listener to the Petri net tab
     *
     * @param zoomController zoom listener
     */
    public void addZoomListener(ZoomController zoomController) {
        zoomController.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                repaint();
            }
        });
    }


    /**
     * Legacy update method
     * @param o
     * @param diffObj
     */
    @Override
    public void update(Observable o, Object diffObj) {
        if (diffObj instanceof AbstractPetriNetViewComponent) {
            AbstractPetriNetViewComponent<?> component = (AbstractPetriNetViewComponent<?>) diffObj;
            addNewPetriNetComponent(component);
        }
    }

    /**
     * Adds the Petri net component to this canvas
     * @param component to add to petri net view
     */
    public void addNewPetriNetComponent(AbstractPetriNetViewComponent<?> component) {
            add(component);
            component.addToContainer(this);
    }

    /**
     * Add the Petri net component to this canvas
     * @param component
     */
    private void add(AbstractPetriNetViewComponent<?> component) {
        registerLocationChangeListener(component.getModel());

        setLayer(component, DEFAULT_LAYER);
        super.add(component);
        petriNetComponents.put(component.getId(), component);
        updatePreferredSize();
        
        component.getModel().addPropertyChangeListener(new NameChangeListener(component, this.petriNetComponents));
        //        repaint();
    }

    /**
     * Update the preferred size of the canvas and grid that is displayed on it
     */
    public void updatePreferredSize() {
        Component[] components = getComponents();
        Dimension d = new Dimension(0, 0);
        for (Component component : components) {
            if (component.getClass() == SelectionManager.class) {
                continue;
            }
            Rectangle r = component.getBounds();
            int x = r.x + r.width + 20;
            int y = r.y + r.height + 20;
            if (x > d.width) {
                d.width = x;
            }
            if (y > d.height) {
                d.height = y;
            }
        }
        setPreferredSize(d);
        Container parent = getParent();
        if (parent != null) {
            parent.validate();
        }
    }
    
    private Rectangle getBoundingRectangle() {
    	Rectangle r = null;
    	int right = 0;
    	int bottom = 0;
    	for (Component component : getComponents()) {
    		if(r == null) {
    			r = component.getBounds();
    			right = r.x + r.width;
    			bottom = r.y + r.height;
    		} else {
    			Rectangle cr = component.getBounds();
    			if(cr.x + cr.width > right) {
    				right = cr.x + cr.width;
    			}
    			if(cr.y + cr.height > bottom) {
    				bottom = cr.y + cr.height;
    			}
    			if(cr.x < r.x) {
    				r.x = cr.x;
    			}
    			if(cr.y < r.y) {
    				r.y = cr.y;
    			}
    		}
    	}
    	r.width = right - r.x;
    	r.height = bottom - r.y;
    	
    	return r;
    }

    /**
     *
     * Registeres a location listener on the Petri net component
     *
     * @param component
     */
    private void registerLocationChangeListener(PetriNetComponent component) {

        PetriNetComponentVisitor changeListener = new ChangeListener();
        try {
            component.accept(changeListener);
        } catch (PetriNetComponentException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * Prints the Petri net tab
     * @param g          graphics object
     * @param pageFormat page format
     * @param pageIndex  page index
     * @return           constant signifying if page exists
     * @throws PrinterException
     */
    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }
        Graphics2D g2D = (Graphics2D) g;
        g2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2D.scale(0.5, 0.5);
        print(g2D);
        return Printable.PAGE_EXISTS;
    }
    
    /* Export as image 
     * http://stackoverflow.com/a/14992446/2209007 
     */
    public BufferedImage exportAsImage() {
    	Rectangle boundingRectangle = getBoundingRectangle();
    	BufferedImage bImg = new BufferedImage((int) boundingRectangle.getWidth(), (int) boundingRectangle.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = bImg.createGraphics();
        cg.translate(-boundingRectangle.x, -boundingRectangle.y);
        this.paintAll(cg);
        
        return bImg;
    }

    /**
     * Paints the underlying grid on the canvas
     * @param g
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (grid.isEnabled()) {
            grid.updateSize(this);
            grid.drawGrid(g);
        }
    }

    /**
     * Set the cursor type. Options are:
     * - arrow
     * - crosshair
     * - move
     * @param type cursor type
     */
    //TODO These should be an enum
    public void setCursorType(String type) {
        if (type.equals("arrow")) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else if (type.equals("crosshair")) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else if (type.equals("move")) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    /**
     * Updates the canvas boundary when dragging is taking place
     * @param dragStart
     * @param dragEnd
     */
    public void drag(Point dragStart, Point dragEnd) {
        if (dragStart == null) {
            return;
        }
        JViewport viewer = (JViewport) getParent();
        Point offScreen = viewer.getViewPosition();
        if (dragStart.x > dragEnd.x) {
            offScreen.translate(viewer.getWidth(), 0);
        }
        if (dragStart.y > dragEnd.y) {
            offScreen.translate(0, viewer.getHeight());
        }
        offScreen.translate(dragStart.x - dragEnd.x, dragStart.y - dragEnd.y);
        Rectangle r = new Rectangle(offScreen.x, offScreen.y, 1, 1);
        scrollRectToVisible(r);
    }

    /**
     * Remove the component with this id from the canvas
     * @param id
     */
    public void deletePetriNetComponent(String id) {
        PetriNetViewComponent component = petriNetComponents.get(id);
        if (component != null) {
            component.delete();
            remove((Component) component);
        }
        validate();
        repaint();
    }

    /**
     *
     * @return Grid displayed on the canvas
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     *
     * @param handler specifies how the canvas should behave to mouse events
     */
    public void setMouseHandler(MouseInputAdapter handler) {
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
    }

    /**
     * Used to set the bounds of the canvas so that it will expand if components go out of bound
     */
    private class ChangeListener implements PlaceVisitor, TransitionVisitor {
        /**
         * Listens to (x,y) changes in components and updates the canvas width
         * if a place/transition goes out of the current bounds
         */
        private PropertyChangeListener updateListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if (name.equals(Connectable.X_CHANGE_MESSAGE)) {
                    int x = (int) evt.getNewValue();
                    if (x > getWidth()) {
                        updatePreferredSize();
                    }

                }
                if (name.equals(Connectable.Y_CHANGE_MESSAGE)) {
                    int y = (int) evt.getNewValue();
                    if (y > getHeight()) {
                        updatePreferredSize();
                    }

                }
            }
        };

        /**
         * Add the update listener to the place
         * @param place
         */
        @Override
        public void visit(Place place) {
            place.addPropertyChangeListener(updateListener);
        }

        /**
         * Add the update listener to the transition
         * @param transition
         */
        @Override
        public void visit(Transition transition) {
            transition.addPropertyChangeListener(updateListener);
        }
    }
    
    /**
     * Very similar to uk.ac.imperial.pipe.models.petrinet.PetriNet.NameChangeListener
     * Listener for changing a components name in the set it is referenced by
     * @param <T>
     */
    private static class NameChangeListener implements PropertyChangeListener {
        /**
         * Comoponent whose name will change
         */
        private final AbstractPetriNetViewComponent viewComponent;

        /**
         * Component map that houses the component, needs to be updated on name change
         */
        private final Map<String, PetriNetViewComponent> viewComponentMap;

        /**
         * Constructor
         * @param viewComponent
         * @param viewComponentMap
         */
        public NameChangeListener(AbstractPetriNetViewComponent viewComponent, Map<String, PetriNetViewComponent> viewComponentMap) {
            this.viewComponent = viewComponent;
            this.viewComponentMap = viewComponentMap;
        }

        /**
         * If the name/id of the component changes then it is updated in the component map.
         * That is the old key is removed and the compoennt is readded with the new name.
         * @param evt
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(PetriNetComponent.ID_CHANGE_MESSAGE)) {
                String oldId = (String) evt.getOldValue();
                String newId = (String) evt.getNewValue();
                viewComponentMap.remove(oldId);
                viewComponentMap.put(newId, viewComponent);
            }

        }
    }
}


