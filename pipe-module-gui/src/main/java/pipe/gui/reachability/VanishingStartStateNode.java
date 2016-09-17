package pipe.gui.reachability;

import net.sourceforge.jpowergraph.defaults.DefaultNode;
import net.sourceforge.jpowergraph.painters.node.ShapeNodePainter;
import net.sourceforge.jpowergraph.swtswinginteraction.color.JPowerGraphColor;

/**
 * Node used when displaying the readability graph to represent a vanishing node that is the start.
 */
class VanishingStartStateNode extends VanishingStateNode implements StateNode {

	private static final JPowerGraphColor BG_COLOR = new JPowerGraphColor(102, 102, 102);

    /**
     * Text color
     */
    private static final JPowerGraphColor TEXT_COLOR = JPowerGraphColor.BLACK;

    /**
     * Circurlar shape
     */
    private static final ShapeNodePainter SHAPE_NODE_PAINTER = new ShapeNodePainter(
            ShapeNodePainter.ELLIPSE, BG_COLOR, BG_COLOR, TEXT_COLOR);

    /**
     * Creates a new node instance.
     * @param label    the node id.
     * @param tooltip  the state tooltip text
     */
    VanishingStartStateNode(String label, String tooltip, int id){
        super(label, tooltip, id);
    }
    
    /**
    *
    * @return type of node
    */
    @Override
    public String getNodeType(){
        return "Vanishing start state";
    }
    
    /**
    *
    * @return the painter
    */
    public static ShapeNodePainter getShapeNodePainter(){
        return SHAPE_NODE_PAINTER;
    }
}
