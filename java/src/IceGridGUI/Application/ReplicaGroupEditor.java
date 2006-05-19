// **********************************************************************
//
// Copyright (c) 2003-2006 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
package IceGridGUI.Application;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import IceGrid.*;
import IceGridGUI.*;

class ReplicaGroupEditor extends Editor
{
    protected void applyUpdate()
    {
	ReplicaGroup replicaGroup = (ReplicaGroup)_target;
	Root root = replicaGroup.getRoot();

	root.disableSelectionListener();
	try
	{
	    if(replicaGroup.isEphemeral())
	    {
		ReplicaGroups replicaGroups = (ReplicaGroups)replicaGroup.getParent();
		writeDescriptor();
		ReplicaGroupDescriptor descriptor = 
		    (ReplicaGroupDescriptor)replicaGroup.getDescriptor();
		replicaGroup.destroy(); // just removes the child
		
		try
		{
		    replicaGroups.tryAdd(descriptor, true);
		}
		catch(UpdateFailedException e)
		{
		    //
		    // Add back ephemeral child
		    //
		    try
		    {
			replicaGroups.insertChild(replicaGroup, true);
		    }
		    catch(UpdateFailedException die)
		    {
			assert false;
		    }
		    root.setSelectedNode(replicaGroup);
		    
		    JOptionPane.showMessageDialog(
			root.getCoordinator().getMainFrame(),
			e.toString(),
			"Apply failed",
			JOptionPane.ERROR_MESSAGE);
		    return;
		}

		//
		// Success
		//
		_target = replicaGroups.findChildWithDescriptor(descriptor);
		root.updated();
	    }
	    else if(isSimpleUpdate())
	    {
		writeDescriptor();
		root.updated();
		replicaGroup.getEditable().markModified();
	    }
	    else
	    {
		//
		// Save to be able to rollback
		//
		Object savedDescriptor = replicaGroup.saveDescriptor();
		ReplicaGroups replicaGroups = (ReplicaGroups)replicaGroup.getParent();
		writeDescriptor();
		ReplicaGroupDescriptor descriptor = 
		    (ReplicaGroupDescriptor)replicaGroup.getDescriptor();
		
		replicaGroups.removeChild(replicaGroup);	    
		try
		{
		    replicaGroups.tryAdd(descriptor, false);
		}
		catch(UpdateFailedException e)
		{
		    //
		    // Restore all
		    //
		    try
		    {
			replicaGroups.insertChild(replicaGroup, true);
		    }
		    catch(UpdateFailedException die)
		    {
			assert false;
		    }
		    replicaGroup.restoreDescriptor(savedDescriptor);
		    root.setSelectedNode(_target);

		    JOptionPane.showMessageDialog(
			root.getCoordinator().getMainFrame(),
			e.toString(),
			"Apply failed",
			JOptionPane.ERROR_MESSAGE);
		    return;
		}
		    
		//
		// Success
		//

		// replaced by brand new ReplicaGroup
		replicaGroups.getEditable().
		    removeElement(replicaGroup.getId(), 
				  replicaGroup.getEditable(), ReplicaGroup.class); 

		_target = replicaGroups.findChildWithDescriptor(descriptor);
		root.updated();
		root.setSelectedNode(_target);
	    }
	    
	    root.getCoordinator().getCurrentTab().showNode(_target);
	    _applyButton.setEnabled(false);
	    _discardButton.setEnabled(false);
	}
	finally
	{
	    root.enableSelectionListener();
	}
    }

    Utils.Resolver getDetailResolver()
    {
	Root root = _target.getRoot();

	if(root.getCoordinator().substitute())
	{
	    return root.getResolver();
	}
	else
	{
	    return null;
	}
    }

