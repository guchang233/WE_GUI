// Generated from com/sk89q/worldedit/antlr/Expression.g4 by ANTLR 4.13.2
package com.sk89q.worldedit.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpressionParser}.
 */
public interface ExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#allStatements}.
	 * @param ctx the parse tree
	 */
	void enterAllStatements(ExpressionParser.AllStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#allStatements}.
	 * @param ctx the parse tree
	 */
	void exitAllStatements(ExpressionParser.AllStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#statements}.
	 * @param ctx the parse tree
	 */
	void enterStatements(ExpressionParser.StatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#statements}.
	 * @param ctx the parse tree
	 */
	void exitStatements(ExpressionParser.StatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(ExpressionParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(ExpressionParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(ExpressionParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(ExpressionParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(ExpressionParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(ExpressionParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(ExpressionParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(ExpressionParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoStatement(ExpressionParser.DoStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoStatement(ExpressionParser.DoStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(ExpressionParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(ExpressionParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#simpleForStatement}.
	 * @param ctx the parse tree
	 */
	void enterSimpleForStatement(ExpressionParser.SimpleForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#simpleForStatement}.
	 * @param ctx the parse tree
	 */
	void exitSimpleForStatement(ExpressionParser.SimpleForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStatement(ExpressionParser.BreakStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStatement(ExpressionParser.BreakStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(ExpressionParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(ExpressionParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(ExpressionParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(ExpressionParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchStatement(ExpressionParser.SwitchStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchStatement(ExpressionParser.SwitchStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Case}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterCase(ExpressionParser.CaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Case}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitCase(ExpressionParser.CaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Default}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterDefault(ExpressionParser.DefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Default}
	 * labeled alternative in {@link ExpressionParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitDefault(ExpressionParser.DefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(ExpressionParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(ExpressionParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(ExpressionParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(ExpressionParser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpression(ExpressionParser.AssignmentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpression(ExpressionParser.AssignmentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(ExpressionParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(ExpressionParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(ExpressionParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(ExpressionParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CEFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void enterCEFallthrough(ExpressionParser.CEFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CEFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void exitCEFallthrough(ExpressionParser.CEFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TernaryExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpr(ExpressionParser.TernaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TernaryExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpr(ExpressionParser.TernaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterCOFallthrough(ExpressionParser.COFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitCOFallthrough(ExpressionParser.COFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConditionalOrExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalOrExpr(ExpressionParser.ConditionalOrExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConditionalOrExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalOrExpr(ExpressionParser.ConditionalOrExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CAFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterCAFallthrough(ExpressionParser.CAFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CAFallthrough}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitCAFallthrough(ExpressionParser.CAFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConditionalAndExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalAndExpr(ExpressionParser.ConditionalAndExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConditionalAndExpr}
	 * labeled alternative in {@link ExpressionParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalAndExpr(ExpressionParser.ConditionalAndExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EqualityExpr}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpr(ExpressionParser.EqualityExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EqualityExpr}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpr(ExpressionParser.EqualityExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EqFallthrough}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqFallthrough(ExpressionParser.EqFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EqFallthrough}
	 * labeled alternative in {@link ExpressionParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqFallthrough(ExpressionParser.EqFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReFallthrough}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterReFallthrough(ExpressionParser.ReFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReFallthrough}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitReFallthrough(ExpressionParser.ReFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RelationalExpr}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpr(ExpressionParser.RelationalExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RelationalExpr}
	 * labeled alternative in {@link ExpressionParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpr(ExpressionParser.RelationalExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ShFallthrough}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void enterShFallthrough(ExpressionParser.ShFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ShFallthrough}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void exitShFallthrough(ExpressionParser.ShFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ShiftExpr}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void enterShiftExpr(ExpressionParser.ShiftExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ShiftExpr}
	 * labeled alternative in {@link ExpressionParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void exitShiftExpr(ExpressionParser.ShiftExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AddExpr}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAddExpr(ExpressionParser.AddExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AddExpr}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAddExpr(ExpressionParser.AddExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AdFallthrough}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdFallthrough(ExpressionParser.AdFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AdFallthrough}
	 * labeled alternative in {@link ExpressionParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdFallthrough(ExpressionParser.AdFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MultiplicativeExpr}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpr(ExpressionParser.MultiplicativeExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MultiplicativeExpr}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpr(ExpressionParser.MultiplicativeExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MuFallthrough}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMuFallthrough(ExpressionParser.MuFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MuFallthrough}
	 * labeled alternative in {@link ExpressionParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMuFallthrough(ExpressionParser.MuFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PowerExpr}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 */
	void enterPowerExpr(ExpressionParser.PowerExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PowerExpr}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 */
	void exitPowerExpr(ExpressionParser.PowerExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PwFallthrough}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 */
	void enterPwFallthrough(ExpressionParser.PwFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PwFallthrough}
	 * labeled alternative in {@link ExpressionParser#powerExpression}.
	 * @param ctx the parse tree
	 */
	void exitPwFallthrough(ExpressionParser.PwFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PreCrementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreCrementExpr(ExpressionParser.PreCrementExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PreCrementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreCrementExpr(ExpressionParser.PreCrementExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PlusMinusExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPlusMinusExpr(ExpressionParser.PlusMinusExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PlusMinusExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPlusMinusExpr(ExpressionParser.PlusMinusExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UaFallthrough}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUaFallthrough(ExpressionParser.UaFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UaFallthrough}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUaFallthrough(ExpressionParser.UaFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComplementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterComplementExpr(ExpressionParser.ComplementExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComplementExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitComplementExpr(ExpressionParser.ComplementExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(ExpressionParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link ExpressionParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(ExpressionParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostfixExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpr(ExpressionParser.PostfixExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostfixExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpr(ExpressionParser.PostfixExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PoFallthrough}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPoFallthrough(ExpressionParser.PoFallthroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PoFallthrough}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPoFallthrough(ExpressionParser.PoFallthroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostCrementExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostCrementExpr(ExpressionParser.PostCrementExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostCrementExpr}
	 * labeled alternative in {@link ExpressionParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostCrementExpr(ExpressionParser.PostCrementExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FunctionCallExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpr(ExpressionParser.FunctionCallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FunctionCallExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpr(ExpressionParser.FunctionCallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstantExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpr(ExpressionParser.ConstantExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstantExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpr(ExpressionParser.ConstantExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IdExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterIdExpr(ExpressionParser.IdExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IdExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitIdExpr(ExpressionParser.IdExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WrappedExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterWrappedExpr(ExpressionParser.WrappedExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WrappedExpr}
	 * labeled alternative in {@link ExpressionParser#unprioritizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitWrappedExpr(ExpressionParser.WrappedExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(ExpressionParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(ExpressionParser.ConstantExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ExpressionParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ExpressionParser.FunctionCallContext ctx);
}