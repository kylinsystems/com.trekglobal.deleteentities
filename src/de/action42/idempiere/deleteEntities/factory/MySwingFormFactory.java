/******************************************************************************
 * Copyright (C) 2015 Dirk Niemeyer                                            *
 * Copyright (C) 2015 action 42 GmbH                 							  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package de.action42.idempiere.deleteEntities.factory;

import java.util.logging.Level;

import org.adempiere.ui.swing.factory.IFormFactory;
import org.compiere.apps.form.FormPanel;
import org.compiere.util.CLogger;

/**
 * @author a42niem
 *
 */
public class MySwingFormFactory implements IFormFactory {

	private final static CLogger log = CLogger.getCLogger(MySwingFormFactory.class);
	
	/**
	 * default constructor
	 */
	public MySwingFormFactory() {
	}

	/* (non-Javadoc)
	 * @see org.adempiere.ui.swing.factory.IFormFactory#newFormInstance(java.lang.String)
	 */
	@Override
	public FormPanel newFormInstance(String formName) {
		if (formName.equals("org.compiere.apps.form.VDelete") ) {
			Object form = null;
			Class<?> clazz = null;
			ClassLoader loader = getClass().getClassLoader();
			if (loader != null) {
				try {
					clazz = loader.loadClass(formName);
				} catch (Exception e) {
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, e.getLocalizedMessage(), e);
					}
				}
			}
			if (clazz == null) {
				loader = this.getClass().getClassLoader();
				try {
					clazz = loader.loadClass(formName);
				} catch (Exception e) {
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, e.getLocalizedMessage(), e);
					}
				}
			}
			if (clazz != null) {
				try {
					form = (FormPanel) clazz.newInstance();
				} catch (Exception e) {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, e.getLocalizedMessage(), e);
					}
				}
			}
			if (form != null) {
				if (form instanceof FormPanel) {
					return (FormPanel) form;
				}
			}
		}
		return null;
	}

}
