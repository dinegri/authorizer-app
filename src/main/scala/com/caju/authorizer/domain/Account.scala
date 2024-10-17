package com.caju.authorizer.domain

import zio.schema.{DeriveSchema, Schema}

case class Account(id: String, balanceFood: BigDecimal, balanceMeal: BigDecimal, balanceCash: BigDecimal)

object Account:
	given Schema[Account] = DeriveSchema.gen[Account]