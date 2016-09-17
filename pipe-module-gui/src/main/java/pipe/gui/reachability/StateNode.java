package pipe.gui.reachability;

public interface StateNode {
	/**
	 * 
	 * @return Label to be displayed on node
	 */
	public String getLabel();


   /**
    *
    * @return Type of node
    */
   public String getNodeType();


   /**
    *
    * @return Part of the tooltip (label and dynamic information is displayed there too)
    */
   public String getToolTip();
   
   /**
    * 
    * @return id
    */
   public int getId();
}
