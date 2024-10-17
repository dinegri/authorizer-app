package com.caju.authorizer.domain

import zio.schema.{DeriveSchema, Schema}

case class Transaction(account: String, totalAmount: BigDecimal, mcc: String, merchant: String)

object Transaction:
	given Schema[Transaction] = DeriveSchema.gen[Transaction]
