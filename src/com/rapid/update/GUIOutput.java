package com.rapid.update;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class GUIOutput extends JPanel implements UpdateOutput {
	
	private static final long serialVersionUID = 1L;
	
	JTextArea _log;
	
	public GUIOutput() {
		
		super(new BorderLayout());
		
		_log = new JTextArea(20,50);
        _log.setMargin(new Insets(5,5,5,5));
        _log.setEditable(false);
              
        JScrollPane logScrollPane = new JScrollPane(_log);        
  
        add(logScrollPane, BorderLayout.CENTER);
                				
	}

	@Override
	public void log(final String message) {
		SwingUtilities.invokeLater( new Runnable() {
            public void run() {
            	_log.append(message + "\n");
            }
        });  
		
	}

}
