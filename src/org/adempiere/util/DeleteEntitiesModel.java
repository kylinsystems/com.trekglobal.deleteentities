package org.adempiere.util;

import org.compiere.util.DB;
import org.compiere.util.Trx;

public class DeleteEntitiesModel {

	private boolean mandatoryLink;
	private String tableName;
	private String joinColumn;
	private String whereClause;
	private int clientId;

	public DeleteEntitiesModel(String tableName, boolean mandatoryLink, int clientId) {
		this.tableName = tableName;
		this.mandatoryLink = mandatoryLink;
		this.clientId = clientId;
	}

	public String getTableName() {
		return tableName;
	}

	public boolean isMandatoryLink() {
		return mandatoryLink;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(String whereClause) {
		if (   "T_Report".equals(tableName)
			|| "T_ReportStatement".equals(tableName)
			|| "AD_PInstance_Log".equals(tableName)) {
			// Replace AD_Client_ID=? with AD_PInstance_ID IN (SELECT AD_PInstance_ID FROM AD_PInstance WHERE AD_Client_ID=?)
			whereClause = whereClause.replaceAll(tableName + ".AD_Client_ID=\\?", 
				tableName +".AD_PInstance_ID IN (SELECT AD_PInstance_ID FROM AD_PInstance WHERE AD_Client_ID=?)");
		} else if ("AD_Attribute_Value".equals(tableName)) {
			// Replace AD_Client_ID=? with AD_Attribute_ID IN (SELECT AD_Attribute_ID FROM AD_Attribute WHERE AD_Client_ID=?)
			whereClause = whereClause.replaceAll(tableName + ".AD_Client_ID=\\?", 
				tableName +".AD_Attribute_ID IN (SELECT AD_Attribute_ID FROM AD_Attribute WHERE AD_Client_ID=?)");
		}
		this.whereClause = whereClause;
	}

	public String getJoinColumn() {
		return joinColumn;
	}

	public void setJoinColumn(String joinColumn) {
		this.joinColumn = joinColumn;
	}

	public int getCount() {
		String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
		return DB.getSQLValueEx(null, sql, clientId);
	}

	public int delete(Trx m_trx) {
		String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
		//log.log(Level.FINE, "Deleting: " + sql);
		int count = DB.executeUpdateEx(sql, new Object[] {clientId}, m_trx.getTrxName());
		//log.log(Level.FINE, (mandatoryLink ? "Deleted: " : "Updated: ") + count + " FROM " + tableName);
		return count;
	}
	
	public int update(Trx m_trx) {
		String sql = "UPDATE " + tableName + " SET " + joinColumn + "=NULL WHERE " + whereClause;
		//log.log(Level.FINE, "Updating: " + sql);
		int count = DB.executeUpdateEx(sql, new Object[] {clientId}, m_trx.getTrxName());
		//log.log(Level.FINE, (mandatoryLink ? "Deleted: " : "Updated: ") + count + " FROM " + tableName);
		return count;
	}
	
	@Override
	public String toString() {
		return tableName + (joinColumn == null ? "" :  "." + joinColumn);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeleteEntitiesModel other = (DeleteEntitiesModel) obj;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}

}