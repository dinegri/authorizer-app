package com.caju.authorizer.repository

import com.caju.authorizer.domain.Transaction
import zio.*
import zio.schema.*

trait TransactionRepository:
  def register(transaction: Transaction): Task[Unit]

object TransactionRepository:
  def register(user: Transaction): ZIO[TransactionRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[TransactionRepository](_.register(user))
