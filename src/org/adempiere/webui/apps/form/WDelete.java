package org.adempiere.webui.apps.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.DeleteEntitiesModel;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;


public class WDelete implements IFormController,EventListener<Event>, ValueChangeListener{
	
	/**
	 *  
	 */
	private static CLogger log = CLogger.getCLogger(WDelete.class);
	private CustomForm form = new CustomForm();
	
	public WDelete()
	{
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "Y");   //  defaults to no
		try
		{
			dynInit();
			zkInit();
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
	}
	
	private Borderlayout mainLayout = new Borderlayout();
	private Panel parameterPanel = new Panel();
	private Panel centerPanel = new Panel();
	private Panel southPanel = new Panel();
	private Grid centerLayout = GridFactory.newGridLayout();
	private Grid parameterLayout = GridFactory.newGridLayout();
	private Grid southLayout = GridFactory.newGridLayout();
	private ConfirmPanel confirmPanel = new ConfirmPanel(true);
	private Label clientLabel = new Label();
	private Label tableLabel = new Label();
	private WTableDirEditor tablePick = null;
	private Combobox clientPick = null;
	private static final int AD_COLUMN_AD_TABLE_ID = 114;
	private Tree tree;
	private Treecols treeCols;
	private Treecol treeCol;
	private Treecol treeCol2;
	private Checkbox dryRun ;
	private Trx m_trx;
	private int m_totalTable;
	private int m_totalDelete;
	private int m_totalUpdate;
	private Integer clientId;
	private HashMap<String, Integer> clientMap = new HashMap<String, Integer>();
	
	private void zkInit() throws Exception
	{
		//Form Init()
		form.appendChild(mainLayout);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		clientLabel.setText(Msg.translate(Env.getCtx(), "AD_Client_ID"));
		tableLabel.setText(Msg.translate(Env.getCtx(), "AD_Table_ID"));
		dryRun = new Checkbox("Dry Run");
		dryRun.setChecked(true);
		
		parameterPanel.appendChild(parameterLayout);
		North north = new North();
		north.setStyle("border: none");
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);
		Rows rows = null;
		Row row = null;
		parameterLayout.setWidth("100%");		
		rows = parameterLayout.newRows();
		row = rows.newRow();
		row.appendChild(clientLabel.rightAlign());
		clientPick.setHflex("true");
		row.appendChild(clientPick);
		row.appendChild(tableLabel.rightAlign());
		tablePick.getComponent().setHflex("true");
		row.appendChild(tablePick.getComponent());
		row.appendChild(dryRun);
		
		centerPanel.appendChild(centerLayout);
		centerLayout.setWidth("100%");			
		Center center = new Center();
		mainLayout.appendChild(center);
		center.setStyle("border: none");
		center.appendChild(centerPanel);
		tree = new Tree();
		tree.setHflex("true");
		treeCols = new Treecols();
		treeCol = new Treecol("");
		treeCol2 = new Treecol();
		centerPanel.appendChild(tree);
		treeCols.appendChild(treeCol);
		treeCols.appendChild(treeCol2);
        tree.appendChild(treeCols); 		
		//center.setFlex(true);
		center.setAutoscroll(true);
		
		South south = new South();
		south.appendChild(southPanel);
		southPanel.appendChild(southLayout);		
		southPanel.setWidth("100%");
		mainLayout.appendChild(south);
		Rows rows2 = southLayout.newRows();		
		Row south_row = rows2.newRow();		
		south_row.appendChild(confirmPanel);
		confirmPanel.addActionListener(this);
		
