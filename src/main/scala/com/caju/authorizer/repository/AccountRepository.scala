package com.caju.authorizer.repository

import zio.*
import zio.schema.*
import zio.schema.DeriveSchema.*

case class Account(id: String, balanceFood: BigDecimal, balanceMeal: BigDecimal, balanceCash: BigDecimal)

object Account:
	given Schema[Account] = DeriveSchema.gen[Account]

trait AccountRepository:
  def register(user: Account): Task[String]

  def lookup(id: String): Task[Option[Account]]

  def update(account: Account): Task[Unit]

  def delete(id: String): Task[Unit]

  def updateFoodBalance(user: Account, balance: BigDecimal): Task[String]

  def updateMealBalance(user: Account, balance: BigDecimal): Task[Unit]

  def updateCashBalance(user: Account, balance: BigDecimal): Task[String]

object AccountRepository:
  def register(account: Account): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.register(account))

  def lookup(id: String): ZIO[AccountRepository, Throwable, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.lookup(id))

  def update(account: Account): ZIO[AccountRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.update(account))

  def delete(id: String): ZIO[AccountRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(id))

  def updateFoodBalance(account: Account, balance: BigDecimal): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.updateFoodBalance(account, balance))

  def updateMealBalance(account: Account, balance: BigDecimal): ZIO[AccountRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.updateMealBalance(account, balance))

  def updateCashBalance(account: Account, balance: BigDecimal): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.updateCashBalance(account, balance))