    ReplicaGroupEditor()
    {
	_objects = new MapField(this, "Identity", "Type", true);
	
	//
	// load balancing
	//
	_loadBalancing.addItemListener(new ItemListener()
	    {
		public void itemStateChanged(ItemEvent e)
		{
		    if(e.getStateChange() == ItemEvent.SELECTED)
		    {
			updated();

			Object item = e.getItem();
			_nReplicasLabel.setVisible(item != RETURN_ALL);
			_nReplicas.setVisible(item != RETURN_ALL);
			
			_loadSampleLabel.setVisible(item == ADAPTIVE);
			_loadSample.setVisible(item == ADAPTIVE);
		    }
		}
	    });
	_loadBalancing.setToolTipText(
	    "Specifies how IceGrid selects adapters when resolving a replica group ID");

	//
	// Associate updateListener with various fields
	//
	_id.getDocument().addDocumentListener(_updateListener);
	_id.setToolTipText("Must be unique within this IceGrid deployment");

	_description.getDocument().addDocumentListener(_updateListener);
	_description.setToolTipText(
	    "An optional description for this replica group");

	_nReplicas.getDocument().addDocumentListener(_updateListener);
	_nReplicas.setToolTipText("<html>IceGrid returns the endpoints of "
				  + "up to <i>number</i> adapters<br>"
				  + "when resolving a replica group ID</html>");

	_loadSample.setEditable(true);
	JTextField loadSampleTextField = (JTextField)
	    _loadSample.getEditor().getEditorComponent();
	loadSampleTextField.getDocument().addDocumentListener(_updateListener);
	_loadSample.setToolTipText(
	    "Use the load average or CPU usage over the last 1, 5 or 15 minutes?");

    }
    
    void writeDescriptor()
    {
	ReplicaGroupDescriptor descriptor = 
	    (ReplicaGroupDescriptor)getReplicaGroup().getDescriptor();

	descriptor.id = _id.getText();
	descriptor.description = _description.getText();
	descriptor.objects = AdapterEditor.mapToObjectDescriptorSeq(_objects.get());
	
	Object loadBalancing = _loadBalancing.getSelectedItem();
	if(loadBalancing == RETURN_ALL)
	{
	    descriptor.loadBalancing = null;
	}
	else if(loadBalancing == RANDOM)
	{
	    descriptor.loadBalancing = new RandomLoadBalancingPolicy(
		_nReplicas.getText());
	}
	else if(loadBalancing == ROUND_ROBIN)
	{
	    descriptor.loadBalancing = new RoundRobinLoadBalancingPolicy(
		_nReplicas.getText());
	}
	else if(loadBalancing == ADAPTIVE)
	{
	    descriptor.loadBalancing = new AdaptiveLoadBalancingPolicy(
		_nReplicas.getText(), 
		_loadSample.getSelectedItem().toString());
	}
	else
	{
	    assert false;
	}
    }	    
    
    boolean isSimpleUpdate()
    {
	ReplicaGroupDescriptor descriptor = 
	    (ReplicaGroupDescriptor)getReplicaGroup().getDescriptor();
	return descriptor.id.equals(_id.getText());
    }

    protected void appendProperties(DefaultFormBuilder builder)
    {
	builder.append("Replica Group ID" );
	builder.append(_id, 3);
	builder.nextLine();
	
	builder.append("Description");
	builder.nextLine();
	builder.append("");
	builder.nextRow(-2);
	CellConstraints cc = new CellConstraints();
	JScrollPane scrollPane = new JScrollPane(_description);
	builder.add(scrollPane, 
		    cc.xywh(builder.getColumn(), builder.getRow(), 3, 3));
	builder.nextRow(2);
	builder.nextLine();

	builder.append("Registered Objects");
	builder.nextLine();
	builder.append("");
	builder.nextLine();
	builder.append("");
	builder.nextLine();
	builder.append("");
	builder.nextRow(-6);
	scrollPane = new JScrollPane(_objects);
	builder.add(scrollPane, 
		    cc.xywh(builder.getColumn(), builder.getRow(), 3, 7));
	builder.nextRow(6);
	builder.nextLine();

	builder.append("Load Balancing Policy");
	builder.append(_loadBalancing, 3);
	builder.nextLine();
	_nReplicasLabel = builder.append("How many Adapters?");
	builder.append(_nReplicas, 3);
	builder.nextLine();
	_loadSampleLabel = builder.append("Load Sample");
	builder.append(_loadSample, 3);
	builder.nextLine();
    }

