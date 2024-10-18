package com.caju.authorizer.domain

case class AccountBalance(id: String, accountId: String, balanceType: String, balance: BigDecimal)

case class AccountBalanceType(id: String, accountId: String, balanceType: Balance, balance: BigDecimal)