		clientPick.addEventListener(Events.ON_SELECT, this);
		
	} 
	
	public void dynInit() throws Exception
	{
		
		// Client Pick
		String sql = "SELECT AD_Client_ID, Name FROM AD_Client WHERE AD_Client_ID <> 0";
		clientPick = new Combobox();

		PreparedStatement pstmt1 = DB.prepareStatement(sql, null);
		ResultSet rs1 = null;

		String clientName = null;
		Integer clientID = null;

		try 
		{
			rs1 = pstmt1.executeQuery();
			while (rs1.next()) 
			{
				clientID = new Integer(rs1.getInt(1));
				clientName = new String(rs1.getString(2));
				clientPick.appendItem(clientName, clientID);
				clientMap.put(clientName, clientID);
			}

		} 
		catch (SQLException e) 
		{
			log.log(Level.SEVERE, "", e);
		} 
		finally 
		{
			DB.close(rs1);
			DB.close(pstmt1);
		}

		// Table Pick
		MLookup lookupTable = MLookupFactory.get(Env.getCtx(), form.getWindowNo(), AD_COLUMN_AD_TABLE_ID, DisplayType.TableDir,
				Env.getLanguage(Env.getCtx()), "AD_Table_ID", 0,
				false, "AD_Table.IsView='N' AND AD_Table.AccessLevel!='4'");
		tablePick = new WTableDirEditor("AD_Table_ID", true, false, true, lookupTable);
		tablePick.setValue(new Integer((Integer) Env.getContextAsInt(Env.getCtx(), "$AD_Table_ID")));  // TODO: What's this???
		tablePick.addValueChangeListener(this);
	}   //  dynInit
	
	
	
	private void createNodes(DeleteEntitiesModel tableData, Treechildren itemChildren) 
	{
		DeleteEntitiesModel currentNode = tableData;

		String sql = "SELECT t.TableName, c.ColumnName, c.IsMandatory, t.AD_Table_ID "
			+ "FROM AD_Table t"
			+ " INNER JOIN AD_Column c ON (t.AD_Table_ID=c.AD_Table_ID) "
			+ "WHERE t.IsView='N' AND t.IsActive='Y'"
			+ " AND c.ColumnName NOT IN ('CreatedBy', 'UpdatedBy') "
				+ " AND t.TableName!=?"     // not the same table
				+ " AND c.ColumnName=?" 	//	#1 - direct
				+ " AND c.IsKey='N' AND c.ColumnSQL IS NULL "
			+ "ORDER BY t.LoadSeq DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String keyCol = currentNode.getTableName() + "_ID";
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, currentNode.getTableName());
			pstmt.setString(2, keyCol);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				String tableName = rs.getString(1);
				String columnName = rs.getString(2);
				boolean isMandatory = "Y".equals(rs.getString(3));
				int tableId = rs.getInt(4);

				DeleteEntitiesModel data = new DeleteEntitiesModel(tableName, isMandatory, clientId);
				data.setJoinColumn(columnName);
				data.setWhereClause(" EXISTS (SELECT 1 FROM " + currentNode.getTableName() 
						+ " WHERE " + currentNode.getTableName() + "." + currentNode.getTableName() + "_ID" // + currentNode.getJoinColumn() 
						+ "=" + data.getTableName() + "." + data.getJoinColumn() + " AND " + currentNode.getWhereClause() + ") ");

				int count = data.getCount();
				if ( count > 0 )
				{						
					Treeitem treeitem = new Treeitem();
					itemChildren.appendChild(treeitem);
					boolean isTrxWin = isTrxWin(tableId);
					treeitem.setLabel(data.getTableName()/*+"."+data.getJoinColumn()*/+" ("+count+")" + (isTrxWin ? " $" : ""));
					treeitem.setValue(data);			            
				}
				else
					log.log(Level.FINE, "No records:" + data.getTableName());
			}
			
		} 
		catch (SQLException e) 
		{
			log.log(Level.INFO, sql);
			throw new AdempiereException("Couldn't load child tables", e);
		}
		finally
		{
			DB.close(rs, pstmt);
		}		

		Collection<Treeitem> collItemChild = (Collection<Treeitem>) itemChildren.getItems();
		Iterator<Treeitem> it = collItemChild.iterator();
		
		while ( it.hasNext() )
		{
			Treeitem node = it.next();
			Treeitem rootOfNode = node.getParentItem();
			if ( rootOfNode != null && rootOfNode.getParentItem() != null &&  rootOfNode.getParentItem().equals(node))
			{
				log.log(Level.WARNING, "Loop detected, escaping.");
				break;
			}
			else if ( ((DeleteEntitiesModel) node.getValue()).isMandatoryLink() )
			{	
				DeleteEntitiesModel itemTableData = (DeleteEntitiesModel) node.getValue();
				Treechildren nodeChild = new Treechildren();
				createNodes(itemTableData, nodeChild);
				
				if(nodeChild.getItemCount() != 0 )
				{
					node.appendChild(nodeChild);
				}
			}	
		}
	}	// createNodes(arg1, arg2)	

	@Override
	public void onEvent(Event e) throws Exception 
	{
		boolean commit = ! dryRun.isChecked();
		
		if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			dispose();
		}
		
		else if (e.getTarget().equals(clientPick)) {

			String clientIDStr = clientPick.getSelectedItem().getLabel();
			clientId = clientMap.get(clientIDStr);
			
			Object value=tablePick.getValue();
			generateTree(value,clientId);
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			//String clientIDStr = clientPick.getText();
			//Integer clientId = clientMap.get(clientIDStr);
			
			Object objTableID = tablePick.getValue();
			int tableId = 0;
			if ( objTableID != null )
				tableId = (Integer) objTableID;
			
			if (tableId == 0 || clientId == null)
			{
				FDialog.error(form.getWindowNo(), "Error", 
						"Select client and base table for cascade delete.");
			}
			else
			{
				m_totalTable = 0;
				m_totalDelete = 0;
				m_totalUpdate = 0;
				m_trx = Trx.get(Trx.createTrxName("delete"), true);
				String errorMsg = "";
				StringBuilder logMsg = new StringBuilder();
				try 
				{
					Collection<Treeitem> items = tree.getItems();	
					Iterator<Treeitem> nodes = items.iterator();
					
					Stack<DeleteEntitiesModel> stack = new Stack<DeleteEntitiesModel>();
					
					while ( nodes.hasNext() )
					{
						stack.push((DeleteEntitiesModel) (nodes.next().getValue()));
					}
					
					while ( !stack.empty() )
					{
						DeleteEntitiesModel tableData = stack.pop();
						int cnt;
						if (tableData.isMandatoryLink()) {
							cnt = tableData.delete(m_trx);
							if (cnt > 0) {
								m_totalDelete += cnt;
								m_totalTable++;
								logMsg.append(tableData.getTableName()).append(" -").append(cnt).append("<br>");
							}
						} else {
							cnt = tableData.update(m_trx);
							if (cnt > 0) {
								m_totalUpdate += cnt;
								m_totalTable++;
								logMsg.append(tableData.getTableName()).append(" =").append(cnt).append("<br>");
							}
						}
					}
					if  ( commit )
						m_trx.commit(true);
					else
						m_trx.rollback(true);
				}
				catch (Exception ex) 
				{
					errorMsg = ex.getLocalizedMessage();
					log.log(Level.WARNING, "Cascade delete failed.", ex);
					m_totalDelete = 0;
					m_totalUpdate = 0;
					m_trx.rollback();
					FDialog.error(form.getWindowNo(), "DeleteError", errorMsg);
					return;
				}
				finally 
				{
					m_trx.close();
				}
				logMsg.insert(0,
					new StringBuilder()
						.append(" @AD_Table_ID@: #")
						.append(m_totalTable)
						.append(" @Updated@: #")
						.append(m_totalUpdate)
						.append(" @Deleted@: #")
						.append(m_totalDelete)
						.append("<br><br>"));

				FDialog.info(form.getWindowNo(), form, commit ? "DeleteSuccess" : "Test",
						Msg.parseTranslation(Env.getCtx(), logMsg.toString()));

				Object value=tablePick.getValue();
				generateTree(value,clientId);
				//dispose();

			}
		}		
	}	// onEvent
	
	/**
	 * Dispose
	 */
	
	public void dispose() {
		
		SessionManager.getAppDesktop().closeActiveWindow();
	} // dispose

	@Override
	public void valueChange(ValueChangeEvent e) 
	{
		log.info(e.getPropertyName() + "=" + e.getNewValue());
		
		String name = e.getPropertyName();
		Object value = e.getNewValue();
		
		//String clientIDStr = clientPick.getText();
		//Integer clientId = clientMap.get(clientIDStr);
		
		log.config(name + "=" + value);

		if (name.equals("AD_Table_ID")) {
			
			generateTree(value,clientId);
		}
	}	// ValueChange
	
	private void generateTree(Object value,Integer clientID) {
		
		if (value == null)
			return;
		
		Integer selectedTableID = ((Integer) value).intValue();
		
		if (selectedTableID == 0 || clientId == null) {
			FDialog.error(form.getWindowNo(), "ParameterError", "Table or Client cannot be Null.");
			return;
		}

		MTable table = MTable.get(Env.getCtx(), selectedTableID);

		DeleteEntitiesModel data = new DeleteEntitiesModel(table.getTableName(), true, clientId);
		if (table.getKeyColumns().length > 0)
			data.setJoinColumn(table.getKeyColumns()[0]);
		data.setWhereClause(" " + data.getTableName() + ".AD_Client_ID=?");
		
		tree.clear();
		if((tree.getChildren()).size() > 1) {
			
			List<Component> treePreviousChild = tree.getChildren();
			tree.removeChild((Treechildren) treePreviousChild.get(1));
		}
		
		Treechildren rootTreeChild = new Treechildren();				
		Treeitem rootTreeItem = new Treeitem();
		rootTreeItem.setValue(data);
		int count = data.getCount();
		boolean isTrxWin = isTrxWin(table.getAD_Table_ID());
		rootTreeItem.setLabel(data.getTableName()/*+"."+data.getJoinColumn()*/+" ("+count+")" + (isTrxWin ? " $" : ""));
	
		Treechildren rootTreeItemChild = new Treechildren();
		createNodes(data, rootTreeItemChild);
				
		rootTreeItem.appendChild(rootTreeItemChild);		
		rootTreeChild.appendChild(rootTreeItem);
		tree.appendChild(rootTreeChild);
	}

	private boolean isTrxWin(int tableId) {
		final String sql = "SELECT COUNT(*) FROM AD_Window w JOIN AD_Tab t ON t.AD_Window_ID=w.AD_Window_ID WHERE t.AD_Table_ID=? AND w.WindowType='T' AND t.IsActive='Y' AND w.IsActive='Y'";
		int cntTrxWin = DB.getSQLValue(null, sql, tableId);
		return (cntTrxWin > 0);
	}
	
	public ADForm getForm()
	{
		return form;
	}

} 	// WDelete