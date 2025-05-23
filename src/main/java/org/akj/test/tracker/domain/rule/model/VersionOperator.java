package org.akj.test.tracker.domain.rule.model;

public enum VersionOperator {
    GREATER_EQUAL(">="), LESS_EQUAL("<="), EQUAL("="), GREATER(">"), LESS("<"), TILDE("~"), CARET("^");
    private String sign;

    VersionOperator(String sign) {
        this.sign = sign;
    }

    public String getSign() {
        return sign;
    }

    public static VersionOperator fromSign(String sign) {
        for (VersionOperator operator : VersionOperator.values()) {
            if (operator.getSign().equals(sign)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Invalid version operator: " + sign);
    }
}
