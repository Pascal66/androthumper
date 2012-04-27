package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class MapWindow extends JFrame{

	final MapApplet app;
	final Window mainWindow;
	
	public MapWindow(Window window, float lat, float lng, Vector<float[]> points){
		mainWindow = window;
		this.setSize(600, 600);
		this.setResizable(false);
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(600, 600));
		app = new MapApplet(window, lat, lng, points);
		app.setPreferredSize(new Dimension(600, 600));
		panel.add(app);
		this.add(panel);
		
        java.awt.GridBagConstraints gridBagConstraints;

        JPanel jPanel1 = new javax.swing.JPanel();
        JButton jButton1 = new javax.swing.JButton();
        JButton sendButton = new javax.swing.JButton();
        JButton stopButton = new javax.swing.JButton();
        JButton clearButton = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jButton1.setText("Start");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jButton1, gridBagConstraints);
        
        stopButton.setText("Stop");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(stopButton, gridBagConstraints);

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
				mainWindow.sendGPSWaypointData(app.getPointsAsByteArray());
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
}
