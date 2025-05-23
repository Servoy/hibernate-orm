/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public abstract class PostgreSQLPGObjectJdbcType implements JdbcType {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PostgreSQLPGObjectJdbcType.class );
	private static final Constructor<Object> PG_OBJECT_CONSTRUCTOR;
	private static final Method TYPE_SETTER;
	private static final Method VALUE_SETTER;

	static {
		Constructor<Object> constructor = null;
		Method typeSetter = null;
		Method valueSetter = null;
		try {
			final Class<?> pgObjectClass = ReflectHelper.classForName(
					"org.postgresql.util.PGobject",
					PostgreSQLPGObjectJdbcType.class
			);
			//noinspection unchecked
			constructor = (Constructor<Object>) pgObjectClass.getConstructor();
			typeSetter = ReflectHelper.setterMethodOrNull( pgObjectClass, "type", String.class );
			valueSetter = ReflectHelper.setterMethodOrNull( pgObjectClass, "value", String.class );
		}
		catch (Exception e) {
			LOG.warn( "PostgreSQL JDBC driver classes are inaccessible and thus, certain DDL types like JSONB, JSON, GEOMETRY can not be used!", e );
		}
		PG_OBJECT_CONSTRUCTOR = constructor;
		TYPE_SETTER = typeSetter;
		VALUE_SETTER = valueSetter;
	}

	private final String typeName;
	private final int sqlTypeCode;

	public PostgreSQLPGObjectJdbcType(String typeName, int sqlTypeCode) {
		this.typeName = typeName;
		this.sqlTypeCode = sqlTypeCode;
	}

	public static boolean isUsable() {
		return PG_OBJECT_CONSTRUCTOR != null;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return sqlTypeCode;
	}

	public String getTypeName() {
		return typeName;
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		return javaType.wrap( string, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		return javaType.unwrap( value, String.class, options );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (PostgreSQLPGObjectJdbcType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				try {
					Object holder = PG_OBJECT_CONSTRUCTOR.newInstance();
					TYPE_SETTER.invoke( holder, typeName );
					VALUE_SETTER.invoke( holder, stringValue );
					st.setObject( index, holder );
				}
				catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
					throw new IllegalArgumentException( e );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String stringValue = ( (PostgreSQLPGObjectJdbcType) getJdbcType() ).toString(
						value,
						getJavaType(),
						options
				);
				try {
					Object holder = PG_OBJECT_CONSTRUCTOR.newInstance();
					TYPE_SETTER.invoke( holder, typeName );
					VALUE_SETTER.invoke( holder, stringValue );
					st.setObject( name, holder );
				}
				catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
					throw new IllegalArgumentException( e );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getObject( statement.getObject( name ), options );
			}

			private X getObject(Object object, WrapperOptions options) throws SQLException {
				if ( object == null ) {
					return null;
				}
				return ( (PostgreSQLPGObjectJdbcType) getJdbcType() ).fromString(
						object.toString(),
						getJavaType(),
						options
				);
			}
		};
	}
}
