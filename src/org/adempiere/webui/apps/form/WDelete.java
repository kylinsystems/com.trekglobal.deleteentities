package org.adempiere.webui.apps.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.DeleteEntitiesModel;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.West;


public class WDelete implements IFormController,EventListener<Event>, WTableModelListener {
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
	private Panel eastPanel = new Panel();
	private Panel westPanel = new Panel();
	private Panel southPanel = new Panel();
	private Grid eastLayout = GridFactory.newGridLayout();
	private Grid westLayout = GridFactory.newGridLayout();
	private Grid parameterLayout = GridFactory.newGridLayout();
	private Grid southLayout = GridFactory.newGridLayout();
	private ConfirmPanel confirmPanel = new ConfirmPanel(true);
	private Label clientLabel = new Label();
	private Combobox clientPick = null;
	private Tree tree;
	private Treechildren rootTreeChild;
	private Treecols treeCols;
	private Treecol treeCol;
	private Checkbox dryRun ;
	private Trx m_trx;
	private int m_totalTable;
	private int m_totalDelete;
	private int m_totalUpdate;
	private Integer clientId;
	private HashMap<String, Integer> clientMap = new HashMap<String, Integer>();
	private Hlayout statusBar = new Hlayout();

	private WListbox m_tableListbox;
	private int m_selected;

	private void zkInit() throws Exception
	{
		//Form Init()
		form.appendChild(mainLayout);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		clientLabel.setText(Msg.translate(Env.getCtx(), "AD_Client_ID"));
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
		row.appendChild(dryRun);

		westPanel.appendChild(westLayout);
		West west = new West();
		west.setWidth("50%");
		mainLayout.appendChild(west);
		west.setStyle("border: none");
		west.appendChild(westPanel);
		m_tableListbox.setWidth("100%");
		westPanel.appendChild(m_tableListbox);
        west.setAutoscroll(true);

		eastPanel.appendChild(eastLayout);
		East east = new East();
		east.setWidth("50%");
		mainLayout.appendChild(east);
		east.setStyle("border: none");
		east.appendChild(eastPanel);
		tree = new Tree();
		tree.setWidth("100%");
		treeCols = new Treecols();
		treeCol = new Treecol("");
		eastPanel.appendChild(tree);
		treeCols.appendChild(treeCol);
        tree.appendChild(treeCols);
        east.setAutoscroll(true);

        rootTreeChild = new Treechildren();
		tree.appendChild(rootTreeChild);

		South south = new South();
		south.appendChild(southPanel);
		southPanel.appendChild(southLayout);
		southPanel.setWidth("100%");
		mainLayout.appendChild(south);
		Rows rows2 = southLayout.newRows();
		Row south_row = rows2.newRow();
		south_row.appendChild(statusBar);
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

		m_tableListbox = ListboxFactory.newDataTable();
	}   //  dynInit



