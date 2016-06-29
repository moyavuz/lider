package tr.org.liderahenk.lider.report.templates;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;

import tr.org.liderahenk.lider.core.api.persistence.entities.IReportTemplate;
import tr.org.liderahenk.lider.core.api.persistence.entities.IReportTemplateColumn;
import tr.org.liderahenk.lider.core.api.persistence.entities.IReportTemplateParameter;
import tr.org.liderahenk.lider.core.api.plugin.BaseReportTemplate;

public class UserSessionReportTemplateImpl extends BaseReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8330754495877362709L;

	@Override
	public String getName() {
		return "Çevrimiçi Kullanıcılar";
	}

	@Override
	public String getDescription() {
		return "Anlık Olarak, Sisteme Bağlı Bulunan Tüm Kullanıcılara Ait Bilgiler İçeren Rapor.";
	}

	@Override
	public String getQuery() {
		return  "SELECT a.id, a.jid, us.username, us.createDate, a.ipAddresses, a.dn " +
				"  FROM UserSessionImpl us INNER JOIN us.agent a" +
				" WHERE us.sessionEvent = 1 " +
				"	AND NOT EXISTS (select 1 from UserSessionImpl logout where logout.sessionEvent = 2 and logout.agent = us.agent " +
				" 			and logout.username = us.username and logout.createDate > us.createDate)" +
				" ORDER BY us.createDate, us.username";
	}

	@Override
	public Set<? extends IReportTemplateParameter> getTemplateParams() {
		return null;
	}

	@SuppressWarnings("serial")
	@Override
	public Set<? extends IReportTemplateColumn> getTemplateColumns() {
		Set<IReportTemplateColumn> columns = new HashSet<IReportTemplateColumn>();
		columns.add(new IReportTemplateColumn() {
			@Override
			public Date getCreateDate() {
				return new Date();
			}
			
			@Override
			public IReportTemplate getTemplate() {
				return getSelf();
			}
			
			@Override
			public String getName() {
				return "Sıra No.";
			}
			
			@Override
			public Long getId() {
				return null;
			}
			
			@Override
			public Integer getColumnOrder() {
				return 1;
			}
		});
		columns.add(new IReportTemplateColumn() {
			@Override
			public Date getCreateDate() {
				return new Date();
			}

			@Override
			public IReportTemplate getTemplate() {
				return getSelf();
			}

			@Override
			public String getName() {
				return "Kullanıcı Adı";
			}

			@Override
			public Long getId() {
				return null;
			}

			@Override
			public Integer getColumnOrder() {
				return 4;
			}
		});
		columns.add(new IReportTemplateColumn() {
			@Override
			public Date getCreateDate() {
				return new Date();
			}

			@Override
			public IReportTemplate getTemplate() {
				return getSelf();
			}

			@Override
			public String getName() {
				return "Sisteme Giriş Tarihi";
			}

			@Override
			public Long getId() {
				return null;
			}

			@Override
			public Integer getColumnOrder() {
				return 5;
			}
		});
		columns.add(new IReportTemplateColumn() {
			@Override
			public Date getCreateDate() {
				return new Date();
			}

			@Override
			public IReportTemplate getTemplate() {
				return getSelf();
			}

			@Override
			public String getName() {
				return "IP Adresleri";
			}

			@Override
			public Long getId() {
				return null;
			}

			@Override
			public Integer getColumnOrder() {
				return 6;
			}
		});
		columns.add(new IReportTemplateColumn() {
			@Override
			public Date getCreateDate() {
				return new Date();
			}

			@Override
			public IReportTemplate getTemplate() {
				return getSelf();
			}

			@Override
			public String getName() {
				return "DN";
			}

			@Override
			public Long getId() {
				return null;
			}

			@Override
			public Integer getColumnOrder() {
				return 7;
			}
		});
		return columns;
	}

	@Override
	public String toJson() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected UserSessionReportTemplateImpl getSelf() {
		return this;
	}
}