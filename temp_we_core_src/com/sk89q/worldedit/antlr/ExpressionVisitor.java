// Generated from com/sk89q/worldedit/antlr/Expression.g4 by ANTLR 4.13.2
package com.sk89q.worldedit.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#allStatements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllStatements(ExpressionParser.AllStatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(ExpressionParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(ExpressionParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(ExpressionParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(ExpressionParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(ExpressionParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#doStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoStatement(ExpressionParser.DoStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(ExpressionParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#simpleForStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleForStatement(ExpressionParser.SimpleForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(ExpressionParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(ExpressionParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(ExpressionParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#switchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(ExpressionParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Case}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase(ExpressionParser.CaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Default}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault(ExpressionParser.DefaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(ExpressionParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#emptyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement(ExpressionParser.EmptyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#assignmentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(ExpressionParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(ExpressionParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(ExpressionParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CEFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCEFallthrough(ExpressionParser.CEFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpr(ExpressionParser.TernaryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOFallthrough(ExpressionParser.COFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConditionalOrExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalOrExpr(ExpressionParser.ConditionalOrExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CAFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCAFallthrough(ExpressionParser.CAFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConditionalAndExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalAndExpr(ExpressionParser.ConditionalAndExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqualityExpr}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpr(ExpressionParser.EqualityExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqFallthrough}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqFallthrough(ExpressionParser.EqFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ReFallthrough}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReFallthrough(ExpressionParser.ReFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RelationalExpr}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpr(ExpressionParser.RelationalExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ShFallthrough}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShFallthrough(ExpressionParser.ShFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ShiftExpr}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftExpr(ExpressionParser.ShiftExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AddExpr}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExpr(ExpressionParser.AddExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AdFallthrough}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdFallthrough(ExpressionParser.AdFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MultiplicativeExpr}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpr(ExpressionParser.MultiplicativeExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MuFallthrough}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMuFallthrough(ExpressionParser.MuFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PowerExpr}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowerExpr(ExpressionParser.PowerExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PwFallthrough}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPwFallthrough(ExpressionParser.PwFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PreCrementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreCrementExpr(ExpressionParser.PreCrementExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PlusMinusExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusMinusExpr(ExpressionParser.PlusMinusExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UaFallthrough}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUaFallthrough(ExpressionParser.UaFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ComplementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplementExpr(ExpressionParser.ComplementExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpr(ExpressionParser.NotExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostfixExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpr(ExpressionParser.PostfixExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PoFallthrough}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPoFallthrough(ExpressionParser.PoFallthroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostCrementExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostCrementExpr(ExpressionParser.PostCrementExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FunctionCallExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallExpr(ExpressionParser.FunctionCallExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstantExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpr(ExpressionParser.ConstantExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IdExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdExpr(ExpressionParser.IdExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WrappedExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWrappedExpr(ExpressionParser.WrappedExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(ExpressionParser.ConstantExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(ExpressionParser.FunctionCallContext ctx);
}