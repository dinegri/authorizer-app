package com.caju.authorizer.repository

import zio.*
import zio.schema.*
import zio.schema.DeriveSchema.*

case class Transaction(account: String, totalAmount: BigDecimal, mcc: String, merchant: String)

object Transaction:
	given Schema[Transaction] = DeriveSchema.gen[Transaction]

trait TransactionRepository:
  def register(transaction: Transaction): Task[Unit]

object TransactionRepository:
  def register(user: Transaction): ZIO[TransactionRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[TransactionRepository](_.register(user))
