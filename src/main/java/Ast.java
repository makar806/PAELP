sealed interface Expression permits VariableExpression, ConstantExpression, BinaryExpression {
}

record VariableExpression(String name) implements Expression {
}

record ConstantExpression(long value) implements Expression {
}

record BinaryExpression(String operator, Expression left, Expression right) implements Expression {
}

sealed interface Statement permits ReadStatement, WriteStatement, AssignmentStatement,
        SequenceStatement, WhileStatement, DoWhileStatement, IfStatement, SkipStatement {
}

record ReadStatement(String variable) implements Statement {
}

record WriteStatement(Expression expression) implements Statement {
}

record AssignmentStatement(String variable, Expression expression) implements Statement {
}

record SequenceStatement(Statement first, Statement second) implements Statement {
}

record WhileStatement(Expression condition, Statement body) implements Statement {
}

record DoWhileStatement(Statement body, Expression condition) implements Statement {
}

record IfStatement(Expression condition, Statement thenBranch, Statement elseBranch) implements Statement {
}

record SkipStatement() implements Statement {
}
