package pipe.handlers;

import pipe.controllers.AbstractConnectableController;
import pipe.views.TextLabel;
import java.awt.Container;

import javax.swing.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;


/**
 * Handler for connectable views name label
 */
public class ArcLabelHandler extends javax.swing.event.MouseInputAdapter {


    /**
     * Connectable the name label refers to
     */
    private final Container parent;

    /**
     * Name label for the corresponding connectable
     */
    private final TextLabel textLabel;


    /**
     * Constructor
     * @param textLabel name label for the connectable
     * @param connectable connectable with a name label
     * @param controller
     */
    public ArcLabelHandler(TextLabel textLabel, Container parent) {
        this.textLabel = textLabel;
        this.parent = parent;
    }

    /**
     * Passes the event to the parent object with relative coordinates
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent);
    	MouseEvent newEvent = new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(),
                (int) point.getX(), (int) point.getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
        parent.dispatchEvent(newEvent);
        e.consume();
    }
}
