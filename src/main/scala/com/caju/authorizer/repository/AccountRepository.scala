package com.caju.authorizer.repository

import zio.*
import zio.schema.*
import zio.schema.DeriveSchema.*

case class Account(id: String, balanceFood: Double, balanceMeal: Double, balanceCash: Double)

object Account:
	given Schema[Account] = DeriveSchema.gen[Account]

trait AccountRepository:
  def register(user: Account): Task[String]

  def lookup(id: String): Task[Option[Account]]

  def updateFoodBalance(user: Account, balance: Double): Task[String]

  def updateMealBalance(user: Account, balance: Double): Task[String]

  def updateCashBalance(user: Account, balance: Double): Task[String]

object AccountRepository:
  def register(account: Account): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.register(account))

  def lookup(id: String): ZIO[AccountRepository, Throwable, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.lookup(id))

  def updateFoodBalance(account: Account, balance: Double): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.updateFoodBalance(account, balance))

  def updateMealBalance(account: Account, balance: Double): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.updateMealBalance(account, balance))

  def updateCashBalance(account: Account, balance: Double): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.updateCashBalance(account, balance))