	private void createNodes(DeleteEntitiesModel tableData, Treechildren itemChildren)
	{
		DeleteEntitiesModel currentNode = tableData;

		final String sql = ""
				+ "SELECT q.tablename, "
				+ "       q.columnname, "
				+ "       q.ismandatory, "
				+ "       q.ad_table_id "
				+ "FROM   (SELECT t.ad_table_id, "
				+ "               t.tablename, "
				+ "               c.ad_column_id, "
				+ "               c.columnname, "
				+ "               r.ad_reference_id, "
				+ "               r.NAME, "
				+ "               c.ad_reference_value_id, "
				+ "               CASE "
				+ "                 WHEN c.ad_reference_id = 25 THEN 'C_ValidCombination' "
				+ "                 WHEN c.ad_reference_id = 33 THEN 'S_ResourceAssignment' "
				+ "                 WHEN c.ad_reference_id = 32 THEN 'AD_Image' "
				+ "                 WHEN c.ad_reference_id = 21 THEN 'C_Location' "
				+ "                 WHEN c.ad_reference_id = 31 THEN 'M_Locator' "
				+ "                 WHEN c.ad_reference_id = 35 THEN 'M_AttributeSetInstance' "
				+ "                 WHEN c.ad_reference_id = 53370 THEN 'AD_Chart' "
				+ "                 WHEN c.ad_reference_id = 19 "
				+ "                       OR ( c.ad_reference_id = 30 "
				+ "                            AND c.ad_reference_value_id IS NULL ) THEN Cast(Substr(columnname, 1, Length(columnname) - 3) AS VARCHAR(40)) "
				+ "                 WHEN c.ad_reference_id = 18 "
				+ "                       OR ( c.ad_reference_id = 30 "
				+ "                            AND c.ad_reference_value_id IS NOT NULL ) THEN tf.tablename "
				+ "                 ELSE '?' "
				+ "               END AS foreigntable, "
				+ "               c.ismandatory, "
				+ "               t.loadseq "
				+ "        FROM   ad_column c "
				+ "               JOIN ad_table t ON c.ad_table_id = t.ad_table_id "
				+ "               JOIN ad_reference r ON r.ad_reference_id = c.ad_reference_id "
				+ "               LEFT JOIN ad_ref_table rt ON c.ad_reference_value_id = rt.ad_reference_id "
				+ "               LEFT JOIN ad_table tf ON rt.ad_table_id = tf.ad_table_id "
				+ "        WHERE  c.ad_reference_id IN ( 18, 19, 30, 25, 33, 32, 21, 31, 35, 53370 ) "
				+ "               AND t.isactive = 'Y' "
				+ "               AND t.isview = 'N' "
				+ "               AND c.isactive = 'Y') q "
				+ "WHERE  q.foreigntable = ? "
				+ "ORDER  BY q.loadseq DESC";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, currentNode.getTableName());
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				String tableName = rs.getString(1);
				String columnName = rs.getString(2);
				boolean isMandatory = "Y".equals(rs.getString(3));

				DeleteEntitiesModel data = new DeleteEntitiesModel(tableName, isMandatory, clientId);
				data.setJoinColumn(columnName);
				data.setWhereClause(" EXISTS (SELECT 1 FROM " + currentNode.getTableName()
						+ " WHERE " + currentNode.getTableName() + "." + currentNode.getTableName() + "_ID" // + currentNode.getJoinColumn()
						+ "=" + data.getTableName() + "." + data.getJoinColumn() + " AND " + currentNode.getWhereClause() + ") ");

