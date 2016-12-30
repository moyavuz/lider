/*
*
*    Copyright © 2015-2016 Tübitak ULAKBIM
*
*    This file is part of Lider Ahenk.
*
*    Lider Ahenk is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    Lider Ahenk is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with Lider Ahenk.  If not, see <http://www.gnu.org/licenses/>.
*/
package tr.org.liderahenk.lider.core.api.persistence.entities;

import java.util.Date;
import java.util.Set;

/**
 * 
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Kağan Akkaya</a>
 *
 */
public interface IReportTemplate extends IEntity {

	Long getId();

	String getName();

	String getDescription();

	/**
	 * Reporting bundle executes this query and returns its result as a list of
	 * object array (so make sure to use comma-separated SELECT statement
	 * instead of simply returning class instance, that way JPA will always
	 * return an object array).
	 * 
	 * @return JPQL statement
	 */
	String getQuery();

	Set<? extends IReportTemplateParameter> getTemplateParams();

	Set<? extends IReportTemplateColumn> getTemplateColumns();

	Date getCreateDate();

	Date getModifyDate();

	void addTemplateParameter(IReportTemplateParameter param);

	void addTemplateColumn(IReportTemplateColumn column);

	/**
	 * Unique report code
	 * 
	 * @return report code which is used in report privileges
	 */
	String getCode();

}
