package pipe.gui.reachability;

import net.sourceforge.jpowergraph.defaults.DefaultNode;
import net.sourceforge.jpowergraph.painters.node.ShapeNodePainter;
import net.sourceforge.jpowergraph.swtswinginteraction.color.JPowerGraphColor;

/**
 * Node used when displaying the readability graph to represent a tangile node.
 * Tanglible nodes are displayed in a light red color.
 */
class TangibleStateNode extends DefaultNode implements StateNode {

    /**
     * Name to display within the node
     */
    private final String label;

    /**
     * Tooltipof the node
     */
    private final String toolTip;
    
    /**
     * id of node
     */
    private final int id;

    /**
     * Background color
     */
    private static final JPowerGraphColor BG_COLOR = new JPowerGraphColor(255, 102, 102);

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
    TangibleStateNode(String label, String tooltip, int id){
        this.label = label;
        this.toolTip = tooltip;
        this.id = id;
    }


    /**
     *
     * @return name label of the node
     */
    @Override
    public String getLabel() {
        return label;
    }


    /**
     *
     * @return type of node
     */
    @Override
    public String getNodeType(){
        return "Tangible state";
    }


    /**
     *
     * @return tooptip message for this node
     */
    public String getToolTip(){
        return toolTip;
    }
    
    /**
     * 
     * @return id
     */
    @Override
    public int getId() {
    	return id;
    }


    /**
     *
     * @return the painter
     */
    public static ShapeNodePainter getShapeNodePainter(){
        return SHAPE_NODE_PAINTER;
    }
}