    protected void buildPropertiesPanel()
    {
	super.buildPropertiesPanel();
	_propertiesPanel.setName("Replica Group Properties");
    }

    void show(ReplicaGroup replicaGroup)
    {
	//
	// Make sure everything is built
	//
	getProperties();

	detectUpdates(false);
	_target = replicaGroup;

	Utils.Resolver resolver = getDetailResolver();
	boolean isEditable = (resolver == null);
	
	ReplicaGroupDescriptor descriptor = 
	    (ReplicaGroupDescriptor)replicaGroup.getDescriptor();
	
	_id.setText(descriptor.id);
	_id.setEditable(isEditable);
	
	_description.setText(
	    Utils.substitute(descriptor.description, resolver));
	_description.setEditable(isEditable);
	_description.setOpaque(isEditable);

	_objects.set(AdapterEditor.objectDescriptorSeqToMap(descriptor.objects), resolver, isEditable);

	_loadBalancing.setEnabled(true);

	if(descriptor.loadBalancing == null)
	{
	    _loadBalancing.setSelectedItem(RETURN_ALL);
	    _nReplicas.setText("1");
	    _loadSample.setSelectedItem("1");
	}
	else if(descriptor.loadBalancing instanceof RandomLoadBalancingPolicy)
	{
	    _loadBalancing.setSelectedItem(RANDOM);
	    _nReplicas.setText(
		Utils.substitute(descriptor.loadBalancing.nReplicas, resolver));
	    _loadSample.setSelectedItem("1");
	}
	else if(descriptor.loadBalancing instanceof RoundRobinLoadBalancingPolicy)
	{
	    _loadBalancing.setSelectedItem(ROUND_ROBIN);
	    _nReplicas.setText(
		Utils.substitute(descriptor.loadBalancing.nReplicas, resolver));
	    _loadSample.setSelectedItem("1");
	}
	else if(descriptor.loadBalancing instanceof AdaptiveLoadBalancingPolicy)
	{
	    _loadBalancing.setSelectedItem(ADAPTIVE);
	    _nReplicas.setText(
		Utils.substitute(descriptor.loadBalancing.nReplicas, resolver));

	    _loadSample.setSelectedItem(
		Utils.substitute(
		    ((AdaptiveLoadBalancingPolicy)descriptor.loadBalancing).loadSample,
		    resolver));
	}
	else
	{
	    assert false;
	}
	_nReplicas.setEditable(isEditable);
	_loadSample.setEditable(isEditable);
	_loadBalancing.setEnabled(isEditable);

	_applyButton.setEnabled(replicaGroup.isEphemeral());
	_discardButton.setEnabled(replicaGroup.isEphemeral());	  
	detectUpdates(true);
    }

    private ReplicaGroup getReplicaGroup()
    {
	return (ReplicaGroup)_target;
    }

    static private String RETURN_ALL = "Return all";
    static private String RANDOM = "Random";
    static private String ROUND_ROBIN = "Round-robin";
    static private String ADAPTIVE = "Adaptive";

    private JTextField _id = new JTextField(20);
    private JTextArea _description = new JTextArea(3, 20);

    private JComboBox _loadBalancing = new JComboBox(new Object[]
	{ADAPTIVE, RANDOM, RETURN_ALL, ROUND_ROBIN}); 
    
    private JLabel _nReplicasLabel;
    private JTextField _nReplicas = new JTextField(20);

    private JLabel _loadSampleLabel;
    private JComboBox _loadSample = new JComboBox(new Object[]
	{"1", "5", "15"});
    
    private MapField _objects;
}
