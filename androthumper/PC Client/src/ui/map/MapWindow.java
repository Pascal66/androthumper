package ui.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import ui.ContextMenu;
import ui.Window;

import constants.Conts;


public class MapWindow extends JFrame{

	final MapApplet app;
	final Window mainWindow;
	private ContextMenu contextMenu;
	
	public MapWindow(Window window, float lat, float lng, Vector<float[]> points){
		mainWindow = window;
		this.setSize(600, 600);
		this.setResizable(false);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(600, 600));
		app = new MapApplet(this,window, lat, lng, points);
		app.setPreferredSize(new Dimension(600, 600));
		panel.add(app);
		this.add(panel);
		
        java.awt.GridBagConstraints gridBagConstraints;

        JPanel jPanel1 = new javax.swing.JPanel();
        JButton startButton = new javax.swing.JButton();
        JButton sendButton = new javax.swing.JButton();
        JButton stopButton = new javax.swing.JButton();
        JButton clearButton = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.GridBagLayout());

        startButton.setText("Start");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(startButton, gridBagConstraints);
        startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.startWaypointDriver();
			}
		});
        
        stopButton.setText("Stop");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(stopButton, gridBagConstraints);
        stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				mainWindow.stopWaypointDriver();
			}
		});

        sendButton.setText("Send waypoints");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(sendButton, gridBagConstraints);
        sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {		
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream(Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE);
					baos.write(Conts.UtilsCodes.DataType.SEND_GPS_WAYPOINTS);
					baos.write(app.getPointsAsByteArray());
					byte[] info = baos.toByteArray();
					byte[] data = new byte[Conts.PacketSize.UTILS_CONTROL_PACKET_SIZE];
					System.arraycopy(info, 0, data, 0, info.length);
					mainWindow.sendGPSWaypointData(data);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
		});

        clearButton.setText("Clear Points");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                app.clearPoints();
            }
        });
        jPanel1.add(clearButton, gridBagConstraints);
		
		this.add(jPanel1,BorderLayout.SOUTH);
		this.pack();
		
		this.setVisible(true);
		app.init();
	}
	
	public void jumpToLocation(float lat, float lng){
		app.jumpToLocation(lat, lng);
	}
	
	public void showMenu(int x, int y){
		System.out.println("here");
		contextMenu = new ContextMenu();
		contextMenu.show(this, x, y);
		System.out.println("shoig");
	}
}
