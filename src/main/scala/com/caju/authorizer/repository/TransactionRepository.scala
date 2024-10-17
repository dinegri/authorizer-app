package com.caju.authorizer.repository

import zio.*
import zio.schema.*
import zio.schema.DeriveSchema.*

case class Transaction(account: String, totalAmount: Long, mcc: String, merchant: String)

object Transaction:
	given Schema[Transaction] = DeriveSchema.gen[Transaction]

trait TransactionRepository:
  def register(transaction: Transaction): Task[String]

object TransactionRepository:
  def register(user: Transaction): ZIO[TransactionRepository, Throwable, String] =
    ZIO.serviceWithZIO[TransactionRepository](_.register(user))