				int count = getCountFromModel(tableName);
				if ( count > 0 )
				{
					if (! alreadyOnTree(data.getTableName(), rootTreeChild.getChildren())) {
						Treeitem treeitem = new Treeitem();
						itemChildren.appendChild(treeitem);
						treeitem.setLabel(data.getTableName()+"."+data.getJoinColumn());
						treeitem.setValue(data);
					}
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

	private boolean alreadyOnTree(String tableName, List<Component> list) {
		for (Component co : list) {
			if (co instanceof Treeitem) {
				Treeitem ti = (Treeitem) co;
				DeleteEntitiesModel dem = ti.getValue();
				if (dem.getTableName().equals(tableName)) {
					return true;
				}
				if (ti.getChildren().size() > 0) {
					if (alreadyOnTree(tableName, ti.getChildren())) {
						return true;
					}
				}
			} else if (co instanceof Treechildren) {
				Treechildren tc = (Treechildren) co;
				if (tc.getChildren().size() > 0) {
					if (alreadyOnTree(tableName, tc.getChildren())) {
						return true;
					}
				}
			}
		}
		return false;
	}

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

			generateTable(clientId);
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			if (clientId == null)
			{
				FDialog.error(form.getWindowNo(), "Error",
						"Select client for cascade delete.");
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

				if (commit) {
					generateTable(clientId);
				}
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

	private void generateTree(int selectedTableID, int clientID) {
		if (selectedTableID <= 0 || clientId <= 0) {
			FDialog.error(form.getWindowNo(), "ParameterError", "Table or Client cannot be Null.");
			return;
		}

		MTable table = MTable.get(Env.getCtx(), selectedTableID);

		DeleteEntitiesModel data = new DeleteEntitiesModel(table.getTableName(), true, clientId);
		if (table.getKeyColumns().length > 0)
			data.setJoinColumn(table.getKeyColumns()[0]);
		data.setWhereClause(" " + data.getTableName() + ".AD_Client_ID=?");

		// Special label for root node (bold)
		Label nodeLabel = new Label(data.getTableName());
		nodeLabel.setStyle("font-weight: bold;"); // background-color: #90EE90;");
		Div div = new Div();
		div.setStyle("display:inline;");
		div.appendChild(nodeLabel);

		Treeitem rootTreeItem = new Treeitem();
		rootTreeItem.setValue(data);

		Treerow treerow = new Treerow();
		rootTreeItem.appendChild(treerow);				
		Treecell treecell = new Treecell();
		treerow.appendChild(treecell);
		treecell.appendChild(div);

		Treechildren rootTreeItemChild = new Treechildren();
		rootTreeItem.appendChild(rootTreeItemChild);
		rootTreeChild.appendChild(rootTreeItem);

		createNodes(data, rootTreeItemChild);
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

	private void generateTable(int clientId) {
		clearTree();
		m_selected = 0;

		m_tableListbox.clear();
		m_tableListbox.getModel().removeTableModelListener(this);
		//	Header
		Vector<String> columnNames = new Vector<String>();
		columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
		columnNames.add(Msg.translate(Env.getCtx(), "TableName"));
		columnNames.add(Msg.translate(Env.getCtx(), "Counter"));
		columnNames.add(Msg.translate(Env.getCtx(), "TrxName"));

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		final String sql = "SELECT TableName, AD_Table_ID FROM AD_Table WHERE IsActive='Y' AND IsView='N' AND AccessLevel!='4' ORDER BY TableName";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String tableName = rs.getString(1);
				int tableId = rs.getInt(2);
				DeleteEntitiesModel dem = new DeleteEntitiesModel(tableName, true, clientId);
				dem.setWhereClause(" " + tableName + ".AD_Client_ID=?");
				int cnt = dem.getCount();
				boolean isTrx = isTrxWin(tableId);

				if (cnt > 0) {
					Vector<Object> line = new Vector<Object>(4);
					line.add(new Boolean(false));  //  0-Selection
					line.add(tableName);	//  TableName
					line.add(cnt);  		//  Counter
					line.add(isTrx);  		//  IsTrx
					data.add(line);
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}

		//  Table
		ListModelTable tableModel = new ListModelTable(data);
		tableModel.addTableModelListener(this);
		m_tableListbox.setData(tableModel, columnNames);
		//
		m_tableListbox.setColumnClass(0, Boolean.class, false);   //  Selection
		m_tableListbox.setColumnClass(1, String.class, true);   //  TableName
		m_tableListbox.setColumnClass(2, Integer.class, true);  //  Counter
		m_tableListbox.setColumnClass(3, Boolean.class, true);  //  IsTrx
		//
		m_tableListbox.autoSize();
		setStatus();
	}

	private void clearTree() {
		tree.clear();
		if((tree.getChildren()).size() > 1) {
			List<Component> treePreviousChild = tree.getChildren();
			tree.removeChild((Treechildren) treePreviousChild.get(1));
		}
		tree.appendChild(rootTreeChild);
	}

	private void setStatus() {
		statusBar.getChildren().clear();
		statusBar.appendChild(new Label(Msg.getElement(Env.getCtx(), "AD_Table_ID") + ": "+ m_tableListbox.getModel().getSize()+ " / "
				+ Msg.getMsg(Env.getCtx(), "Selected") + ": "+ m_selected));
	}

	@Override
	public void tableChanged(WTableModelEvent e) {
		int row = e.getFirstRow();

		if (row < 0)
			return;

		clearTree();
		m_selected = 0;
		for (int i = 0; i < e.getModel().getSize(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> data = (Vector<Object>) e.getModel().getElementAt(i);
			boolean selected = (Boolean) data.get(0);
			if (selected) {
				String tableName = (String) data.get(1);
				MTable table = MTable.get(Env.getCtx(), tableName);
				generateTree(table.getAD_Table_ID(), clientId);
				m_selected++;
			}
		}
		setStatus();
	}

	private int getCountFromModel(String tableName) {
		int cnt = 0;
		for (int i=0; i<m_tableListbox.getModel().getSize(); i++) {
			String table = (String) m_tableListbox.getModel().getDataAt(i, 1);
			if (table.equals(tableName)) {
				cnt = (Integer) m_tableListbox.getModel().getDataAt(i, 2);
				break;
			}
		}
		return cnt;
	}

} 	// WDelete