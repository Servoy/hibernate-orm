/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class JdbcOperationQueryDelete extends AbstractJdbcOperationQuery implements JdbcOperationQueryMutation {

	public JdbcOperationQueryDelete(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			Set<FilterJdbcParameter> filterJdbcParameters,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters) {
		super( sql, parameterBinders, affectedTableNames, filterJdbcParameters, appliedParameters );
	}
}
