/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.internal.SelfInterpretingSqmPath;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAggregateFunction;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmAnyDiscriminatorValue;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.tree.expression.SqmOver;
import org.hibernate.query.sqm.tree.expression.SqmOverflow;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.SqmWindow;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;

/**
 * Base support for an SQM walker
 *
 * @author Steve Ebersole
 */
public abstract class BaseSemanticQueryWalker implements SemanticQueryWalker<Object> {
	private final ServiceRegistry serviceRegistry;

	public BaseSemanticQueryWalker(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public Object visitStatement(SqmStatement<?> sqmStatement) {
		return sqmStatement.accept( this );
	}

	@Override
	public Object visitSelectStatement(SqmSelectStatement<?> statement) {
		visitCteContainer( statement );
		visitQueryPart( statement.getQueryPart() );
		return statement;
	}

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement<?> statement) {
		visitCteContainer( statement );
		statement.getTarget().accept( this );
		visitSetClause( statement.getSetClause() );
		visitWhereClause( statement.getWhereClause() );
		return statement;
	}

	@Override
	public Object visitSetClause(SqmSetClause setClause) {
		if ( setClause != null ) {
			for ( SqmAssignment<?> assignment : setClause.getAssignments() ) {
				visitAssignment( assignment );
			}
		}
		return setClause;
	}

	@Override
	public Object visitAssignment(SqmAssignment<?> assignment) {
		assignment.getTargetPath().accept( this );
		assignment.getValue().accept( this );
		return assignment;
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement<?> statement) {
		visitCteContainer( statement );
		statement.getTarget().accept( this );
		for ( SqmPath<?> stateField : statement.getInsertionTargetPaths() ) {
			stateField.accept( this );
		}
		statement.getSelectQueryPart().accept( this );
		return statement;
	}

	@Override
	public Object visitInsertValuesStatement(SqmInsertValuesStatement<?> statement) {
		visitCteContainer( statement );
		statement.getTarget().accept( this );
		for ( SqmPath<?> stateField : statement.getInsertionTargetPaths() ) {
			stateField.accept( this );
		}
		for ( SqmValues sqmValues : statement.getValuesList() ) {
			visitValues( sqmValues );
		}
		return statement;
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement<?> statement) {
		visitCteContainer( statement );
		statement.getTarget().accept( this );
		visitWhereClause( statement.getWhereClause() );
		return statement;
	}

