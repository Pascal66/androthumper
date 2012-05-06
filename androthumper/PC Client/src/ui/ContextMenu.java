package ui;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class ContextMenu extends JPopupMenu {

	JMenuItem menuItem;
	
	public ContextMenu(){
		menuItem = new JMenuItem("click me");
		this.add(menuItem);
	}
}
