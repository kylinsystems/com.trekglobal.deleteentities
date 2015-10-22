/******************************************************************************
 * Copyright (C) 2015 Dirk Niemeyer                                           *
 * Copyright (C) 2015 action 42 GmbH                 						  *
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
package com.trekglobal.deleteEntities.factory;

import java.util.logging.Level;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.util.CLogger;

/**
 * @author a42niem
 *
 */
public class MyZkFormFactory implements IFormFactory {

	private static final CLogger log = CLogger.getCLogger(MyZkFormFactory.class); 
			
	/**
	 * default constructor
	 */
	public MyZkFormFactory() {
	}

	/* (non-Javadoc)
	 * @see org.adempiere.webui.factory.IFormFactory#newFormInstance(java.lang.String)
	 */
	@Override
	public ADForm newFormInstance(String formName) {
		Object form = null;
		if (formName.equals("org.compiere.apps.form.VDelete") ) {
			// we cut short here
			String webClassName = "org.adempiere.webui.apps.form.WDelete";
    		
     		Class<?> clazz = null; 
     		ClassLoader	loader = this.getClass().getClassLoader();
     		try
     		{
     			//	Create instance w/o parameters
     			clazz = loader.loadClass(webClassName);
     		}
     		catch (Exception e)
     		{
     			if (log.isLoggable(Level.INFO))
     				log.log(Level.INFO, e.getLocalizedMessage(), e);
     		}

     		if (clazz != null) {
     			try
     			{
     				form = clazz.newInstance();
     			}
     			catch (Exception e)
     			{
     				if (log.isLoggable(Level.WARNING))
     					log.log(Level.WARNING, e.getLocalizedMessage(), e);
     			}
     		}
     		
		}
				
		
		if (form != null) {
			if (form instanceof ADForm) {
				return (ADForm)form;
			} else if (form instanceof IFormController) {
				IFormController controller = (IFormController) form;
				ADForm adForm = controller.getForm();
				adForm.setICustomForm(controller);
				return adForm;
			}
		}
		
		if (log.isLoggable(Level.INFO))
			log.info(formName + " not found at extension registry and classpath");
		return null;
	}



}