	@Override
	public Object visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
		visitSelectQuery( sqmCteStatement.getCteDefinition() );
		return sqmCteStatement;
	}

	protected Object visitSelectQuery(SqmSelectQuery<?> selectQuery) {
		if ( selectQuery instanceof SqmSelectStatement<?> ) {
			return visitSelectStatement( (SqmSelectStatement<?>) selectQuery );
		}
		else {
			return visitSubQueryExpression( (SqmSubQuery<?>) selectQuery );
		}
	}

	@Override
	public Object visitCteContainer(SqmCteContainer consumer) {
		for ( SqmCteStatement<?> cteStatement : consumer.getCteStatements() ) {
			cteStatement.accept( this );
		}

		return consumer;
	}

	public Object visitQueryPart(SqmQueryPart<?> queryPart) {
		return queryPart.accept( this );
	}

	@Override
	public Object visitQueryGroup(SqmQueryGroup<?> queryGroup) {
		for ( SqmQueryPart<?> queryPart : queryGroup.getQueryParts() ) {
			queryPart.accept( this );
		}
		visitOrderByClause( queryGroup.getOrderByClause() );
		visitOffsetExpression( queryGroup.getOffsetExpression() );
		visitFetchExpression( queryGroup.getFetchExpression() );
		return queryGroup;
	}

	@Override
	public Object visitQuerySpec(SqmQuerySpec<?> querySpec) {
		visitFromClause( querySpec.getFromClause() );
		visitSelectClause( querySpec.getSelectClause() );
		visitWhereClause( querySpec.getWhereClause() );
		visitGroupByClause( querySpec.getGroupByClauseExpressions() );
		visitHavingClause( querySpec.getHavingClausePredicate() );
		visitOrderByClause( querySpec.getOrderByClause() );
		visitOffsetExpression( querySpec.getOffsetExpression() );
		visitFetchExpression( querySpec.getFetchExpression() );
		return querySpec;
	}

	@Override
	public Object visitFromClause(SqmFromClause fromClause) {
		fromClause.visitRoots( root -> root.accept( this ) );
		return fromClause;
	}

	@Override
	public Object visitRootPath(SqmRoot<?> sqmRoot) {
		sqmRoot.visitReusablePaths( path -> path.accept( this ) );
		sqmRoot.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return sqmRoot;
	}

	@Override
	public Object visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
		sqmRoot.getQueryPart().accept( this );
		sqmRoot.visitReusablePaths( path -> path.accept( this ) );
		sqmRoot.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return sqmRoot;
	}

	@Override
	public Object visitRootCte(SqmCteRoot<?> sqmRoot) {
		sqmRoot.visitReusablePaths( path -> path.accept( this ) );
		sqmRoot.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return sqmRoot;
	}

	@Override
	public Object visitCrossJoin(SqmCrossJoin<?> joinedFromElement) {
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return joinedFromElement;
	}

	@Override
	public Object visitPluralPartJoin(SqmPluralPartJoin<?, ?> joinedFromElement) {
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return joinedFromElement;
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin<?> joinedFromElement) {
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		if ( joinedFromElement.getJoinPredicate() != null ) {
			joinedFromElement.getJoinPredicate().accept( this );
		}
		return joinedFromElement;
	}

	@Override
	public Object visitQualifiedAttributeJoin(SqmAttributeJoin<?,?> joinedFromElement) {
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		if ( joinedFromElement.getJoinPredicate() != null ) {
			joinedFromElement.getJoinPredicate().accept( this );
		}
		return joinedFromElement;
	}

	@Override
	public Object visitQualifiedDerivedJoin(SqmDerivedJoin<?> joinedFromElement) {
		joinedFromElement.getQueryPart().accept( this );
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		if ( joinedFromElement.getJoinPredicate() != null ) {
			joinedFromElement.getJoinPredicate().accept( this );
		}
		return joinedFromElement;
	}

	@Override
	public Object visitQualifiedCteJoin(SqmCteJoin<?> joinedFromElement) {
		joinedFromElement.visitReusablePaths( path -> path.accept( this ) );
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		if ( joinedFromElement.getJoinPredicate() != null ) {
			joinedFromElement.getJoinPredicate().accept( this );
		}
		return joinedFromElement;
	}

	@Override
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath<?> path) {
		return path;
	}

	@Override
	public Object visitFkExpression(SqmFkExpression<?> fkExpression) {
		return fkExpression;
	}

	@Override
	public Object visitSelfInterpretingSqmPath(SelfInterpretingSqmPath<?> path) {
		return path;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		return path;
	}

	@Override
	public Object visitElementAggregateFunction(SqmElementAggregateFunction<?> path) {
		return path;
	}

	@Override
	public Object visitIndexAggregateFunction(SqmIndexAggregateFunction<?> path) {
		return path;
	}

	@Override
	public Object visitCorrelation(SqmCorrelation<?, ?> correlation) {
		correlation.visitReusablePaths( path -> path.accept( this ) );
		correlation.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return correlation;
	}

	@Override
	public Object visitSelectClause(SqmSelectClause selectClause) {
		if ( selectClause == null ) {
			return null;
		}

		// todo (6.0) : add the ability for certain SqlSelections to be sort of "implicit"...
		//		- they do not get rendered into the SQL, but do have a SqlReader
		//
		// this is useful in 2 specific:
		///		1) literals : no need to even send those to the database - we could
		//			just have the SqlSelectionReader return us back the literal value
		//		2) `EmptySqlSelection` : if this ends up being important at all..
		selectClause.getSelections().forEach( this::visitSelection );
		return selectClause;
	}

	@Override
	public Object visitSelection(SqmSelection<?> selection) {
		selection.getSelectableNode().accept( this );
		return selection;
	}

	@Override
	public Object visitJpaCompoundSelection(SqmJpaCompoundSelection<?> selection) {
		for ( SqmSelectableNode<?> selectionItem : selection.getSelectionItems() ) {
			selectionItem.accept( this );
		}
		return selection;
	}

	@Override
	public Object visitValues(SqmValues values) {
		for ( SqmExpression<?> expression : values.getExpressions() ) {
			expression.accept( this );
		}
		return values;
	}

	@Override
	public Object visitWhereClause(SqmWhereClause whereClause) {
		if ( whereClause == null ) {
			return null;
		}

		whereClause.getPredicate().accept( this );
		return whereClause;
	}

	@Override
	public Object visitGroupedPredicate(SqmGroupedPredicate predicate) {
		predicate.getSubPredicate().accept( this );
		return predicate;
	}

	@Override
	public Object visitJunctionPredicate(SqmJunctionPredicate predicate) {
		for ( SqmPredicate subPredicate : predicate.getPredicates() ) {
			subPredicate.accept( this );
		}
		return predicate;
	}

	@Override
	public Object visitComparisonPredicate(SqmComparisonPredicate predicate) {
		predicate.getLeftHandExpression().accept( this );
		predicate.getRightHandExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitIsNullPredicate(SqmNullnessPredicate predicate) {
		predicate.getExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitBetweenPredicate(SqmBetweenPredicate predicate) {
		predicate.getExpression().accept( this );
		predicate.getLowerBound().accept( this );
		predicate.getUpperBound().accept( this );
		return predicate;
	}

	@Override
	public Object visitLikePredicate(SqmLikePredicate predicate) {
		predicate.getMatchExpression().accept( this );
		predicate.getPattern().accept( this );
		if ( predicate.getEscapeCharacter() != null ) {
			predicate.getEscapeCharacter().accept( this );
		}
		return predicate;
	}

	@Override
	public Object visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		predicate.getLeftHandExpression().accept( this );
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitNegatedPredicate(SqmNegatedPredicate predicate) {
		predicate.getWrappedPredicate().accept( this );
		return predicate;
	}

	@Override
	public Object visitInListPredicate(SqmInListPredicate<?> predicate) {
		predicate.getTestExpression().accept( this );
		for ( SqmExpression<?> expression : predicate.getListExpressions() ) {
			expression.accept( this );
		}
		return predicate;
	}

	@Override
	public Object visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate) {
		predicate.getTestExpression().accept( this );
		predicate.getSubQueryExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate) {
		predicate.getBooleanExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate predicate) {
		predicate.getExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitOrderByClause(SqmOrderByClause orderByClause) {
		if ( orderByClause == null ) {
			return null;
		}

		if ( orderByClause.getSortSpecifications() != null ) {
			for ( SqmSortSpecification sortSpecification : orderByClause.getSortSpecifications() ) {
				visitSortSpecification( sortSpecification );
			}
		}
		return orderByClause;
	}

	@Override
	public Object visitSortSpecification(SqmSortSpecification sortSpecification) {
		sortSpecification.getSortExpression().accept( this );
		return sortSpecification;
	}

	@Override
	public Object visitOffsetExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		return expression.accept( this );
	}

	@Override
	public Object visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions) {
		if ( groupByClauseExpressions != null ) {
			groupByClauseExpressions.forEach( e -> e.accept( this ) );
		}
		return groupByClauseExpressions;
	}

	@Override
	public Object visitHavingClause(SqmPredicate sqmPredicate) {
		if ( sqmPredicate == null ) {
			return null;
		}
		return sqmPredicate.accept( this );
	}

	@Override
	public Object visitFetchExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		return expression.accept( this );
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter<?> expression) {
		return expression;
	}

	@Override
	public Object visitNamedParameterExpression(SqmNamedParameter<?> expression) {
		return expression;
	}

	@Override
	public Object visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression) {
		return expression;
	}

	@Override
	public Object visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> expression) {
		return expression;
	}

	@Override
	public Object visitAnyDiscriminatorTypeExpression(AnyDiscriminatorSqmPath expression) {
		return expression;
	}

	@Override
	public Object visitAnyDiscriminatorTypeValueExpression(SqmAnyDiscriminatorValue expression) {
		return expression;
	}

	@Override
	public Object visitParameterizedEntityTypeExpression(SqmParameterizedEntityType<?> expression) {
		expression.getDiscriminatorSource().accept( this );
		return expression;
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation<?> sqmExpression) {
		sqmExpression.getOperand().accept( this );
		return sqmExpression;
	}

	@Override
	public Object visitFunction(SqmFunction<?> sqmFunction) {
		sqmFunction.getArguments().forEach(
				e -> {
					if ( e instanceof SqmVisitableNode ) {
						( (SqmVisitableNode) e ).accept( this );
					}
				}
		);
		if ( sqmFunction instanceof SqmAggregateFunction<?> ) {
			final SqmPredicate filter = ( (SqmAggregateFunction<?>) sqmFunction ).getFilter();
			if ( filter != null ) {
				filter.accept( this );
			}
			if ( sqmFunction instanceof SqmOrderedSetAggregateFunction<?> ) {
				visitOrderByClause( ( (SqmOrderedSetAggregateFunction<?>) sqmFunction ).getWithinGroup() );
			}
		}
		return sqmFunction;
	}

	@Override
	public Object visitModifiedSubQueryExpression(SqmModifiedSubQueryExpression<?> expression) {
		return expression.getSubQuery().accept( this );
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit<?> extractUnit) {
		return extractUnit;
	}

	@Override
	public Object visitFormat(SqmFormat sqmFormat) {
		return sqmFormat;
	}

	@Override
	public Object visitCastTarget(SqmCastTarget<?> castTarget) {
		return castTarget;
	}

	@Override
	public Object visitCoalesce(SqmCoalesce<?> sqmCoalesce) {
		sqmCoalesce.getArguments().forEach( e -> e.accept( this ) );
		return sqmCoalesce;
	}

	@Override
	public Object visitToDuration(SqmToDuration<?> toDuration) {
		toDuration.getMagnitude().accept( this );
		toDuration.getUnit().accept( this );
		return toDuration;
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification trimSpecification) {
		return trimSpecification;
	}

	@Override
	public Object visitDistinct(SqmDistinct<?> distinct) {
		distinct.getExpression().accept( this );
		return distinct;
	}

	@Override
	public Object visitStar(SqmStar sqmStar) {
		return sqmStar;
	}

	@Override
	public Object visitOver(SqmOver<?> over) {
		over.getExpression().accept( this );
		over.getWindow().accept( this );
		return over;
	}

	@Override
	public Object visitWindow(SqmWindow window) {
		for ( SqmExpression<?> partition : window.getPartitions() ) {
			partition.accept( this );
		}
		for ( SqmSortSpecification sqmSortSpecification : window.getOrderList() ) {
			visitSortSpecification( sqmSortSpecification );
		}
		if ( window.getStartExpression() != null ) {
			window.getStartExpression().accept( this );
		}
		if ( window.getEndExpression() != null ) {
			window.getEndExpression().accept( this );
		}
		return window;
	}

	@Override
	public Object visitOverflow(SqmOverflow<?> sqmOverflow) {
		sqmOverflow.getSeparatorExpression().accept( this );
		if ( sqmOverflow.getFillerExpression() != null ) {
			sqmOverflow.getFillerExpression().accept( this );
		}
		return sqmOverflow;
	}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions


	@Override
	public Object visitTreatedPath(SqmTreatedPath<?, ?> sqmTreatedPath) {
		return sqmTreatedPath;
	}

	@Override
	public Object visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		function.getPluralPath().accept( this );
		return function;
	}

	@Override
	public Object visitMapEntryFunction(SqmMapEntryReference<?, ?> binding) {
		binding.getMapPath().accept( this );
		return binding;
	}

	@Override
	public Object visitLiteral(SqmLiteral<?> literal) {
		return literal;
	}

	@Override
	public Object visitTuple(SqmTuple<?> sqmTuple) {
		sqmTuple.getGroupedExpressions().forEach( e -> e.accept( this ) );
		return sqmTuple;
	}

	@Override
	public Object visitCollation(SqmCollation sqmCollate) {
		return sqmCollate;
	}

	@Override
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression) {
		expression.getLeftHandOperand().accept( this );
		expression.getRightHandOperand().accept( this );
		return expression;
	}

	public Object visitByUnit(SqmByUnit byUnit) {
		byUnit.getDuration().accept( this );
		byUnit.getUnit().accept( this );
		return byUnit;
	}

	@Override
	public Object visitDurationUnit(SqmDurationUnit<?> durationUnit) {
		return durationUnit;
	}

	@Override
	public Object visitSubQueryExpression(SqmSubQuery<?> expression) {
		expression.getQueryPart().accept( this );
		return expression;
	}

	@Override
	public Object visitSimpleCaseExpression(SqmCaseSimple<?,?> expression) {
		expression.getFixture().accept( this );
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			whenFragment.getCheckValue().accept( this );
			whenFragment.getResult().accept( this );
		}
		if ( expression.getOtherwise() != null ) {
			expression.getOtherwise().accept( this );
		}
		return expression;
	}

	@Override
	public Object visitAny(SqmAny<?> sqmAny) {
		sqmAny.getSubquery().accept( this );
		return sqmAny;
	}

	@Override
	public Object visitEvery(SqmEvery<?> sqmEvery) {
		sqmEvery.getSubquery().accept( this );
		return sqmEvery;
	}

	@Override
	public Object visitSummarization(SqmSummarization<?> sqmSummarization) {
		sqmSummarization.getGroupings().forEach( e -> e.accept( this ) );
		return sqmSummarization;
	}

	@Override
	public Object visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			whenFragment.getPredicate().accept( this );
			whenFragment.getResult().accept( this );
		}
		if ( expression.getOtherwise() != null ) {
			expression.getOtherwise().accept( this );
		}
		return expression;
	}

	@Override
	public Object visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation) {
		sqmDynamicInstantiation.getArguments().forEach( e -> e.getSelectableNode().accept( this ) );
		return sqmDynamicInstantiation;
	}


	@Override
	public Object visitFullyQualifiedClass(Class<?> namedClass) {
		throw new UnsupportedOperationException( "Not supported" );
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral<?> sqmEnumLiteral) {
		return sqmEnumLiteral;
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral) {
		return sqmFieldLiteral;
	}

}
